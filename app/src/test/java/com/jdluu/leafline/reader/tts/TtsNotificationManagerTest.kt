package com.jdluu.leafline.reader.tts

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TtsNotificationManagerTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun `createNotificationChannel registers channel with correct id and importance`() {
        TtsNotificationManager.createNotificationChannel(context)

        val channel = notificationManager.getNotificationChannel(TtsNotificationManager.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(TtsNotificationManager.CHANNEL_ID, channel.id)
        assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
        assertFalse(channel.canShowBadge())
    }

    @Test
    fun `buildNotification configures fields and actions for playing state`() {
        val session = MediaSession(context, "TestSession")
        val notification = TtsNotificationManager.buildNotification(
            context = context,
            bookTitle = "The Hobbit",
            author = "J.R.R. Tolkien",
            chapterTitle = "An Unexpected Party",
            isPlaying = true,
            sessionToken = session.sessionToken
        )

        val extras = notification.extras
        assertEquals("An Unexpected Party", extras.getString(Notification.EXTRA_TITLE))
        assertEquals("J.R.R. Tolkien", extras.getString(Notification.EXTRA_TEXT))
        assertEquals("The Hobbit", extras.getString(Notification.EXTRA_SUB_TEXT))

        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(Notification.CATEGORY_TRANSPORT, notification.category)
        assertEquals(Notification.VISIBILITY_PUBLIC, notification.visibility)

        val actions = notification.actions
        assertNotNull(actions)
        assertEquals(4, actions.size)
        assertEquals("Previous", actions[0].title.toString())
        assertEquals("Pause", actions[1].title.toString())
        assertEquals("Next", actions[2].title.toString())
        assertEquals("Stop", actions[3].title.toString())

        session.release()
    }

    @Test
    fun `buildNotification configures fields and actions for paused state`() {
        val notification = TtsNotificationManager.buildNotification(
            context = context,
            bookTitle = "Dune",
            author = "Frank Herbert",
            chapterTitle = null,
            isPlaying = false,
            sessionToken = null
        )

        val extras = notification.extras
        assertEquals("Dune", extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Frank Herbert", extras.getString(Notification.EXTRA_TEXT))

        // Paused notifications are not ongoing so they can be swiped away
        assertFalse(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)

        val actions = notification.actions
        assertNotNull(actions)
        assertEquals(4, actions.size)
        assertEquals("Previous", actions[0].title.toString())
        assertEquals("Play", actions[1].title.toString())
        assertEquals("Next", actions[2].title.toString())
        assertEquals("Stop", actions[3].title.toString())
    }

    @Test
    fun `service actions invoke corresponding TtsAudioService listener methods`() {
        var playCalled = false
        var pauseCalled = false
        var skipNextCalled = false
        var skipPrevCalled = false
        var stopCalled = false

        TtsAudioService.serviceListener = object : TtsAudioService.Listener {
            override fun onPlay() { playCalled = true }
            override fun onPause() { pauseCalled = true }
            override fun onSkipNext() { skipNextCalled = true }
            override fun onSkipPrevious() { skipPrevCalled = true }
            override fun onStop() { stopCalled = true }
        }

        val service = Robolectric.buildService(TtsAudioService::class.java).create().get()

        service.onStartCommand(Intent(TtsAudioService.ACTION_PLAY), 0, 1)
        assertTrue(playCalled)

        service.onStartCommand(Intent(TtsAudioService.ACTION_PAUSE), 0, 2)
        assertTrue(pauseCalled)

        service.onStartCommand(Intent(TtsAudioService.ACTION_NEXT), 0, 3)
        assertTrue(skipNextCalled)

        service.onStartCommand(Intent(TtsAudioService.ACTION_PREVIOUS), 0, 4)
        assertTrue(skipPrevCalled)

        service.onStartCommand(Intent(TtsAudioService.ACTION_STOP), 0, 5)
        assertTrue(stopCalled)

        TtsAudioService.serviceListener = null
    }
}
