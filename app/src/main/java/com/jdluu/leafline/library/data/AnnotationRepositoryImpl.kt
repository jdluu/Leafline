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

    private fun AnnotationEntity.toAnnotation(): Annotation {
        return Annotation(
            id = id,
            bookId = bookId,
            locatorJson = locatorJson,
            colorHex = colorHex,
            note = note,
            createdAt = createdAt
        )
    }
}
