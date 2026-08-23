package com.jdluu.leafline.library

/**
 * Sort orders available in the library screen.
 */
enum class LibrarySort(val label: String) {
    RECENT("Recent"),
    TITLE("Title"),
    AUTHOR("Author");

    fun sorted(books: List<LibraryBook>): List<LibraryBook> = when (this) {
        RECENT -> books.sortedByDescending { it.addedAtEpochMillis ?: Long.MIN_VALUE }
        TITLE -> books.sortedWith(compareBy { it.title.lowercase() })
        AUTHOR -> books.sortedWith(
            compareBy<LibraryBook> { it.authors.firstOrNull()?.lowercase() }
                .thenBy { it.title.lowercase() }
        )
    }

    companion object {
        fun fromNameOrDefault(name: String?): LibrarySort {
            return entries.firstOrNull { it.name == name } ?: RECENT
        }
    }
}
