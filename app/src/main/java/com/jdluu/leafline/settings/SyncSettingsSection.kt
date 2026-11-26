package com.jdluu.leafline.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdluu.leafline.di.appContainer
import com.jdluu.leafline.sync.KoreaderSyncConfig
import com.jdluu.leafline.sync.KoreaderSyncConfigStore
import com.jdluu.leafline.sync.SyncWorker
import com.jdluu.leafline.sync.isKoreaderServerUrlValid
import com.jdluu.leafline.sync.koreaderSyncSummary
import com.jdluu.leafline.theme.FormPanel
import com.jdluu.leafline.theme.IconSize
import com.jdluu.leafline.theme.LeaflineTextField
import com.jdluu.leafline.theme.Spacing
import kotlinx.coroutines.launch

/** Outcome of a save or connection test, shown beside an icon so it never relies on colour. */
private data class SyncStatus(val message: String, val success: Boolean)

@Composable
internal fun SyncSection() {
    val context = LocalContext.current
    // Touch the container once so saved settings land in the store before the
    // row summary reads it; otherwise a fresh process reports "Not set up".
    remember { context.appContainer.koreaderSyncConfigStore }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val stored by KoreaderSyncConfigStore.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSectionHeading("Progress sync")
        SettingsRow(
            title = "KOReader-compatible server",
            summary = koreaderSyncSummary(stored),
            onClick = { expanded = !expanded },
            trailing = {
                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.ExpandLess
                    } else {
                        Icons.Default.ExpandMore
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        AnimatedVisibility(visible = expanded) {
            KoreaderSyncForm(stored = stored)
        }
    }
}

/**
 * The KOReader sync form, collapsed behind its summary row until opened.
 */
@Composable
private fun KoreaderSyncForm(stored: KoreaderSyncConfig?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var serverUrl by remember { mutableStateOf(stored?.serverUrl.orEmpty()) }
    var syncUsername by remember { mutableStateOf(stored?.username.orEmpty()) }
    var syncPassword by remember { mutableStateOf(stored?.password.orEmpty()) }
    var enabled by remember { mutableStateOf(stored?.enabled ?: false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<SyncStatus?>(null) }

    fun currentConfig() = KoreaderSyncConfig(
        serverUrl = serverUrl.trim(),
        username = syncUsername.trim(),
        password = syncPassword,
        enabled = enabled
    )

    fun applySyncConfig(config: KoreaderSyncConfig) {
        KoreaderSyncConfigStore.config = config
        context.appContainer.koreaderSyncConfigStore.save(config)
        if (config.enabled) SyncWorker.schedule(context) else SyncWorker.cancel(context)
    }

    // Save writes what the reader typed, so the button stays off until the
    // address is one this app can actually reach.
    val urlValid = isKoreaderServerUrlValid(serverUrl)
    val showUrlError = serverUrl.isNotBlank() && !urlValid

    fun testConnection() {
        testing = true
        status = null
        scope.launch {
            status = try {
                val authorized = context.appContainer.koreaderSyncApi
                    .auth(serverUrl.trim(), syncUsername.trim(), syncPassword)
                if (authorized) {
                    SyncStatus("Connection OK", success = true)
                } else {
                    SyncStatus("Authentication failed", success = false)
                }
            } catch (e: Exception) {
                SyncStatus("Could not reach the server: ${e.message}", success = false)
            }
            testing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.xs)
    ) {
        FormPanel {
            LeaflineTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it; status = null },
                label = "Server URL",
                placeholder = "http://server:6061/api/koreader",
                keyboardType = KeyboardType.Uri,
                isError = showUrlError,
                supportingText = if (showUrlError) {
                    "Enter the full address, starting with http:// or https://"
                } else {
                    null
                }
            )
            LeaflineTextField(
                value = syncUsername,
                onValueChange = { syncUsername = it; status = null },
                label = "Username"
            )
            LeaflineTextField(
                value = syncPassword,
                onValueChange = { syncPassword = it; status = null },
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(
                        value = enabled,
                        // An invalid field must not strand a syncing config: turning
                        // sync off stays available even while the address is broken.
                        enabled = urlValid || enabled,
                        role = Role.Switch,
                        onValueChange = { checked ->
                            enabled = checked
                            applySyncConfig(currentConfig().copy(enabled = checked))
                            status = null
                        }
                    )
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sync in the background", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (urlValid || enabled) {
                            "Checks for newer positions automatically."
                        } else {
                            "Set a server URL first."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = enabled, onCheckedChange = null)
            }
            status?.let { current ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite }
                ) {
                    Icon(
                        imageVector = if (current.success) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.ErrorOutline
                        },
                        contentDescription = null,
                        tint = if (current.success) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(IconSize.small) // touch-target-ok: status glyph
                    )
                    Text(
                        current.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (current.success) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs)
            ) {
                OutlinedButton(
                    onClick = { testConnection() },
                    enabled = !testing,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(if (testing) "Testing\u2026" else "Test connection") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        applySyncConfig(currentConfig())
                        status = SyncStatus("Saved", success = true)
                    },
                    enabled = urlValid,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Save") }
            }
        }
        Text(
            "Credentials stay on this device; the password is encrypted before it is stored.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs)
        )
    }
}
