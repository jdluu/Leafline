package com.jdluu.leafline.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdluu.leafline.di.appContainer
import com.jdluu.leafline.hardcover.HardcoverConfig
import com.jdluu.leafline.hardcover.HardcoverConfigStore
import com.jdluu.leafline.hardcover.HardcoverTrackingMode
import com.jdluu.leafline.sync.SyncWorker
import com.jdluu.leafline.theme.LeaflineTextField
import com.jdluu.leafline.theme.Spacing
import com.jdluu.leafline.theme.leaflineEnterTransition
import com.jdluu.leafline.theme.leaflineExitTransition
import com.jdluu.leafline.theme.rememberReduceMotion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Opt-in Hardcover sync.
 *
 * Two ways in: "Connect" runs the OAuth flow, which is the normal path and keeps a
 * refresh token so the connection does not expire mid-read; pasting a personal API
 * token stays available underneath for anyone who already has one, or for builds
 * with no OAuth app registered.
 *
 * Neither path sends anything on its own: the switch is the opt-in.
 */
@Composable
internal fun HardcoverSection() {
    val context = LocalContext.current
    remember { context.appContainer.hardcoverConfigStore }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val stored by HardcoverConfigStore.state.collectAsStateWithLifecycle()
    val reduceMotion = rememberReduceMotion()

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionHeading("Hardcover")
        SettingsRow(
            title = "Reading progress",
            summary = when {
                stored == null -> "Not set up"
                stored!!.needsReauthorization -> "Sign in again"
                !stored!!.enabled -> "Connected, paused"
                else -> "Sending progress"
            },
            onClick = { expanded = !expanded },
            trailing = {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        AnimatedVisibility(
            visible = expanded,
            enter = leaflineEnterTransition(reduceMotion),
            exit = leaflineExitTransition(reduceMotion)
        ) {
            HardcoverForm(stored = stored)
        }
    }
}

/**
 * Asks once whether one book may be sent to Hardcover.
 *
 * Dismissing is deliberately not an answer: it opens the book and records nothing, so
 * an accidental tap does not silently opt a book out for good.
 */
@Composable
fun HardcoverTrackingPrompt(
    onAnswer: (tracked: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send this book to Hardcover?") },
        text = {
            Text(
                "Leafline can send this book's reading status and page progress to your " +
                    "Hardcover account as you read it. The answer is remembered, and you " +
                    "can change it later in the book's details."
            )
        },
        confirmButton = {
            TextButton(onClick = { onAnswer(true) }) { Text("Send") }
        },
        dismissButton = {
            TextButton(onClick = { onAnswer(false) }) { Text("Don't send") }
        }
    )
}

/**
 * Which books are allowed to reach Hardcover.
 *
 * "All books" is the default because switching sync on is already the consent;
 * asking about each book would be a second question the reader has effectively
 * answered. "Ask each book" exists for people who want to keep some out.
 */
@Composable
private fun HardcoverTrackingModePicker(
    mode: HardcoverTrackingMode,
    onModeChange: (HardcoverTrackingMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Text(
            "Track books",
            style = MaterialTheme.typography.bodyMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            val options = listOf(
                HardcoverTrackingMode.ALL_BOOKS to "All books",
                HardcoverTrackingMode.ASK_EACH_BOOK to "Ask each book",
                HardcoverTrackingMode.OFF to "Off"
            )
            options.forEach { (option, label) ->
                FilterChip(
                    selected = mode == option,
                    onClick = { onModeChange(option) },
                    label = { Text(label) }
                )
            }
        }
        Text(
            text = when (mode) {
                HardcoverTrackingMode.ALL_BOOKS ->
                    "Every book with a matching ISBN is sent as you read it."
                HardcoverTrackingMode.ASK_EACH_BOOK ->
                    "You are asked once for each book, and the answer is remembered. " +
                        "Change a book's answer in its details."
                HardcoverTrackingMode.OFF ->
                    "Nothing is sent. Your Hardcover connection is kept."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HardcoverForm(stored: HardcoverConfig?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val coordinator = remember { context.appContainer.hardcoverOAuth }

    var token by remember(stored?.token) { mutableStateOf(stored?.token.orEmpty()) }
    var tokenVisible by remember { mutableStateOf(false) }
    var showTokenEntry by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var trackingMode by remember { mutableStateOf(context.appContainer.hardcoverTracking.mode()) }

    fun persist(next: HardcoverConfig) {
        HardcoverConfigStore.config = next
        context.appContainer.hardcoverConfigStore.save(next)
        // The worker exits immediately when nothing is enabled, so scheduling is
        // harmless; cancelling here would stop KOReader sync too.
        SyncWorker.schedule(context)
    }

    val connected = stored != null && (stored.token.isNotBlank() || stored.needsReauthorization)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        if (coordinator.isConfigured) {
            if (!connected) {
                Text(
                    "Sign in to send your reading progress to Hardcover. Leafline asks only " +
                        "for your profile, your library, catalog lookups, and writing progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        val url = coordinator.startAuthorization()
                        if (url == null) {
                            status = "Hardcover sign-in is not configured in this build."
                        } else {
                            uriHandler.openUri(url)
                        }
                    },
                    enabled = !working
                ) {
                    Text("Connect to Hardcover")
                }
            } else {
                Text(
                    text = when {
                        stored!!.needsReauthorization -> "Hardcover needs you to sign in again."
                        stored.userId != null -> "Connected as Hardcover user ${stored.userId}."
                        else -> "Connected to Hardcover."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    if (stored.needsReauthorization) {
                        Button(
                            onClick = {
                                val url = coordinator.startAuthorization()
                                if (url == null) {
                                    status = "Hardcover sign-in is not configured in this build."
                                } else {
                                    uriHandler.openUri(url)
                                }
                            }
                        ) { Text("Sign in again") }
                    }
                    TextButton(
                        onClick = {
                            working = true
                            scope.launch {
                                try {
                                    coordinator.disconnect()
                                    token = ""
                                    status = "Disconnected from Hardcover"
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    status = e.message ?: "Could not disconnect"
                                } finally {
                                    working = false
                                }
                            }
                        },
                        enabled = !working
                    ) { Text("Disconnect") }
                }
            }
        } else {
            Text(
                "This build has no Hardcover OAuth app, so sign-in is unavailable. " +
                    "You can still connect with a personal API token below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        stored?.takeIf { connected && !it.needsReauthorization }?.let { activeConfig ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Send reading progress", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = activeConfig.enabled,
                    onCheckedChange = { next ->
                        persist(activeConfig.copy(enabled = next))
                        status = if (next) "Sending progress" else "Paused"
                    }
                )
            }
            if (activeConfig.enabled) {
                HardcoverTrackingModePicker(
                    mode = trackingMode,
                    onModeChange = { mode ->
                        trackingMode = mode
                        context.appContainer.hardcoverTracking.setMode(mode)
                    }
                )
            }
        }

        TextButton(onClick = { showTokenEntry = !showTokenEntry }) {
            Text(if (showTokenEntry) "Hide API token option" else "Use an API token instead")
        }

        if (showTokenEntry) {
            Text(
                "Create a token at hardcover.app/account/api with the read:me, read:library " +
                    "and write:library scopes. It is stored encrypted on this device and is " +
                    "only used when you turn sync on.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LeaflineTextField(
                value = token,
                onValueChange = {
                    token = it
                    status = null
                },
                label = "API token",
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (tokenVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { tokenVisible = !tokenVisible }) {
                        Icon(
                            imageVector = if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (tokenVisible) "Hide token" else "Show token"
                        )
                    }
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Button(
                    onClick = {
                        val clean = token.trim()
                        if (clean.isEmpty() || working) return@Button
                        working = true
                        status = null
                        scope.launch {
                            try {
                                val userId = context.appContainer.hardcoverApi.userId(clean)
                                // Saving a token leaves the switch as it was, so the
                                // integration still only runs when the reader turns it on.
                                persist(
                                    HardcoverConfig.apiToken(
                                        token = clean,
                                        userId = userId,
                                        enabled = stored?.enabled ?: false
                                    )
                                )
                                status = "Connected as Hardcover user $userId"
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                status = e.message ?: "Could not reach Hardcover"
                            } finally {
                                working = false
                            }
                        }
                    },
                    enabled = token.isNotBlank() && !working
                ) {
                    Text(if (working) "Checking\u2026" else "Test and save")
                }
            }
        }

        status?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
