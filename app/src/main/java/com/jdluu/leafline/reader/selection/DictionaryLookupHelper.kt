package com.jdluu.leafline.reader.selection

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build

/**
 * Pure helper for constructing and resolving dictionary lookup, share,
 * and text processing intents for reader text selections.
 *
 * Adheres strictly to Leafline's privacy and offline-first boundaries:
 * prioritizes installed offline dictionaries (ColorDict, GoldenDict, QuickDic)
 * and system definitions without making automatic network requests.
 */
object DictionaryLookupHelper {

    const val COLORDICT_ACTION = "colordict.intent.action.SEARCH"
    const val EXTRA_QUERY = "EXTRA_QUERY"
    const val EXTRA_FULLSCREEN = "EXTRA_FULLSCREEN"

    private val PUNCTUATION_CHARS = charArrayOf(
        '"', '\'', '“', '”', '‘', '’', '«', '»',
        '.', ',', ';', ':', '!', '?',
        '(', ')', '[', ']', '{', '}',
        '—', '–', '-', '…', '`'
    )

    /**
     * Strips leading and trailing punctuation and whitespace from selected text
     * so that word definitions are queried cleanly.
     */
    fun sanitizeQuery(rawText: String): String {
        val trimmed = rawText.trim()
        val cleaned = trimmed.trim(*PUNCTUATION_CHARS).trim()
        return cleaned.ifBlank { trimmed }
    }

    /**
     * Builds an intent compatible with the classic e-reader ColorDict standard,
     * supported by ColorDict, GoldenDict, Offline Dictionaries, QuickDic, and Aard2.
     */
    fun buildColorDictIntent(query: String): Intent {
        return Intent(COLORDICT_ACTION).apply {
            putExtra(EXTRA_QUERY, query)
            putExtra(Intent.EXTRA_TEXT, query)
            putExtra(EXTRA_FULLSCREEN, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Builds the standard Android 10+ (API 29+) definition intent.
     */
    fun buildDefineIntent(query: String): Intent {
        return Intent(Intent.ACTION_DEFINE).apply {
            putExtra(Intent.EXTRA_TEXT, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Builds a standard Android ACTION_PROCESS_TEXT intent (API 23+).
     */
    fun buildProcessTextIntent(text: String, readonly: Boolean = true): Intent {
        return Intent(Intent.ACTION_PROCESS_TEXT).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_PROCESS_TEXT, text)
            putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, readonly)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Builds a standard Android ACTION_SEND share intent.
     */
    fun buildShareIntent(text: String, title: String? = null): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            if (!title.isNullOrBlank()) {
                putExtra(Intent.EXTRA_TITLE, title)
            }
        }
    }

    /**
     * Builds a privacy-preserving web search fallback intent (DuckDuckGo definition search).
     * Only invoked upon explicit user interaction when no offline dictionary app exists.
     */
    fun buildWebSearchIntent(query: String): Intent {
        val url = "https://duckduckgo.com/?q=define+" + Uri.encode(query)
        return Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Resolves the best available dictionary or definition intent.
     * Order of preference:
     * 1. ColorDict-compatible offline dictionary
     * 2. Platform ACTION_DEFINE (API 29+)
     * 3. An installed ACTION_PROCESS_TEXT activity matching dictionary / definition keywords
     */
    fun resolveDictionaryIntent(pm: PackageManager, query: String): Intent? {
        // 1. ColorDict-compatible offline dictionaries
        val colorDictIntent = buildColorDictIntent(query)
        if (pm.resolveActivity(colorDictIntent, 0) != null) {
            return colorDictIntent
        }

        // 2. Android 10+ ACTION_DEFINE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val defineIntent = buildDefineIntent(query)
            if (pm.resolveActivity(defineIntent, 0) != null) {
                return defineIntent
            }
        }

        // 3. Installed text processing app specifically matching dictionary keywords
        val baseProcessText = buildProcessTextIntent(query)
        val activities = pm.queryIntentActivities(baseProcessText, 0)
        val dictActivity = activities.firstOrNull { info ->
            val label = info.loadLabel(pm).toString().lowercase()
            val pkg = info.activityInfo.packageName.lowercase()
            label.contains("dict") || label.contains("define") || label.contains("lexicon") ||
                label.contains("lookup") || pkg.contains("dict") || pkg.contains("define")
        }

        if (dictActivity != null) {
            return Intent(baseProcessText).apply {
                setClassName(dictActivity.activityInfo.packageName, dictActivity.activityInfo.name)
            }
        }

        return null
    }

    /**
     * Queries available ACTION_PROCESS_TEXT activities on the system, excluding Leafline itself.
     */
    fun findProcessTextActivities(pm: PackageManager, callingPackage: String = ""): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            type = "text/plain"
        }
        return pm.queryIntentActivities(intent, 0).filter { info ->
            info.activityInfo.packageName != callingPackage
        }
    }
}
