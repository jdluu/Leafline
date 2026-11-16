package com.jdluu.leafline.reader.font

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class FontStoreTest {

    private lateinit var context: Context
    private lateinit var fontStore: FontStore

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val fontsDir = File(context.filesDir, "fonts")
        fontsDir.deleteRecursively()
        fontStore = FontStore(context)
    }

    @Test
    fun `getCustomFonts is empty initially`() {
        assertTrue(fontStore.getCustomFonts().isEmpty())
    }

    @Test
    fun `deleteFont removes font and metadata`() {
        val fontsDir = File(context.filesDir, "fonts").apply { mkdirs() }
        val fontFile = File(fontsDir, "test.ttf").apply { writeText("dummy font data") }

        val font = CustomFont("Test Font", "test.ttf", fontFile)
        assertTrue(fontStore.deleteFont(font))
        assertTrue(!fontFile.exists())
        assertTrue(fontStore.getCustomFonts().isEmpty())
    }

    @Test
    fun `findByName matches case insensitively`() {
        val fontsDir = File(context.filesDir, "fonts").apply { mkdirs() }
        val fontFile = File(fontsDir, "bookerly.ttf").apply { writeText("dummy") }

        val found = fontStore.findByName("bookerly")
        assertNotNull(found)
        assertEquals("bookerly", found?.name)
    }
}
