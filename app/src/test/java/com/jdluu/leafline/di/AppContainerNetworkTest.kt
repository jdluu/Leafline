package com.jdluu.leafline.di

import androidx.test.core.app.ApplicationProvider
import com.jdluu.leafline.hardcover.HardcoverClient
import com.jdluu.leafline.opds.OpdsCoverLoader
import com.jdluu.leafline.sync.KoreaderSyncClient
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppContainerNetworkTest {

    @Test
    fun `network clients share one OkHttp connection pool and dispatcher`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val container = AppContainer(context)
        val shared = container.httpClient

        val opds = container.opdsClient
        val hardcover = container.hardcoverApi as HardcoverClient
        val koreader = container.koreaderSyncApi as KoreaderSyncClient

        assertSame(shared.connectionPool, opds.okHttpClient.connectionPool)
        assertSame(shared.dispatcher, opds.okHttpClient.dispatcher)

        assertSame(shared.connectionPool, hardcover.okHttpClient.connectionPool)
        assertSame(shared.dispatcher, hardcover.okHttpClient.dispatcher)

        assertSame(shared.connectionPool, koreader.httpClient.connectionPool)
        assertSame(shared.dispatcher, koreader.httpClient.dispatcher)

        assertSame(shared, OpdsCoverLoader.currentHttpClientForTesting())

        container.hardcoverOAuthClient?.let { oauth ->
            assertSame(shared.connectionPool, oauth.okHttpClient.connectionPool)
            assertSame(shared.dispatcher, oauth.okHttpClient.dispatcher)
        }
    }

    @Test
    fun `coverLoader is lazily reused from AppContainer`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val container = AppContainer(context)

        assertSame(container.coverLoader, container.coverLoader)
    }
}
