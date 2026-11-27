package com.jdluu.leafline.reader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.reader.footnotes.FootnoteContent
import com.jdluu.leafline.reader.tts.TtsSleepOption
import com.jdluu.leafline.reader.tts.TtsSleepState
import com.jdluu.leafline.reader.tts.formatSleepTimerLabel
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** UI state for the sync conflict bottom sheet. */
data class SyncConflictState(
    val localPercentage: Double?,
    val remotePercentage: Double?,
    val remoteDevice: String?,
    val remoteTimestamp: Long?,
    val onJump: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SyncConflictSheet(
    conflict: SyncConflictState,
    onJump: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text("Progress sync", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Local", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = conflict.localPercentage?.let { "%.0f%%".format(it) } ?: "—",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Remote", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = conflict.remotePercentage?.let { "%.0f%%".format(it) } ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(Padding.compact))

            if (conflict.remoteDevice != null) {
                MetadataRow("Device", conflict.remoteDevice)
            }
            conflict.remoteTimestamp?.let { ts ->
                MetadataRow("Last synced", formatEpochMillis(ts))
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Padding.compact)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text("Stay") }
                Button(
                    onClick = onJump,
                    modifier = Modifier.weight(1f)
                ) { Text("Jump to remote") }
            }

            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@Composable
internal fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

internal fun formatEpochMillis(millis: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US)
    return sdf.format(Date(millis))
}

/**
 * Bottom sheet for configuring text-to-speech sleep timers (off, timed durations, or end of chapter).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TtsSleepTimerSheet(
    currentState: TtsSleepState,
    onSelectOption: (TtsSleepOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Padding.screen)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sleep timer",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close sleep timer")
                }
            }

            Text(
                text = "Automatically pause reading aloud",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (currentState.isActive) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = when {
                                currentState.option == TtsSleepOption.END_OF_CHAPTER -> {
                                    "Pausing at the end of the chapter"
                                }
                                currentState.isFadingOut -> {
                                    "Gradually fading audio: ${formatSleepTimerLabel(currentState)}"
                                }
                                else -> {
                                    "Time remaining: ${formatSleepTimerLabel(currentState)}"
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            val options = listOf(
                TtsSleepOption.OFF to ("Off" to "Do not pause automatically"),
                TtsSleepOption.MINUTES_5 to ("5 minutes" to "Pause after 5 minutes of reading"),
                TtsSleepOption.MINUTES_10 to ("10 minutes" to "Pause after 10 minutes of reading"),
                TtsSleepOption.MINUTES_15 to ("15 minutes" to "Pause after 15 minutes of reading"),
                TtsSleepOption.MINUTES_30 to ("30 minutes" to "Pause after 30 minutes of reading"),
                TtsSleepOption.MINUTES_45 to ("45 minutes" to "Pause after 45 minutes of reading"),
                TtsSleepOption.MINUTES_60 to ("60 minutes" to "Pause after 60 minutes of reading"),
                TtsSleepOption.END_OF_CHAPTER to ("End of chapter" to "Pause when current chapter ends")
            )

            options.forEach { (option, labels) ->
                val (title, subtitle) = labels
                val isSelected = currentState.option == option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable {
                            onSelectOption(option)
                            onDismiss()
                        }
                        .padding(vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        modifier = Modifier.padding(end = Spacing.sm)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}

/**
 * Non-disruptive bottom sheet preview for footnotes and endnotes, allowing
 * the user to inspect note content without leaving their reading position.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FootnotePreviewSheet(
    footnote: FootnoteContent,
    onDismiss: () -> Unit,
    onJump: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Padding.screen)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp) // touch-target-ok: non-interactive decorative icon
                    )
                    Text(
                        text = footnote.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss footnote preview"
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = footnote.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = Spacing.sm)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .padding(end = Spacing.xs)
                ) {
                    Text("Close")
                }
                if (footnote.targetLocatorJson != null) {
                    FilledTonalButton(
                        onClick = {
                            val locJson = footnote.targetLocatorJson
                            onDismiss()
                            onJump(locJson)
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp) // touch-target-ok: non-interactive icon inside button
                                .padding(end = 4.dp)
                        )
                        Text("Jump to note")
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
        }
    }
}
