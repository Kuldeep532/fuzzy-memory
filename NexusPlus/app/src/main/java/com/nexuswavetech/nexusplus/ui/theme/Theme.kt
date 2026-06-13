package com.nexuswavetech.nexusplus.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary              = NexusPrimary,
    onPrimary            = NexusOnPrimary,
    primaryContainer     = NexusPrimaryContainer,
    onPrimaryContainer   = NexusOnPrimaryContainer,
    secondary            = NexusSecondary,
    onSecondary          = NexusOnSecondary,
    secondaryContainer   = NexusSecondaryContainer,
    onSecondaryContainer = NexusOnSecondaryContainer,
    tertiary             = NexusTertiary,
    onTertiary           = NexusOnTertiary,
    background           = NexusBackground,
    onBackground         = NexusOnBackground,
    surface              = NexusSurface,
    onSurface            = NexusOnSurface,
    surfaceVariant       = NexusSurfaceVariant,
    onSurfaceVariant     = NexusOnSurfaceVariant,
    surfaceContainer     = NexusSurfaceContainer,
    outline              = NexusOutline,
    error                = NexusError,
    onError              = NexusOnError,
    errorContainer       = NexusErrorContainer,
    onErrorContainer     = NexusOnErrorContainer,
)

private val LightColorScheme = lightColorScheme(
    primary              = NexusPrimaryLight,
    onPrimary            = NexusOnPrimary,
    primaryContainer     = NexusPrimaryContainer,
    onPrimaryContainer   = NexusOnPrimaryContainer,
    secondary            = NexusSecondary,
    onSecondary          = NexusOnSecondary,
    secondaryContainer   = NexusSecondaryContainer,
    onSecondaryContainer = NexusOnSecondaryContainer,
    tertiary             = NexusTertiary,
    onTertiary           = NexusOnTertiary,
    background           = NexusBackgroundLight,
    onBackground         = NexusOnSurfaceLight,
    surface              = NexusSurfaceLight,
    onSurface            = NexusOnSurfaceLight,
    surfaceVariant       = Color(0xFFE8E8FF),
    onSurfaceVariant     = Color(0xFF4A4A6A),
    surfaceContainer     = Color(0xFFF0F0FF),
    outline              = Color(0xFF9090BB),
    error                = NexusError,
    onError              = NexusOnError,
    errorContainer       = Color(0xFFFFCDD2),
    onErrorContainer     = Color(0xFF3D1A1A),
)

/**
 * Nexus Plus — unified Material 3 theme.
 *
 * Key properties:
 *  - Shape tokens:   [NexusShapes]
 *  - Typography:     [NexusTypography]
 *  - Dynamic color:  off by default (preserves Nexus brand identity)
 *                    expose a Settings toggle to set dynamicColor = true at runtime.
 *  - Dark mode:      follows system — no manual toggle needed.
 *  - System bars:    transparent + icon brightness matched to theme.
 *                    SideEffect ensures the assignment runs after composition,
 *                    before the frame is drawn — eliminating status-bar flicker
 *                    on theme switch.
 */
@Composable
fun NexusPlusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    // Flicker-free system bar sync — runs imperatively after each recomposition.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor     = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = NexusTypography,
        shapes      = NexusShapes,
        content     = content,
    )
}
