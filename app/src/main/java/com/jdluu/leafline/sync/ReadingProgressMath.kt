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
}
