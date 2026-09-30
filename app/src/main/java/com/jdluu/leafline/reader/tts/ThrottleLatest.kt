package com.jdluu.leafline.reader.tts

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Throttles flow emissions to at most one item per [period].
 *
 * The first item is emitted immediately. Any items received during the [period]
 * are buffered, and the latest one is emitted when the period elapses.
 */
fun <T> Flow<T>.throttleLatest(period: Duration): Flow<T> = channelFlow {
    var lastValue: T? = null
    var hasValue = false
    var throttleJob: Job? = null
    val delayMs = period.inWholeMilliseconds

    collect { value ->
        lastValue = value
        hasValue = true
        if (throttleJob?.isActive != true) {
            send(value)
            hasValue = false
            throttleJob = launch {
                delay(delayMs)
                if (hasValue) {
                    @Suppress("UNCHECKED_CAST")
                    send(lastValue as T)
                    hasValue = false
                }
            }
        }
    }
}
