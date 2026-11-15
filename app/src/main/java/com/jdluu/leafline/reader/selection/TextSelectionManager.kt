package com.jdluu.leafline.reader.selection

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Locator

/**
 * Manages text-selection actions in the reader WebView/Navigator.
 *
 * Provides a clean ActionMode context menu containing:
 * - Highlight (saves selection as an annotation)
 * - Copy (copies selection to system clipboard)
 * - Define (looks up word definition via offline dictionaries like ColorDict/GoldenDict/QuickDic,
 *   Android 10+ ACTION_DEFINE, or process text apps)
 * - Share (shares selected text via standard Android share sheet)
 * - Dynamic system ACTION_PROCESS_TEXT actions (Translate, Wikipedia, etc. placed in overflow)
 */
class TextSelectionManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val navigatorProvider: () -> EpubNavigatorFragment?,
    private val onHighlight: () -> Unit,
    private val onSnackbar: (String) -> Unit,
    private val onSnackbarWithAction: ((message: String, actionLabel: String, onAction: () -> Unit) -> Unit)? = null,
    private val bookTitleProvider: (() -> String?)? = null,
) {

    companion object {
        private const val TAG = "TextSelectionManager"

        const val GROUP_PRIMARY = 0
        const val GROUP_PROCESS_TEXT = 1

        const val MENU_ITEM_HIGHLIGHT_ID = 1
        const val MENU_ITEM_COPY_ID = 2
        const val MENU_ITEM_DEFINE_ID = 3
        const val MENU_ITEM_SHARE_ID = 4
        const val MENU_ITEM_PROCESS_TEXT_BASE = 100
    }

    /** Cached process text activities for the current action mode session */
    private var cachedProcessTextActivities: List<ResolveInfo> = emptyList()

    fun selectionActionMode(): ActionMode.Callback {
        return object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                menu.add(GROUP_PRIMARY, MENU_ITEM_HIGHLIGHT_ID, 1, "Highlight")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                menu.add(GROUP_PRIMARY, MENU_ITEM_COPY_ID, 2, "Copy")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                menu.add(GROUP_PRIMARY, MENU_ITEM_DEFINE_ID, 3, "Define")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                menu.add(GROUP_PRIMARY, MENU_ITEM_SHARE_ID, 4, "Share")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                try {
                    menu.removeGroup(GROUP_PROCESS_TEXT)
                    val pm = context.packageManager
                    val activities = DictionaryLookupHelper.findProcessTextActivities(pm, context.packageName)
                    cachedProcessTextActivities = activities

                    activities.forEachIndexed { index, info ->
                        val title = info.loadLabel(pm)
                        val item = menu.add(
                            GROUP_PROCESS_TEXT,
                            MENU_ITEM_PROCESS_TEXT_BASE + index,
                            10 + index,
                            title
                        )
                        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error populating process text activities", e)
                }
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                return when (item.itemId) {
                    MENU_ITEM_HIGHLIGHT_ID -> {
                        onHighlight()
                        mode.finish()
                        true
                    }
                    MENU_ITEM_COPY_ID -> {
                        copySelectedText()
                        mode.finish()
                        true
                    }
                    MENU_ITEM_DEFINE_ID -> {
                        lookupSelectedText()
                        mode.finish()
                        true
                    }
                    MENU_ITEM_SHARE_ID -> {
                        shareSelectedText()
                        mode.finish()
                        true
                    }
                    else -> {
                        if (item.groupId == GROUP_PROCESS_TEXT) {
                            val index = item.itemId - MENU_ITEM_PROCESS_TEXT_BASE
                            val info = cachedProcessTextActivities.getOrNull(index)
                            if (info != null) {
                                processTextWithActivity(info)
                            }
                            mode.finish()
                            true
                        } else {
                            false
                        }
                    }
                }
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                cachedProcessTextActivities = emptyList()
            }
        }
    }

    fun copySelectedText() {
        scope.launch {
            val text = runCatching {
                navigatorProvider()?.currentSelection()?.locator?.let { selectedTextOf(it) }
            }.getOrNull()?.trim()?.takeIf { it.isNotBlank() }

            navigatorProvider()?.clearSelection()

            if (text == null) {
                onSnackbar("No text selected")
                return@launch
            }

            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Selected text", text))
            onSnackbar("Copied to clipboard")
        }
    }

    fun lookupSelectedText() {
        scope.launch {
            val rawText = runCatching {
                navigatorProvider()?.currentSelection()?.locator?.let { selectedTextOf(it) }
            }.getOrNull()?.trim()?.takeIf { it.isNotBlank() }

            navigatorProvider()?.clearSelection()

            if (rawText == null) {
                onSnackbar("No text selected")
                return@launch
            }

            val query = DictionaryLookupHelper.sanitizeQuery(rawText)
            val pm = context.packageManager
            val resolvedIntent = DictionaryLookupHelper.resolveDictionaryIntent(pm, query)

            if (resolvedIntent != null) {
                try {
                    context.startActivity(resolvedIntent)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to launch dictionary intent", e)
                    onSnackbar("Could not open dictionary app")
                }
            } else {
                // Check if any PROCESS_TEXT activities exist as general fallback
                val allProcessText = DictionaryLookupHelper.findProcessTextActivities(pm, context.packageName)
                if (allProcessText.isNotEmpty()) {
                    val chooserIntent = Intent.createChooser(
                        DictionaryLookupHelper.buildProcessTextIntent(query),
                        "Define \"$query\""
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    try {
                        context.startActivity(chooserIntent)
                        return@launch
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to launch process text chooser", e)
                    }
                }

                // Privacy-first fallback: informative snackbar with optional user-directed search
                if (onSnackbarWithAction != null) {
                    onSnackbarWithAction.invoke(
                        "No dictionary app found (e.g. QuickDic or GoldenDict)",
                        "Search web"
                    ) {
                        try {
                            context.startActivity(DictionaryLookupHelper.buildWebSearchIntent(query))
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not open web search", e)
                        }
                    }
                } else {
                    onSnackbar("No dictionary app found. Install an offline dictionary like QuickDic or GoldenDict.")
                }
            }
        }
    }

    fun shareSelectedText() {
        scope.launch {
            val text = runCatching {
                navigatorProvider()?.currentSelection()?.locator?.let { selectedTextOf(it) }
            }.getOrNull()?.trim()?.takeIf { it.isNotBlank() }

            navigatorProvider()?.clearSelection()

            if (text == null) {
                onSnackbar("No text selected")
                return@launch
            }

            val bookTitle = bookTitleProvider?.invoke()
            val shareIntent = DictionaryLookupHelper.buildShareIntent(text, bookTitle)
            val chooser = Intent.createChooser(shareIntent, "Share text").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(chooser)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch share chooser", e)
                onSnackbar("Could not share text")
            }
        }
    }

    fun processTextWithActivity(info: ResolveInfo) {
        scope.launch {
            val text = runCatching {
                navigatorProvider()?.currentSelection()?.locator?.let { selectedTextOf(it) }
            }.getOrNull()?.trim()?.takeIf { it.isNotBlank() }

            navigatorProvider()?.clearSelection()

            if (text == null) {
                onSnackbar("No text selected")
                return@launch
            }

            val intent = DictionaryLookupHelper.buildProcessTextIntent(text).apply {
                setClassName(info.activityInfo.packageName, info.activityInfo.name)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch process text component", e)
                onSnackbar("Could not start action")
            }
        }
    }

    private fun selectedTextOf(locator: Locator): String? {
        return locator.text.highlight?.takeIf { it.isNotBlank() }
            ?: runCatching {
                val textObj = locator.toJSON().optJSONObject("text")
                textObj?.optString("highlight")?.takeIf { it.isNotBlank() }
                    ?: textObj?.optString("exact")?.takeIf { it.isNotBlank() }
            }.getOrNull()
    }
}
