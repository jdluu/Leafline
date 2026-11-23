package com.jdluu.leafline.hardcover

/**
 * Whether a given book's progress may be sent to Hardcover.
 *
 * This is per book, separate from the on/off switch for the integration itself: the
 * switch says "Hardcover may receive progress", and this says "which books".
 */
enum class HardcoverTrackingMode {
    /** Every book that can be matched is tracked. The default once sync is on. */
    ALL_BOOKS,

    /** Ask once per book and remember the answer. */
    ASK_EACH_BOOK,

    /** Track nothing, without disconnecting the account. */
    OFF
}

/** A remembered answer for one book. */
enum class HardcoverBookPreference {
    TRACK,
    IGNORE
}

/** What the app should do about a particular book right now. */
sealed interface HardcoverTrackingDecision {
    data object Track : HardcoverTrackingDecision
    data object Skip : HardcoverTrackingDecision

    /** Only produced in [HardcoverTrackingMode.ASK_EACH_BOOK] with no answer yet. */
    data object Ask : HardcoverTrackingDecision
}

/**
 * Resolves the tracking decision for one book.
 *
 * Rules, in order:
 *  1. A book with no usable ISBN is never tracked. Hardcover matches by ISBN, so
 *     asking about it, or sending it, would be a question with no possible answer.
 *  2. [HardcoverTrackingMode.OFF] wins over everything, including a book the reader
 *     once said yes to. Turning tracking off must mean off.
 *  3. A remembered answer wins over the mode, so choosing "don't track" for one book
 *     survives toggling the mode, and choosing "track" survives it too.
 *  4. Otherwise [HardcoverTrackingMode.ALL_BOOKS] tracks and
 *     [HardcoverTrackingMode.ASK_EACH_BOOK] asks.
 *
 * Pure and free of Android so the rules can be pinned by tests.
 */
object HardcoverTrackingPolicy {

    fun decide(
        mode: HardcoverTrackingMode,
        preference: HardcoverBookPreference?,
        hasIsbn: Boolean
    ): HardcoverTrackingDecision {
        if (!hasIsbn) return HardcoverTrackingDecision.Skip
        if (mode == HardcoverTrackingMode.OFF) return HardcoverTrackingDecision.Skip

        return when (preference) {
            HardcoverBookPreference.TRACK -> HardcoverTrackingDecision.Track
            HardcoverBookPreference.IGNORE -> HardcoverTrackingDecision.Skip
            null -> when (mode) {
                HardcoverTrackingMode.ALL_BOOKS -> HardcoverTrackingDecision.Track
                HardcoverTrackingMode.ASK_EACH_BOOK -> HardcoverTrackingDecision.Ask
                HardcoverTrackingMode.OFF -> HardcoverTrackingDecision.Skip
            }
        }
    }
}

/**
 * Applies [HardcoverTrackingPolicy] to a stored mode and per-book answers.
 *
 * Callers ask this before pushing instead of the syncer asking, so the syncer keeps
 * its single responsibility: given a book that should be tracked, send it.
 */
class HardcoverTrackingResolver(
    private val store: HardcoverTrackingStore
) : HardcoverTrackingStore by store {

    fun decide(stableId: String, identifier: String?): HardcoverTrackingDecision =
        HardcoverTrackingPolicy.decide(
            mode = store.mode(),
            preference = store.preferenceFor(stableId),
            hasIsbn = HardcoverIsbn.isbn13From(identifier) != null
        )



    /** Records the reader's answer for one book. */
    fun remember(stableId: String, preference: HardcoverBookPreference) {
        store.setPreference(stableId, preference)
    }

    fun forget(stableId: String) {
        store.clearPreference(stableId)
    }
}
