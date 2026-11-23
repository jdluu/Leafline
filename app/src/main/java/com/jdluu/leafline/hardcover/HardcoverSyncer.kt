package com.jdluu.leafline.hardcover

import com.jdluu.leafline.library.ReadingStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What a push attempt did, so the caller can tell the reader something true. */
sealed interface HardcoverPushOutcome {
    /** No token, or the integration is off: no request was made. */
    data object Disabled : HardcoverPushOutcome

    /** The EPUB carries no ISBN, so the book cannot be identified remotely. */
    data object SkippedNoIsbn : HardcoverPushOutcome

    /** The ISBN is valid but Hardcover has no edition for it. */
    data object NoMatchingEdition : HardcoverPushOutcome

    /** Hardcover knows the book, but has no page count to express progress. */
    data object StatusOnly : HardcoverPushOutcome

    data object Pushed : HardcoverPushOutcome

    /** The token was rejected; the integration should be turned off. */
    data object Unauthorized : HardcoverPushOutcome

    data object RateLimited : HardcoverPushOutcome

    data class Failure(val message: String) : HardcoverPushOutcome
}

/**
 * Pushes one book's reading state to Hardcover.
 *
 * Opt-in is enforced here, not only in the UI: with no config, or with the switch
 * off, this returns [HardcoverPushOutcome.Disabled] before touching the network. A
 * book with no ISBN is skipped without a request, so an unidentifiable local EPUB
 * never causes Hardcover traffic.
 */
class HardcoverSyncer(
    private val api: HardcoverApi,
    private val config: () -> HardcoverConfig?,
    /**
     * Supplies the access token, refreshing it when needed. Keeping this separate from
     * [config] is what lets OAuth tokens stay short-lived without every push having to
     * know how refresh works.
     */
    private val accessToken: suspend () -> String?,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun push(
        identifier: String?,
        status: ReadingStatus,
        progression: Double?
    ): HardcoverPushOutcome = withContext(dispatcher) {
        val current = config()
        if (current == null || !current.enabled) {
            return@withContext HardcoverPushOutcome.Disabled
        }
        val token = accessToken()
        if (token.isNullOrBlank()) {
            return@withContext HardcoverPushOutcome.Disabled
        }

        val isbn = HardcoverIsbn.isbn13From(identifier)
            ?: return@withContext HardcoverPushOutcome.SkippedNoIsbn

        try {
            val userId = current.userId ?: api.userId(token)
            val edition = api.editionByIsbn13(token, isbn)
                ?: return@withContext HardcoverPushOutcome.NoMatchingEdition

            val statusId = HardcoverStatus.idFor(status)
            val existing = api.userBookRead(token, userId, edition.bookId)

            val read = existing
                ?: api.insertUserBook(token, edition.bookId, statusId)
                ?: return@withContext HardcoverPushOutcome.Failure("Hardcover did not return the new library entry")

            if (existing != null && existing.userBookId == read.userBookId) {
                // Keep the remote status in step; Hardcover starts or finishes the
                // read itself, so no journal entry is written here.
                api.updateStatus(token, read.userBookId, statusId)
            }

            val pages = HardcoverProgress.pages(progression, read.edition?.pages ?: edition.pages)
                ?: return@withContext HardcoverPushOutcome.StatusOnly
            val readId = read.openReadId
                ?: return@withContext HardcoverPushOutcome.StatusOnly

            api.updateProgress(token, readId, pages)
            HardcoverPushOutcome.Pushed
        } catch (e: HardcoverException) {
            when {
                e.isUnauthorized -> HardcoverPushOutcome.Unauthorized
                e.isRateLimited -> HardcoverPushOutcome.RateLimited
                else -> HardcoverPushOutcome.Failure(e.message ?: "Hardcover request failed")
            }
        } catch (e: Exception) {
            HardcoverPushOutcome.Failure(e.message ?: "Hardcover request failed")
        }
    }
}
