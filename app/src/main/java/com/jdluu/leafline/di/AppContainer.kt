package com.jdluu.leafline.di

import android.content.Context
import com.jdluu.leafline.LeaflineApplication
import com.jdluu.leafline.library.data.AnnotationRepository
import com.jdluu.leafline.library.data.AnnotationRepositoryImpl
import com.jdluu.leafline.library.data.BookmarkRepository
import com.jdluu.leafline.library.data.BookmarkRepositoryImpl
import com.jdluu.leafline.library.data.LibraryRepository
import com.jdluu.leafline.library.data.LibraryRepositoryImpl
import com.jdluu.leafline.library.data.local.LeaflineDatabase
import com.jdluu.leafline.library.data.local.RoomBookDataSource

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
}

val Context.appContainer: AppContainer
    get() = (applicationContext as LeaflineApplication).appContainer
