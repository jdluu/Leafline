package com.jdluu.leafline.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jdluu.leafline.di.appContainer
import com.jdluu.leafline.hardcover.HardcoverConfigStore
import com.jdluu.leafline.hardcover.HardcoverPushOutcome
import com.jdluu.leafline.library.BookIdentity
import kotlinx.coroutines.flow.first
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Background worker that pushes reading positions to whichever destinations the
 * reader has turned on.
 *
 * KOReader and Hardcover are independent: each is skipped when it is disabled, and
 * the worker does nothing at all when neither is enabled, so an unconfigured
 * install makes no network call.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val koreader = KoreaderSyncConfigStore.config?.takeIf { it.enabled }
        val hardcover = HardcoverConfigStore.config?.takeIf { it.enabled }
        if (koreader == null && hardcover == null) {
            Log.d(TAG, "No sync destination enabled - skipping")
            return Result.success()
        }

        val repository = try {
            applicationContext.appContainer.libraryRepository
        } catch (e: Exception) {
            Log.w(TAG, "Could not get repository", e)
            return Result.retry()
        }

        val allBooks = try {
            repository.getAllBooks().first()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read books", e)
            return Result.retry()
        }

        var pushed = 0
        var failed = 0

        if (koreader != null) {
            val koreaderResult = pushKoreader(koreader, allBooks)
            pushed += koreaderResult.first
            failed += koreaderResult.second
        }

        if (hardcover != null) {
            val hardcoverResult = pushHardcover(allBooks)
            pushed += hardcoverResult.first
            failed += hardcoverResult.second
        }

        Log.d(TAG, "Background sync complete: $pushed pushed, $failed failed")
        return if (failed > 0 && pushed == 0) Result.retry() else Result.success()
    }

    private suspend fun pushKoreader(
        config: com.jdluu.leafline.sync.KoreaderSyncConfig,
        allBooks: List<com.jdluu.leafline.library.LibraryBook>
    ): Pair<Int, Int> {
        val books = allBooks.filter { it.lastLocatorJson != null && it.koreaderHash != null }
        if (books.isEmpty()) return 0 to 0

        val api = applicationContext.appContainer.koreaderSyncApi
        val deviceId = getDeviceId(applicationContext)
        val deviceName = android.os.Build.MODEL ?: "Leafline"
        var pushed = 0
        var failed = 0

        for (book in books) {
            val locatorJson = book.lastLocatorJson ?: continue
            val percentage = ReadingProgressMath.percentageFromLocator(locatorJson) ?: continue
            val hash = book.koreaderHash ?: continue
            try {
                api.putProgress(
                    config.serverUrl,
                    config.username,
                    config.password,
                    KoreaderPushProgress(
                        document = hash,
                        percentage = percentage,
                        progress = locatorJson,
                        device = deviceName,
                        deviceId = deviceId,
                        timestamp = System.currentTimeMillis()
                    )
                )
                pushed++
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push progress for ${book.title}", e)
                failed++
            }
        }
        return pushed to failed
    }

    /**
     * Pushes reading state for every book that has progress. Books without an ISBN
     * cost no request: the syncer rejects them before touching the network.
     */
    private suspend fun pushHardcover(
        allBooks: List<com.jdluu.leafline.library.LibraryBook>
    ): Pair<Int, Int> {
        val syncer = applicationContext.appContainer.hardcoverSyncer
        var pushed = 0
        var failed = 0

        for (book in allBooks) {
            val locatorJson = book.lastLocatorJson ?: continue
            val percentage = ReadingProgressMath.percentageFromLocator(locatorJson) ?: continue
            val outcome = syncer.push(
                identifier = BookIdentity.publicationOf(book.stableId),
                status = book.readingStatus,
                progression = percentage / 100.0
            )
            when (outcome) {
                HardcoverPushOutcome.Pushed, HardcoverPushOutcome.StatusOnly -> pushed++
                HardcoverPushOutcome.Unauthorized -> {
                    // Stop, and do not count this as a retryable failure: a rejected
                    // token stays rejected until the reader replaces it, so asking
                    // WorkManager to retry would loop forever. The settings row is
                    // where they can fix it.
                    Log.w(TAG, "Hardcover rejected the token - replace it in Settings")
                    break
                }
                HardcoverPushOutcome.RateLimited -> {
                    // The next scheduled run is an hour away; no backoff storm.
                    Log.d(TAG, "Hardcover rate limited; resuming next run")
                    break
                }
                is HardcoverPushOutcome.Failure -> {
                    Log.w(TAG, "Hardcover push failed for ${book.title}: $outcome")
                    failed++
                }
                HardcoverPushOutcome.Disabled,
                HardcoverPushOutcome.SkippedNoIsbn,
                HardcoverPushOutcome.NoMatchingEdition -> Unit
            }
        }
        return pushed to failed
    }

    private fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val KEY_DEVICE_ID = "sync_device_id"
        private const val WORK_NAME = "bg_progress_sync"

        /** Schedule the periodic sync job (once per hour, network required). */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                1, TimeUnit.HOURS,
                15, TimeUnit.MINUTES // 15-min flex interval
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Background sync scheduled (hourly)")
        }

        /** Cancel the periodic sync. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Background sync cancelled")
        }
    }
}
