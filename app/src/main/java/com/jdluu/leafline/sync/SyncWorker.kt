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
import com.jdluu.leafline.library.LeaflineDependencyHolder
import kotlinx.coroutines.flow.first
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Background worker that pushes reading positions for books with saved
 * progress to the KOReader-compatible sync server.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val config = KoreaderSyncConfigStore.config
        if (config == null || !config.enabled) {
            Log.d(TAG, "Sync config not available or disabled - skipping")
            return Result.success()
        }
        val repository = try {
            LeaflineDependencyHolder.getRepository(applicationContext)
        } catch (e: Exception) {
            Log.w(TAG, "Could not get repository", e)
            return Result.retry()
        }

        val books = try {
            repository.getAllBooks().first()
                .filter { it.lastLocatorJson != null && it.koreaderHash != null }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read books", e)
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

        Log.d(TAG, "Background sync complete: $pushed pushed, $failed failed")
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
