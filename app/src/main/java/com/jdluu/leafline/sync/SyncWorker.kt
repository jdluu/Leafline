package com.jdluu.leafline

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.jdluu.leafline.sync.KoreaderSyncClient
import com.jdluu.leafline.sync.KoreaderSyncConfigStore
import com.jdluu.leafline.sync.KoreaderPushProgress
import com.jdluu.leafline.sync.ReadingProgressMath
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Background worker that pushes reading positions for recently-read books
 * to the KOReader-compatible sync server.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        val config = KoreaderSyncConfigStore.config
        if (config == null || !config.enabled) {
            Log.d(TAG, "Sync config not available or disabled — skipping")
            return Result.success()
        }
        val repository = try {
            com.jdluu.leafline.library.LeaflineDependencyHolder.getRepository(applicationContext)
        } catch (e: Exception) {
            Log.w(TAG, "Could not get repository", e)
            return Result.retry()
        }

        // Get books with saved progress (blocking — this is on Worker's background thread)
        val books = runCatching {
            kotlinx.coroutines.runBlocking {
                val all = repository.getAllBooks().let { flow ->
                    // Collect first emission for snapshot
                    kotlinx.coroutines.flow.first(flow)
                }
                all.filter { it.lastLocatorJson != null }
                    .filter { it.koreaderHash != null || it.lastLocatorJson != null }
            }
        }.getOrElse {
            Log.w(TAG, "Failed to read books", it)
            return Result.retry()
        }

        if (books.isEmpty()) {
            Log.d(TAG, "No books with progress to sync")
            return Result.success()
        }

        val api = KoreaderSyncClient()
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

        Log.d(TAG, "Sync complete: $pushed pushed, $failed failed")
        return if (failed > 0 && pushed == 0) Result.retry() else Result.success()
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