package com.jdluu.leafline.library.data

data class Annotation(
    val id: Long,
    val bookId: String,
    val locatorJson: String,
    val colorHex: String,
    val note: String? = null,
    val createdAt: Long
) {
    companion object {
        const val DEFAULT_COLOR_HEX = "#55E65100"
    }
}
