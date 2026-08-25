package com.jdluu.leafline.reader.bookmarks

import android.util.Log
import com.jdluu.leafline.reader.Bookmark
import com.jdluu.leafline.reader.BookmarkRepository
import com.jdluu.leafline.reader.BookmarkToggleResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Manages bookmark CRUD operations for the reader.
 *
 * Does not own Android lifecycle — callers pass a [CoroutineScope] and
 * the navigator/state lambdas so this class stays testable.
 */
class BookmarkManager(
    private val scope: CoroutineScope,
    private val repository: BookmarkRepository,
    private val navigatorLocator: () -> String?,
    private val onBookmarkToggled: (String) -> Unit = {}
) {

    fun toggle(bookStableId: String?) {
        val stableId = bookStableId ?: return
        val locatorJson = navigatorLocator() ?: return
        scope.launch {
            try {
                val result = repository.toggleBookmark(
                    bookId = stableId,
                    locatorJson = locatorJson
                )
                onBookmarkToggled(
                    when (result) {
                        is BookmarkToggleResult.Added -> "Bookmark added"
                        is BookmarkToggleResult.Removed -> "Bookmark removed"
                    }
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not toggle bookmark", e)
            }
        }
    }

    fun delete(bookmark: Bookmark) {
        scope.launch {
            try {
                repository.removeBookmark(bookmark.id)
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete bookmark", e)
            }
        }
    }

    companion object {
        private const val TAG = "BookmarkManager"
    }
}