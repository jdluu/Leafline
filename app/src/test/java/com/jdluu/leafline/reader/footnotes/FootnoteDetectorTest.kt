package com.jdluu.leafline.reader.footnotes

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FootnoteDetectorTest {

    @Test
    fun `isLikelyFootnoteTarget matches conventional footnote and note fragments`() {
        assertTrue(FootnoteDetector.isLikelyFootnoteTarget("fn1"))
        assertTrue(FootnoteDetector.isLikelyFootnoteTarget("fn-12"))
        assertTrue(FootnoteDetector.isLikelyFootnoteTarget("fn_3"))
        assertTrue(FootnoteDetector.isLikelyFootnoteTarget("footnote-1"))
        assertTrue(FootnoteDetector.isLikelyFootnoteTarget("footnote_5"))
        assertTrue(FootnoteDetector.isLikelyFootnoteTarget("footnote"))
        assertTrue(FootnoteDetector.isLikelyFootnoteTarget("note-4"))
        assertTrue(FootnoteDetector.isLikelyFootnoteTarget("endnote-2"))
        assertTrue(FootnoteDetector.isLikelyFootnoteTarget("n1"))
        assertTrue(FootnoteDetector.isLikelyFootnoteTarget("n99"))
        assertTrue(FootnoteDetector.isLikelyFootnoteTarget("ch1-note-2"))
        assertTrue(FootnoteDetector.isLikelyFootnoteTarget("noteref-1"))
        assertTrue(FootnoteDetector.isLikelyFootnoteTarget("ref-1"))
        assertTrue(FootnoteDetector.isLikelyFootnoteTarget("#fn1"))
    }

    @Test
    fun `isLikelyFootnoteTarget rejects chapter sections and non-note anchors`() {
        assertFalse(FootnoteDetector.isLikelyFootnoteTarget(null))
        assertFalse(FootnoteDetector.isLikelyFootnoteTarget(""))
        assertFalse(FootnoteDetector.isLikelyFootnoteTarget("chapter1"))
        assertFalse(FootnoteDetector.isLikelyFootnoteTarget("sec-12"))
        assertFalse(FootnoteDetector.isLikelyFootnoteTarget("header-main"))
        assertFalse(FootnoteDetector.isLikelyFootnoteTarget("introduction"))
        assertFalse(FootnoteDetector.isLikelyFootnoteTarget("toc-item"))
    }

    @Test
    fun `backlinks are recognized and rejected as footnote targets`() {
        assertTrue(FootnoteDetector.isBacklinkTarget("fnref1"))
        assertTrue(FootnoteDetector.isBacklinkTarget("fnref-2"))
        assertTrue(FootnoteDetector.isBacklinkTarget("back-to-text"))
        assertTrue(FootnoteDetector.isBacklinkTarget("refback"))
        assertTrue(FootnoteDetector.isBacklinkTarget("note-back"))

        // Consequently not treated as footnote target
        assertFalse(FootnoteDetector.isLikelyFootnoteTarget("fnref1"))
        assertFalse(FootnoteDetector.isLikelyFootnoteTarget("back-to-text"))
    }

    @Test
    fun `isLikelyFootnoteLink evaluates rels and url paths`() {
        assertTrue(FootnoteDetector.isLikelyFootnoteLink("chap.xhtml", "fn1"))
        assertTrue(FootnoteDetector.isLikelyFootnoteLink("notes.xhtml", "custom-id"))
        assertTrue(FootnoteDetector.isLikelyFootnoteLink("footnotes.xhtml", "sec1"))
        assertTrue(FootnoteDetector.isLikelyFootnoteLink("chap.xhtml", "custom-id", rels = setOf("footnote")))

        assertFalse(FootnoteDetector.isLikelyFootnoteLink("chap.xhtml", null))
        assertFalse(FootnoteDetector.isLikelyFootnoteLink("chap.xhtml", "chapter2"))
        assertFalse(FootnoteDetector.isLikelyFootnoteLink("chap.xhtml", "fn1", rels = setOf("backlink")))
    }
}
