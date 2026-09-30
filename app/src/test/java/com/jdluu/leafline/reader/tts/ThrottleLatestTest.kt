package com.jdluu.leafline.reader.tts

import app.cash.turbine.test
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class ThrottleLatestTest {

    @Test
    fun `emits first value immediately`() = runTest {
        val source = flow {
            emit("first")
        }

        source.throttleLatest(100.milliseconds).test {
            assertEquals("first", awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `throttles intermediate emissions within period and emits latest trailing value`() = runTest {
        val source = flow {
            emit("item-1")
            kotlinx.coroutines.delay(20)
            emit("item-2")
            kotlinx.coroutines.delay(20)
            emit("item-3")
            kotlinx.coroutines.delay(120)
        }

        source.throttleLatest(100.milliseconds).test {
            assertEquals("item-1", awaitItem())
            assertEquals("item-3", awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits subsequent values after period has elapsed`() = runTest {
        val source = flow {
            emit("item-1")
            kotlinx.coroutines.delay(150)
            emit("item-2")
        }

        source.throttleLatest(100.milliseconds).test {
            assertEquals("item-1", awaitItem())
            assertEquals("item-2", awaitItem())
            awaitComplete()
        }
    }
}
