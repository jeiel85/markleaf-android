package com.markleaf.notes.core.markdown.preview

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.markleaf.notes.util.AttachmentManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markleaf.notes.R
import com.markleaf.notes.core.markdown.CalloutKind
import com.markleaf.notes.core.markdown.CommonMarkPreviewAdapter
import com.markleaf.notes.core.markdown.PreviewInlineSegment
import com.markleaf.notes.core.markdown.PreviewInlineType
import com.markleaf.notes.core.markdown.PreviewLine
import com.markleaf.notes.core.markdown.PreviewLineType
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import com.markleaf.notes.core.markdown.TableAlignment
import com.markleaf.notes.core.markdown.TableData
import com.markleaf.notes.core.markdown.syntax.SyntaxHighlighter
import com.markleaf.notes.util.LocalMarkdownLink
import kotlin.math.min

// Vertical rhythm of the rendered preview (#340).
//
// Before this, every block carried an ad-hoc 2/4/6/8dp padding and a blank line
// in the source produced no row at all, so two paragraphs sat 4dp apart while
// the lines *inside* one paragraph were 26sp apart — the gap between paragraphs
// was smaller than the gap between two lines of the same paragraph. These
// values put the ordering back: line < list row < paragraph < heading.
private val ListRowSpacing = 3.dp
// 8dp rather than something smaller because the body line height is a generous
// 26sp: with less, the space between two paragraphs stays close enough to the
// space between two lines of one paragraph that the break does not read.
private val ParagraphSpacing = 8.dp

// Indentation per nesting level for rows inside a list (#339 gives them their
// depth). Shallower than the outline sheet's 24dp per level, because a list can
// nest far deeper than the six heading levels that sheet ever shows, and the
// cap keeps a pathological note from squeezing its own text off the screen.
private val ListIndentPerLevel = 16.dp
private const val MaxIndentDepth = 6

/**
 * A heading's top margin separates it from the block above it. The first block
 * in the preview has nothing above it, so that margin would read as dead space
 * at the top of every note instead — and most notes open with a heading.
 */
private fun headingTop(isFirstBlock: Boolean, margin: Dp): Dp =
    if (isFirstBlock) 0.dp else margin

/**
 * A list whose items are separated by blank lines in the source is *loose* in
 * CommonMark's sense — the author spaced it out deliberately — so its rows get
 * the paragraph rhythm rather than the compact one. Without this the blank
 * lines disappear and a loose list is indistinguishable from a tight one.
 */
private fun listRowSpacing(line: PreviewLine): Dp =
    if (line.looseList) ParagraphSpacing else ListRowSpacing

/**
 * Renders a list of [PreviewLine]s as a scrollable Markdown preview.
 * Pulled out of [com.markleaf.notes.feature.editor.EditorScreen] so the same
 * rendering can be exercised by snapshot tests independently of the editor
 * scaffolding.
 *
 * @param onWikilinkClick called when the user taps a `[[Title]]` segment.
 *   The argument is the target text inside the brackets (already trimmed).
 *   Default is a no-op so existing snapshot tests don't need to wire navigation.
 * @param fontScale multiplier for the rendered type scale (#346). Growing the
 *   text grows the checkbox glyphs and the link tap targets with it — the
 *   whole point of the setting. At the default 1f the lines list renders
 *   byte-identically to before.
 */
@Composable
fun MarkdownPreviewList(
    lines: List<PreviewLine>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    listState: LazyListState = rememberLazyListState(),
    onWikilinkClick: (String) -> Unit = {},
    onLocalLinkClick: ((String) -> Unit)? = null,
    onImageLongPress: (path: String, currentAlt: String) -> Unit = { _, _ -> },
    onToggleTask: ((sourceLine: Int) -> Unit)? = null,
    fontScale: Float = 1f,
    /**
     * A `<details>` section's [PreviewLine.collapsibleId] is in this set when
     * the user has toggled it *away* from its parsed default (#403) — not the
     * set of currently-collapsed ids. Tracking the delta instead of the
     * absolute state means a section with no `open` attribute reads as
     * collapsed the moment it is typed, with nothing needing to seed the set
     * up front; see [isSectionExpanded].
     */
    toggledSectionIds: Set<Int> = emptySet(),
    onToggleSection: (Int) -> Unit = {},
    /**
     * Find-in-note in preview (#417): every occurrence of this is highlighted,
     * and [currentFindMatch] — whose index is into [lines], not the visible
     * rows — is drawn more strongly. Empty means find is closed.
     */
    findQuery: String = "",
    currentFindMatch: PreviewFindMatch? = null
) {
    val scope = rememberCoroutineScope()
    // The scale rides the PreviewLine rather than a CompositionLocal:
    // material3's LocalTypography is internal, and threading it through would
    // also resize the pieces of this file that are NOT body text (callout
    // labels, code blocks, frontmatter, table cells), which the setting does
    // not promise. PreviewLine is this preview's own model, so the scale can
    // live there with no public API change.
    val scaledLines = remember(lines, fontScale) {
        if (fontScale == 1f) lines else lines.map { it.copy(fontScale = fontScale) }
    }
    // Rows inside a currently-collapsed `<details>` section (#403) are left
    // out of the LazyColumn entirely rather than rendered-and-hidden, so a
    // long collapsed section costs nothing while it stays closed. Recomputed
    // from scratch on every toggle rather than incrementally — cheap even for
    // a large note, since it is one filter pass over rows already in memory.
    val visibleIndices = remember(scaledLines, toggledSectionIds) {
        visiblePreviewLineIndices(scaledLines, toggledSectionIds)
    }
    val visibleLines = remember(scaledLines, visibleIndices) {
        visibleIndices.map { scaledLines[it] }
    }
    // Footnote ref → def: clicking a superscript `[^N]` scrolls the matching
    // `[^N]: …` definition row into view. If no matching def exists in the
    // current preview, the click is a silent no-op (better than crashing or
    // jumping to a wrong section). Looked up in visibleLines, not lines: the
    // index this scrolls to is a LazyColumn item index, and the list actually
    // laid out there is the filtered one.
    val onFootnoteRefClick: (String) -> Unit = onFootnoteRefClick@{ label ->
        val targetIndex = findFootnoteDefIndex(visibleLines, label)
        if (targetIndex < 0) return@onFootnoteRefClick
        scope.launch {
            listState.animateScrollToItem(targetIndex)
        }
    }
    // Preview text is selectable (#386). Before this, copying a sentence out of
    // a rendered note meant switching back to the editor and bringing the
    // keyboard up for it. A long press starts a selection; the taps the preview
    // already handles — links, wikilinks, checkbox markers — are untouched,
    // since none of them begin with a long press.
    //
    // A long press on a *link* is the one collision, since that copies the
    // address: the same press is a long press to selection as well, and
    // selection cannot be called off once it starts — only restarted.
    // Rebuilding the container under a new epoch is that restart. It is also
    // why [linkPressGestures] consumes nothing until it is certain rather than
    // claiming the press early: a consumed pointer reads as a cancelled
    // gesture to the scrolling container too, which left a drag that began on
    // a link unable to scroll at all. `listState` lives outside the key, so the
    // scroll position survives the rebuild.
    //
    // The lazy-list caveat that comes with selection: it only spans the rows
    // that are currently composed, so dragging far past what the viewport holds
    // drops the part that scrolled away. That is how selection behaves in any
    // LazyColumn, and the alternative — composing the whole note at once — is
    // the cost this preview exists to avoid.
    var selectionEpoch by remember { mutableIntStateOf(0) }
    val resetSelection: () -> Unit = remember { { selectionEpoch++ } }
    CompositionLocalProvider(
        LocalPreviewSelectionReset provides resetSelection,
        LocalNoteLinkHandler provides onLocalLinkClick
    ) {
        key(selectionEpoch) {
            SelectionContainer(modifier = modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = contentPadding
                ) {
                    itemsIndexed(visibleLines) { index, line ->
                        val lineIndex = visibleIndices[index]
                        val highlight = findQuery.takeIf { it.isNotEmpty() }?.let { query ->
                            PreviewFindHighlight(
                                query = query,
                                current = currentFindMatch
                                    ?.takeIf { it.lineIndex == lineIndex }
                                    ?.occurrence
                            )
                        }
                        CompositionLocalProvider(LocalPreviewFindHighlight provides highlight) {
                            PreviewLineRenderer(
                                line = line,
                                // Only a block with something above it needs
                                // separating from it; see [headingTop].
                                isFirstBlock = index == 0,
                                onWikilinkClick = onWikilinkClick,
                                onImageLongPress = onImageLongPress,
                                onFootnoteRefClick = onFootnoteRefClick,
                                onToggleTask = onToggleTask,
                                isSectionToggled = { id -> id in toggledSectionIds },
                                onToggleSection = onToggleSection
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreviewLineRenderer(
    line: PreviewLine,
    isFirstBlock: Boolean = false,
    onWikilinkClick: (String) -> Unit = {},
    onImageLongPress: (path: String, currentAlt: String) -> Unit = { _, _ -> },
    onFootnoteRefClick: (String) -> Unit = {},
    onToggleTask: ((sourceLine: Int) -> Unit)? = null,
    /** Whether [PreviewLine.collapsibleId] has been toggled away from its parsed default (#403). */
    isSectionToggled: (Int) -> Boolean = { false },
    onToggleSection: (Int) -> Unit = {}
) {
    val indent = ListIndentPerLevel * min(line.depth, MaxIndentDepth)
    if (indent == 0.dp) {
        // The common case pays for no extra layout node.
        PreviewLineContent(
            line = line,
            isFirstBlock = isFirstBlock,
            onWikilinkClick = onWikilinkClick,
            onImageLongPress = onImageLongPress,
            onFootnoteRefClick = onFootnoteRefClick,
            onToggleTask = onToggleTask,
            isSectionToggled = isSectionToggled,
            onToggleSection = onToggleSection
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = indent)
        ) {
            PreviewLineContent(
                line = line,
                isFirstBlock = isFirstBlock,
                onWikilinkClick = onWikilinkClick,
                onImageLongPress = onImageLongPress,
                onFootnoteRefClick = onFootnoteRefClick,
                onToggleTask = onToggleTask,
                isSectionToggled = isSectionToggled,
                onToggleSection = onToggleSection
            )
        }
    }
}

@Composable
private fun PreviewLineContent(
    line: PreviewLine,
    isFirstBlock: Boolean,
    onWikilinkClick: (String) -> Unit,
    onImageLongPress: (path: String, currentAlt: String) -> Unit,
    onFootnoteRefClick: (String) -> Unit,
    onToggleTask: ((sourceLine: Int) -> Unit)?,
    isSectionToggled: (Int) -> Boolean = { false },
    onToggleSection: (Int) -> Unit = {}
) {
    // Only a row that knows its own source line can be toggled; see
    // PreviewLine.sourceLine for why we refuse to guess (#219).
    val toggle: (() -> Unit)? = line.sourceLine?.let { source ->
        onToggleTask?.let { handler -> { handler(source) } }
    }
    val scale = line.fontScale
    val scaled = scale != 1f
    when (line.type) {
        PreviewLineType.H1 -> Text(
            text = findHighlighted(line.text),
            style = if (scaled) MaterialTheme.typography.headlineMedium.scaledBy(scale)
            else MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = headingTop(isFirstBlock, 24.dp), bottom = 8.dp)
        )
        PreviewLineType.H2 -> Text(
            text = findHighlighted(line.text),
            style = if (scaled) MaterialTheme.typography.headlineSmall.scaledBy(scale)
            else MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = headingTop(isFirstBlock, 20.dp), bottom = 6.dp)
        )
        PreviewLineType.H3 -> Text(
            text = findHighlighted(line.text),
            style = if (scaled) MaterialTheme.typography.titleLarge.scaledBy(scale)
            else MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = headingTop(isFirstBlock, 16.dp), bottom = 4.dp)
        )
        // H4–H6 continue down the same type scale rather than getting a
        // treatment of their own. The last two also drop to the muted colour:
        // by that depth the heading is closer to a label than a section title,
        // and six visually distinct heading styles in one note is noise.
        PreviewLineType.H4 -> Text(
            text = findHighlighted(line.text),
            style = if (scaled) MaterialTheme.typography.titleMedium.scaledBy(scale)
            else MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = headingTop(isFirstBlock, 12.dp), bottom = 4.dp)
        )
        PreviewLineType.H5 -> Text(
            text = findHighlighted(line.text),
            style = if (scaled) MaterialTheme.typography.titleSmall.scaledBy(scale)
            else MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = headingTop(isFirstBlock, 10.dp), bottom = 2.dp)
        )
        PreviewLineType.H6 -> Text(
            text = findHighlighted(line.text),
            style = if (scaled) MaterialTheme.typography.labelLarge.scaledBy(scale)
            else MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = headingTop(isFirstBlock, 8.dp), bottom = 2.dp)
        )
        PreviewLineType.BULLET -> InlineMarkdownText(
            line = line,
            leadingMarker = "• ",
            verticalPadding = listRowSpacing(line),
            onWikilinkClick = onWikilinkClick,
            onFootnoteRefClick = onFootnoteRefClick
        )
        PreviewLineType.CHECKBOX_DONE -> InlineMarkdownText(
            line = line,
            leadingMarker = "☑ ",
            verticalPadding = listRowSpacing(line),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            onWikilinkClick = onWikilinkClick,
            onFootnoteRefClick = onFootnoteRefClick,
            onMarkerClick = toggle
        )
        PreviewLineType.CHECKBOX_TODO -> InlineMarkdownText(
            line = line,
            leadingMarker = "☐ ",
            verticalPadding = listRowSpacing(line),
            onWikilinkClick = onWikilinkClick,
            onFootnoteRefClick = onFootnoteRefClick,
            onMarkerClick = toggle
        )
        PreviewLineType.CODE_BLOCK -> MarkdownCodeBlock(line.text, line.extra)
        PreviewLineType.BODY -> InlineMarkdownText(
            line = line,
            verticalPadding = ParagraphSpacing,
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
        PreviewLineType.TABLE -> line.tableData?.let {
            MarkdownTable(
                data = it,
                onWikilinkClick = onWikilinkClick,
                onFootnoteRefClick = onFootnoteRefClick
            )
        }
        PreviewLineType.ORDERED_LIST -> InlineMarkdownText(
            line = line,
            leadingMarker = "${line.extra ?: "1"}. ",
            verticalPadding = listRowSpacing(line),
            onWikilinkClick = onWikilinkClick,
            onFootnoteRefClick = onFootnoteRefClick
        )
        PreviewLineType.HORIZONTAL_RULE -> HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        PreviewLineType.COLLAPSIBLE_SUMMARY -> {
            val id = line.collapsibleId
            val defaultOpen = line.extra == CommonMarkPreviewAdapter.OPEN_MARKER
            val expanded = if (id != null && isSectionToggled(id)) !defaultOpen else defaultOpen
            // The default label for an empty <summary> is not the note's text, so
            // find neither counts nor highlights it (#417).
            val highlight = LocalPreviewFindHighlight.current.takeIf { line.text.isNotEmpty() }
            CompositionLocalProvider(LocalPreviewFindHighlight provides highlight) {
                CollapsibleSummaryRow(
                    text = line.text.ifEmpty { stringResource(R.string.collapsible_section_default_summary) },
                    expanded = expanded,
                    onClick = { id?.let(onToggleSection) }
                )
            }
        }
        // Internal bookkeeping only -- stripped by CommonMarkPreviewAdapter
        // .applyCollapsibleRanges before a PreviewLine list ever reaches this
        // renderer. See PreviewLineType.COLLAPSIBLE_END's own doc comment.
        PreviewLineType.COLLAPSIBLE_END -> Unit
    }
}

@Composable
private fun CollapsibleSummaryRow(text: String, expanded: Boolean, onClick: () -> Unit) {
    // The ▾/▸ glyph is a visual-only cue; without this a screen reader
    // announces "double tap to activate" with no way to tell whether
    // activating it will open or close the section (a Codex review finding).
    val stateLabel = stringResource(if (expanded) R.string.expanded else R.string.collapsed)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .semantics { stateDescription = stateLabel }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // A plain glyph rather than a Material icon, matching how this file
        // already marks a checkbox (☑/☐) and a callout (ℹ/💡/…) instead of
        // reaching for the icon library.
        Text(
            text = if (expanded) "▾" else "▸",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = findHighlighted(text),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
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
    /**
     * Space above and below the row. Defaults to the tighter list rhythm; a
     * standalone paragraph passes [ParagraphSpacing] so prose breathes without
     * pulling list items apart from each other.
     */
    verticalPadding: Dp = ListRowSpacing,
    color: Color = MaterialTheme.colorScheme.onBackground,
    /**
     * When set, [leadingMarker] becomes a clickable region. Carried inside the
     * same AnnotatedString as the text rather than split into its own composable
     * so the row lays out exactly as before — the checklist goldens must not
     * move for a change that only adds an interaction (#219).
     */
    onMarkerClick: (() -> Unit)? = null
) {
    // Some line types (and the legacy hand-rolled parser) can leave segments
    // empty even when text is present — fall back to the raw text so we never
    // silently drop content.
    val segments = line.segments.ifEmpty {
        listOf(PreviewInlineSegment(line.text, PreviewInlineType.TEXT))
    }
    val annotated = inlineAnnotatedString(
        segments = segments,
        leadingMarker = leadingMarker,
        onWikilinkClick = onWikilinkClick,
        onFootnoteRefClick = onFootnoteRefClick,
        onMarkerClick = onMarkerClick,
        fontScale = line.fontScale
    ).withFindHighlights(LocalPreviewFindHighlight.current, searchStart = leadingMarker.length)
    val baseStyle = MaterialTheme.typography.bodyLarge
    val layout = remember { TextLayoutHolder() }
    // Links are now embedded as LinkAnnotations in `annotated`, so a plain Text
    // handles styling, clicks, and accessibility — no offset-mapped onClick.
    // The layout result is kept because copying a link's address on long press
    // has to map a touch back to a text offset (#386).
    Text(
        text = annotated,
        style = (if (line.fontScale == 1f) baseStyle else baseStyle.scaledBy(line.fontScale))
            .copy(color = color),
        onTextLayout = { layout.value = it },
        modifier = Modifier
            .padding(vertical = verticalPadding)
            .linkPressGestures(annotated, layout)
    )
}

/**
 * Returns this style with its font size and line height multiplied by [scale]
 * (#346). Only called when the row's scale is not 1f, so the default rendering
 * keeps using the theme styles untouched.
 */
private fun androidx.compose.ui.text.TextStyle.scaledBy(
    scale: Float
): androidx.compose.ui.text.TextStyle =
    copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale)

/**
 * Builds the [AnnotatedString] for a list of [PreviewInlineSegment]s, embedding
 * clickable [LinkAnnotation]s for links, wikilinks, and footnote refs. Shared by
 * [InlineMarkdownText] and the table-cell renderer so links behave the same
 * inside tables as anywhere else (#197).
 */
@Composable
private fun inlineAnnotatedString(
    segments: List<PreviewInlineSegment>,
    leadingMarker: String = "",
    onWikilinkClick: (String) -> Unit = {},
    onFootnoteRefClick: (String) -> Unit = {},
    onMarkerClick: (() -> Unit)? = null,
    /** Scales the only absolute size in here — the footnote ref (#346). */
    fontScale: Float = 1f
): AnnotatedString {
    // Captured by the LinkAnnotation click listeners built below, so it must be
    // resolved before buildAnnotatedString rather than at the Text call site.
    val context = LocalContext.current
    val onLocalLinkClick = LocalNoteLinkHandler.current
    return buildAnnotatedString {
        if (leadingMarker.isNotEmpty()) {
            if (onMarkerClick == null) {
                append(leadingMarker)
            } else {
                // No styles: the checkbox already looks like a control, and
                // link colouring here would read as a hyperlink and change
                // every checklist golden.
                withLink(
                    LinkAnnotation.Clickable(
                        tag = TASK_MARKER_TAG,
                        linkInteractionListener = { onMarkerClick() }
                    )
                ) {
                    append(leadingMarker)
                }
            }
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
                    val label = segment.text
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = FOOTNOTE_REF_TAG,
                            styles = TextLinkStyles(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    // Superscript refs scale with the text-size
                                    // setting (#346); 11.sp is the theme's own
                                    // labelSmall, so the default is unchanged.
                                    fontSize = 11.sp * fontScale,
                                    baselineShift = BaselineShift.Superscript
                                )
                            ),
                            linkInteractionListener = { onFootnoteRefClick(label) }
                        )
                    ) {
                        append(segment.text)
                    }
                }
                PreviewInlineType.WIKILINK -> {
                    val target = segment.href ?: segment.text
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = WIKILINK_TAG,
                            styles = TextLinkStyles(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline
                                )
                            ),
                            linkInteractionListener = { onWikilinkClick(target) }
                        )
                    ) {
                        append(segment.text)
                    }
                }
                PreviewInlineType.LINK -> {
                    val href = segment.href.orEmpty()
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = LINK_TAG,
                            styles = TextLinkStyles(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline
                                )
                            ),
                            linkInteractionListener = {
                                val localName = LocalMarkdownLink.fileName(href)
                                if (localName != null && onLocalLinkClick != null) {
                                    onLocalLinkClick(localName)
                                } else {
                                    openExternalLink(context, href)
                                }
                            }
                        )
                    ) {
                        if (href.isBlank()) {
                            append(segment.text)
                        } else {
                            // Inside the LinkAnnotation the address is reachable
                            // only from the click listener's closure, and the
                            // long press that copies it (#386) has to find it by
                            // text offset instead — so it also rides along as a
                            // plain string annotation.
                            pushStringAnnotation(tag = LINK_HREF_TAG, annotation = href)
                            append(segment.text)
                            pop()
                        }
                    }
                }
            }
        }
    }
}

private const val WIKILINK_TAG = "wikilink"
private const val LINK_TAG = "link"

/**
 * Carries a link's address alongside its [LinkAnnotation] so [hrefAt] can
 * resolve the address under a long press (#386).
 */
private const val LINK_HREF_TAG = "link_href"
private val LocalNoteLinkHandler = compositionLocalOf<((String) -> Unit)?> { null }

/**
 * The find highlight for the row being drawn (#417), or null while find is
 * closed. Provided per LazyColumn item rather than passed down, because the
 * text it applies to sits several renderers deep (a callout's body line, a
 * footnote definition) and none of those signatures otherwise need to know.
 */
private val LocalPreviewFindHighlight = compositionLocalOf<PreviewFindHighlight?> { null }

/** [text] with the current row's find highlight applied; see [withFindHighlights]. */
@Composable
private fun findHighlighted(text: String): AnnotatedString =
    AnnotatedString(text).withFindHighlights(LocalPreviewFindHighlight.current)

/**
 * Marks [highlight]'s occurrences in this text: a quiet background for every
 * match and the primary colour for the current one, the way the editor's
 * selection marks its current match. Returns the text untouched while find is
 * closed, so a preview without a query renders exactly as before.
 */
@Composable
private fun AnnotatedString.withFindHighlights(
    highlight: PreviewFindHighlight?,
    searchStart: Int = 0
): AnnotatedString {
    if (highlight == null) return this
    val scheme = MaterialTheme.colorScheme
    return highlightFindMatches(
        text = this,
        highlight = highlight,
        matchStyle = SpanStyle(background = scheme.tertiaryContainer, color = scheme.onTertiaryContainer),
        currentStyle = SpanStyle(background = scheme.primary, color = scheme.onPrimary),
        searchStart = searchStart
    )
}

/**
 * Lets a link long press reach the [SelectionContainer] wrapping the whole
 * preview, which is several composables above the row that handled the press.
 * Passing it down as a parameter would thread one callback through five
 * signatures that have no other reason to know about selection.
 */
private val LocalPreviewSelectionReset = compositionLocalOf<() -> Unit> { {} }

/**
 * Holds the latest [TextLayoutResult] for a preview row without making it
 * state. The long-press gesture reads it only once a press arrives, and a row
 * that recomposed on every layout pass would be a real cost in a note that
 * renders hundreds of them.
 */
private class TextLayoutHolder {
    var value: TextLayoutResult? = null
}

/**
 * Owns the press on a link: a tap opens it, a long press copies its address to
 * the clipboard (#386). The preview shows a link's label and never its target,
 * so until now the address was reachable only by leaving preview and reading
 * the raw Markdown.
 *
 * Both halves have to live here together. The press is claimed on the Initial
 * pass, before the two handlers that sit *inside* this Text — the selection
 * detector and the LinkAnnotation's own clickable — get to look at it, because
 * neither can be called off later: a long press that only raced them ended up
 * copying the address while the browser opened over the note and selection
 * handles rose over the link. Claiming it means the tap has to be answered here
 * as well, since the LinkAnnotation will no longer see one. Its listener stays
 * for the accessibility path, which never goes through this gesture.
 *
 * Away from a link nothing is consumed, so ordinary text keeps the selection
 * this preview now offers, and a drag that starts on a link still scrolls: the
 * scrolling container consumes the movement, which reads here as a cancelled
 * press.
 */
@Composable
private fun Modifier.linkPressGestures(
    annotated: AnnotatedString,
    layout: TextLayoutHolder
): Modifier {
    // Most rows carry no link; those pay for no pointer handler at all.
    if (annotated.getStringAnnotations(LINK_HREF_TAG, 0, annotated.length).isEmpty()) {
        return this
    }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.preview_link_address_copied)
    val onAddressCopied = LocalPreviewSelectionReset.current
    return this.pointerInput(annotated) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial
            )
            val href = hrefAt(annotated, layout.value, down.position)
                ?: return@awaitEachGesture
            // Nothing is consumed until the long press is certain. Consuming
            // sooner reaches further than intended: the scrolling container
            // reads a consumed pointer as its gesture being called off, so an
            // early claim here left a drag that started on a link unable to
            // scroll the preview at all.
            var longPressed = false
            try {
                // The platform's own threshold, not a shortened one. Deciding
                // early would make a slow tap — released after 400ms, say, but
                // before Android calls it a long press — copy the address
                // instead of opening the link.
                withTimeout(viewConfiguration.longPressTimeoutMillis) {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: return@withTimeout
                        // Lifted before the timeout: a tap, which belongs to
                        // the LinkAnnotation's own click listener.
                        if (!change.pressed) return@withTimeout
                        // A finger that has travelled is no longer resting on
                        // the link, whether or not anything else claimed the
                        // movement — sliding off a link sideways is not a
                        // vertical scroll, so nothing would consume it, and
                        // without this the address would still be copied when
                        // the timeout came round.
                        if ((change.position - down.position).getDistance() >
                            viewConfiguration.touchSlop
                        ) {
                            return@withTimeout
                        }
                        // Someone else claiming the movement means the press
                        // became a scroll. Only movement counts: the press
                        // itself arrives here already consumed, because the
                        // link's tap detector — inside this Text, so ahead of
                        // this node on the Main pass — consumes every down it
                        // sees.
                        if (change.positionChanged() && change.isConsumed) {
                            return@withTimeout
                        }
                    }
                }
            } catch (_: PointerEventTimeoutCancellationException) {
                longPressed = true
            }
            if (!longPressed) return@awaitEachGesture
            clipboard.setText(AnnotatedString(href))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                // Android 13 and up shows its own clipboard confirmation, and a
                // toast on top of that says the same thing twice.
                Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
            }
            onAddressCopied()
            // From here the press is this gesture's, and it is claimed on the
            // Initial pass: the LinkAnnotation's own click handling lives
            // *inside* this Text, so on the Main pass it would see the release
            // first and open the link on the way out of a press meant to copy.
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }
}

/**
 * The link address under [position] in the laid-out text, or null when the
 * point falls outside the text or onto a stretch that is not a link.
 */
private fun hrefAt(
    annotated: AnnotatedString,
    layout: TextLayoutResult?,
    position: Offset
): String? {
    val result = layout ?: return null
    if (position.y < 0f || position.y > result.size.height.toFloat()) return null
    val line = result.getLineForVerticalPosition(position.y)
    // Without this, a press past the end of a short line would be pulled back
    // onto that line's last character by getOffsetForPosition.
    if (position.x < result.getLineLeft(line) || position.x > result.getLineRight(line)) {
        return null
    }
    // getOffsetForPosition returns a *cursor* offset, rounding to whichever side
    // of the glyph is nearer, so the character actually under the finger is
    // whichever of the two candidates has the press inside its box.
    val cursor = result.getOffsetForPosition(position)
    val index = intArrayOf(cursor, cursor - 1).firstOrNull { candidate ->
        candidate >= 0 && candidate < annotated.length &&
            result.getBoundingBox(candidate).let {
                position.x >= it.left && position.x <= it.right
            }
    } ?: return null
    return annotated.getStringAnnotations(LINK_HREF_TAG, index, index).firstOrNull()?.item
}
private const val FOOTNOTE_REF_TAG = "footnote_ref"
private const val TASK_MARKER_TAG = "task_marker"

/**
 * Returns the index of the first `FOOTNOTE_DEF` line whose label matches [label],
 * or -1 if none. Lifted out of [MarkdownPreviewList] so it can be unit-tested.
 */
internal fun findFootnoteDefIndex(lines: List<PreviewLine>, label: String): Int =
    lines.indexOfFirst { line ->
        line.type == PreviewLineType.FOOTNOTE_DEF && line.extra == label
    }

/**
 * Whether [id] (a [PreviewLine.collapsibleId]) is currently expanded (#403):
 * its parsed default (`<details open>` or not, carried on the summary row's
 * own [PreviewLine.extra]) flipped once if the user has toggled it. Tracking
 * the toggle as a delta rather than storing the absolute open/closed set is
 * what lets a `<details>` typed into the note default to collapsed with
 * nothing having to seed the set first.
 */
internal fun isSectionExpanded(
    defaultOpenById: Map<Int, Boolean>,
    toggledSectionIds: Set<Int>,
    id: Int
): Boolean {
    val defaultOpen = defaultOpenById[id] ?: true
    return if (id in toggledSectionIds) !defaultOpen else defaultOpen
}

/**
 * [lines] filtered down to what is actually visible with [toggledSectionIds]
 * applied — every row still shows unless one of its enclosing `<details>`
 * sections ([PreviewLine.collapsibleIds]) is currently collapsed. Used both
 * for what [MarkdownPreviewList] actually lays out in its `LazyColumn` and
 * for [extractHeadings]/[findFootnoteDefIndex] callers outside this file
 * (`EditorScreen`'s outline) to compute indices into the *same* list that is
 * actually on screen, since a heading inside a collapsed section is not a
 * jump target until it is expanded.
 */
internal fun visiblePreviewLines(lines: List<PreviewLine>, toggledSectionIds: Set<Int>): List<PreviewLine> =
    visiblePreviewLineIndices(lines, toggledSectionIds).map { lines[it] }

/**
 * The indices into [lines] of the rows [visiblePreviewLines] keeps, in order.
 * Find-in-note (#417) needs these to map a match in the full list onto the
 * LazyColumn item that draws it.
 */
internal fun visiblePreviewLineIndices(lines: List<PreviewLine>, toggledSectionIds: Set<Int>): List<Int> {
    val summaries = lines.filter { it.type == PreviewLineType.COLLAPSIBLE_SUMMARY }
    if (summaries.isEmpty()) return lines.indices.toList()
    val defaultOpenById = summaries.associate { (it.collapsibleId ?: -1) to (it.extra == CommonMarkPreviewAdapter.OPEN_MARKER) }
    return lines.indices.filter { index ->
        lines[index].collapsibleIds.all { id -> isSectionExpanded(defaultOpenById, toggledSectionIds, id) }
    }
}

/**
 * A heading entry for the table of contents: its [index] into the rendered
 * [PreviewLine] list (so the same `animateScrollToItem` used for footnote jumps
 * lands on the heading), the display [text], and the [level] (1..6).
 *
 * [sourceLine] is the 0-based line the heading occupies in the note's own text.
 * The rendered index can only ever scroll the preview; jumping while the user
 * is *editing* needs a caret position, which is what this resolves to (#215).
 * Null when the parser could not attribute a line — the jump is then dropped
 * rather than aimed at a guess.
 */
data class TocHeading(
    val index: Int,
    val text: String,
    val level: Int,
    val sourceLine: Int? = null
)

/**
 * Extracts the H1–H6 outline from rendered [lines] for the table of contents.
 * The index matches the LazyColumn item index, so tapping an entry can scroll the
 * preview to that heading. Lifted out of the UI so it can be unit-tested.
 */
internal fun extractHeadings(lines: List<PreviewLine>): List<TocHeading> =
    lines.mapIndexedNotNull { index, line ->
        val level = when (line.type) {
            PreviewLineType.H1 -> 1
            PreviewLineType.H2 -> 2
            PreviewLineType.H3 -> 3
            PreviewLineType.H4 -> 4
            PreviewLineType.H5 -> 5
            PreviewLineType.H6 -> 6
            else -> null
        }
        level?.let {
            TocHeading(
                index = index,
                text = line.text,
                level = it,
                sourceLine = line.sourceLine
            )
        }
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
            // Each body line is its own Text, so the callout's current match is
            // re-counted from the line it falls in (#417).
            val highlight = LocalPreviewFindHighlight.current
            var consumed = 0
            line.text.split("\n").forEach { bodyLine ->
                if (bodyLine.isBlank()) {
                    Spacer(Modifier.height(4.dp))
                } else {
                    val bodyPreviewLine = PreviewLine(
                        text = bodyLine,
                        type = PreviewLineType.BODY,
                        segments = SimpleMarkdownPreview.parseInlineSegments(bodyLine)
                    )
                    CompositionLocalProvider(
                        LocalPreviewFindHighlight provides highlight?.after(consumed)
                    ) {
                        InlineMarkdownText(
                            line = bodyPreviewLine,
                            onFootnoteRefClick = onFootnoteRefClick
                        )
                    }
                    if (highlight != null) {
                        consumed += previewFindParts(bodyPreviewLine)
                            .sumOf { findOccurrences(it, highlight.query).size }
                    }
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
private fun MarkdownTable(
    data: TableData,
    onWikilinkClick: (String) -> Unit = {},
    onFootnoteRefClick: (String) -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(scheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        // Find highlights (#417) count occurrences across every cell, header
        // first and then row by row, in the order previewFindParts lists them;
        // each row starts counting after the cells above it.
        val highlight = LocalPreviewFindHighlight.current
        val occurrencesBeforeRow = remember(data, highlight?.query) {
            val query = highlight?.query ?: return@remember emptyList<Int>()
            val cellCounts = previewFindParts(PreviewLine("", PreviewLineType.TABLE, tableData = data))
                .map { findOccurrences(it, query).size }
            // One pass: occurrences before each row, header first. Summing a
            // growing prefix per row would be quadratic in a large table.
            val rowSizes = listOf(data.headers.size) + data.rows.map { it.size }
            var cell = 0
            var occurrences = 0
            buildList {
                add(0)
                rowSizes.forEach { size ->
                    repeat(size) { occurrences += cellCounts.getOrElse(cell++) { 0 } }
                    add(occurrences)
                }
            }
        }
        // Header row
        TableRow(
            cells = data.headers,
            cellSegments = data.headerSegments,
            alignments = data.alignments,
            background = scheme.surfaceVariant,
            textColor = scheme.onSurface,
            bold = true,
            highlight = highlight?.after(occurrencesBeforeRow.getOrElse(0) { 0 }),
            onWikilinkClick = onWikilinkClick,
            onFootnoteRefClick = onFootnoteRefClick
        )
        // Body rows — divider between each, slight zebra-stripe via alpha
        data.rows.forEachIndexed { index, row ->
            HorizontalDivider(
                color = scheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 1.dp
            )
            TableRow(
                cells = row,
                cellSegments = data.rowSegments.getOrElse(index) { emptyList() },
                alignments = data.alignments,
                background = if (index % 2 == 0) {
                    androidx.compose.ui.graphics.Color.Transparent
                } else {
                    scheme.surfaceVariant.copy(alpha = 0.3f)
                },
                textColor = scheme.onBackground,
                bold = false,
                highlight = highlight?.after(occurrencesBeforeRow.getOrElse(index + 1) { 0 }),
                onWikilinkClick = onWikilinkClick,
                onFootnoteRefClick = onFootnoteRefClick
            )
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    cellSegments: List<List<PreviewInlineSegment>>,
    alignments: List<TableAlignment>,
    background: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    bold: Boolean,
    highlight: PreviewFindHighlight?,
    onWikilinkClick: (String) -> Unit,
    onFootnoteRefClick: (String) -> Unit
) {
    var consumed = 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(vertical = 6.dp)
    ) {
        cells.forEachIndexed { col, cell ->
            val alignment = alignments.getOrElse(col) { TableAlignment.LEFT }
            // Cells with parsed segments go through the same LinkAnnotation
            // machinery as body text so links stay tappable (#197); cells
            // without segment data (hand-built TableData) render the plain
            // string exactly as before.
            val segments = cellSegments.getOrElse(col) { emptyList() }
            val plain = if (segments.isEmpty()) {
                AnnotatedString(cell)
            } else {
                inlineAnnotatedString(
                    segments = segments,
                    onWikilinkClick = onWikilinkClick,
                    onFootnoteRefClick = onFootnoteRefClick
                )
            }
            val content = plain.withFindHighlights(highlight?.after(consumed))
            if (highlight != null) consumed += findOccurrences(plain.text, highlight.query).size
            val layout = remember { TextLayoutHolder() }
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = when (alignment) {
                    TableAlignment.LEFT -> androidx.compose.ui.text.style.TextAlign.Start
                    TableAlignment.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
                    TableAlignment.RIGHT -> androidx.compose.ui.text.style.TextAlign.End
                },
                onTextLayout = { layout.value = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    // A link in a table cell copies its address like any other
                    // (#386), same as it became tappable like any other (#197).
                    .linkPressGestures(content, layout)
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
            text = findHighlighted(text),
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
        // Opted out of the preview's selection (#386): there is no text here to
        // select, and the long press this image already owns — editing its alt
        // text — must not have to win a race against the selection gesture.
        DisableSelection {
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
        }
    } else {
        Text(
            text = findHighlighted(unresolvedImageText(line.text, destination)),
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
            text = annotated.withFindHighlights(LocalPreviewFindHighlight.current),
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
