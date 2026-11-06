package com.example.focuslock.ui.screens.history

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focuslock.data.local.entity.FocusSessionEntity
import com.example.focuslock.data.model.SessionStatus
import com.example.focuslock.ui.components.StatCard
import com.example.focuslock.viewmodel.FocusUiState
import com.example.focuslock.viewmodel.MostBlockedAppStat
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    state: FocusUiState,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Insights, 1 = Sessions History
    var historyFilter by remember { mutableStateOf("All") } // "All", "Completed", "Cancelled"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "History & Insights",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "Track your productivity and distraction patterns",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Tabs Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = NavySurfaceCard,
                contentColor = PurpleLight,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PurplePrimary,
                        height = 3.dp
                    )
                },
                modifier = Modifier.padding(horizontal = 20.dp).clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Focus Insights", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_insights")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sessions (${state.sessionsHistory.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    modifier = Modifier.testTag("tab_history")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                InsightsTabContent(state = state)
            } else {
                SessionsHistoryTabContent(
                    sessions = state.sessionsHistory,
                    selectedFilter = historyFilter,
                    onFilterChange = { historyFilter = it }
                )
            }
        }
    }
}

@Composable
private fun InsightsTabContent(state: FocusUiState) {
    val totalMinutes = state.stats.totalFocusMinutes
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    val totalTimeString = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

    // Estimated distraction cost saved: Average distraction takes ~18 minutes to recover focus
    val estimatedMinutesSaved = state.stats.totalDeflectedAttempts * 18L
    val savedHours = estimatedMinutesSaved / 60
    val savedMins = estimatedMinutesSaved % 60
    val savedTimeString = if (savedHours > 0) "${savedHours}h ${savedMins}m" else "${savedMins}m"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High Level Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Focus Time",
                    value = totalTimeString,
                    icon = Icons.Default.Timer,
                    iconTint = PurplePrimary,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Time Saved",
                    value = savedTimeString,
                    icon = Icons.Default.Bolt,
                    iconTint = AccentEmerald,
                    subtitle = "by deflecting apps",
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
                    title = "Sessions Done",
                    value = "${state.stats.completedSessionsCount}",
                    icon = Icons.Default.CheckCircle,
                    iconTint = AccentCyanOrEmerald,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Distractions Stopped",
                    value = "${state.stats.totalDeflectedAttempts}",
                    icon = Icons.Default.Shield,
                    iconTint = AccentCoral,
                    subtitle = "impulses deflected",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Weekly Focus Canvas Bar Chart
        item {
            WeeklyFocusBarChartCard(weeklyStats = state.weeklyFocusStats)
        }

        // Most Frequently Blocked Apps
        item {
            MostFrequentlyBlockedCard(mostBlocked = state.mostBlockedApps)
        }

        // Strict Mode Impact Summary Card
        item {
            val totalSessions = state.sessionsHistory.size
            val strictSessions = state.sessionsHistory.count { it.isStrictMode }
            val strictPercent = if (totalSessions > 0) ((strictSessions * 100) / totalSessions) else 0

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NavySurfaceCard,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = AccentCoral,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Strict Mode Discipline",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "$strictPercent% of your sessions utilized Strict Mode protection.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { strictPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AccentCoral,
                        trackColor = NavySurfaceElevated,
                    )
                }
            }
        }
    }
}

private val AccentCyanOrEmerald = Color(0xFF38BDF8)

@Composable
private fun WeeklyFocusBarChartCard(
    weeklyStats: List<com.example.focuslock.viewmodel.DailyFocusStat>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NavySurfaceCard,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Last 7 Days Focus (Minutes)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            val maxMinutes = weeklyStats.maxOfOrNull { it.minutes }?.coerceAtLeast(60L) ?: 60L

            // Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barCount = weeklyStats.size.coerceAtLeast(1)
                    val totalWidth = size.width
                    val totalHeight = size.height - 30.dp.toPx()
                    val barWidth = 22.dp.toPx()
                    val spacing = (totalWidth - (barWidth * barCount)) / (barCount + 1)

                    weeklyStats.forEachIndexed { index, stat ->
                        val barHeight = ((stat.minutes.toFloat() / maxMinutes.toFloat()) * totalHeight)
                            .coerceAtLeast(4.dp.toPx())
                        val x = spacing + index * (barWidth + spacing)
                        val y = totalHeight - barHeight

                        // Draw bar
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(PurpleLight, PurplePrimary)
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }

                // Row of labels underneath bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (stat in weeklyStats) {
                        Text(
                            text = stat.dayLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MostFrequentlyBlockedCard(
    mostBlocked: List<MostBlockedAppStat>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NavySurfaceCard,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Most Frequently Blocked Apps",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (mostBlocked.isEmpty()) {
                Text(
                    text = "No app blocking data recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                val maxCount = mostBlocked.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (app in mostBlocked) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${app.count} sessions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PurpleLight
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { app.count.toFloat() / maxCount.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = PurplePrimary,
                                trackColor = NavySurfaceElevated,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionsHistoryTabContent(
    sessions: List<FocusSessionEntity>,
    selectedFilter: String,
    onFilterChange: (String) -> Unit
) {
    val filteredSessions = remember(sessions, selectedFilter) {
        when (selectedFilter) {
            "Completed" -> sessions.filter { it.status == SessionStatus.COMPLETED.name }
            "Cancelled" -> sessions.filter { it.status == SessionStatus.CANCELLED.name }
            else -> sessions
        }
    }

    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Completed", "Cancelled").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurpleContainer,
                            selectedLabelColor = PurpleLight,
                            containerColor = NavySurfaceElevated,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) PurplePrimary else NavyBorder
                        )
                    )
                }
            }
        }

        if (filteredSessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No focus sessions found",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Completed and recorded sessions will show here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
            }
        } else {
            items(
                items = filteredSessions,
                key = { it.id }
            ) { session ->
                SessionHistoryItemCard(session = session, dateFormatter = dateFormatter)
            }
        }
    }
}

@Composable
private fun SessionHistoryItemCard(
    session: FocusSessionEntity,
    dateFormatter: SimpleDateFormat
) {
    val isCompleted = session.status == SessionStatus.COMPLETED.name
    val statusColor = if (isCompleted) AccentEmerald else AccentCoral
    val statusText = if (isCompleted) "Completed" else "Ended Early"
    val formattedDate = dateFormatter.format(Date(session.startTimeMillis))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NavySurfaceCard,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )

                    if (session.isStrictMode) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = AccentCoral.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Strict",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = AccentCoral,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "${session.originalDurationMinutes}m focus",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (session.blocksPreventedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = PurpleLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${session.blocksPreventedCount} distraction attempts deflected",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = PurpleLight
                    )
                }
            }
        }
    }
}
