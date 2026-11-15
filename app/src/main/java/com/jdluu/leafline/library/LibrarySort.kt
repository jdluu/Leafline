package com.jdluu.leafline.library

import com.jdluu.leafline.sync.ReadingProgressMath

/**
 * Sort orders available in the library screen.
 */
enum class LibrarySort(
    val label: String,
    val defaultAscending: Boolean = false
) {
    RECENT("Date Added", defaultAscending = false),
    RECENTLY_READ("Recently Read", defaultAscending = false),
    TITLE("Title", defaultAscending = true),
    AUTHOR("Author", defaultAscending = true),
    PROGRESS("Progress", defaultAscending = false),
    RATING("Rating", defaultAscending = false);

    fun sorted(books: List<LibraryBook>, ascending: Boolean = defaultAscending): List<LibraryBook> {
        return when (this) {
            RECENT -> if (ascending) {
                books.sortedWith(
                    compareBy<LibraryBook> { it.addedAtEpochMillis ?: Long.MAX_VALUE }
                        .thenBy { it.title.lowercase() }
                )
            } else {
                books.sortedWith(
                    compareByDescending<LibraryBook> { it.addedAtEpochMillis ?: Long.MIN_VALUE }
                        .thenBy { it.title.lowercase() }
                )
            }
            RECENTLY_READ -> if (ascending) {
                books.sortedWith(
                    compareBy<LibraryBook> { it.lastReadAtEpochMillis ?: Long.MAX_VALUE }
                        .thenBy { it.addedAtEpochMillis ?: Long.MAX_VALUE }
                        .thenBy { it.title.lowercase() }
                )
            } else {
                books.sortedWith(
                    compareByDescending<LibraryBook> { it.lastReadAtEpochMillis ?: Long.MIN_VALUE }
                        .thenByDescending { it.addedAtEpochMillis ?: Long.MIN_VALUE }
                        .thenBy { it.title.lowercase() }
                )
            }
            TITLE -> if (ascending) {
                books.sortedWith(
                    compareBy<LibraryBook> { it.title.lowercase() }
                        .thenBy { it.authors.firstOrNull()?.lowercase() ?: "" }
                )
            } else {
                books.sortedWith(
                    compareByDescending<LibraryBook> { it.title.lowercase() }
                        .thenByDescending { it.authors.firstOrNull()?.lowercase() ?: "" }
                )
            }
            AUTHOR -> if (ascending) {
                books.sortedWith(
                    compareBy<LibraryBook> { it.authors.firstOrNull()?.lowercase() }
                        .thenBy { it.title.lowercase() }
                )
            } else {
                books.sortedWith(
                    compareByDescending<LibraryBook> { it.authors.firstOrNull()?.lowercase() }
                        .thenByDescending { it.title.lowercase() }
                )
            }
            PROGRESS -> if (ascending) {
                books.sortedWith(
                    compareBy<LibraryBook> { ReadingProgressMath.percentageFromLocator(it.lastLocatorJson) ?: 0.0 }
                        .thenBy { it.title.lowercase() }
                )
            } else {
                books.sortedWith(
                    compareByDescending<LibraryBook> { ReadingProgressMath.percentageFromLocator(it.lastLocatorJson) ?: 0.0 }
                        .thenByDescending { it.lastReadAtEpochMillis ?: Long.MIN_VALUE }
                        .thenBy { it.title.lowercase() }
                )
            }
            RATING -> if (ascending) {
                books.sortedWith(
                    compareBy<LibraryBook> { it.rating ?: 0 }
                        .thenBy { it.title.lowercase() }
                )
            } else {
                books.sortedWith(
                    compareByDescending<LibraryBook> { it.rating ?: 0 }
                        .thenByDescending { it.addedAtEpochMillis ?: Long.MIN_VALUE }
                        .thenBy { it.title.lowercase() }
                )
            }
        }
    }

    companion object {
        fun fromNameOrDefault(name: String?): LibrarySort {
            return entries.firstOrNull { it.name == name } ?: RECENT
        }
    }
}
