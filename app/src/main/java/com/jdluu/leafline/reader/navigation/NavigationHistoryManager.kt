package com.jdluu.leafline.reader.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.r2.shared.publication.Locator
import kotlin.math.abs

/**
 * An entry recorded in the reader navigation history stack.
 */
data class NavigationHistoryEntry(
    val locator: Locator,
    val description: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Transient notice displayed when a jump occurs (such as tapping a footnote,
 * following an internal link, jumping to a chapter, or opening a search result),
 * offering the user one-tap return access.
 */
data class JumpNotice(
    val returnLocator: Locator,
    val sourceDescription: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Manages backward and forward location history stacks for non-sequential jumps
 * (e.g. table of contents, footnotes/internal links, search results, bookmarks).
 */
class NavigationHistoryManager(
    private val maxHistorySize: Int = 50
) {
    private val backStack = mutableListOf<NavigationHistoryEntry>()
    private val forwardStack = mutableListOf<NavigationHistoryEntry>()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

    private val _jumpNotice = MutableStateFlow<JumpNotice?>(null)
    val jumpNotice: StateFlow<JumpNotice?> = _jumpNotice.asStateFlow()

    val backStackCount: Int get() = backStack.size
    val forwardStackCount: Int get() = forwardStack.size

    /**
     * Records a jump from [from] to [to].
     * Pushes [from] onto the back stack and resets the forward stack.
     */
    fun recordJump(from: Locator, to: Locator? = null, description: String? = null) {
        if (to != null && areLocatorsSimilar(from, to)) {
            return
        }

        val top = backStack.lastOrNull()
        if (top != null && areLocatorsSimilar(top.locator, from)) {
            updateJumpNotice(from, description)
            return
        }

        backStack.add(NavigationHistoryEntry(from, description))
        if (backStack.size > maxHistorySize) {
            backStack.removeAt(0)
        }
        forwardStack.clear()

        updateState()
        updateJumpNotice(from, description)
    }

    /**
     * Navigates back one step in history, pushing [current] to the forward stack
     * and returning the previous [Locator], or null if the back stack is empty.
     */
    fun goBack(current: Locator): Locator? {
        if (backStack.isEmpty()) return null
        val target = backStack.removeAt(backStack.lastIndex)
        forwardStack.add(NavigationHistoryEntry(current))
        if (forwardStack.size > maxHistorySize) {
            forwardStack.removeAt(0)
        }
        _jumpNotice.value = null
        updateState()
        return target.locator
    }

    /**
     * Navigates forward one step in history, pushing [current] to the back stack
     * and returning the next [Locator], or null if the forward stack is empty.
     */
    fun goForward(current: Locator): Locator? {
        if (forwardStack.isEmpty()) return null
        val target = forwardStack.removeAt(forwardStack.lastIndex)
        backStack.add(NavigationHistoryEntry(current))
        if (backStack.size > maxHistorySize) {
            backStack.removeAt(0)
        }
        _jumpNotice.value = null
        updateState()
        return target.locator
    }

    /**
     * Dismisses the active jump notice banner.
     */
    fun dismissJumpNotice() {
        _jumpNotice.value = null
    }

    /**
     * Clears all history entries and notices.
     */
    fun clear() {
        backStack.clear()
        forwardStack.clear()
        _jumpNotice.value = null
        updateState()
    }

    private fun updateJumpNotice(returnLocator: Locator, description: String?) {
        val label = description?.takeIf { it.isNotBlank() }
            ?: returnLocator.title?.takeIf { it.isNotBlank() }
            ?: "previous location"
        _jumpNotice.value = JumpNotice(returnLocator, label)
    }

    private fun updateState() {
        _canGoBack.value = backStack.isNotEmpty()
        _canGoForward.value = forwardStack.isNotEmpty()
    }

    companion object {
        fun areLocatorsSimilar(a: Locator, b: Locator): Boolean {
            if (a.href != b.href) return false
            val progA = a.locations.progression ?: a.locations.totalProgression
            val progB = b.locations.progression ?: b.locations.totalProgression
            if (progA != null && progB != null) {
                return abs(progA - progB) < 0.005
            }
            return a == b
        }
    }
}
