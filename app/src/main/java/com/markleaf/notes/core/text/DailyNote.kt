package com.markleaf.notes.core.text

import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Today's note, for the "Today's note" choice of the new-note shortcuts (#481).
 *
 * Input: the [NoteRepository], the local date, the moment, and the reader's
 * title rule. Output: the one note for that date, found or created.
 *
 * Core logic: a note counts as the date's note when its title is the date in
 * ISO form (`2026-10-06`) and it is neither in the trash nor archived — a note
 * the reader put away is not one they want new lines added to. When several
 * match (a copy brought in by sync, say), the most recently edited one wins, so
 * the shortcut keeps adding to the note the reader has been using. A new one is
 * seeded with the date as its heading and an empty line under it, which puts
 * the caret where the first line of the day goes.
 *
 * The date is the caller's to decide: the shortcut passes the device's local
 * date, so the day turns at local midnight, as #481 asked.
 */
object DailyNote {

    fun title(date: LocalDate): String = date.toString()

    fun initialContent(date: LocalDate): String = "# ${title(date)}\n\n"

    /** The date's note among [notes], or null when there is none. */
    fun find(notes: List<Note>, date: LocalDate): Note? {
        val title = title(date)
        return notes
            .filter { it.title == title && !it.trashed && !it.archived }
            .maxByOrNull { it.updatedAt }
    }

    /**
     * One lookup-and-create at a time, app-wide (#516 review): two shortcut
     * launches overlapping each found no note and each made one. This covers
     * the app's own callers. A note with the same title arriving from the sync
     * folder is a separate path that a lock here can't reach (titles aren't
     * unique); [find] then picks the most recently edited of the two.
     */
    private val lookupLock = Mutex()

    suspend fun findOrCreate(
        repository: NoteRepository,
        date: LocalDate,
        now: Instant,
        titleSource: NoteTitleSource
    ): Note = lookupLock.withLock {
        find(repository.getAllNotes(), date)?.let { return@withLock it }
        val content = initialContent(date)
        val note = Note(
            id = UUID.randomUUID().toString(),
            title = TitleExtractor.extractTitle(content, titleSource),
            contentMarkdown = content,
            excerpt = TitleExtractor.generateExcerpt(content, titleSource),
            createdAt = now,
            updatedAt = now
        )
        repository.createNote(note)
        note
    }
}
