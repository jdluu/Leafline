package com.jdluu.leafline.hardcover

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Hardcover GraphQL client.
 *
 * The API is in beta and its queries can change, so every response is read
 * defensively and a payload without the expected `data` field is reported as a
 * failure rather than treated as an empty success — a silent success would mark a
 * book as synced when nothing was written.
 *
 * Requests are only ever made through [HardcoverSyncer], which returns early when
 * the integration is off.
 */
class HardcoverClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val userAgent: String = "Leafline/unknown",
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : HardcoverApi {

    override suspend fun userId(token: String): Int = withContext(dispatcher) {
        val data = post(
            token,
            QUERY_ME,
            emptyMap()
        )
        val me = data.getAsJsonArray("me")
        if (me == null || me.size() == 0) {
            throw HardcoverException("Hardcover did not return a user for this token")
        }
        me[0].asJsonObject.requiredInt("id", "user id")
    }

    override suspend fun editionByIsbn13(token: String, isbn13: String): HardcoverEdition? =
        withContext(dispatcher) {
            val data = post(token, QUERY_EDITION_BY_ISBN, mapOf("isbn" to isbn13))
            val editions = data.getAsJsonArray("editions") ?: return@withContext null
            if (editions.size() == 0) return@withContext null

            val row = editions[0].asJsonObject
            HardcoverEdition(
                editionId = row.requiredInt("id", "edition id"),
                bookId = row.requiredInt("book_id", "book id"),
                pages = row.optionalInt("pages")
            )
        }

    override suspend fun userBookRead(
        token: String,
        userId: Int,
        bookId: Int
    ): HardcoverRead? = withContext(dispatcher) {
        val data = post(token, QUERY_USER_BOOK_READ, mapOf("userId" to userId, "bookId" to bookId))
        val rows = data.getAsJsonArray("user_books") ?: return@withContext null
        if (rows.size() == 0) return@withContext null

        val row = rows[0].asJsonObject
        val reads = row.getAsJsonArray("user_book_reads")
        val openRead = reads?.takeIf { it.size() > 0 }?.get(0)?.asJsonObject
        HardcoverRead(
            userBookId = row.requiredInt("id", "library entry id"),
            openReadId = openRead?.optionalInt("id"),
            edition = openRead?.getAsJsonObject("edition")?.let { edition ->
                HardcoverEdition(
                    editionId = 0,
                    bookId = bookId,
                    pages = edition.optionalInt("pages")
                )
            }
        )
    }

    override suspend fun insertUserBook(
        token: String,
        bookId: Int,
        statusId: Int
    ): HardcoverRead? = withContext(dispatcher) {
        val data = post(token, MUTATION_INSERT_USER_BOOK, mapOf("bookId" to bookId, "statusId" to statusId))
        val payload = data.requireMutation("insert_user_book")
        val inserted = payload.getAsJsonObject("user_book") ?: return@withContext null
        val reads = inserted.getAsJsonArray("user_book_reads")
        val openRead = reads?.takeIf { it.size() > 0 }?.get(0)?.asJsonObject
        HardcoverRead(
            userBookId = inserted.requiredInt("id", "library entry id"),
            openReadId = openRead?.optionalInt("id"),
            edition = openRead?.getAsJsonObject("edition")?.let { edition ->
                HardcoverEdition(
                    editionId = 0,
                    bookId = bookId,
                    pages = edition.optionalInt("pages")
                )
            }
        )
    }

    override suspend fun updateStatus(token: String, userBookId: Int, statusId: Int) =
        withContext(dispatcher) {
            val data = post(token, MUTATION_UPDATE_STATUS, mapOf("id" to userBookId, "statusId" to statusId))
            data.requireMutation("update_user_book")
            Unit
        }

    override suspend fun updateProgress(token: String, userBookReadId: Int, pages: Int) =
        withContext(dispatcher) {
            val data = post(token, MUTATION_UPDATE_PROGRESS, mapOf("readId" to userBookReadId, "pages" to pages))
            data.requireMutation("update_user_book_read")
            Unit
        }

    /**
     * Posts one GraphQL operation and returns its `data` object.
     *
     * A non-2xx response becomes a [HardcoverException] carrying the code, so 401
     * and 429 can be told apart from a generic failure. A 200 whose body has
     * `errors` is a failure too: Hasura returns those for rejected mutations.
     */
    private fun post(token: String, query: String, variables: Map<String, Any>): JsonObject {
        val payload = JsonObject().apply {
            addProperty("query", query)
            add(
                "variables",
                JsonObject().apply {
                    variables.forEach { (key, value) ->
                        when (value) {
                            is Int -> addProperty(key, value)
                            else -> addProperty(key, value.toString())
                        }
                    }
                }
            )
        }

        val request = Request.Builder()
            .url(baseUrl)
            .header("authorization", "Bearer $token")
            .header("user-agent", userAgent)
            .post(payload.toString().toRequestBody(JSON))
            .build()

        val response = okHttpClient.newCall(request).execute()
        response.use { res ->
            val body = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                throw HardcoverException(
                    "Hardcover returned HTTP ${res.code}",
                    statusCode = res.code
                )
            }

            val root = runCatching { JsonParser.parseString(body).asJsonObject }
                .getOrElse { throw HardcoverException("Hardcover returned a response that is not JSON") }

            if (root.has("errors") && !root.get("errors").isJsonNull) {
                val message = runCatching {
                    root.getAsJsonArray("errors")[0].asJsonObject.get("message").asString
                }.getOrNull() ?: "Hardcover rejected the request"
                throw HardcoverException(message)
            }

            return root.getAsJsonObject("data")
                ?: throw HardcoverException("Hardcover response had no data")
        }
    }

    /**
     * Returns the mutation payload, rejecting it when Hasura reported a logical
     * failure in `error` on an otherwise-200 response. Without this a refused write
     * (for example a book already in the library) would read as success.
     */
    private fun JsonObject.requireMutation(name: String): JsonObject {
        val payload = getAsJsonObject(name)
            ?: throw HardcoverException("Hardcover did not return $name")
        val error = payload.get("error")
        if (error != null && !error.isJsonNull) {
            throw HardcoverException(error.asString)
        }
        return payload
    }

    private fun JsonObject.requiredInt(field: String, label: String): Int {
        val element = get(field)
        if (element == null || element.isJsonNull) {
            throw HardcoverException("Hardcover response is missing the $label")
        }
        return runCatching { element.asInt }
            .getOrElse { throw HardcoverException("Hardcover returned a non-numeric $label") }
    }

    private fun JsonObject.optionalInt(field: String): Int? {
        val element = get(field) ?: return null
        if (element.isJsonNull) return null
        return runCatching { element.asInt }.getOrNull()
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.hardcover.app/v1/graphql"

        private val JSON = "application/json; charset=utf-8".toMediaType()

        internal const val QUERY_ME = """
            query Me { me { id } }
        """

        internal const val QUERY_EDITION_BY_ISBN = """
            query EditionByIsbn(${'$'}isbn: String!) {
              editions(where: {isbn_13: {_eq: ${'$'}isbn}}, limit: 1) { id book_id pages }
            }
        """

        internal const val QUERY_USER_BOOK_READ = """
            query UserBookRead(${'$'}userId: Int!, ${'$'}bookId: Int!) {
              user_books(where: {user_id: {_eq: ${'$'}userId}, book_id: {_eq: ${'$'}bookId}}, limit: 1) {
                id
                status_id
                user_book_reads(where: {finished_at: {_is_null: true}}, order_by: {id: desc}, limit: 1) {
                  id
                  edition { pages }
                }
              }
            }
        """

        // The mutation payloads are Hasura "IdType" wrappers: { error, id, <row> }.
        // Selecting a column directly (user_book_reads / status_id / progress_pages)
        // is rejected with "field not found in type", so the row is selected nested.
        internal const val MUTATION_INSERT_USER_BOOK = """
            mutation InsertUserBook(${'$'}bookId: Int!, ${'$'}statusId: Int!) {
              insert_user_book(object: {book_id: ${'$'}bookId, status_id: ${'$'}statusId}) {
                error
                id
                user_book {
                  id
                  status_id
                  user_book_reads(order_by: {id: desc}, limit: 1) { id edition { pages } }
                }
              }
            }
        """

        internal const val MUTATION_UPDATE_STATUS = """
            mutation UpdateStatus(${'$'}id: Int!, ${'$'}statusId: Int!) {
              update_user_book(id: ${'$'}id, object: {status_id: ${'$'}statusId}) {
                error
                id
                user_book { id status_id }
              }
            }
        """

        internal const val MUTATION_UPDATE_PROGRESS = """
            mutation UpdateProgress(${'$'}readId: Int!, ${'$'}pages: Int!) {
              update_user_book_read(id: ${'$'}readId, object: {progress_pages: ${'$'}pages}) {
                error
                id
                user_book_read { id progress_pages }
              }
            }
        """
    }
}
