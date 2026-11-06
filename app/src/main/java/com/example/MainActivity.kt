package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.focuslock.ui.navigation.Screen
import com.example.focuslock.ui.screens.active.ActiveSessionScreen
import com.example.focuslock.ui.screens.apps.AppSelectionScreen
import com.example.focuslock.ui.screens.dashboard.DashboardScreen
import com.example.focuslock.ui.screens.duration.DurationSelectionScreen
import com.example.focuslock.ui.screens.history.HistoryScreen
import com.example.focuslock.ui.screens.onboarding.OnboardingScreen
import com.example.focuslock.ui.screens.schedules.SchedulesScreen
import com.example.focuslock.ui.screens.settings.SettingsScreen
import com.example.focuslock.viewmodel.FocusViewModel
import com.example.focuslock.viewmodel.FocusViewModelFactory
import com.example.ui.theme.FocusLockTheme
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurfaceCard
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurpleLight
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.TextSecondary

data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {

    private val viewModel: FocusViewModel by viewModels {
        val app = application as FocusLockApp
        FocusViewModelFactory(
            repository = app.repository,
            preferences = app.preferences,
            context = applicationContext
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FocusLockTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val navItems = listOf(
                    NavItem(Screen.Dashboard.route, "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
                    NavItem(Screen.AppSelection.route, "Apps", Icons.Filled.Apps, Icons.Outlined.Apps),
                    NavItem(Screen.Schedules.route, "Schedules", Icons.Filled.Schedule, Icons.Outlined.Schedule),
                    NavItem(Screen.History.route, "Insights", Icons.Filled.BarChart, Icons.Outlined.BarChart),
                    NavItem(Screen.Settings.route, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
                )

                val showBottomBar = currentRoute in listOf(
                    Screen.Dashboard.route,
                    Screen.AppSelection.route,
                    Screen.Schedules.route,
                    Screen.History.route,
                    Screen.Settings.route
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        AnimatedVisibility(
                            visible = showBottomBar,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            NavigationBar(
                                containerColor = NavySurfaceCard,
                                tonalElevation = 6.dp,
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .testTag("main_bottom_nav")
                            ) {
                                navItems.forEach { item ->
                                    val isSelected = currentRoute == item.route
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = PurpleLight,
                                            unselectedIconColor = TextSecondary,
                                            selectedTextColor = PurpleLight,
                                            unselectedTextColor = TextSecondary,
                                            indicatorColor = PurpleContainer
                                        ),
                                        modifier = Modifier.testTag("nav_${item.route}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    val startRoute = if (uiState.isOnboardingDone) {
                        Screen.Dashboard.route
                    } else {
                        Screen.Onboarding.route
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startRoute,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // Onboarding Screen
                        composable(Screen.Onboarding.route) {
                            OnboardingScreen(
                                onGetStarted = {
                                    viewModel.completeOnboarding()
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Dashboard Screen
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                state = uiState,
                                onNavigateToApps = { navController.navigate(Screen.AppSelection.route) },
                                onNavigateToDuration = { navController.navigate(Screen.DurationSelection.route) },
                                onNavigateToActiveSession = { navController.navigate(Screen.ActiveSession.route) },
                                onNavigateToSchedules = { navController.navigate(Screen.Schedules.route) },
                                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                                onQuickStartPreset = { minutes ->
                                    viewModel.startSession(
                                        durationMinutes = minutes,
                                        isStrictMode = uiState.isStrictModeDefault
                                    )
                                    navController.navigate(Screen.ActiveSession.route)
                                }
                            )
                        }

                        // App Selection Screen
                        composable(Screen.AppSelection.route) {
                            BackHandler {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                                }
                            }
                            AppSelectionScreen(
                                state = uiState,
                                onBack = { navController.popBackStack() },
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onCategoryChange = { viewModel.setSelectedCategory(it) },
                                onToggleBlocked = { viewModel.toggleAppBlocked(it) },
                                onSelectAllFiltered = { viewModel.selectAllFilteredApps(it) },
                                onRefreshApps = { viewModel.loadInstalledApps() },
                                onProceedToDuration = { navController.navigate(Screen.DurationSelection.route) }
                            )
                        }

                        // Duration Selection Screen
                        composable(Screen.DurationSelection.route) {
                            BackHandler { navController.popBackStack() }
                            DurationSelectionScreen(
                                state = uiState,
                                onBack = { navController.popBackStack() },
                                onNavigateToApps = { navController.navigate(Screen.AppSelection.route) },
                                onStartSession = { durationMinutes, isStrictMode ->
                                    viewModel.startSession(durationMinutes, isStrictMode)
                                    navController.navigate(Screen.ActiveSession.route) {
                                        popUpTo(Screen.Dashboard.route)
                                    }
                                }
                            )
                        }

                        // Active Session Screen
                        composable(Screen.ActiveSession.route) {
                            BackHandler {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                                }
                            }
                            ActiveSessionScreen(
                                state = uiState,
                                onBack = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                                    }
                                },
                                onEndSessionEarly = {
                                    viewModel.endSessionEarly()
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // History & Insights Screen
                        composable(Screen.History.route) {
                            BackHandler {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                                }
                            }
                            HistoryScreen(state = uiState)
                        }

                        // Daily Schedules Screen
                        composable(Screen.Schedules.route) {
                            BackHandler {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                                }
                            }
                            SchedulesScreen(
                                state = uiState,
                                onAddSchedule = { title, startH, startM, endH, endM, mask, strict, pkgs ->
                                    viewModel.addSchedule(title, startH, startM, endH, endM, mask, strict, pkgs)
                                },
                                onToggleSchedule = { viewModel.toggleSchedule(it) },
                                onDeleteSchedule = { viewModel.deleteSchedule(it) }
                            )
                        }

                        // Settings Screen
                        composable(Screen.Settings.route) {
                            BackHandler {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                                }
                            }
                            SettingsScreen(
                                state = uiState,
                                onBack = { navController.popBackStack() },
                                onToggleStrictModeDefault = { viewModel.setStrictModeDefault(it) },
                                onToggleVibration = { viewModel.setVibrationFeedback(it) },
                                onToggleBlockNotifications = { viewModel.setBlockNotifications(it) },
                                onRefreshPermissions = { viewModel.refreshPermissions() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }
}
