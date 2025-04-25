package com.example.smartparkingsystem.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.ui.graphics.toArgb

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    secondary = TealSecondary,
    tertiary = Color(0xFFFF8A65),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = OnPrimary,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    secondary = TealSecondary,
    tertiary = Color(0xFFFF7043),
    background = BackgroundColor,
    surface = SurfaceColor,
    onPrimary = OnPrimary,
    onSecondary = Color.White,
    onBackground = OnBackground,
    onSurface = Color.Black
)

private val AppTypography = Typography(
    displayLarge = Typography().displayLarge,
    titleLarge = Typography().titleLarge,
    bodyLarge = Typography().bodyLarge,
    labelLarge = Typography().labelLarge
)

@Composable
fun SmartParkingSystemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.statusBarColor = colorScheme.primary.toArgb()
            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
