package com.jdluu.leafline.reader.tts

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.session.MediaSession
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.IntentCompat

/**
 * Foreground service that keeps TTS playback alive when the screen turns off or the
 * app is switched to the background.
 */
class TtsAudioService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        TtsNotificationManager.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_OR_UPDATE -> {
                val bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE) ?: "Reading"
                val author = intent.getStringExtra(EXTRA_AUTHOR)
                val chapterTitle = intent.getStringExtra(EXTRA_CHAPTER_TITLE)
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
                val sessionToken = IntentCompat.getParcelableExtra(
                    intent,
                    EXTRA_SESSION_TOKEN,
                    MediaSession.Token::class.java
                )

                val notification = TtsNotificationManager.buildNotification(
                    context = this,
                    bookTitle = bookTitle,
                    author = author,
                    chapterTitle = chapterTitle,
                    isPlaying = isPlaying,
                    sessionToken = sessionToken
                )

                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }

                    if (!isPlaying) {
                        // Allow user to dismiss notification when playback is paused
                        stopForeground(STOP_FOREGROUND_DETACH)
                    }
                }.onFailure { e ->
                    Log.w(TAG, "Failed to start foreground service for TTS", e)
                }
            }
            ACTION_PLAY -> serviceListener?.onPlay()
            ACTION_PAUSE -> serviceListener?.onPause()
            ACTION_PREVIOUS -> serviceListener?.onSkipPrevious()
            ACTION_NEXT -> serviceListener?.onSkipNext()
            ACTION_STOP -> {
                serviceListener?.onStop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        runCatching {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        super.onDestroy()
    }

    interface Listener {
        fun onPlay()
        fun onPause()
        fun onSkipNext()
        fun onSkipPrevious()
        fun onStop()
    }

    companion object {
        private const val TAG = "TtsAudioService"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START_OR_UPDATE = "com.jdluu.leafline.tts.action.START_OR_UPDATE"
        const val ACTION_PLAY = "com.jdluu.leafline.tts.action.PLAY"
        const val ACTION_PAUSE = "com.jdluu.leafline.tts.action.PAUSE"
        const val ACTION_PREVIOUS = "com.jdluu.leafline.tts.action.PREVIOUS"
        const val ACTION_NEXT = "com.jdluu.leafline.tts.action.NEXT"
        const val ACTION_STOP = "com.jdluu.leafline.tts.action.STOP"

        const val EXTRA_BOOK_TITLE = "extra_book_title"
        const val EXTRA_AUTHOR = "extra_author"
        const val EXTRA_CHAPTER_TITLE = "extra_chapter_title"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_SESSION_TOKEN = "extra_session_token"

        var serviceListener: Listener? = null

        fun startOrUpdate(
            context: Context,
            bookTitle: String,
            author: String? = null,
            chapterTitle: String? = null,
            isPlaying: Boolean = false,
            sessionToken: MediaSession.Token? = null
        ) {
            val intent = Intent(context, TtsAudioService::class.java).apply {
                action = ACTION_START_OR_UPDATE
                putExtra(EXTRA_BOOK_TITLE, bookTitle)
                putExtra(EXTRA_AUTHOR, author)
                putExtra(EXTRA_CHAPTER_TITLE, chapterTitle)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                putExtra(EXTRA_SESSION_TOKEN, sessionToken)
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }.onFailure { e ->
                Log.w(TAG, "Could not start or update TtsAudioService", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TtsAudioService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching {
                context.startService(intent)
            }.onFailure { e ->
                Log.w(TAG, "Could not stop TtsAudioService", e)
            }
        }
    }
}
