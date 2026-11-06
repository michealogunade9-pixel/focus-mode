package com.example.focuslock.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.focuslock.data.local.FocusDatabase
import com.example.focuslock.data.local.SessionPreferences
import com.example.focuslock.data.local.entity.FocusScheduleEntity
import com.example.focuslock.data.repository.FocusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        if (scheduleId == -1L) return

        val repo = FocusRepository.getInstance(context)
        val db = FocusDatabase.getInstance(context)

        CoroutineScope(Dispatchers.IO).launch {
            val schedules = db.focusScheduleDao().getEnabledSchedulesSync()
            val schedule = schedules.find { it.id == scheduleId } ?: return@launch

            if (!schedule.isEnabled) return@launch

            val now = Calendar.getInstance()
            val dayOfWeek = now.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon... 7=Sat
            val dayBit = 1 shl (dayOfWeek - 1)

            if ((schedule.daysOfWeekMask and dayBit) != 0) {
                // Today matches! Check duration
                val startMins = schedule.startHour * 60 + schedule.startMinute
                val endMins = schedule.endHour * 60 + schedule.endMinute
                val durationMins = if (endMins > startMins) endMins - startMins else (1440 - startMins + endMins)

                val targetPackages = if (schedule.targetPackagesJson.isNotBlank()) {
                    schedule.targetPackagesJson.split(",").filter { it.isNotBlank() }
                } else null

                repo.startSession(
                    durationMinutes = durationMins.coerceAtLeast(15),
                    isStrictMode = schedule.isStrictMode,
                    overrideBlockedPackages = targetPackages
                )
            }

            // Reschedule next occurrence
            rescheduleNextOccurrence(context, schedule)
        }
    }

    companion object {
        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"

        fun scheduleAlarm(context: Context, schedule: FocusScheduleEntity) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
                putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, (10000 + schedule.id).toInt(), intent, flags)

            val nextTrigger = computeNextTriggerMillis(schedule.startHour, schedule.startMinute)

            AlarmSchedulerHelper.scheduleAlarmSafely(
                alarmManager = alarmManager,
                type = AlarmManager.RTC_WAKEUP,
                triggerAtMillis = nextTrigger,
                operation = pendingIntent
            )
        }

        fun cancelAlarm(context: Context, scheduleId: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ScheduleAlarmReceiver::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, (10000 + scheduleId).toInt(), intent, flags)
            alarmManager.cancel(pendingIntent)
        }

        private fun rescheduleNextOccurrence(context: Context, schedule: FocusScheduleEntity) {
            scheduleAlarm(context, schedule)
        }

        private fun computeNextTriggerMillis(hour: Int, minute: Int): Long {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }
    }
}
