package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NavySurfaceHigherOrBorder = Color(0xFF1E2946)

private val FocusLockColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    primaryContainer = PurpleContainer,
    onPrimaryContainer = PurpleOnContainer,
    secondary = PurpleLight,
    onSecondary = NavyBackground,
    secondaryContainer = NavySurfaceElevated,
    onSecondaryContainer = PurpleLight,
    tertiary = AccentCyan,
    onTertiary = NavyBackground,
    background = NavyBackground,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = NavyBorder,
    outlineVariant = NavySurfaceHigherOrBorder,
    error = AccentCoral,
    onError = Color.White,
    errorContainer = Color(0xFF38101C),
    onErrorContainer = AccentCoralLight
)

@Composable
fun FocusLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // FocusLock is an immersive dark navy experience
    MaterialTheme(
        colorScheme = FocusLockColorScheme,
        typography = Typography,
        content = content
    )
}

// Retain alias for existing references
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    FocusLockTheme(content = content)
}
