package com.markleaf.notes.feature.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.core.markdown.preview.TocHeading

// The note's heading outline, on a screen of its own (#215).
//
// It used to be one section of the "Note information" bottom sheet, sharing a
// scroll with the statistics and the backlinks — so on the long notes that
// actually need an outline it was squeezed into whatever the sheet had left.
//
// The rows are deliberately uniform: one text style for every entry, with
// indentation as the only thing carrying the level. The sheet varied the type
// style *and* the indent, which encoded the same fact twice and read as noise
// rather than structure.

private val OutlineIndentPerLevel = 24.dp
private val OutlineHorizontalPadding = 20.dp

/** The outline screen's own bar, replacing the editor's while it is open. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteOutlineTopBar(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.table_of_contents),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = modifier
    )
}

/**
 * The outline list itself. Separate from [NoteOutlineTopBar] so the editor can
 * hand it the Scaffold's padding, and so a snapshot test can render the list
 * without a bar above it.
 */
@Composable
internal fun NoteOutlineContent(
    headings: List<TocHeading>,
    onHeadingClick: (TocHeading) -> Unit,
    modifier: Modifier = Modifier
) {
    // Resolved out here, in a @Composable context, because the LazyListScope
    // lambda below is not one. Doubles as the TalkBack action label (#152).
    val headingActionLabel = stringResource(R.string.note_information_heading_action)
    if (headings.isEmpty()) {
        Text(
            text = stringResource(R.string.note_information_no_headings),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = OutlineHorizontalPadding, vertical = 16.dp)
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = headings,
            key = { heading -> "${heading.index}:${heading.level}:${heading.text}" }
        ) { heading ->
            Text(
                text = heading.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    // Announced as a button with an explicit action so TalkBack
                    // offers "double-tap to jump to section" rather than reading
                    // the heading as static text (#152).
                    .clickable(
                        onClickLabel = headingActionLabel,
                        role = Role.Button
                    ) { onHeadingClick(heading) }
                    .padding(
                        start = OutlineHorizontalPadding +
                            OutlineIndentPerLevel * (heading.level - 1),
                        end = OutlineHorizontalPadding,
                        top = 12.dp,
                        bottom = 12.dp
                    )
            )
        }
    }
}
