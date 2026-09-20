package com.markleaf.notes.widget

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import com.markleaf.notes.core.markdown.PreviewInlineType
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview

/**
 * Turns one line of markdown source into styled text a `RemoteViews` `TextView`
 * can actually show (#438): [SingleNoteWidgetFactory] used to hand the source
 * line straight to `setTextViewText`, so `**bold**` read as `**bold**`.
 *
 * Why this is possible at all, when [SingleNoteWidget]'s own doc comment says a
 * widget can only draw plain text: that claim is true of *block* constructs — a
 * `RemoteViews` tree holds a fixed set of platform *views*, so a real heading,
 * table or image needs layout this factory's one-row-per-line model was never
 * built to provide. It is not true of *character styling* within one `TextView`.
 * `StyleSpan`, `StrikethroughSpan` and `TypefaceSpan` all implement
 * `android.text.ParcelableSpan`, so they survive the Binder trip a widget
 * update makes — this is a standard, long-documented Android technique, not
 * new plumbing invented for this fix.
 *
 * [style] reuses [SimpleMarkdownPreview.parseInlineSegments] — the same regexes
 * the in-app preview renders from — so a line styled here and the same line
 * previewed in the editor never disagree on what counts as emphasis.
 *
 * Deliberately inline-only. Headings, lists, blockquotes, tables, images and
 * fenced code stay untouched and keep showing their literal markers: [style]
 * is never applied to a fenced line (see [fenceFlags]), and nothing here reads
 * a leading `#`, `-`, `>` or table pipe. Rendering those for real needs either
 * multiple views per row or paragraph-level layout this widget does not have,
 * which is a larger and riskier change than this fix takes on.
 */
internal object WidgetInlineSpans {

    private val FENCE_MARKER = Regex("""^\s*(```+|~~~+)""")

    /**
     * One line of markdown, styled with [Typeface] and strikethrough spans in
     * place of the syntax markers `parseInlineSegments` recognizes; a line with
     * none of them comes back unchanged rather than wrapped for no reason.
     */
    fun style(line: String): CharSequence {
        val segments = SimpleMarkdownPreview.parseInlineSegments(line)
        if (segments.size == 1 && segments[0].type == PreviewInlineType.TEXT && segments[0].text == line) {
            return line
        }

        val builder = SpannableStringBuilder()
        for (segment in segments) {
            val start = builder.length
            builder.append(segment.text)
            val end = builder.length
            val span = spanFor(segment.type) ?: continue
            builder.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return builder
    }

    private fun spanFor(type: PreviewInlineType): Any? = when (type) {
        PreviewInlineType.BOLD -> StyleSpan(Typeface.BOLD)
        PreviewInlineType.ITALIC -> StyleSpan(Typeface.ITALIC)
        PreviewInlineType.BOLD_ITALIC -> StyleSpan(Typeface.BOLD_ITALIC)
        PreviewInlineType.STRIKETHROUGH -> StrikethroughSpan()
        PreviewInlineType.INLINE_CODE -> TypefaceSpan("monospace")
        // TEXT needs no span. FOOTNOTE_REF is left as parseInlineSegments's
        // bracket-stripped display text with no further styling. WIKILINK and
        // LINK are never actually produced by parseInlineSegments — those
        // types exist for the CommonMark-aware preview path, not this
        // line-local regex pass — so they cannot reach here, but the `when`
        // stays exhaustive rather than assuming that never changes.
        PreviewInlineType.TEXT,
        PreviewInlineType.FOOTNOTE_REF,
        PreviewInlineType.WIKILINK,
        PreviewInlineType.LINK -> null
    }

    /**
     * Per-line "leave this one raw" flags for [lines], true on a fence
     * delimiter itself and on every line between an opening and closing one.
     *
     * Needed because [style] parses each line in isolation and has no notion
     * of "inside a fenced block" on its own — without this, a code line like
     * Python's `**kwargs` would come out bolded, which is worse than the
     * literal asterisks it replaced.
     */
    fun fenceFlags(lines: List<String>): List<Boolean> {
        val flags = ArrayList<Boolean>(lines.size)
        var insideFence = false
        for (line in lines) {
            val isDelimiter = FENCE_MARKER.containsMatchIn(line)
            if (isDelimiter) insideFence = !insideFence
            flags += isDelimiter || insideFence
        }
        return flags
    }
}
