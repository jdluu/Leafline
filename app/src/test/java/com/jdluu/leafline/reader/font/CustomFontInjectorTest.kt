package com.jdluu.leafline.reader.font

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomFontInjectorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun buildInjectionScriptReturnsNullForEmptyOrMissingFonts() {
        assertNull(CustomFontInjector.buildInjectionScript(emptyList()))
        val missing = CustomFont(
            name = "Missing",
            fileName = "missing.ttf",
            file = File(tempFolder.root, "missing.ttf")
        )
        assertNull(CustomFontInjector.buildInjectionScript(listOf(missing)))
    }

    @Test
    fun buildInjectionScriptCachesAndInvalidatesOnFontFileChange() {
        val fontFile = tempFolder.newFile("custom.ttf").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }
        val font = CustomFont(
            name = "My Serif",
            fileName = "custom.ttf",
            file = fontFile
        )

        val first = CustomFontInjector.buildInjectionScript(listOf(font))
        val second = CustomFontInjector.buildInjectionScript(listOf(font))
        assertSame(first, second)

        fontFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6))
        val updated = CustomFontInjector.buildInjectionScript(listOf(font))
        assertNotEquals(first, updated)
        assertEquals(CustomFontInjector.buildInjectionScript(listOf(font)), updated)
    }
}
