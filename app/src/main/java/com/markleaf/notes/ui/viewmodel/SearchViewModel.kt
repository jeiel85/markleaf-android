package com.markleaf.notes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Note>>(emptyList())
    val searchResults: StateFlow<List<Note>> = _searchResults.asStateFlow()

    /** Every visible (non-trashed/archived/locked) note. Backs the Quick Access
     *  behaviours folded into the Search screen (#193): the recent-notes list
     *  shown before typing, and the titles-only match mode. */
    private val _allNotes = MutableStateFlow<List<Note>>(emptyList())
    val allNotes: StateFlow<List<Note>> = _allNotes.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            noteRepository.observeNotes().collect { notes ->
                _allNotes.value = notes
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce delay
            if (query.isBlank()) {
                _searchResults.value = emptyList()
            } else {
                noteRepository.searchNotes(query)
                    .collect { notes ->
                        _searchResults.value = notes
                    }
            }
        }
    }
}
