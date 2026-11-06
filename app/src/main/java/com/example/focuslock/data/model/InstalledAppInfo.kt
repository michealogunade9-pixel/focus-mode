package com.example.focuslock.data.model

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = false,
    val category: String = "Other",
    val isSystemApp: Boolean = false
)

enum class SessionStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}

data class SessionStats(
    val totalFocusMinutes: Long,
    val completedSessionsCount: Int,
    val totalDeflectedAttempts: Int,
    val currentStreakDays: Int
)
