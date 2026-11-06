package com.example.focuslock.data.local

import com.example.focuslock.data.local.SessionPreferences

/**
 * Thread-safe, low-latency in-memory singleton tracking current active focus session state.
 * Allows FocusAccessibilityService and FocusMonitorService to query active status and blocked
 * package membership with zero disk I/O and immediate synchronization.
 */
object ActiveFocusState {
    @Volatile
    var isSessionActive: Boolean = false
        private set

    @Volatile
    var activeSessionId: Long = -1L
        private set

    @Volatile
    var activeSessionStartTime: Long = 0L
        private set

    @Volatile
    var activeSessionEndTime: Long = 0L
        private set

    @Volatile
    var isStrictModeActive: Boolean = false
        private set

    @Volatile
    private var blockedPackageSet: Set<String> = emptySet()

    fun getBlockedPackages(): Set<String> = blockedPackageSet

    @Synchronized
    fun setActiveSession(
        sessionId: Long,
        startTime: Long,
        endTime: Long,
        isStrictMode: Boolean,
        blockedPackages: Set<String>
    ) {
        activeSessionId = sessionId
        activeSessionStartTime = startTime
        activeSessionEndTime = endTime
        isStrictModeActive = isStrictMode
        blockedPackageSet = blockedPackages
        isSessionActive = true
    }

    @Synchronized
    fun updateBlockedPackages(packages: Set<String>) {
        blockedPackageSet = packages
    }

    @Synchronized
    fun clear() {
        isSessionActive = false
        activeSessionId = -1L
        activeSessionStartTime = 0L
        activeSessionEndTime = 0L
        isStrictModeActive = false
        blockedPackageSet = emptySet()
    }

    fun isBlocked(packageName: String): Boolean {
        if (!isSessionActive) return false
        if (System.currentTimeMillis() >= activeSessionEndTime) return false
        return blockedPackageSet.contains(packageName)
    }

    @Synchronized
    fun syncFromPrefs(prefs: SessionPreferences) {
        val now = System.currentTimeMillis()
        if (prefs.isSessionActive && prefs.activeSessionEndTime > now) {
            isSessionActive = true
            activeSessionId = prefs.activeSessionId
            activeSessionStartTime = prefs.activeSessionStartTime
            activeSessionEndTime = prefs.activeSessionEndTime
            isStrictModeActive = prefs.isStrictModeActive
            blockedPackageSet = prefs.activeBlockedPackages
        } else {
            clear()
        }
    }
}
