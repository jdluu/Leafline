package com.jdluu.leafline

import android.content.Context
import com.jdluu.leafline.library.LibraryBook
import com.jdluu.leafline.library.data.LibraryRepository
import com.jdluu.leafline.opds.OpdsEpubDownloader
import com.jdluu.leafline.opds.OpdsFeedEntry
import com.jdluu.leafline.opds.OpdsServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class OpdsDownloadCoordinator(
    private val context: Context,
    private val repository: LibraryRepository
) {
    suspend fun downloadAndImport(
        config: OpdsServerConfig,
        entry: OpdsFeedEntry
    ): LibraryBook? = withContext(Dispatchers.IO) {
        val file = OpdsEpubDownloader(File(context.filesDir, "opds-books"))
            .download(config, entry.href)
        val hash = FileHashUtil.computeSha256(file)
        val book = EpubImporter(context).importEpub(file, hash) ?: return@withContext null
        repository.addBook(book)
        book
    }
}
