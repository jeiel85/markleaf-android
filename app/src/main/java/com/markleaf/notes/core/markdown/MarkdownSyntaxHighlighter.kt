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
    /**
     * Above this many characters the document is handed back unstyled without
     * being scanned at all.
     *
     * The scan is linear and cheap — 11 regex passes over 200,000 characters
     * measure ~12 ms on desktop-class hardware — but it runs on the UI thread
     * once per text change, so on a phone a document this size costs a visible
     * fraction of a second on every keystroke for styling nobody can read all
     * of at once. Past this length the cheapest correct answer is no answer.
     */
    internal const val MAX_HIGHLIGHT_CHARS = 100_000

    /**
     * The number of span styles a single document may carry. Past it the
     * document is handed back unstyled.
     *
     * This is the guard that matters (#437). The scan is linear; what is not
     * linear is what happens to the result afterwards. The reporter measured
     * paste time on one device growing far faster than the document —
     * 50k→~4 s, 100k→~11 s, 150k→~25 s, 200k→ANR — on a markdown file, while a
     * *prose* file of the same lengths pasted in ~2 s flat. Both run the same
     * 11 passes over the same number of characters; the only thing that differs
     * is how many matches they produce, and therefore how many spans this
     * function emits (my markdown fixture: ~2,500 at 50k, ~10,000 at 200k;
     * prose: zero at every size). So the cost is superlinear in the *span
     * count*, and it is paid downstream in text layout, not here.
     *
     * 1,500 is anchored to that same report: a 10k-character markdown file was
     * "instant" on that device (~500 spans on my fixture), and 50k — roughly
     * 2,500 spans — already froze the editor for four seconds. The budget sits
     * between the two, closer to the size that worked. It is deliberately a
     * count of spans rather than of characters, because that is what the
     * evidence says costs: a 200,000-character prose note emits no spans and
     * stays styled, while a heavily marked-up note degrades early.
     *
     * Degrading means dropping *all* styling for that document rather than the
     * first N spans: a note styled down to some arbitrary offset and plain
     * after it looks like a rendering bug, and where that offset fell would
     * depend on nothing the reader can see.
     */
    internal const val MAX_SPAN_COUNT = 1_500

    /** A span the passes have decided on, before it is applied to the text. */
    private class PendingSpan(val style: SpanStyle, val start: Int, val end: Int)

    private class Memo(
        val text: String,
        val colors: MarkdownSyntaxColors,
        val fontScale: Float,
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

    /**
     * In: the editor's raw markdown, the theme's [colors], and the editor's
     * text scale. Out: the same characters, with span styles applied — or with
     * none at all when the document is past [MAX_HIGHLIGHT_CHARS] or would need
     * more than [MAX_SPAN_COUNT] spans.
     *
     * The passes collect into a list first and the result is built from it,
     * rather than styling straight into the builder. That is what lets the
     * budget be a decision about the whole document instead of a cut-off in the
     * middle of one: the count is known before a single span is applied, and
     * the spans are applied in the order the passes produced them, so a
     * document under budget renders exactly as it did before.
     */
    fun highlight(text: String, colors: MarkdownSyntaxColors, fontScale: Float = 1f): AnnotatedString {
        memo?.let { cached ->
            if (cached.text == text && cached.colors == colors && cached.fontScale == fontScale) {
                return cached.result
            }
        }

        val result = if (text.length > MAX_HIGHLIGHT_CHARS) {
            plain(text)
        } else {
            val spans = ArrayList<PendingSpan>(64)
            addLineStyles(spans, text, colors, fontScale)
            addInlineStyles(spans, text, colors)
            if (spans.size > MAX_SPAN_COUNT) plain(text) else styled(text, spans)
        }

        // The memo covers the unstyled answers too. They are the documents
        // where re-deciding on every recomposition would cost the most.
        memo = Memo(text, colors, fontScale, result)
        return result
    }

    private fun plain(text: String): AnnotatedString =
        AnnotatedString.Builder(text).toAnnotatedString()

    private fun styled(text: String, spans: List<PendingSpan>): AnnotatedString {
        val builder = AnnotatedString.Builder(text)
        spans.forEach { builder.addStyle(it.style, it.start, it.end) }
        return builder.toAnnotatedString()
    }

    private fun MutableList<PendingSpan>.addStyle(style: SpanStyle, start: Int, end: Int) {
        add(PendingSpan(style, start, end))
    }

    private fun addLineStyles(
        spans: MutableList<PendingSpan>,
        text: String,
        colors: MarkdownSyntaxColors,
        fontScale: Float
    ) {
        HEADING_REGEX.findAll(text).forEach { match ->
            val markerLen = match.value.takeWhile { it == '#' }.length
            val (size, weight) = headingMetrics(markerLen, fontScale)

            // Heading line gets the heading color across the full range.
            spans.addStyle(
                SpanStyle(color = colors.heading),
                match.range.first,
                match.range.last + 1
            )
            // Content (after `# `) gets the rich heading size + weight.
            val contentStart = match.range.first + markerLen + 1 // +1 for the required space
            if (contentStart <= match.range.last + 1) {
                spans.addStyle(
                    SpanStyle(fontSize = size, fontWeight = weight),
                    contentStart,
                    match.range.last + 1
                )
            }
            // Marker (`#`s) muted to syntax color and reset to body weight so it
            // visually retreats next to the larger content.
            spans.addStyle(
                muteMarkerStyle(colors),
                match.range.first,
                match.range.first + markerLen
            )
        }

        CHECKBOX_REGEX.findAll(text).forEach { match ->
            spans.addStyle(
                SpanStyle(color = colors.checkbox),
                match.range.first,
                match.range.last + 1
            )
            val markerStart = match.value.indexOf("[")
            if (markerStart >= 0) {
                val start = match.range.first + markerStart
                spans.addStyle(
                    SpanStyle(color = colors.syntax, fontWeight = FontWeight.SemiBold),
                    start,
                    start + 3
                )
            }
        }

        CODE_BLOCK_REGEX.findAll(text).forEach { match ->
            spans.addStyle(
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
            spans.addStyle(
                SpanStyle(color = colors.blockquote),
                match.range.first,
                match.range.last + 1
            )
            val markerLength = match.value.takeWhile { it == '>' || it == ' ' }.length
            spans.addStyle(
                SpanStyle(color = colors.blockquote, fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.first + markerLength
            )
        }

        HORIZONTAL_RULE_REGEX.findAll(text).forEach { match ->
            spans.addStyle(
                SpanStyle(color = colors.horizontalRule, fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    private fun addInlineStyles(
        spans: MutableList<PendingSpan>,
        text: String,
        colors: MarkdownSyntaxColors
    ) {
        INLINE_CODE_REGEX.findAll(text).forEach { match ->
            spans.addStyle(
                SpanStyle(
                    color = colors.code,
                    fontFamily = FontFamily.Monospace
                ),
                match.range.first,
                match.range.last + 1
            )
            muteMarker(spans, colors, match.range.first, 1)
            muteMarker(spans, colors, match.range.last, 1)
        }

        STRIKETHROUGH_REGEX.findAll(text).forEach { match ->
            spans.addStyle(
                SpanStyle(
                    color = colors.emphasis,
                    textDecoration = TextDecoration.LineThrough
                ),
                match.range.first,
                match.range.last + 1
            )
            muteMarker(spans, colors, match.range.first, 2)
            muteMarker(spans, colors, match.range.last - 1, 2)
        }

        BOLD_REGEX.findAll(text).forEach { match ->
            // Bear-class: real bold weight on `**bold**` content.
            spans.addStyle(
                SpanStyle(color = colors.emphasis, fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
            muteMarker(spans, colors, match.range.first, 2)
            muteMarker(spans, colors, match.range.last - 1, 2)
        }

        ITALIC_REGEX.findAll(text).forEach { match ->
            spans.addStyle(
                SpanStyle(color = colors.emphasis, fontStyle = FontStyle.Italic),
                match.range.first,
                match.range.last + 1
            )
            muteMarker(spans, colors, match.range.first, 1)
            muteMarker(spans, colors, match.range.last, 1)
        }

        ITALIC_UNDERSCORE_REGEX.findAll(text).forEach { match ->
            spans.addStyle(
                SpanStyle(color = colors.emphasis, fontStyle = FontStyle.Italic),
                match.range.first,
                match.range.last + 1
            )
            muteMarker(spans, colors, match.range.first, 1)
            muteMarker(spans, colors, match.range.last, 1)
        }

        MARKDOWN_LINK_REGEX.findAll(text).forEach { match ->
            spans.addStyle(
                SpanStyle(color = colors.link, textDecoration = TextDecoration.Underline),
                match.range.first,
                match.range.last + 1
            )
            muteMarker(spans, colors, match.range.first, 1)
            match.value.indexOf("](").takeIf { it >= 0 }?.let { localIndex ->
                muteMarker(spans, colors, match.range.first + localIndex, 2)
            }
            muteMarker(spans, colors, match.range.last, 1)
        }
    }

    private fun headingMetrics(level: Int, fontScale: Float): Pair<TextUnit, FontWeight> = when (level) {
        1 -> (24.sp * fontScale) to FontWeight.Bold
        2 -> (20.sp * fontScale) to FontWeight.SemiBold
        3 -> (18.sp * fontScale) to FontWeight.SemiBold
        else -> (16.sp * fontScale) to FontWeight.SemiBold
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
        spans: MutableList<PendingSpan>,
        colors: MarkdownSyntaxColors,
        start: Int,
        length: Int
    ) {
        spans.addStyle(muteMarkerStyle(colors), start, start + length)
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
