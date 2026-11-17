package com.jdluu.leafline.sync

/**
 * Durable storage for KOReader sync settings.
 *
 * Declared here so the sync package stays free of Android types; the platform layer
 * supplies the implementation.
 */
interface KoreaderSyncConfigPersistence {

    /** Returns the stored settings, or null when nothing has been saved yet. */
    fun load(): KoreaderSyncConfig?

    fun save(config: KoreaderSyncConfig)

    fun clear()
}
