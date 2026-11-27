package com.jdluu.leafline.library.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing
import com.jdluu.leafline.theme.leaflineEnterTransition
import com.jdluu.leafline.theme.leaflineExitTransition
import com.jdluu.leafline.theme.rememberReduceMotion
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreSheet(
    backupManager: LibraryBackupManager,
    onDismiss: () -> Unit,
    onBackupRestored: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var restoreSummary by remember { mutableStateOf<BackupRestoreSummary?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        backupManager.exportBackup(outputStream)
                    }
                    statusMessage = "Library backup saved successfully."
                    restoreSummary = null
                } catch (e: Exception) {
                    statusMessage = "Export failed: ${e.localizedMessage ?: "Unknown error"}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            scope.launch {
                try {
                    val summary = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        backupManager.importBackup(inputStream)
                    }
                    restoreSummary = summary
                    if (summary != null) {
                        onBackupRestored()
                    }
                    statusMessage = "Backup restored successfully."
                } catch (e: Exception) {
                    statusMessage = "Restore failed: ${e.localizedMessage ?: "Unknown error"}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    val reduceMotion = rememberReduceMotion()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Without this the last card sits under the gesture bar on gesture-nav devices.
                .navigationBarsPadding()
                .padding(horizontal = Padding.screen, vertical = Spacing.sm)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Library Backup & Restore",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(Spacing.xs))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp) // touch-target-ok: decorative icon
                )
                Text(
                    text = "100% on-device and private. No network needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(
                visible = statusMessage != null,
                enter = leaflineEnterTransition(reduceMotion),
                exit = leaflineExitTransition(reduceMotion)
            ) {
                statusMessage?.let { msg ->
                    Spacer(Modifier.height(Spacing.md))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(Spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Icon(
                                imageVector = if (restoreSummary != null) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(24.dp) // touch-target-ok: decorative feedback icon
                            )
                            Column {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                restoreSummary?.let { summary ->
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = buildString {
                                            append("${summary.booksMatched} ${plural(summary.booksMatched, "book")} matched • ")
                                            append("${summary.collectionsRestored} ${plural(summary.collectionsRestored, "collection")} restored • ")
                                            append("${summary.bookmarksRestored} ${plural(summary.bookmarksRestored, "bookmark")} • ")
                                            append("${summary.annotationsRestored} ${plural(summary.annotationsRestored, "highlight")}")

                                            // Only mention sessions/preferences when the file
                                            // actually carried them, so older backups stay quiet.
                                            if (summary.readingSessionsRestored > 0) {
                                                append(" • ${summary.readingSessionsRestored} ${plural(summary.readingSessionsRestored, "reading session")} restored")
                                            }
                                            if (summary.opdsFeedsRestored > 0) {
                                                append(" • ${summary.opdsFeedsRestored} ${plural(summary.opdsFeedsRestored, "catalog")} added")
                                            }
                                            if (summary.preferencesRestored) {
                                                append(" • settings restored")
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(Spacing.md))

            // Export Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        text = "Export Backup",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "Save collections, tags, series, ratings, reading stats, bookmarks, notes, and app/reader settings (without passwords or tokens) to a portable JSON file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Button(
                        onClick = {
                            val timeStamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                            createDocumentLauncher.launch("leafline-backup-$timeStamp.json")
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp) // touch-target-ok: decorative button icon
                        )
                        Spacer(Modifier.size(Spacing.xs)) // touch-target-ok: layout spacing
                        Text("Export to file")
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // Restore Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        text = "Restore from Backup",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "Merge your tags, shelves, bookmarks, and highlights from a previously exported Leafline JSON backup file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedButton(
                        onClick = {
                            openDocumentLauncher.launch(
                                arrayOf("application/json", "application/octet-stream", "*/*")
                            )
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp) // touch-target-ok: decorative button icon
                        )
                        Spacer(Modifier.size(Spacing.xs)) // touch-target-ok: layout spacing
                        Text("Restore from file")
                    }
                }
            }

            if (isProcessing) {
                Spacer(Modifier.height(Spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp) // touch-target-ok: progress spinner
                    )
                    Spacer(Modifier.size(Spacing.sm)) // touch-target-ok: layout spacing
                    Text("Processing backup...", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

/** Pluralises a count for the restore summary, e.g. "1 collection" vs "2 collections". */
private fun plural(count: Int, singular: String): String =
    if (count == 1) singular else "${singular}s"
