package com.jdluu.leafline.sync

import java.net.URI

/**
 * One line that answers "is sync on, and to what" for the settings row, so the
 * reader does not have to open the form to find out.
 *
 * The host is shown rather than the full URL: the path identifies the API, not
 * the server, and it is the server the reader recognises. A value that is not a
 * parseable URL falls back to itself rather than disappearing.
 */
internal fun koreaderSyncSummary(config: KoreaderSyncConfig?): String {
    if (config == null || config.serverUrl.isBlank()) return "Not set up"
    val url = config.serverUrl.trim()
    val host = runCatching { URI(url) }.getOrNull()?.host
    val where = (host ?: url).removePrefix("www.")
    return if (config.enabled) "On \u00B7 $where" else "Off \u00B7 $where saved"
}
