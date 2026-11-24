package com.jdluu.leafline.reader.tts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.session.MediaSession
import com.jdluu.leafline.R
import com.jdluu.leafline.ReaderActivity

/**
 * Handles creation and configuration of the media-styled text-to-speech notification.
 */
object TtsNotificationManager {

    const val CHANNEL_ID = "leafline_tts_playback_channel"
    private const val CHANNEL_NAME = "Text-to-Speech Playback"
    private const val CHANNEL_DESCRIPTION = "Media transport controls and status for Leafline TTS reading"

    /**
     * Registers the notification channel with the system [NotificationManager].
     */
    fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = CHANNEL_DESCRIPTION
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Builds a [Notification] with [Notification.MediaStyle] and transport controls.
     */
    fun buildNotification(
        context: Context,
        bookTitle: String,
        author: String? = null,
        chapterTitle: String? = null,
        isPlaying: Boolean = false,
        sessionToken: MediaSession.Token? = null
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, ReaderActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Previous Action
        val prevPendingIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, TtsAudioService::class.java).apply { action = TtsAudioService.ACTION_PREVIOUS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val prevAction = Notification.Action.Builder(
            Icon.createWithResource(context, android.R.drawable.ic_media_previous),
            "Previous",
            prevPendingIntent
        ).build()

        // Play / Pause Action
        val playPauseActionIntent = if (isPlaying) {
            Intent(context, TtsAudioService::class.java).apply { action = TtsAudioService.ACTION_PAUSE }
        } else {
            Intent(context, TtsAudioService::class.java).apply { action = TtsAudioService.ACTION_PLAY }
        }
        val playPausePendingIntent = PendingIntent.getService(
            context,
            2,
            playPauseActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"
        val playPauseAction = Notification.Action.Builder(
            Icon.createWithResource(context, playPauseIcon),
            playPauseTitle,
            playPausePendingIntent
        ).build()

        // Next Action
        val nextPendingIntent = PendingIntent.getService(
            context,
            3,
            Intent(context, TtsAudioService::class.java).apply { action = TtsAudioService.ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextAction = Notification.Action.Builder(
            Icon.createWithResource(context, android.R.drawable.ic_media_next),
            "Next",
            nextPendingIntent
        ).build()

        // Stop Action
        val stopPendingIntent = PendingIntent.getService(
            context,
            4,
            Intent(context, TtsAudioService::class.java).apply { action = TtsAudioService.ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(context, android.R.drawable.ic_menu_close_clear_cancel),
            "Stop",
            stopPendingIntent
        ).build()

        val mediaStyle = Notification.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
        if (sessionToken != null) {
            mediaStyle.setMediaSession(sessionToken)
        }

        val primaryTitle = chapterTitle ?: bookTitle
        val secondaryText = if (!author.isNullOrBlank()) author else bookTitle

        val builder = Notification.Builder(context, CHANNEL_ID)

        return builder
            .setSmallIcon(R.drawable.splash_leaf)
            .setContentTitle(primaryTitle)
            .setContentText(secondaryText)
            .apply {
                if (chapterTitle != null && chapterTitle != bookTitle) {
                    setSubText(bookTitle)
                }
            }
            .setContentIntent(contentIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setOngoing(isPlaying)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(stopAction)
            .setStyle(mediaStyle)
            .build()
    }
}
