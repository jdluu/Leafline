package com.jdluu.leafline.reader.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchDecorationPlannerTest {

    private val planner = SearchDecorationPlanner(
        maxDecorations = 5,
        matchTint = 0x11111111,
        activeTint = 0x22222222
    )

    private fun stateWith(vararg pairs: Pair<Int, String>): BookSearchState {
        return BookSearchState(
            query = "q",
            status = BookSearchStatus.Completed(pairs.size),
            results = pairs.map { (id, text) ->
                BookSearchResult(
                    id = id,
                    sectionTitle = null,
                    excerptBefore = "",
                    excerptMatch = text,
                    excerptAfter = "",
                    locatorJson = "{}"
                )
            }
        )
    }

    @Test
    fun `empty results produce no decorations`() {
        assertTrue(planner.planFor(BookSearchState(), activeResultId = null).isEmpty())
    }

    @Test
    fun `each decorated result carries its result id`() {
        val specs = planner.planFor(stateWith(0 to "a", 1 to "b", 2 to "c"), activeResultId = null)
        assertEquals(listOf(0, 1, 2), specs.map { it.resultId })
    }

    @Test
    fun `no active result marks everything as a plain match`() {
        val specs = planner.planFor(stateWith(0 to "a", 1 to "b"), activeResultId = null)
        assertTrue(specs.all { !it.isActive })
    }

    @Test
    fun `matching active id marks exactly that result active`() {
        val specs = planner.planFor(stateWith(0 to "a", 1 to "b", 2 to "c"), activeResultId = 1)
        specs.forEach { spec -> assertEquals(spec.resultId == 1, spec.isActive) }
    }

    @Test
    fun `active result carries the active tint, others the match tint`() {
        val specs = planner.planFor(stateWith(0 to "a", 1 to "b"), activeResultId = 0)
        assertEquals(0x22222222, specs.first { it.resultId == 0 }.tint)
        assertEquals(0x11111111, specs.first { it.resultId == 1 }.tint)
    }

    @Test
    fun `decorations are capped at the configured maximum`() {
        val results = (0 until 10).map { it to "x$it" }
        val specs = planner.planFor(stateWith(*results.toTypedArray()), activeResultId = null)
        assertEquals(5, specs.size)
        assertEquals(listOf(0, 1, 2, 3, 4), specs.map { it.resultId })
    }
}
