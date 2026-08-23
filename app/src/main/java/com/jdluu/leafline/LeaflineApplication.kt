package com.jdluu.leafline

import android.app.Application
import com.jdluu.leafline.sync.SyncWorker

class LeaflineApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Schedule background sync if config exists (will be a no-op if no config)
        SyncWorker.schedule(this)
    }
}