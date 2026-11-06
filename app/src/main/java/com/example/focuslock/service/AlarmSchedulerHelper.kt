package com.example.focuslock.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build

object AlarmSchedulerHelper {
    /**
     * Safely schedules an alarm, checking canScheduleExactAlarms() on Android 12+ (API 31+)
     * and gracefully falling back to setAndAllowWhileIdle() or set() to avoid SecurityException.
     */
    fun scheduleAlarmSafely(
        alarmManager: AlarmManager,
        type: Int,
        triggerAtMillis: Long,
        operation: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(type, triggerAtMillis, operation)
                } else {
                    alarmManager.setAndAllowWhileIdle(type, triggerAtMillis, operation)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(type, triggerAtMillis, operation)
            } else {
                alarmManager.setExact(type, triggerAtMillis, operation)
            }
        } catch (_: SecurityException) {
            // Fallback for Android 12+ when exact alarm permission is not held
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(type, triggerAtMillis, operation)
                } else {
                    alarmManager.set(type, triggerAtMillis, operation)
                }
            } catch (_: Exception) {}
        } catch (_: Exception) {}
    }
}
