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

/**
 * True when the text is an absolute http or https URL with a host, which is the
 * only shape the sync client can send a request to. Saving is gated on this so
 * the form never reports a state it did not reach: a stored config with no
 * usable address would leave the row reading "Not set up" after a save that
 * claimed to succeed.
 */
internal fun isKoreaderServerUrlValid(raw: String): Boolean {
    val url = raw.trim()
    if (url.isEmpty()) return false
    val uri = runCatching { URI(url) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase() ?: return false
    return (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
}
