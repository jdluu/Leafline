package com.jdluu.leafline.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.export.BookExportData
import com.jdluu.leafline.export.NotesExporter
import com.jdluu.leafline.export.ui.NotesExportSheet
import com.jdluu.leafline.theme.Spacing
import com.jdluu.leafline.theme.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    appearance: ThemeMode,
    onAppearanceChange: (ThemeMode) -> Unit,
    notesExporter: NotesExporter? = null,
    scrollState: ScrollState = rememberScrollState()
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = Spacing.md)
        ) {
            Spacer(Modifier.height(Spacing.xs))
            AppearanceSection(
                appearance = appearance,
                onAppearanceChange = onAppearanceChange
            )
            Spacer(Modifier.height(Spacing.lg))
            SyncSection()
            Spacer(Modifier.height(Spacing.lg))
            HardcoverSection()
            Spacer(Modifier.height(Spacing.lg))
            if (notesExporter != null) {
                ExportSection(notesExporter = notesExporter)
                Spacer(Modifier.height(Spacing.lg))
            }
            AboutSection()
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

/** Section heading on the 8dp rhythm: more air above the heading than below it. */
@Composable
internal fun SettingsSectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = Spacing.xs)
    )
}

/**
 * One decision on the settings screen: a label, its current value, and the
 * control that changes it. The whole row is the target, so the reader aims at
 * a name instead of a chip buried in a wall of controls.
 */
@Composable
internal fun SettingsRow(
    title: String,
    summary: String?,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(vertical = Spacing.xs)
            .semantics(mergeDescendants = true) {}
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(Spacing.xs))
        trailing()
    }
}

@Composable
private fun ExportSection(notesExporter: NotesExporter) {
    val scope = rememberCoroutineScope()
    var batchData by remember { mutableStateOf<List<BookExportData>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionHeading("Your data")
        SettingsRow(
            title = "Export notes & highlights",
            summary = when {
                isLoading -> "Preparing\u2026"
                errorMessage != null ->
                    "Could not read your notes: $errorMessage. Tap to try again."
                else -> "Markdown, JSON, Obsidian vault, Readwise CSV"
            },
            onClick = {
                if (!isLoading) {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            batchData = notesExporter.getAllExportData(includeEmptyBooks = false)
                            showExportSheet = true
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "unknown error"
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            trailing = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp), // touch-target-ok: status indicator inside the row's 56dp target
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    if (showExportSheet && batchData != null) {
        NotesExportSheet(
            notesExporter = notesExporter,
            batchBooksData = batchData,
            onDismiss = {
                showExportSheet = false
                batchData = null
            }
        )
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionHeading("About")
        Text("Leafline $versionName", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "All reading data stays on this device. No analytics, crash reporting, or telemetry.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs)
        )
    }
}
