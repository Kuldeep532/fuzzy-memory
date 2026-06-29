package com.nexuswavetech.nexusplus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.nexuswavetech.nexusplus.core.SettingsRepository
import org.koin.compose.koinInject

/**
 * Nexus Plus — unified Material 3 theme.
 *
 * Token files (all in this package):
 *  - [Color.kt]  — complete light and dark color palettes (NexusPrimary… / NexusPrimaryLight…)
 *  - [Type.kt]   — [NexusTypography] — full M3 type scale
 *  - [Shape.kt]  — [NexusShapes] — extraSmall … extraLarge corner tokens
 *
 * Platform-specific actuals (one per source set):
 *  - androidMain/ui/theme/Theme.kt — dynamic color (Material You), status/nav bar tint
 *  - iosMain/ui/theme/Theme.kt     — system bar effect (no dynamic color)
 *  - desktopMain/ui/theme/Theme.kt — no-op system bars
 *
 * How to reuse in another project:
 *  1. Copy this package (Color.kt, Type.kt, Shape.kt, NexusPlusTheme.kt) into your project.
 *  2. Copy the platform Theme.kt actuals into each source set.
 *  3. Adjust token values in Color.kt / Type.kt / Shape.kt as required.
 *  4. Wrap your content in NexusPlusTheme { ... } — dark mode, dynamic color and system
 *     bars are handled automatically.
 *
 * Dark mode precedence:
 *  THEME_DARK  → always dark
 *  THEME_LIGHT → always light
 *  THEME_SYSTEM (default) → follows device system setting
 */
internal val DarkColorScheme = androidx.compose.material3.darkColorScheme(
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
    tertiaryContainer    = NexusTertiaryContainer,
    onTertiaryContainer  = NexusOnTertiaryContainer,
    background           = NexusBackground,
    onBackground         = NexusOnBackground,
    surface              = NexusSurface,
    onSurface            = NexusOnSurface,
    surfaceVariant       = NexusSurfaceVariant,
    onSurfaceVariant     = NexusOnSurfaceVariant,
    surfaceContainer     = NexusSurfaceContainer,
    outline              = NexusOutline,
    outlineVariant       = NexusOutlineVariant,
    error                = NexusError,
    onError              = NexusOnError,
    errorContainer       = NexusErrorContainer,
    onErrorContainer     = NexusOnErrorContainer,
)

internal val LightColorScheme = androidx.compose.material3.lightColorScheme(
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
    outlineVariant       = NexusOutlineVariantLight,
    error                = NexusError,
    onError              = NexusOnError,
    errorContainer       = NexusErrorContainerLight,
    onErrorContainer     = NexusOnErrorContainerLight,
)

@Composable
fun NexusPlusTheme(
    darkTheme    : Boolean = isSystemInDarkTheme(),
    dynamicColor : Boolean = false,
    content      : @Composable () -> Unit,
) {
    val settings: SettingsRepository = koinInject()
    val themePref = settings.theme.collectAsState(initial = SettingsRepository.THEME_SYSTEM).value
    val isDark = when (themePref) {
        SettingsRepository.THEME_DARK  -> true
        SettingsRepository.THEME_LIGHT -> false
        else                           -> darkTheme
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

/** Platform-specific color scheme resolver. Android uses Material You dynamic colors on API 31+. */
@Composable
expect fun resolveColorScheme(isDark: Boolean, dynamicColor: Boolean): ColorScheme

/** Platform-specific system bar tinting. Android adjusts icon brightness; others are no-op. */
@Composable
expect fun PlatformSystemBarsEffect(isDark: Boolean)
