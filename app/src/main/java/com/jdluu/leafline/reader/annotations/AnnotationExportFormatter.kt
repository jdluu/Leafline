package com.jdluu.leafline.reader.annotations

import com.jdluu.leafline.library.data.Annotation

/** Formats local annotations without Android or storage dependencies. */
object AnnotationExportFormatter {
    fun plainText(annotations: List<Annotation>, excerptFor: (Annotation) -> String): String =
        annotations.joinToString("\n\n") { annotation ->
            buildString {
                append(excerptFor(annotation))
                annotation.note?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    append("\nNote: ").append(it)
                }
            }
        }

    fun markdown(annotations: List<Annotation>, excerptFor: (Annotation) -> String): String =
        annotations.joinToString("\n\n") { annotation ->
            buildString {
                append("> ").append(excerptFor(annotation).replace("\n", "\n> "))
                annotation.note?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    append("\n\n").append(it)
                }
                append("\n\n<!-- color: ").append(annotation.colorHex)
                    .append("; locator: ").append(annotation.locatorJson).append(" -->")
            }
        }

    fun json(annotations: List<Annotation>, excerptFor: (Annotation) -> String): String =
        annotations.joinToString(prefix = "[", postfix = "]") { annotation ->
            buildString {
                append("{\"id\":").append(annotation.id)
                append(",\"bookId\":").append(quoted(annotation.bookId))
                append(",\"excerpt\":").append(quoted(excerptFor(annotation)))
                append(",\"locator\":").append(annotation.locatorJson)
                append(",\"color\":").append(quoted(annotation.colorHex))
                append(",\"note\":")
                append(annotation.note?.let(::quoted) ?: "null")
                append(",\"createdAt\":").append(annotation.createdAt)
                append('}')
            }
        }

    private fun quoted(value: String): String = buildString {
        append('"')
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
        append('"')
    }
}
