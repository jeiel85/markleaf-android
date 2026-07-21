package com.markleaf.notes.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalTagRepository
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.model.Tag
import com.markleaf.notes.ui.component.EmptyState
import com.markleaf.notes.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    initialQuery: String = "",
    onBack: () -> Unit = {},
    onNoteClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val tagRepository = remember { LocalTagRepository(db) }
    val settingsRepository = remember { AppSettingsRepository(context.applicationContext) }
    val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val allTags by tagRepository.observeVisibleTags().collectAsState(initial = emptyList())
    val matchingTags = remember(searchQuery, allTags) {
        if (searchQuery.isBlank()) emptyList() else allTags.quickFilter(searchQuery) { it.name }.take(12)
    }
    // Quick Access folded into Search (#193): before any typing the screen
    // offers the most recently edited notes, and a persisted titles-only mode
    // narrows matching to note titles (the quick switcher's semantics).
    // Computed off the main thread — at thousands of notes an in-place filter
    // and sort per keystroke would jank the query field (#195). The effects
    // restart when their inputs change, cancelling a stale computation.
    val titlesOnly = appSettings.searchTitlesOnly
    var recentNotes by remember { mutableStateOf(emptyList<Note>()) }
    LaunchedEffect(searchQuery, allNotes) {
        recentNotes = if (searchQuery.isBlank()) {
            withContext(Dispatchers.Default) { recentNotesForSearch(allNotes) }
        } else {
            emptyList()
        }
    }
    var titleResults by remember { mutableStateOf(emptyList<Note>()) }
    LaunchedEffect(searchQuery, allNotes, titlesOnly) {
        titleResults = if (titlesOnly) {
            withContext(Dispatchers.Default) { filterNotesByTitle(allNotes, searchQuery) }
        } else {
            emptyList()
        }
    }
    val noteResults = if (titlesOnly) titleResults else searchResults

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && searchQuery != initialQuery) {
            viewModel.setSearchQuery(initialQuery)
        }
    }

    // Focus the query field as soon as the screen opens so the keyboard is up
    // and ready — searching should never need a second tap (#190). Skipped when
    // the screen was opened *with* a query (a tag tapped on the Tags screen):
    // there the user came to read results, not to type.
    val queryFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (initialQuery.isBlank()) queryFocusRequester.requestFocus()
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                    .focusRequester(queryFocusRequester),
                placeholder = { Text(stringResource(R.string.search_notes_hint)) },
                singleLine = true
            )

            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !titlesOnly,
                    onClick = { scope.launch { settingsRepository.setSearchTitlesOnly(false) } },
                    label = { Text(stringResource(R.string.search_mode_all)) }
                )
                FilterChip(
                    selected = titlesOnly,
                    onClick = { scope.launch { settingsRepository.setSearchTitlesOnly(true) } },
                    label = { Text(stringResource(R.string.search_mode_titles)) }
                )
            }

            if (searchQuery.isBlank()) {
                if (recentNotes.isEmpty()) {
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
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { SearchSectionHeader(stringResource(R.string.search_recent_notes)) }
                        items(recentNotes, key = { note -> note.id }) { note ->
                            NoteSearchResult(
                                note = note,
                                showPreview = appSettings.notesShowPreview,
                                onNoteClick = onNoteClick
                            )
                        }
                    }
                }
            } else if (noteResults.isEmpty() && matchingTags.isEmpty()) {
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
                    if (noteResults.isNotEmpty()) {
                        item { SearchSectionHeader(stringResource(R.string.matching_notes)) }
                        items(noteResults, key = { note -> note.id }) { note ->
                            NoteSearchResult(
                                note = note,
                                showPreview = appSettings.notesShowPreview,
                                onNoteClick = onNoteClick
                            )
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
    showPreview: Boolean,
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
        if (showPreview && note.excerpt.isNotBlank()) {
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

/** How many recently edited notes the blank-query Quick Access list offers —
 *  the same window the quick switcher shows. */
private const val RECENT_NOTES_LIMIT = 20

/** Cap titles-only results like the FTS queries cap theirs. */
private const val TITLE_RESULTS_LIMIT = 200

/** The tap-to-open list shown before any query is typed (#193): most recently
 *  edited first, untitled notes skipped — same rules as the quick switcher. */
internal fun recentNotesForSearch(notes: List<Note>): List<Note> =
    notes.asSequence()
        .filter { it.title.isNotBlank() }
        .sortedByDescending { it.updatedAt }
        .take(RECENT_NOTES_LIMIT)
        .toList()

/** Titles-only matching (#193) — the quick switcher's semantics: substring,
 *  case-insensitive, most recently edited first. */
internal fun filterNotesByTitle(notes: List<Note>, query: String): List<Note> {
    val needle = query.trim()
    if (needle.isEmpty()) return emptyList()
    return notes.asSequence()
        .filter { it.title.contains(needle, ignoreCase = true) }
        .sortedByDescending { it.updatedAt }
        .take(TITLE_RESULTS_LIMIT)
        .toList()
}
