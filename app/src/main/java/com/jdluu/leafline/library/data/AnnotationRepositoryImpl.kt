package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.data.local.AnnotationDao
import com.jdluu.leafline.library.data.local.AnnotationEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AnnotationRepositoryImpl(
    private val annotationDao: AnnotationDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis
) : AnnotationRepository {

    override fun observeAnnotations(bookId: String): Flow<List<Annotation>> {
        return annotationDao.observeForBook(bookId).map { entities ->
            entities.map { it.toAnnotation() }
        }
    }

    override suspend fun getAnnotations(bookId: String): List<Annotation> =
        withContext(ioDispatcher) {
            annotationDao.getForBook(bookId).map { it.toAnnotation() }
        }

    override suspend fun addAnnotation(
        bookId: String,
        locatorJson: String,
        colorHex: String,
        note: String?
    ): Long = withContext(ioDispatcher) {
        annotationDao.insert(
            AnnotationEntity(
                bookId = bookId,
                locatorJson = locatorJson,
                colorHex = colorHex,
                note = note,
                createdAt = clock()
            )
        )
    }

    override suspend fun removeAnnotation(id: Long) = withContext(ioDispatcher) {
        annotationDao.deleteById(id)
        Unit
    }

    override suspend fun restoreAnnotation(annotation: Annotation) = withContext(ioDispatcher) {
        // Reuses the normal insert path so autoincrement and defaults behave
        // identically; Room preserves an explicit non-zero autoGenerate id.
        annotationDao.insert(
            AnnotationEntity(
                id = annotation.id,
                bookId = annotation.bookId,
                locatorJson = annotation.locatorJson,
                colorHex = annotation.colorHex,
                note = annotation.note,
                createdAt = annotation.createdAt
            )
        )
        Unit
    }

    override suspend fun updateAnnotationNote(id: Long, note: String?): Int = withContext(ioDispatcher) {
        val normalizedNote = normalizeNote(note)
        annotationDao.updateNote(id, normalizedNote)
    }

    override suspend fun updateAnnotationColor(id: Long, colorHex: String): Int = withContext(ioDispatcher) {
        annotationDao.updateColor(id, colorHex)
    }

    private fun normalizeNote(note: String?): String? {
        return note?.trim()?.takeIf { it.isNotBlank() }
    }
}
