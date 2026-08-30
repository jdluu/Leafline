package com.jdluu.leafline.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme

/**
 * App-level theme selection mode. This controls the Leafline *app UI* color
 * scheme (navigation bars, settings sheets, etc.) independently from the
 * Readium EPUB rendering theme.
 */
enum class ThemeMode {
    /** Light color scheme (always). */
    LIGHT,
    /** Standard dark gray color scheme. */
    DARK,
    /** Pure-black OLED-optimized dark surface. */
    OLED,
    /** High-contrast monochrome palette for e-ink displays. */
    E_INK,
    /** Follow the system dark/light setting. */
    SYSTEM
}

// -- Light scheme --
private val LightColorScheme = lightColorScheme(
    primary = LeaflineColors.LightPrimary,
    onPrimary = LeaflineColors.LightOnPrimary,
    primaryContainer = LeaflineColors.LightPrimaryContainer,
    onPrimaryContainer = LeaflineColors.LightOnPrimaryContainer,
    secondary = LeaflineColors.LightSecondary,
    onSecondary = LeaflineColors.LightOnSecondary,
    secondaryContainer = LeaflineColors.LightSecondaryContainer,
    onSecondaryContainer = LeaflineColors.LightOnSecondaryContainer,
    tertiary = LeaflineColors.LightTertiary,
    onTertiary = LeaflineColors.LightOnTertiary,
    tertiaryContainer = LeaflineColors.LightTertiaryContainer,
    onTertiaryContainer = LeaflineColors.LightOnTertiaryContainer,
    error = LeaflineColors.LightError,
    onError = LeaflineColors.LightOnError,
    errorContainer = LeaflineColors.LightErrorContainer,
    onErrorContainer = LeaflineColors.LightOnErrorContainer,
    background = LeaflineColors.LightBackground,
    onBackground = LeaflineColors.LightOnBackground,
    surface = LeaflineColors.LightSurface,
    onSurface = LeaflineColors.LightOnSurface,
    surfaceVariant = LeaflineColors.LightSurfaceVariant,
    onSurfaceVariant = LeaflineColors.LightOnSurfaceVariant,
    outline = LeaflineColors.LightOutline,
    outlineVariant = LeaflineColors.LightOutlineVariant
)

// -- Standard dark scheme --
private val DarkColorScheme = darkColorScheme(
    primary = LeaflineColors.DarkPrimary,
    onPrimary = LeaflineColors.DarkOnPrimary,
    primaryContainer = LeaflineColors.DarkPrimaryContainer,
    onPrimaryContainer = LeaflineColors.DarkOnPrimaryContainer,
    secondary = LeaflineColors.DarkSecondary,
    onSecondary = LeaflineColors.DarkOnSecondary,
    secondaryContainer = LeaflineColors.DarkSecondaryContainer,
    onSecondaryContainer = LeaflineColors.DarkOnSecondaryContainer,
    tertiary = LeaflineColors.DarkTertiary,
    onTertiary = LeaflineColors.DarkOnTertiary,
    tertiaryContainer = LeaflineColors.DarkTertiaryContainer,
    onTertiaryContainer = LeaflineColors.DarkOnTertiaryContainer,
    error = LeaflineColors.DarkError,
    onError = LeaflineColors.DarkOnError,
    errorContainer = LeaflineColors.DarkErrorContainer,
    onErrorContainer = LeaflineColors.DarkOnErrorContainer,
    background = LeaflineColors.DarkBackground,
    onBackground = LeaflineColors.DarkOnBackground,
    surface = LeaflineColors.DarkSurface,
    onSurface = LeaflineColors.DarkOnSurface,
    surfaceVariant = LeaflineColors.DarkSurfaceVariant,
    onSurfaceVariant = LeaflineColors.DarkOnSurfaceVariant,
    outline = LeaflineColors.DarkOutline,
    outlineVariant = LeaflineColors.DarkOutlineVariant
)

// -- OLED (pure black) scheme -- same as dark but with true black surfaces
private val OledColorScheme = darkColorScheme(
    primary = LeaflineColors.DarkPrimary,
    onPrimary = LeaflineColors.DarkOnPrimary,
    primaryContainer = LeaflineColors.DarkPrimaryContainer,
    onPrimaryContainer = LeaflineColors.DarkOnPrimaryContainer,
    secondary = LeaflineColors.DarkSecondary,
    onSecondary = LeaflineColors.DarkOnSecondary,
    secondaryContainer = LeaflineColors.DarkSecondaryContainer,
    onSecondaryContainer = LeaflineColors.DarkOnSecondaryContainer,
    tertiary = LeaflineColors.DarkTertiary,
    onTertiary = LeaflineColors.DarkOnTertiary,
    tertiaryContainer = LeaflineColors.DarkTertiaryContainer,
    onTertiaryContainer = LeaflineColors.DarkOnTertiaryContainer,
    error = LeaflineColors.DarkError,
    onError = LeaflineColors.DarkOnError,
    errorContainer = LeaflineColors.DarkErrorContainer,
    onErrorContainer = LeaflineColors.DarkOnErrorContainer,
    background = LeaflineColors.OledBackground,
    onBackground = LeaflineColors.OledOnBackground,
    surface = LeaflineColors.OledSurface,
    onSurface = LeaflineColors.OledOnSurface,
    surfaceVariant = LeaflineColors.OledSurfaceVariant,
    onSurfaceVariant = LeaflineColors.OledOnSurfaceVariant,
    outline = LeaflineColors.DarkOutline,
    outlineVariant = LeaflineColors.DarkOutlineVariant
)

// -- E-ink high-contrast scheme (light mode, monochrome) --
private val EinkColorScheme = lightColorScheme(
    primary = LeaflineColors.EinkPrimary,
    onPrimary = LeaflineColors.EinkOnPrimary,
    primaryContainer = LeaflineColors.EinkPrimaryContainer,
    onPrimaryContainer = LeaflineColors.EinkOnPrimaryContainer,
    secondary = LeaflineColors.EinkSecondary,
    onSecondary = LeaflineColors.EinkOnSecondary,
    secondaryContainer = LeaflineColors.EinkSecondaryContainer,
    onSecondaryContainer = LeaflineColors.EinkOnSecondaryContainer,
    tertiary = LeaflineColors.EinkTertiary,
    onTertiary = LeaflineColors.EinkOnTertiary,
    tertiaryContainer = LeaflineColors.EinkTertiaryContainer,
    onTertiaryContainer = LeaflineColors.EinkOnTertiaryContainer,
    error = LeaflineColors.EinkError,
    onError = LeaflineColors.EinkOnError,
    errorContainer = LeaflineColors.EinkErrorContainer,
    onErrorContainer = LeaflineColors.EinkOnErrorContainer,
    background = LeaflineColors.EinkBackground,
    onBackground = LeaflineColors.EinkOnBackground,
    surface = LeaflineColors.EinkSurface,
    onSurface = LeaflineColors.EinkOnSurface,
    surfaceVariant = LeaflineColors.EinkSurfaceVariant,
    onSurfaceVariant = LeaflineColors.EinkOnSurfaceVariant,
    outline = LeaflineColors.EinkOutline,
    outlineVariant = LeaflineColors.EinkOutlineVariant
)

/**
 * Resolves the color scheme for [mode]. [ThemeMode.SYSTEM] follows the system
 * dark/light state but always uses the fixed Leafline light and dark schemes;
 * the brand palette is never replaced by dynamic wallpaper colors. The other
 * modes map to their own fixed schemes regardless of the system setting.
 */
internal fun themeScheme(mode: ThemeMode, darkSystem: Boolean): ColorScheme = when (mode) {
    ThemeMode.LIGHT -> LightColorScheme
    ThemeMode.DARK -> DarkColorScheme
    ThemeMode.OLED -> OledColorScheme
    ThemeMode.E_INK -> EinkColorScheme
    ThemeMode.SYSTEM -> if (darkSystem) DarkColorScheme else LightColorScheme
}

/**
 * Top-level Leafline theme composable. Wraps MaterialTheme with the brand
 * color schemes, typography, and shapes.
 *
 * @param mode The [ThemeMode] to apply. [ThemeMode.SYSTEM] follows the system
 *   dark/light state with the fixed Leafline schemes.
 * @param darkSystem Whether the system is in dark mode (only used when
 *   [mode] is [ThemeMode.SYSTEM]).
 * @param content The composable content tree.
 */
@Composable
fun LeaflineTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    darkSystem: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = themeScheme(mode, darkSystem),
        typography = LeaflineTypography,
        shapes = LeaflineShapes,
        content = content
    )
}