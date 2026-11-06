package com.example.focuslock.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val originalDurationMinutes: Int,
    val status: String, // "ACTIVE", "COMPLETED", "CANCELLED"
    val isStrictMode: Boolean = false,
    val blockedPackageNamesJson: String = "",
    val blocksPreventedCount: Int = 0
)

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isCurrentlyBlocked: Boolean = true,
    val category: String = "Other",
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_schedules")
data class FocusScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeekMask: Int = 127, // Default all 7 days (1+2+4+8+16+32+64 = 127)
    val isEnabled: Boolean = true,
    val isStrictMode: Boolean = false,
    val targetPackagesJson: String = ""
)
