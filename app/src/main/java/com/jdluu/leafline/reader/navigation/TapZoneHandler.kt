package com.jdluu.leafline.reader.navigation

import android.content.Context
import android.util.Log
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
            val ratio = event.point.x / width
            Log.d("TapZoneHandler", "onTap: point=${event.point}, width=$width, ratio=$ratio")
            val action = effectiveTapZoneAction(
                tapZoneAt(ratio),
                settings.tapZoneConfig,
                settings.epub.scroll == true
            )
            Log.d("TapZoneHandler", "onTap action: $action")
            return when (action) {
                TapZoneAction.NONE -> false
                TapZoneAction.TOGGLE_MENU -> {
                    onToggleMenu()
                    true
                }
                TapZoneAction.NEXT_PAGE -> {
                    val handled = navigatorProvider()?.goForward(pageTurnAnimated(settings)) ?: false
                    Log.d("TapZoneHandler", "goForward returned $handled")
                    handled
                }
                TapZoneAction.PREVIOUS_PAGE -> {
                    val handled = navigatorProvider()?.goBackward(pageTurnAnimated(settings)) ?: false
                    Log.d("TapZoneHandler", "goBackward returned $handled")
                    handled
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
                ?.times(100f)
                ?.roundToInt()
                ?: -1
        } catch (_: Exception) {
            -1
        }
        val href = locator.href.toString()

        if (!primed) {
            primed = true
            lastHref = href
            lastPercent = percent
            return
        }

        val hrefChanged = href != lastHref
        val percentChanged = percent >= 0 && (lastPercent < 0 || abs(percent - lastPercent) >= 1)

        if (!hrefChanged && !percentChanged) return

        lastHref = href
        lastPercent = percent

        val message = when {
            percent >= 0 -> "$percent percent"
            locator.title?.isNotBlank() == true -> locator.title!!
            else -> "Page changed"
        }

        announceJob?.cancel()
        announceJob = scope.launch {
            delay(DEBOUNCE_MILLIS)
            announcement = message
            onAnnouncement(message)
        }
    }

    companion object {
        private const val DEBOUNCE_MILLIS = 300L
    }
}
