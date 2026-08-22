package com.jdluu.leafline

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
        internal const val EPUB_MIME_TYPE = "application/epub+zip"
    }

    private var pendingToast: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LeaflineApp(this) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleToast()
    }

    private fun handleToast() {
        pendingToast?.let { message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            pendingToast = null
        }
    }

    fun showToast(message: String) {
        pendingToast = message
        handleToast()
    }

    fun copyEpubToFile(contentUri: Uri, context: Context, onFailure: (String) -> Unit) {
        try {
            val fileName = getFileName(contentUri) ?: "imported.epub"
            val sanitizedName = sanitizeFileName(fileName)
            val targetFile = File(context.filesDir, sanitizedName)

            val inputStream = context.contentResolver.openInputStream(contentUri) ?: run {
                onFailure("No data available")
                return
            }

            val outputStream = targetFile.outputStream()
            try {
                inputStream.copyTo(outputStream)
            } finally {
                outputStream.close()
            }

            if (targetFile.length() == 0L) {
                targetFile.delete()
                onFailure("Empty file cannot be imported")
                return
            }

            val intent = ReaderActivity.newIntent(context, targetFile.absolutePath)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy EPUB", e)
            onFailure("Failed to import EPUB: ${e.message}")
        }
    }

    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }
}

@Composable
fun LeaflineApp(activity: ComponentActivity) {
    val context = LocalContext.current

    val importEpubLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            (activity as? MainActivity)?.copyEpubToFile(it, context) { message ->
                activity.showToast(message)
            }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Leafline", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "A focused EPUB reader for your library.",
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(
                    onClick = {
                        importEpubLauncher.launch(arrayOf(MainActivity.EPUB_MIME_TYPE))
                    },
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text("Import EPUB")
                }
                Button(
                    onClick = {
                        activity.startActivity(ReaderActivity.newIntent(context))
                    },
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text("Open EPUB Spike")
                }
            }
        }
    }
}