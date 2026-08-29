package com.jdluu.leafline.reader.search

/**
 * Defines the highlight decoration to draw for one search result. This is a
 * pure Kotlin description deliberately independent of the Readium [Decoration]
 * type so the planning logic stays JVM-testable; [ReaderActivity] maps each
 * spec onto a Readium highlight at the Android boundary.
 */
data class SearchDecorationSpec(
    val resultId: Int,
    /** Whether this result is the currently open one. */
    val isActive: Boolean,
    /** ARGB tint to draw, depending on [isActive]. */
    val tint: Int
)

/**
 * Plans which search results become highlight decorations for the navigator.
 *
 * The reader shows at most [maxDecorations] freshly located matches, marking
 * the single active (currently open) result with a stronger tint so the user
 * can find where they navigated. This is pure orchestration over
 * [BookSearchState]; locating the backing
 * [org.readium.r2.shared.publication.Locator] and building the actual Readium
 * decoration stays at the Android boundary.
 */
class SearchDecorationPlanner(
    private val maxDecorations: Int = MAX_SEARCH_DECORATIONS,
    private val matchTint: Int = SEARCH_MATCH_TINT,
    private val activeTint: Int = SEARCH_ACTIVE_TINT
) {

    /** Plans the decorations to draw for [state], marking [activeResultId] active. */
    fun planFor(state: BookSearchState, activeResultId: Int?): List<SearchDecorationSpec> {
        return state.results
            .take(maxDecorations)
            .map { result ->
                val active = result.id == activeResultId
                SearchDecorationSpec(
                    resultId = result.id,
                    isActive = active,
                    tint = if (active) activeTint else matchTint
                )
            }
    }

    companion object {
        /** Soft amber tint for ordinary search matches. */
        const val SEARCH_MATCH_TINT = 0x55FFD54F.toInt()

        /** Stronger amber tint for the currently open search result. */
        const val SEARCH_ACTIVE_TINT = 0xCCFF8F00.toInt()

        /** Highest number of search matches decorated at once on screen. */
        const val MAX_SEARCH_DECORATIONS = 200
    }
}
