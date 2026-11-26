package com.jdluu.leafline.reader

import com.jdluu.leafline.export.BookExportData
import com.jdluu.leafline.library.data.Annotation
import com.jdluu.leafline.library.data.Bookmark
import com.jdluu.leafline.reader.font.CustomFont
import com.jdluu.leafline.reader.footnotes.FootnoteContent
import com.jdluu.leafline.reader.navigation.ReaderScrubberHelper
import com.jdluu.leafline.reader.ui.SyncConflictState
import com.jdluu.leafline.reader.ui.matchesChapter
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator

/**
 * Derived chapter navigation state used by the reader scrubber and chapter-skip buttons.
 */
data class ChapterNavigationState(
    val chapterItems: List<Link> = emptyList(),
    val currentChapterIndex: Int = -1
) {
    val canSkipPrevious: Boolean
        get() = ReaderScrubberHelper.canSkipPrevious(currentChapterIndex)

    val canSkipNext: Boolean
        get() = ReaderScrubberHelper.canSkipNext(currentChapterIndex, chapterItems.size)

    val chapterTitles: List<String>
        get() = chapterItems.map { it.title ?: "" }
}

/**
 * Immutable UI state for [com.jdluu.leafline.ReaderActivity].
 */
data class ReaderUiState(
    val isLoadingPublication: Boolean = true,
    val bookStableId: String? = null,
    val bookTitle: String = "",
    val bookAuthor: String? = null,
    val currentChapterTitle: String? = null,
    val tocLinks: List<Pair<Link, Int>> = emptyList(),
    val readingOrder: List<Link> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val annotations: List<Annotation> = emptyList(),
    val currentLocation: Locator? = null,
    val currentLocatorJson: String? = null,
    val toolbarVisible: Boolean = false,
    val isDrawerOpen: Boolean = false,
    val settingsSheetVisible: Boolean = false,
    val bookmarkSheetVisible: Boolean = false,
    val highlightsSheetVisible: Boolean = false,
    val highlightTintSheetVisible: Boolean = false,
    val searchSheetVisible: Boolean = false,
    val exportSheetVisible: Boolean = false,
    val exportSheetData: BookExportData? = null,
    val activeFootnote: FootnoteContent? = null,
    val syncConflict: SyncConflictState? = null,
    val pageAnnouncement: String? = null,
    val customFonts: List<CustomFont> = emptyList()
) {
    /**
     * True when the Compose overlay must intercept touch events instead of
     * passing them through to Readium's EPUB viewport.
     */
    val isOverlayInteractive: Boolean
        get() = isLoadingPublication ||
            toolbarVisible ||
            settingsSheetVisible ||
            bookmarkSheetVisible ||
            highlightsSheetVisible ||
            exportSheetVisible ||
            searchSheetVisible ||
            highlightTintSheetVisible ||
            syncConflict != null ||
            isDrawerOpen

    val currentChapterHref: String?
        get() = currentLocation?.href?.toString()

    fun resolveLoadingTitle(fallbackTitle: String?): String? =
        bookTitle.takeUnless { it.isBlank() || it == "Reading" } ?: fallbackTitle

    fun resolveCurrentChapterTitle(): String? =
        currentLocation?.title
            ?: tocLinks.firstOrNull { (link, depth) ->
                depth == 0 && link.href.toString() == currentChapterHref
            }?.first?.title

    fun resolveChapterNavigation(): ChapterNavigationState {
        val items = if (tocLinks.isNotEmpty()) tocLinks.map { it.first } else readingOrder
        val href = currentChapterHref
        val idx = if (href != null) {
            items.indexOfFirst { matchesChapter(it.href.toString(), href) }
        } else {
            -1
        }
        return ChapterNavigationState(chapterItems = items, currentChapterIndex = idx)
    }
}
