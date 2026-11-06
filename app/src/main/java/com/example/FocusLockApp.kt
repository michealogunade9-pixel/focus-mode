package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.focuslock.data.local.FocusDatabase
import com.example.focuslock.data.local.SessionPreferences
import com.example.focuslock.data.repository.FocusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FocusLockApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: FocusDatabase
        private set
    lateinit var preferences: SessionPreferences
        private set
    lateinit var repository: FocusRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = FocusDatabase.getInstance(this)
        preferences = SessionPreferences.getInstance(this)
        repository = FocusRepository.getInstance(this)

        com.example.focuslock.data.local.ActiveFocusState.syncFromPrefs(preferences)

        createNotificationChannels()

        // Sync and restore active session if needed
        applicationScope.launch(Dispatchers.IO) {
            val activeSession = database.focusSessionDao().getActiveSessionSync()
            val now = System.currentTimeMillis()
            if (activeSession != null) {
                if (activeSession.endTimeMillis <= now) {
                    // Stale session, mark complete
                    repository.completeSession(activeSession.id)
                } else {
                    // Still active! Sync with preferences & ActiveFocusState
                    val blockedPackages = database.blockedAppDao().getBlockedPackageNamesSync().toSet()
                    preferences.setActiveSession(
                        sessionId = activeSession.id,
                        startTime = activeSession.startTimeMillis,
                        endTime = activeSession.endTimeMillis,
                        isStrictMode = activeSession.isStrictMode,
                        blockedPackages = blockedPackages
                    )
                }
            } else if (preferences.isSessionActive) {
                // Prefs says active but DB has none, clean up
                preferences.clearActiveSession()
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            val foregroundChannel = NotificationChannel(
                "focuslock_foreground_channel",
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }

            val completionChannel = NotificationChannel(
                "focuslock_completion_channel",
                "FocusLock Completion",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when a focus session finishes successfully"
            }

            manager.createNotificationChannel(foregroundChannel)
            manager.createNotificationChannel(completionChannel)
        }
    }
}
