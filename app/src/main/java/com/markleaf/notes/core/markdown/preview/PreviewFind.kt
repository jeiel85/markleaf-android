package com.markleaf.notes.core.markdown.preview

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.markleaf.notes.core.markdown.CommonMarkPreviewAdapter
import com.markleaf.notes.core.markdown.PreviewInlineSegment
import com.markleaf.notes.core.markdown.PreviewLine
import com.markleaf.notes.core.markdown.PreviewLineType
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview

/**
 * One find-in-note hit in the preview (#417): the [lineIndex]-th row of the
 * full, unfiltered [PreviewLine] list, and which [occurrence] of the query
 * inside that row it is, counted across the row's [previewFindParts] in order.
 *
 * The index is into the full list rather than the visible one so a match inside
 * a collapsed `<details>` section is still found; navigating to it expands the
 * section first (see [expandSectionsFor]).
 */
data class PreviewFindMatch(val lineIndex: Int, val occurrence: Int)

/**
 * The text a preview row actually draws, one entry per separately laid-out
 * piece of text, in the order the renderer draws them. Find matches against
 * this rather than the Markdown source so a query hits what the reader sees —
 * `bold` finds **bold**, and a link matches its label, not its address.
 *
 * Decorations the renderer adds on its own (list bullets and numbers, callout
 * labels, a footnote's `[^n]` tag, a table's grid) are not part of the note's
 * text and are left out. A resolved image draws no text, so image rows are
 * never matched.
 */
internal fun previewFindParts(line: PreviewLine): List<String> = when (line.type) {
    PreviewLineType.H1, PreviewLineType.H2, PreviewLineType.H3,
    PreviewLineType.H4, PreviewLineType.H5, PreviewLineType.H6,
    PreviewLineType.CODE_BLOCK, PreviewLineType.FRONTMATTER,
    PreviewLineType.COLLAPSIBLE_SUMMARY -> listOf(line.text)
    PreviewLineType.BULLET, PreviewLineType.CHECKBOX_DONE, PreviewLineType.CHECKBOX_TODO,
    PreviewLineType.ORDERED_LIST, PreviewLineType.BODY, PreviewLineType.BLOCKQUOTE,
    PreviewLineType.FOOTNOTE_DEF -> listOf(inlineDisplayText(line.segments, line.text))
    PreviewLineType.CALLOUT -> calloutBodyLines(line.text).map {
        inlineDisplayText(SimpleMarkdownPreview.parseInlineSegments(it), it)
    }
    PreviewLineType.TABLE -> line.tableData?.let { data ->
        data.headers.mapIndexed { col, cell ->
            inlineDisplayText(data.headerSegments.getOrElse(col) { emptyList() }, cell)
        } + data.rows.flatMapIndexed { row, cells ->
            val segments = data.rowSegments.getOrElse(row) { emptyList() }
            cells.mapIndexed { col, cell -> inlineDisplayText(segments.getOrElse(col) { emptyList() }, cell) }
        }
    }.orEmpty()
    PreviewLineType.IMAGE, PreviewLineType.HORIZONTAL_RULE,
    PreviewLineType.COLLAPSIBLE_END -> emptyList()
}

/** A callout's body lines as its renderer draws them: blank lines are spacers, not text. */
internal fun calloutBodyLines(text: String): List<String> =
    if (text.isBlank()) emptyList() else text.split("\n").filter { it.isNotBlank() }

/** Mirrors `InlineMarkdownText`'s fallback: no segments means the raw text is drawn. */
private fun inlineDisplayText(segments: List<PreviewInlineSegment>, fallback: String): String =
    if (segments.isEmpty()) fallback else segments.joinToString("") { it.text }

/**
 * Start offsets of every non-overlapping, case-insensitive occurrence of
 * [query] in [text]. Compares with `ignoreCase` instead of lowercasing both
 * strings first, because lowercasing can change a string's length (`İ`) and
 * the offsets have to land on the text as drawn.
 */
internal fun findOccurrences(text: String, query: String): List<Int> {
    if (query.isEmpty() || text.length < query.length) return emptyList()
    val starts = mutableListOf<Int>()
    var from = 0
    while (true) {
        val found = text.indexOf(query, from, ignoreCase = true)
        if (found < 0) break
        starts += found
        from = found + query.length
    }
    return starts
}

/** Every match of [query] across [lines], in reading order. */
internal fun findInPreview(lines: List<PreviewLine>, query: String): List<PreviewFindMatch> {
    if (query.isEmpty()) return emptyList()
    val matches = mutableListOf<PreviewFindMatch>()
    lines.forEachIndexed { index, line ->
        val count = previewFindParts(line).sumOf { findOccurrences(it, query).size }
        repeat(count) { matches += PreviewFindMatch(index, it) }
    }
    return matches
}

/**
 * [toggledSectionIds] with every `<details>` section enclosing
 * `lines[lineIndex]` switched to expanded, so a match inside one can be
 * scrolled to. Sections already open, and every other section, are left as
 * the user set them.
 */
internal fun expandSectionsFor(
    lines: List<PreviewLine>,
    toggledSectionIds: Set<Int>,
    lineIndex: Int
): Set<Int> {
    val enclosing = lines.getOrNull(lineIndex)?.collapsibleIds.orEmpty()
    if (enclosing.isEmpty()) return toggledSectionIds
    val defaultOpenById = lines
        .filter { it.type == PreviewLineType.COLLAPSIBLE_SUMMARY }
        .associate { (it.collapsibleId ?: -1) to (it.extra == CommonMarkPreviewAdapter.OPEN_MARKER) }
    var result = toggledSectionIds
    enclosing.forEach { id ->
        if (!isSectionExpanded(defaultOpenById, result, id)) {
            result = if (id in result) result - id else result + id
        }
    }
    return result
}

/**
 * What one preview row should highlight: every occurrence of [query], with the
 * [current] one (an occurrence index within the row) drawn more strongly, or
 * none of them if the current match is elsewhere.
 */
internal data class PreviewFindHighlight(val query: String, val current: Int?) {
    /** The same highlight for a later piece of the row, after [consumed] occurrences. */
    fun after(consumed: Int): PreviewFindHighlight =
        copy(current = current?.minus(consumed)?.takeIf { it >= 0 })
}

/**
 * [text] with [highlight]'s occurrences marked, searching only from
 * [searchStart] on so a row's leading bullet or number is never matched.
 */
internal fun highlightFindMatches(
    text: AnnotatedString,
    highlight: PreviewFindHighlight?,
    matchStyle: SpanStyle,
    currentStyle: SpanStyle,
    searchStart: Int = 0
): AnnotatedString {
    if (highlight == null) return text
    val starts = findOccurrences(text.text.substring(searchStart), highlight.query)
    if (starts.isEmpty()) return text
    val builder = AnnotatedString.Builder(text)
    starts.forEachIndexed { occurrence, start ->
        val from = searchStart + start
        builder.addStyle(
            if (occurrence == highlight.current) currentStyle else matchStyle,
            from,
            from + highlight.query.length
        )
    }
    return builder.toAnnotatedString()
}
