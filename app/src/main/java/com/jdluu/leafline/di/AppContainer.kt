package com.jdluu.leafline.di

import android.content.Context
import com.jdluu.leafline.EpubImporter
import com.jdluu.leafline.LeaflineApplication
import com.jdluu.leafline.ReadiumEpubImporter
import com.jdluu.leafline.library.data.AnnotationRepository
import com.jdluu.leafline.library.data.AnnotationRepositoryImpl
import com.jdluu.leafline.library.data.BookmarkRepository
import com.jdluu.leafline.library.data.BookmarkRepositoryImpl
import com.jdluu.leafline.library.data.LibraryRepository
import com.jdluu.leafline.library.data.LibraryRepositoryImpl
import com.jdluu.leafline.library.data.ReadingSessionRepository
import com.jdluu.leafline.library.data.ReadingSessionRepositoryImpl
import com.jdluu.leafline.library.data.local.LeaflineDatabase
import com.jdluu.leafline.library.data.local.RoomBookDataSource
import com.jdluu.leafline.sync.KoreaderSyncApi
import com.jdluu.leafline.sync.KoreaderSyncClient

/**
 * Application-wide dependency graph, built eagerly at construction. Room
 * defers opening the database until the first query, so this stays cheap on
 * the main thread; dependencies are exposed as constructor-injected values
 * rather than fetched from a singleton service locator.
 */
class AppContainer(context: Context) {

    val database: LeaflineDatabase = LeaflineDatabase.build(context)

    val libraryRepository: LibraryRepository =
        LibraryRepositoryImpl(RoomBookDataSource(database), database.collectionDao())

    val bookmarkRepository: BookmarkRepository =
        BookmarkRepositoryImpl(database.bookmarkDao())

    val annotationRepository: AnnotationRepository =
        AnnotationRepositoryImpl(database.annotationDao())

    val readingSessionRepository: ReadingSessionRepository =
        ReadingSessionRepositoryImpl(database.readingSessionDao())

    /**
     * Lazy like-for-like with the previous call-site construction: the OkHttp
     * client and cover directory are only created once sync or import is
     * actually used.
     */
    val epubImporter: EpubImporter by lazy { ReadiumEpubImporter(context) }

    val koreaderSyncApi: KoreaderSyncApi by lazy { KoreaderSyncClient() }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as LeaflineApplication).appContainer
