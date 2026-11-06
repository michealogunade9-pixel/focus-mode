package com.example.focuslock.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.focuslock.ui.components.PermissionBanner
import com.example.focuslock.ui.components.PermissionDisclosureDialog
import com.example.focuslock.ui.components.QuoteCard
import com.example.focuslock.ui.components.StatCard
import com.example.focuslock.viewmodel.FocusUiState
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurfaceCard
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurpleLight
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    state: FocusUiState,
    onNavigateToApps: () -> Unit,
    onNavigateToDuration: () -> Unit,
    onNavigateToActiveSession: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onQuickStartPreset: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingPresetMinutes by remember { mutableIntStateOf(0) }

    if (showPermissionDialog) {
        PermissionDisclosureDialog(
            permissions = state.permissions,
            onDismiss = { showPermissionDialog = false },
            onProceed = {
                showPermissionDialog = false
                if (pendingPresetMinutes > 0) {
                    onQuickStartPreset(pendingPresetMinutes)
                } else {
                    onNavigateToDuration()
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_focus_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "FocusLock",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = if (state.isSessionRunning) "Shield is active" else "Ready to focus",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.isSessionRunning) AccentEmerald else TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("dashboard_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = PurpleLight
                        )
                    }
                }
            }

            // Permissions Alert Banner if needed
            item {
                PermissionBanner(
                    permissions = state.permissions,
                    onOpenSettings = { showPermissionDialog = true }
                )
            }

            // Active Session Hero Banner OR Start Session Card
            item {
                if (state.isSessionRunning && state.activeSession != null) {
                    ActiveSessionHeroCard(
                        remainingMillis = state.remainingMillis,
                        originalMinutes = state.activeSession.originalDurationMinutes,
                        isStrictMode = state.activeSession.isStrictMode,
                        deflectionsCount = state.activeSession.blocksPreventedCount,
                        onClick = onNavigateToActiveSession
                    )
                } else {
                    StartSessionPromptCard(
                        blockedAppsCount = state.blockedApps.size,
                        onStartClick = onNavigateToDuration,
                        onQuickStart = { minutes ->
                            if (!state.permissions.hasAccessibilityPermission && !state.permissions.hasUsageStatsPermission) {
                                pendingPresetMinutes = minutes
                                showPermissionDialog = true
                            } else {
                                onQuickStartPreset(minutes)
                            }
                        }
                    )
                }
            }

            // Quick Stats Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Focus Time",
                        value = "${state.stats.totalFocusMinutes}m",
                        icon = Icons.Default.Timer,
                        iconTint = PurplePrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Deflections",
                        value = "${state.stats.totalDeflectedAttempts}",
                        icon = Icons.Default.Shield,
                        iconTint = AccentCoral,
                        subtitle = "impulses stopped",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Completed",
                        value = "${state.stats.completedSessionsCount}",
                        icon = Icons.Default.CheckCircle,
                        iconTint = AccentEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Streak",
                        value = "${state.stats.currentStreakDays} days",
                        icon = Icons.Default.Bolt,
                        iconTint = AccentAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Blocked Applications Overview Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigateToApps() }
                        .testTag("dashboard_blocked_apps_card"),
                    color = NavySurfaceCard,
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PurpleContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = PurpleLight,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Blocked Applications",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (state.blockedApps.isEmpty()) {
                                    "No apps selected yet • Tap to configure"
                                } else {
                                    "${state.blockedApps.size} apps configured for locking"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.blockedApps.isEmpty()) AccentAmber else TextSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Configure Apps",
                            tint = PurpleLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Daily Schedules Overview Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigateToSchedules() }
                        .testTag("dashboard_schedules_card"),
                    color = NavySurfaceCard,
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(NavySurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = AccentCyanOrEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Focus Schedules",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val activeSchedulesCount = state.schedules.count { it.isEnabled }
                            Text(
                                text = if (activeSchedulesCount > 0) {
                                    "$activeSchedulesCount active recurring schedules"
                                } else {
                                    "Set automatic recurring focus blocks"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Schedules",
                            tint = PurpleLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Mindful Quote
            item {
                QuoteCard()
            }
        }
    }
}

private val AccentCyanOrEmerald = Color(0xFF38BDF8)

@Composable
private fun ActiveSessionHeroCard(
    remainingMillis: Long,
    originalMinutes: Int,
    isStrictMode: Boolean,
    deflectionsCount: Int,
    onClick: () -> Unit
) {
    val hours = (remainingMillis / (1000 * 60 * 60))
    val minutes = (remainingMillis / (1000 * 60)) % 60
    val seconds = (remainingMillis / 1000) % 60

    val timeString = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .testTag("active_session_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = if (isStrictMode) {
                            listOf(Color(0xFF3F1322), Color(0xFF1E0B14))
                        } else {
                            listOf(Color(0xFF281452), Color(0xFF130F2B))
                        }
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isStrictMode) AccentCoral else AccentEmerald)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isStrictMode) "STRICT LOCK ACTIVE" else "FOCUS SHIELD ACTIVE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = if (isStrictMode) AccentCoral else PurpleLight
                        )
                    }

                    Surface(
                        color = NavySurfaceElevated.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${originalMinutes}m session",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = timeString,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (deflectionsCount > 0) {
                            "🛡️ $deflectionsCount distraction attempt${if (deflectionsCount > 1) "s" else ""} deflected"
                        } else {
                            "🛡️ Apps are actively blocked"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View Shield",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = PurpleLight
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = PurpleLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StartSessionPromptCard(
    blockedAppsCount: Int,
    onStartClick: () -> Unit,
    onQuickStart: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1B1938), Color(0xFF101226))
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                Text(
                    text = "Ready to Focus?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (blockedAppsCount == 0) {
                        "Pick distracting apps to shield, then select a duration."
                    } else {
                        "$blockedAppsCount app${if (blockedAppsCount > 1) "s" else ""} primed for distraction deflection."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Quick presets row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(15, 30, 60, 120)
                    for (mins in presets) {
                        val label = if (mins >= 60) "${mins / 60}h" else "${mins}m"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onQuickStart(mins) }
                                .testTag("quick_start_${mins}m"),
                            color = NavySurfaceElevated,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PurpleLight
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onStartClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("dashboard_start_session_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Customize & Start Session",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}
