package com.markleaf.notes.feature.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
    QuickInsertCommand.TABLE -> QuickInsertion(
        "| Column 1 | Column 2 |\n| --- | --- |\n|  |  |",
        caretOffset = 2
    )
    QuickInsertCommand.CALLOUT -> QuickInsertion("> [!NOTE]\n> ")
    QuickInsertCommand.WIKILINK -> QuickInsertion("[[]]", caretOffset = 2)
    QuickInsertCommand.IMAGE -> QuickInsertion("")
    QuickInsertCommand.DATE -> QuickInsertion(today.toString())
}
