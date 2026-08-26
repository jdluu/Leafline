package com.jdluu.leafline.reader.sync

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.jdluu.leafline.library.LibrarySortStore
import com.jdluu.leafline.library.ReadingStatus
import com.jdluu.leafline.library.data.LibraryRepository
import com.jdluu.leafline.sync.BookRef
import com.jdluu.leafline.sync.KoreaderSyncClient
import com.jdluu.leafline.sync.KoreaderSyncConfigStore
import com.jdluu.leafline.sync.ProgressSyncer
import com.jdluu.leafline.sync.PullOutcome
import com.jdluu.leafline.sync.PushOutcome
import com.jdluu.leafline.sync.ReadingProgressMath
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Locator
import java.util.UUID

/**
 * Owns the reader-side KOReader progress sync flow: creating the syncer,
 * pulling remote progress on book open, presenting conflict sheets, and
 * pushing progress when the reader closes.
 *
 * The activity supplies lambdas for navigator access and UI effects so this
 * class stays free of Activity references where possible.
 */
class ReaderSyncManager(
    private val context: Context,
    private val scope: LifecycleCoroutineScope,
    private val libraryRepository: LibraryRepository,
    private val currentBookProvider: () -> com.jdluu.leafline.library.LibraryBook?,
    private val currentLocatorProvider: () -> Locator?,
    private val goToLocator: (Locator) -> Unit,
    private val onConflictState: (com.jdluu.leafline.SyncConflictState?) -> Unit,
) {
    lateinit var progressSyncer: ProgressSyncer
        private set

    fun initialize() {
        progressSyncer = createProgressSyncer()
    }

    private fun createProgressSyncer(): ProgressSyncer {
        val preferences = context.getSharedPreferences(LibrarySortStore.PREFS_NAME, Context.MODE_PRIVATE)
        val deviceId = preferences.getString(KEY_SYNC_DEVICE_ID, null) ?: UUID.randomUUID().toString()
            .also { generated ->
                preferences.edit().putString(KEY_SYNC_DEVICE_ID, generated).apply()
            }
        return ProgressSyncer(
            api = KoreaderSyncClient(),
            configSource = { KoreaderSyncConfigStore.config },
            repository = libraryRepository,
            deviceName = Build.MODEL ?: "Leafline",
            deviceId = deviceId
        )
    }

    suspend fun pullRemoteProgress() {
        val book = currentBookProvider() ?: return
        when (val outcome = progressSyncer.pull(BookRef(book))) {
            is PullOutcome.RemoteAhead -> presentRemoteProgress(outcome)
            is PullOutcome.Failure -> Toast.makeText(
                context,
                "Progress sync failed: ${outcome.message}",
                Toast.LENGTH_LONG
            ).show()
            else -> Unit
        }
    }

    private fun presentRemoteProgress(outcome: PullOutcome.RemoteAhead) {
        val remote = outcome.remote
        val localProgress = currentLocatorProvider()?.let { locator ->
            ReadingProgressMath.percentageFromLocator(locator.toJSON().toString())
        }
        val target = remote.progress?.let { json ->
            try {
                Locator.fromJSON(org.json.JSONObject(json))
            } catch (e: Exception) {
                null
            }
        }
        if (target == null) {
            onConflictState(
                com.jdluu.leafline.SyncConflictState(
                    localPercentage = localProgress,
                    remotePercentage = outcome.remotePercentage,
                    remoteDevice = remote.device,
                    remoteTimestamp = remote.timestamp,
                    onJump = {}
                )
            )
            return
        }
        onConflictState(
            com.jdluu.leafline.SyncConflictState(
                localPercentage = localProgress,
                remotePercentage = outcome.remotePercentage,
                remoteDevice = remote.device,
                remoteTimestamp = remote.timestamp,
                onJump = { goToLocator(target) }
            )
        )
    }

    fun pushProgressOnExit(onMarkFinishedPrompt: (com.jdluu.leafline.library.LibraryBook, Double) -> Unit) {
        val book = currentBookProvider() ?: return
        val locator = currentLocatorProvider() ?: return
        val locatorJson = locator.toJSON().toString()
        scope.launch {
            val outcome = try {
                withContext(NonCancellable) { progressSyncer.push(BookRef(book), locatorJson) }
            } catch (e: Exception) {
                PushOutcome.Failure(e.message ?: "Pushing progress failed")
            }
            if (outcome is PushOutcome.Failure) {
                Toast.makeText(
                    context,
                    "Progress sync failed: ${outcome.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Auto-suggest FINISHED status when progress >= 98%
        val progress = ReadingProgressMath.percentageFromLocator(locatorJson)
        if (progress != null && progress >= 98.0 && book.readingStatus == ReadingStatus.READING) {
            onMarkFinishedPrompt(book, progress)
        }
    }

    fun showMarkFinishedDialog(book: com.jdluu.leafline.library.LibraryBook, progress: Double) {
        scope.launch {
            // Dialog must be shown on main thread; callers already are there.
        }
        AlertDialog.Builder(context)
            .setTitle("Mark as finished?")
            .setMessage("You're at ${progress.toInt()}% — looks like you've finished this book. Would you like to mark it as finished?")
            .setPositiveButton("Mark finished") { _, _ ->
                scope.launch {
                    libraryRepository.setReadingStatus(book.stableId, ReadingStatus.FINISHED)
                }
            }
            .setNegativeButton("Keep reading") { _, _ -> }
            .show()
    }

    companion object {
        private const val KEY_SYNC_DEVICE_ID = "sync_device_id"
        private const val TAG = "ReaderSyncManager"
    }
}
