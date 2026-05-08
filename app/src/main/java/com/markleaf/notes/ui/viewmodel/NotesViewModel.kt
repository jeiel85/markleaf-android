package com.markleaf.notes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markleaf.notes.core.text.TitleExtractor
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class NotesViewModel(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    init {
        observeNotes()
    }

    private fun observeNotes() {
        viewModelScope.launch {
            noteRepository.observeNotes().collect { noteList ->
                _notes.value = noteList
            }
        }
    }

    suspend fun createNote(initialContent: String = ""): Note {
        val newNote = Note(
            id = UUID.randomUUID().toString(),
            title = if (initialContent.isBlank()) "" else TitleExtractor.extractTitle(initialContent),
            contentMarkdown = initialContent,
            excerpt = if (initialContent.isBlank()) "" else TitleExtractor.generateExcerpt(initialContent),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        noteRepository.createNote(newNote)
        return newNote
    }

    fun moveToTrash(noteId: String) {
        viewModelScope.launch {
            noteRepository.moveToTrash(noteId)
        }
    }

    fun setPinned(noteId: String, pinned: Boolean) {
        viewModelScope.launch {
            noteRepository.setPinned(noteId, pinned)
        }
    }

    fun reorderNotes(reorderedNotes: List<Note>) {
        viewModelScope.launch {
            _notes.value = reorderedNotes
            noteRepository.reorderNotes(reorderedNotes)
        }
    }
}
