package com.jdluu.leafline.opds

/** A navigation or acquisition item returned by an OPDS feed. */
data class OpdsFeedEntry(
    val title: String,
    val href: String,
    val isAcquisition: Boolean,
    val authors: List<String> = emptyList()
)

data class OpdsFeedPage(
    val title: String,
    val entries: List<OpdsFeedEntry>
)

fun OpdsFeedPage.navigationEntries(): List<OpdsNavigationEntry> =
    entries.filterNot { it.isAcquisition }.map { OpdsNavigationEntry(it.title, it.href) }

fun OpdsFeedPage.acquisitionEntries(): List<OpdsFeedEntry> =
    entries.filter { it.isAcquisition }

