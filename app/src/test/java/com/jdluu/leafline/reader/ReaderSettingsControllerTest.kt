package com.jdluu.leafline.reader

import app.cash.turbine.test
import com.jdluu.leafline.reader.theme.ReaderTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * Plain JVM coverage of [ReaderSettingsController]: it owns the settings
 * state flow and fixes the submission and persistence order, while the
 * navigator, window, and storage calls are injected callbacks recorded here.
 */
@OptIn(ExperimentalReadiumApi::class)
class ReaderSettingsControllerTest {

    private class Harness(initial: ReaderSettings = ReaderSettings()) {
        val ordering = mutableListOf<String>()
        val submitted = mutableListOf<ReaderSettings>()
        val windowBrightness = mutableListOf<Float?>()
        val persisted = mutableListOf<ReaderSettings>()
        val controller = ReaderSettingsController(
            initialSettings = initial,
            submitToNavigator = { submitted += it; ordering += "navigator" },
            applyWindowBrightness = { windowBrightness += it; ordering += "window" },
            save = { persisted += it; ordering += "save" }
        )
    }

    @Test
    fun `initial settings are exposed by the state flow`() {
        val harness = Harness(initial = ReaderSettings(brightness = 0.42f))

        assertEquals(0.42f, harness.controller.settings.value.brightness!!)
    }

    @Test
    fun `submit updates state, submits to navigator, then persists the same settings`() {
        val harness = Harness()
        val target = ReaderSettings(theme = ReaderTheme.DARK)

        harness.controller.submit(target)

        assertEquals(target, harness.controller.settings.value)
        assertEquals(listOf("navigator", "save"), harness.ordering)
        assertEquals(listOf(target), harness.submitted)
        assertEquals(listOf(target), harness.persisted)
    }

    @Test
    fun `submitBrightness clamps out of range values before applying`() = runTest {
        val harness = Harness()
        harness.controller.settings.test {
            assertEquals(ReaderSettings(), awaitItem())

            harness.controller.submitBrightness(7f)

            assertEquals(BRIGHTNESS_MAX, awaitItem().brightness!!)
        }

        assertEquals(listOf(BRIGHTNESS_MAX), harness.windowBrightness)
        assertEquals(BRIGHTNESS_MAX, harness.persisted.single().brightness!!)
    }

    @Test
    fun `submitBrightness lifts values below the floor to brightness minimum`() {
        val harness = Harness(initial = ReaderSettings(brightness = 0.5f))

        harness.controller.submitBrightness(0f)

        assertEquals(BRIGHTNESS_MIN, harness.controller.settings.value.brightness!!)
        assertEquals(listOf(BRIGHTNESS_MIN), harness.windowBrightness)
        assertEquals(BRIGHTNESS_MIN, harness.persisted.single().brightness!!)
    }

    @Test
    fun `submitBrightness null restores the system default`() {
        val harness = Harness(initial = ReaderSettings(brightness = 0.5f))

        harness.controller.submitBrightness(null)

        assertNull(harness.controller.settings.value.brightness)
        assertEquals(listOf(null as Float?), harness.windowBrightness)
        assertNull(harness.persisted.single().brightness)
    }

    @Test
    fun `brightness transitions flow state, then window, then save`() {
        val harness = Harness()

        harness.controller.submitBrightness(0.42f)

        assertEquals(listOf("window", "save"), harness.ordering)
        assertEquals(listOf(0.42f), harness.windowBrightness)
        assertEquals(0.42f, harness.persisted.single().brightness!!)
    }

    @Test
    fun `toggleSepia engages and routes through submit`() {
        val harness = Harness(initial = ReaderSettings(theme = ReaderTheme.LIGHT))

        harness.controller.toggleSepia()

        assertEquals(ReaderTheme.SEPIA, harness.controller.settings.value.theme)
        assertEquals(ReaderTheme.LIGHT, harness.controller.settings.value.preSepiaTheme)
        assertEquals(listOf("navigator", "save"), harness.ordering)
        assertEquals(ReaderTheme.SEPIA, harness.persisted.single().theme)
    }

    @Test
    fun `toggleSepia disengage restores the remembered theme and clears the memory`() {
        val harness = Harness(initial = ReaderSettings(theme = ReaderTheme.SEPIA))

        harness.controller.toggleSepia()

        assertNull(harness.controller.settings.value.theme)
        assertNull(harness.controller.settings.value.preSepiaTheme)
        assertNull(harness.persisted.single().theme)
    }
}