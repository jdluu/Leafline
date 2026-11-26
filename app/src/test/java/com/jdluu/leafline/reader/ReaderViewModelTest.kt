package com.jdluu.leafline.reader

import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.data.Annotation
import com.jdluu.leafline.library.data.Bookmark
import com.jdluu.leafline.reader.font.CustomFont
import com.jdluu.leafline.reader.footnotes.FootnoteContent
import com.jdluu.leafline.reader.ui.SyncConflictState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderViewModelTest {

    private lateinit var viewModel: ReaderViewModel

    @Before
    fun setUp() {
        viewModel = ReaderViewModel()
    }

    private fun link(
        href: String,
        title: String? = null,
        children: List<Link> = emptyList()
    ): Link = Link(
        href = Url(href)!!,
        title = title,
        children = children
    )

    private fun locator(href: String, title: String? = null): Locator = Locator(
        href = Url(href)!!,
        mediaType = MediaType.XHTML,
        title = title
    )

    @Test
    fun initialStateStartsLoadingAndInteractive() {
        val state = viewModel.uiState.value
        assertTrue(state.isLoadingPublication)
        assertTrue(state.isOverlayInteractive)
        assertFalse(state.toolbarVisible)
        assertEquals("", state.bookTitle)
        assertTrue(state.tocLinks.isEmpty())
        assertTrue(state.readingOrder.isEmpty())
        assertNull(state.currentLocation)
    }

    @Test
    fun applyLaunchExtrasSetsRequestedSheetsAndBookId() {
        viewModel.applyLaunchExtras(
            showToolbar = false,
            showHighlights = true,
            showBookmarks = false,
            bookId = "book-42"
        )

        val state = viewModel.uiState.value
        assertTrue(state.toolbarVisible)
        assertTrue(state.highlightsSheetVisible)
        assertFalse(state.bookmarkSheetVisible)
        assertEquals("book-42", state.bookStableId)
    }

    @Test
    fun onPublicationLoadedStoresMetadataFlattenedTocAndClearsLoading() {
        val nested = link("ch1_1.xhtml", "Section 1.1")
        val ch1 = link("ch1.xhtml", "Chapter 1", children = listOf(nested))
        val ch2 = link("ch2.xhtml", "Chapter 2")

        viewModel.onPublicationLoaded(
            bookStableId = "book-1",
            bookTitle = "Dune",
            bookAuthor = "Frank Herbert",
            tableOfContents = listOf(ch1, ch2),
            readingOrder = listOf(ch1, ch2)
        )

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingPublication)
        assertFalse(state.isOverlayInteractive)
        assertEquals("book-1", state.bookStableId)
        assertEquals("Dune", state.bookTitle)
        assertEquals("Frank Herbert", state.bookAuthor)
        assertEquals(listOf(ch1 to 0, nested to 1, ch2 to 0), state.tocLinks)
        assertEquals(listOf(ch1, ch2), state.readingOrder)
    }

    @Test
    fun isOverlayInteractiveTracksAllOverlayAndSheetFlags() {
        viewModel.setLoadingPublication(false)
        assertFalse(viewModel.uiState.value.isOverlayInteractive)

        val toggles = listOf(
            { v: Boolean -> viewModel.setToolbarVisible(v) },
            { v: Boolean -> viewModel.setSettingsSheetVisible(v) },
            { v: Boolean -> viewModel.setBookmarkSheetVisible(v) },
            { v: Boolean -> viewModel.setHighlightsSheetVisible(v) },
            { v: Boolean -> viewModel.setHighlightTintSheetVisible(v) },
            { v: Boolean -> viewModel.setSearchSheetVisible(v) },
            { v: Boolean -> viewModel.setDrawerOpen(v) }
        )

        for (toggle in toggles) {
            toggle(true)
            assertTrue(viewModel.uiState.value.isOverlayInteractive)
            toggle(false)
            assertFalse(viewModel.uiState.value.isOverlayInteractive)
        }

        viewModel.setSyncConflict(
            SyncConflictState(
                localPercentage = 25.0,
                remotePercentage = 50.0,
                remoteDevice = "Kobo",
                remoteTimestamp = 1_700_000_000_000L,
                onJump = {}
            )
        )
        assertTrue(viewModel.uiState.value.isOverlayInteractive)
        viewModel.setSyncConflict(null)
        assertFalse(viewModel.uiState.value.isOverlayInteractive)

        viewModel.showExportSheet(
            selectedAnnotations = emptyList(),
            currentBook = null,
            fallbackAuthorNames = listOf("Author"),
            title = "Title"
        )
        assertTrue(viewModel.uiState.value.isOverlayInteractive)
        viewModel.dismissExportSheet()
        assertFalse(viewModel.uiState.value.isOverlayInteractive)
    }

    @Test
    fun resolveCurrentChapterTitlePrefersLocatorTitleAndFallsBackToTopLevelToc() {
        val sub = link("ch1_1.xhtml", "Subsection")
        val ch1 = link("ch1.xhtml", "Chapter One", children = listOf(sub))
        viewModel.onPublicationLoaded(
            bookStableId = "book-1",
            bookTitle = "Title",
            bookAuthor = null,
            tableOfContents = listOf(ch1),
            readingOrder = listOf(ch1)
        )

        viewModel.updateLocation(locator("ch1.xhtml", title = "Explicit Title"), "{}")
        assertEquals("Explicit Title", viewModel.uiState.value.resolveCurrentChapterTitle())

        viewModel.updateLocation(locator("ch1.xhtml", title = null), "{}")
        assertEquals("Chapter One", viewModel.uiState.value.resolveCurrentChapterTitle())

        // Depth-1 links are excluded from top-level fallback matching ReaderActivity behavior.
        viewModel.updateLocation(locator("ch1_1.xhtml", title = null), "{}")
        assertNull(viewModel.uiState.value.resolveCurrentChapterTitle())
    }

    @Test
    fun resolveChapterNavigationUsesTocAndFallsBackToReadingOrder() {
        val ch1 = link("EPUB/ch1.xhtml", "One")
        val ch2 = link("EPUB/ch2.xhtml", "Two")
        val ch3 = link("EPUB/ch3.xhtml", "Three")

        viewModel.onPublicationLoaded(
            bookStableId = "b1",
            bookTitle = "Book",
            bookAuthor = null,
            tableOfContents = listOf(ch1, ch2, ch3),
            readingOrder = listOf(ch1)
        )
        viewModel.updateLocation(locator("EPUB/ch2.xhtml#sec1"), "{}")

        val navFromToc = viewModel.uiState.value.resolveChapterNavigation()
        assertEquals(listOf("One", "Two", "Three"), navFromToc.chapterTitles)
        assertEquals(1, navFromToc.currentChapterIndex)
        assertTrue(navFromToc.canSkipPrevious)
        assertTrue(navFromToc.canSkipNext)

        // Empty TOC falls back to readingOrder.
        viewModel.onPublicationLoaded(
            bookStableId = "b1",
            bookTitle = "Book",
            bookAuthor = null,
            tableOfContents = emptyList(),
            readingOrder = listOf(ch1, ch2)
        )
        viewModel.updateLocation(locator("EPUB/ch1.xhtml"), "{}")

        val navFromOrder = viewModel.uiState.value.resolveChapterNavigation()
        assertEquals(listOf(ch1, ch2), navFromOrder.chapterItems)
        assertEquals(0, navFromOrder.currentChapterIndex)
        assertFalse(navFromOrder.canSkipPrevious)
        assertTrue(navFromOrder.canSkipNext)
    }

    @Test
    fun resolveLoadingTitleHidesPlaceholderReadingString() {
        assertEquals("Fallback", viewModel.uiState.value.resolveLoadingTitle("Fallback"))

        viewModel.onPublicationLoaded(
            bookStableId = "b1",
            bookTitle = "Reading",
            bookAuthor = null,
            tableOfContents = emptyList(),
            readingOrder = emptyList()
        )
        assertEquals("Fallback", viewModel.uiState.value.resolveLoadingTitle("Fallback"))

        viewModel.onPublicationLoaded(
            bookStableId = "b1",
            bookTitle = "Real Title",
            bookAuthor = null,
            tableOfContents = emptyList(),
            readingOrder = emptyList()
        )
        assertEquals("Real Title", viewModel.uiState.value.resolveLoadingTitle("Fallback"))
    }

    @Test
    fun sheetNavigationHelpersAndTtsChapterResolutionUpdateState() {
        val ch1 = link("ch1.xhtml", "Opening")
        viewModel.onPublicationLoaded(
            bookStableId = "b1",
            bookTitle = "Novel",
            bookAuthor = "Writer",
            tableOfContents = listOf(ch1),
            readingOrder = listOf(ch1)
        )

        viewModel.toggleToolbar()
        assertTrue(viewModel.uiState.value.toolbarVisible)

        viewModel.setHighlightsSheetVisible(true)
        viewModel.onAnnotationNavigated()
        assertFalse(viewModel.uiState.value.highlightsSheetVisible)
        assertFalse(viewModel.uiState.value.toolbarVisible)

        viewModel.setToolbarVisible(true)
        viewModel.setBookmarkSheetVisible(true)
        viewModel.onBookmarkSheetClosed()
        assertFalse(viewModel.uiState.value.bookmarkSheetVisible)
        assertFalse(viewModel.uiState.value.toolbarVisible)

        viewModel.setToolbarVisible(true)
        viewModel.setSearchSheetVisible(true)
        viewModel.onSearchResultNavigated()
        assertFalse(viewModel.uiState.value.searchSheetVisible)
        assertFalse(viewModel.uiState.value.toolbarVisible)

        assertEquals("Opening", viewModel.updateTtsChapterForUtterance("ch1.xhtml", "Fallback"))
        assertEquals("Opening", viewModel.uiState.value.currentChapterTitle)

        assertEquals("Fallback", viewModel.updateTtsChapterForUtterance("unknown.xhtml", "Fallback"))
        assertEquals("Fallback", viewModel.uiState.value.currentChapterTitle)
    }

    @Test
    fun exportCollectionsBookmarksAnnotationsFontsAndFootnoteUpdateCleanly() {
        val bookmark = Bookmark(
            id = 1L,
            bookId = "b1",
            locatorJson = "{}",
            createdAt = 1_000L,
            label = "BM"
        )
        val annotation = Annotation(
            id = 2L,
            bookId = "b1",
            locatorJson = "{}",
            colorHex = Annotation.DEFAULT_COLOR_HEX,
            note = "Quote",
            createdAt = 2_000L
        )
        val font = CustomFont(
            name = "Custom Serif",
            fileName = "font.ttf",
            file = File("/tmp/font.ttf")
        )
        val footnote = FootnoteContent(
            id = "fn1",
            title = "Note 1",
            text = "Note body",
            targetLocatorJson = null
        )

        viewModel.setBookmarks(listOf(bookmark))
        viewModel.setAnnotations(listOf(annotation))
        viewModel.setCustomFonts(listOf(font))
        viewModel.setActiveFootnote(footnote)
        viewModel.setPageAnnouncement("Page 2")

        val book = LibraryBook(stableId = "b1", title = "Dune", authors = listOf("Frank Herbert"))
        viewModel.showExportSheet(
            selectedAnnotations = listOf(annotation),
            currentBook = book,
            fallbackAuthorNames = emptyList(),
            title = "Dune"
        )

        val state = viewModel.uiState.value
        assertEquals(listOf(bookmark), state.bookmarks)
        assertEquals(listOf(annotation), state.annotations)
        assertEquals(listOf(font), state.customFonts)
        assertEquals(footnote, state.activeFootnote)
        assertEquals("Page 2", state.pageAnnouncement)
        assertTrue(state.exportSheetVisible)
        assertNotNull(state.exportSheetData)
        assertEquals(book, state.exportSheetData?.book)
        assertEquals(listOf(annotation), state.exportSheetData?.annotations)

        viewModel.dismissExportSheet()
        assertFalse(viewModel.uiState.value.exportSheetVisible)
        assertNull(viewModel.uiState.value.exportSheetData)
    }
}
