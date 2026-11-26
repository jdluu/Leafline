package com.jdluu.leafline.reader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.library.data.Bookmark
import com.jdluu.leafline.library.data.LocatorIdentity
import com.jdluu.leafline.reader.BRIGHTNESS_MAX
import com.jdluu.leafline.reader.BRIGHTNESS_MIN
import com.jdluu.leafline.reader.MinimalHudMode
import com.jdluu.leafline.reader.ReaderPreferencesStore
import com.jdluu.leafline.reader.clampBrightness
import com.jdluu.leafline.reader.navigation.ReaderScrubberHelper
import com.jdluu.leafline.reader.tts.ReaderTtsState
import com.jdluu.leafline.reader.tts.TtsSleepState
import com.jdluu.leafline.reader.tts.formatSleepTimerLabel
import com.jdluu.leafline.sync.ReadingProgressMath
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing
import kotlin.math.roundToInt
import org.readium.r2.shared.publication.Link

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrightnessControl(
    brightness: Float?,
    onBrightnessChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = Padding.screen)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Brightness6,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = Spacing.xs)
                )
                Text(
                    text = "Brightness",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = brightness?.let { "%.0f%%".format(it * 100) } ?: "System",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.semantics {
                        contentDescription = "Reset brightness to system default"
                    }
                ) {
                    Text("Reset")
                }
            }
            Slider(
                value = brightness ?: BRIGHTNESS_MAX,
                onValueChange = { onBrightnessChange(clampBrightness(it)) },
                valueRange = BRIGHTNESS_MIN..BRIGHTNESS_MAX,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Brightness" }
            )
        }
    }
}

/**
 * Quick controls row in the reader overlay toolbar, providing one-tap
 * theme switching (sepia) and explicit text-to-speech controls (play, pause, stop).
 */
@Composable
internal fun QuickControls(
    sepiaSelected: Boolean,
    onToggleSepia: () -> Unit,
    ttsState: ReaderTtsState = ReaderTtsState.UNAVAILABLE,
    ttsRate: Float = 1.0f,
    ttsSleepState: TtsSleepState = TtsSleepState(),
    onTtsPlay: () -> Unit = {},
    onTtsPause: () -> Unit = {},
    onTtsStop: () -> Unit = {},
    onTtsSkipPrevious: () -> Unit = {},
    onTtsSkipNext: () -> Unit = {},
    onTtsRateChange: (Float) -> Unit = {},
    onOpenSleepTimer: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 6.dp
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.padding(horizontal = Padding.screen, vertical = Spacing.xs)
        ) {
            FilterChip(
                selected = sepiaSelected,
                onClick = onToggleSepia,
                label = { Text("Sepia") },
                leadingIcon = {
                    Icon(Icons.Default.WbSunny, contentDescription = null)
                },
                modifier = Modifier.semantics {
                    contentDescription = if (sepiaSelected) "Disable sepia theme" else "Enable sepia theme"
                }
            )
            when (ttsState) {
                ReaderTtsState.PLAYING -> {
                    FilterChip(
                        selected = false,
                        onClick = onTtsSkipPrevious,
                        label = { Text("Prev") },
                        leadingIcon = {
                            Icon(Icons.Default.SkipPrevious, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Previous sentence"
                        }
                    )
                    FilterChip(
                        selected = true,
                        onClick = onTtsPause,
                        label = { Text("Pause") },
                        leadingIcon = {
                            Icon(Icons.Default.Pause, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Pause text-to-speech"
                        }
                    )
                    FilterChip(
                        selected = false,
                        onClick = onTtsSkipNext,
                        label = { Text("Next") },
                        leadingIcon = {
                            Icon(Icons.Default.SkipNext, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Next sentence"
                        }
                    )
                    FilterChip(
                        selected = false,
                        onClick = onTtsStop,
                        label = { Text("Stop") },
                        leadingIcon = {
                            Icon(Icons.Default.Stop, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Stop text-to-speech"
                        }
                    )
                    val speedOptions = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    FilterChip(
                        selected = false,
                        onClick = {
                            val idx = speedOptions.indexOf(ttsRate).let { if (it == -1) 1 else it }
                            val next = speedOptions[(idx + 1) % speedOptions.size]
                            onTtsRateChange(next)
                        },
                        label = { Text("${ttsRate}x") },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Text-to-speech speed ${ttsRate}x"
                        }
                    )
                    val timerLabel = formatSleepTimerLabel(ttsSleepState)
                    FilterChip(
                        selected = ttsSleepState.isActive,
                        onClick = onOpenSleepTimer,
                        label = { Text(timerLabel) },
                        leadingIcon = {
                            Icon(Icons.Default.Timer, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = if (ttsSleepState.isActive) {
                                "Sleep timer active: $timerLabel. Tap to adjust"
                            } else {
                                "Set sleep timer"
                            }
                        }
                    )
                }
                ReaderTtsState.PAUSED -> {
                    FilterChip(
                        selected = false,
                        onClick = onTtsSkipPrevious,
                        label = { Text("Prev") },
                        leadingIcon = {
                            Icon(Icons.Default.SkipPrevious, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Previous sentence"
                        }
                    )
                    FilterChip(
                        selected = false,
                        onClick = onTtsPlay,
                        label = { Text("Resume") },
                        leadingIcon = {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Resume text-to-speech"
                        }
                    )
                    FilterChip(
                        selected = false,
                        onClick = onTtsSkipNext,
                        label = { Text("Next") },
                        leadingIcon = {
                            Icon(Icons.Default.SkipNext, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Next sentence"
                        }
                    )
                    FilterChip(
                        selected = false,
                        onClick = onTtsStop,
                        label = { Text("Stop") },
                        leadingIcon = {
                            Icon(Icons.Default.Stop, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Stop text-to-speech"
                        }
                    )
                    val speedOptions = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    FilterChip(
                        selected = false,
                        onClick = {
                            val idx = speedOptions.indexOf(ttsRate).let { if (it == -1) 1 else it }
                            val next = speedOptions[(idx + 1) % speedOptions.size]
                            onTtsRateChange(next)
                        },
                        label = { Text("${ttsRate}x") },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Text-to-speech speed ${ttsRate}x"
                        }
                    )
                    val timerLabel = formatSleepTimerLabel(ttsSleepState)
                    FilterChip(
                        selected = ttsSleepState.isActive,
                        onClick = onOpenSleepTimer,
                        label = { Text(timerLabel) },
                        leadingIcon = {
                            Icon(Icons.Default.Timer, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = if (ttsSleepState.isActive) {
                                "Sleep timer active: $timerLabel. Tap to adjust"
                            } else {
                                "Set sleep timer"
                            }
                        }
                    )
                }
                ReaderTtsState.ERROR -> {
                    FilterChip(
                        selected = false,
                        onClick = onTtsPlay,
                        label = { Text("Retry TTS") },
                        leadingIcon = {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Retry text-to-speech"
                        }
                    )
                    FilterChip(
                        selected = false,
                        onClick = onTtsStop,
                        label = { Text("Stop") },
                        leadingIcon = {
                            Icon(Icons.Default.Stop, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Stop text-to-speech"
                        }
                    )
                }
                ReaderTtsState.IDLE -> {
                    FilterChip(
                        selected = false,
                        onClick = onTtsPlay,
                        label = { Text("Read aloud") },
                        leadingIcon = {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Read aloud with text-to-speech"
                        }
                    )
                    val timerLabel = formatSleepTimerLabel(ttsSleepState)
                    FilterChip(
                        selected = ttsSleepState.isActive,
                        onClick = onOpenSleepTimer,
                        label = { Text(timerLabel) },
                        leadingIcon = {
                            Icon(Icons.Default.Timer, contentDescription = null)
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = if (ttsSleepState.isActive) {
                                "Sleep timer active: $timerLabel. Tap to adjust"
                            } else {
                                "Set sleep timer"
                            }
                        }
                    )
                }
                ReaderTtsState.UNAVAILABLE -> {
                    // Hidden when TTS is not supported for the publication
                }
            }
        }
    }
}

/**
 * Warm/sepia quick control shown next to the brightness slider in the reader
 * overlay. Selected means the publication renders with the sepia theme;
 * toggling swaps between sepia and the previously active theme, persisted by
 * [ReaderPreferencesStore] across restarts. The full light, sepia, and dark
 * choice stays in the settings sheet.
 */
@Composable
internal fun SepiaQuickControl(
    selected: Boolean,
    onToggle: () -> Unit
) {
    QuickControls(
        sepiaSelected = selected,
        onToggleSepia = onToggle
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderTopBar(
    title: String,
    bookmarks: List<Bookmark>,
    bookmarkActionsEnabled: Boolean,
    currentLocatorJson: String?,
    onBack: () -> Unit,
    onOpenToc: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHighlights: () -> Unit,
    canNavigateHistoryBack: Boolean = false,
    canNavigateHistoryForward: Boolean = false,
    onNavigateHistoryBack: () -> Unit = {},
    onNavigateHistoryForward: () -> Unit = {}
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }
    val currentKey = remember(currentLocatorJson) {
        currentLocatorJson?.let { LocatorIdentity.key(it) }
    }
    val isCurrentLocationBookmarked = currentKey != null && bookmarks.any {
        LocatorIdentity.key(it.locatorJson) == currentKey
    }
    TopAppBar(
        title = {
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if (canNavigateHistoryBack) {
                IconButton(onClick = onNavigateHistoryBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Back to previous location"
                    )
                }
            }
            if (canNavigateHistoryForward) {
                IconButton(onClick = onNavigateHistoryForward) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Forward to next location"
                    )
                }
            }
            IconButton(onClick = onOpenToc) {
                Icon(Icons.Default.Menu, contentDescription = "Contents")
            }
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Default.Search, contentDescription = "Search in book")
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Reader settings")
            }
            IconButton(onClick = { moreMenuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(
                expanded = moreMenuExpanded,
                onDismissRequest = { moreMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(if (isCurrentLocationBookmarked) "Remove bookmark" else "Add bookmark")
                    },
                    onClick = {
                        moreMenuExpanded = false
                        onToggleBookmark()
                    },
                    leadingIcon = {
                        Icon(
                            if (isCurrentLocationBookmarked) Icons.Default.Bookmark
                            else Icons.Default.BookmarkBorder,
                            contentDescription = null
                        )
                    },
                    enabled = bookmarkActionsEnabled && currentLocatorJson != null
                )
                DropdownMenuItem(
                    text = { Text("Bookmarks") },
                    onClick = {
                        moreMenuExpanded = false
                        onOpenBookmarks()
                    },
                    leadingIcon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null) },
                    enabled = bookmarkActionsEnabled
                )
                DropdownMenuItem(
                    text = { Text("Highlights") },
                    onClick = {
                        moreMenuExpanded = false
                        onOpenHighlights()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.FormatColorFill, contentDescription = null)
                    },
                    enabled = bookmarkActionsEnabled
                )
            }
        }
    )
}

internal fun matchesChapter(linkHref: String, currentHref: String?): Boolean {
    if (currentHref == null) return false
    val cleanLink = linkHref.substringBefore("#").trimStart('/')
    val cleanCurrent = currentHref.substringBefore("#").trimStart('/')
    if (cleanLink.isEmpty() || cleanCurrent.isEmpty()) return false
    return cleanLink == cleanCurrent || cleanCurrent.endsWith(cleanLink) || cleanLink.endsWith(cleanCurrent)
}

@Composable
internal fun TocItem(
    link: Link,
    depth: Int,
    isCurrent: Boolean = false,
    hasChildren: Boolean = false,
    isExpanded: Boolean = true,
    onToggleExpand: () -> Unit = {},
    onClick: () -> Unit
) {
    val indent = (depth * 16).dp
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(
                onClickLabel = if (isCurrent) "Current section" else "Open section",
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.small,
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else ComposeColor.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = indent + 12.dp,
                    top = if (hasChildren) 4.dp else 12.dp,
                    bottom = if (hasChildren) 4.dp else 12.dp,
                    end = if (hasChildren) 4.dp else 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isCurrent) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp) // touch-target-ok: decorative icon in 48dp Surface
                )
            }
            Text(
                text = link.title ?: link.href.toString(),
                style = if (isCurrent) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                else MaterialTheme.typography.bodyMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (hasChildren) {
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse section" else "Expand section",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun ReaderBottomBar(
    currentLocatorJson: String?,
    activeDurationMillis: Long = 0L,
    currentChapterTitle: String? = null,
    chapterTitles: List<String> = emptyList(),
    canSkipPreviousChapter: Boolean = false,
    canSkipNextChapter: Boolean = false,
    onPreviousChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    onSeekToPercentage: ((Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val percent = remember(currentLocatorJson) {
        ReadingProgressMath.percentageFromLocator(currentLocatorJson)
    }
    val label = remember(currentLocatorJson) {
        ReadingProgressMath.progressLabel(currentLocatorJson)
    }
    val remaining = remember(percent, activeDurationMillis) {
        ReadingProgressMath.estimatedRemainingTimeLabel(percent, activeDurationMillis)
    }
    val displayText = remember(label, remaining, currentChapterTitle) {
        val chapter = currentChapterTitle?.trim()?.takeIf { it.isNotBlank() }
        val progressInfo = if (label == null) null
        else if (remaining != null) "$label • $remaining"
        else label
        when {
            chapter != null && progressInfo != null -> "$chapter • $progressInfo"
            progressInfo != null -> progressInfo
            else -> chapter
        }
    }

    var draggingPercent by remember { mutableStateOf<Double?>(null) }
    val activePercent = draggingPercent ?: percent ?: 0.0

    if (displayText != null || percent != null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (draggingPercent != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = ReaderScrubberHelper.resolveScrubberPreview(activePercent, chapterTitles),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp)
                        )
                    }
                } else if (displayText != null) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPreviousChapter,
                        enabled = canSkipPreviousChapter,
                        modifier = Modifier.semantics {
                            contentDescription = "Previous chapter"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = null
                        )
                    }

                    Slider(
                        value = activePercent.toFloat().coerceIn(0f, 100f),
                        onValueChange = { draggingPercent = it.toDouble() },
                        onValueChangeFinished = {
                            draggingPercent?.let { onSeekToPercentage?.invoke(it) }
                            draggingPercent = null
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = "Reading progress: ${activePercent.toInt()}%"
                            }
                    )

                    IconButton(
                        onClick = onNextChapter,
                        enabled = canSkipNextChapter,
                        modifier = Modifier.semantics {
                            contentDescription = "Next chapter"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

/**
 * An unobtrusive single-line footer rendered at the bottom edge while menus
 * are hidden. Displays reading progress according to the selected [MinimalHudMode].
 */
internal fun formatMinimalFooterText(
    chapterTitle: String?,
    percent: Int?,
    remainingTime: String? = null,
    mode: MinimalHudMode = MinimalHudMode.CHAPTER_AND_PERCENT
): String? {
    val chapter = chapterTitle?.trim()?.takeIf { it.isNotBlank() }
    val progressText = percent?.let { "$it%" }
    val remaining = remainingTime?.trim()?.takeIf { it.isNotBlank() }

    return when (mode) {
        MinimalHudMode.CHAPTER_AND_PERCENT -> when {
            chapter != null && progressText != null -> "$chapter • $progressText"
            chapter != null -> chapter
            progressText != null -> progressText
            else -> null
        }
        MinimalHudMode.PERCENT_ONLY -> progressText ?: chapter
        MinimalHudMode.TIME_REMAINING -> remaining ?: progressText ?: chapter
        MinimalHudMode.CHAPTER_ONLY -> chapter ?: progressText
    }
}

/**
 * Height reserved at the bottom of the reading view for the minimal footer: its 48dp
 * minimum touch target plus the 6dp bottom margin it is laid out with.
 */
internal const val READER_FOOTER_INSET_DP = 54

@Composable
internal fun MinimalReadingFooter(
    currentChapterTitle: String?,
    currentLocatorJson: String?,
    activeDurationMillis: Long = 0L,
    mode: MinimalHudMode = MinimalHudMode.CHAPTER_AND_PERCENT,
    onCycleMode: () -> Unit = {},
    pageTextColor: ComposeColor,
    modifier: Modifier = Modifier
) {
    val percent = remember(currentLocatorJson) {
        ReadingProgressMath.percentageFromLocator(currentLocatorJson)?.let {
            it.roundToInt()
        }
    }
    val remainingTime = remember(percent, activeDurationMillis) {
        percent?.let {
            ReadingProgressMath.estimatedRemainingTimeLabel(it.toDouble(), activeDurationMillis)
        }
    }
    val displayText = remember(currentChapterTitle, percent, remainingTime, mode) {
        formatMinimalFooterText(currentChapterTitle, percent, remainingTime, mode)
    }

    if (displayText != null) {
        Box(
            modifier = modifier
                .defaultMinSize(minHeight = 48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCycleMode
                )
                .semantics { contentDescription = "Reading status: $displayText. Tap to cycle format." },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.labelSmall,
                // The page's own ink, not the app scheme: the app can be dark while
                // the book page is white, where a 45% light gray disappears.
                color = pageTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
