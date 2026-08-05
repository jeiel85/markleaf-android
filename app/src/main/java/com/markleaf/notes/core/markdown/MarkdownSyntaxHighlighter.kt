package com.markleaf.notes.core.markdown

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * The colours the live editor paints markdown with.
 *
 * Every field is required on purpose. Five of them used to default to
 * [Color.Gray], and the editor — the only construction site — named six of the
 * ten, so blockquote text and `---` rules drew the same fixed grey in both
 * themes: 3.41:1 against the light background, under the 4.5:1 WCAG threshold
 * for text. A default cannot know the theme, so there are none; a caller that
 * forgets a role now fails to compile instead of silently rendering grey.
 *
 * Build it with [markdownSyntaxColors] rather than by hand — that is where the
 * roles are chosen and where the contrast is pinned by `EditorColorContrastTest`.
 */
data class MarkdownSyntaxColors(
    val heading: Color,
    val emphasis: Color,
    val link: Color,
    val syntax: Color,
    val checkbox: Color,
    val code: Color,
    val codeBlock: Color,
    val blockquote: Color,
    val horizontalRule: Color
)

/**
 * Derives the editor palette from the active [ColorScheme], so it follows the
 * app theme and Material You alike. The editor and the live-rendering golden
 * test both build it here, so the snapshot pins what the app actually draws.
 */
fun markdownSyntaxColors(scheme: ColorScheme): MarkdownSyntaxColors = MarkdownSyntaxColors(
    heading = scheme.primary,
    emphasis = scheme.tertiary,
    link = scheme.primary,
    syntax = scheme.onSurfaceVariant,
    checkbox = scheme.secondary,
    code = scheme.tertiary,
    // Read only as `codeBlock.copy(alpha = 0.1f)` — a wash behind a fenced
    // block, not text. onSurfaceVariant is the role that darkens a light
    // surface and lightens a dark one, so the wash stays visible either way.
    codeBlock = scheme.onSurfaceVariant,
    // Quoted text and rules are structure, not body copy: Material's
    // medium-emphasis role recedes the way the fixed grey was trying to,
    // and clears 4.5:1 in both themes (8.94:1 light, 10.14:1 dark).
    blockquote = scheme.onSurfaceVariant,
    horizontalRule = scheme.onSurfaceVariant
)

/**
 * Bear-style live preview: applies inline span styles directly to the editor's
 * text so that `# Heading` renders at heading size, `**bold**` renders bold, and
 * markdown markers (`#`, `**`, `_`, etc.) recede visually as muted small chars.
 *
 * Character indices are preserved exactly — only the visual rendering changes.
 * The underlying text remains plain markdown, which is what the cursor and
 * `BasicTextField`'s text manipulation operate on.
 */
object MarkdownSyntaxHighlighter {
    private class Memo(
        val text: String,
        val colors: MarkdownSyntaxColors,
        val result: AnnotatedString
    )

    // Single-entry memo. The editor's `BasicTextField` runs this through a
    // `VisualTransformation` on the UI thread, and Compose invokes that filter on
    // every recomposition / measure pass — often more than once per keystroke.
    // Each uncached call re-scans the whole document with 11 regex passes, so on a
    // large note that O(n) work lands repeatedly on the main thread and drops
    // frames. Caching the last (text, colors) keeps it to once per actual change.
    // `highlight` is a pure function of its inputs and the result is an immutable
    // `AnnotatedString`, so a benign cross-thread race can at worst recompute —
    // it can never return wrong output.
    @Volatile
    private var memo: Memo? = null

    fun highlight(text: String, colors: MarkdownSyntaxColors): AnnotatedString {
        memo?.let { cached ->
            if (cached.text == text && cached.colors == colors) return cached.result
        }

        val builder = AnnotatedString.Builder(text)

        addLineStyles(builder, text, colors)
        addInlineStyles(builder, text, colors)

        val result = builder.toAnnotatedString()
        memo = Memo(text, colors, result)
        return result
    }

    private fun addLineStyles(
        builder: AnnotatedString.Builder,
        text: String,
        colors: MarkdownSyntaxColors
    ) {
        HEADING_REGEX.findAll(text).forEach { match ->
            val markerLen = match.value.takeWhile { it == '#' }.length
            val (size, weight) = headingMetrics(markerLen)

            // Heading line gets the heading color across the full range.
            builder.addStyle(
                SpanStyle(color = colors.heading),
                match.range.first,
                match.range.last + 1
            )
            // Content (after `# `) gets the rich heading size + weight.
            val contentStart = match.range.first + markerLen + 1 // +1 for the required space
            if (contentStart <= match.range.last + 1) {
                builder.addStyle(
                    SpanStyle(fontSize = size, fontWeight = weight),
                    contentStart,
                    match.range.last + 1
                )
            }
            // Marker (`#`s) muted to syntax color and reset to body weight so it
            // visually retreats next to the larger content.
            builder.addStyle(
                muteMarkerStyle(colors),
                match.range.first,
                match.range.first + markerLen
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
                    fontFamily = FontFamily.Monospace
                ),
                match.range.first,
                match.range.last + 1
            )
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
                    fontFamily = FontFamily.Monospace
                ),
                match.range.first,
                match.range.last + 1
            )
            muteMarker(builder, colors, match.range.first, 1)
            muteMarker(builder, colors, match.range.last, 1)
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
            muteMarker(builder, colors, match.range.first, 2)
            muteMarker(builder, colors, match.range.last - 1, 2)
        }

        BOLD_REGEX.findAll(text).forEach { match ->
            // Bear-class: real bold weight on `**bold**` content.
            builder.addStyle(
                SpanStyle(color = colors.emphasis, fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
            muteMarker(builder, colors, match.range.first, 2)
            muteMarker(builder, colors, match.range.last - 1, 2)
        }

        ITALIC_REGEX.findAll(text).forEach { match ->
            builder.addStyle(
                SpanStyle(color = colors.emphasis, fontStyle = FontStyle.Italic),
                match.range.first,
                match.range.last + 1
            )
            muteMarker(builder, colors, match.range.first, 1)
            muteMarker(builder, colors, match.range.last, 1)
        }

        ITALIC_UNDERSCORE_REGEX.findAll(text).forEach { match ->
            builder.addStyle(
                SpanStyle(color = colors.emphasis, fontStyle = FontStyle.Italic),
                match.range.first,
                match.range.last + 1
            )
            muteMarker(builder, colors, match.range.first, 1)
            muteMarker(builder, colors, match.range.last, 1)
        }

        MARKDOWN_LINK_REGEX.findAll(text).forEach { match ->
            builder.addStyle(
                SpanStyle(color = colors.link, textDecoration = TextDecoration.Underline),
                match.range.first,
                match.range.last + 1
            )
            muteMarker(builder, colors, match.range.first, 1)
            match.value.indexOf("](").takeIf { it >= 0 }?.let { localIndex ->
                muteMarker(builder, colors, match.range.first + localIndex, 2)
            }
            muteMarker(builder, colors, match.range.last, 1)
        }
    }

    private fun headingMetrics(level: Int): Pair<TextUnit, FontWeight> = when (level) {
        1 -> 24.sp to FontWeight.Bold
        2 -> 20.sp to FontWeight.SemiBold
        3 -> 18.sp to FontWeight.SemiBold
        else -> 16.sp to FontWeight.SemiBold
    }

    /**
     * Markers (`#`, `**`, `_`, backticks, `~~`, `[`, `](`, `)`) should visually
     * recede: muted color and reset of the rich attributes the surrounding
     * content carries (weight, style, decoration). They keep their position so
     * the cursor still moves through them, but they don't compete with the
     * styled content next to them.
     */
    private fun muteMarkerStyle(colors: MarkdownSyntaxColors): SpanStyle = SpanStyle(
        color = colors.syntax,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Normal,
        textDecoration = TextDecoration.None
    )

    private fun muteMarker(
        builder: AnnotatedString.Builder,
        colors: MarkdownSyntaxColors,
        start: Int,
        length: Int
    ) {
        builder.addStyle(muteMarkerStyle(colors), start, start + length)
    }

    private val HEADING_REGEX = Regex("""(?m)^#{1,6}\s.+$""")
    private val CHECKBOX_REGEX = Regex("""(?m)^-\s\[[ xX]]\s.+$""")
    private val CODE_BLOCK_REGEX = Regex("""(?sm)^```.*?```""")
    private val BLOCKQUOTE_REGEX = Regex("""(?m)^>.*$""")
    private val HORIZONTAL_RULE_REGEX = Regex("""(?m)^(\*\*\*|---|___)\s*$""")
    private val INLINE_CODE_REGEX = Regex("""`[^`\n]+?`""")
    private val STRIKETHROUGH_REGEX = Regex("""~~[^~\n]+?~~""")
    private val BOLD_REGEX = Regex("""\*\*[^*\n]+?\*\*""")
    private val ITALIC_REGEX = Regex("""(?<!\*)\*[^*\n]+?\*(?!\*)""")
    private val ITALIC_UNDERSCORE_REGEX = Regex("""(?<!\w)_[^_\n]+?_(?!\w)""")
    private val MARKDOWN_LINK_REGEX = Regex("""\[[^\]\n]+]\([^) \n][^)\n]*\)""")
}
