package com.jdluu.leafline

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.jdluu.leafline.library.data.local.BookEntity
import com.jdluu.leafline.library.data.local.LeaflineDatabase
import com.jdluu.leafline.library.cover.CoverCache
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

/**
 * Library grid performance benchmarks (#25).
 *
 * Pre-populates an in-memory Room DB with 100 book records and generated
 * cover files, then asserts database query latency and verifies cover
 * decoding does not leak memory. UI scroll smoothness belongs in a future
 * :benchmark macrobenchmark module; this covers the data-layer contract.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class LibraryGridBenchmarkTest {

    private lateinit var db: LeaflineDatabase
    private lateinit var cache: CoverCache
    private lateinit var importDir: File
    private var bookCount = 0

    @Before
    fun setUp() {
        runBlocking { setupFixture() }
    }

    private suspend fun setupFixture() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, LeaflineDatabase::class.java).build()
        cache = CoverCache(CoverCache.coversDirectory(context.filesDir))
        importDir = File(context.filesDir, "benchmark-imports").apply { mkdirs() }

        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val epubNames = assets.list("benchmark-books")?.filter { it.endsWith(".epub") } ?: emptyList()
        bookCount = epubNames.size
        Log.i(TAG, "Found $bookCount benchmark EPUBs")

        val startImport = System.nanoTime()
        for (name in epubNames) {
            val dest = File(importDir, name)
            assets.open("benchmark-books/$name").use { input ->
                dest.outputStream().use { out -> input.copyTo(out) }
            }
            // Real SHA-256 over file bytes so the bench reflects import cost.
            val sha = MessageDigest.getInstance("SHA-256").digest(dest.readBytes())
                .joinToString("") { "%02x".format(it) }
            db.bookDao().insert(BookEntity(
                stableId = name.removeSuffix(".epub"),
                title = name.removeSuffix(".epub").replace("-", " ")
                    .replaceFirstChar { it.uppercase() },
                authors = listOf("Benchmark Author"),
                language = "en",
                description = null,
                publisher = null,
                publishedAtEpochMillis = null,
                filePath = dest.absolutePath,
                fileHash = sha,
                addedAtEpochMillis = System.currentTimeMillis(),
                pageCount = null
            ))
        }
        val importMs = (System.nanoTime() - startImport) / 1_000_000
        Log.i(TAG, "Imported+hashed+inserted $bookCount books in ${importMs}ms " +
            "(%.2f ms/book)".format(importMs.toDouble() / bookCount))

        // Generate a few cover files (300x200 JPEG) for decode benchmarks.
        val startCover = System.nanoTime()
        for (i in 1..10) {
            val stableId = "bench-%04d".format(i)
            val bitmap = Bitmap.createBitmap(300, 200, Bitmap.Config.RGB_565)
            bitmap.eraseColor(android.graphics.Color.rgb((i * 23) % 256, 128, 255 - (i * 17) % 256))
            val bytes = ByteArrayOutputStream().use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)
                it.toByteArray()
            }
            cache.storeCoverBytes(stableId, bytes)
            bitmap.recycle()
        }
        Log.i(TAG, "Generated 10 cover files in ${(System.nanoTime() - startCover) / 1_000_000}ms")
    }

    @After
    fun tearDown() {
        db.close()
        importDir.deleteRecursively()
    }

    @Test
    fun queryAllBooks_under100ms() = runBlocking {
        // Warm-up pass: compiles the SQL statement and initializes the Flow
        // pipeline. Cold-start cost is dominated by one-time setup.
        db.bookDao().getAllBooks().first()

        // Sample the live queries several times and gate on the median rather
        // than a single measurement. A lone sample can exceed the budget on a
        // transient (GC, scheduler, thermal), which flakes the gate even when
        // the query itself is healthy (observed once at 154ms during #96). The
        // median is robust to such outliers and reports the representative
        // warm latency. Nine samples neutralizes one transient at worst.
        val samples = DoubleArray(QUERY_SAMPLES)
        var lastBooks: List<BookEntity> = emptyList()
        for (i in samples.indices) {
            val start = System.nanoTime()
            lastBooks = db.bookDao().getAllBooks().first()
            samples[i] = (System.nanoTime() - start) / 1_000_000.0
        }
        samples.sort()
        val medianMs = samples[samples.size / 2]
        Log.i(TAG, "getAllBooks() $bookCount books (warm): median ${"%.2f".format(medianMs)}ms " +
            "samples=${samples.joinToString(",") { "%.2f".format(it) }}")

        assertTrue("getAllBooks should return all books", lastBooks.size >= bookCount)
        assertTrue(
            "Warm getAllBooks median should complete under 100ms for $bookCount books " +
                "(median ${"%.2f".format(medianMs)}ms)",
            medianMs < 100
        )
    }

    @Test
    fun coverDecoding_memoryBounded() {
        val coverFile = cache.coverFileFor("bench-0001")
        if (!coverFile.isFile) return // no cover fixture; nothing to assert

        Runtime.getRuntime().gc()
        val memBefore = usedMemory()

        repeat(50) {
            BitmapFactory.decodeFile(coverFile.absolutePath)?.recycle()
        }

        Runtime.getRuntime().gc()
        val deltaKb = (usedMemory() - memBefore) / 1024
        Log.i(TAG, "Memory delta after 50 cover decodes: ${deltaKb}KB")
        assertTrue(
            "Cover decoding should not retain memory (delta=${deltaKb}KB)",
            deltaKb < 5_000
        )
    }

    private fun usedMemory(): Long =
        Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

    private companion object {
        const val TAG = "LibraryBench"

        // Number of warm query samples whose median backs the latency budget.
        // Odd count so the median is a real observed sample.
        const val QUERY_SAMPLES = 9
    }
}
