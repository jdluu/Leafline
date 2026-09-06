package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.data.local.AnnotationEntity
import com.jdluu.leafline.library.data.local.BookmarkEntity
import com.jdluu.leafline.library.data.local.CollectionEntity
import com.jdluu.leafline.library.data.local.ReadingSessionEntity

fun BookmarkEntity.toBookmark(): Bookmark {
    return Bookmark(
        id = id,
        bookId = bookId,
        locatorJson = locatorJson,
        createdAt = createdAt,
        label = label
    )
}

fun AnnotationEntity.toAnnotation(): Annotation {
    return Annotation(
        id = id,
        bookId = bookId,
        locatorJson = locatorJson,
        colorHex = colorHex,
        note = note,
        createdAt = createdAt
    )
}

fun CollectionEntity.toCollection(): Collection {
    return Collection(
        id = id,
        name = name,
        createdAtEpochMillis = createdAtEpochMillis
    )
}

fun ReadingSessionEntity.toReadingSession(): ReadingSession {
    return ReadingSession(
        id = id,
        bookId = bookId,
        startTimeEpochMillis = startTimeEpochMillis,
        endTimeEpochMillis = endTimeEpochMillis,
        activeDurationMillis = activeDurationMillis,
        startProgression = startProgression,
        endProgression = endProgression,
        startLocatorJson = startLocatorJson,
        endLocatorJson = endLocatorJson
    )
}

fun ReadingSession.toEntity(): ReadingSessionEntity {
    return ReadingSessionEntity(
        id = id,
        bookId = bookId,
        startTimeEpochMillis = startTimeEpochMillis,
        endTimeEpochMillis = endTimeEpochMillis,
        activeDurationMillis = activeDurationMillis,
        startProgression = startProgression,
        endProgression = endProgression,
        startLocatorJson = startLocatorJson,
        endLocatorJson = endLocatorJson
    )
}