package com.markleaf.notes.feature.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalTagRepository

/**
 * The persistent tag sidebar shown as the leading pane of the tablet 3-column
 * layout (tags | note list | editor). Chrome-less on purpose — no Scaffold or
 * TopAppBar — so it can sit inside a [androidx.compose.foundation.layout.Row]
 * pane. Tapping a tag filters the adjacent note list in place via
 * [onSelectTag]; "All notes" (and re-tapping the active tag) clears the filter.
 *
 * [selectedTag] is the normalized full tag name currently filtering the list,
 * or null for "all notes".
 */
@Composable
fun TagRail(
    selectedTag: String?,
    onSelectTag: (String?) -> Unit,
    modifier: Modifier = Modifier,
    onCollapse: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val tagRepository = remember { LocalTagRepository(AppDatabase.getInstance(context)) }
    val tagSummaries by tagRepository.observeTagSummaries().collectAsState(initial = emptyList())
    val rows = remember(tagSummaries) { buildHierarchicalRows(tagSummaries) }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "__rail_header__") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.tags),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 6.dp)
                )
                // Bear-style: hide the whole rail to reclaim writing space. The
                // re-show affordance lives in the note-list top bar (NotesListScreen).
                if (onCollapse != null) {
                    IconButton(onClick = onCollapse) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.hide_tags),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item(key = "__all_notes__") {
            TagRailRow(
                label = stringResource(R.string.all_notes),
                depth = 0,
                selected = selectedTag == null,
                emphasized = true,
                onClick = { onSelectTag(null) }
            )
        }
        items(rows, key = { it.fullName }) { row ->
            TagRailRow(
                label = if (row.depth == 0) "#${row.displayName}" else row.displayName,
                depth = row.depth,
                selected = selectedTag == row.fullName,
                emphasized = row.depth == 0,
                // Toggle: re-tapping the active tag clears the filter.
                onClick = { onSelectTag(if (selectedTag == row.fullName) null else row.fullName) }
            )
        }
    }
}

@Composable
private fun TagRailRow(
    label: String,
    depth: Int,
    selected: Boolean,
    emphasized: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val textColor = when {
        selected -> MaterialTheme.colorScheme.onSecondaryContainer
        emphasized -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(background)
            .padding(start = (16 + depth * 16).dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
    )
}
