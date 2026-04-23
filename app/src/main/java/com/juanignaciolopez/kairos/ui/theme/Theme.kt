package com.juanignaciolopez.kairos.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = KairosAmber,
    onPrimary = KairosBlack,
    primaryContainer = KairosAmberSoft,
    onPrimaryContainer = KairosBlack,
    secondary = KairosAmber,
    onSecondary = KairosBlack,
    background = KairosBlack,
    onBackground = KairosOnDark,
    surface = KairosBlack,
    onSurface = KairosOnDark,
    surfaceVariant = KairosBlackSoft,
    onSurfaceVariant = KairosOnDark,
    outline = KairosAmber,
    error = KairosError,
    onError = KairosOnDark
)

private val LightColorScheme = lightColorScheme(
    primary = KairosAmber,
    onPrimary = KairosBlack,
    primaryContainer = KairosAmberSoft,
    onPrimaryContainer = KairosBlack,
    secondary = KairosAmber,
    onSecondary = KairosBlack,
    background = KairosBlack,
    onBackground = KairosOnDark,
    surface = KairosBlack,
    onSurface = KairosOnDark,
    surfaceVariant = KairosBlackSoft,
    onSurfaceVariant = KairosOnDark,
    outline = KairosAmber,
    error = KairosError,
    onError = KairosOnDark
)

@Composable
fun KairosTheme(
    darkTheme: Boolean = true,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val systemBarColor = colorScheme.surface.toArgb()

            window.statusBarColor = systemBarColor
            window.navigationBarColor = systemBarColor

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}