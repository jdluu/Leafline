package com.jdluu.leafline.opds.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.opds.model.DownloadStatus
import com.jdluu.leafline.opds.model.OpdsBook
import com.jdluu.leafline.opds.model.OpdsFeedSource
import com.jdluu.leafline.theme.LeaflineTextField
import com.jdluu.leafline.theme.Spacing

@Composable
internal fun OpdsBookDetailDialog(
    book: OpdsBook,
    downloadStatus: DownloadStatus,
    isInLibrary: Boolean,
    onDismiss: () -> Unit,
    onDownloadClick: () -> Unit,
    onOpenInReader: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = book.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                if (!book.author.isNullOrBlank()) {
                    Text(
                        text = "By ${book.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (book.categories.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        book.categories.forEach { category ->
                            AssistChip(
                                onClick = {},
                                label = { Text(category) }
                            )
                        }
                    }
                }

                if (!book.summary.isNullOrBlank()) {
                    Text(
                        text = book.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when (downloadStatus) {
                    is DownloadStatus.Downloading -> {
                        LinearProgressIndicator(
                            progress = { downloadStatus.progressFraction },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Downloading... ${(downloadStatus.progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    is DownloadStatus.Importing -> {
                        Text(
                            text = "Importing into your Leafline library...",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    is DownloadStatus.Error -> {
                        Text(
                            text = "Download error: ${downloadStatus.message}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            if (isInLibrary) {
                Button(onClick = onOpenInReader) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Read Book")
                }
            } else if (!book.downloadUrl.isNullOrBlank()) {
                Button(
                    onClick = onDownloadClick,
                    enabled = downloadStatus !is DownloadStatus.Downloading && downloadStatus !is DownloadStatus.Importing
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Download & Add to Library")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private enum class OpdsAuthType(val label: String) {
    NONE("None"),
    BASIC("Basic"),
    BEARER("Token")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddOpdsFeedDialog(
    onDismiss: () -> Unit,
    initialTitle: String = "",
    initialUrl: String = "",
    onAdd: (String, String, String?, String?, String?) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var url by remember { mutableStateOf(initialUrl) }
    var authType by remember { mutableStateOf(OpdsAuthType.NONE) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var token by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Catalog Feed") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = "Add an OPDS catalog URL from Calibre-Web, Kavita, or any open catalog feed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LeaflineTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Feed Name",
                    placeholder = "My Calibre Library"
                )
                LeaflineTextField(
                    value = url,
                    onValueChange = { url = it; isError = false },
                    label = "OPDS URL",
                    placeholder = "http://192.168.1.10:8080/opds",
                    keyboardType = KeyboardType.Uri,
                    isError = isError,
                    supportingText = if (isError) "URL must start with http:// or https://" else null
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "Authentication",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    OpdsAuthType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = authType == type,
                            onClick = { authType = type },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = OpdsAuthType.entries.size
                            ),
                            // No leading icon: the default check glyph reserves width and
                            // forces these three labels to wrap.
                            icon = {},
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(type.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                when (authType) {
                    OpdsAuthType.NONE -> { /* Open catalog: nothing more to ask for. */ }
                    OpdsAuthType.BASIC -> {
                        LeaflineTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = "Username",
                            placeholder = "admin"
                        )
                        LeaflineTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Password",
                            keyboardType = KeyboardType.Password,
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (passwordVisible) {
                                            "Hide password"
                                        } else {
                                            "Show password"
                                        }
                                    )
                                }
                            }
                        )
                    }
                    OpdsAuthType.BEARER -> {
                        LeaflineTextField(
                            value = token,
                            onValueChange = { token = it },
                            label = "API / Bearer Token",
                            placeholder = "kavita-api-key"
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = url.trim()
                    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                        onAdd(
                            title.trim().ifEmpty { "Custom Catalog" },
                            trimmed,
                            if (authType == OpdsAuthType.BASIC) username.trim().ifBlank { null } else null,
                            if (authType == OpdsAuthType.BASIC) password.ifBlank { null } else null,
                            if (authType == OpdsAuthType.BEARER) token.trim().ifBlank { null } else null
                        )
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Add Feed")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun OpdsRemoveCredentialsDialog(
    source: OpdsFeedSource,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove saved credentials?") },
        text = {
            Text(
                "${source.title} keeps its place in the list. The next time it loads, " +
                    "Leafline asks the catalog for access without the saved account."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Remove credentials", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep")
            }
        }
    )
}
