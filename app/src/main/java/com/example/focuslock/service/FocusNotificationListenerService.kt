package com.example.focuslock.service

import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.focuslock.data.local.ActiveFocusState
import com.example.focuslock.data.local.SessionPreferences
import com.example.focuslock.data.repository.FocusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Optional Notification Shield Service.
 * Suppresses incoming notification banners, sounds, and alerts from user-selected
 * blocked applications (e.g. Chrome, social media, messaging) while an active focus session is running.
 */
class FocusNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val targetPackage = sbn.packageName ?: return
        val ownPackage = packageName

        // Ignore our own app, system alerts, calls, and Android OS notifications
        if (targetPackage == ownPackage ||
            targetPackage == "android" ||
            targetPackage == "com.android.systemui" ||
            targetPackage == "com.android.dialer" ||
            targetPackage == "com.google.android.dialer"
        ) {
            return
        }

        // Never cancel ongoing system or call notifications
        if (sbn.isOngoing) {
            return
        }

        val prefs = SessionPreferences.getInstance(this)
        if (!prefs.blockNotificationsEnabled) {
            return
        }

        val isSessionActive = ActiveFocusState.isSessionActive || prefs.isSessionActive
        if (!isSessionActive) {
            return
        }

        val now = System.currentTimeMillis()
        val endTime = if (ActiveFocusState.activeSessionEndTime > 0) {
            ActiveFocusState.activeSessionEndTime
        } else {
            prefs.activeSessionEndTime
        }

        if (now >= endTime) {
            return
        }

        val isBlocked = ActiveFocusState.isBlocked(targetPackage) ||
                prefs.activeBlockedPackages.contains(targetPackage)

        if (isBlocked) {
            try {
                // Cancel notification from blocked app to prevent distraction
                cancelNotification(sbn.key)

                // Increment blocked distraction stat
                val sessionId = prefs.activeSessionId
                if (sessionId > 0) {
                    serviceScope.launch {
                        FocusRepository.getInstance(applicationContext).recordBlockInterception(sessionId)
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
