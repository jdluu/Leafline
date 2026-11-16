package com.jdluu.leafline.export.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.export.BatchExportResult
import com.jdluu.leafline.export.BookExportData
import com.jdluu.leafline.export.ExportFormat
import com.jdluu.leafline.export.NotesExporter
import com.jdluu.leafline.export.SingleExportResult
import com.jdluu.leafline.theme.Padding
import com.jdluu.leafline.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Bottom sheet modal for exporting book notes and highlights to Obsidian Markdown,
 * structured JSON, plain text, or Obsidian Vault ZIP archives.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesExportSheet(
    notesExporter: NotesExporter,
    singleBookData: BookExportData? = null,
    batchBooksData: List<BookExportData>? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isBatch = batchBooksData != null
    val availableFormats = remember(isBatch) {
        if (isBatch) {
            listOf(
                ExportFormat.VAULT_ZIP,
                ExportFormat.OBSIDIAN_MARKDOWN,
                ExportFormat.READWISE_CSV,
                ExportFormat.ANKI_TSV,
                ExportFormat.STRUCTURED_JSON,
                ExportFormat.PLAIN_TEXT
            )
        } else {
            listOf(
                ExportFormat.OBSIDIAN_MARKDOWN,
                ExportFormat.READWISE_CSV,
                ExportFormat.ANKI_TSV,
                ExportFormat.STRUCTURED_JSON,
                ExportFormat.PLAIN_TEXT
            )
        }
    }

    var selectedFormat by remember {
        mutableStateOf(if (isBatch) ExportFormat.VAULT_ZIP else ExportFormat.OBSIDIAN_MARKDOWN)
    }

    var singleResult by remember { mutableStateOf<SingleExportResult?>(null) }
    var batchResult by remember { mutableStateOf<BatchExportResult?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    // Recompute export data whenever format changes
    LaunchedEffect(selectedFormat, singleBookData, batchBooksData) {
        if (singleBookData != null) {
            singleResult = notesExporter.exportSingle(singleBookData, selectedFormat)
        } else if (batchBooksData != null) {
            batchResult = notesExporter.exportBatch(batchBooksData, selectedFormat)
        }
    }

    // Document creation launcher for saving directly to storage
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(selectedFormat.mimeType)
    ) { uri: Uri? ->
        if (uri != null) {
            isExporting = true
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        if (singleResult != null) {
                            notesExporter.writeSingleToStream(singleResult!!, out)
                        } else if (batchResult != null) {
                            notesExporter.writeBatchToStream(batchResult!!, out)
                        }
                    }
                    snackbarHostState.showSnackbar("File saved successfully")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                } finally {
                    isExporting = false
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.surface
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(Spacing.md)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBatch) "Export Library Notes" else "Export Book Notes",
                            style = MaterialTheme.typography.titleLarge
                        )
                        val subtitle = if (isBatch) {
                            val booksCount = batchBooksData?.size ?: 0
                            val highlightsCount = batchBooksData?.sumOf { it.annotations.size } ?: 0
                            "$booksCount books • $highlightsCount highlights"
                        } else {
                            val title = singleBookData?.book?.title.orEmpty()
                            val count = singleBookData?.annotations?.size ?: 0
                            "$title • $count highlights"
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(Spacing.sm))

                // Format Selector Chips
                Text(
                    text = "Format",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.xs))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    availableFormats.forEach { format ->
                        FilterChip(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            label = { Text(format.label) },
                            leadingIcon = if (selectedFormat == format) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp) // touch-target-ok: chip decorative icon
                                    )
                                }
                            } else null
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.md))

                // Preview Card
                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.xs))

                val previewText = remember(selectedFormat, singleResult, batchResult) {
                    when {
                        singleResult != null -> singleResult!!.content
                        batchResult?.textPayload != null -> batchResult!!.textPayload!!
                        selectedFormat == ExportFormat.VAULT_ZIP -> {
                            val count = batchBooksData?.size ?: 0
                            val hlCount = batchBooksData?.sumOf { it.annotations.size } ?: 0
                            """
                            📦 Obsidian Vault Archive (.zip)
                            --------------------------------
                            Includes $count individual book markdown notes with rich YAML frontmatter, quote blocks, and ratings.
                            Includes Index.md with Obsidian [[WikiLinks]] connecting all books.
                            Total highlights: $hlCount
                            
                            Ready to extract directly into your Obsidian Vault or any markdown knowledge base.
                            """.trimIndent()
                        }
                        else -> "Generating preview…"
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 220.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(Spacing.sm)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = previewText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isExporting) {
                    Spacer(Modifier.height(Spacing.sm))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(Spacing.lg))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy to Clipboard (only if text available)
                    val copyableText = singleResult?.content ?: batchResult?.textPayload
                    if (copyableText != null) {
                        OutlinedButton(
                            onClick = {
                                notesExporter.copyToClipboard(context, copyableText, "Leafline Notes")
                                scope.launch {
                                    snackbarHostState.showSnackbar("Copied to clipboard")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp) // touch-target-ok: button decorative icon
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Copy")
                        }
                    }

                    // Share Intent
                    OutlinedButton(
                        onClick = {
                            if (singleResult != null) {
                                notesExporter.shareSingle(context, singleResult!!)
                            } else if (batchResult != null) {
                                notesExporter.shareBatch(context, batchResult!!)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            Icons.Default.IosShare,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp) // touch-target-ok: button decorative icon
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Share")
                    }

                    // Save to File
                    Button(
                        onClick = {
                            val defaultName = singleResult?.filename ?: batchResult?.filename ?: "Notes.${selectedFormat.extension}"
                            createDocLauncher.launch(defaultName)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp) // touch-target-ok: button decorative icon
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Save")
                    }
                }

                Spacer(Modifier.height(Spacing.md))
            }
        }
    }
}
