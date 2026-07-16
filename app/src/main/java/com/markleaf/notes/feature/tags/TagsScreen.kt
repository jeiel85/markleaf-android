package com.markleaf.notes.feature.tags

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.ui.component.EmptyState
import com.markleaf.notes.data.repository.LocalTagRepository
import com.markleaf.notes.domain.model.TagSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    onBack: () -> Unit = {},
    onTagClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val tagRepository = remember { LocalTagRepository(AppDatabase.getInstance(context)) }
    val tagSummaries by tagRepository.observeTagSummaries().collectAsState(initial = emptyList())
    val rows = remember(tagSummaries) { buildHierarchicalRows(tagSummaries) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tags)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            if (tagSummaries.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Sell,
                    title = stringResource(R.string.no_tags_yet),
                    hint = stringResource(R.string.tags_empty_hint)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                    ) {
                        items(rows, key = { row -> row.fullName }) { row ->
                            TagRow(
                                row = row,
                                onClick = { onTagClick("#${row.fullName}") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 16.dp + (row.depth * 20).dp,
                                        end = 16.dp,
                                        top = 10.dp,
                                        bottom = 10.dp
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

internal data class TagRowData(
    val fullName: String,
    val displayName: String,
    val depth: Int,
    val noteCount: Int
)

internal fun buildHierarchicalRows(summaries: List<TagSummary>): List<TagRowData> {
    if (summaries.isEmpty()) return emptyList()

    val byFullName = summaries.associateBy { it.tag.name }

    val sortedNames = summaries.map { it.tag.name }.distinct().sorted()

    return sortedNames.map { fullName ->
        val segments = fullName.split('/')
        val depth = (segments.size - 1).coerceAtLeast(0)
        TagRowData(
            fullName = fullName,
            displayName = segments.last(),
            depth = depth,
            noteCount = byFullName[fullName]?.noteCount ?: 0
        )
    }
}

@Composable
private fun TagRow(
    row: TagRowData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (row.depth == 0) "#${row.displayName}" else row.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = if (row.depth == 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondary,
                fontWeight = if (row.depth == 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = pluralStringResource(R.plurals.tag_note_count_format, row.noteCount, row.noteCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = row.noteCount.toString(),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}
