package com.jdluu.leafline.sync

/**
 * Configuration for the KOReader-compatible progress sync endpoint.
 *
 * Credentials live only in
 * this model and the associated in-memory store. They are never written to
 * git, logs, or durable storage.
 */
data class KoreaderSyncConfig(
    val serverUrl: String,
    val username: String,
    val password: String,
    val enabled: Boolean
)

/** Session-scoped holder for [KoreaderSyncConfig]. */
object KoreaderSyncConfigStore {
    @Volatile
    var config: KoreaderSyncConfig? = null
}

/** Progress payload returned by GET /syncs/progress/{bookHash}. */
data class KoreaderRemoteProgress(
    val document: String?,
    val percentage: Double?,
    val progress: String?,
    val device: String?,
    val deviceId: String?,
    val timestamp: Long?
)

/** Progress payload sent to PUT /syncs/progress. */
data class KoreaderPushProgress(
    val document: String,
    val percentage: Double,
    val progress: String,
    val device: String,
    val deviceId: String,
    val timestamp: Long
)

/**
 * Normalizes a server timestamp to epoch milliseconds. KOReader-compatible
 * servers historically report seconds, while clients may send milliseconds;
 * values below the year 33658 in seconds are scaled up.
 */
fun normalizeTimestampToMillis(timestamp: Long): Long {
    return if (timestamp < MILLIS_THRESHOLD) timestamp * 1000L else timestamp
}

private const val MILLIS_THRESHOLD = 1_000_000_000_000L
