package com.jdluu.leafline.reader.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Url

@RunWith(RobolectricTestRunner::class)
class TocHierarchyTest {

    private fun link(href: String, title: String, children: List<Link> = emptyList()): Link {
        return Link(
            href = Url(href)!!,
            title = title,
            children = children
        )
    }

    @Test
    fun `tocLinkKey produces unique and stable identifiers`() {
        val l1 = link("ch1.xhtml", "Chapter 1")
        assertEquals("ch1.xhtml::Chapter 1", tocLinkKey(l1))
    }

    @Test
    fun `findAncestorKeysForIndex returns correct ancestors across hierarchy`() {
        val child2 = link("sec2_1.xhtml", "Section 2.1")
        val child1 = link("ch2.xhtml", "Chapter 2", children = listOf(child2))
        val rootPart = link("part1.xhtml", "Part 1", children = listOf(child1))
        val standAlone = link("outro.xhtml", "Outro")

        val allLinks = listOf(
            rootPart to 0,       // 0
            child1 to 1,         // 1
            child2 to 2,         // 2
            standAlone to 0      // 3
        )

        // Root level item has no ancestors
        assertEquals(emptySet<String>(), findAncestorKeysForIndex(allLinks, 0))

        // Level 1 item has Part 1 as ancestor
        assertEquals(setOf("part1.xhtml::Part 1"), findAncestorKeysForIndex(allLinks, 1))

        // Level 2 item has Part 1 and Chapter 2 as ancestors
        assertEquals(
            setOf("part1.xhtml::Part 1", "ch2.xhtml::Chapter 2"),
            findAncestorKeysForIndex(allLinks, 2)
        )

        // Invalid index
        assertEquals(emptySet<String>(), findAncestorKeysForIndex(allLinks, -1))
        assertEquals(emptySet<String>(), findAncestorKeysForIndex(allLinks, 99))
    }

    @Test
    fun `filterVisibleTocLinks hides children of collapsed parents`() {
        val sub1 = link("sec1_1.xhtml", "Section 1.1")
        val sub2 = link("sec1_2.xhtml", "Section 1.2")
        val ch1 = link("ch1.xhtml", "Chapter 1", children = listOf(sub1, sub2))
        val ch2 = link("ch2.xhtml", "Chapter 2")

        val allLinks = listOf(
            ch1 to 0,
            sub1 to 1,
            sub2 to 1,
            ch2 to 0
        )

        // When nothing is collapsed, all links visible
        assertEquals(allLinks, filterVisibleTocLinks(allLinks, emptySet()))

        // When Chapter 1 is collapsed, Section 1.1 and 1.2 are hidden, but Chapter 2 remains visible
        val collapsed = setOf(tocLinkKey(ch1))
        val visible = filterVisibleTocLinks(allLinks, collapsed)
        assertEquals(
            listOf(
                ch1 to 0,
                ch2 to 0
            ),
            visible
        )
    }

    @Test
    fun `filterVisibleTocLinks hides deeply nested descendants`() {
        val deepChild = link("deep.xhtml", "Deep")
        val midChild = link("mid.xhtml", "Mid", children = listOf(deepChild))
        val root = link("root.xhtml", "Root", children = listOf(midChild))
        val nextRoot = link("next.xhtml", "Next Root")

        val allLinks = listOf(
            root to 0,
            midChild to 1,
            deepChild to 2,
            nextRoot to 0
        )

        // Collapsing root hides mid and deep
        val collapsedRoot = setOf(tocLinkKey(root))
        assertEquals(
            listOf(root to 0, nextRoot to 0),
            filterVisibleTocLinks(allLinks, collapsedRoot)
        )

        // Collapsing mid hides deep only
        val collapsedMid = setOf(tocLinkKey(midChild))
        assertEquals(
            listOf(root to 0, midChild to 1, nextRoot to 0),
            filterVisibleTocLinks(allLinks, collapsedMid)
        )
    }
}
