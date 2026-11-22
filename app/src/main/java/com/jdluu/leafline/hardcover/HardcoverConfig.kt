package com.jdluu.leafline.hardcover

/**
 * Hardcover connection settings.
 *
 * [enabled] is false until the reader turns it on, and nothing is sent while it is
 * false. [userId] is cached from the token check so a push does not spend one
 * request per book just to ask who the user is.
 */
data class HardcoverConfig(
    val token: String = "",
    val enabled: Boolean = false,
    val userId: Int? = null
)

/** In-memory holder, restored from disk when [com.jdluu.leafline.di.AppContainer] builds. */
object HardcoverConfigStore {
    @Volatile
    var config: HardcoverConfig? = null
}

/** Durable storage seam, kept Android-free for tests. */
interface HardcoverConfigPersistence {
    fun load(): HardcoverConfig?
    fun save(config: HardcoverConfig)
    fun clear()
}
