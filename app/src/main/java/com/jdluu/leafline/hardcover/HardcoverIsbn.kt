package com.jdluu.leafline.hardcover

/**
 * Extracts an ISBN-13 from an EPUB publication identifier so a local book can be
 * matched to a Hardcover edition.
 *
 * Only identifiers that are *entirely* an ISBN are accepted. A URL such as
 * "http://www.gutenberg.org/1952" contains digits, and matching on digits alone
 * would attribute the wrong book to a real ISBN, so the shape and the checksum are
 * both validated before anything leaves the device.
 */
object HardcoverIsbn {

    private val ISBN13 = Regex("^[0-9]{13}$")
    private val ISBN10 = Regex("^[0-9]{9}[0-9X]$")

    fun isbn13From(identifier: String?): String? {
        val raw = identifier?.trim().orEmpty()
        if (raw.isEmpty()) return null

        val body = stripScheme(raw)
        val compact = body.replace("-", "").replace(" ", "").uppercase()
        return when {
            ISBN13.matches(compact) -> compact.takeIf(::isValidIsbn13)
            ISBN10.matches(compact) -> isbn10To13(compact)
            else -> null
        }
    }

    /**
     * Removes a leading scheme or label ("urn:isbn:", "ISBN:", "ISBN ") without
     * caring about case or whether the colon is present, because real EPUB
     * metadata uses all of those forms.
     */
    private fun stripScheme(raw: String): String {
        val lower = raw.lowercase()
        val cut = when {
            lower.startsWith("urn:isbn") -> "urn:isbn".length
            lower.startsWith("isbn") -> "isbn".length
            else -> return raw.trim()
        }
        return raw.substring(cut).trim().trimStart(':', ' ').trim()
    }

    private fun isbn10To13(isbn10: String): String? {
        if (!isValidIsbn10(isbn10)) return null
        val core = "978" + isbn10.take(9)
        return core + checkDigit13(core)
    }

    private fun isValidIsbn10(isbn10: String): Boolean {
        var sum = 0
        for (index in 0 until 10) {
            val char = isbn10[index]
            val value = if (char == 'X') 10 else char.digitToIntOrNull() ?: return false
            sum += (10 - index) * value
        }
        return sum % 11 == 0
    }

    private fun isValidIsbn13(isbn13: String): Boolean {
        var sum = 0
        for (index in 0 until 13) {
            val value = isbn13[index].digitToIntOrNull() ?: return false
            sum += value * if (index % 2 == 0) 1 else 3
        }
        return sum % 10 == 0
    }

    /** Check digit for the first twelve digits of an ISBN-13. */
    private fun checkDigit13(twelveDigits: String): String {
        var sum = 0
        for (index in 0 until 12) {
            val value = twelveDigits[index].digitToIntOrNull() ?: return ""
            sum += value * if (index % 2 == 0) 1 else 3
        }
        return ((10 - sum % 10) % 10).toString()
    }
}
