package com.jdluu.leafline.reader.navigation

import org.readium.r2.shared.publication.Link

/**
 * Returns a stable key identifying a table of contents [Link] in a hierarchy.
 */
fun tocLinkKey(link: Link, index: Int = -1): String {
    val href = link.href.toString()
    val title = link.title ?: ""
    return if (href.isNotEmpty() || title.isNotEmpty()) "$href::$title" else "index_$index"
}

/**
 * Filters the flattened TOC link list to only show items that are not descendants
 * of collapsed parent nodes.
 */
fun filterVisibleTocLinks(
    allLinks: List<Pair<Link, Int>>,
    collapsedKeys: Set<String>
): List<Pair<Link, Int>> {
    val visible = mutableListOf<Pair<Link, Int>>()
    var hidingDepth = -1
    for (item in allLinks) {
        val (link, depth) = item
        if (hidingDepth != -1) {
            if (depth > hidingDepth) {
                continue
            } else {
                hidingDepth = -1
            }
        }
        visible.add(item)
        val key = tocLinkKey(link)
        if (link.children.isNotEmpty() && collapsedKeys.contains(key)) {
            hidingDepth = depth
        }
    }
    return visible
}

/**
 * Finds the keys of all ancestor links containing [targetIndex] in the flattened TOC.
 * This allows ensuring that parents of the active chapter remain expanded.
 */
fun findAncestorKeysForIndex(
    allLinks: List<Pair<Link, Int>>,
    targetIndex: Int
): Set<String> {
    if (targetIndex !in allLinks.indices) return emptySet()
    val ancestors = mutableSetOf<String>()
    var currentDepth = allLinks[targetIndex].second
    for (i in targetIndex - 1 downTo 0) {
        val (link, depth) = allLinks[i]
        if (depth < currentDepth) {
            ancestors.add(tocLinkKey(link))
            currentDepth = depth
            if (depth == 0) break
        }
    }
    return ancestors
}
