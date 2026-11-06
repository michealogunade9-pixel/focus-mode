package com.example.focuslock.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.focuslock.viewmodel.PermissionState
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

/**
 * Google Play Policy compliant prominent in-app disclosure and consent dialog for
 * Accessibility Service and Usage Access APIs.
 *
 * Explains clearly:
 * 1. FocusLock is a focus & productivity tool, NOT an assistive disability tool.
 * 2. Why Accessibility is required: to detect when user-selected blocked apps (like Chrome) are opened during a focus session.
 * 3. Zero data collection: 100% on-device processing, no personal data/keystrokes/content read or transmitted.
 */
@Composable
fun PermissionDisclosureDialog(
    permissions: PermissionState,
    onDismiss: () -> Unit,
    onProceed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasPrimaryPermission = permissions.hasAccessibilityPermission || permissions.hasUsageStatsPermission

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, NavyBorder, RoundedCornerShape(24.dp)),
            color = NavyBackground,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Shield Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(PurpleContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = PurpleLight,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Enable App Protection",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Required to deflect blocked applications",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Prominent Policy Disclosure Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NavySurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = PurpleLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Prominent Disclosure & Policy Notice",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = PurpleLight
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "• Focus & Productivity Tool: FocusLock is designed strictly as a self-control and focus tool, not as an assistive technology tool for disabilities.\n\n" +
                                    "• Why We Need Permission: FocusLock utilizes Android's Accessibility Service and Usage Access APIs solely to detect when user-selected blocked applications (e.g. Chrome) are opened during an active session, in order to display the focus shield.\n\n" +
                                    "• 100% Private & On-Device: FocusLock never collects, stores, or transmits personal data, keystrokes, messages, or screen content. All detection happens exclusively on your device.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.5.sp),
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Permission Item 1: Accessibility Service (Primary)
                PermissionSetupCard(
                    title = "1. Accessibility Service (Recommended)",
                    subtitle = "Instantly detects when a blocked app opens and deflects it.",
                    instructions = "Tap below -> Under 'Downloaded apps', tap FocusLock -> Toggle ON",
                    isGranted = permissions.hasAccessibilityPermission,
                    isRequired = true,
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    testTag = "dialog_accessibility_btn"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Permission Item 2: Usage Access (Secondary Monitor)
                PermissionSetupCard(
                    title = "2. Usage Access (Backup Monitor)",
                    subtitle = "Allows background verification of running apps.",
                    instructions = "Tap below -> Find FocusLock -> Enable 'Permit usage access'",
                    isGranted = permissions.hasUsageStatsPermission,
                    isRequired = false,
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    testTag = "dialog_usage_btn"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Permission Item 3: Display Over Other Apps (Overlay)
                PermissionSetupCard(
                    title = "3. Display Over Other Apps (Overlay)",
                    subtitle = "Ensures full-screen shield appears over foreground apps.",
                    instructions = "Tap below -> Enable 'Allow display over other apps'",
                    isGranted = permissions.hasOverlayPermission,
                    isRequired = false,
                    onOpenSettings = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            ).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    },
                    testTag = "dialog_overlay_btn"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Permission Item 4: Notification Shield (Silence Blocked Apps)
                PermissionSetupCard(
                    title = "4. Notification Shield (Optional)",
                    subtitle = "Silences incoming banners & sounds from shielded apps.",
                    instructions = "Tap below -> Find FocusLock -> Allow Notification Access",
                    isGranted = permissions.hasNotificationListenerPermission,
                    isRequired = false,
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    testTag = "dialog_notification_listener_btn"
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Button(
                    onClick = onProceed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("dialog_confirm_start_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasPrimaryPermission) PurplePrimary else Color(0xFF6B46C1)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (hasPrimaryPermission) "Continue to Focus Session" else "I've Enabled It • Start Session",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("dialog_cancel_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
                ) {
                    Text("Not Now / Cancel", color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun PermissionSetupCard(
    title: String,
    subtitle: String,
    instructions: String,
    isGranted: Boolean,
    isRequired: Boolean,
    onOpenSettings: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenSettings() },
        color = NavySurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) AccentEmerald.copy(alpha = 0.5f) else NavyBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = if (isGranted) AccentEmerald.copy(alpha = 0.15f) else AccentAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isGranted) AccentEmerald else AccentAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isGranted) "Enabled" else if (isRequired) "Required" else "Optional",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            ),
                            color = if (isGranted) AccentEmerald else AccentAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = TextSecondary
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "👉 $instructions",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = PurpleLight)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isGranted) "Settings Configured" else "Open Android Settings",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isGranted) AccentEmerald else PurpleLight
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (isGranted) AccentEmerald else PurpleLight,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
