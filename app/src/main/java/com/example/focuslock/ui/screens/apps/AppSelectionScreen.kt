package com.example.focuslock.ui.screens.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focuslock.data.model.InstalledAppInfo
import com.example.focuslock.data.repository.AppManager
import com.example.focuslock.ui.components.AppItemRow
import com.example.focuslock.viewmodel.FocusUiState
import com.example.ui.theme.AccentCoral
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

@Composable
fun AppSelectionScreen(
    state: FocusUiState,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onToggleBlocked: (InstalledAppInfo) -> Unit,
    onSelectAllFiltered: (Boolean) -> Unit,
    onRefreshApps: () -> Unit,
    onProceedToDuration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appManager = remember { AppManager(context) }
    val categories = listOf("All", "Social", "Entertainment", "Games", "Browsers", "Productivity", "Other")

    val filteredApps = remember(state.installedApps, state.searchQuery, state.selectedCategory) {
        state.installedApps.filter { app ->
            val matchesCategory = state.selectedCategory == "All" || app.category == state.selectedCategory
            val matchesQuery = state.searchQuery.isBlank() ||
                    app.appName.contains(state.searchQuery, ignoreCase = true) ||
                    app.packageName.contains(state.searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    val blockedCount = state.installedApps.count { it.isBlocked }
    val isStrictSessionActive = state.isSessionRunning && state.activeSession?.isStrictMode == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isStrictSessionActive) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    color = Color(0xFF28101E),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentCoral)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = AccentCoral,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Strict session active: Blocked apps cannot be modified until the timer finishes.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = Color(0xFFFDA4AF)
                        )
                    }
                }
            }

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("app_selection_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Select Blocked Apps",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "$blockedCount apps marked for focus",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (blockedCount > 0) PurpleLight else TextSecondary
                    )
                }

                IconButton(
                    onClick = onRefreshApps,
                    modifier = Modifier.testTag("refresh_apps_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload installed apps",
                        tint = PurpleLight
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("app_search_input"),
                placeholder = { Text("Search installed applications...", color = TextTertiary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary,
                    unfocusedBorderColor = NavyBorder,
                    focusedContainerColor = NavySurfaceCard,
                    unfocusedContainerColor = NavySurfaceCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (cat in categories) {
                    val isSelected = state.selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategoryChange(cat) },
                        label = { Text(cat, fontSize = 12.sp) },
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

            // Bulk actions row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredApps.size} apps found",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Row {
                    TextButton(
                        onClick = { onSelectAllFiltered(true) },
                        enabled = !isStrictSessionActive,
                        modifier = Modifier.testTag("select_all_button")
                    ) {
                        Text(
                            text = "Select All",
                            fontSize = 12.sp,
                            color = if (!isStrictSessionActive) PurpleLight else TextTertiary
                        )
                    }

                    TextButton(
                        onClick = { onSelectAllFiltered(false) },
                        enabled = !isStrictSessionActive,
                        modifier = Modifier.testTag("clear_all_button")
                    ) {
                        Text(
                            text = "Clear",
                            fontSize = 12.sp,
                            color = if (!isStrictSessionActive) TextSecondary else TextTertiary
                        )
                    }
                }
            }

            // Apps List or Loading State
            if (state.isLoadingApps) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PurplePrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Scanning installed applications...", color = TextSecondary)
                    }
                }
            } else if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No applications match your filter",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing the search query or selecting 'All'",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        AppItemRow(
                            app = app,
                            icon = appManager.getAppIcon(app.packageName),
                            onToggleBlocked = onToggleBlocked,
                            enabled = !isStrictSessionActive
                        )
                    }
                }
            }

            // Bottom Sticky Confirmation Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NavySurfaceCard,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$blockedCount apps blocked",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = if (isStrictSessionActive) "Locked during active session" else "Selection saved automatically",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = if (isStrictSessionActive) AccentCoral else TextSecondary
                        )
                    }

                    androidx.compose.material3.Button(
                        onClick = onProceedToDuration,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (isStrictSessionActive) PurplePrimary.copy(alpha = 0.6f) else PurplePrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("app_selection_done_button")
                    ) {
                        Text(
                            text = if (isStrictSessionActive) "View Active Shield" else "Continue to Duration",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
