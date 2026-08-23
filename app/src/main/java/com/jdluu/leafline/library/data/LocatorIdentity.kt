package com.jdluu.leafline.library.data

import com.google.gson.JsonParser

/**
 * Derives a stable identity for a Readium locator JSON payload.
 *
 * A locator's `text` excerpt and `title` change with tiny scroll adjustments,
 * so they are excluded from the identity. Two locators are considered the same
 * position when they share an `href` and a `locations` object.
 */
object LocatorIdentity {

    fun key(locatorJson: String): String {
        return try {
            val obj = JsonParser.parseString(locatorJson).asJsonObject
            val href = obj.get("href")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
            val locations = obj.get("locations")?.takeIf { it.isJsonObject }?.toString().orEmpty()
            "$href|$locations"
        } catch (e: Exception) {
            locatorJson
        }
    }

    fun displayTitle(locatorJson: String): String? {
        return try {
            val obj = JsonParser.parseString(locatorJson).asJsonObject
            obj.get("title")?.takeIf { it.isJsonPrimitive }?.asString
                ?: obj.get("href")?.takeIf { it.isJsonPrimitive }?.asString
                    ?.substringAfterLast('/')?.substringBefore('#')
        } catch (e: Exception) {
            null
        }
    }
}
