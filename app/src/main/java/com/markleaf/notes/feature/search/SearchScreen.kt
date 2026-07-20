package com.markleaf.notes.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalTagRepository
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.model.Tag
import com.markleaf.notes.ui.component.EmptyState
import com.markleaf.notes.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    initialQuery: String = "",
    onBack: () -> Unit = {},
    onNoteClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val tagRepository = remember { LocalTagRepository(db) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val allTags by tagRepository.observeVisibleTags().collectAsState(initial = emptyList())
    val matchingTags = remember(searchQuery, allTags) {
        if (searchQuery.isBlank()) emptyList() else allTags.quickFilter(searchQuery) { it.name }.take(12)
    }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && searchQuery != initialQuery) {
            viewModel.setSearchQuery(initialQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text(stringResource(R.string.search_notes_hint)) },
                singleLine = true
            )

            if (searchQuery.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.type_to_search_notes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (searchResults.isEmpty() && matchingTags.isEmpty()) {
                EmptyState(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.SearchOff,
                    title = stringResource(R.string.no_results_found)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (searchResults.isNotEmpty()) {
                        item { SearchSectionHeader(stringResource(R.string.matching_notes)) }
                        items(searchResults, key = { note -> note.id }) { note ->
                            NoteSearchResult(note = note, onNoteClick = onNoteClick)
                        }
                    }

                    if (matchingTags.isNotEmpty()) {
                        item { SearchSectionHeader(stringResource(R.string.matching_tags)) }
                        items(matchingTags, key = { tag -> tag.id }) { tag ->
                            TagSearchResult(
                                tag = tag,
                                onClick = { viewModel.setSearchQuery("#${tag.name}") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun NoteSearchResult(
    note: Note,
    onNoteClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onNoteClick(note.id) }
    ) {
        Text(
            text = if (note.title.isBlank()) stringResource(R.string.untitled_parenthesized) else note.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (note.excerpt.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note.excerpt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TagSearchResult(
    tag: Tag,
    onClick: () -> Unit
) {
    Text(
        text = stringResource(R.string.tag_result_format, tag.name),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

private fun <T> List<T>.quickFilter(
    query: String,
    value: (T) -> String
): List<T> {
    val normalizedQuery = query.trim().removePrefix("#")
    return filter { item ->
        value(item).contains(normalizedQuery, ignoreCase = true)
    }
}
