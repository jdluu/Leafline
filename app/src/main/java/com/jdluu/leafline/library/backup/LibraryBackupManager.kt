package com.jdluu.leafline.library.backup

import com.jdluu.leafline.hardcover.HardcoverBookPreference
import com.jdluu.leafline.hardcover.HardcoverTrackingStore
import com.jdluu.leafline.library.LibraryLayout
import com.jdluu.leafline.library.LibraryLayoutStore
import com.jdluu.leafline.library.LibrarySort
import com.jdluu.leafline.library.LibrarySortStore
import com.jdluu.leafline.library.ReadingGoalStore
import com.jdluu.leafline.library.ReadingGoals
import com.jdluu.leafline.library.data.local.AnnotationDao
import com.jdluu.leafline.library.data.local.AnnotationEntity
import com.jdluu.leafline.library.data.local.BookCollectionCrossRef
import com.jdluu.leafline.library.data.local.BookDao
import com.jdluu.leafline.library.data.local.BookmarkDao
import com.jdluu.leafline.library.data.local.BookmarkEntity
import com.jdluu.leafline.library.data.local.CollectionDao
import com.jdluu.leafline.library.data.local.CollectionEntity
import com.jdluu.leafline.library.data.local.ReadingSessionDao
import com.jdluu.leafline.library.data.local.ReadingSessionEntity
import com.jdluu.leafline.opds.OpdsFeedStore
import com.jdluu.leafline.reader.MinimalHudMode
import com.jdluu.leafline.reader.PageTurnAnimation
import com.jdluu.leafline.reader.ReaderOrientation
import com.jdluu.leafline.reader.ReaderPreferencesStore
import com.jdluu.leafline.reader.ReaderSettings
import com.jdluu.leafline.reader.StyleMode
import com.jdluu.leafline.reader.TapZoneAction
import com.jdluu.leafline.reader.TapZoneConfig
import com.jdluu.leafline.reader.TapZoneLayout
import com.jdluu.leafline.reader.clampBrightness
import com.jdluu.leafline.reader.fontFamilyFromStoredName
import com.jdluu.leafline.reader.snapFontSize
import com.jdluu.leafline.reader.snapPageMargins
import com.jdluu.leafline.reader.theme.ReaderTheme
import com.jdluu.leafline.reader.withStyleMode
import com.jdluu.leafline.theme.AppearanceStore
import com.jdluu.leafline.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.shared.ExperimentalReadiumApi
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalReadiumApi::class)
class LibraryBackupManager(
    private val bookDao: BookDao,
    private val collectionDao: CollectionDao,
    private val bookmarkDao: BookmarkDao,
    private val annotationDao: AnnotationDao,
    private val readingSessionDao: ReadingSessionDao,
    /** Recorded in the file so a shared backup can be traced to the build that wrote it. */
    private val appVersion: String = "1.0",
    /**
     * Where the reader's per-book Hardcover choices live. Optional so the manager can
     * still be built without one; null simply means none are exported or restored.
     */
    private val hardcoverTracking: HardcoverTrackingStore? = null,
    private val appearanceStore: AppearanceStore? = null,
    private val libraryLayoutStore: LibraryLayoutStore? = null,
    private val librarySortStore: LibrarySortStore? = null,
    private val readingGoalStore: ReadingGoalStore? = null,
    private val readerPreferencesStore: ReaderPreferencesStore? = null,
    private val opdsFeedStore: OpdsFeedStore? = null
) {

    suspend fun createBackup(): LibraryBackup = withContext(Dispatchers.IO) {
        val allBooks = bookDao.getAllBooksList()
        // Only decisions the reader actually made; an unanswered book is absent rather
        // than exported as a default, so a restore cannot invent a choice.
        val trackingChoices = hardcoverTracking
            ?.let { store ->
                allBooks.mapNotNull { entity ->
                    store.preferenceFor(entity.stableId)?.let { pref ->
                        entity.stableId to pref.name
                    }
                }.toMap()
            }
            .orEmpty()

        val sessions = readingSessionDao.getAll().map { session ->
            ReadingSessionBackup(
                bookStableId = session.bookId,
                startTimeEpochMillis = session.startTimeEpochMillis,
                endTimeEpochMillis = session.endTimeEpochMillis,
                activeDurationMillis = session.activeDurationMillis,
                startProgression = session.startProgression,
                endProgression = session.endProgression,
                startLocatorJson = session.startLocatorJson,
                endLocatorJson = session.endLocatorJson
            )
        }

        val books = allBooks.map { entity ->
            BookBackup(
                stableId = entity.stableId,
                title = entity.title,
                authors = entity.authors,
                tags = entity.tags,
                rating = entity.rating,
                readingStatus = entity.readingStatus,
                lastLocatorJson = entity.lastLocatorJson,
                series = entity.series,
                seriesIndex = entity.seriesIndex,
                koreaderHash = entity.koreaderHash
            )
        }

        val collections = collectionDao.getAllCollectionsList().map { col ->
            val bookIds = collectionDao.getBookIdsForCollection(col.id).first()
            CollectionBackup(
                name = col.name,
                bookStableIds = bookIds
            )
        }

        val bookmarks = bookmarkDao.getAllBookmarks().map { bm ->
            BookmarkBackup(
                bookStableId = bm.bookId,
                locatorJson = bm.locatorJson,
                label = bm.label,
                createdAt = bm.createdAt
            )
        }

        val annotations = annotationDao.getAllAnnotations().map { ann ->
            AnnotationBackup(
                bookStableId = ann.bookId,
                locatorJson = ann.locatorJson,
                colorHex = ann.colorHex,
                note = ann.note,
                createdAt = ann.createdAt
            )
        }

        val preferences = buildPreferencesBackup()

        LibraryBackup(
            version = 4,
            exportedAt = System.currentTimeMillis(),
            appVersion = appVersion,
            books = books,
            collections = collections,
            bookmarks = bookmarks,
            annotations = annotations,
            hardcoverTracking = trackingChoices,
            readingSessions = sessions,
            preferences = preferences
        )
    }

    private fun buildPreferencesBackup(): BackupPreferences? {
        if (appearanceStore == null &&
            libraryLayoutStore == null &&
            librarySortStore == null &&
            readingGoalStore == null &&
            readerPreferencesStore == null &&
            opdsFeedStore == null
        ) {
            return null
        }

        val loadedSort = librarySortStore?.load()
        val loadedGoals = readingGoalStore?.loadGoals()
        val readerPrefs = readerPreferencesStore?.load()?.let { settings ->
            val epub = settings.epub
            ReaderPreferencesBackup(
                theme = settings.theme?.name,
                fontFamily = epub.fontFamily?.name,
                fontSize = epub.fontSize,
                lineHeight = epub.lineHeight,
                pageMargins = epub.pageMargins,
                publisherStyles = epub.publisherStyles,
                scroll = epub.scroll,
                hyphens = epub.hyphens,
                columnCount = epub.columnCount?.name,
                tapZoneLeft = settings.tapZoneConfig.leftZone.name,
                tapZoneCenter = settings.tapZoneConfig.centerZone.name,
                tapZoneRight = settings.tapZoneConfig.rightZone.name,
                tapZoneLayout = settings.tapZoneConfig.layout.name,
                pageTurnAnimation = settings.pageTurnAnimation.name,
                brightness = settings.brightness,
                preSepiaTheme = settings.preSepiaTheme?.name,
                reduceMotion = settings.reduceMotion,
                previewFootnotes = settings.previewFootnotes,
                keepScreenOn = settings.keepScreenOn,
                orientation = settings.orientation.name,
                showMinimalHud = settings.showMinimalHud,
                minimalHudMode = settings.minimalHudMode.name,
                hapticFeedback = settings.hapticFeedback,
                volumeKeyNavigation = settings.volumeKeyNavigation,
                invertVolumeKeys = settings.invertVolumeKeys
            )
        }

        // Only carry non-secret catalog metadata; credentials are never backed up.
        val opdsFeeds = opdsFeedStore?.loadFeeds()?.map { source ->
            OpdsFeedBackup(
                id = source.id,
                title = source.title,
                url = source.url,
                description = source.description
            )
        }.orEmpty()

        return BackupPreferences(
            appearanceMode = appearanceStore?.load()?.name,
            libraryLayout = libraryLayoutStore?.load()?.name,
            librarySort = loadedSort?.name,
            librarySortAscending = if (librarySortStore != null && loadedSort != null) {
                librarySortStore.loadAscending(loadedSort.defaultAscending)
            } else {
                null
            },
            dailyGoalMinutes = loadedGoals?.dailyMinutes,
            booksGoalTarget = loadedGoals?.booksTarget,
            opdsFeeds = opdsFeeds,
            readerPreferences = readerPrefs
        )
    }

    suspend fun exportBackup(outputStream: OutputStream): Unit = withContext(Dispatchers.IO) {
        val backup = createBackup()
        val json = LibraryBackupSerializer.serialize(backup)
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(json)
            writer.flush()
        }
    }

    suspend fun restoreBackup(backup: LibraryBackup): BackupRestoreSummary = withContext(Dispatchers.IO) {
        val currentBooks = bookDao.getAllBooksList()
        val bookByStableId = currentBooks.associateBy { it.stableId }.toMutableMap()
        val bookByTitleAuthors = currentBooks.associateBy { "${it.title.trim().lowercase()}|${it.authors.joinToString(",").trim().lowercase()}" }

        var booksMatched = 0
        var tagsUpdated = 0
        var collectionsRestored = 0
        var bookmarksRestored = 0
        var annotationsRestored = 0

        // Map backup stable IDs to actual current library stable IDs
        val backupToCurrentIdMap = mutableMapOf<String, String>()

        for (backupBook in backup.books) {
            val localBook = bookByStableId[backupBook.stableId]
                ?: bookByTitleAuthors["${backupBook.title.trim().lowercase()}|${backupBook.authors.joinToString(",").trim().lowercase()}"]

            if (localBook != null) {
                booksMatched++
                backupToCurrentIdMap[backupBook.stableId] = localBook.stableId

                // Merge tags
                if (backupBook.tags.isNotEmpty()) {
                    val mergedTags = (localBook.tags + backupBook.tags).distinct()
                    if (mergedTags.size != localBook.tags.size) {
                        bookDao.setTags(localBook.stableId, mergedTags)
                        tagsUpdated++
                    }
                }

                // Restore rating if not set locally
                if (localBook.rating == null && backupBook.rating != null) {
                    bookDao.setRating(localBook.stableId, backupBook.rating)
                }

                // Restore reading status if not set locally
                if ((localBook.readingStatus.isBlank() || localBook.readingStatus == "unread") && !backupBook.readingStatus.isNullOrBlank()) {
                    bookDao.setReadingStatus(localBook.stableId, backupBook.readingStatus)
                }

                // Restore last reading locator if not present locally
                if (localBook.lastLocatorJson == null && backupBook.lastLocatorJson != null) {
                    bookDao.updateLastLocator(localBook.stableId, backupBook.lastLocatorJson, System.currentTimeMillis())
                }

                // Restore series metadata when missing locally
                val restoredSeries = if (localBook.series.isNullOrBlank() && !backupBook.series.isNullOrBlank()) {
                    backupBook.series
                } else {
                    localBook.series
                }
                val restoredSeriesIndex = if (localBook.seriesIndex == null && backupBook.seriesIndex != null) {
                    backupBook.seriesIndex
                } else {
                    localBook.seriesIndex
                }
                if (restoredSeries != localBook.series || restoredSeriesIndex != localBook.seriesIndex) {
                    bookDao.setSeries(localBook.stableId, restoredSeries, restoredSeriesIndex)
                }

                // Restore KOReader hash if missing locally
                if (localBook.koreaderHash.isNullOrBlank() && !backupBook.koreaderHash.isNullOrBlank()) {
                    bookDao.setKoreaderHash(localBook.stableId, backupBook.koreaderHash)
                }
            }
        }

        // Restore Collections
        for (colBackup in backup.collections) {
            val existingCol = collectionDao.getCollectionByName(colBackup.name)
            val collectionId = existingCol?.id ?: collectionDao.insert(
                CollectionEntity(name = colBackup.name, createdAtEpochMillis = System.currentTimeMillis())
            )
            collectionsRestored++

            for (backupBookId in colBackup.bookStableIds) {
                val currentBookId = backupToCurrentIdMap[backupBookId]
                if (currentBookId != null) {
                    collectionDao.addBookToCollection(
                        BookCollectionCrossRef(
                            bookStableId = currentBookId,
                            collectionId = collectionId
                        )
                    )
                }
            }
        }

        // Restore Bookmarks
        for (bmBackup in backup.bookmarks) {
            val currentBookId = backupToCurrentIdMap[bmBackup.bookStableId]
            if (currentBookId != null) {
                val existingBookmarks = bookmarkDao.getForBook(currentBookId)
                val alreadyExists = existingBookmarks.any { it.locatorJson == bmBackup.locatorJson }
                if (!alreadyExists) {
                    bookmarkDao.insert(
                        BookmarkEntity(
                            bookId = currentBookId,
                            locatorJson = bmBackup.locatorJson,
                            label = bmBackup.label,
                            createdAt = bmBackup.createdAt
                        )
                    )
                    bookmarksRestored++
                }
            }
        }

        // Restore Annotations
        for (annBackup in backup.annotations) {
            val currentBookId = backupToCurrentIdMap[annBackup.bookStableId]
            if (currentBookId != null) {
                val existingAnnotations = annotationDao.getForBook(currentBookId)
                val alreadyExists = existingAnnotations.any { it.locatorJson == annBackup.locatorJson }
                if (!alreadyExists) {
                    annotationDao.insert(
                        AnnotationEntity(
                            bookId = currentBookId,
                            locatorJson = annBackup.locatorJson,
                            colorHex = annBackup.colorHex,
                            note = annBackup.note,
                            createdAt = annBackup.createdAt
                        )
                    )
                    annotationsRestored++
                }
            }
        }

        // Restored last, and only where nothing is decided locally: a backup must not
        // override a choice the reader made on this device.
        var hardcoverChoicesRestored = 0
        if (hardcoverTracking != null && backup.hardcoverTracking.isNotEmpty()) {
            backup.hardcoverTracking.forEach { (stableId, name) ->
                val preference = runCatching { HardcoverBookPreference.valueOf(name) }.getOrNull()
                if (preference != null && hardcoverTracking.preferenceFor(stableId) == null) {
                    hardcoverTracking.setPreference(stableId, preference)
                    hardcoverChoicesRestored++
                }
            }
        }

        // Sessions are restored last, because a session without its book is an orphan.
        // Identity is (book, start, end): importing the same file twice must not double
        // count the reader's hours.
        var readingSessionsRestored = 0
        var readingSessionsSkipped = 0
        if (backup.readingSessions.isNotEmpty()) {
            val knownBooks = bookDao.getAllBooksList().map { it.stableId }.toSet()
            val existing = readingSessionDao.getAll()
                .map { Triple(it.bookId, it.startTimeEpochMillis, it.endTimeEpochMillis) }
                .toMutableSet()

            backup.readingSessions.forEach { session ->
                when {
                    session.bookStableId !in knownBooks -> readingSessionsSkipped++
                    !existing.add(session.identity) -> readingSessionsSkipped++
                    else -> {
                        readingSessionDao.insert(
                            ReadingSessionEntity(
                                bookId = session.bookStableId,
                                startTimeEpochMillis = session.startTimeEpochMillis,
                                endTimeEpochMillis = session.endTimeEpochMillis,
                                activeDurationMillis = session.activeDurationMillis,
                                startProgression = session.startProgression,
                                endProgression = session.endProgression,
                                startLocatorJson = session.startLocatorJson,
                                endLocatorJson = session.endLocatorJson
                            )
                        )
                        readingSessionsRestored++
                    }
                }
            }
        }

        val (preferencesRestored, opdsFeedsRestored) = restorePreferences(backup.preferences)

        BackupRestoreSummary(
            booksMatched = booksMatched,
            tagsUpdated = tagsUpdated,
            collectionsRestored = collectionsRestored,
            bookmarksRestored = bookmarksRestored,
            annotationsRestored = annotationsRestored,
            hardcoverChoicesRestored = hardcoverChoicesRestored,
            readingSessionsRestored = readingSessionsRestored,
            readingSessionsSkipped = readingSessionsSkipped,
            preferencesRestored = preferencesRestored,
            opdsFeedsRestored = opdsFeedsRestored
        )
    }

    private fun restorePreferences(prefs: BackupPreferences?): Pair<Boolean, Int> {
        if (prefs == null) return false to 0
        var restoredAnyPreference = false
        var opdsRestored = 0

        if (appearanceStore != null && prefs.appearanceMode != null) {
            enumOrNull<ThemeMode>(prefs.appearanceMode)?.let { mode ->
                appearanceStore.save(mode)
                restoredAnyPreference = true
            }
        }

        if (libraryLayoutStore != null && prefs.libraryLayout != null) {
            enumOrNull<LibraryLayout>(prefs.libraryLayout)?.let { layout ->
                libraryLayoutStore.save(layout)
                restoredAnyPreference = true
            }
        }

        if (librarySortStore != null) {
            prefs.librarySort?.let { sortName ->
                enumOrNull<LibrarySort>(sortName)?.let { sort ->
                    librarySortStore.save(sort)
                    restoredAnyPreference = true
                }
            }
            prefs.librarySortAscending?.let { ascending ->
                librarySortStore.saveAscending(ascending)
                restoredAnyPreference = true
            }
        }

        if (readingGoalStore != null && (prefs.dailyGoalMinutes != null || prefs.booksGoalTarget != null)) {
            val current = readingGoalStore.loadGoals()
            readingGoalStore.saveGoals(
                ReadingGoals(
                    dailyMinutes = (prefs.dailyGoalMinutes ?: current.dailyMinutes).coerceAtLeast(1),
                    booksTarget = (prefs.booksGoalTarget ?: current.booksTarget).coerceAtLeast(1)
                )
            )
            restoredAnyPreference = true
        }

        if (opdsFeedStore != null && prefs.opdsFeeds.isNotEmpty()) {
            val existingUrls = opdsFeedStore.loadFeeds().map { it.url.trim() }.toMutableSet()
            prefs.opdsFeeds.forEach { feed ->
                val url = feed.url.trim()
                if (url.isNotBlank() && existingUrls.add(url)) {
                    opdsFeedStore.addCustomFeed(title = feed.title, url = url)
                    opdsRestored++
                }
            }
            if (opdsRestored > 0) {
                restoredAnyPreference = true
            }
        }

        if (readerPreferencesStore != null && prefs.readerPreferences != null) {
            readerPreferencesStore.save(
                buildRestoredReaderSettings(
                    backup = prefs.readerPreferences,
                    fallback = readerPreferencesStore.load(),
                    store = readerPreferencesStore
                )
            )
            restoredAnyPreference = true
        }

        return restoredAnyPreference to opdsRestored
    }

    private fun buildRestoredReaderSettings(
        backup: ReaderPreferencesBackup,
        fallback: ReaderSettings,
        store: ReaderPreferencesStore
    ): ReaderSettings {
        val currentEpub = fallback.epub
        val rawEpub = EpubPreferences(
            fontFamily = backup.fontFamily?.let { storedName ->
                fontFamilyFromStoredName(storedName) { store.isCustomFont(it) }
            } ?: currentEpub.fontFamily,
            lineHeight = backup.lineHeight ?: currentEpub.lineHeight,
            fontSize = (backup.fontSize ?: currentEpub.fontSize)?.let(::snapFontSize),
            pageMargins = (backup.pageMargins ?: currentEpub.pageMargins)?.let(::snapPageMargins),
            publisherStyles = backup.publisherStyles ?: currentEpub.publisherStyles,
            scroll = backup.scroll ?: currentEpub.scroll,
            hyphens = backup.hyphens ?: currentEpub.hyphens,
            columnCount = backup.columnCount?.let { enumOrNull<ColumnCount>(it) } ?: currentEpub.columnCount
        )
        val normalizedEpub = if (rawEpub.publisherStyles == true) {
            rawEpub.withStyleMode(StyleMode.PUBLISHER)
        } else {
            rawEpub
        }

        val tapZones = TapZoneConfig(
            leftZone = backup.tapZoneLeft?.let { enumOrNull<TapZoneAction>(it) } ?: fallback.tapZoneConfig.leftZone,
            centerZone = backup.tapZoneCenter?.let { enumOrNull<TapZoneAction>(it) } ?: fallback.tapZoneConfig.centerZone,
            rightZone = backup.tapZoneRight?.let { enumOrNull<TapZoneAction>(it) } ?: fallback.tapZoneConfig.rightZone,
            layout = backup.tapZoneLayout?.let { enumOrNull<TapZoneLayout>(it) } ?: fallback.tapZoneConfig.layout
        )

        val restoredTheme = backup.theme?.let { enumOrNull<ReaderTheme>(it) } ?: fallback.theme
        val restoredPreSepia = backup.preSepiaTheme
            ?.let { enumOrNull<ReaderTheme>(it) }
            ?.takeUnless { it == ReaderTheme.SEPIA }
            ?: fallback.preSepiaTheme

        return fallback.copy(
            epub = normalizedEpub,
            theme = restoredTheme,
            tapZoneConfig = tapZones,
            pageTurnAnimation = backup.pageTurnAnimation?.let { enumOrNull<PageTurnAnimation>(it) }
                ?: fallback.pageTurnAnimation,
            brightness = (backup.brightness ?: fallback.brightness)?.let(::clampBrightness),
            preSepiaTheme = restoredPreSepia,
            reduceMotion = backup.reduceMotion ?: fallback.reduceMotion,
            previewFootnotes = backup.previewFootnotes ?: fallback.previewFootnotes,
            keepScreenOn = backup.keepScreenOn ?: fallback.keepScreenOn,
            orientation = backup.orientation?.let { enumOrNull<ReaderOrientation>(it) }
                ?: fallback.orientation,
            showMinimalHud = backup.showMinimalHud ?: fallback.showMinimalHud,
            minimalHudMode = backup.minimalHudMode?.let { enumOrNull<MinimalHudMode>(it) }
                ?: fallback.minimalHudMode,
            hapticFeedback = backup.hapticFeedback ?: fallback.hapticFeedback,
            volumeKeyNavigation = backup.volumeKeyNavigation ?: fallback.volumeKeyNavigation,
            invertVolumeKeys = backup.invertVolumeKeys ?: fallback.invertVolumeKeys
        )
    }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? =
        runCatching { enumValueOf<T>(name) }.getOrNull()

    suspend fun importBackup(inputStream: InputStream): BackupRestoreSummary = withContext(Dispatchers.IO) {
        val json = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val backup = LibraryBackupSerializer.deserialize(json)
        restoreBackup(backup)
    }
}
