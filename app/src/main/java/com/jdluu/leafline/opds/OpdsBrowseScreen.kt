package com.jdluu.leafline.opds

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpdsBrowseScreen(
    viewModel: OpdsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("OPDS Catalog") },
            navigationIcon = {
                TextButton(onClick = onBack) { Text("Back") }
            }
        )

        when (val s = state) {
            is OpdsUiState.Idle -> OpdsConfigForm(
                onConnect = { url, user, pass ->
                    viewModel.saveConfig(url, user, pass)
                    viewModel.loadRootNavigation()
                }
            )

            is OpdsUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is OpdsUiState.ConfigSaved -> Text(
                "Connected to ${s.config.catalogUrl}",
                modifier = Modifier.padding(16.dp)
            )

            is OpdsUiState.Loaded -> OpdsNavigationList(
                entries = s.navigation,
                onEdit = { viewModel.reset() }
            )

            is OpdsUiState.Error -> Column(Modifier.padding(16.dp)) {
                Text("Error: ${s.message}", color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.reset() }) { Text("Back to settings") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpdsConfigForm(
    onConnect: (String, String, String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Catalog URL") },
            placeholder = { Text("http://server:6060/api/v1/opds") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("OPDS username") },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("OPDS password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        Button(
            onClick = { onConnect(url, username, password) },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Browse Grimmory")
        }
        Text(
            "Credentials are stored only in memory and are never committed.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun OpdsNavigationList(
    entries: List<OpdsNavigationEntry>,
    onEdit: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize()) {
        if (entries.isEmpty()) {
            item { Text("No navigation feeds found", modifier = Modifier.padding(16.dp)) }
        } else {
            items(entries) { entry ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(entry.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            entry.href,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
        item {
            TextButton(onClick = onEdit) { Text("Edit settings") }
        }
    }
}
