package com.jdluu.leafline.theme

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Pure helper deciding whether motion should be skipped.
 *
 * Android's "Remove animations" accessibility toggle and Developer Options
 * "Animator duration scale = Off" both set `Settings.Global.ANIMATOR_DURATION_SCALE`
 * to `0f` (or below).
 */
internal fun resolveReduceMotion(
    animatorDurationScale: Float,
    explicitReduceMotion: Boolean = false
): Boolean = explicitReduceMotion || !animatorDurationScale.isFinite() || animatorDurationScale <= 0f

/**
 * Returns `0` when [reduceMotion] is true so transitions snap instantly;
 * otherwise returns [durationMillis] coerced non-negative.
 */
internal fun motionDurationMs(durationMillis: Int, reduceMotion: Boolean): Int =
    if (reduceMotion) 0 else durationMillis.coerceAtLeast(0)

internal fun readAnimatorDurationScale(contentResolver: ContentResolver): Float = try {
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
} catch (_: Throwable) {
    1f
}

/**
 * Whether motion should snap instantly for the current composition tree.
 * Supplied once at the root by [LeaflineTheme].
 */
internal val LocalReduceMotion = staticCompositionLocalOf { false }

/**
 * Observes `Settings.Global.ANIMATOR_DURATION_SCALE` once for the enclosing theme root.
 */
@Composable
internal fun observeReduceMotion(explicitReduceMotion: Boolean = false): Boolean {
    val contentResolver = LocalContext.current.contentResolver
    var animatorScale by remember(contentResolver) {
        mutableStateOf(readAnimatorDurationScale(contentResolver))
    }

    DisposableEffect(contentResolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                animatorScale = readAnimatorDurationScale(contentResolver)
            }
        }
        val uri = runCatching {
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        }.getOrNull()
        if (uri != null) {
            runCatching {
                contentResolver.registerContentObserver(uri, false, observer)
            }
        }
        animatorScale = readAnimatorDurationScale(contentResolver)
        onDispose {
            runCatching {
                contentResolver.unregisterContentObserver(observer)
            }
        }
    }

    return resolveReduceMotion(animatorScale, explicitReduceMotion)
}

@Composable
internal fun rememberReduceMotion(): Boolean = LocalReduceMotion.current

internal fun <T> leaflineTween(durationMillis: Int, reduceMotion: Boolean) =
    tween<T>(durationMillis = motionDurationMs(durationMillis, reduceMotion))

internal fun leaflineEnterTransition(
    reduceMotion: Boolean,
    default: EnterTransition = fadeIn() + expandVertically()
): EnterTransition = if (reduceMotion) EnterTransition.None else default

internal fun leaflineExitTransition(
    reduceMotion: Boolean,
    default: ExitTransition = fadeOut() + shrinkVertically()
): ExitTransition = if (reduceMotion) ExitTransition.None else default
