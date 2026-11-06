package com.example.focuslock.viewmodel

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.focuslock.data.local.SessionPreferences
import com.example.focuslock.data.local.entity.BlockedAppEntity
import com.example.focuslock.data.local.entity.FocusScheduleEntity
import com.example.focuslock.data.local.entity.FocusSessionEntity
import com.example.focuslock.data.model.InstalledAppInfo
import com.example.focuslock.data.model.SessionStats
import com.example.focuslock.data.repository.FocusRepository
import com.example.focuslock.service.ScheduleAlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PermissionState(
    val hasAccessibilityPermission: Boolean = false,
    val hasUsageStatsPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val hasNotificationListenerPermission: Boolean = false
)

data class MostBlockedAppStat(
    val appName: String,
    val packageName: String,
    val count: Int
)

data class DailyFocusStat(
    val dayLabel: String,
    val minutes: Long
)

data class FocusUiState(
    val activeSession: FocusSessionEntity? = null,
    val remainingMillis: Long = 0L,
    val isSessionRunning: Boolean = false,
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val blockedApps: List<BlockedAppEntity> = emptyList(),
    val sessionsHistory: List<FocusSessionEntity> = emptyList(),
    val schedules: List<FocusScheduleEntity> = emptyList(),
    val stats: SessionStats = SessionStats(0, 0, 0, 0),
    val mostBlockedApps: List<MostBlockedAppStat> = emptyList(),
    val weeklyFocusStats: List<DailyFocusStat> = emptyList(),
    val permissions: PermissionState = PermissionState(),
    val isLoadingApps: Boolean = false,
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val isStrictModeDefault: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val isBlockNotificationsEnabled: Boolean = true,
    val isOnboardingDone: Boolean = false
)

class FocusViewModel(
    private val repository: FocusRepository,
    private val preferences: SessionPreferences,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FocusUiState(
            isStrictModeDefault = preferences.isStrictModeDefault,
            isVibrationEnabled = preferences.vibrationFeedbackEnabled,
            isBlockNotificationsEnabled = preferences.blockNotificationsEnabled,
            isOnboardingDone = preferences.isOnboardingCompleted
        )
    )
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private val _remainingTimeMillis = MutableStateFlow(0L)

    init {
        // Collect active session and update remaining countdown
        viewModelScope.launch {
            repository.activeSessionFlow.collect { activeSession ->
                val isRunning = activeSession != null && activeSession.endTimeMillis > System.currentTimeMillis()
                _uiState.value = _uiState.value.copy(
                    activeSession = activeSession,
                    isSessionRunning = isRunning
                )
            }
        }

        // Timer countdown ticker
        viewModelScope.launch {
            while (isActive) {
                val active = _uiState.value.activeSession
                if (active != null) {
                    val remaining = (active.endTimeMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                    _remainingTimeMillis.value = remaining
                    _uiState.value = _uiState.value.copy(
                        remainingMillis = remaining,
                        isSessionRunning = remaining > 0
                    )
                    if (remaining == 0L && active.status == "ACTIVE") {
                        repository.completeSession(active.id)
                    }
                } else {
                    _remainingTimeMillis.value = 0L
                    _uiState.value = _uiState.value.copy(
                        remainingMillis = 0L,
                        isSessionRunning = false
                    )
                }
                delay(1000L)
            }
        }

        // Collect blocked apps
        viewModelScope.launch {
            repository.blockedAppsFlow.collect { blockedList ->
                _uiState.value = _uiState.value.copy(blockedApps = blockedList)
                // Refresh blocked state on installed apps if loaded
                val blockedSet = blockedList.map { it.packageName }.toSet()
                val updatedInstalled = _uiState.value.installedApps.map {
                    it.copy(isBlocked = blockedSet.contains(it.packageName))
                }
                _uiState.value = _uiState.value.copy(installedApps = updatedInstalled)
            }
        }

        // Collect session history & compute analytics
        viewModelScope.launch {
            repository.allSessionsFlow.collect { sessions ->
                val mostBlocked = computeMostBlockedApps(sessions)
                val weekly = computeWeeklyFocusStats(sessions)
                _uiState.value = _uiState.value.copy(
                    sessionsHistory = sessions,
                    mostBlockedApps = mostBlocked,
                    weeklyFocusStats = weekly
                )
            }
        }

        // Collect stats
        viewModelScope.launch {
            repository.sessionStatsFlow.collect { stats ->
                _uiState.value = _uiState.value.copy(stats = stats)
            }
        }

        // Collect schedules
        viewModelScope.launch {
            repository.schedulesFlow.collect { schedulesList ->
                _uiState.value = _uiState.value.copy(schedules = schedulesList)
            }
        }

        refreshPermissions()
        loadInstalledApps()
    }

    fun completeOnboarding() {
        preferences.isOnboardingCompleted = true
        _uiState.value = _uiState.value.copy(isOnboardingDone = true)
    }

    fun refreshPermissions() {
        viewModelScope.launch(Dispatchers.IO) {
            val hasAccessibility = isAccessibilityServiceEnabled(context)
            val hasUsage = hasUsageStatsPermission(context)
            val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
            val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true

            val hasNotificationListener = isNotificationListenerEnabled(context)

            _uiState.value = _uiState.value.copy(
                permissions = PermissionState(
                    hasAccessibilityPermission = hasAccessibility,
                    hasUsageStatsPermission = hasUsage,
                    hasNotificationPermission = hasNotification,
                    hasOverlayPermission = hasOverlay,
                    hasNotificationListenerPermission = hasNotificationListener
                )
            )
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingApps = true)
            val apps = repository.getInstalledApps()
            _uiState.value = _uiState.value.copy(
                installedApps = apps,
                isLoadingApps = false
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setSelectedCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun toggleAppBlocked(app: InstalledAppInfo) {
        val active = _uiState.value.activeSession
        if (active != null && active.isStrictMode && _uiState.value.isSessionRunning) {
            // Strict Mode: selected apps are locked and cannot be modified while session is active
            return
        }
        viewModelScope.launch {
            repository.toggleAppBlocked(app)
            // Immediately update in-memory list for snappy responsiveness
            val updated = _uiState.value.installedApps.map {
                if (it.packageName == app.packageName) it.copy(isBlocked = !app.isBlocked) else it
            }
            _uiState.value = _uiState.value.copy(installedApps = updated)
        }
    }

    fun selectAllFilteredApps(block: Boolean) {
        val active = _uiState.value.activeSession
        if (active != null && active.isStrictMode && _uiState.value.isSessionRunning) {
            // Strict Mode: selected apps are locked and cannot be modified while session is active
            return
        }
        viewModelScope.launch {
            val filtered = getFilteredApps()
            repository.setAppsBlockedState(filtered, block)
            val updatedMap = filtered.associate { it.packageName to block }
            val updated = _uiState.value.installedApps.map {
                if (updatedMap.containsKey(it.packageName)) it.copy(isBlocked = block) else it
            }
            _uiState.value = _uiState.value.copy(installedApps = updated)
        }
    }

    fun getFilteredApps(): List<InstalledAppInfo> {
        val state = _uiState.value
        return state.installedApps.filter { app ->
            val matchesCategory = state.selectedCategory == "All" || app.category == state.selectedCategory
            val matchesQuery = state.searchQuery.isBlank() ||
                    app.appName.contains(state.searchQuery, ignoreCase = true) ||
                    app.packageName.contains(state.searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    fun startSession(durationMinutes: Int, isStrictMode: Boolean, overridePackages: List<String>? = null) {
        if (_uiState.value.isSessionRunning) {
            // An active session is already running - cannot override duration
            return
        }
        viewModelScope.launch {
            repository.startSession(durationMinutes, isStrictMode, overridePackages)
        }
    }

    fun endSessionEarly() {
        val active = _uiState.value.activeSession ?: return
        if (active.isStrictMode) {
            // Strict Mode: ordinary cancellation through the interface is prohibited
            return
        }
        viewModelScope.launch {
            repository.endSessionEarly(active.id)
        }
    }

    fun addSchedule(
        title: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        daysOfWeekMask: Int,
        isStrictMode: Boolean,
        targetPackages: List<String>
    ) {
        viewModelScope.launch {
            val entity = FocusScheduleEntity(
                title = title,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                daysOfWeekMask = daysOfWeekMask,
                isEnabled = true,
                isStrictMode = isStrictMode,
                targetPackagesJson = targetPackages.joinToString(",")
            )
            val id = repository.addSchedule(entity)
            ScheduleAlarmReceiver.scheduleAlarm(context, entity.copy(id = id))
        }
    }

    fun toggleSchedule(schedule: FocusScheduleEntity) {
        viewModelScope.launch {
            val updated = schedule.copy(isEnabled = !schedule.isEnabled)
            repository.updateSchedule(updated)
            if (updated.isEnabled) {
                ScheduleAlarmReceiver.scheduleAlarm(context, updated)
            } else {
                ScheduleAlarmReceiver.cancelAlarm(context, updated.id)
            }
        }
    }

    fun deleteSchedule(scheduleId: Long) {
        viewModelScope.launch {
            ScheduleAlarmReceiver.cancelAlarm(context, scheduleId)
            repository.deleteSchedule(scheduleId)
        }
    }

    fun setStrictModeDefault(enabled: Boolean) {
        preferences.isStrictModeDefault = enabled
        _uiState.value = _uiState.value.copy(isStrictModeDefault = enabled)
    }

    fun setVibrationFeedback(enabled: Boolean) {
        preferences.vibrationFeedbackEnabled = enabled
        _uiState.value = _uiState.value.copy(isVibrationEnabled = enabled)
    }

    private fun computeMostBlockedApps(sessions: List<FocusSessionEntity>): List<MostBlockedAppStat> {
        val packageCounts = mutableMapOf<String, Int>()
        for (session in sessions) {
            val pkgs = session.blockedPackageNamesJson.split(",").filter { it.isNotBlank() }
            for (pkg in pkgs) {
                packageCounts[pkg] = (packageCounts[pkg] ?: 0) + 1
            }
        }
        return packageCounts.entries
            .sortedByDescending { it.value }
            .take(6)
            .map { (pkg, count) ->
                MostBlockedAppStat(
                    appName = repository.appManager.getAppName(pkg),
                    packageName = pkg,
                    count = count
                )
            }
    }

    private fun computeWeeklyFocusStats(sessions: List<FocusSessionEntity>): List<DailyFocusStat> {
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val now = java.util.Calendar.getInstance()
        val result = mutableListOf<DailyFocusStat>()

        // Last 7 days
        for (i in 6 downTo 0) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
            val dayOfYear = cal.get(java.util.Calendar.DAY_OF_YEAR)
            val year = cal.get(java.util.Calendar.YEAR)
            val dayOfWeekIndex = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 // 0=Mon ... 6=Sun
            val label = dayLabels[dayOfWeekIndex]

            val minutesOnDay = sessions.filter {
                val sCal = java.util.Calendar.getInstance().apply { timeInMillis = it.startTimeMillis }
                sCal.get(java.util.Calendar.DAY_OF_YEAR) == dayOfYear &&
                        sCal.get(java.util.Calendar.YEAR) == year &&
                        (it.status == "COMPLETED" || it.status == "CANCELLED")
            }.sumOf {
                if (it.status == "COMPLETED") it.originalDurationMinutes.toLong()
                else ((it.endTimeMillis - it.startTimeMillis) / 60000L).coerceAtLeast(0L)
            }

            result.add(DailyFocusStat(label, minutesOnDay))
        }

        return result
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedServiceName = "${context.packageName}/com.example.focuslock.service.FocusAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(":").any {
            it.equals(expectedServiceName, ignoreCase = true) ||
                    it.contains("FocusAccessibilityService", ignoreCase = true)
        }
    }

    @Suppress("DEPRECATION")
    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val myService = "${context.packageName}/com.example.focuslock.service.FocusNotificationListenerService"
        return enabledListeners.split(":").any {
            it.equals(myService, ignoreCase = true) || it.contains("FocusNotificationListenerService", ignoreCase = true)
        }
    }

    fun setBlockNotifications(enabled: Boolean) {
        preferences.blockNotificationsEnabled = enabled
        _uiState.value = _uiState.value.copy(isBlockNotificationsEnabled = enabled)
    }
}

class FocusViewModelFactory(
    private val repository: FocusRepository,
    private val preferences: SessionPreferences,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FocusViewModel(repository, preferences, context) as T
    }
}
