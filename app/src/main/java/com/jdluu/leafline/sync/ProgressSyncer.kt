package com.jdluu.leafline.sync

import com.jdluu.leafline.FileHashUtil
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.data.LibraryRepository
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Minimal book identity needed for progress sync.
 *
 * Keeps the syncer decoupled from the full [LibraryBook] model and easy to
 * construct in tests.
 */
data class BookRef(
    val stableId: String,
    val filePath: String,
    val koreaderHash: String? = null,
    val lastReadAtEpochMillis: Long? = null,
    val lastLocatorJson: String? = null
) {
    constructor(book: LibraryBook) : this(
        stableId = book.stableId,
        filePath = book.filePath,
        koreaderHash = book.koreaderHash,
        lastReadAtEpochMillis = book.lastReadAtEpochMillis,
        lastLocatorJson = book.lastLocatorJson
    )
}

/** Outcome of a pull attempt on book open. */
sealed interface PullOutcome {
    data object Disabled : PullOutcome
    data object HashUnavailable : PullOutcome
    data object NoRemoteProgress : PullOutcome
    data object UpToDate : PullOutcome
    data class RemoteAhead(
        val remote: KoreaderRemoteProgress,
        val remotePercentage: Double?
    ) : PullOutcome
    data class Failure(val message: String) : PullOutcome
}

/** Outcome of a push attempt on reader exit. */
sealed interface PushOutcome {
    data object Disabled : PushOutcome
    data object Skipped : PushOutcome
    data object Pushed : PushOutcome
    data class Failure(val message: String) : PushOutcome
}

/**
 * Orchestrates KOReader-compatible progress sync for one book.
 *
 * The syncer stays silent on success; call sites surface only failures and
 * jump offers to the user.
 */
class ProgressSyncer(
    private val api: KoreaderSyncApi,
    private val configSource: () -> KoreaderSyncConfig?,
    private val repository: LibraryRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val deviceName: String,
    private val deviceId: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun push(book: BookRef, locatorJson: String?): PushOutcome {
        val config = configSource() ?: return PushOutcome.Disabled
        if (!config.enabled) return PushOutcome.Disabled
        if (locatorJson.isNullOrBlank()) return PushOutcome.Skipped
        val percentage = ReadingProgressMath.percentageFromLocator(locatorJson)
            ?: return PushOutcome.Skipped
        val hash = resolveKoreaderHash(book) ?: return PushOutcome.Skipped
        return try {
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
                    timestamp = clock()
                )
            )
            PushOutcome.Pushed
        } catch (e: Exception) {
            PushOutcome.Failure(e.message ?: "Pushing progress failed")
        }
    }

    suspend fun pull(book: BookRef): PullOutcome {
        val config = configSource() ?: return PullOutcome.Disabled
        if (!config.enabled) return PullOutcome.Disabled
        val hash = resolveKoreaderHash(book) ?: return PullOutcome.HashUnavailable
        val remote = try {
            api.getProgress(config.serverUrl, config.username, config.password, hash)
        } catch (e: Exception) {
            return PullOutcome.Failure(e.message ?: "Fetching progress failed")
        } ?: return PullOutcome.NoRemoteProgress
        if (!isRemoteNewer(
                remote,
                book.lastReadAtEpochMillis,
                ReadingProgressMath.percentageFromLocator(book.lastLocatorJson)
            )
        ) {
            return PullOutcome.UpToDate
        }
        return PullOutcome.RemoteAhead(
            remote = remote,
            remotePercentage = remote.percentage?.coerceIn(0.0, 100.0)
        )
    }

    private suspend fun resolveKoreaderHash(book: BookRef): String? {
        book.koreaderHash?.let { return it }
        return withContext(ioDispatcher) {
            val file = File(book.filePath)
            if (!file.exists()) return@withContext null
            try {
                val computed = FileHashUtil.koreaderHash(file)
                repository.setKoreaderHash(book.stableId, computed)
                computed
            } catch (e: Exception) {
                null
            }
        }
    }

    internal fun isRemoteNewer(
        remote: KoreaderRemoteProgress,
        localReadAtEpochMillis: Long?,
        localPercentage: Double?
    ): Boolean {
        // Local read stamps are always milliseconds; only the remote value
        // may arrive in seconds scale depending on the server generation.
        val remoteTimestamp = remote.timestamp?.let(::normalizeTimestampToMillis)
        if (remoteTimestamp != null) {
            return remoteTimestamp > (localReadAtEpochMillis ?: Long.MIN_VALUE)
        }
        val remotePercentage = remote.percentage ?: return false
        val current = localPercentage ?: return false
        return remotePercentage > current + PERCENTAGE_EPSILON
    }

    companion object {
        private const val PERCENTAGE_EPSILON = 0.01
    }
}
