package com.markleaf.notes.core.markdown.preview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.markleaf.notes.util.AttachmentManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.markleaf.notes.core.markdown.PreviewInlineSegment
import com.markleaf.notes.core.markdown.PreviewInlineType
import com.markleaf.notes.core.markdown.PreviewLine
import com.markleaf.notes.core.markdown.PreviewLineType
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import com.markleaf.notes.core.markdown.TableAlignment
import com.markleaf.notes.core.markdown.TableData
import com.markleaf.notes.core.markdown.syntax.SyntaxHighlighter

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
    onWikilinkClick: (String) -> Unit = {},
    onImageLongPress: (path: String, currentAlt: String) -> Unit = { _, _ -> }
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Footnote ref → def: clicking a superscript `[^N]` scrolls the matching
    // `[^N]: …` definition row into view. If no matching def exists in the
    // current preview, the click is a silent no-op (better than crashing or
    // jumping to a wrong section).
    val onFootnoteRefClick: (String) -> Unit = onFootnoteRefClick@{ label ->
        val targetIndex = findFootnoteDefIndex(lines, label)
        if (targetIndex < 0) return@onFootnoteRefClick
        scope.launch {
            listState.animateScrollToItem(targetIndex)
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding
    ) {
        items(lines) { line ->
            PreviewLineRenderer(
                line = line,
                onWikilinkClick = onWikilinkClick,
                onImageLongPress = onImageLongPress,
                onFootnoteRefClick = onFootnoteRefClick
            )
        }
    }
}

@Composable
fun PreviewLineRenderer(
    line: PreviewLine,
    onWikilinkClick: (String) -> Unit = {},
    onImageLongPress: (path: String, currentAlt: String) -> Unit = { _, _ -> },
    onFootnoteRefClick: (String) -> Unit = {}
) {
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
        PreviewLineType.BULLET -> InlineMarkdownText(
            line = line,
            leadingMarker = "• ",
            onWikilinkClick = onWikilinkClick,
            onFootnoteRefClick = onFootnoteRefClick
        )
        PreviewLineType.CHECKBOX_DONE -> InlineMarkdownText(
            line = line,
            leadingMarker = "☑ ",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            onWikilinkClick = onWikilinkClick,
            onFootnoteRefClick = onFootnoteRefClick
        )
        PreviewLineType.CHECKBOX_TODO -> InlineMarkdownText(
            line = line,
            leadingMarker = "☐ ",
            onWikilinkClick = onWikilinkClick,
            onFootnoteRefClick = onFootnoteRefClick
        )
        PreviewLineType.CODE_BLOCK -> MarkdownCodeBlock(line.text, line.extra)
        PreviewLineType.BODY -> InlineMarkdownText(
            line = line,
            onWikilinkClick = onWikilinkClick,
            onFootnoteRefClick = onFootnoteRefClick
        )
        PreviewLineType.BLOCKQUOTE -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                InlineMarkdownText(
                    line = line,
                    onWikilinkClick = onWikilinkClick,
                    onFootnoteRefClick = onFootnoteRefClick
                )
                HorizontalDivider(
                    modifier = Modifier.padding(top = 4.dp),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
        PreviewLineType.CALLOUT -> CalloutBox(line, onFootnoteRefClick = onFootnoteRefClick)
        PreviewLineType.FRONTMATTER -> FrontmatterBlock(line.text)
        PreviewLineType.FOOTNOTE_DEF -> FootnoteDefRow(line)
        PreviewLineType.IMAGE -> AttachmentImage(line, onLongPress = onImageLongPress)
        PreviewLineType.TABLE -> line.tableData?.let { MarkdownTable(it) }
        PreviewLineType.ORDERED_LIST -> InlineMarkdownText(
            line = line,
            leadingMarker = "${line.extra ?: "1"}. ",
            onWikilinkClick = onWikilinkClick,
            onFootnoteRefClick = onFootnoteRefClick
        )
        PreviewLineType.HORIZONTAL_RULE -> HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        PreviewLineType.EMPTY -> Spacer(Modifier.height(8.dp))
    }
}

@Composable
internal fun InlineMarkdownText(
    line: PreviewLine,
    onWikilinkClick: (String) -> Unit = {},
    onFootnoteRefClick: (String) -> Unit = {},
    /**
     * Plain prefix drawn before the inline content — used by list rows to put
     * the bullet / number / checkbox glyph ahead of the formatted text. Kept as
     * part of the same [androidx.compose.ui.text.AnnotatedString] (rather than a
     * separate Text) so wrapping and click offsets stay aligned.
     */
    leadingMarker: String = "",
    color: Color = MaterialTheme.colorScheme.onBackground
) {
    val annotated = buildAnnotatedString {
        if (leadingMarker.isNotEmpty()) append(leadingMarker)
        // Some line types (and the legacy hand-rolled parser) can leave segments
        // empty even when text is present — fall back to the raw text so we never
        // silently drop content.
        val segments = line.segments.ifEmpty {
            listOf(PreviewInlineSegment(line.text, PreviewInlineType.TEXT))
        }
        segments.forEach { segment ->
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
                PreviewInlineType.FOOTNOTE_REF -> {
                    pushStringAnnotation(tag = FOOTNOTE_REF_TAG, annotation = segment.text)
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            baselineShift = BaselineShift.Superscript
                        )
                    ) {
                        append(segment.text)
                    }
                    pop()
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
                PreviewInlineType.LINK -> {
                    val href = segment.href.orEmpty()
                    pushStringAnnotation(tag = LINK_TAG, annotation = href)
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
    val context = LocalContext.current
    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge.copy(color = color),
        modifier = Modifier.padding(vertical = 2.dp),
        onClick = { offset ->
            annotated.getStringAnnotations(WIKILINK_TAG, offset, offset).firstOrNull()
                ?.let { onWikilinkClick(it.item); return@ClickableText }
            annotated.getStringAnnotations(FOOTNOTE_REF_TAG, offset, offset).firstOrNull()
                ?.let { onFootnoteRefClick(it.item); return@ClickableText }
            annotated.getStringAnnotations(LINK_TAG, offset, offset).firstOrNull()
                ?.let { ann ->
                    openExternalLink(context, ann.item)
                }
        }
    )
}

private const val WIKILINK_TAG = "wikilink"
private const val LINK_TAG = "link"
private const val FOOTNOTE_REF_TAG = "footnote_ref"

/**
 * Returns the index of the first `FOOTNOTE_DEF` line whose label matches [label],
 * or -1 if none. Lifted out of [MarkdownPreviewList] so it can be unit-tested.
 */
internal fun findFootnoteDefIndex(lines: List<PreviewLine>, label: String): Int =
    lines.indexOfFirst { line ->
        line.type == PreviewLineType.FOOTNOTE_DEF && line.extra == label
    }

/**
 * Launch the system browser (or whatever else handles the URI scheme) for an
 * external markdown link. Silent no-op for empty or malformed targets so
 * tapping never crashes — there's no INTERNET permission in our app, so we're
 * just handing the URI off to another app via the standard intent.
 */
private fun openExternalLink(context: android.content.Context, href: String) {
    if (href.isBlank()) return
    val normalized = if (
        href.startsWith("http://") || href.startsWith("https://") ||
        href.startsWith("mailto:") || href.startsWith("tel:")
    ) {
        href
    } else if (href.contains("://")) {
        href // any other explicit scheme — leave to the system
    } else {
        // Bare hostname like "markleaf.app" — assume https.
        "https://$href"
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(normalized))
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

@Composable
private fun CalloutBox(
    line: PreviewLine,
    onFootnoteRefClick: (String) -> Unit = {}
) {
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
                        ),
                        onFootnoteRefClick = onFootnoteRefClick
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
private fun MarkdownTable(data: TableData) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(scheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        // Header row
        TableRow(
            cells = data.headers,
            alignments = data.alignments,
            background = scheme.surfaceVariant,
            textColor = scheme.onSurface,
            bold = true
        )
        // Body rows — divider between each, slight zebra-stripe via alpha
        data.rows.forEachIndexed { index, row ->
            HorizontalDivider(
                color = scheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 1.dp
            )
            TableRow(
                cells = row,
                alignments = data.alignments,
                background = if (index % 2 == 0) {
                    androidx.compose.ui.graphics.Color.Transparent
                } else {
                    scheme.surfaceVariant.copy(alpha = 0.3f)
                },
                textColor = scheme.onBackground,
                bold = false
            )
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    alignments: List<TableAlignment>,
    background: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    bold: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(vertical = 6.dp)
    ) {
        cells.forEachIndexed { col, cell ->
            val alignment = alignments.getOrElse(col) { TableAlignment.LEFT }
            Text(
                text = cell,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = when (alignment) {
                    TableAlignment.LEFT -> androidx.compose.ui.text.style.TextAlign.Start
                    TableAlignment.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
                    TableAlignment.RIGHT -> androidx.compose.ui.text.style.TextAlign.End
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            )
        }
    }
}

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AttachmentImage(
    line: PreviewLine,
    onLongPress: (path: String, currentAlt: String) -> Unit
) {
    val context = LocalContext.current
    val destination = line.extra.orEmpty()
    val resolved = remember(destination) {
        AttachmentManager.resolveFile(context, destination)
    }
    if (resolved != null) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(resolved).build(),
            contentDescription = line.text.ifEmpty { destination },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = {},
                    onLongClick = { onLongPress(destination, line.text) }
                )
        )
    } else {
        Text(
            text = "![${line.text}]($destination)",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
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
    val scheme = MaterialTheme.colorScheme
    // Token-type → color. Reuses the theme palette so dark / light / Material You
    // all coordinate. Unknown languages fall through to TEXT (onSurfaceVariant).
    val annotated = remember(text, language, scheme) {
        val tokens = SyntaxHighlighter.tokenize(text, language)
        buildAnnotatedString {
            tokens.forEach { token ->
                val color = when (token.type) {
                    SyntaxHighlighter.TokenType.KEYWORD -> scheme.primary
                    SyntaxHighlighter.TokenType.STRING -> scheme.secondary
                    SyntaxHighlighter.TokenType.NUMBER -> scheme.tertiary
                    SyntaxHighlighter.TokenType.COMMENT -> scheme.onSurfaceVariant.copy(alpha = 0.6f)
                    SyntaxHighlighter.TokenType.FUNCTION -> scheme.primary
                    SyntaxHighlighter.TokenType.TYPE -> scheme.tertiary
                    SyntaxHighlighter.TokenType.PUNCTUATION -> scheme.onSurfaceVariant
                    SyntaxHighlighter.TokenType.TEXT -> scheme.onSurfaceVariant
                }
                val italic = token.type == SyntaxHighlighter.TokenType.COMMENT
                withStyle(
                    SpanStyle(
                        color = color,
                        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal
                    )
                ) {
                    append(token.text)
                }
            }
        }
    }
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
            text = annotated,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
