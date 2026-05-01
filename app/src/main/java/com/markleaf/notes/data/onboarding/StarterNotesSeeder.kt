package com.markleaf.notes.data.onboarding

import android.content.Context
import com.markleaf.notes.R
import com.markleaf.notes.core.text.TitleExtractor
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.repository.LocalTagRepository
import com.markleaf.notes.domain.model.Note
import java.time.Instant

object StarterNotesSeeder {
    private const val PREFS_NAME = "markleaf_onboarding"
    private const val KEY_STARTER_NOTES_SEEDED = "starter_notes_seeded"

    suspend fun seedIfNeeded(context: Context, database: AppDatabase) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_STARTER_NOTES_SEEDED, false)) {
            return
        }

        if (database.noteDao().countAllNotes() > 0) {
            prefs.edit().putBoolean(KEY_STARTER_NOTES_SEEDED, true).apply()
            return
        }

        val noteRepository = LocalNoteRepository(database)
        val tagRepository = LocalTagRepository(database)

        starterNotes(context).forEach { note ->
            noteRepository.createNote(note)
            tagRepository.reindexTagsForNote(note.id, note.contentMarkdown)
        }

        prefs.edit().putBoolean(KEY_STARTER_NOTES_SEEDED, true).apply()
    }

    internal fun starterNotes(
        context: Context,
        now: Instant = Instant.now()
    ): List<Note> {
        return starterNotes(
            contents = context.resources.openRawResource(R.raw.starter_notes)
                .bufferedReader()
                .use { reader -> reader.readText() }
                .split(STARTER_NOTE_SEPARATOR)
                .map { content -> content.trim() }
                .filter { content -> content.isNotBlank() },
            now = now
        )
    }

    internal fun starterNotes(now: Instant = Instant.now()): List<Note> {
        return starterNotes(
            contents = listOf(
            """
            # Welcome to Markleaf

            Markleaf is a local-first Markdown notes app for Android. It is designed to open quickly, let you write immediately, and keep your notes portable as Markdown files.

            ## Try first

            - Create a new note and capture an idea
            - Add tags like #idea inside the body
            - Link notes with `[[Write beautifully with Markdown]]`
            - Create a ZIP backup from Settings

            You can edit or delete these starter notes at any time. #start #guide
            """.trimIndent(),
            """
            # Write beautifully with Markdown

            Markdown keeps your writing readable as plain text while still supporting structure and style.

            ## Common syntax

            - **Bold**
            - _Italic_
            - `Code`
            - [ ] Todo
            - [x] Done

            Use Preview mode to see how the note renders. #markdown #writing
            """.trimIndent(),
            """
            # Organize with tags and links

            Markleaf is organized around tags and links instead of folders.

            ## Tags

            Write tags such as #project, #meeting, or #reading in the body to collect related notes. A note can have multiple tags.

            ## Wiki links

            Wrap another note title like `[[Local-first backup and export]]` to connect notes.

            This note also links back to `[[Welcome to Markleaf]]`. #organize #links
            """.trimIndent(),
            """
            # Local-first backup and export

            In the MVP, Markleaf stores notes on your device without accounts, analytics, ads, or server integration.

            ## Take your data with you

            - Share a single note as Markdown.
            - Export all notes as Markdown files.
            - Create and restore ZIP backups from Settings.

            By default, your notes do not leave the device until you explicitly export, share, or back them up. #backup #privacy
            """.trimIndent()
            ),
            now = now
        )
    }

    private fun starterNotes(
        contents: List<String>,
        now: Instant
    ): List<Note> {
        return contents.mapIndexed { index, content ->
            Note(
                id = "starter-note-${index + 1}",
                title = TitleExtractor.extractTitle(content),
                contentMarkdown = content,
                excerpt = TitleExtractor.generateExcerpt(content),
                createdAt = now.plusMillis(index.toLong()),
                updatedAt = now.plusMillis(index.toLong()),
                pinned = index == 0
            )
        }
    }

    private const val STARTER_NOTE_SEPARATOR = "---markleaf-note---"
}
