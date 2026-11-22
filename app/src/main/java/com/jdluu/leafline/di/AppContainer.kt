package com.jdluu.leafline.di

import android.content.Context
import com.jdluu.leafline.EpubImporter
import com.jdluu.leafline.LeaflineApplication
import com.jdluu.leafline.ReadiumEpubImporter
import com.jdluu.leafline.export.NotesExporter
import com.jdluu.leafline.hardcover.HardcoverApi
import com.jdluu.leafline.hardcover.HardcoverClient
import com.jdluu.leafline.hardcover.HardcoverConfigStore
import com.jdluu.leafline.hardcover.HardcoverConfigPersistence
import com.jdluu.leafline.hardcover.HardcoverConfigStorage
import com.jdluu.leafline.hardcover.HardcoverSyncer
import com.jdluu.leafline.library.data.AnnotationRepository
import com.jdluu.leafline.library.data.AnnotationRepositoryImpl
import com.jdluu.leafline.library.data.BookmarkRepository
import com.jdluu.leafline.library.data.BookmarkRepositoryImpl
import com.jdluu.leafline.library.data.LibraryRepository
import com.jdluu.leafline.library.data.LibraryRepositoryImpl
import com.jdluu.leafline.library.data.ReadingSessionRepository
import com.jdluu.leafline.library.data.ReadingSessionRepositoryImpl
import com.jdluu.leafline.library.backup.LibraryBackupManager
import com.jdluu.leafline.library.data.local.LeaflineDatabase
import com.jdluu.leafline.library.data.local.RoomBookDataSource
import com.jdluu.leafline.opds.OpdsClient
import com.jdluu.leafline.opds.OpdsFeedStore
import com.jdluu.leafline.storage.KoreaderSyncConfigStorage
import com.jdluu.leafline.sync.KoreaderSyncApi
import com.jdluu.leafline.sync.KoreaderSyncClient
import com.jdluu.leafline.sync.KoreaderSyncConfigPersistence
import com.jdluu.leafline.sync.KoreaderSyncConfigStore

/**
 * Application-wide dependency graph, built eagerly at construction. Room
 * defers opening the database until the first query, so this stays cheap on
 * the main thread; dependencies are exposed as constructor-injected values
 * rather than fetched from a singleton service locator.
 */
class AppContainer(private val context: Context) {

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

    /**
     * Restores saved KOReader sync settings into [KoreaderSyncConfigStore] when the
     * container is built, so sync and background sync work after a restart instead of
     * needing the server and password re-entered every launch.
     */
    val koreaderSyncConfigStore: KoreaderSyncConfigPersistence by lazy {
        KoreaderSyncConfigStorage(context).also { storage ->
            runCatching { KoreaderSyncConfigStore.config = storage.load() }
        }
    }

    val backupManager: LibraryBackupManager by lazy {
        LibraryBackupManager(
            bookDao = database.bookDao(),
            collectionDao = database.collectionDao(),
            bookmarkDao = database.bookmarkDao(),
            annotationDao = database.annotationDao(),
            appVersion = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull() ?: "unknown"
        )
    }

    val opdsFeedStore: OpdsFeedStore by lazy { OpdsFeedStore.fromContext(context) }

    val opdsClient: OpdsClient by lazy {
        // Report the real installed version so servers and support can tell builds apart.
        val versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
        OpdsClient(userAgent = "Leafline/$versionName (Android; OPDS Client)")
    }

    /**
     * Hardcover sync. [hardcoverConfigStore] restores the saved token when the
     * container is built, so the integration keeps working across restarts without
     * the reader re-entering it. It stays inactive until they enable it.
     */
    val hardcoverApi: HardcoverApi by lazy {
        val versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
        HardcoverClient(userAgent = "Leafline/$versionName")
    }

    val hardcoverConfigStore: HardcoverConfigPersistence by lazy {
        HardcoverConfigStorage(context).also { storage ->
            runCatching { HardcoverConfigStore.config = storage.load() }
        }
    }

    val hardcoverSyncer: HardcoverSyncer by lazy {
        // Touch the storage first so a fresh process has the token before a push.
        hardcoverConfigStore
        HardcoverSyncer(api = hardcoverApi, config = { HardcoverConfigStore.config })
    }

    val notesExporter: NotesExporter by lazy {
        NotesExporter(
            libraryRepository = libraryRepository,
            annotationRepository = annotationRepository
        )
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as LeaflineApplication).appContainer
