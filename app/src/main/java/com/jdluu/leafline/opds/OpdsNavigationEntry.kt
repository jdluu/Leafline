package com.jdluu.leafline.opds

/** A single navigation entry surfaced from a parsed OPDS 1.2 root feed. */
data class OpdsNavigationEntry(
    val title: String,
    val href: String
)
