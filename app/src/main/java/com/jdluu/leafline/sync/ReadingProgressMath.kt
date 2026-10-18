package com.jdluu.leafline.sync

import com.google.gson.JsonParser

/**
 * Derives a 0-100 percentage from a Readium locator JSON payload.
 *
 * Prefers `locations.totalProgression` (0-1 across the whole publication) and
 * falls back to `locations.progression` (progress inside the current
 * resource). Returns null when neither is present or the payload is invalid.
 */
object ReadingProgressMath {

    const val FULL_PERCENT = 100.0

    fun percentageFromLocator(locatorJson: String?): Double? {
        if (locatorJson.isNullOrBlank()) return null
        return try {
            val obj = JsonParser.parseString(locatorJson).asJsonObject
            val locations = obj.getAsJsonObject("locations") ?: return null
            val progression = locations.optionalProgression("totalProgression")
                ?: locations.optionalProgression("progression")
                ?: return null
            (progression.coerceIn(0.0, 1.0)) * FULL_PERCENT
        } catch (e: Exception) {
            null
        }
    }

    private fun com.google.gson.JsonObject.optionalProgression(name: String): Double? {
        val value = get(name) ?: return null
        if (!value.isJsonPrimitive) return null
        return runCatching { value.asJsonPrimitive.asDouble }.getOrNull()
    }

    /**
     * Returns a display label like "42% read" derived from a locator JSON
     * payload, or null when the locator is missing or invalid.
     */
    fun progressLabel(locatorJson: String?): String? {
        val percent = percentageFromLocator(locatorJson) ?: return null
        return "${Math.round(percent)}% read"
    }
    /**
     * Computes an estimated remaining reading time string like "~45 min left" or "~1h 20m left"
     * based on current progress percentage (0.0 to 100.0) and active reading time (millis).
     * Returns null if progress is missing, at/beyond 100%, or if reading duration is under 1 minute.
     */
    fun estimatedRemainingTimeLabel(
        currentPercent: Double?,
        activeDurationMillis: Long
    ): String? {
        if (currentPercent == null || currentPercent <= 0.0 || currentPercent >= FULL_PERCENT) return null
        if (activeDurationMillis < 60_000L) return null
        val remainingPercent = FULL_PERCENT - currentPercent
        val millisPerPercent = activeDurationMillis.toDouble() / currentPercent
        val remainingMillis = (remainingPercent * millisPerPercent).toLong()
        val remainingMinutes = remainingMillis / 60_000L
        return when {
            remainingMinutes < 1L -> "< 1 min left"
            remainingMinutes < 60L -> "~$remainingMinutes min left"
            else -> {
                val hours = remainingMinutes / 60L
                val mins = remainingMinutes % 60L
                if (mins == 0L) "~${hours}h left" else "~${hours}h ${mins}m left"
            }
        }
    }
}
