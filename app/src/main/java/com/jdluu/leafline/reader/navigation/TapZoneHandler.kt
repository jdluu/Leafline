package com.jdluu.leafline.reader.navigation

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.jdluu.leafline.reader.PageTurnAnimation
import com.jdluu.leafline.reader.ReaderSettings
import com.jdluu.leafline.reader.TapZoneAction
import com.jdluu.leafline.reader.effectiveTapZoneAction
import com.jdluu.leafline.reader.pageTurnIsAnimated
import com.jdluu.leafline.reader.tapZoneAt
import com.jdluu.leafline.sync.ReadingProgressMath
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.publication.Locator
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Turns navigator taps into tap-zone actions and tracks reading position
 * for TalkBack page-turn announcements.
 *
 * The listener consumes every tap except zones configured as none, which are
 * left unconsumed so the publication webview keeps default handling. While
 * scroll mode is on, page-turn actions resolve to none so taps stay
 * unconsumed and vertical webview gestures own the navigation.
 *
 * Announcements: the first locator emission primes the tracker silently;
 * later emissions announce when the resource changes or the reading
 * percentage moves by at least one point. Rapid successive updates, such as
 * continuous scrolling, collapse into one announcement through a short
 * debounce.
 */
class TapZoneHandler(
    private val context: Context,
    private val scope: LifecycleCoroutineScope,
    private val navigatorProvider: () -> EpubNavigatorFragment?,
    private val settingsProvider: () -> ReaderSettings,
    private val onToggleMenu: () -> Unit,
    private val onAnnouncement: (String) -> Unit,
) {

    /** State-backed announcement text rendered by the reader overlay. */
    var announcement: String? = null
        private set

    private var announceJob: Job? = null
    private var primed = false
    private var lastHref: String? = null
    private var lastPercent = -1

    val inputListener = object : InputListener {
        override fun onTap(event: TapEvent): Boolean {
            val width = navigatorProvider()?.publicationView?.width ?: return false
            if (width <= 0) return false
            val settings = settingsProvider()
            return when (
                effectiveTapZoneAction(
                    tapZoneAt(event.point.x / width),
                    settings.tapZoneConfig,
                    settings.epub.scroll == true
                )
            ) {
                TapZoneAction.NONE -> false
                TapZoneAction.TOGGLE_MENU -> {
                    onToggleMenu()
                    true
                }
                TapZoneAction.NEXT_PAGE -> {
                    navigatorProvider()?.goForward(pageTurnAnimated(settings))
                    true
                }
                TapZoneAction.PREVIOUS_PAGE -> {
                    navigatorProvider()?.goBackward(pageTurnAnimated(settings))
                    true
                }
            }
        }
    }

    /**
     * Resolves the effective page turn animation against the system animator
     * duration scale so turns snap instantly while the OS has animations
     * removed, regardless of the stored preference.
     */
    fun pageTurnAnimated(settings: ReaderSettings): Boolean {
        val animatorScale = android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        return pageTurnIsAnimated(settings.pageTurnAnimation, animatorScale, settings.reduceMotion)
    }

    fun onReadingPositionChanged(locator: Locator) {
        val percent = try {
            ReadingProgressMath.percentageFromLocator(locator.toJSON().toString())
        } catch (e: Exception) {
            null
        }?.roundToInt() ?: return
        val href = locator.href.toString()
        if (!primed) {
            primed = true
            lastHref = href
            lastPercent = percent
            return
        }
        val changedResource = href != lastHref
        val changedPage = abs(percent - lastPercent) >= 1
        if (!changedResource && !changedPage) return
        lastHref = href
        lastPercent = percent
        announceJob?.cancel()
        announceJob = scope.launch {
            delay(PAGE_TURN_ANNOUNCE_DEBOUNCE_MS)
            announcement = "Page $percent%"
            onAnnouncement(announcement!!)
        }
    }

    companion object {
        private const val PAGE_TURN_ANNOUNCE_DEBOUNCE_MS = 300L
    }
}
