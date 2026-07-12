package com.rekaro.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ReKaroColorScheme = darkColorScheme(
    primary = GreenPrimary,
    onPrimary = TextPrimary,
    primaryContainer = GreenSurface,
    onPrimaryContainer = GreenLight,
    secondary = AccentBlue,
    onSecondary = DarkBackground,
    secondaryContainer = BlueSurface,
    onSecondaryContainer = AccentBlueLight,
    tertiary = WarningAmber,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceTint = GreenPrimary,
    error = ErrorRed,
    onError = TextPrimary,
    errorContainer = ErrorRedDark,
    onErrorContainer = ErrorRed,
    outline = TextTertiary,
    outlineVariant = DarkSurfaceElevated
)

@Composable
fun ReKaroTheme(content: @Composable () -> Unit) {
    val colorScheme = ReKaroColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ReKaroTypography,
        content = content
    )
}