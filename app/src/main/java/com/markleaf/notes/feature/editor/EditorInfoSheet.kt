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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.ui.component.formatNoteDateTime
import java.time.Instant

/**
 * What the "Note information" sheet shows. The heading outline used to be a
 * third section here; it moved to a screen of its own, where a long one is not
 * competing with the statistics and the backlinks for the same scroll (#215).
 *
 * [createdAt] and [updatedAt] are the saved note's (#593): the list shows only
 * how long ago a note was edited, and only until the note is opened. Null while
 * the row is still loading, and for a note that has no row yet; the dates
 * section is left out until both are known.
 */
internal data class EditorInfoUiState(
    val statsText: String,
    val backlinks: List<Note>,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorInfoSheet(
    state: EditorInfoUiState,
    onBacklinkClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        EditorInfoSheetContent(
            state = state,
            onBacklinkClick = onBacklinkClick
        )
    }
}

@Composable
internal fun EditorInfoSheetContent(
    state: EditorInfoUiState,
    onBacklinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Resolved here (a @Composable context) rather than inside the LazyListScope
    // lambda below, which is not composable. Used as the TalkBack action label
    // for the clickable backlink rows (#152).
    val backlinkActionLabel = stringResource(R.string.note_information_backlink_action)
    val context = LocalContext.current
    val createdAt = state.createdAt
    val updatedAt = state.updatedAt
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

            if (createdAt != null && updatedAt != null) {
                item(key = "dates-heading") {
                    InfoSectionHeading(stringResource(R.string.note_information_dates))
                }
                item(key = "dates-created") {
                    InfoDateText(
                        stringResource(
                            R.string.note_information_created,
                            formatNoteDateTime(context, createdAt)
                        )
                    )
                }
                item(key = "dates-edited") {
                    InfoDateText(
                        stringResource(
                            R.string.note_information_edited,
                            formatNoteDateTime(context, updatedAt)
                        )
                    )
                }
                item(key = "dates-divider") { InfoDivider() }
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
                            .clickable(
                                onClickLabel = backlinkActionLabel,
                                role = Role.Button
                            ) { onBacklinkClick(note.id) }
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
private fun InfoDateText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
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
