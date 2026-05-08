package com.markleaf.notes.core.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

object MarkdownEditActions {
    private val headingPattern = Regex("""^(#{1,6})\s+""")
    private val bulletPattern = Regex("""^([-*+])\s+""")
    private val orderedPattern = Regex("""^(\d+)\.\s+""")
    private val blockquotePattern = Regex("""^(>+)\s+""")

    fun bold(value: TextFieldValue): TextFieldValue =
        wrapSelection(value, "**", "**", "bold")

    fun italic(value: TextFieldValue): TextFieldValue =
        wrapSelection(value, "*", "*", "italic")

    fun strikethrough(value: TextFieldValue): TextFieldValue =
        wrapSelection(value, "~~", "~~", "text")

    fun inlineCode(value: TextFieldValue): TextFieldValue =
        wrapSelection(value, "`", "`", "code")

    fun checkbox(value: TextFieldValue): TextFieldValue =
        insertAtLineStart(value, "- [ ] ")

    fun markdownLink(value: TextFieldValue): TextFieldValue {
        val selected = selectedText(value)
        return if (selected.isBlank()) {
            replaceSelection(value, "[label](target)", cursorOffset = 1)
        } else {
            replaceSelection(value, "[$selected]($selected)")
        }
    }

    /** Cycle current line through `# ` -> `## ` -> `### ` -> none -> `# `. */
    fun heading(value: TextFieldValue): TextFieldValue {
        val (lineStart, line) = currentLine(value)
        val match = headingPattern.find(line)
        val (newPrefix, removed) = if (match != null) {
            val level = match.groupValues[1].length
            when (level) {
                1 -> "## " to match.value.length
                2 -> "### " to match.value.length
                else -> "" to match.value.length
            }
        } else {
            "# " to 0
        }
        val updated = value.text.substring(0, lineStart) +
            newPrefix +
            value.text.substring(lineStart + removed)
        val delta = newPrefix.length - removed
        val newCursor = (value.selection.max + delta).coerceAtLeast(lineStart)
        return value.copy(text = updated, selection = TextRange(newCursor))
    }

    /** Toggle `- ` at the start of the current line. */
    fun bulletList(value: TextFieldValue): TextFieldValue =
        toggleLinePrefix(value, bulletPattern, "- ")

    /** Toggle `1. ` at the start of the current line. */
    fun orderedList(value: TextFieldValue): TextFieldValue =
        toggleLinePrefix(value, orderedPattern, "1. ")

    /** Toggle `> ` at the start of the current line. */
    fun blockquote(value: TextFieldValue): TextFieldValue =
        toggleLinePrefix(value, blockquotePattern, "> ")

    /** Insert a horizontal rule on its own line. */
    fun horizontalRule(value: TextFieldValue): TextFieldValue {
        val (lineStart, line) = currentLine(value)
        val before = if (lineStart == 0 || value.text.getOrNull(lineStart - 1) == '\n') "" else "\n"
        val after = "\n"
        val insertion = if (line.isBlank()) "${before}---$after" else "\n---$after"
        val cursor = value.selection.max
        val updated = value.text.substring(0, cursor) + insertion + value.text.substring(cursor)
        return value.copy(text = updated, selection = TextRange(cursor + insertion.length))
    }

    /** Wrap selection in a fenced code block, or insert an empty one. */
    fun codeBlock(value: TextFieldValue): TextFieldValue {
        val selected = selectedText(value)
        val cursor = value.selection.max
        val prefix = if (cursor == 0 || value.text.getOrNull(cursor - 1) == '\n') "" else "\n"
        return if (selected.isBlank()) {
            val insertion = "${prefix}```\n\n```\n"
            val newCursor = value.selection.min + prefix.length + 4 // after "```\n"
            val updated = value.text.replaceRange(value.selection.min, value.selection.max, insertion)
            value.copy(text = updated, selection = TextRange(newCursor))
        } else {
            val replacement = "${prefix}```\n$selected\n```\n"
            replaceSelection(value, replacement)
        }
    }

    /**
     * If [old] -> [new] looks like a single Enter key press at the end of a list /
     * checklist / blockquote / ordered-list line, return a text-field value that
     * either continues the prefix on the new line, or removes a stale empty prefix
     * from the previous line. Returns [new] unchanged when no continuation rule applies.
     */
    fun applyAutoContinuation(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
        if (new.text.length != old.text.length + 1) return new
        val cursor = new.selection.start
        if (cursor < 1 || cursor > new.text.length) return new
        if (new.text[cursor - 1] != '\n') return new

        val prevLineStart = new.text.lastIndexOf('\n', cursor - 2).let { if (it == -1) 0 else it + 1 }
        val prevLine = new.text.substring(prevLineStart, cursor - 1)

        val continuation = continuationFor(prevLine) ?: return new

        return if (continuation.bodyEmpty) {
            // Remove the bare prefix from the previous line, keep the newline.
            val updated = new.text.substring(0, prevLineStart) + new.text.substring(cursor - 1)
            val delta = (cursor - 1) - prevLineStart // chars removed
            val newCursor = (cursor - delta).coerceAtLeast(0)
            new.copy(text = updated, selection = TextRange(newCursor))
        } else {
            val insertion = continuation.nextPrefix
            val updated = new.text.substring(0, cursor) + insertion + new.text.substring(cursor)
            new.copy(text = updated, selection = TextRange(cursor + insertion.length))
        }
    }

    private data class Continuation(val nextPrefix: String, val bodyEmpty: Boolean)

    private fun continuationFor(line: String): Continuation? {
        // Checklist: `- [ ] body` / `- [x] body`
        val checklist = Regex("""^(\s*)([-*+])\s\[[ xX]]\s(.*)$""").matchEntire(line)
        if (checklist != null) {
            val indent = checklist.groupValues[1]
            val marker = checklist.groupValues[2]
            val body = checklist.groupValues[3]
            return Continuation(
                nextPrefix = "$indent$marker [ ] ",
                bodyEmpty = body.isEmpty()
            )
        }
        // Bullet: `- body` / `* body` / `+ body`
        val bullet = Regex("""^(\s*)([-*+])\s(.*)$""").matchEntire(line)
        if (bullet != null) {
            val indent = bullet.groupValues[1]
            val marker = bullet.groupValues[2]
            val body = bullet.groupValues[3]
            return Continuation(
                nextPrefix = "$indent$marker ",
                bodyEmpty = body.isEmpty()
            )
        }
        // Ordered: `1. body` -> `2. body`
        val ordered = Regex("""^(\s*)(\d+)\.\s(.*)$""").matchEntire(line)
        if (ordered != null) {
            val indent = ordered.groupValues[1]
            val n = ordered.groupValues[2].toIntOrNull() ?: 1
            val body = ordered.groupValues[3]
            return Continuation(
                nextPrefix = "$indent${n + 1}. ",
                bodyEmpty = body.isEmpty()
            )
        }
        // Blockquote: `> body`
        val quote = Regex("""^(\s*)(>+)\s(.*)$""").matchEntire(line)
        if (quote != null) {
            val indent = quote.groupValues[1]
            val markers = quote.groupValues[2]
            val body = quote.groupValues[3]
            return Continuation(
                nextPrefix = "$indent$markers ",
                bodyEmpty = body.isEmpty()
            )
        }
        return null
    }

    private fun toggleLinePrefix(
        value: TextFieldValue,
        existing: Regex,
        prefix: String
    ): TextFieldValue {
        val (lineStart, line) = currentLine(value)
        val match = existing.find(line)
        return if (match != null) {
            val removeLen = match.value.length
            val updated = value.text.substring(0, lineStart) + value.text.substring(lineStart + removeLen)
            val newCursor = (value.selection.max - removeLen).coerceAtLeast(lineStart)
            value.copy(text = updated, selection = TextRange(newCursor))
        } else {
            insertAtLineStart(value, prefix)
        }
    }

    private fun currentLine(value: TextFieldValue): Pair<Int, String> {
        val cursor = value.selection.min.coerceIn(0, value.text.length)
        val lineStart = value.text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
            .let { if (it == -1) 0 else it + 1 }
        val nextNewline = value.text.indexOf('\n', lineStart)
        val lineEnd = if (nextNewline == -1) value.text.length else nextNewline
        return lineStart to value.text.substring(lineStart, lineEnd)
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
