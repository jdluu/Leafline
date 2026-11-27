package com.jdluu.leafline.opds

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Enqueues an OkHttp [Call] and cancels it if the suspending coroutine is cancelled.
 */
internal suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            if (continuation.isActive) {
                continuation.resume(response)
            } else {
                response.close()
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) {
                continuation.resumeWithException(e)
            }
        }
    })
}

/**
 * Executes an OkHttp [Call] cancellably across both response wait and body streaming.
 */
internal suspend fun <T> Call.executeCancellable(block: suspend (Response) -> T): T = coroutineScope {
    var completed = false
    val cancellationWatcher = launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            awaitCancellation()
        } finally {
            if (!completed) {
                cancel()
            }
        }
    }
    try {
        awaitResponse().use { response ->
            coroutineContext.ensureActive()
            block(response)
        }.also {
            completed = true
        }
    } finally {
        completed = true
        cancellationWatcher.cancel()
    }
}
