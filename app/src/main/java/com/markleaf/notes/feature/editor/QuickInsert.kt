package com.markleaf.notes.feature.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.markleaf.notes.core.markdown.MarkdownEditActions
import java.time.LocalDate
import java.util.Locale

internal enum class QuickInsertCommand(
    val aliases: List<String>
) {
    HEADING_1(listOf("h1", "heading1", "title1")),
    HEADING_2(listOf("h2", "heading2", "title2")),
    HEADING_3(listOf("h3", "heading3", "title3")),
    BULLET_LIST(listOf("bullet", "bullets", "unordered", "list")),
    NUMBERED_LIST(listOf("numbered", "ordered", "numbers", "list")),
    CHECKLIST(listOf("check", "checkbox", "task", "todo")),
    QUOTE(listOf("quote", "blockquote")),
    CODE_BLOCK(listOf("code", "fence", "snippet")),
    DIVIDER(listOf("divider", "rule", "hr")),
    TABLE(listOf("table", "grid")),
    CALLOUT(listOf("callout", "note", "notice")),
    WIKILINK(listOf("wiki", "wikilink", "note", "link")),
    IMAGE(listOf("image", "photo", "picture", "attachment")),
    DATE(listOf("date", "today"))
}

/**
 * The formatting-panel row that inserts the same construct as this command.
 *
 * Exhaustive on purpose, and that is the whole point: a new
 * [QuickInsertCommand] will not compile until someone says which panel row it
 * belongs to. Twice now a construct has shipped behind the slash menu alone and
 * been found only when a user gave up looking for it in the panel — tables and
 * callouts in #390, wikilinks and the date in #424 — because nothing made the
 * omission visible at the point it was introduced. `EditorFormattingParityTest`
 * turns the mapping below into a failing test when a row goes missing.
 *
 * The reverse does not hold and should not: bold, italic, strikethrough, inline
 * code and the Markdown link wrap a selection rather than insert a construct,
 * so they have no `/` command and need none.
 */
internal fun QuickInsertCommand.panelEquivalent(): EditorFormattingAction = when (this) {
    // The panel's heading row cycles H1 -> H2 -> H3 where `/h2` lands directly;
    // the construct is the same one and that is what parity is about here.
    QuickInsertCommand.HEADING_1,
    QuickInsertCommand.HEADING_2,
    QuickInsertCommand.HEADING_3 -> EditorFormattingAction.HEADING
    QuickInsertCommand.BULLET_LIST -> EditorFormattingAction.BULLET_LIST
    QuickInsertCommand.NUMBERED_LIST -> EditorFormattingAction.ORDERED_LIST
    QuickInsertCommand.CHECKLIST -> EditorFormattingAction.CHECKLIST
    QuickInsertCommand.QUOTE -> EditorFormattingAction.QUOTE
    QuickInsertCommand.CODE_BLOCK -> EditorFormattingAction.CODE_BLOCK
    QuickInsertCommand.DIVIDER -> EditorFormattingAction.DIVIDER
    QuickInsertCommand.TABLE -> EditorFormattingAction.TABLE
    QuickInsertCommand.CALLOUT -> EditorFormattingAction.CALLOUT
    QuickInsertCommand.WIKILINK -> EditorFormattingAction.WIKILINK
    QuickInsertCommand.IMAGE -> EditorFormattingAction.IMAGE
    QuickInsertCommand.DATE -> EditorFormattingAction.DATE
}

internal data class QuickInsertQuery(
    val start: Int,
    val end: Int,
    val text: String
)

internal data class QuickInsertSearchItem(
    val command: QuickInsertCommand,
    val label: String,
    val aliases: List<String> = command.aliases
)

internal fun detectQuickInsertQuery(value: TextFieldValue): QuickInsertQuery? {
    if (!value.selection.collapsed) return null

    val cursor = value.selection.start.coerceIn(0, value.text.length)
    val lineStart = value.text.lastIndexOf('\n', cursor - 1)
        .let { if (it < 0) 0 else it + 1 }
    val beforeCursor = value.text.substring(lineStart, cursor)
    val slashOffset = beforeCursor.indexOfFirst { !it.isWhitespace() }
    if (slashOffset < 0 || beforeCursor[slashOffset] != '/') return null

    val query = beforeCursor.substring(slashOffset + 1)
    if (query.any { !isQuickInsertQueryChar(it) }) return null

    val slashIndex = lineStart + slashOffset
    return QuickInsertQuery(
        start = slashIndex,
        end = cursor,
        text = query
    )
}

internal fun filterQuickInsertCommands(
    items: List<QuickInsertSearchItem>,
    query: String
): List<QuickInsertSearchItem> {
    val needle = query.trim().lowercase(Locale.ROOT)
    if (needle.isEmpty()) return items

    fun QuickInsertSearchItem.terms(): List<String> =
        listOf(label) + aliases

    val matches = items.filter { item ->
        item.terms().any { it.lowercase(Locale.ROOT).contains(needle) }
    }
    val (prefix, substring) = matches.partition { item ->
        item.terms().any { it.lowercase(Locale.ROOT).startsWith(needle) }
    }
    return prefix + substring
}

internal fun safeQuickInsertIndex(
    selectedIndex: Int,
    itemCount: Int
): Int = selectedIndex.coerceIn(0, itemCount - 1)

internal fun applyQuickInsertCommand(
    value: TextFieldValue,
    query: QuickInsertQuery,
    command: QuickInsertCommand,
    today: LocalDate = LocalDate.now()
): TextFieldValue {
    if (query.start !in 0..value.text.length || query.end !in query.start..value.text.length) {
        return value
    }

    val insertion = insertionFor(command, today)
    val updated = value.text.replaceRange(query.start, query.end, insertion.text)
    return value.copy(
        text = updated,
        selection = TextRange(query.start + insertion.caretOffset)
    )
}

private fun isQuickInsertQueryChar(char: Char): Boolean =
    char.isLetterOrDigit() || char == '_' || char == '-'

private data class QuickInsertion(
    val text: String,
    val caretOffset: Int = text.length
)

private fun insertionFor(
    command: QuickInsertCommand,
    today: LocalDate
): QuickInsertion = when (command) {
    QuickInsertCommand.HEADING_1 -> QuickInsertion("# ")
    QuickInsertCommand.HEADING_2 -> QuickInsertion("## ")
    QuickInsertCommand.HEADING_3 -> QuickInsertion("### ")
    QuickInsertCommand.BULLET_LIST -> QuickInsertion("- ")
    QuickInsertCommand.NUMBERED_LIST -> QuickInsertion("1. ")
    QuickInsertCommand.CHECKLIST -> QuickInsertion("- [ ] ")
    QuickInsertCommand.QUOTE -> QuickInsertion("> ")
    QuickInsertCommand.CODE_BLOCK -> QuickInsertion("```\n\n```\n", caretOffset = 4)
    QuickInsertCommand.DIVIDER -> QuickInsertion("---\n")
    // Both templates live in MarkdownEditActions, which the formatting panel
    // inserts from too — the same construct behind two doors (#390).
    QuickInsertCommand.TABLE -> QuickInsertion(
        MarkdownEditActions.TABLE_TEMPLATE,
        caretOffset = MarkdownEditActions.TABLE_CARET_OFFSET
    )
    QuickInsertCommand.CALLOUT -> QuickInsertion(MarkdownEditActions.CALLOUT_TEMPLATE)
    QuickInsertCommand.WIKILINK -> QuickInsertion(
        MarkdownEditActions.WIKILINK_TEMPLATE,
        caretOffset = MarkdownEditActions.WIKILINK_CARET_OFFSET
    )
    QuickInsertCommand.IMAGE -> QuickInsertion("")
    QuickInsertCommand.DATE -> QuickInsertion(today.toString())
}
