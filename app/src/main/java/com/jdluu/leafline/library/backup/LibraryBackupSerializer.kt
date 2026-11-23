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

            booksArray.put(bookObj)
        }
        root.put("books", booksArray)

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

        return root.toString(2)
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
                val rating = if (obj.has("rating")) obj.optInt("rating") else null
                val readingStatus = if (obj.has("readingStatus")) obj.optString("readingStatus") else null
                val lastLocatorJson = if (obj.has("lastLocatorJson")) obj.optString("lastLocatorJson") else null

                if (stableId.isNotBlank() && title.isNotBlank()) {
                    books.add(
                        BookBackup(
                            stableId = stableId,
                            title = title,
                            authors = authors,
                            tags = tags,
                            rating = rating,
                            readingStatus = readingStatus,
                            lastLocatorJson = lastLocatorJson
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
                val label = if (obj.has("label")) obj.optString("label") else null
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
                val note = if (obj.has("note")) obj.optString("note") else null
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

        return LibraryBackup(
            hardcoverTracking = root.optJSONObject("hardcoverTracking")?.let { obj ->
                obj.keys().asSequence().associateWith { key -> obj.optString(key) }
            }.orEmpty(),
            version = version,
            exportedAt = exportedAt,
            appVersion = appVersion,
            books = books,
            collections = collections,
            bookmarks = bookmarks,
            annotations = annotations
        )
    }
}
