package com.jdluu.leafline.library

/**
 * A user-selected library folder as shown in the management surface.
 *
 * @param uri the persisted SAF tree URI string.
 * @param label a human-readable name resolved from the provider, when available.
 * @param accessible whether the app still holds the persisted read grant for
 *   this folder. A folder without its grant is shown as permission-lost and
 *   offers reselection.
 */
data class LibraryFolder(
    val uri: String,
    val label: String?,
    val accessible: Boolean
)

/**
 * Resolves display metadata and persisted-grant state for a saved folder URI.
 *
 * The concrete SAF implementation needs a [android.content.ContentResolver] and
 * is therefore not JVM-testable; the interface exists so the [LibraryFolderViewModel]
 * logic can be driven by a fake in unit tests.
 */
interface FolderMetadata {
    /** Human-readable label for a folder, or null when the provider cannot resolve one. */
    fun labelFor(uri: String): String?

    /** True when the app still holds the persisted read grant for this folder. */
    fun hasPersistedReadGrant(uri: String): Boolean

    /** Releases the persisted read grant for this folder, if the app holds it. */
    fun releasePersistedReadGrant(uri: String)
}
