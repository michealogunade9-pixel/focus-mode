package com.example.focuslock.ui.screens.active

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focuslock.data.repository.AppManager
import com.example.focuslock.ui.components.FocusCircularTimer
import com.example.focuslock.ui.components.QuoteCard
import com.example.focuslock.viewmodel.FocusUiState
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurfaceCard
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.PurpleLight
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ActiveSessionScreen(
    state: FocusUiState,
    onBack: () -> Unit,
    onEndSessionEarly: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appManager = remember { AppManager(context) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showStrictLockedDialog by remember { mutableStateOf(false) }

    val active = state.activeSession
    val isStrictMode = active?.isStrictMode == true
    val originalMinutes = active?.originalDurationMinutes ?: 30
    val deflectionsCount = active?.blocksPreventedCount ?: 0

    val blockedPackageNames = remember(active?.blockedPackageNamesJson) {
        active?.blockedPackageNamesJson?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top Navigation Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("active_session_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    Surface(
                        color = if (isStrictMode) AccentCoral.copy(alpha = 0.2f) else NavySurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isStrictMode) AccentCoral else NavyBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isStrictMode) Icons.Default.Lock else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isStrictMode) AccentCoral else PurpleLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isStrictMode) "Strict Mode Active" else "Shield Active",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isStrictMode) AccentCoral else PurpleLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            // Big Glowing Circular Timer
            item {
                Spacer(modifier = Modifier.height(10.dp))
                FocusCircularTimer(
                    remainingMillis = state.remainingMillis,
                    totalDurationMinutes = originalMinutes,
                    isStrictMode = isStrictMode,
                    size = 260.dp
                )
            }

            // Deflections Counter Pill
            item {
                Surface(
                    color = NavySurfaceCard,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = AccentEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (deflectionsCount > 0) {
                                "$deflectionsCount distraction attempt${if (deflectionsCount > 1) "s" else ""} intercepted"
                            } else {
                                "Focus shield active • Distractions blocked"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                    }
                }
            }

            // Blocked Apps in this Session
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Shielded Applications (${blockedPackageNames.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (blockedPackageNames.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = NavySurfaceCard,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "All configured focus apps are locked",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (pkg in blockedPackageNames) {
                                val appName = appManager.getAppName(pkg)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = NavySurfaceCard,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = appName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary
                                        )

                                        Surface(
                                            color = AccentCoral.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = AccentCoral,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Blocked",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    color = AccentCoral
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Mindful Quote
            item {
                QuoteCard()
            }

            // Cancellation Action Button (subject to Strict Mode)
            item {
                Spacer(modifier = Modifier.height(10.dp))
                if (isStrictMode) {
                    // Strict Mode: Stop Lock / End Session button is removed / disabled
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("strict_locked_button"),
                        color = NavySurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = AccentCoral,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Strict Mode Active • Cancellation Locked",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFFFDA4AF)
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("end_session_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AccentCoral
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            AccentCoral.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "End Session Early",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Normal Mode Cancellation
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Text(
                    text = "End Focus Session?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "You still have time remaining on your focus timer. Giving up now will interrupt your streak.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onEndSessionEarly()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCoral),
                    modifier = Modifier.testTag("confirm_end_session_button")
                ) {
                    Text("End Session", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelDialog = false },
                    modifier = Modifier.testTag("dismiss_end_session_button")
                ) {
                    Text("Keep Focusing", color = PurpleLight)
                }
            },
            containerColor = NavySurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Strict Mode Locked Notice Dialog
    if (showStrictLockedDialog) {
        AlertDialog(
            onDismissRequest = { showStrictLockedDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AccentCoral,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Strict Mode Protection",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AccentCoral
                    )
                }
            },
            text = {
                Text(
                    text = "You started this session with Strict Mode enabled. Ordinary cancellation through the FocusLock interface is disabled to protect you against impulse distractions.\n\nYour apps will unlock automatically when the timer reaches zero.",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showStrictLockedDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Text("I Understand • Back to Work", color = Color.White)
                }
            },
            containerColor = NavySurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
