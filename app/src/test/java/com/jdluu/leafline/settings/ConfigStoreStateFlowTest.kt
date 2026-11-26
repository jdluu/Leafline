package com.jdluu.leafline.settings

import app.cash.turbine.test
import com.jdluu.leafline.hardcover.HardcoverConfig
import com.jdluu.leafline.hardcover.HardcoverConfigStore
import com.jdluu.leafline.sync.KoreaderSyncConfig
import com.jdluu.leafline.sync.KoreaderSyncConfigStore
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ConfigStoreStateFlowTest {

    @Before
    fun resetStores() {
        HardcoverConfigStore.config = null
        KoreaderSyncConfigStore.config = null
    }

    @After
    fun cleanUpStores() {
        HardcoverConfigStore.config = null
        KoreaderSyncConfigStore.config = null
    }

    @Test
    fun `HardcoverConfigStore state mirrors config updates reactively`() = runTest {
        HardcoverConfigStore.state.test {
            assertNull(awaitItem())

            val first = HardcoverConfig.apiToken(token = "tok-1", userId = 42, enabled = true)
            HardcoverConfigStore.config = first
            assertEquals(first, awaitItem())
            assertEquals(first, HardcoverConfigStore.config)

            HardcoverConfigStore.config = null
            assertNull(awaitItem())
        }
    }

    @Test
    fun `KoreaderSyncConfigStore state mirrors config updates reactively`() = runTest {
        KoreaderSyncConfigStore.state.test {
            assertNull(awaitItem())

            val next = KoreaderSyncConfig(
                serverUrl = "https://sync.example.com",
                username = "reader",
                password = "secret",
                enabled = true
            )
            KoreaderSyncConfigStore.config = next
            assertEquals(next, awaitItem())
            assertEquals(next, KoreaderSyncConfigStore.config)

            val disabled = next.copy(enabled = false)
            KoreaderSyncConfigStore.config = disabled
            assertEquals(disabled, awaitItem())
        }
    }
}
