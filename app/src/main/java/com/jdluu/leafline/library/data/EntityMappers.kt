package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.data.local.AnnotationEntity
import com.jdluu.leafline.library.data.local.BookmarkEntity
import com.jdluu.leafline.library.data.local.CollectionEntity

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