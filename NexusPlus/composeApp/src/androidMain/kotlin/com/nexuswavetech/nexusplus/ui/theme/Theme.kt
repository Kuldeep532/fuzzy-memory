package com.nexuswavetech.nexusplus.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nexuswavetech.nexusplus.core.SettingsRepository
import org.koin.compose.koinInject

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
    onPrimary            = NexusOnPrimaryLight,
    primaryContainer     = NexusPrimaryContainerLight,
    onPrimaryContainer   = NexusOnPrimaryContainerLight,
    secondary            = NexusSecondaryLight,
    onSecondary          = NexusOnSecondaryLight,
    secondaryContainer   = NexusSecondaryContainerLight,
    onSecondaryContainer = NexusOnSecondaryContainerLight,
    tertiary             = NexusTertiaryLight,
    onTertiary           = NexusOnTertiaryLight,
    tertiaryContainer    = NexusTertiaryContainerLight,
    onTertiaryContainer  = NexusOnTertiaryContainerLight,
    background           = NexusBackgroundLight,
    onBackground         = NexusOnSurfaceLight,
    surface              = NexusSurfaceLight,
    onSurface            = NexusOnSurfaceLight,
    surfaceVariant       = NexusSurfaceVariantLight,
    onSurfaceVariant     = NexusOnSurfaceVariantLight,
    surfaceContainer     = NexusSurfaceContainerLight,
    outline              = NexusOutlineLight,
    error                = NexusError,
    onError              = NexusOnError,
    errorContainer       = NexusErrorContainerLight,
    onErrorContainer     = NexusOnErrorContainerLight,
)

/**
 * Nexus Plus — unified Material 3 theme.
 *
 * Key properties:
 *  - Shape tokens:   [NexusShapes]
 *  - Typography:     [NexusTypography]
 *  - Dynamic color:  off by default (preserves Nexus brand identity).
 *  - Dark mode:      follows system setting by default; respects user override.
 *  - System bars:    MainActivity calls enableEdgeToEdge(); this SideEffect
 *                    only adjusts the icon brightness to match the theme,
 *                    with no deprecated color assignments.
 */
@Composable
fun NexusPlusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val settings: SettingsRepository = koinInject()
    val themePref = settings.theme.collectAsState(initial = SettingsRepository.THEME_SYSTEM).value
    val isDark = when (themePref) {
        SettingsRepository.THEME_DARK  -> true
        SettingsRepository.THEME_LIGHT -> false
        else -> darkTheme
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else   -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = !isDark
                isAppearanceLightNavigationBars = !isDark
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
