package com.example.focuslock.ui.screens.duration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focuslock.ui.components.PermissionDisclosureDialog
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
import com.example.ui.theme.TextTertiary

data class DurationPreset(
    val label: String,
    val durationMinutes: Int,
    val subtext: String
)

val PRESET_DURATIONS = listOf(
    DurationPreset("15 min", 15, "Quick Sprint"),
    DurationPreset("30 min", 30, "Short Focus"),
    DurationPreset("1 hour", 60, "Standard Block"),
    DurationPreset("2 hours", 120, "Deep Work"),
    DurationPreset("3 hours", 180, "Intense Study"),
    DurationPreset("6 hours", 360, "Work Session"),
    DurationPreset("12 hours", 720, "Half Day"),
    DurationPreset("24 hours", 1440, "Full Day Detox")
)

@Composable
fun DurationSelectionScreen(
    state: FocusUiState,
    onBack: () -> Unit,
    onNavigateToApps: () -> Unit,
    onStartSession: (durationMinutes: Int, isStrictMode: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMinutes by remember { mutableIntStateOf(60) }
    var isCustomSelected by remember { mutableStateOf(false) }
    var customHours by remember { mutableIntStateOf(1) }
    var customMinutes by remember { mutableIntStateOf(30) }
    var isStrictMode by remember { mutableStateOf(state.isStrictModeDefault) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val effectiveMinutes = if (isCustomSelected) {
        (customHours * 60 + customMinutes).coerceAtLeast(5)
    } else {
        selectedMinutes
    }

    if (showPermissionDialog) {
        PermissionDisclosureDialog(
            permissions = state.permissions,
            onDismiss = { showPermissionDialog = false },
            onProceed = {
                showPermissionDialog = false
                onStartSession(effectiveMinutes, isStrictMode)
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("duration_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Focus Duration",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Choose how long to lock your distractions",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Blocked Apps Status Banner
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToApps() }
                        .testTag("duration_edit_apps_banner"),
                    color = NavySurfaceCard,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = PurpleLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${state.blockedApps.size} apps selected to block",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                text = if (state.blockedApps.isEmpty()) {
                                    "Tap to choose apps before starting"
                                } else {
                                    "Tap to view or add more applications"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.blockedApps.isEmpty()) AccentAmber else TextSecondary
                            )
                        }
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = PurpleLight
                        )
                    }
                }
            }

            // Duration Presets Grid (2 columns)
            item {
                Text(
                    text = "Preset Durations",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (i in PRESET_DURATIONS.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val preset1 = PRESET_DURATIONS[i]
                            val isSelected1 = !isCustomSelected && selectedMinutes == preset1.durationMinutes
                            PresetCard(
                                preset = preset1,
                                isSelected = isSelected1,
                                onSelect = {
                                    selectedMinutes = preset1.durationMinutes
                                    isCustomSelected = false
                                },
                                modifier = Modifier.weight(1f)
                            )

                            if (i + 1 < PRESET_DURATIONS.size) {
                                val preset2 = PRESET_DURATIONS[i + 1]
                                val isSelected2 = !isCustomSelected && selectedMinutes == preset2.durationMinutes
                                PresetCard(
                                    preset = preset2,
                                    isSelected = isSelected2,
                                    onSelect = {
                                        selectedMinutes = preset2.durationMinutes
                                        isCustomSelected = false
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Custom Duration Option
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { isCustomSelected = true }
                        .border(
                            width = if (isCustomSelected) 2.dp else 1.dp,
                            color = if (isCustomSelected) PurplePrimary else NavyBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .testTag("custom_duration_card"),
                    color = if (isCustomSelected) PurpleContainer.copy(alpha = 0.35f) else NavySurfaceCard,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (isCustomSelected) PurpleLight else TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Custom Duration",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Set exact hours and minutes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            if (isCustomSelected) {
                                Surface(
                                    color = PurplePrimary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${customHours}h ${customMinutes}m",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = isCustomSelected) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                // Hours row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Hours: $customHours", color = TextPrimary)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { if (customHours > 0) customHours-- },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = PurpleLight)
                                        }
                                        Text(
                                            "$customHours",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        IconButton(
                                            onClick = { if (customHours < 24) customHours++ },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = PurpleLight)
                                        }
                                    }
                                }

                                // Minutes row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Minutes: $customMinutes", color = TextPrimary)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { if (customMinutes >= 5) customMinutes -= 5 },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = PurpleLight)
                                        }
                                        Text(
                                            "$customMinutes",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                        IconButton(
                                            onClick = { if (customMinutes < 55) customMinutes += 5 },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = PurpleLight)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Strict Mode Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    color = if (isStrictMode) Color(0xFF28101E) else NavySurfaceCard,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isStrictMode) AccentCoral else NavyBorder
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isStrictMode) AccentCoral.copy(alpha = 0.2f)
                                            else NavySurfaceElevated
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (isStrictMode) AccentCoral else TextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = "Strict Mode",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isStrictMode) AccentCoral else TextPrimary
                                    )
                                    Text(
                                        text = "Locks session until timer ends",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Switch(
                                checked = isStrictMode,
                                onCheckedChange = { isStrictMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AccentCoral,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = NavySurfaceElevated,
                                    uncheckedBorderColor = NavyBorder
                                ),
                                modifier = Modifier.testTag("strict_mode_switch")
                            )
                        }

                        if (isStrictMode) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "⚠️ With Strict Mode enabled, ordinary cancellation is blocked. FocusLock will maintain the shield until time expires.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                                color = Color(0xFFFDA4AF)
                            )
                        }
                    }
                }
            }

            // Notification Shield Status Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    color = NavySurfaceCard,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PurpleContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = PurpleLight,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Notification Shield",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = if (state.permissions.hasNotificationListenerPermission) {
                                    "Active: incoming alerts from shielded apps are silenced"
                                } else {
                                    "Tap settings if you wish to silence notification banners"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.permissions.hasNotificationListenerPermission) AccentEmerald else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Bottom Sticky Start Session Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = NavySurfaceCard,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                val isSessionRunning = state.isSessionRunning
                Button(
                    onClick = {
                        if (isSessionRunning) {
                            onBack()
                            return@Button
                        }
                        if (!state.permissions.hasAccessibilityPermission && !state.permissions.hasUsageStatsPermission) {
                            showPermissionDialog = true
                        } else {
                            onStartSession(effectiveMinutes, isStrictMode)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_focus_session_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSessionRunning) PurpleLight else if (isStrictMode) AccentCoral else PurplePrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = if (isSessionRunning) Icons.Default.Lock else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isSessionRunning) "Session Currently Active (Locked)" else "Start Focus Session (${formatMinutes(effectiveMinutes)})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: DurationPreset,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) PurplePrimary else NavyBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("preset_${preset.durationMinutes}m"),
        color = if (isSelected) PurpleContainer.copy(alpha = 0.5f) else NavySurfaceCard,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Text(
                text = preset.label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) PurpleLight else TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = preset.subtext,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextSecondary
            )
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    return if (minutes >= 60) {
        val h = minutes / 60
        val m = minutes % 60
        if (m > 0) "${h}h ${m}m" else "${h}h"
    } else {
        "${minutes}m"
    }
}
