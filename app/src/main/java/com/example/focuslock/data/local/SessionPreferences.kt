package com.example.focuslock.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("focuslock_prefs", Context.MODE_PRIVATE)

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var isStrictModeDefault: Boolean
        get() = prefs.getBoolean(KEY_STRICT_MODE_DEFAULT, true)
        set(value) = prefs.edit().putBoolean(KEY_STRICT_MODE_DEFAULT, value).apply()

    var soundFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_FEEDBACK, value).apply()

    var vibrationFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_FEEDBACK, value).apply()

    var blockNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_NOTIFICATIONS, value).apply()

    var isSessionActive: Boolean
        get() = prefs.getBoolean(KEY_IS_SESSION_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_SESSION_ACTIVE, value).apply()

    var activeSessionId: Long
        get() = prefs.getLong(KEY_ACTIVE_SESSION_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_ACTIVE_SESSION_ID, value).apply()

    var activeSessionEndTime: Long
        get() = prefs.getLong(KEY_ACTIVE_SESSION_END_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_ACTIVE_SESSION_END_TIME, value).apply()

    var activeSessionStartTime: Long
        get() = prefs.getLong(KEY_ACTIVE_SESSION_START_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_ACTIVE_SESSION_START_TIME, value).apply()

    var isStrictModeActive: Boolean
        get() = prefs.getBoolean(KEY_IS_STRICT_MODE_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_STRICT_MODE_ACTIVE, value).apply()

    var activeBlockedPackages: Set<String>
        get() = prefs.getStringSet(KEY_ACTIVE_BLOCKED_PACKAGES, emptySet()) ?: emptySet()
        set(value) {
            prefs.edit().putStringSet(KEY_ACTIVE_BLOCKED_PACKAGES, value).apply()
            ActiveFocusState.updateBlockedPackages(value)
        }

    fun clearActiveSession() {
        prefs.edit()
            .putBoolean(KEY_IS_SESSION_ACTIVE, false)
            .putLong(KEY_ACTIVE_SESSION_ID, -1L)
            .putLong(KEY_ACTIVE_SESSION_END_TIME, 0L)
            .putLong(KEY_ACTIVE_SESSION_START_TIME, 0L)
            .putBoolean(KEY_IS_STRICT_MODE_ACTIVE, false)
            .remove(KEY_ACTIVE_BLOCKED_PACKAGES)
            .apply()
        ActiveFocusState.clear()
    }

    fun setActiveSession(
        sessionId: Long,
        startTime: Long,
        endTime: Long,
        isStrictMode: Boolean,
        blockedPackages: Set<String>
    ) {
        prefs.edit()
            .putBoolean(KEY_IS_SESSION_ACTIVE, true)
            .putLong(KEY_ACTIVE_SESSION_ID, sessionId)
            .putLong(KEY_ACTIVE_SESSION_START_TIME, startTime)
            .putLong(KEY_ACTIVE_SESSION_END_TIME, endTime)
            .putBoolean(KEY_IS_STRICT_MODE_ACTIVE, isStrictMode)
            .putStringSet(KEY_ACTIVE_BLOCKED_PACKAGES, blockedPackages)
            .apply()
        ActiveFocusState.setActiveSession(
            sessionId = sessionId,
            startTime = startTime,
            endTime = endTime,
            isStrictMode = isStrictMode,
            blockedPackages = blockedPackages
        )
    }

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_STRICT_MODE_DEFAULT = "strict_mode_default"
        private const val KEY_SOUND_FEEDBACK = "sound_feedback"
        private const val KEY_VIBRATION_FEEDBACK = "vibration_feedback"
        private const val KEY_BLOCK_NOTIFICATIONS = "block_notifications"
        private const val KEY_IS_SESSION_ACTIVE = "is_session_active"
        private const val KEY_ACTIVE_SESSION_ID = "active_session_id"
        private const val KEY_ACTIVE_SESSION_START_TIME = "active_session_start_time"
        private const val KEY_ACTIVE_SESSION_END_TIME = "active_session_end_time"
        private const val KEY_IS_STRICT_MODE_ACTIVE = "is_strict_mode_active"
        private const val KEY_ACTIVE_BLOCKED_PACKAGES = "active_blocked_packages"

        @Volatile
        private var INSTANCE: SessionPreferences? = null

        fun getInstance(context: Context): SessionPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = SessionPreferences(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
