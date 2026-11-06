package com.example.focuslock.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.focuslock.data.local.FocusDatabase
import com.example.focuslock.data.local.SessionPreferences
import com.example.focuslock.data.local.entity.BlockedAppEntity
import com.example.focuslock.data.local.entity.FocusScheduleEntity
import com.example.focuslock.data.local.entity.FocusSessionEntity
import com.example.focuslock.data.model.InstalledAppInfo
import com.example.focuslock.data.model.SessionStats
import com.example.focuslock.data.model.SessionStatus
import com.example.focuslock.service.FocusMonitorService
import com.example.focuslock.service.SessionAlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FocusRepository(
    private val context: Context,
    private val database: FocusDatabase = FocusDatabase.getInstance(context),
    private val prefs: SessionPreferences = SessionPreferences.getInstance(context),
    val appManager: AppManager = AppManager(context)
) {
    private val sessionDao = database.focusSessionDao()
    private val blockedAppDao = database.blockedAppDao()
    private val scheduleDao = database.focusScheduleDao()

    val activeSessionFlow: Flow<FocusSessionEntity?> = sessionDao.getActiveSessionFlow()
    val allSessionsFlow: Flow<List<FocusSessionEntity>> = sessionDao.getAllSessionsFlow()
    val blockedAppsFlow: Flow<List<BlockedAppEntity>> = blockedAppDao.getAllBlockedAppsFlow()
    val schedulesFlow: Flow<List<FocusScheduleEntity>> = scheduleDao.getAllSchedulesFlow()

    val sessionStatsFlow: Flow<SessionStats> = allSessionsFlow.map { sessions ->
        var totalMinutes = 0L
        var completedCount = 0
        var totalDeflections = 0

        for (session in sessions) {
            totalDeflections += session.blocksPreventedCount
            if (session.status == SessionStatus.COMPLETED.name) {
                completedCount++
                totalMinutes += session.originalDurationMinutes
            } else if (session.status == SessionStatus.CANCELLED.name) {
                val elapsed = (session.endTimeMillis - session.startTimeMillis) / (60 * 1000L)
                if (elapsed > 0) {
                    totalMinutes += elapsed
                }
            }
        }

        SessionStats(
            totalFocusMinutes = totalMinutes,
            completedSessionsCount = completedCount,
            totalDeflectedAttempts = totalDeflections,
            currentStreakDays = calculateStreakDays(sessions)
        )
    }

    suspend fun getInstalledApps(): List<InstalledAppInfo> {
        val blockedPackages = withContext(Dispatchers.IO) {
            blockedAppDao.getBlockedPackageNamesSync().toSet()
        }
        return appManager.getInstalledLauncherApps(blockedPackages)
    }

    suspend fun toggleAppBlocked(app: InstalledAppInfo) = withContext(Dispatchers.IO) {
        if (app.isBlocked) {
            blockedAppDao.delete(app.packageName)
        } else {
            blockedAppDao.insertOrUpdate(
                BlockedAppEntity(
                    packageName = app.packageName,
                    appName = app.appName,
                    isCurrentlyBlocked = true,
                    category = app.category
                )
            )
        }
        // If there's an active session, also update prefs
        if (prefs.isSessionActive) {
            val updated = blockedAppDao.getBlockedPackageNamesSync().toSet()
            prefs.activeBlockedPackages = updated
        }
    }

    suspend fun setAppsBlockedState(packages: List<InstalledAppInfo>, shouldBlock: Boolean) =
        withContext(Dispatchers.IO) {
            if (shouldBlock) {
                val entities = packages.map {
                    BlockedAppEntity(
                        packageName = it.packageName,
                        appName = it.appName,
                        isCurrentlyBlocked = true,
                        category = it.category
                    )
                }
                blockedAppDao.insertAll(entities)
            } else {
                for (app in packages) {
                    blockedAppDao.delete(app.packageName)
                }
            }
            if (prefs.isSessionActive) {
                prefs.activeBlockedPackages = blockedAppDao.getBlockedPackageNamesSync().toSet()
            }
        }

    suspend fun startSession(
        durationMinutes: Int,
        isStrictMode: Boolean,
        overrideBlockedPackages: List<String>? = null
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val endTime = now + (durationMinutes * 60 * 1000L)

        val targetPackages = if (!overrideBlockedPackages.isNullOrEmpty()) {
            overrideBlockedPackages
        } else {
            blockedAppDao.getBlockedPackageNamesSync()
        }

        // Clean up any stale active sessions
        sessionDao.endAllActiveSessions(SessionStatus.CANCELLED.name, now)

        val entity = FocusSessionEntity(
            startTimeMillis = now,
            endTimeMillis = endTime,
            originalDurationMinutes = durationMinutes,
            status = SessionStatus.ACTIVE.name,
            isStrictMode = isStrictMode,
            blockedPackageNamesJson = targetPackages.joinToString(","),
            blocksPreventedCount = 0
        )

        val sessionId = sessionDao.insertSession(entity)

        // Update fast SharedPreferences for instantaneous service lookup
        prefs.setActiveSession(
            sessionId = sessionId,
            startTime = now,
            endTime = endTime,
            isStrictMode = isStrictMode,
            blockedPackages = targetPackages.toSet()
        )

        // Schedule Alarm for completion
        scheduleSessionAlarm(sessionId, endTime)

        // Start Foreground Notification Service
        FocusMonitorService.start(context, endTime, durationMinutes)

        sessionId
    }

    suspend fun endSessionEarly(sessionId: Long) = withContext(Dispatchers.IO) {
        val session = sessionDao.getSessionById(sessionId)
        if (session != null && session.isStrictMode) {
            // Strict Mode: early cancellation is strictly forbidden
            return@withContext
        }

        val now = System.currentTimeMillis()
        if (session != null) {
            sessionDao.updateSession(
                session.copy(
                    status = SessionStatus.CANCELLED.name,
                    endTimeMillis = now
                )
            )
        } else {
            sessionDao.endAllActiveSessions(SessionStatus.CANCELLED.name, now)
        }

        prefs.clearActiveSession()
        cancelSessionAlarm(sessionId)
        FocusMonitorService.stop(context)
    }

    suspend fun completeSession(sessionId: Long) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val session = sessionDao.getSessionById(sessionId)
        if (session != null) {
            sessionDao.updateSession(
                session.copy(
                    status = SessionStatus.COMPLETED.name,
                    endTimeMillis = now
                )
            )
        } else {
            sessionDao.endAllActiveSessions(SessionStatus.COMPLETED.name, now)
        }

        prefs.clearActiveSession()
        cancelSessionAlarm(sessionId)
        FocusMonitorService.stop(context)
    }

    suspend fun recordBlockInterception(sessionId: Long) = withContext(Dispatchers.IO) {
        sessionDao.incrementBlocksPrevented(sessionId)
    }

    // Schedule management
    suspend fun addSchedule(schedule: FocusScheduleEntity) = withContext(Dispatchers.IO) {
        scheduleDao.insert(schedule)
    }

    suspend fun updateSchedule(schedule: FocusScheduleEntity) = withContext(Dispatchers.IO) {
        scheduleDao.update(schedule)
    }

    suspend fun deleteSchedule(id: Long) = withContext(Dispatchers.IO) {
        scheduleDao.deleteById(id)
    }

    private fun scheduleSessionAlarm(sessionId: Long, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, SessionAlarmReceiver::class.java).apply {
            putExtra(SessionAlarmReceiver.EXTRA_SESSION_ID, sessionId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, sessionId.toInt(), intent, flags)

        com.example.focuslock.service.AlarmSchedulerHelper.scheduleAlarmSafely(
            alarmManager = alarmManager,
            type = AlarmManager.RTC_WAKEUP,
            triggerAtMillis = triggerAtMillis,
            operation = pendingIntent
        )
    }

    private fun cancelSessionAlarm(sessionId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, SessionAlarmReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, sessionId.toInt(), intent, flags)
        alarmManager.cancel(pendingIntent)
    }

    private fun calculateStreakDays(sessions: List<FocusSessionEntity>): Int {
        val completed = sessions.filter { it.status == SessionStatus.COMPLETED.name }
        if (completed.isEmpty()) return 0
        // Simple day grouping calculation
        val days = completed.map {
            it.startTimeMillis / (1000L * 60 * 60 * 24)
        }.distinct().sortedDescending()

        val today = System.currentTimeMillis() / (1000L * 60 * 60 * 24)
        var streak = 0
        var expectedDay = today

        for (day in days) {
            if (day == expectedDay || (streak == 0 && day == expectedDay - 1)) {
                streak++
                expectedDay = day - 1
            } else {
                break
            }
        }
        return streak
    }

    companion object {
        @Volatile
        private var INSTANCE: FocusRepository? = null

        fun getInstance(context: Context): FocusRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = FocusRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
