package com.jdluu.leafline.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing
import com.jdluu.leafline.theme.ThemeMode
import com.jdluu.leafline.theme.themeScheme

/** The four fixed schemes, in the order the app offers them. */
private val FIXED_THEME_MODES = listOf(
    ThemeMode.LIGHT,
    ThemeMode.DARK,
    ThemeMode.OLED,
    ThemeMode.E_INK
)

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
        ThemeMode.OLED -> "OLED"
        ThemeMode.E_INK -> "E-ink"
    }

private val ThemeMode.description: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "Follows the system light or dark setting using the Leafline palette."
        ThemeMode.LIGHT -> "Always uses the Leafline light scheme."
        ThemeMode.DARK -> "Always uses the Leafline dark scheme."
        ThemeMode.OLED -> "Uses the pure-black dark scheme for OLED displays."
        ThemeMode.E_INK -> "Uses the high-contrast monochrome scheme for e-ink displays."
    }

/** The one-line subtitle a fixed tile carries under its name. */
private val ThemeMode.shortSummary: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "Follows your device setting"
        ThemeMode.LIGHT -> "Leafline light"
        ThemeMode.DARK -> "Leafline dark"
        ThemeMode.OLED -> "True black surfaces"
        ThemeMode.E_INK -> "Monochrome, high contrast"
    }

/**
 * App-wide scheme choice. Every option is previewed as a miniature page drawn
 * in that scheme's own colors, so the choice is made by looking at it rather
 * than by reading five names and hoping. Following the system is a different
 * kind of choice from picking one of the four fixed schemes, so it reads as
 * its own full-width row above the 2x2 grid.
 */
@Composable
internal fun AppearanceSection(
    appearance: ThemeMode,
    onAppearanceChange: (ThemeMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionHeading("Appearance")
        // One radio group across the full-width row and the 2x2 grid, so TalkBack
        // announces a position in the set instead of five unrelated buttons.
        Column(modifier = Modifier.selectableGroup()) {
            ThemePreviewTile(
                mode = ThemeMode.SYSTEM,
                selected = appearance == ThemeMode.SYSTEM,
                onClick = { onAppearanceChange(ThemeMode.SYSTEM) },
                horizontal = true,
                modifier = Modifier.fillMaxWidth()
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.padding(top = Spacing.xs)
            ) {
                FIXED_THEME_MODES.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        pair.forEach { mode ->
                            ThemePreviewTile(
                                mode = mode,
                                selected = appearance == mode,
                                onClick = { onAppearanceChange(mode) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = "Active: ${appearance.label}. ${appearance.description}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.sm)
        )
    }
}

@Composable
private fun ThemePreviewTile(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontal: Boolean = false
) {
    // System previews the scheme applied right now; the others preview their
    // own fixed scheme.
    val preview = if (mode == ThemeMode.SYSTEM) {
        MaterialTheme.colorScheme
    } else {
        themeScheme(mode, darkSystem = false)
    }
    val tileModifier = modifier
        .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
        .semantics(mergeDescendants = true) {}
        .padding(vertical = Spacing.xs)
    if (horizontal) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = tileModifier
        ) {
            ThemePreviewCanvas(
                preview = preview,
                selected = selected,
                modifier = Modifier.width(72.dp)
            )
            Spacer(Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                ThemeTileName(mode = mode, selected = selected)
                Text(
                    text = mode.shortSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(modifier = tileModifier) {
            ThemePreviewCanvas(
                preview = preview,
                selected = selected,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.xs))
            ThemeTileName(mode = mode, selected = selected)
        }
    }
}

/** Name plus the selection mark, so selection never rests on color alone. */
@Composable
private fun ThemeTileName(mode: ThemeMode, selected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = mode.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = Spacing.xs)
                    .size(16.dp) // touch-target-ok: selection glyph inside the selectable tile
            )
        }
    }
}

/**
 * A miniature page: three text lines and one accent mark drawn in the scheme's
 * own tokens. Geometry only, no type, so a large system font scale cannot
 * distort or clip the preview.
 */
@Composable
private fun ThemePreviewCanvas(
    preview: ColorScheme,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(preview.surface)
            .border(
                border = BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                ),
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Padding.compact),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.72f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(preview.onSurface.copy(alpha = 0.85f))
            )
            Box(
                Modifier
                    .fillMaxWidth(0.9f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(preview.onSurface.copy(alpha = 0.45f))
            )
            Box(
                Modifier
                    .fillMaxWidth(0.62f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(preview.onSurface.copy(alpha = 0.45f))
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .width(24.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(preview.primary)
            )
        }
    }
}
