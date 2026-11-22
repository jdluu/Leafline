package com.jdluu.leafline.hardcover

/** Raised for any Hardcover failure the caller should report. */
class HardcoverException(
    message: String,
    /** 401 means the token is wrong or expired; 429 means back off. */
    val statusCode: Int? = null
) : Exception(message) {

    val isUnauthorized: Boolean get() = statusCode == 401
    val isRateLimited: Boolean get() = statusCode == 429
}

/** An edition of a book, as far as matching and progress need it. */
data class HardcoverEdition(
    val editionId: Int,
    val bookId: Int,
    /** Null when Hardcover does not know how many pages this edition has. */
    val pages: Int?
)

/** A library entry plus its open read, when one is in progress. */
data class HardcoverRead(
    val userBookId: Int,
    val openReadId: Int?,
    val edition: HardcoverEdition?
)

/**
 * Contract for Hardcover's GraphQL API.
 *
 * Deliberately free of OkHttp and Android so sync logic can be tested against a
 * fake. Each call takes the token explicitly: nothing is cached inside the client,
 * so disabling the integration leaves no credential behind in a field.
 */
interface HardcoverApi {

    /** Returns the user id when the token is valid. Throws [HardcoverException]. */
    suspend fun userId(token: String): Int

    /** Null when no edition carries that ISBN. */
    suspend fun editionByIsbn13(token: String, isbn13: String): HardcoverEdition?

    /** Null when the book is not in this user's library yet. */
    suspend fun userBookRead(token: String, userId: Int, bookId: Int): HardcoverRead?

    /** Adds the book to the library, which makes Hardcover start a read. */
    suspend fun insertUserBook(token: String, bookId: Int, statusId: Int): HardcoverRead?

    suspend fun updateStatus(token: String, userBookId: Int, statusId: Int)

    suspend fun updateProgress(token: String, userBookReadId: Int, pages: Int)
}
