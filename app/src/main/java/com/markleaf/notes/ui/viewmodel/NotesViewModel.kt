package com.markleaf.notes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markleaf.notes.core.text.TitleExtractor
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import com.markleaf.notes.util.TagParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class NotesViewModel(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    /** The full, unfiltered note list. Used by the quick switcher so it can
     *  always reach every note regardless of the active tag filter. */
    val notes: StateFlow<List<Note>> = _notes

    // Tablet tag-rail filter. Null means "all notes". The rail passes a
    // normalized tag name; selecting a parent tag also matches its children.
    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag

    /** The note list actually shown in the list pane: [notes] filtered by
     *  [selectedTag]. Equals [notes] whenever no tag is selected (always the
     *  case on phones, which have no tag rail). */
    val displayedNotes: StateFlow<List<Note>> =
        combine(_notes, _selectedTag) { notes, tag ->
            if (tag == null) notes else notes.filter { noteMatchesTag(it, tag) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }

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

    fun setArchived(noteId: String, archived: Boolean) {
        viewModelScope.launch {
            noteRepository.setArchived(noteId, archived)
        }
    }

    fun reorderNotes(reorderedNotes: List<Note>) {
        viewModelScope.launch {
            _notes.value = reorderedNotes
            noteRepository.reorderNotes(reorderedNotes)
        }
    }
}

/**
 * Whether [note] carries [tag] (or a child of it). Tags are parsed from the note
 * body via [TagParser] and normalized, so this matches the same hashtags the tag
 * index and the rail show. Selecting a parent tag (`#project`) also matches its
 * descendants (`#project/site`), mirroring Bear's nested-tag behaviour.
 */
internal fun noteMatchesTag(note: Note, tag: String): Boolean {
    val needle = TagParser.normalizeTagName(tag)
    if (needle.isEmpty()) return false
    val noteTags = TagParser.parseTags(note.contentMarkdown).map { TagParser.normalizeTagName(it) }
    return noteTags.any { it == needle || it.startsWith("$needle/") }
}
