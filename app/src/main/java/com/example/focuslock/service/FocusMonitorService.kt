package com.example.focuslock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.focuslock.data.local.ActiveFocusState
import com.example.focuslock.data.local.SessionPreferences
import com.example.focuslock.data.repository.FocusRepository
import com.example.ui.BlockingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Secondary focus deflection and foreground countdown service.
 * Continuously validates foreground applications using UsageStatsManager and maintains
 * the ongoing notification for session persistence.
 */
class FocusMonitorService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private var lastUsageBlockedPackage: String? = null
    private var lastUsageBlockTimestamp: Long = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val endTime = intent?.getLongExtra(EXTRA_END_TIME, 0L) ?: 0L

        startForeground(NOTIFICATION_ID, buildForegroundNotification(endTime))
        startMonitorLoop(endTime)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startMonitorLoop(endTime: Long) {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            val prefs = SessionPreferences.getInstance(applicationContext)
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

            while (isActive) {
                val now = System.currentTimeMillis()
                val targetEndTime = if (ActiveFocusState.activeSessionEndTime > 0) {
                    ActiveFocusState.activeSessionEndTime
                } else if (endTime > 0) {
                    endTime
                } else {
                    prefs.activeSessionEndTime
                }

                if (targetEndTime in 1..now) {
                    // Session expired!
                    ActiveFocusState.clear()
                    FocusRepository.getInstance(applicationContext)
                        .completeSession(prefs.activeSessionId)
                    stopSelf()
                    break
                }

                // Update ongoing notification periodically
                if (now % 10000 < 400) {
                    val notification = buildForegroundNotification(targetEndTime)
                    val manager = getSystemService(NotificationManager::class.java)
                    manager?.notify(NOTIFICATION_ID, notification)
                }

                // Secondary monitor via UsageStatsManager
                val isSessionActive = ActiveFocusState.isSessionActive || prefs.isSessionActive
                if (usageStatsManager != null && isSessionActive) {
                    checkRecentUsage(usageStatsManager, prefs, now, targetEndTime)
                }

                // Low-latency polling interval (350ms) for timely deflection
                delay(350L)
            }
        }
    }

    private fun checkRecentUsage(
        usageStatsManager: UsageStatsManager,
        prefs: SessionPreferences,
        now: Long,
        endTime: Long
    ) {
        try {
            var currentForegroundPackage: String? = null

            // Method 1: Query events in recent window
            val events = usageStatsManager.queryEvents(now - 2000L, now)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    currentForegroundPackage = event.packageName
                }
            }

            // Method 2: Fallback query usage stats if event query was empty
            if (currentForegroundPackage == null) {
                val stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    now - 60000L,
                    now
                )
                currentForegroundPackage = stats.maxByOrNull { it.lastTimeUsed }?.packageName
            }

            if (currentForegroundPackage != null &&
                currentForegroundPackage != packageName &&
                currentForegroundPackage != "com.android.systemui" &&
                currentForegroundPackage != "com.android.settings" &&
                !currentForegroundPackage.contains("launcher")
            ) {
                val isBlocked = ActiveFocusState.isBlocked(currentForegroundPackage) ||
                        prefs.activeBlockedPackages.contains(currentForegroundPackage)

                if (isBlocked) {
                    if (currentForegroundPackage == lastUsageBlockedPackage &&
                        (now - lastUsageBlockTimestamp) < 1200L
                    ) {
                        return
                    }
                    lastUsageBlockedPackage = currentForegroundPackage
                    lastUsageBlockTimestamp = now

                    // Launch blocking screen
                    val blockingIntent = Intent(this, BlockingActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                        )
                        putExtra(BlockingActivity.EXTRA_PACKAGE_NAME, currentForegroundPackage)
                        putExtra(BlockingActivity.EXTRA_END_TIME, endTime)
                        putExtra(BlockingActivity.EXTRA_STRICT_MODE, prefs.isStrictModeActive)
                        putExtra(BlockingActivity.EXTRA_SESSION_ID, prefs.activeSessionId)
                    }

                    try {
                        startActivity(blockingIntent)
                    } catch (e: Exception) {
                        // If background start is restricted without overlay permission, show high priority full-screen alert
                        showFullScreenAlertNotification(currentForegroundPackage, blockingIntent)
                    }

                    serviceScope.launch {
                        FocusRepository.getInstance(applicationContext)
                            .recordBlockInterception(prefs.activeSessionId)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun showFullScreenAlertNotification(blockedPackage: String, fullScreenIntent: Intent) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val pendingIntent = PendingIntent.getActivity(
            this,
            blockedPackage.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FocusLock Shield")
            .setContentText("Application locked: tap to return to focus")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ALERT_ID, notification)
    }

    private fun buildForegroundNotification(endTime: Long): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remainingMillis = (endTime - System.currentTimeMillis()).coerceAtLeast(0L)
        val minutes = (remainingMillis / (1000 * 60)) % 60
        val hours = remainingMillis / (1000 * 60 * 60)

        val timeString = if (hours > 0) {
            String.format("%dh %02dm remaining", hours, minutes)
        } else {
            String.format("%d min remaining", (remainingMillis / (1000 * 60)).coerceAtLeast(1L))
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.session_active_notification_title))
            .setContentText("FocusLock active — $timeString")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "focuslock_foreground_channel"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_ALERT_ID = 1002
        const val EXTRA_END_TIME = "extra_end_time"
        const val EXTRA_DURATION_MIN = "extra_duration_min"

        fun start(context: Context, endTime: Long, durationMinutes: Int) {
            val intent = Intent(context, FocusMonitorService::class.java).apply {
                putExtra(EXTRA_END_TIME, endTime)
                putExtra(EXTRA_DURATION_MIN, durationMinutes)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FocusMonitorService::class.java)
            context.stopService(intent)
        }
    }
}
