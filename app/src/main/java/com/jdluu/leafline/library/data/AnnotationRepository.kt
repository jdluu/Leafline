package com.jdluu.leafline.library.data

import kotlinx.coroutines.flow.Flow

interface AnnotationRepository {
    fun observeAnnotations(bookId: String): Flow<List<Annotation>>
    suspend fun getAnnotations(bookId: String): List<Annotation>
    suspend fun addAnnotation(
        bookId: String,
        locatorJson: String,
        colorHex: String = Annotation.DEFAULT_COLOR_HEX,
        note: String? = null
    ): Long
    suspend fun removeAnnotation(id: Long)
    suspend fun updateAnnotationNote(id: Long, note: String?): Int
    suspend fun updateAnnotationColor(id: Long, colorHex: String): Int
}
