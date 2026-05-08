package com.markleaf.notes.core.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

object MarkdownEditActions {
    fun bold(value: TextFieldValue): TextFieldValue {
        return wrapSelection(value, "**", "**", "bold")
    }

    fun italic(value: TextFieldValue): TextFieldValue {
        return wrapSelection(value, "*", "*", "italic")
    }

    fun strikethrough(value: TextFieldValue): TextFieldValue {
        return wrapSelection(value, "~~", "~~", "text")
    }

    fun inlineCode(value: TextFieldValue): TextFieldValue {
        return wrapSelection(value, "`", "`", "code")
    }

    fun checkbox(value: TextFieldValue): TextFieldValue {
        return insertAtLineStart(value, "- [ ] ")
    }

    fun markdownLink(value: TextFieldValue): TextFieldValue {
        val selected = selectedText(value)
        return if (selected.isBlank()) {
            replaceSelection(value, "[label](target)", cursorOffset = 1)
        } else {
            replaceSelection(value, "[$selected]($selected)")
        }
    }

    private fun wrapSelection(
        value: TextFieldValue,
        before: String,
        after: String,
        placeholder: String
    ): TextFieldValue {
        val selected = selectedText(value)
        val body = selected.ifBlank { placeholder }
        val replacement = "$before$body$after"
        val cursorOffset = if (selected.isBlank()) before.length else replacement.length
        return replaceSelection(value, replacement, cursorOffset = cursorOffset)
    }

    private fun insertAtLineStart(value: TextFieldValue, prefix: String): TextFieldValue {
        val start = value.selection.min
        val lineStart = value.text.lastIndexOf('\n', start.coerceAtMost(value.text.length) - 1)
            .let { if (it == -1) 0 else it + 1 }
        val updated = value.text.substring(0, lineStart) + prefix + value.text.substring(lineStart)
        val cursor = value.selection.max + prefix.length
        return value.copy(text = updated, selection = TextRange(cursor))
    }

    private fun replaceSelection(
        value: TextFieldValue,
        replacement: String,
        cursorOffset: Int = replacement.length
    ): TextFieldValue {
        val start = value.selection.min
        val end = value.selection.max
        val updated = value.text.replaceRange(start, end, replacement)
        return value.copy(
            text = updated,
            selection = TextRange(start + cursorOffset)
        )
    }

    private fun selectedText(value: TextFieldValue): String {
        val start = value.selection.min
        val end = value.selection.max
        return value.text.substring(start, end)
    }
}
