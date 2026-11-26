package com.jdluu.leafline.reader.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.reader.FONT_SIZE_DEFAULT
import com.jdluu.leafline.reader.FONT_SIZE_MAX
import com.jdluu.leafline.reader.FONT_SIZE_MIN
import com.jdluu.leafline.reader.FONT_SIZE_STEP
import com.jdluu.leafline.reader.PAGE_MARGINS_DEFAULT
import com.jdluu.leafline.reader.PAGE_MARGINS_MAX
import com.jdluu.leafline.reader.PAGE_MARGINS_MIN
import com.jdluu.leafline.reader.PAGE_MARGINS_STEP
import com.jdluu.leafline.reader.PageTurnAnimation
import com.jdluu.leafline.reader.READER_FONT_FAMILIES
import com.jdluu.leafline.reader.ReaderOrientation
import com.jdluu.leafline.reader.ReaderSettings
import com.jdluu.leafline.reader.ReaderStylePreset
import com.jdluu.leafline.reader.StyleMode
import com.jdluu.leafline.reader.TapZoneAction
import com.jdluu.leafline.reader.TapZoneLayout
import com.jdluu.leafline.reader.font.CustomFont
import com.jdluu.leafline.reader.snapFontSize
import com.jdluu.leafline.reader.snapPageMargins
import com.jdluu.leafline.reader.styleModeFor
import com.jdluu.leafline.reader.theme.ReaderTheme
import com.jdluu.leafline.reader.withStyleMode
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalReadiumApi::class)
@Composable
internal fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit,
    onDismiss: () -> Unit,
    customFonts: List<CustomFont> = emptyList(),
    onImportFont: () -> Unit = {},
    onDeleteFont: (CustomFont) -> Unit = {}
) {
    val preferences = settings.epub
    var showFontManagerDialog by remember { mutableStateOf(false) }
    // Typography knobs imply custom styles: touching them while publisher
    // styles are active switches the mode so the preview matches the choice.
    val applyCustomPreference: ((EpubPreferences) -> EpubPreferences) -> Unit = { transform ->
        val base =
            if (styleModeFor(preferences.publisherStyles) == StyleMode.PUBLISHER) {
                preferences.withStyleMode(StyleMode.CUSTOM)
            } else {
                preferences
            }
        onSettingsChange(settings.copy(epub = transform(base)))
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                "Reader Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text("Theme", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.sm)) {
                listOf(
                    ReaderTheme.LIGHT to "Light",
                    ReaderTheme.SEPIA to "Sepia",
                    ReaderTheme.DARK to "Dark",
                    ReaderTheme.EINK to "E-Ink"
                ).forEach { (theme, label) ->
                    FilterChip(
                        selected = settings.theme == theme,
                        onClick = {
                            onSettingsChange(settings.copy(theme = theme))
                        },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = Spacing.xs)
                    )
                }
            }

            Text("Presets", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = Spacing.xs, bottom = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                val activePreset = ReaderStylePreset.matchingPreset(preferences)
                ReaderStylePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = activePreset == preset,
                        onClick = {
                            applyCustomPreference { current ->
                                preset.applyTo(current)
                            }
                        },
                        label = { Text(preset.label) }
                    )
                }
            }

            Text("Font", style = MaterialTheme.typography.labelLarge)
            FlowRow(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                READER_FONT_FAMILIES.forEach { (family, label) ->
                    FilterChip(
                        selected = preferences.fontFamily == family,
                        onClick = {
                            applyCustomPreference { it.copy(fontFamily = family) }
                        },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = Spacing.xs)
                    )
                }
                customFonts.forEach { customFont ->
                    val fontFam = FontFamily(customFont.name)
                    FilterChip(
                        selected = preferences.fontFamily == fontFam,
                        onClick = {
                            applyCustomPreference { it.copy(fontFamily = fontFam) }
                        },
                        label = { Text(customFont.name) },
                        modifier = Modifier.padding(end = Spacing.xs)
                    )
                }
                AssistChip(
                    onClick = onImportFont,
                    label = { Text("Import font…") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp) // touch-target-ok: decorative icon inside chip
                        )
                    },
                    modifier = Modifier.padding(end = Spacing.xs)
                )
            }

            if (customFonts.isNotEmpty()) {
                TextButton(
                    onClick = { showFontManagerDialog = true },
                    modifier = Modifier.padding(bottom = Spacing.xs)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp) // touch-target-ok: decorative icon inside text button
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text("Manage imported fonts (${customFonts.size})")
                }
            }

            if (showFontManagerDialog) {
                AlertDialog(
                    onDismissRequest = { showFontManagerDialog = false },
                    title = { Text("Imported Fonts") },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            customFonts.forEach { font ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(font.name, style = MaterialTheme.typography.bodyLarge)
                                        Text(font.fileName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(
                                        onClick = {
                                            onDeleteFont(font)
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete ${font.name}")
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showFontManagerDialog = false }) {
                            Text("Done")
                        }
                    }
                )
            }

            Text("Text size", style = MaterialTheme.typography.labelLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp, bottom = Spacing.sm)
            ) {
                IconButton(onClick = {
                    applyCustomPreference { prefs ->
                        val current = prefs.fontSize ?: FONT_SIZE_DEFAULT
                        if (current > FONT_SIZE_MIN) {
                            prefs.copy(fontSize = snapFontSize(current - FONT_SIZE_STEP))
                        } else {
                            prefs
                        }
                    }
                }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease text size")
                }
                val fontSizeValue = preferences.fontSize ?: FONT_SIZE_DEFAULT
                Text(
                    text = "${(fontSizeValue * 100).toInt()}%",
                    modifier = Modifier
                        .padding(horizontal = Padding.compact)
                        .semantics {
                            contentDescription = "Text size: ${(fontSizeValue * 100).toInt()} percent"
                        }
                )
                IconButton(onClick = {
                    applyCustomPreference { prefs ->
                        val current = prefs.fontSize ?: FONT_SIZE_DEFAULT
                        if (current < FONT_SIZE_MAX) {
                            prefs.copy(fontSize = snapFontSize(current + FONT_SIZE_STEP))
                        } else {
                            prefs
                        }
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase text size")
                }
            }

            Text("Page margins", style = MaterialTheme.typography.labelLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp, bottom = Spacing.sm)
            ) {
                IconButton(onClick = {
                    applyCustomPreference { prefs ->
                        val current = prefs.pageMargins ?: PAGE_MARGINS_DEFAULT
                        if (current > PAGE_MARGINS_MIN) {
                            prefs.copy(
                                pageMargins = snapPageMargins(current - PAGE_MARGINS_STEP)
                            )
                        } else {
                            prefs
                        }
                    }
                }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease page margins")
                }
                Text(
                    text = "%.2f".format(preferences.pageMargins ?: PAGE_MARGINS_DEFAULT),
                    modifier = Modifier
                        .padding(horizontal = Padding.compact)
                        .semantics {
                            contentDescription = "Page margin: ${"%.2f".format(preferences.pageMargins ?: PAGE_MARGINS_DEFAULT)}"
                        }
                )
                IconButton(onClick = {
                    applyCustomPreference { prefs ->
                        val current = prefs.pageMargins ?: PAGE_MARGINS_DEFAULT
                        if (current < PAGE_MARGINS_MAX) {
                            prefs.copy(
                                pageMargins = snapPageMargins(current + PAGE_MARGINS_STEP)
                            )
                        } else {
                            prefs
                        }
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase page margins")
                }
            }

            Text("Line height", style = MaterialTheme.typography.labelLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp, bottom = Spacing.sm)
            ) {
                IconButton(onClick = {
                    applyCustomPreference { prefs ->
                        val current = prefs.lineHeight ?: 1.2
                        if (current > 1.0) {
                            prefs.copy(lineHeight = current - 0.2)
                        } else {
                            prefs
                        }
                    }
                }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease line height")
                }
                Text(
                    text = "%.1f".format(preferences.lineHeight ?: 1.2),
                    modifier = Modifier
                        .padding(horizontal = Padding.compact)
                        .semantics {
                            contentDescription = "Line height: ${"%.1f".format(preferences.lineHeight ?: 1.2)}"
                        }
                )
                IconButton(onClick = {
                    applyCustomPreference { prefs ->
                        val current = prefs.lineHeight ?: 1.2
                        if (current < 2.5) {
                            prefs.copy(lineHeight = current + 0.2)
                        } else {
                            prefs
                        }
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase line height")
                }
            }

            Text("Columns", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xs, bottom = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                val currentColumnCount = preferences.columnCount ?: ColumnCount.AUTO
                listOf(
                    ColumnCount.AUTO to "Auto",
                    ColumnCount.ONE to "1 Column",
                    ColumnCount.TWO to "2 Columns"
                ).forEach { (count, label) ->
                    FilterChip(
                        selected = currentColumnCount == count,
                        onClick = {
                            applyCustomPreference { it.copy(columnCount = count) }
                        },
                        label = { Text(label) }
                    )
                }
            }
            Text(
                text = when (preferences.columnCount ?: ColumnCount.AUTO) {
                    ColumnCount.AUTO -> "Single column in portrait, dual columns on wide screens or landscape."
                    ColumnCount.ONE -> "Single column layout across all orientations."
                    ColumnCount.TWO -> "Two-column side-by-side spread."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.sm)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = preferences.hyphens ?: true,
                        role = Role.Switch,
                        onValueChange = { checked ->
                            applyCustomPreference { it.copy(hyphens = checked) }
                        }
                    )
            ) {
                Text(
                    "Hyphenation",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = preferences.hyphens ?: true,
                    onCheckedChange = null
                )
            }
            Text(
                text = if (preferences.hyphens != false) {
                    "Words are hyphenated across line breaks to balance text justification."
                } else {
                    "Words are kept intact at line ends without hyphen breaks."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = Spacing.sm)
            )

            Text("Styles", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.padding(top = 8.dp)) {
                listOf(
                    StyleMode.PUBLISHER to "Publisher",
                    StyleMode.CUSTOM to "Custom"
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = styleModeFor(preferences.publisherStyles) == mode,
                        onClick = {
                            onSettingsChange(
                                settings.copy(epub = preferences.withStyleMode(mode))
                            )
                        },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = Spacing.xs)
                    )
                }
            }
            Text(
                text = when (styleModeFor(preferences.publisherStyles)) {
                    StyleMode.PUBLISHER -> "The publication's own typography is used."
                    StyleMode.CUSTOM -> "Your font, line height, and margins are applied."
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = Spacing.sm)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = preferences.scroll ?: false,
                        role = Role.Switch,
                        onValueChange = { checked ->
                            onSettingsChange(settings.copy(epub = preferences.copy(scroll = checked)))
                        }
                    )
            ) {
                Text(
                    "Scroll mode",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = preferences.scroll ?: false,
                    onCheckedChange = null
                )
            }
            Text(
                text = if (preferences.scroll == true) {
                    "Content scrolls continuously. Swipe up or down to turn; page-turn taps are inactive."
                } else {
                    "Content is paginated and tap zones can turn pages."
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = Spacing.sm)
            )

            Text("Tap zones", style = MaterialTheme.typography.labelLarge)
            Text(
                "Zone layout",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            FlowRow(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                TapZoneLayout.values().forEach { layout ->
                    FilterChip(
                        selected = settings.tapZoneConfig.layout == layout,
                        onClick = {
                            onSettingsChange(
                                settings.copy(
                                    tapZoneConfig = settings.tapZoneConfig.copy(layout = layout)
                                )
                            )
                        },
                        label = { Text(layout.label) },
                        modifier = Modifier
                            .padding(end = Spacing.xs)
                            .semantics { contentDescription = "Zone layout: ${layout.description}" }
                    )
                }
            }
            Text(
                settings.tapZoneConfig.layout.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.xs)
            )

            TapZoneActionRow(
                label = "Left zone",
                selected = settings.tapZoneConfig.leftZone,
                onSelect = { action ->
                    onSettingsChange(
                        settings.copy(
                            tapZoneConfig = settings.tapZoneConfig.copy(leftZone = action)
                        )
                    )
                }
            )
            TapZoneActionRow(
                label = "Center zone",
                selected = settings.tapZoneConfig.centerZone,
                onSelect = { action ->
                    onSettingsChange(
                        settings.copy(
                            tapZoneConfig = settings.tapZoneConfig.copy(centerZone = action)
                        )
                    )
                }
            )
            TapZoneActionRow(
                label = "Right zone",
                selected = settings.tapZoneConfig.rightZone,
                onSelect = { action ->
                    onSettingsChange(
                        settings.copy(
                            tapZoneConfig = settings.tapZoneConfig.copy(rightZone = action)
                        )
                    )
                }
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            Text("Screen orientation", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.sm)) {
                listOf(
                    ReaderOrientation.AUTO to "Auto",
                    ReaderOrientation.PORTRAIT to "Portrait",
                    ReaderOrientation.LANDSCAPE to "Landscape"
                ).forEach { (orient, label) ->
                    FilterChip(
                        selected = settings.orientation == orient,
                        onClick = { onSettingsChange(settings.copy(orientation = orient)) },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = Spacing.xs)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))
            Text("Page turn animation", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.sm)) {
                listOf(
                    PageTurnAnimation.SLIDE to "Slide",
                    PageTurnAnimation.NONE to "None"
                ).forEach { (animation, label) ->
                    FilterChip(
                        selected = settings.pageTurnAnimation == animation,
                        onClick = { onSettingsChange(settings.copy(pageTurnAnimation = animation)) },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = Spacing.xs)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = settings.reduceMotion,
                        role = Role.Switch,
                        onValueChange = { reduce ->
                            onSettingsChange(settings.copy(reduceMotion = reduce))
                        }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Reduce motion",
                    style = MaterialTheme.typography.labelLarge
                )
                Switch(
                    checked = settings.reduceMotion,
                    onCheckedChange = null
                )
            }
            Text(
                "Disable page-turn animations and sheet transitions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = settings.previewFootnotes,
                        role = Role.Switch,
                        onValueChange = { preview ->
                            onSettingsChange(settings.copy(previewFootnotes = preview))
                        }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Preview footnotes & notes",
                    style = MaterialTheme.typography.labelLarge
                )
                Switch(
                    checked = settings.previewFootnotes,
                    onCheckedChange = null
                )
            }
            Text(
                "Display a popup preview when tapping footnotes instead of jumping to the end of the chapter",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .toggleable(
                        value = settings.keepScreenOn,
                        role = Role.Switch,
                        onValueChange = { keepOn ->
                            onSettingsChange(settings.copy(keepScreenOn = keepOn))
                        }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Keep screen on",
                    style = MaterialTheme.typography.labelLarge
                )
                Switch(
                    checked = settings.keepScreenOn,
                    onCheckedChange = null
                )
            }
            Text(
                "Prevent screen from timing out while reading",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .toggleable(
                        value = settings.showMinimalHud,
                        role = Role.Switch,
                        onValueChange = { showHud ->
                            onSettingsChange(settings.copy(showMinimalHud = showHud))
                        }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Minimal reading HUD",
                    style = MaterialTheme.typography.labelLarge
                )
                Switch(
                    checked = settings.showMinimalHud,
                    onCheckedChange = null
                )
            }
            Text(
                "Show chapter and progress subtly when menus are hidden",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .toggleable(
                        value = settings.hapticFeedback,
                        role = Role.Switch,
                        onValueChange = { haptic ->
                            onSettingsChange(settings.copy(hapticFeedback = haptic))
                        }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Haptic feedback",
                    style = MaterialTheme.typography.labelLarge
                )
                Switch(
                    checked = settings.hapticFeedback,
                    onCheckedChange = null
                )
            }
            Text(
                "Subtle vibration when turning pages via edge taps or volume keys",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .toggleable(
                        value = settings.volumeKeyNavigation,
                        role = Role.Switch,
                        onValueChange = { enabled ->
                            onSettingsChange(settings.copy(volumeKeyNavigation = enabled))
                        }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Volume key navigation",
                    style = MaterialTheme.typography.labelLarge
                )
                Switch(
                    checked = settings.volumeKeyNavigation,
                    onCheckedChange = null
                )
            }
            Text(
                "Turn pages using physical volume buttons when not scrolling",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (settings.volumeKeyNavigation) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .toggleable(
                            value = settings.invertVolumeKeys,
                            role = Role.Switch,
                            onValueChange = { inverted ->
                                onSettingsChange(settings.copy(invertVolumeKeys = inverted))
                            }
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Invert volume buttons",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Switch(
                        checked = settings.invertVolumeKeys,
                        onCheckedChange = null
                    )
                }
                Text(
                    "Volume Up turns forward, Volume Down turns back",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}

/** Offered actions for a configurable tap zone in the settings sheet. */
internal val TAP_ZONE_ACTION_OPTIONS: List<Pair<TapZoneAction, String>> = listOf(
    TapZoneAction.PREVIOUS_PAGE to "Previous page",
    TapZoneAction.NEXT_PAGE to "Next page",
    TapZoneAction.TOGGLE_MENU to "Toggle menu",
    TapZoneAction.NONE to "None"
)

@Composable
internal fun TapZoneActionRow(
    label: String,
    selected: TapZoneAction,
    onSelect: (TapZoneAction) -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        FlowRow(modifier = Modifier.padding(top = 4.dp)) {
            TAP_ZONE_ACTION_OPTIONS.forEach { (action, optionLabel) ->
                FilterChip(
                    selected = selected == action,
                    onClick = { onSelect(action) },
                    label = { Text(optionLabel) },
                    modifier = Modifier
                        .padding(end = Spacing.xs)
                        .semantics { contentDescription = "$label: $optionLabel" }
                )
            }
        }
    }
}
