package com.markleaf.notes.core.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

object MarkdownEditActions {
    /**
     * The two-column GFM table skeleton, and the caret offset that lands inside
     * its first header cell (right after `| `).
     *
     * Shared with the `/table` quick-insert command rather than written twice:
     * the panel row and the slash command are two doors onto the same
     * construct, and a user who learns the shape through one and then uses the
     * other must not get a different table.
     */
    const val TABLE_TEMPLATE = "| Column 1 | Column 2 |\n| --- | --- |\n|  |  |"
    const val TABLE_CARET_OFFSET = 2

    /** The callout head, and the head plus an empty body line for insertion. */
    const val CALLOUT_HEAD = "> [!NOTE]"
    const val CALLOUT_TEMPLATE = CALLOUT_HEAD + "\n> "

    private val headingPattern = Regex("""^(#{1,6})\s+""")
    private val bulletPattern = Regex("""^([-*+])\s+""")
    private val orderedPattern = Regex("""^(\d+)\.\s+""")
    private val blockquotePattern = Regex("""^(>+)\s+""")
    private val checkboxPattern = Regex("""^(\s*[-*+]) \[([ xX])]""")

    /** A `> [!NOTE]` head on its own line — see [callout]. */
    private val calloutHeadPattern = Regex("""^\s*>\s*\[![A-Za-z]+]""", RegexOption.MULTILINE)

    fun bold(value: TextFieldValue): TextFieldValue =
        wrapSelection(value, "**", "**", "bold")

    fun italic(value: TextFieldValue): TextFieldValue =
        wrapSelection(value, "*", "*", "italic")

    fun strikethrough(value: TextFieldValue): TextFieldValue =
        wrapSelection(value, "~~", "~~", "text")

    fun inlineCode(value: TextFieldValue): TextFieldValue =
        wrapSelection(value, "`", "`", "code")

    /**
     * On a line that is already a checklist item, toggle it between TODO `[ ]` and
     * DONE `[x]` (#145). Otherwise turn the current line into a new `- [ ] ` item.
     * Toggling swaps a single character, so the caret stays put.
     */
    fun checkbox(value: TextFieldValue): TextFieldValue {
        val (lineStart, line) = currentLine(value)
        val match = checkboxPattern.find(line) ?: return insertAtLineStart(value, "- [ ] ")
        val markerOffset = lineStart + match.value.indexOf('[') + 1
        val newMarker = if (value.text[markerOffset] == ' ') 'x' else ' '
        val updated = value.text.substring(0, markerOffset) + newMarker + value.text.substring(markerOffset + 1)
        return value.copy(text = updated, selection = value.selection)
    }

    /**
     * Flip the task marker on [lineIndex] (0-based) of [markdown] and return the
     * new text, or null when that line is not a checklist item (#219).
     *
     * Used by the preview, where the tap carries a line number rather than a
     * caret. Returning null instead of guessing matters: a preview built from
     * slightly older text could point at a line that has since become something
     * else, and silently rewriting it would corrupt the note. Exactly one
     * character changes, so line endings, indentation and everything else in the
     * document are left byte-for-byte alone.
     */
    fun toggleTaskAtLine(markdown: String, lineIndex: Int): String? {
        val start = offsetOfLine(markdown, lineIndex) ?: return null
        val end = markdown.indexOf('\n', start).takeIf { it >= 0 } ?: markdown.length
        val match = checkboxPattern.find(markdown.substring(start, end)) ?: return null
        val box = start + match.value.indexOf('[') + 1
        val flipped = if (markdown[box] == ' ') 'x' else ' '
        return markdown.substring(0, box) + flipped + markdown.substring(box + 1)
    }

    /**
     * Character offset where 0-based [lineIndex] begins in [markdown], or null
     * when the text has no such line.
     *
     * The bridge from a preview row back to the source: both the checkbox
     * toggle (#219) and the outline's jump-to-heading (#215) arrive holding a
     * line number and need a caret position. Returning null rather than
     * clamping to the end matters — a preview built from slightly older text
     * can name a line that no longer exists, and dropping the tap is better
     * than moving the caret somewhere the user did not point at.
     */
    fun offsetOfLine(markdown: String, lineIndex: Int): Int? {
        if (lineIndex < 0) return null
        var start = 0
        repeat(lineIndex) {
            val newline = markdown.indexOf('\n', start)
            if (newline < 0) return null
            start = newline + 1
        }
        return start
    }

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

    /**
     * Indent the line under the caret (or every line touched by a multi-line selection)
     * by two spaces. Used as the Tab-key handler in the editor.
     */
    fun indent(value: TextFieldValue): TextFieldValue {
        val (blockStart, blockEnd) = selectionLineRange(value)
        val block = value.text.substring(blockStart, blockEnd)
        val transformed = block.split("\n").joinToString("\n") { "  $it" }
        val text = value.text.substring(0, blockStart) + transformed + value.text.substring(blockEnd)
        val delta = transformed.length - block.length
        val newSelection = TextRange(
            (value.selection.min + 2).coerceAtMost(text.length),
            (value.selection.max + delta).coerceAtMost(text.length)
        )
        return value.copy(text = text, selection = newSelection)
    }

    /**
     * Remove up to two leading spaces (or one tab) from every line touched by the
     * selection. Used as the Shift+Tab handler.
     */
    fun outdent(value: TextFieldValue): TextFieldValue {
        val (blockStart, blockEnd) = selectionLineRange(value)
        val block = value.text.substring(blockStart, blockEnd)
        var firstRemoved = 0
        var totalRemoved = 0
        val transformed = block.split("\n").mapIndexed { i, line ->
            val trimmed = when {
                line.startsWith("  ") -> line.removePrefix("  ").also {
                    if (i == 0) firstRemoved = 2
                    totalRemoved += 2
                }
                line.startsWith("\t") -> line.removePrefix("\t").also {
                    if (i == 0) firstRemoved = 1
                    totalRemoved += 1
                }
                line.startsWith(" ") -> line.removePrefix(" ").also {
                    if (i == 0) firstRemoved = 1
                    totalRemoved += 1
                }
                else -> line
            }
            trimmed
        }.joinToString("\n")
        val text = value.text.substring(0, blockStart) + transformed + value.text.substring(blockEnd)
        val newMin = (value.selection.min - firstRemoved).coerceAtLeast(blockStart)
        val newMax = (value.selection.max - totalRemoved).coerceAtLeast(newMin)
        return value.copy(text = text, selection = TextRange(newMin, newMax))
    }

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

    /**
     * Insert a two-column GFM table below the line the caret is on.
     *
     * Input: the field value, caret anywhere. Output: the value with
     * [TABLE_TEMPLATE] inserted as its own block, caret inside the first header
     * cell so the first thing typed names a column.
     *
     * Why its own block, with a blank line: a GFM table cannot interrupt a
     * paragraph, so a skeleton appended straight under a line of prose renders
     * as literal pipes — the exact failure a beginner reaching for this button
     * has no way to diagnose. `/table` never met it because that command always
     * runs on a line holding nothing but the query; a panel tap can land
     * mid-sentence, so the insertion point is the end of the caret's line
     * rather than the caret itself, and the separating newlines are computed
     * from what is actually there.
     */
    fun table(value: TextFieldValue): TextFieldValue {
        val at = blockInsertPoint(value)
        val leading = blockLeading(value.text, at)
        val trailing = blockTrailing(value.text, at)
        val insertion = leading + TABLE_TEMPLATE + trailing
        val updated = value.text.substring(0, at) + insertion + value.text.substring(at)
        return value.copy(
            text = updated,
            selection = TextRange(at + leading.length + TABLE_CARET_OFFSET)
        )
    }

    /**
     * Insert a `> [!NOTE]` callout, or turn the selected lines into one.
     *
     * Input: the field value. Output: with a selection, the selected lines
     * wrapped in a callout and left selected; without one, an empty callout as
     * its own block with the caret on its body line.
     *
     * Why the selection case exists at all: "I wrote this paragraph, make it a
     * note" is the way the construct is reached in practice, and the
     * alternative — dropping an empty callout after the selection and leaving
     * the user to re-type the text — is worse than doing nothing. The block
     * separation is the same rule [table] uses and for the same reason: a
     * blockquote glued to the line above is swallowed into that paragraph.
     */
    fun callout(value: TextFieldValue): TextFieldValue {
        val (blockStart, blockEnd) = selectionLineRange(value)
        val block = value.text.substring(blockStart, blockEnd)
        if (!value.selection.collapsed && block.isNotBlank()) {
            // Wrapping a callout in a callout is a mis-tap every time, and the
            // `> > [!NOTE]` it would produce is exactly the broken syntax a
            // beginner cannot unpick. Leave the text alone instead.
            if (calloutHeadPattern.containsMatchIn(block)) return value
            val quoted = block.split("\n").joinToString("\n") { line -> "> $line" }
            val leading = blockLeading(value.text, blockStart)
            val trailing = blockTrailing(value.text, blockEnd)
            val body = CALLOUT_HEAD + "\n" + quoted
            val updated = value.text.substring(0, blockStart) +
                leading + body + trailing +
                value.text.substring(blockEnd)
            val start = blockStart + leading.length
            return value.copy(text = updated, selection = TextRange(start, start + body.length))
        }

        val at = blockInsertPoint(value)
        val leading = blockLeading(value.text, at)
        val trailing = blockTrailing(value.text, at)
        val insertion = leading + CALLOUT_TEMPLATE + trailing
        val updated = value.text.substring(0, at) + insertion + value.text.substring(at)
        return value.copy(
            text = updated,
            selection = TextRange(at + leading.length + CALLOUT_TEMPLATE.length)
        )
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

    private fun selectionLineRange(value: TextFieldValue): Pair<Int, Int> {
        val text = value.text
        val selStart = value.selection.min.coerceIn(0, text.length)
        val selEnd = value.selection.max.coerceIn(0, text.length)
        val blockStart = text.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0))
            .let { if (it == -1) 0 else it + 1 }
        val nextNewline = text.indexOf('\n', selEnd)
        val blockEnd = if (nextNewline == -1) text.length else nextNewline
        return blockStart to blockEnd
    }

    /**
     * Where a block-level construct should be inserted for the current caret:
     * the end of the line it sits on, or the end of the selection's last line.
     *
     * Inserting at the caret would split a word in half; a block belongs after
     * the line, not inside it.
     */
    private fun blockInsertPoint(value: TextFieldValue): Int = selectionLineRange(value).second

    /**
     * Newlines needed before [at] so a block starting there begins a new block.
     *
     * Trailing spaces and tabs are ignored when looking backwards, because a
     * line of nothing but whitespace is a blank line to Markdown and should not
     * earn an extra one.
     */
    private fun blockLeading(text: String, at: Int): String {
        val before = text.substring(0, at.coerceIn(0, text.length)).trimEnd(' ', '\t')
        return when {
            before.isEmpty() -> ""
            before.endsWith("\n\n") -> ""
            before.endsWith("\n") -> "\n"
            else -> "\n\n"
        }
    }

    /**
     * Newlines needed after [at] so whatever follows is not absorbed by the
     * block just inserted — a plain line under a table row is read as another
     * row, and under a blockquote as more of the quote.
     */
    private fun blockTrailing(text: String, at: Int): String {
        val after = text.substring(at.coerceIn(0, text.length))
        return when {
            after.isEmpty() -> ""
            after.startsWith("\n\n") -> ""
            after.startsWith("\n") -> "\n"
            else -> "\n\n"
        }
    }

    fun findWordAtCursor(text: String, cursor: Int): TextRange {
        if (text.isEmpty() || cursor < 0 || cursor > text.length) return TextRange(cursor)

        // 단어 경계로 삼지 않을 문자(알파벳, 숫자, 한글 등 다국어 지원)
        // 공백, 줄바꿈, 마크다운 제어 문자(*, ~, `, _) 등은 단어 경계로 취급
        val isWordChar = { char: Char ->
            char.isLetterOrDigit() && char != '*' && char != '~' && char != '`' && char != '_'
        }

        // 1. 커서 오른쪽 문자가 단어 문자인 경우 -> 그 단어를 탐색
        if (cursor < text.length && isWordChar(text[cursor])) {
            var start = cursor
            while (start > 0 && isWordChar(text[start - 1])) {
                start--
            }
            var end = cursor
            while (end < text.length && isWordChar(text[end])) {
                end++
            }
            return TextRange(start, end)
        }

        // 2. 커서가 맨 끝이고 왼쪽 문자가 단어 문자인 경우 -> 그 단어를 탐색
        if (cursor == text.length && cursor > 0 && isWordChar(text[cursor - 1])) {
            var start = cursor - 1
            while (start > 0 && isWordChar(text[start - 1])) {
                start--
            }
            return TextRange(start, cursor)
        }

        // 3. 그 외의 경우 (공백 위이거나 단어 문자가 아닌 곳) -> collapsed
        return TextRange(cursor)
    }

    private fun wrapSelection(
        value: TextFieldValue,
        before: String,
        after: String,
        placeholder: String
    ): TextFieldValue {
        val text = value.text
        val start = value.selection.min
        val end = value.selection.max
        val isCollapsed = value.selection.collapsed

        // Case 1: 선택 범위가 존재하는 경우
        if (!isCollapsed) {
            val selected = text.substring(start, end)

            // Case 1-A: 선택 텍스트 자체가 이미 마커로 감싸인 경우 (예: **hello**) -> Unwrap
            if (selected.length >= (before.length + after.length) &&
                selected.startsWith(before) && selected.endsWith(after)
            ) {
                val unwrapped = selected.substring(before.length, selected.length - after.length)
                val updatedText = text.replaceRange(start, end, unwrapped)
                return value.copy(
                    text = updatedText,
                    selection = TextRange(start, start + unwrapped.length)
                )
            }

            // Case 1-B: 선택 영역 바로 바깥에 마커가 감싸고 있는 경우 (예: **[hello]**) -> Unwrap
            if (start >= before.length && end <= text.length - after.length) {
                val prefix = text.substring(start - before.length, start)
                val suffix = text.substring(end, end + after.length)
                if (prefix == before && suffix == after) {
                    val updatedText = text.removeRange(end, end + after.length)
                                          .removeRange(start - before.length, start)
                    val newStart = start - before.length
                    val newEnd = end - before.length
                    return value.copy(
                        text = updatedText,
                        selection = TextRange(newStart, newEnd)
                    )
                }
            }

            // Case 1-C: 감싸여 있지 않음 -> Wrap
            val replacement = "$before$selected$after"
            val updatedText = text.replaceRange(start, end, replacement)
            return value.copy(
                text = updatedText,
                selection = TextRange(start, start + replacement.length)
            )
        }

        // Case 2: 선택 범위가 없고 커서만 있는 경우 (Collapsed)
        val (lineStart, lineText) = currentLine(value)
        val relativeCursor = start - lineStart

        // Case 2-A: 커서 주변이 이미 마커로 감싸여 있는지 체크 (예: **hel|lo**) -> Unwrap
        val prefixCount = countOccurrences(lineText.substring(0, relativeCursor), before)
        val isInsideMarker = prefixCount % 2 != 0

        if (isInsideMarker) {
            val prefixIndex = lineText.lastIndexOf(before, relativeCursor - 1)
            val suffixIndex = lineText.indexOf(after, relativeCursor)

            if (prefixIndex != -1 && suffixIndex != -1 && prefixIndex < suffixIndex) {
                val innerText = lineText.substring(prefixIndex + before.length, suffixIndex)
                if (!innerText.contains(before) && !innerText.contains(after)) {
                    val absolutePrefix = lineStart + prefixIndex
                    val absoluteSuffix = lineStart + suffixIndex

                    val updatedText = text.removeRange(absoluteSuffix, absoluteSuffix + after.length)
                                          .removeRange(absolutePrefix, absolutePrefix + before.length)

                    val newCursor = if (start < absoluteSuffix) {
                        start - before.length
                    } else {
                        start - before.length - after.length
                    }.coerceAtLeast(0)

                    return value.copy(
                        text = updatedText,
                        selection = TextRange(newCursor)
                    )
                }
            }
        }

        // Case 2-B: 언랩 대상이 아님 -> 스마트 주변 단어 탐색 및 Wrap
        val wordRange = findWordAtCursor(text, start)
        if (!wordRange.collapsed) {
            val word = text.substring(wordRange.min, wordRange.max)
            val replacement = "$before$word$after"
            val updatedText = text.replaceRange(wordRange.min, wordRange.max, replacement)

            val relativeOffset = start - wordRange.min
            val newCursor = wordRange.min + before.length + relativeOffset
            return value.copy(
                text = updatedText,
                selection = TextRange(newCursor)
            )
        }

        // Case 2-C: 주변에 유효한 단어도 없음 -> 기존 Fallback
        val body = placeholder
        val replacement = "$before$body$after"
        val updatedText = text.replaceRange(start, start, replacement)
        val newCursor = start + before.length
        return value.copy(
            text = updatedText,
            selection = TextRange(newCursor)
        )
    }

    private fun countOccurrences(text: String, sub: String): Int {
        if (sub.isEmpty()) return 0
        var count = 0
        var idx = 0
        while (true) {
            idx = text.indexOf(sub, idx)
            if (idx == -1) break
            count++
            idx += sub.length
        }
        return count
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
