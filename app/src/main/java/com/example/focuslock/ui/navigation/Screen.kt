package com.example.focuslock.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object AppSelection : Screen("app_selection")
    object DurationSelection : Screen("duration_selection")
    object ActiveSession : Screen("active_session")
    object History : Screen("history")
    object Schedules : Screen("schedules")
    object Settings : Screen("settings")
}
