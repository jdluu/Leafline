package com.jdluu.leafline

import android.app.Application
import android.util.Log
import com.jdluu.leafline.sync.KoreaderSyncConfigStore
import com.jdluu.leafline.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Application entry point.
 *
 * Startup-time policy (#26): Application.onCreate runs on the main thread
 * before the first frame, so everything here must be cheap. WorkManager's
 * initialization and the periodic-sync enqueue are deferred to a background
 * coroutine; they are not needed until after first frame, and the sync is
 * hourly anyway.
 */
class LeaflineApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val app = this
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (KoreaderSyncConfigStore.config?.enabled == true) {
                    SyncWorker.schedule(app)
                }
            } catch (e: Exception) {
                Log.w("LeaflineApp", "Failed to schedule background sync", e)
            }
        }
    }
}
