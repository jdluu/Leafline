package com.jdluu.leafline.library.data

import com.jdluu.leafline.library.data.local.AnnotationDao
import com.jdluu.leafline.library.data.local.AnnotationEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnotationRepositoryTest {

    private class FakeAnnotationDao : AnnotationDao {
        val stored = MutableStateFlow<List<AnnotationEntity>>(emptyList())
        private var nextId = 1L

        override fun observeForBook(bookId: String): Flow<List<AnnotationEntity>> {
            return stored.map { list ->
                list.filter { it.bookId == bookId }
                    .sortedWith(
                        compareByDescending<AnnotationEntity> { it.createdAt }
                            .thenByDescending { it.id }
                    )
            }
        }

        override suspend fun getForBook(bookId: String): List<AnnotationEntity> {
            return observeForBook(bookId).first()
        }

        override suspend fun insert(annotation: AnnotationEntity): Long {
            val id = annotation.id.takeIf { it != 0L } ?: nextId++
            stored.value = stored.value + annotation.copy(id = id)
            return id
        }

        override suspend fun deleteById(id: Long): Int {
            val current = stored.value
            val removed = current.count { it.id == id }
            stored.value = current.filterNot { it.id == id }
            return removed
        }
    }

    private fun newRepository(ticks: MutableList<Long>): Pair<AnnotationRepositoryImpl, FakeAnnotationDao> {
        val dao = FakeAnnotationDao()
        val repository = AnnotationRepositoryImpl(
            annotationDao = dao,
            clock = { ticks.removeAt(0) }
        )
        return repository to dao
    }

    private fun clockTicks(): MutableList<Long> {
        return mutableListOf(1000L, 2000L, 3000L, 4000L, 5000L, 6000L, 7000L, 8000L)
    }

    private val chapterOne = """
        {"href": "/OEBPS/chapter01.xhtml", "type": "application/xhtml+xml",
         "title": "Chapter One", "locations": {"progression": 0.5},
         "text": {"after": "some words"}}
    """.trimIndent()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `addAnnotation persists annotation with defaults`() = runTest {
        val (repository, dao) = newRepository(clockTicks())

        val id = repository.addAnnotation("book-1", chapterOne)

        assertTrue(id > 0)
        assertEquals(1, dao.stored.value.size)
        assertEquals("book-1", dao.stored.value[0].bookId)
        assertEquals(chapterOne, dao.stored.value[0].locatorJson)
        assertEquals(Annotation.DEFAULT_COLOR_HEX, dao.stored.value[0].colorHex)
        assertEquals(null, dao.stored.value[0].note)
        assertEquals(1000L, dao.stored.value[0].createdAt)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `addAnnotation stores custom color and note`() = runTest {
        val (repository, _) = newRepository(clockTicks())

        repository.addAnnotation(
            bookId = "book-1",
            locatorJson = chapterOne,
            colorHex = "#80B39DDB",
            note = "Key passage"
        )

        val annotations = repository.getAnnotations("book-1")
        assertEquals("#80B39DDB", annotations[0].colorHex)
        assertEquals("Key passage", annotations[0].note)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `addAnnotation keeps duplicates for same locator`() = runTest {
        val (repository, dao) = newRepository(clockTicks())

        repository.addAnnotation("book-1", chapterOne)
        val result = repository.addAnnotation("book-1", chapterOne)

        assertEquals(2, dao.stored.value.size)
        assertTrue(result > 0)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `observeAnnotations returns newest first`() = runTest {
        val (repository, _) = newRepository(clockTicks())

        repository.addAnnotation("book-1", chapterOne)
        repository.addAnnotation(
            "book-1",
            """{"href": "/OEBPS/chapter02.xhtml", "locations": {}}""".trimIndent()
        )

        val annotations = repository.observeAnnotations("book-1").first()
        assertEquals(2, annotations.size)
        assertEquals(2000L, annotations[0].createdAt)
        assertEquals(1000L, annotations[1].createdAt)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `observeAnnotations breaks createdAt ties by newest insertion`() = runTest {
        val ticks = mutableListOf(1000L, 1000L, 1000L, 1000L)
        val (repository, _) = newRepository(ticks)

        repository.addAnnotation("book-1", chapterOne)
        repository.addAnnotation(
            "book-1",
            """{"href": "/OEBPS/chapter02.xhtml", "locations": {}}""".trimIndent()
        )

        val annotations = repository.observeAnnotations("book-1").first()
        assertEquals(2, annotations.size)
        assertTrue(annotations[0].id > annotations[1].id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `annotations are isolated per book`() = runTest {
        val (repository, dao) = newRepository(clockTicks())

        repository.addAnnotation("book-1", chapterOne)
        repository.addAnnotation("book-2", chapterOne)

        assertEquals(2, dao.stored.value.size)
        assertEquals(1, repository.observeAnnotations("book-1").first().size)
        assertEquals(1, repository.observeAnnotations("book-2").first().size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `removeAnnotation deletes only the targeted annotation`() = runTest {
        val (repository, dao) = newRepository(clockTicks())
        repository.addAnnotation("book-1", chapterOne)
        repository.addAnnotation(
            "book-1",
            """{"href": "/OEBPS/chapter02.xhtml", "locations": {}}""".trimIndent()
        )
        val first = repository.getAnnotations("book-1")[0]

        repository.removeAnnotation(first.id)

        assertEquals(1, dao.stored.value.size)
        assertTrue(repository.getAnnotations("book-1").none { it.id == first.id })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getAnnotations returns empty list for unknown book`() = runTest {
        val (repository, _) = newRepository(clockTicks())

        assertTrue(repository.getAnnotations("missing").isEmpty())
    }
}
