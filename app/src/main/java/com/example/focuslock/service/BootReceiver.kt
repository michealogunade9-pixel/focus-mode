package com.example.focuslock.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.focuslock.data.local.FocusDatabase
import com.example.focuslock.data.local.SessionPreferences
import com.example.focuslock.data.repository.FocusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val db = FocusDatabase.getInstance(context)
        val prefs = SessionPreferences.getInstance(context)
        val repo = FocusRepository.getInstance(context)

        CoroutineScope(Dispatchers.IO).launch {
            // 1. Check for active focus session in database
            val activeSession = db.focusSessionDao().getActiveSessionSync()
            val now = System.currentTimeMillis()

            if (activeSession != null) {
                if (activeSession.endTimeMillis > now) {
                    // Session is still active!
                    // Restore using the saved end timestamp (never restarting the original duration)
                    val blockedPackages = db.blockedAppDao().getBlockedPackageNamesSync().toSet()
                    prefs.setActiveSession(
                        sessionId = activeSession.id,
                        startTime = activeSession.startTimeMillis,
                        endTime = activeSession.endTimeMillis,
                        isStrictMode = activeSession.isStrictMode,
                        blockedPackages = blockedPackages
                    )

                    val remainingMinutes = ((activeSession.endTimeMillis - now) / 60000L).toInt().coerceAtLeast(1)
                    FocusMonitorService.start(context, activeSession.endTimeMillis, remainingMinutes)

                    // Reschedule completion alarm in AlarmManager
                    rescheduleSessionAlarm(context, activeSession.id, activeSession.endTimeMillis)
                } else {
                    // Session expired while device was rebooting/turned off
                    repo.completeSession(activeSession.id)
                    prefs.clearActiveSession()
                }
            } else {
                // No active session exists - ensure blocking state is not active
                prefs.clearActiveSession()
            }

            // 2. Reschedule all enabled daily focus schedules
            val enabledSchedules = db.focusScheduleDao().getEnabledSchedulesSync()
            for (schedule in enabledSchedules) {
                ScheduleAlarmReceiver.scheduleAlarm(context, schedule)
            }
        }
    }

    private fun rescheduleSessionAlarm(context: Context, sessionId: Long, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val alarmIntent = Intent(context, SessionAlarmReceiver::class.java).apply {
            putExtra(SessionAlarmReceiver.EXTRA_SESSION_ID, sessionId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, sessionId.toInt(), alarmIntent, flags)

        AlarmSchedulerHelper.scheduleAlarmSafely(
            alarmManager = alarmManager,
            type = AlarmManager.RTC_WAKEUP,
            triggerAtMillis = triggerAtMillis,
            operation = pendingIntent
        )
    }
}
