package com.jdluu.leafline.sync

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/** Error raised for unexpected sync server responses. */
class KoreaderSyncException(
    message: String,
    val statusCode: Int? = null
) : Exception(message)

/** Contract for the KOReader-compatible sync endpoints, testable without OkHttp. */
interface KoreaderSyncApi {
    /** Returns true when the credentials are accepted, false on 401. */
    suspend fun auth(serverUrl: String, username: String, password: String): Boolean

    /** Returns the stored progress, or null when the server reports 404. */
    suspend fun getProgress(
        serverUrl: String,
        username: String,
        password: String,
        bookHash: String
    ): KoreaderRemoteProgress?

    /** Uploads progress for a book hash. Throws on failure. */
    suspend fun putProgress(
        serverUrl: String,
        username: String,
        password: String,
        progress: KoreaderPushProgress
    )
}

/** OkHttp implementation of the Grimmory KOReader-compatible sync API. */
class KoreaderSyncClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : KoreaderSyncApi {

    override suspend fun auth(serverUrl: String, username: String, password: String): Boolean {
        return send(serverUrl, username, password, listOf(USERS, AUTH), requestBody = null) { response ->
            when (response.code) {
                HTTP_OK -> true
                HTTP_UNAUTHORIZED -> false
                else -> throw KoreaderSyncException(
                    "Auth failed with HTTP ${response.code}",
                    response.code
                )
            }
        }
    }

    override suspend fun getProgress(
        serverUrl: String,
        username: String,
        password: String,
        bookHash: String
    ): KoreaderRemoteProgress? {
        return send(
            serverUrl,
            username,
            password,
            listOf(SYNCS, PROGRESS, bookHash),
            requestBody = null
        ) { response ->
            when (response.code) {
                HTTP_OK -> response.body?.string()?.let(::parseRemoteProgress)
                HTTP_NOT_FOUND -> null
                else -> throw KoreaderSyncException(
                    "Fetching progress failed with HTTP ${response.code}",
                    response.code
                )
            }
        }
    }

    override suspend fun putProgress(
        serverUrl: String,
        username: String,
        password: String,
        progress: KoreaderPushProgress
    ) {
        val payload = JsonObject().apply {
            addProperty("timestamp", progress.timestamp)
            addProperty("document", progress.document)
            addProperty("percentage", progress.percentage)
            addProperty("progress", progress.progress)
            addProperty("device", progress.device)
            addProperty("device_id", progress.deviceId)
        }
        val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
        send(serverUrl, username, password, listOf(SYNCS, PROGRESS), body) { response ->
            if (!response.isSuccessful) {
                throw KoreaderSyncException(
                    "Pushing progress failed with HTTP ${response.code}",
                    response.code
                )
            }
            Unit
        }
    }

    private suspend fun <T> send(
        serverUrl: String,
        username: String,
        password: String,
        pathSegments: List<String>,
        requestBody: okhttp3.RequestBody?,
        handler: (Response) -> T
    ): T {
        val request = buildRequest(serverUrl, username, password, pathSegments, requestBody)
        val response = withContext(ioDispatcher) { httpClient.newCall(request).execute() }
        return response.use { handler(it) }
    }

    private fun buildRequest(
        serverUrl: String,
        username: String,
        password: String,
        pathSegments: List<String>,
        requestBody: okhttp3.RequestBody?
    ): Request {
        val url = endpointUrl(serverUrl, pathSegments)
        // KOReader sync servers authenticate with X-Auth-User / X-Auth-Key headers,
        // where the key is the MD5 hex digest of the user's key phrase.
        val md5Key = java.security.MessageDigest.getInstance("MD5")
            .digest(password.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return Request.Builder()
            .url(url)
            .header("X-Auth-User", username)
            .header("X-Auth-Key", md5Key)
            .apply {
                if (requestBody == null) get() else put(requestBody)
            }
            .build()
    }

    private fun endpointUrl(serverUrl: String, pathSegments: List<String>): HttpUrl {
        val base = serverUrl.trim().trimEnd('/').toHttpUrlOrNull()
            ?: throw KoreaderSyncException("Invalid sync server URL")
        val builder = base.newBuilder()
        for (segment in pathSegments) {
            builder.addPathSegment(segment)
        }
        return builder.build()
    }

    internal fun parseRemoteProgress(body: String): KoreaderRemoteProgress {
        val obj = try {
            JsonParser.parseString(body).asJsonObject
        } catch (e: Exception) {
            throw KoreaderSyncException("Malformed progress response")
        }
        return KoreaderRemoteProgress(
            document = obj.stringOrNull("document"),
            percentage = obj.doubleOrNull("percentage"),
            progress = obj.stringOrNull("progress"),
            device = obj.stringOrNull("device"),
            deviceId = obj.stringOrNull("device_id"),
            timestamp = obj.longOrNull("timestamp")
        )
    }

    private fun JsonObject.stringOrNull(name: String): String? {
        val value = get(name) ?: return null
        if (!value.isJsonPrimitive) return null
        return runCatching { value.asJsonPrimitive.asString }.getOrNull()
    }

    private fun JsonObject.doubleOrNull(name: String): Double? {
        val value = get(name) ?: return null
        if (!value.isJsonPrimitive) return null
        return runCatching { value.asJsonPrimitive.asDouble }.getOrNull()
    }

    private fun JsonObject.longOrNull(name: String): Long? {
        val value = get(name) ?: return null
        if (!value.isJsonPrimitive) return null
        return runCatching { value.asJsonPrimitive.asLong }.getOrNull()
    }

    companion object {
        private const val USERS = "users"
        private const val AUTH = "auth"
        private const val SYNCS = "syncs"
        private const val PROGRESS = "progress"
        private const val HTTP_OK = 200
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_NOT_FOUND = 404
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
