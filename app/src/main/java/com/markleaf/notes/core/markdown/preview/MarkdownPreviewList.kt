package com.markleaf.notes.core.markdown.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markleaf.notes.R
import com.markleaf.notes.core.markdown.CalloutKind
import com.markleaf.notes.core.markdown.PreviewInlineType
import com.markleaf.notes.core.markdown.PreviewLine
import com.markleaf.notes.core.markdown.PreviewLineType
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview

/**
 * Renders a list of [PreviewLine]s as a scrollable Markdown preview.
 * Pulled out of [com.markleaf.notes.feature.editor.EditorScreen] so the same
 * rendering can be exercised by snapshot tests independently of the editor
 * scaffolding.
 *
 * @param onWikilinkClick called when the user taps a `[[Title]]` segment.
 *   The argument is the target text inside the brackets (already trimmed).
 *   Default is a no-op so existing snapshot tests don't need to wire navigation.
 */
@Composable
fun MarkdownPreviewList(
    lines: List<PreviewLine>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    onWikilinkClick: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        items(lines) { line ->
            PreviewLineRenderer(line, onWikilinkClick = onWikilinkClick)
        }
    }
}

@Composable
fun PreviewLineRenderer(line: PreviewLine, onWikilinkClick: (String) -> Unit = {}) {
    when (line.type) {
        PreviewLineType.H1 -> Text(
            text = line.text,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        PreviewLineType.H2 -> Text(
            text = line.text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
        )
        PreviewLineType.H3 -> Text(
            text = line.text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
        )
        PreviewLineType.BULLET -> Text("• ${line.text}", style = MaterialTheme.typography.bodyLarge)
        PreviewLineType.CHECKBOX_DONE -> Text(
            text = "☑ ${line.text}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PreviewLineType.CHECKBOX_TODO -> Text("☐ ${line.text}", style = MaterialTheme.typography.bodyLarge)
        PreviewLineType.CODE_BLOCK -> MarkdownCodeBlock(line.text, line.extra)
        PreviewLineType.BODY -> InlineMarkdownText(line, onWikilinkClick = onWikilinkClick)
        PreviewLineType.BLOCKQUOTE -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                InlineMarkdownText(line, onWikilinkClick = onWikilinkClick)
                HorizontalDivider(
                    modifier = Modifier.padding(top = 4.dp),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
        PreviewLineType.CALLOUT -> CalloutBox(line)
        PreviewLineType.FRONTMATTER -> FrontmatterBlock(line.text)
        PreviewLineType.FOOTNOTE_DEF -> FootnoteDefRow(line)
        PreviewLineType.ORDERED_LIST -> Text(
            text = "${line.extra ?: "1"}. ${line.text}",
            style = MaterialTheme.typography.bodyLarge
        )
        PreviewLineType.HORIZONTAL_RULE -> HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        PreviewLineType.EMPTY -> Spacer(Modifier.height(8.dp))
    }
}

@Composable
internal fun InlineMarkdownText(line: PreviewLine, onWikilinkClick: (String) -> Unit = {}) {
    val annotated = buildAnnotatedString {
        line.segments.forEach { segment ->
            when (segment.type) {
                PreviewInlineType.TEXT -> append(segment.text)
                PreviewInlineType.BOLD -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(segment.text)
                }
                PreviewInlineType.ITALIC -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(segment.text)
                }
                PreviewInlineType.BOLD_ITALIC -> withStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                ) {
                    append(segment.text)
                }
                PreviewInlineType.STRIKETHROUGH -> withStyle(
                    SpanStyle(textDecoration = TextDecoration.LineThrough)
                ) {
                    append(segment.text)
                }
                PreviewInlineType.INLINE_CODE -> withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    append(segment.text)
                }
                PreviewInlineType.FOOTNOTE_REF -> withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        baselineShift = BaselineShift.Superscript
                    )
                ) {
                    append(segment.text)
                }
                PreviewInlineType.WIKILINK -> {
                    pushStringAnnotation(tag = WIKILINK_TAG, annotation = segment.text)
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(segment.text)
                    }
                    pop()
                }
            }
        }
    }
    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground
        ),
        modifier = Modifier.padding(vertical = 2.dp),
        onClick = { offset ->
            annotated.getStringAnnotations(WIKILINK_TAG, offset, offset).firstOrNull()
                ?.let { onWikilinkClick(it.item) }
        }
    )
}

private const val WIKILINK_TAG = "wikilink"

@Composable
private fun CalloutBox(line: PreviewLine) {
    val kind = CalloutKind.parse(line.extra.orEmpty())
    val visuals = calloutVisuals(kind, line.extra.orEmpty())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(visuals.containerColor)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = visuals.icon, color = visuals.accentColor)
            Spacer(Modifier.width(8.dp))
            Text(
                text = visuals.label,
                style = MaterialTheme.typography.labelLarge,
                color = visuals.accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (line.text.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            line.text.split("\n").forEach { bodyLine ->
                if (bodyLine.isBlank()) {
                    Spacer(Modifier.height(4.dp))
                } else {
                    InlineMarkdownText(
                        line = PreviewLine(
                            text = bodyLine,
                            type = PreviewLineType.BODY,
                            segments = SimpleMarkdownPreview.parseInlineSegments(bodyLine)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun calloutVisuals(kind: CalloutKind?, raw: String): CalloutVisuals = when (kind) {
    CalloutKind.NOTE -> CalloutVisuals(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.primary,
        stringResource(R.string.callout_note),
        "ℹ"
    )
    CalloutKind.TIP -> CalloutVisuals(
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.secondary,
        stringResource(R.string.callout_tip),
        "💡"
    )
    CalloutKind.IMPORTANT -> CalloutVisuals(
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.tertiary,
        stringResource(R.string.callout_important),
        "★"
    )
    CalloutKind.WARNING -> CalloutVisuals(
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.error,
        stringResource(R.string.callout_warning),
        "⚠"
    )
    CalloutKind.CAUTION -> CalloutVisuals(
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.error,
        stringResource(R.string.callout_caution),
        "⛔"
    )
    null -> CalloutVisuals(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.onSurfaceVariant,
        raw,
        "•"
    )
}

private data class CalloutVisuals(
    val containerColor: Color,
    val accentColor: Color,
    val label: String,
    val icon: String
)

@Composable
private fun FrontmatterBlock(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun FootnoteDefRow(line: PreviewLine) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "[^${line.extra}]",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 6.dp)
        )
        InlineMarkdownText(line.copy(type = PreviewLineType.BODY))
    }
}

@Composable
private fun MarkdownCodeBlock(text: String, language: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (!language.isNullOrEmpty()) {
            Text(
                text = language,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
