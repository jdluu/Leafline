package com.jdluu.leafline.reader.tts

import java.util.Locale

/**
 * Selectable sleep timer durations for text-to-speech reading mode.
 */
enum class TtsSleepOption(val label: String, val minutes: Int? = null) {
    OFF("Off", null),
    MINUTES_15("15 min", 15),
    MINUTES_30("30 min", 30),
    MINUTES_45("45 min", 45),
    MINUTES_60("60 min", 60),
    END_OF_CHAPTER("End of chapter", null);

    val isTimed: Boolean get() = minutes != null
}

/**
 * Pure Kotlin representation of the active TTS sleep timer state.
 *
 * @param option The selected sleep option (off, timed, or end of chapter).
 * @param remainingSeconds Seconds remaining until auto-pause for timed options.
 * @param targetChapterHref Normalized chapter href being tracked for [TtsSleepOption.END_OF_CHAPTER].
 */
data class TtsSleepState(
    val option: TtsSleepOption = TtsSleepOption.OFF,
    val remainingSeconds: Long? = null,
    val targetChapterHref: String? = null
) {
    val isActive: Boolean get() = option != TtsSleepOption.OFF
}

/**
 * Formats the sleep timer display label for UI chips and status indicators.
 *
 * - Off -> "Timer"
 * - End of chapter -> "End ch."
 * - Timed -> "MM:SS" (or "H:MM:SS" if >= 60 minutes)
 */
fun formatSleepTimerLabel(state: TtsSleepState): String {
    if (!state.isActive) return "Timer"
    if (state.option == TtsSleepOption.END_OF_CHAPTER) return "End ch."

    val seconds = state.remainingSeconds ?: return "${state.option.minutes ?: 0}m"
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins >= 60) {
        val hours = mins / 60
        val remMins = mins % 60
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, remMins, secs)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", mins, secs)
    }
}

/**
 * Normalizes a publication resource href for reliable chapter comparison by
 * stripping query parameters, URI fragments, and leading slashes.
 */
fun normalizeChapterHref(href: String?): String? {
    if (href == null) return null
    return href.substringBefore('#').substringBefore('?').trimStart('/')
}
