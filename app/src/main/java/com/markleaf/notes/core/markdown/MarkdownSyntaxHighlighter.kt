package com.markleaf.notes.core.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

data class MarkdownSyntaxColors(
    val heading: Color,
    val emphasis: Color,
    val link: Color,
    val syntax: Color,
    val checkbox: Color,
    val code: Color = Color.Gray,
    val codeBlock: Color = Color.Gray,
    val table: Color = Color.Gray,
    val blockquote: Color = Color.Gray,
    val horizontalRule: Color = Color.Gray
)

object MarkdownSyntaxHighlighter {
    fun highlight(text: String, colors: MarkdownSyntaxColors): AnnotatedString {
        val builder = AnnotatedString.Builder(text)

        addLineStyles(builder, text, colors)
        addInlineStyles(builder, text, colors)

        return builder.toAnnotatedString()
    }

    private fun addLineStyles(
        builder: AnnotatedString.Builder,
        text: String,
        colors: MarkdownSyntaxColors
    ) {
        HEADING_REGEX.findAll(text).forEach { match ->
            builder.addStyle(
                SpanStyle(color = colors.heading, fontWeight = FontWeight.SemiBold),
                match.range.first,
                match.range.last + 1
            )
            builder.addStyle(
                SpanStyle(color = colors.syntax),
                match.range.first,
                match.range.first + match.value.takeWhile { it == '#' }.length
            )
        }

        CHECKBOX_REGEX.findAll(text).forEach { match ->
            builder.addStyle(
                SpanStyle(color = colors.checkbox),
                match.range.first,
                match.range.last + 1
            )
            val markerStart = match.value.indexOf("[")
            if (markerStart >= 0) {
                val start = match.range.first + markerStart
                builder.addStyle(
                    SpanStyle(color = colors.syntax, fontWeight = FontWeight.SemiBold),
                    start,
                    start + 3
                )
            }
        }

        CODE_BLOCK_REGEX.findAll(text).forEach { match ->
            builder.addStyle(
                SpanStyle(
                    color = colors.code,
                    background = colors.codeBlock.copy(alpha = 0.1f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                match.range.first,
                match.range.last + 1
            )
        }

        TABLE_REGEX.findAll(text).forEach { match ->
            builder.addStyle(
                SpanStyle(color = colors.table),
                match.range.first,
                match.range.last + 1
            )
            // Highlight the pipe characters
            match.value.forEachIndexed { index, char ->
                if (char == '|') {
                    builder.addStyle(
                        SpanStyle(color = colors.syntax),
                        match.range.first + index,
                        match.range.first + index + 1
                    )
                }
            }
        }

        BLOCKQUOTE_REGEX.findAll(text).forEach { match ->
            builder.addStyle(
                SpanStyle(color = colors.blockquote),
                match.range.first,
                match.range.last + 1
            )
            val markerLength = match.value.takeWhile { it == '>' || it == ' ' }.length
            builder.addStyle(
                SpanStyle(color = colors.blockquote, fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.first + markerLength
            )
        }

        HORIZONTAL_RULE_REGEX.findAll(text).forEach { match ->
            builder.addStyle(
                SpanStyle(color = colors.horizontalRule, fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    private fun addInlineStyles(
        builder: AnnotatedString.Builder,
        text: String,
        colors: MarkdownSyntaxColors
    ) {
        INLINE_CODE_REGEX.findAll(text).forEach { match ->
            builder.addStyle(
                SpanStyle(
                    color = colors.code,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                match.range.first,
                match.range.last + 1
            )
            styleMarker(builder, colors, match.range.first, 1)
            styleMarker(builder, colors, match.range.last, 1)
        }

        STRIKETHROUGH_REGEX.findAll(text).forEach { match ->
            builder.addStyle(
                SpanStyle(
                    color = colors.emphasis,
                    textDecoration = TextDecoration.LineThrough
                ),
                match.range.first,
                match.range.last + 1
            )
            styleMarker(builder, colors, match.range.first, 2)
            styleMarker(builder, colors, match.range.last - 1, 2)
        }

        BOLD_REGEX.findAll(text).forEach { match ->
            builder.addStyle(
                SpanStyle(color = colors.emphasis, fontWeight = FontWeight.SemiBold),
                match.range.first,
                match.range.last + 1
            )
            styleMarker(builder, colors, match.range.first, 2)
            styleMarker(builder, colors, match.range.last - 1, 2)
        }

        ITALIC_REGEX.findAll(text).forEach { match ->
            builder.addStyle(
                SpanStyle(color = colors.emphasis, fontStyle = FontStyle.Italic),
                match.range.first,
                match.range.last + 1
            )
            styleMarker(builder, colors, match.range.first, 1)
            styleMarker(builder, colors, match.range.last, 1)
        }

        ITALIC_UNDERSCORE_REGEX.findAll(text).forEach { match ->
            builder.addStyle(
                SpanStyle(color = colors.emphasis, fontStyle = FontStyle.Italic),
                match.range.first,
                match.range.last + 1
            )
            styleMarker(builder, colors, match.range.first, 1)
            styleMarker(builder, colors, match.range.last, 1)
        }

        MARKDOWN_LINK_REGEX.findAll(text).forEach { match ->
            builder.addStyle(
                SpanStyle(color = colors.link, textDecoration = TextDecoration.Underline),
                match.range.first,
                match.range.last + 1
            )
            styleMarker(builder, colors, match.range.first, 1)
            match.value.indexOf("](").takeIf { it >= 0 }?.let { localIndex ->
                styleMarker(builder, colors, match.range.first + localIndex, 2)
            }
            styleMarker(builder, colors, match.range.last, 1)
        }

        WIKI_LINK_REGEX.findAll(text).forEach { match ->
            builder.addStyle(
                SpanStyle(color = colors.link, textDecoration = TextDecoration.Underline),
                match.range.first,
                match.range.last + 1
            )
            styleMarker(builder, colors, match.range.first, 2)
            styleMarker(builder, colors, match.range.last - 1, 2)
        }
    }

    private fun styleMarker(
        builder: AnnotatedString.Builder,
        colors: MarkdownSyntaxColors,
        start: Int,
        length: Int
    ) {
        builder.addStyle(SpanStyle(color = colors.syntax), start, start + length)
    }

    private val HEADING_REGEX = Regex("""(?m)^#{1,6}\s.+$""")
    private val CHECKBOX_REGEX = Regex("""(?m)^-\s\[[ xX]]\s.+$""")
    private val CODE_BLOCK_REGEX = Regex("""(?sm)^```.*?```""")
    private val TABLE_REGEX = Regex("""(?m)^\|.*$""")
    private val BLOCKQUOTE_REGEX = Regex("""(?m)^>.*$""")
    private val HORIZONTAL_RULE_REGEX = Regex("""(?m)^(\*\*\*|---|___)\s*$""")
    private val INLINE_CODE_REGEX = Regex("""`[^`\n]+?`""")
    private val STRIKETHROUGH_REGEX = Regex("""~~[^~\n]+?~~""")
    private val BOLD_REGEX = Regex("""\*\*[^*\n]+?\*\*""")
    private val ITALIC_REGEX = Regex("""(?<!\*)\*[^*\n]+?\*(?!\*)""")
    private val ITALIC_UNDERSCORE_REGEX = Regex("""(?<!\w)_[^_\n]+?_(?!\w)""")
    private val MARKDOWN_LINK_REGEX = Regex("""\[[^\]\n]+]\([^) \n][^)\n]*\)""")
    private val WIKI_LINK_REGEX = Regex("""\[\[[^\]\n]+]]""")
}
