package com.jdluu.leafline.reader

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the reader settings state and the submission and persistence order the
 * settings sheet and quick controls rely on. UI reads [settings] and drives
 * transitions through [submit], [submitBrightness], and [toggleSepia].
 *
 * The Readium navigator and window brightness calls stay behind injected
 * callbacks: mapping the pure [com.jdluu.leafline.reader.theme.ReaderTheme]
 * onto the Readium theme touches types whose static initializers need an
 * Android runtime, and the window lives on the activity. Keeping those at the
 * Android boundary leaves this class plain-JVM testable.
 */
class ReaderSettingsController(
    initialSettings: ReaderSettings,
    private val submitToNavigator: (ReaderSettings) -> Unit,
    private val applyWindowBrightness: (Float?) -> Unit,
    private val save: (ReaderSettings) -> Unit
) {

    private val _settings = MutableStateFlow(initialSettings)

    /** Current reader settings, observed by the overlay UI. */
    val settings: StateFlow<ReaderSettings> = _settings.asStateFlow()

    /**
     * Applies [settings] as the current reader settings: updates state, hands
     * them to the navigator, then persists them.
     */
    fun submit(settings: ReaderSettings) {
        _settings.value = settings
        submitToNavigator(settings)
        save(settings)
    }

    /**
     * Applies a brightness override to the current settings and the window,
     * then persists. A null value restores the system default.
     */
    fun submitBrightness(value: Float?) {
        val updated = _settings.value.withBrightness(value)
        _settings.value = updated
        applyWindowBrightness(updated.brightness)
        save(updated)
    }

    /** Toggles the sepia quick control and routes the result through [submit]. */
    fun toggleSepia() {
        submit(toggleSepia(_settings.value))
    }
}