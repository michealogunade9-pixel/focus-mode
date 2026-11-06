package com.example.focuslock.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.accessibility.AccessibilityEvent
import com.example.focuslock.data.local.ActiveFocusState
import com.example.focuslock.data.local.SessionPreferences
import com.example.focuslock.data.repository.FocusRepository
import com.example.ui.BlockingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Primary focus deflection engine.
 * Receives window state change accessibility events with user consent and prominent disclosure,
 * instantly deflecting user-selected blocked apps (e.g. Google Chrome) during an active focus block.
 */
class FocusAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastBlockedPackage: String? = null
    private var lastBlockTimestamp: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 20
        }
        serviceInfo = info

        // Ensure in-memory state is synchronized on service bind
        val prefs = SessionPreferences.getInstance(this)
        ActiveFocusState.syncFromPrefs(prefs)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val targetPackage = event.packageName?.toString() ?: return
        val ownPackage = packageName

        // Ignore our own app, system UI, Android settings (to allow managing permissions), and launchers
        if (targetPackage == ownPackage ||
            targetPackage == "com.android.systemui" ||
            targetPackage == "com.android.settings" ||
            targetPackage.contains("launcher") ||
            targetPackage.contains("trebuchet")
        ) {
            return
        }

        val prefs = SessionPreferences.getInstance(this)
        if (!ActiveFocusState.isSessionActive && !prefs.isSessionActive) {
            return
        }

        // If in-memory state was cleared by process restart, resync immediately
        if (!ActiveFocusState.isSessionActive && prefs.isSessionActive) {
            ActiveFocusState.syncFromPrefs(prefs)
        }

        val now = System.currentTimeMillis()
        val endTime = if (ActiveFocusState.activeSessionEndTime > 0) {
            ActiveFocusState.activeSessionEndTime
        } else {
            prefs.activeSessionEndTime
        }

        if (endTime in 1..now) {
            // Session expired! Mark complete and do not block
            val sessionId = if (ActiveFocusState.activeSessionId > 0) {
                ActiveFocusState.activeSessionId
            } else {
                prefs.activeSessionId
            }
            ActiveFocusState.clear()
            serviceScope.launch {
                FocusRepository.getInstance(applicationContext).completeSession(sessionId)
            }
            return
        }

        val isTargetBlocked = ActiveFocusState.isBlocked(targetPackage) ||
                prefs.activeBlockedPackages.contains(targetPackage)

        if (isTargetBlocked) {
            // Debounce rapid duplicate events from same package
            if (targetPackage == lastBlockedPackage && (now - lastBlockTimestamp) < 1000L) {
                return
            }
            lastBlockedPackage = targetPackage
            lastBlockTimestamp = now

            // 1. Vibrate for physical feedback if enabled
            if (prefs.vibrationFeedbackEnabled) {
                triggerVibration()
            }

            // 2. Launch full-screen blocking activity immediately over the blocked app.
            // Using singleInstance launchMode and reorder-to-front flags ensures instantaneous display.
            val blockingIntent = Intent(this, BlockingActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
                putExtra(BlockingActivity.EXTRA_PACKAGE_NAME, targetPackage)
                putExtra(BlockingActivity.EXTRA_END_TIME, endTime)
                putExtra(BlockingActivity.EXTRA_STRICT_MODE, prefs.isStrictModeActive)
                putExtra(BlockingActivity.EXTRA_SESSION_ID, prefs.activeSessionId)
            }
            startActivity(blockingIntent)

            // 3. Increment deflection analytics counter
            val sessionId = prefs.activeSessionId
            if (sessionId > 0) {
                serviceScope.launch {
                    FocusRepository.getInstance(applicationContext).recordBlockInterception(sessionId)
                }
            }
        }
    }

    override fun onInterrupt() {}

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(120)
                }
            }
        } catch (_: Exception) {}
    }
}
