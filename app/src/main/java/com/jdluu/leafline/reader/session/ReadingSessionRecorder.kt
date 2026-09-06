package com.jdluu.leafline.reader.session

import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.jdluu.leafline.library.data.ReadingSession
import com.jdluu.leafline.library.data.ReadingSessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Lifecycle-aware recorder for active reading sessions in Leafline.
 *
 * Tracks active reading duration (excluding background or paused time) with an
 * injected clock, and persists durable session history in Room via
 * [ReadingSessionRepository].
 *
 * Independent of Activity UI: consumes lifecycle events via [DefaultLifecycleObserver]
 * or direct method calls, and handles process recreation via [saveState] / [restoreState]
 * to prevent double-counting.
 */
class ReadingSessionRecorder(
    private val repository: ReadingSessionRepository,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis
) : DefaultLifecycleObserver {

    private val stateLock = Any()
    private val persistMutex = Mutex()
    private var lastRecordedSessionId: Long? = null

    var activeSessionId: Long? = null
        private set

    var currentBookId: String? = null
        private set

    var sessionStartTime: Long = 0L
        private set

    var accumulatedActiveDuration: Long = 0L
        private set

    var lastResumeTime: Long? = null
        private set

    var startProgression: Double? = null
        private set

    var endProgression: Double? = null
        private set

    var startLocatorJson: String? = null
        private set

    var endLocatorJson: String? = null
        private set

    var isTracking: Boolean = false
        private set

    val isPaused: Boolean
        get() = synchronized(stateLock) { isTracking && lastResumeTime == null }

    val isActive: Boolean
        get() = synchronized(stateLock) { isTracking && lastResumeTime != null }

    override fun onResume(owner: LifecycleOwner) {
        resume()
    }

    override fun onPause(owner: LifecycleOwner) {
        pause()
    }

    /**
     * Starts recording a session for [bookId].
     *
     * If already tracking the same book, this call is a no-op to prevent duplicate
     * sessions. If tracking a different book, the previous session is finalized first.
     */
    fun startSession(
        bookId: String,
        initialLocatorJson: String? = null,
        initialProgression: Double? = null
    ) {
        synchronized(stateLock) {
            if (isTracking && currentBookId == bookId) {
                return
            }
            if (isTracking) {
                stopInternal(flushImmediately = true)
            }
            val now = clock()
            isTracking = true
            activeSessionId = null
            lastRecordedSessionId = null
            currentBookId = bookId
            sessionStartTime = now
            lastResumeTime = now
            accumulatedActiveDuration = 0L
            startLocatorJson = initialLocatorJson
            endLocatorJson = initialLocatorJson
            startProgression = initialProgression
            endProgression = initialProgression
        }
    }

    /**
     * Pauses the active reading timer (e.g. when app is backgrounded or screen turns off).
     * Time spent while paused is excluded from [accumulatedActiveDuration].
     * Flushes the current session state to disk so data is preserved if the process is killed.
     */
    fun pause() {
        var sessionToFlush: ReadingSession? = null
        synchronized(stateLock) {
            if (!isTracking) return
            val resumeTime = lastResumeTime ?: return
            val now = clock()
            val elapsed = maxOf(0L, now - resumeTime)
            accumulatedActiveDuration += elapsed
            lastResumeTime = null
            sessionToFlush = currentSnapshot(now)
        }
        sessionToFlush?.let { persistAsync(it) }
    }

    /**
     * Resumes the active reading timer when the reader returns to foreground.
     */
    fun resume() {
        synchronized(stateLock) {
            if (!isTracking) return
            if (lastResumeTime != null) return
            lastResumeTime = clock()
        }
    }

    /**
     * Updates reading locator and progression as the user turns pages.
     */
    fun updateLocation(locatorJson: String?, progression: Double?) {
        synchronized(stateLock) {
            if (!isTracking) return
            if (startLocatorJson == null && locatorJson != null) {
                startLocatorJson = locatorJson
            }
            if (startProgression == null && progression != null) {
                startProgression = progression
            }
            if (locatorJson != null) {
                endLocatorJson = locatorJson
            }
            if (progression != null) {
                endProgression = progression
            }
        }
    }

    /**
     * Stops and finalizes the active session. Flushes the completed session to the repository.
     */
    fun stopSession(): ReadingSession? {
        val (session, shouldPersist) = synchronized(stateLock) {
            if (!isTracking) return null
            val s = stopInternal(flushImmediately = false)
            s to true
        }
        if (shouldPersist && session != null) {
            persistAsync(session, pruneOld = true)
        }
        return session
    }

    /**
     * Suspends until the finalized session is stopped and committed to the repository.
     */
    suspend fun stopSessionAndAwait(): ReadingSession? {
        val session = synchronized(stateLock) {
            if (!isTracking) return null
            stopInternal(flushImmediately = false)
        } ?: return null

        persistInternal(session, pruneOld = true)
        return session
    }

    /**
     * Flushes current session progress to storage without ending the session.
     */
    fun flushSession() {
        val session = synchronized(stateLock) {
            if (!isTracking) null else currentSnapshot(clock())
        }
        session?.let { persistAsync(it) }
    }

    private fun stopInternal(flushImmediately: Boolean): ReadingSession? {
        val now = clock()
        val resumeTime = lastResumeTime
        if (resumeTime != null) {
            val elapsed = maxOf(0L, now - resumeTime)
            accumulatedActiveDuration += elapsed
            lastResumeTime = null
        }
        val session = currentSnapshot(now)
        isTracking = false
        activeSessionId = null
        currentBookId = null
        sessionStartTime = 0L
        accumulatedActiveDuration = 0L
        lastResumeTime = null
        startLocatorJson = null
        endLocatorJson = null
        startProgression = null
        endProgression = null

        if (flushImmediately && session != null) {
            persistAsync(session, pruneOld = true)
        }
        return session
    }

    private fun currentSnapshot(now: Long): ReadingSession? {
        val bookId = currentBookId ?: return null
        val activeTotal = accumulatedActiveDuration + (lastResumeTime?.let { maxOf(0L, now - it) } ?: 0L)
        return ReadingSession(
            id = activeSessionId ?: 0L,
            bookId = bookId,
            startTimeEpochMillis = sessionStartTime,
            endTimeEpochMillis = maxOf(sessionStartTime, now),
            activeDurationMillis = activeTotal,
            startProgression = startProgression,
            endProgression = endProgression,
            startLocatorJson = startLocatorJson,
            endLocatorJson = endLocatorJson
        )
    }

    private fun persistAsync(session: ReadingSession, pruneOld: Boolean = false) {
        scope.launch(ioDispatcher + NonCancellable) {
            persistInternal(session, pruneOld)
        }
    }

    private suspend fun persistInternal(session: ReadingSession, pruneOld: Boolean = false) {
        withContext(ioDispatcher) {
            persistMutex.withLock {
                try {
                    val idToUse = if (session.id > 0) {
                        session.id
                    } else {
                        synchronized(stateLock) { activeSessionId ?: lastRecordedSessionId } ?: 0L
                    }
                    if (idToUse > 0) {
                        repository.updateSession(session.copy(id = idToUse))
                    } else {
                        val assignedId = repository.recordSession(session)
                        synchronized(stateLock) {
                            lastRecordedSessionId = assignedId
                            if (isTracking && currentBookId == session.bookId && activeSessionId == null) {
                                activeSessionId = assignedId
                            }
                        }
                    }
                    if (pruneOld) {
                        repository.pruneOldSessions()
                    }
                } catch (_: Exception) {
                    // Ignore storage failures during teardown
                }
            }
        }
    }

    /**
     * Saves session tracking state into [outState] during Activity recreation
     * to avoid double-counting sessions across configuration changes or process death.
     */
    fun saveState(outState: Bundle) {
        synchronized(stateLock) {
            if (!isTracking) return
            val now = clock()
            val currentDuration = accumulatedActiveDuration + (lastResumeTime?.let { maxOf(0L, now - it) } ?: 0L)
            outState.putString(KEY_BOOK_ID, currentBookId)
            outState.putLong(KEY_SESSION_ID, activeSessionId ?: 0L)
            outState.putLong(KEY_START_TIME, sessionStartTime)
            outState.putLong(KEY_ACCUMULATED_DURATION, currentDuration)
            outState.putBoolean(KEY_IS_PAUSED, lastResumeTime == null)
            startProgression?.let { outState.putDouble(KEY_START_PROGRESSION, it) }
            endProgression?.let { outState.putDouble(KEY_END_PROGRESSION, it) }
            startLocatorJson?.let { outState.putString(KEY_START_LOCATOR, it) }
            endLocatorJson?.let { outState.putString(KEY_END_LOCATOR, it) }
        }
    }

    /**
     * Restores session state from [savedState]. Returns true if an active session
     * was restored, false otherwise.
     */
    fun restoreState(savedState: Bundle?): Boolean {
        if (savedState == null) return false
        synchronized(stateLock) {
            val bookId = savedState.getString(KEY_BOOK_ID) ?: return false
            val startTime = savedState.getLong(KEY_START_TIME, 0L)
            if (startTime == 0L) return false

            val savedSessionId = savedState.getLong(KEY_SESSION_ID, 0L)
            activeSessionId = if (savedSessionId > 0L) savedSessionId else null
            lastRecordedSessionId = activeSessionId
            currentBookId = bookId
            sessionStartTime = startTime
            accumulatedActiveDuration = savedState.getLong(KEY_ACCUMULATED_DURATION, 0L)
            val wasPaused = savedState.getBoolean(KEY_IS_PAUSED, false)
            lastResumeTime = if (wasPaused) null else clock()

            if (savedState.containsKey(KEY_START_PROGRESSION)) {
                startProgression = savedState.getDouble(KEY_START_PROGRESSION)
            }
            if (savedState.containsKey(KEY_END_PROGRESSION)) {
                endProgression = savedState.getDouble(KEY_END_PROGRESSION)
            }
            startLocatorJson = savedState.getString(KEY_START_LOCATOR)
            endLocatorJson = savedState.getString(KEY_END_LOCATOR)
            isTracking = true
            return true
        }
    }

    companion object {
        const val KEY_SESSION_ID = "reading_session_id"
        const val KEY_BOOK_ID = "reading_session_book_id"
        const val KEY_START_TIME = "reading_session_start_time"
        const val KEY_ACCUMULATED_DURATION = "reading_session_accumulated_duration"
        const val KEY_IS_PAUSED = "reading_session_is_paused"
        const val KEY_START_PROGRESSION = "reading_session_start_progression"
        const val KEY_END_PROGRESSION = "reading_session_end_progression"
        const val KEY_START_LOCATOR = "reading_session_start_locator"
        const val KEY_END_LOCATOR = "reading_session_end_locator"
    }
}
