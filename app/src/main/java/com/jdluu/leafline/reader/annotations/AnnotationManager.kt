package com.jdluu.leafline.reader.annotations

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.jdluu.leafline.library.data.Annotation
import com.jdluu.leafline.library.data.AnnotationRepository
import com.jdluu.leafline.library.data.LocatorIdentity
import com.jdluu.leafline.theme.DEFAULT_HIGHLIGHT_TINT
import com.jdluu.leafline.theme.HighlightTint
import kotlinx.coroutines.launch
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.epub.EpubNavigatorFragment

/**
 * Owns highlight/annotation behavior in the reader: the text-selection
 * action mode, saving highlights with a tint, copying selection text,
 * rendering annotation decorations, and navigating to annotations.
 *
 * The activity supplies navigator access, repository, scope, and UI-effect
 * lambdas so this class holds no Activity reference.
 */
class AnnotationManager(
    private val context: Context,
    private val scope: LifecycleCoroutineScope,
    private val repository: AnnotationRepository,
    private val navigatorProvider: () -> EpubNavigatorFragment?,
    private val bookStableIdProvider: () -> String?,
    private val annotationsProvider: () -> List<Annotation>,
    private val onPendingHighlightChanged: (String?) -> Unit,
    private val onShowHighlightTintSheet: () -> Unit,
    private val onSnackbar: (String) -> Unit,
    private val onAnnotationNavigated: () -> Unit,
) {

    fun selectionActionMode(): ActionMode.Callback {
        return object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                menu.add(Menu.NONE, MENU_ITEM_HIGHLIGHT_ID, Menu.NONE, "Highlight")
                menu.add(Menu.NONE, MENU_ITEM_COPY_ID, Menu.NONE, "Copy")
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                return when (item.itemId) {
                    MENU_ITEM_HIGHLIGHT_ID -> {
                        saveSelectionAsHighlight()
                        mode.finish()
                        true
                    }
                    MENU_ITEM_COPY_ID -> {
                        copySelectedText()
                        mode.finish()
                        true
                    }
                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode) = Unit
        }
    }

    fun saveSelectionAsHighlight() {
        val stableId = bookStableIdProvider()
        if (stableId == null) {
            Toast.makeText(context, "Highlights need an imported library book", Toast.LENGTH_SHORT)
                .show()
            return
        }
        scope.launch {
            try {
                val selection = navigatorProvider()?.currentSelection()
                if (selection == null) {
                    Toast.makeText(context, "No text selected", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                // Capture the locator, clear selection, then show the tint picker
                onPendingHighlightChanged(selection.locator.toJSON().toString())
                navigatorProvider()?.clearSelection()
                onShowHighlightTintSheet()
            } catch (e: Exception) {
                Log.w(TAG, "Could not prepare highlight", e)
                runCatching { navigatorProvider()?.clearSelection() }
                Toast.makeText(context, "Could not create highlight", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun saveHighlightWithTint(tint: HighlightTint, pendingLocatorJson: String?) {
        val stableId = bookStableIdProvider() ?: return
        val locatorJson = pendingLocatorJson ?: return
        scope.launch {
            try {
                repository.addAnnotation(
                    bookId = stableId,
                    locatorJson = locatorJson,
                    colorHex = tint.hex
                )
                onPendingHighlightChanged(null)
                onSnackbar("Highlight added")
            } catch (e: Exception) {
                Log.w(TAG, "Could not save highlight", e)
                runCatching { navigatorProvider()?.clearSelection() }
                Toast.makeText(context, "Could not save highlight", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun copySelectedText() {
        scope.launch {
            val text = runCatching {
                navigatorProvider()?.currentSelection()?.locator?.let { selectedTextOf(it) }
            }.getOrNull().takeIf { !it.isNullOrBlank() }
            if (text == null) {
                navigatorProvider()?.clearSelection()
                return@launch
            }
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Selected text", text))
            navigatorProvider()?.clearSelection()
            onSnackbar("Copied to clipboard")
        }
    }

    private fun selectedTextOf(locator: org.readium.r2.shared.publication.Locator): String? {
        return try {
            locator.toJSON().optJSONObject("text")?.optString("exact")
                ?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    fun applyDecorations() {
        val navigator = navigatorProvider() ?: return
        if (!navigator.supportsDecorationStyle(Decoration.Style.Highlight::class)) return
        scope.launch {
            try {
                val decorations = annotationsProvider().mapNotNull { annotation ->
                    val locator = parseLocator(annotation.locatorJson) ?: return@mapNotNull null
                    Decoration(
                        id = "$ANNOTATION_DECORATION_PREFIX${annotation.id}",
                        locator = locator,
                        style = Decoration.Style.Highlight(tint = annotationTint(annotation.colorHex))
                    )
                }
                navigator.applyDecorations(decorations, ANNOTATION_DECORATION_GROUP)
            } catch (e: Exception) {
                Log.w(TAG, "Could not apply annotation decorations", e)
            }
        }
    }

    fun delete(annotation: Annotation) {
        scope.launch {
            try {
                repository.removeAnnotation(annotation.id)
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete annotation", e)
            }
        }
    }

    fun navigateTo(annotation: Annotation) {
        val locator = parseLocator(annotation.locatorJson)
        if (locator == null) {
            Toast.makeText(context, "Could not open this highlight", Toast.LENGTH_SHORT).show()
            return
        }
        navigatorProvider()?.go(locator, false)
        onAnnotationNavigated()
    }

    /** Excerpt shown in the annotation-activated toast and highlights sheet. */
    fun excerptOf(annotation: Annotation): String {
        return try {
            val text = org.json.JSONObject(annotation.locatorJson).optJSONObject("text")
            text?.optString("exact")?.takeIf { it.isNotBlank() }
                ?: LocatorIdentity.displayTitle(annotation.locatorJson)
                ?: "Highlight"
        } catch (e: Exception) {
            LocatorIdentity.displayTitle(annotation.locatorJson) ?: "Highlight"
        }
    }

    companion object {
        private const val TAG = "AnnotationManager"
        internal const val ANNOTATION_DECORATION_GROUP = "leafline-annotations"
        internal const val ANNOTATION_DECORATION_PREFIX = "annotation-"
        private const val DEFAULT_ANNOTATION_TINT = 0x55E65100.toInt()
        private const val MENU_ITEM_HIGHLIGHT_ID = 1
        private const val MENU_ITEM_COPY_ID = 2

        fun annotationTint(colorHex: String): Int {
            return try {
                Color.parseColor(colorHex)
            } catch (e: IllegalArgumentException) {
                DEFAULT_ANNOTATION_TINT
            }
        }

        fun parseLocator(json: String): org.readium.r2.shared.publication.Locator? {
            return try {
                org.readium.r2.shared.publication.Locator.fromJSON(org.json.JSONObject(json))
            } catch (e: Exception) {
                Log.w(TAG, "Could not parse annotation locator", e)
                null
            }
        }
    }
}
