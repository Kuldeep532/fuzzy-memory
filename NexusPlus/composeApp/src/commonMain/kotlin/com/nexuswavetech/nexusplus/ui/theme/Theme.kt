package com.nexuswavetech.nexusplus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.nexuswavetech.nexusplus.core.SettingsRepository
import org.koin.compose.koinInject

private val DarkColorScheme = androidx.compose.material3.darkColorScheme(
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

private val LightColorScheme = androidx.compose.material3.lightColorScheme(
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
 * Nexus Plus — unified Material 3 theme for all platforms.
 *
 * Key properties:
 *  - Shape tokens:   [NexusShapes]
 *  - Typography:     [NexusTypography]
 *  - Dynamic color:  Android-only (Material You); ignored on iOS/Desktop.
 *  - Dark mode:      follows system setting by default; respects user override.
 *  - System bars:    platform-specific actual handles status-bar icon brightness.
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

    val colorScheme = resolveColorScheme(isDark, dynamicColor)

    PlatformSystemBarsEffect(isDark)

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = NexusTypography,
        shapes      = NexusShapes,
        content     = content,
    )
}

/** Platform-specific color scheme resolver. Android uses Material You dynamic colors. */
@Composable
expect fun resolveColorScheme(isDark: Boolean, dynamicColor: Boolean): ColorScheme

/** Platform-specific system bar effect. Android adjusts status/navigation bar icons. */
@Composable
expect fun PlatformSystemBarsEffect(isDark: Boolean)
