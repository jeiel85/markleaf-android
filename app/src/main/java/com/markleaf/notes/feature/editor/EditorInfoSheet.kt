package com.markleaf.notes.feature.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.core.markdown.preview.TocHeading
import com.markleaf.notes.domain.model.Note

internal data class EditorInfoUiState(
    val statsText: String,
    val headings: List<TocHeading>,
    val backlinks: List<Note>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorInfoSheet(
    state: EditorInfoUiState,
    onHeadingClick: (Int) -> Unit,
    onBacklinkClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        EditorInfoSheetContent(
            state = state,
            onHeadingClick = onHeadingClick,
            onBacklinkClick = onBacklinkClick
        )
    }
}

@Composable
internal fun EditorInfoSheetContent(
    state: EditorInfoUiState,
    onHeadingClick: (Int) -> Unit,
    onBacklinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
            item(key = "title") {
                Text(
                    text = stringResource(R.string.note_information),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .semantics { heading() }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            item(key = "statistics-heading") {
                InfoSectionHeading(stringResource(R.string.note_statistics))
            }
            item(key = "statistics") {
                Text(
                    text = state.statsText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            item(key = "statistics-divider") { InfoDivider() }

            item(key = "outline-heading") {
                InfoSectionHeading(stringResource(R.string.table_of_contents))
            }
            if (state.headings.isEmpty()) {
                item(key = "outline-empty") {
                    InfoEmptyText(stringResource(R.string.note_information_no_headings))
                }
            } else {
                items(
                    items = state.headings,
                    key = { heading -> "${heading.index}:${heading.level}:${heading.text}" }
                ) { heading ->
                    Text(
                        text = heading.text,
                        style = when (heading.level) {
                            1 -> MaterialTheme.typography.titleMedium
                            2 -> MaterialTheme.typography.bodyLarge
                            else -> MaterialTheme.typography.bodyMedium
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable { onHeadingClick(heading.index) }
                            .padding(
                                start = (20 + (heading.level - 1) * 16).dp,
                                end = 20.dp,
                                top = 12.dp,
                                bottom = 12.dp
                            )
                    )
                }
            }
            item(key = "outline-divider") { InfoDivider() }

            item(key = "backlinks-heading") {
                InfoSectionHeading(stringResource(R.string.backlinks_section_title))
            }
            if (state.backlinks.isEmpty()) {
                item(key = "backlinks-empty") {
                    InfoEmptyText(stringResource(R.string.note_information_no_backlinks))
                }
            } else {
                items(
                    items = state.backlinks,
                    key = { note -> note.id }
                ) { note ->
                    Text(
                        text = note.title.ifEmpty { stringResource(R.string.untitled) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable { onBacklinkClick(note.id) }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
    }
}

@Composable
private fun InfoSectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .semantics { heading() }
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun InfoEmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun InfoDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
