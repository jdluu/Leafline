package com.jdluu.leafline.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jdluu.leafline.export.BookExportData
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.data.Annotation
import com.jdluu.leafline.library.data.Bookmark
import com.jdluu.leafline.reader.font.CustomFont
import com.jdluu.leafline.reader.footnotes.FootnoteContent
import com.jdluu.leafline.reader.ui.SyncConflictState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator

/**
 * Holds UI chrome, sheet visibility, publication metadata, bookmarks, annotations,
 * and navigation state for [com.jdluu.leafline.ReaderActivity].
 */
class ReaderViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    fun applyLaunchExtras(
        showToolbar: Boolean,
        showHighlights: Boolean,
        showBookmarks: Boolean,
        bookId: String?
    ) {
        _uiState.update { state ->
            state.copy(
                toolbarVisible = showToolbar || showHighlights || showBookmarks || state.toolbarVisible,
                highlightsSheetVisible = showHighlights || state.highlightsSheetVisible,
                bookmarkSheetVisible = showBookmarks || state.bookmarkSheetVisible,
                bookStableId = bookId ?: state.bookStableId
            )
        }
    }

    fun setBookStableId(bookId: String?) {
        _uiState.update { it.copy(bookStableId = bookId) }
    }

    fun setLoadingPublication(loading: Boolean) {
        _uiState.update { it.copy(isLoadingPublication = loading) }
    }

    fun onPublicationLoaded(
        bookStableId: String?,
        bookTitle: String,
        bookAuthor: String?,
        tableOfContents: List<Link>,
        readingOrder: List<Link>
    ) {
        val flattenedToc = flattenToc(tableOfContents)
        _uiState.update {
            it.copy(
                bookStableId = bookStableId,
                bookTitle = bookTitle,
                bookAuthor = bookAuthor,
                tocLinks = flattenedToc,
                readingOrder = readingOrder,
                isLoadingPublication = false
            )
        }
    }

    fun updateLocation(locator: Locator?, locatorJson: String?) {
        _uiState.update {
            it.copy(
                currentLocation = locator,
                currentLocatorJson = locatorJson
            )
        }
    }

    fun setBookmarks(bookmarks: List<Bookmark>) {
        _uiState.update { it.copy(bookmarks = bookmarks) }
    }

    fun setAnnotations(annotations: List<Annotation>) {
        _uiState.update { it.copy(annotations = annotations) }
    }

    fun setCustomFonts(fonts: List<CustomFont>) {
        _uiState.update { it.copy(customFonts = fonts) }
    }

    fun setCurrentChapterTitle(title: String?) {
        _uiState.update { it.copy(currentChapterTitle = title) }
    }

    fun updateTtsChapterForUtterance(utteranceHref: String, fallbackBookTitle: String?): String? {
        val state = _uiState.value
        val chapterTitle = state.tocLinks.find { it.first.href.toString() == utteranceHref }?.first?.title
            ?: fallbackBookTitle
            ?: state.bookTitle.ifBlank { null }
        setCurrentChapterTitle(chapterTitle)
        return chapterTitle
    }

    fun setToolbarVisible(visible: Boolean) {
        _uiState.update { it.copy(toolbarVisible = visible) }
    }

    fun toggleToolbar() {
        _uiState.update { it.copy(toolbarVisible = !it.toolbarVisible) }
    }

    fun setDrawerOpen(open: Boolean) {
        _uiState.update { it.copy(isDrawerOpen = open) }
    }

    fun setSettingsSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(settingsSheetVisible = visible) }
    }

    fun setBookmarkSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(bookmarkSheetVisible = visible) }
    }

    fun setHighlightsSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(highlightsSheetVisible = visible) }
    }

    fun setHighlightTintSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(highlightTintSheetVisible = visible) }
    }

    fun setSearchSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(searchSheetVisible = visible) }
    }

    fun onAnnotationNavigated() {
        _uiState.update {
            it.copy(
                highlightsSheetVisible = false,
                toolbarVisible = false
            )
        }
    }

    fun onBookmarkSheetClosed() {
        _uiState.update {
            it.copy(
                bookmarkSheetVisible = false,
                toolbarVisible = false
            )
        }
    }

    fun onSearchResultNavigated() {
        _uiState.update {
            it.copy(
                searchSheetVisible = false,
                toolbarVisible = false
            )
        }
    }

    fun showExportSheet(
        selectedAnnotations: List<Annotation>,
        currentBook: LibraryBook?,
        fallbackAuthorNames: List<String>,
        title: String
    ) {
        val state = _uiState.value
        val book = currentBook ?: LibraryBook(
            stableId = state.bookStableId ?: "",
            title = title,
            authors = fallbackAuthorNames
        )
        _uiState.update {
            it.copy(
                exportSheetData = BookExportData(
                    book = book,
                    annotations = selectedAnnotations,
                    collections = emptyList()
                ),
                exportSheetVisible = true
            )
        }
    }

    fun dismissExportSheet() {
        _uiState.update {
            it.copy(
                exportSheetVisible = false,
                exportSheetData = null
            )
        }
    }

    fun setActiveFootnote(footnote: FootnoteContent?) {
        _uiState.update { it.copy(activeFootnote = footnote) }
    }

    fun setSyncConflict(conflict: SyncConflictState?) {
        _uiState.update { it.copy(syncConflict = conflict) }
    }

    fun setPageAnnouncement(announcement: String?) {
        _uiState.update { it.copy(pageAnnouncement = announcement) }
    }

    companion object {
        fun flattenToc(links: List<Link>, depth: Int = 0): List<Pair<Link, Int>> {
            val result = mutableListOf<Pair<Link, Int>>()
            for (link in links) {
                result.add(link to depth)
                if (link.children.isNotEmpty()) {
                    result.addAll(flattenToc(link.children, depth + 1))
                }
            }
            return result
        }
    }
}

class ReaderViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReaderViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
