package com.jdluu.leafline.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme

/**
 * Light Material 3 color scheme based on the Leafline brand palette.
 */
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

/**
 * Dark Material 3 color scheme based on the Leafline brand palette.
 */
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

/**
 * Top-level Leafline theme composable. Wraps MaterialTheme with the brand
 * color schemes, typography, and shapes. Replace bare `MaterialTheme { }`
 * invocations with `LeaflineTheme { }` to pick up the brand palette.
 *
 * @param darkTheme Whether to apply the dark color scheme. Defaults to
 *   [isSystemInDarkTheme] so the theme follows the system setting.
 * @param content The composable content tree.
 */
@Composable
fun LeaflineTheme(
    darkTheme: Boolean = false, // system detection deferred to caller
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LeaflineTypography,
        shapes = LeaflineShapes,
        content = content
    )
}