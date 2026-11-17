package com.jdluu.leafline.theme

import androidx.compose.ui.graphics.Color

/**
 * Leafline brand palette centered on the accent green-teal #315C52.
 *
 * These values are used to build the light, dark, OLED, and e-ink high-contrast
 * Material 3 color schemes; colors outside the scheme (e.g. the highlight
 * annotation tint) live in their owning packages.
 */
object LeaflineColors {

    // -- Core accent --
    /** Primary brand green-teal used across light and dark themes. */
    val GreenTeal = Color(0xFF315C52)

    // ========================================================================
    // Light theme (standard)
    // ========================================================================
    val LightPrimary = GreenTeal
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightPrimaryContainer = Color(0xFFB7DFD2)
    val LightOnPrimaryContainer = Color(0xFF002019)

    val LightSecondary = Color(0xFF4C635B)
    val LightOnSecondary = Color(0xFFFFFFFF)
    val LightSecondaryContainer = Color(0xFFCFE9DE)
    val LightOnSecondaryContainer = Color(0xFF09201A)

    val LightTertiary = Color(0xFF3D6473)
    val LightOnTertiary = Color(0xFFFFFFFF)
    val LightTertiaryContainer = Color(0xFFC1E8FA)
    val LightOnTertiaryContainer = Color(0xFF001F29)

    val LightError = Color(0xFFBA1A1A)
    val LightOnError = Color(0xFFFFFFFF)
    val LightErrorContainer = Color(0xFFFFDAD6)
    val LightOnErrorContainer = Color(0xFF410002)

    val LightBackground = Color(0xFFF9F8F4) // warm off-white (paper)
    val LightOnBackground = Color(0xFF191C1A)
    val LightSurface = Color(0xFFF9F8F4)
    val LightOnSurface = Color(0xFF191C1A)
    val LightSurfaceVariant = Color(0xFFDCE5DE)
    val LightOnSurfaceVariant = Color(0xFF414944)
    val LightOutline = Color(0xFF717974)
    val LightOutlineVariant = Color(0xFFC0C9C3)

    // Surface container ramp. Material 3 components (navigation bar, sheets,
    // dialogs, menus) read these roles; leaving them unset falls back to the
    // baseline palette, which is a lilac neutral this design does not use.
    val LightSurfaceDim = Color(0xFFDFDFD8)
    val LightSurfaceBright = Color(0xFFFCFBF7)
    val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
    val LightSurfaceContainerLow = Color(0xFFF4F4EE)
    val LightSurfaceContainer = Color(0xFFEFEFE8)
    val LightSurfaceContainerHigh = Color(0xFFE9EAE2)
    val LightSurfaceContainerHighest = Color(0xFFE4E5DC)
    val LightInverseSurface = Color(0xFF2E312E)
    val LightInverseOnSurface = Color(0xFFF1F1EB)

    // ========================================================================
    // Dark theme (standard, dark gray background)
    // ========================================================================
    val DarkPrimary = Color(0xFF9CCCC0)
    val DarkOnPrimary = Color(0xFF00382D)
    val DarkPrimaryContainer = Color(0xFF195146)
    val DarkOnPrimaryContainer = Color(0xFFB7DFD2)

    val DarkSecondary = Color(0xFFB4CCC2)
    val DarkOnSecondary = Color(0xFF1F352E)
    val DarkSecondaryContainer = Color(0xFF354C44)
    val DarkOnSecondaryContainer = Color(0xFFCFE9DE)

    val DarkTertiary = Color(0xFFA1CDDD)
    val DarkOnTertiary = Color(0xFF023544)
    val DarkTertiaryContainer = Color(0xFF224C5B)
    val DarkOnTertiaryContainer = Color(0xFFC1E8FA)

    val DarkError = Color(0xFFFFB4AB)
    val DarkOnError = Color(0xFF690005)
    val DarkErrorContainer = Color(0xFF93000A)
    val DarkOnErrorContainer = Color(0xFFFFDAD6)

    val DarkBackground = Color(0xFF191C1A)
    val DarkOnBackground = Color(0xFFE1E3DF)
    val DarkSurface = Color(0xFF191C1A)
    val DarkOnSurface = Color(0xFFE1E3DF)
    val DarkSurfaceVariant = Color(0xFF414944)
    val DarkOnSurfaceVariant = Color(0xFFC0C9C3)
    val DarkOutline = Color(0xFF8B938D)
    val DarkOutlineVariant = Color(0xFF414944)

    val DarkSurfaceDim = Color(0xFF111412)
    val DarkSurfaceBright = Color(0xFF373A38)
    val DarkSurfaceContainerLowest = Color(0xFF0C0F0D)
    val DarkSurfaceContainerLow = Color(0xFF1D201E)
    val DarkSurfaceContainer = Color(0xFF212422)
    val DarkSurfaceContainerHigh = Color(0xFF2B2E2C)
    val DarkSurfaceContainerHighest = Color(0xFF363937)
    val DarkInverseSurface = Color(0xFFE1E3DF)
    val DarkInverseOnSurface = Color(0xFF2E312E)

    // ========================================================================
    // OLED / Pure-black theme
    //
    // Identical to the standard dark theme except surface and background are
    // true black (#000000) so OLED pixels are fully off, saving battery and
    // producing deeper blacks. Non-surface tones keep their dark values.
    // ========================================================================
    /** True black for OLED panel power savings and deeper contrast. */
    private val TrueBlack = Color(0xFF000000)

    val OledBackground = TrueBlack
    val OledOnBackground = Color(0xFFE1E3DF)
    val OledSurface = TrueBlack
    val OledOnSurface = Color(0xFFE1E3DF)
    val OledSurfaceVariant = Color(0xFF292E2B)
    val OledOnSurfaceVariant = Color(0xFFC0C9C3)

    // Containers stay near black so OLED panels keep most pixels off.
    val OledSurfaceDim = TrueBlack
    val OledSurfaceBright = Color(0xFF2A2B2A)
    val OledSurfaceContainerLowest = TrueBlack
    val OledSurfaceContainerLow = Color(0xFF0C0D0C)
    val OledSurfaceContainer = Color(0xFF121312)
    val OledSurfaceContainerHigh = Color(0xFF1B1C1B)
    val OledSurfaceContainerHighest = Color(0xFF242524)
    val OledInverseSurface = Color(0xFFE1E3DF)
    val OledInverseOnSurface = Color(0xFF2E312E)

    // ========================================================================
    // E-ink high-contrast theme
    //
    // Monochrome palette for readability on e-ink displays. Uses pure black
    // text on pure white backgrounds with no color tint. Accent colors are
    // rendered as dark grays. All pairs exceed 10:1 contrast.
    // ========================================================================
    val EinkPrimary = Color(0xFF1A1A1A)
    val EinkOnPrimary = Color(0xFFFFFFFF)
    val EinkPrimaryContainer = Color(0xFFD9D9D9)
    val EinkOnPrimaryContainer = Color(0xFF000000)

    val EinkSecondary = Color(0xFF333333)
    val EinkOnSecondary = Color(0xFFFFFFFF)
    val EinkSecondaryContainer = Color(0xFFE6E6E6)
    val EinkOnSecondaryContainer = Color(0xFF000000)

    val EinkTertiary = EinkSecondary
    val EinkOnTertiary = Color(0xFFFFFFFF)
    val EinkTertiaryContainer = EinkSecondaryContainer
    val EinkOnTertiaryContainer = Color(0xFF000000)

    val EinkError = Color(0xFF1A1A1A)
    val EinkOnError = Color(0xFFFFFFFF)
    val EinkErrorContainer = Color(0xFFE6E6E6)
    val EinkOnErrorContainer = Color(0xFF000000)

    val EinkBackground = Color(0xFFFFFFFF)
    val EinkOnBackground = Color(0xFF000000)
    val EinkSurface = Color(0xFFFFFFFF)
    val EinkOnSurface = Color(0xFF000000)
    val EinkSurfaceVariant = Color(0xFFE6E6E6)
    val EinkOnSurfaceVariant = Color(0xFF333333)
    val EinkOutline = Color(0xFF666666)
    val EinkOutlineVariant = Color(0xFFCCCCCC)

    val EinkSurfaceDim = Color(0xFFE8E8E8)
    val EinkSurfaceBright = Color(0xFFFFFFFF)
    val EinkSurfaceContainerLowest = Color(0xFFFFFFFF)
    val EinkSurfaceContainerLow = Color(0xFFF7F7F7)
    val EinkSurfaceContainer = Color(0xFFEFEFEF)
    val EinkSurfaceContainerHigh = Color(0xFFE6E6E6)
    val EinkSurfaceContainerHighest = Color(0xFFDDDDDD)
    val EinkInverseSurface = Color(0xFF1A1A1A)
    val EinkInverseOnSurface = Color(0xFFFFFFFF)
}