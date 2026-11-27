package com.jdluu.leafline.library.backup

import org.json.JSONArray
import org.json.JSONObject

object LibraryBackupSerializer {

    fun serialize(backup: LibraryBackup): String {
        val root = JSONObject()
        root.put("version", backup.version)
        root.put("exportedAt", backup.exportedAt)
        root.put("appVersion", backup.appVersion)

        val booksArray = JSONArray()
        backup.books.forEach { book ->
            val bookObj = JSONObject()
            bookObj.put("stableId", book.stableId)
            bookObj.put("title", book.title)
            val authorsArray = JSONArray()
            book.authors.forEach { authorsArray.put(it) }
            bookObj.put("authors", authorsArray)

            val tagsArray = JSONArray()
            book.tags.forEach { tagsArray.put(it) }
            bookObj.put("tags", tagsArray)

            book.rating?.let { bookObj.put("rating", it) }
            book.readingStatus?.let { bookObj.put("readingStatus", it) }
            book.lastLocatorJson?.let { bookObj.put("lastLocatorJson", it) }
            book.series?.let { bookObj.put("series", it) }
            book.seriesIndex?.let { bookObj.put("seriesIndex", it.toDouble()) }
            book.koreaderHash?.let { bookObj.put("koreaderHash", it) }

            booksArray.put(bookObj)
        }
        root.put("books", booksArray)

        if (backup.readingSessions.isNotEmpty()) {
            val sessions = JSONArray()
            backup.readingSessions.forEach { session ->
                val obj = JSONObject()
                obj.put("bookStableId", session.bookStableId)
                obj.put("startTimeEpochMillis", session.startTimeEpochMillis)
                obj.put("endTimeEpochMillis", session.endTimeEpochMillis)
                obj.put("activeDurationMillis", session.activeDurationMillis)
                session.startProgression?.let { obj.put("startProgression", it) }
                session.endProgression?.let { obj.put("endProgression", it) }
                session.startLocatorJson?.let { obj.put("startLocatorJson", it) }
                session.endLocatorJson?.let { obj.put("endLocatorJson", it) }
                sessions.put(obj)
            }
            root.put("readingSessions", sessions)
        }

        if (backup.hardcoverTracking.isNotEmpty()) {
            val tracking = JSONObject()
            backup.hardcoverTracking.forEach { (stableId, choice) -> tracking.put(stableId, choice) }
            root.put("hardcoverTracking", tracking)
        }

        val collectionsArray = JSONArray()
        backup.collections.forEach { col ->
            val colObj = JSONObject()
            colObj.put("name", col.name)
            val bookIdsArray = JSONArray()
            col.bookStableIds.forEach { bookIdsArray.put(it) }
            colObj.put("bookStableIds", bookIdsArray)
            collectionsArray.put(colObj)
        }
        root.put("collections", collectionsArray)

        val bookmarksArray = JSONArray()
        backup.bookmarks.forEach { bm ->
            val bmObj = JSONObject()
            bmObj.put("bookStableId", bm.bookStableId)
            bmObj.put("locatorJson", bm.locatorJson)
            bm.label?.let { bmObj.put("label", it) }
            bmObj.put("createdAt", bm.createdAt)
            bookmarksArray.put(bmObj)
        }
        root.put("bookmarks", bookmarksArray)

        val annotationsArray = JSONArray()
        backup.annotations.forEach { ann ->
            val annObj = JSONObject()
            annObj.put("bookStableId", ann.bookStableId)
            annObj.put("locatorJson", ann.locatorJson)
            annObj.put("colorHex", ann.colorHex)
            ann.note?.let { annObj.put("note", it) }
            annObj.put("createdAt", ann.createdAt)
            annotationsArray.put(annObj)
        }
        root.put("annotations", annotationsArray)

        backup.preferences?.let { prefs ->
            root.put("preferences", serializePreferences(prefs))
        }

        return root.toString(2)
    }

    private fun serializePreferences(prefs: BackupPreferences): JSONObject {
        val obj = JSONObject()
        prefs.appearanceMode?.let { obj.put("appearanceMode", it) }
        prefs.libraryLayout?.let { obj.put("libraryLayout", it) }
        prefs.librarySort?.let { obj.put("librarySort", it) }
        prefs.librarySortAscending?.let { obj.put("librarySortAscending", it) }
        prefs.dailyGoalMinutes?.let { obj.put("dailyGoalMinutes", it) }
        prefs.booksGoalTarget?.let { obj.put("booksGoalTarget", it) }
        if (prefs.opdsFeeds.isNotEmpty()) {
            val feedsArray = JSONArray()
            prefs.opdsFeeds.forEach { feed ->
                val feedObj = JSONObject()
                feedObj.put("id", feed.id)
                feedObj.put("title", feed.title)
                feedObj.put("url", feed.url)
                feed.description?.let { feedObj.put("description", it) }
                feedsArray.put(feedObj)
            }
            obj.put("opdsFeeds", feedsArray)
        }
        prefs.readerPreferences?.let { reader ->
            obj.put("readerPreferences", serializeReaderPreferences(reader))
        }
        return obj
    }

    private fun serializeReaderPreferences(reader: ReaderPreferencesBackup): JSONObject {
        val obj = JSONObject()
        reader.theme?.let { obj.put("theme", it) }
        reader.fontFamily?.let { obj.put("fontFamily", it) }
        reader.fontSize?.let { obj.put("fontSize", it) }
        reader.lineHeight?.let { obj.put("lineHeight", it) }
        reader.pageMargins?.let { obj.put("pageMargins", it) }
        reader.publisherStyles?.let { obj.put("publisherStyles", it) }
        reader.scroll?.let { obj.put("scroll", it) }
        reader.hyphens?.let { obj.put("hyphens", it) }
        reader.columnCount?.let { obj.put("columnCount", it) }
        reader.tapZoneLeft?.let { obj.put("tapZoneLeft", it) }
        reader.tapZoneCenter?.let { obj.put("tapZoneCenter", it) }
        reader.tapZoneRight?.let { obj.put("tapZoneRight", it) }
        reader.tapZoneLayout?.let { obj.put("tapZoneLayout", it) }
        reader.pageTurnAnimation?.let { obj.put("pageTurnAnimation", it) }
        reader.brightness?.let { obj.put("brightness", it.toDouble()) }
        reader.preSepiaTheme?.let { obj.put("preSepiaTheme", it) }
        reader.reduceMotion?.let { obj.put("reduceMotion", it) }
        reader.previewFootnotes?.let { obj.put("previewFootnotes", it) }
        reader.keepScreenOn?.let { obj.put("keepScreenOn", it) }
        reader.orientation?.let { obj.put("orientation", it) }
        reader.showMinimalHud?.let { obj.put("showMinimalHud", it) }
        reader.minimalHudMode?.let { obj.put("minimalHudMode", it) }
        reader.hapticFeedback?.let { obj.put("hapticFeedback", it) }
        reader.volumeKeyNavigation?.let { obj.put("volumeKeyNavigation", it) }
        reader.invertVolumeKeys?.let { obj.put("invertVolumeKeys", it) }
        return obj
    }

    fun deserialize(jsonString: String): LibraryBackup {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 1)
        val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
        val appVersion = root.optString("appVersion", "1.0")

        val books = mutableListOf<BookBackup>()
        val booksArray = root.optJSONArray("books")
        if (booksArray != null) {
            for (i in 0 until booksArray.length()) {
                val obj = booksArray.optJSONObject(i) ?: continue
                val stableId = obj.optString("stableId")
                val title = obj.optString("title")
                val authors = mutableListOf<String>()
                val authorsArray = obj.optJSONArray("authors")
                if (authorsArray != null) {
                    for (j in 0 until authorsArray.length()) {
                        authors.add(authorsArray.getString(j))
                    }
                }
                val tags = mutableListOf<String>()
                val tagsArray = obj.optJSONArray("tags")
                if (tagsArray != null) {
                    for (j in 0 until tagsArray.length()) {
                        tags.add(tagsArray.getString(j))
                    }
                }
                val rating = obj.optNullableInt("rating")
                val readingStatus = obj.optNullableString("readingStatus")
                val lastLocatorJson = obj.optNullableString("lastLocatorJson")
                val series = obj.optNullableString("series")
                val seriesIndex = obj.optNullableDouble("seriesIndex")?.toFloat()
                val koreaderHash = obj.optNullableString("koreaderHash")

                if (stableId.isNotBlank() && title.isNotBlank()) {
                    books.add(
                        BookBackup(
                            stableId = stableId,
                            title = title,
                            authors = authors,
                            tags = tags,
                            rating = rating,
                            readingStatus = readingStatus,
                            lastLocatorJson = lastLocatorJson,
                            series = series,
                            seriesIndex = seriesIndex,
                            koreaderHash = koreaderHash
                        )
                    )
                }
            }
        }

        val collections = mutableListOf<CollectionBackup>()
        val collectionsArray = root.optJSONArray("collections")
        if (collectionsArray != null) {
            for (i in 0 until collectionsArray.length()) {
                val obj = collectionsArray.optJSONObject(i) ?: continue
                val name = obj.optString("name")
                val bookStableIds = mutableListOf<String>()
                val bookIdsArray = obj.optJSONArray("bookStableIds")
                if (bookIdsArray != null) {
                    for (j in 0 until bookIdsArray.length()) {
                        bookStableIds.add(bookIdsArray.getString(j))
                    }
                }
                if (name.isNotBlank()) {
                    collections.add(CollectionBackup(name = name, bookStableIds = bookStableIds))
                }
            }
        }

        val bookmarks = mutableListOf<BookmarkBackup>()
        val bookmarksArray = root.optJSONArray("bookmarks")
        if (bookmarksArray != null) {
            for (i in 0 until bookmarksArray.length()) {
                val obj = bookmarksArray.optJSONObject(i) ?: continue
                val bookStableId = obj.optString("bookStableId")
                val locatorJson = obj.optString("locatorJson")
                val label = obj.optNullableString("label")
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                if (bookStableId.isNotBlank() && locatorJson.isNotBlank()) {
                    bookmarks.add(
                        BookmarkBackup(
                            bookStableId = bookStableId,
                            locatorJson = locatorJson,
                            label = label,
                            createdAt = createdAt
                        )
                    )
                }
            }
        }

        val annotations = mutableListOf<AnnotationBackup>()
        val annotationsArray = root.optJSONArray("annotations")
        if (annotationsArray != null) {
            for (i in 0 until annotationsArray.length()) {
                val obj = annotationsArray.optJSONObject(i) ?: continue
                val bookStableId = obj.optString("bookStableId")
                val locatorJson = obj.optString("locatorJson")
                val colorHex = obj.optString("colorHex", "#FFE082")
                val note = obj.optNullableString("note")
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                if (bookStableId.isNotBlank() && locatorJson.isNotBlank()) {
                    annotations.add(
                        AnnotationBackup(
                            bookStableId = bookStableId,
                            locatorJson = locatorJson,
                            colorHex = colorHex,
                            note = note,
                            createdAt = createdAt
                        )
                    )
                }
            }
        }

        val preferences = root.optJSONObject("preferences")?.let(::deserializePreferences)

        return LibraryBackup(
            readingSessions = root.optJSONArray("readingSessions")?.let { array ->
                (0 until array.length()).mapNotNull { index ->
                    val obj = array.optJSONObject(index) ?: return@mapNotNull null
                    val book = obj.optString("bookStableId")
                    if (book.isBlank()) return@mapNotNull null
                    ReadingSessionBackup(
                        bookStableId = book,
                        startTimeEpochMillis = obj.optLong("startTimeEpochMillis"),
                        endTimeEpochMillis = obj.optLong("endTimeEpochMillis"),
                        activeDurationMillis = obj.optLong("activeDurationMillis"),
                        startProgression = obj.optNullableDouble("startProgression"),
                        endProgression = obj.optNullableDouble("endProgression"),
                        startLocatorJson = obj.optNullableString("startLocatorJson"),
                        endLocatorJson = obj.optNullableString("endLocatorJson")
                    )
                }
            }.orEmpty(),
            hardcoverTracking = root.optJSONObject("hardcoverTracking")?.let { obj ->
                obj.keys().asSequence().associateWith { key -> obj.optString(key) }
            }.orEmpty(),
            version = version,
            exportedAt = exportedAt,
            appVersion = appVersion,
            books = books,
            collections = collections,
            bookmarks = bookmarks,
            annotations = annotations,
            preferences = preferences
        )
    }

    private fun deserializePreferences(obj: JSONObject): BackupPreferences {
        val opdsFeeds = obj.optJSONArray("opdsFeeds")?.let { array ->
            (0 until array.length()).mapNotNull { index ->
                val feedObj = array.optJSONObject(index) ?: return@mapNotNull null
                val title = feedObj.optNullableString("title") ?: return@mapNotNull null
                val url = feedObj.optNullableString("url") ?: return@mapNotNull null
                val id = feedObj.optNullableString("id") ?: url
                OpdsFeedBackup(
                    id = id,
                    title = title,
                    url = url,
                    description = feedObj.optNullableString("description")
                )
            }
        }.orEmpty()

        return BackupPreferences(
            appearanceMode = obj.optNullableString("appearanceMode"),
            libraryLayout = obj.optNullableString("libraryLayout"),
            librarySort = obj.optNullableString("librarySort"),
            librarySortAscending = obj.optNullableBoolean("librarySortAscending"),
            dailyGoalMinutes = obj.optNullableInt("dailyGoalMinutes"),
            booksGoalTarget = obj.optNullableInt("booksGoalTarget"),
            opdsFeeds = opdsFeeds,
            readerPreferences = obj.optJSONObject("readerPreferences")?.let(::deserializeReaderPreferences)
        )
    }

    private fun deserializeReaderPreferences(obj: JSONObject): ReaderPreferencesBackup {
        return ReaderPreferencesBackup(
            theme = obj.optNullableString("theme"),
            fontFamily = obj.optNullableString("fontFamily"),
            fontSize = obj.optNullableDouble("fontSize"),
            lineHeight = obj.optNullableDouble("lineHeight"),
            pageMargins = obj.optNullableDouble("pageMargins"),
            publisherStyles = obj.optNullableBoolean("publisherStyles"),
            scroll = obj.optNullableBoolean("scroll"),
            hyphens = obj.optNullableBoolean("hyphens"),
            columnCount = obj.optNullableString("columnCount"),
            tapZoneLeft = obj.optNullableString("tapZoneLeft"),
            tapZoneCenter = obj.optNullableString("tapZoneCenter"),
            tapZoneRight = obj.optNullableString("tapZoneRight"),
            tapZoneLayout = obj.optNullableString("tapZoneLayout"),
            pageTurnAnimation = obj.optNullableString("pageTurnAnimation"),
            brightness = obj.optNullableDouble("brightness")?.toFloat(),
            preSepiaTheme = obj.optNullableString("preSepiaTheme"),
            reduceMotion = obj.optNullableBoolean("reduceMotion"),
            previewFootnotes = obj.optNullableBoolean("previewFootnotes"),
            keepScreenOn = obj.optNullableBoolean("keepScreenOn"),
            orientation = obj.optNullableString("orientation"),
            showMinimalHud = obj.optNullableBoolean("showMinimalHud"),
            minimalHudMode = obj.optNullableString("minimalHudMode"),
            hapticFeedback = obj.optNullableBoolean("hapticFeedback"),
            volumeKeyNavigation = obj.optNullableBoolean("volumeKeyNavigation"),
            invertVolumeKeys = obj.optNullableBoolean("invertVolumeKeys")
        )
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (has(key) && !isNull(key)) optString(key) else null

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key) else null

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val value = optDouble(key, Double.NaN)
        return if (value.isNaN()) null else value
    }

    private fun JSONObject.optNullableBoolean(key: String): Boolean? =
        if (has(key) && !isNull(key)) optBoolean(key) else null
}
