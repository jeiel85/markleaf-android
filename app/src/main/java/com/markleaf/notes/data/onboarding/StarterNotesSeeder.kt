package com.markleaf.notes.data.onboarding

import android.content.Context
import com.markleaf.notes.R
import com.markleaf.notes.core.text.TitleExtractor
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteLinkRepository
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.repository.LocalTagRepository
import com.markleaf.notes.domain.model.Note
import java.io.File
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object StarterNotesSeeder {
    private const val PREFS_NAME = "markleaf_onboarding"
    private const val KEY_STARTER_NOTES_SEEDED = "starter_notes_seeded"
    internal const val STARTER_SAMPLE_IMAGE_PATH = "attachments/starter-note-2/markleaf-sample-cover.png"
    private val seedMutex = Mutex()

    suspend fun seedIfNeeded(context: Context, database: AppDatabase) = seedMutex.withLock {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_STARTER_NOTES_SEEDED, false)) return@withLock

        if (database.noteDao().countAllNotes() > 0) {
            prefs.edit().putBoolean(KEY_STARTER_NOTES_SEEDED, true).apply()
            return@withLock
        }

        val noteRepository = LocalNoteRepository(database)
        val tagRepository = LocalTagRepository(database)
        val linkRepository = LocalNoteLinkRepository(database)

        starterNotes(context).forEach { note ->
            noteRepository.createNote(note)
            tagRepository.reindexTagsForNote(note.id, note.contentMarkdown)
            linkRepository.reindexLinksForNote(note.id, note.contentMarkdown)
        }
        copyStarterSampleAttachment(context)

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
            contents = DEFAULT_STARTER_NOTE_CONTENTS,
            now = now
        )
    }

    internal fun copyStarterSampleAttachment(context: Context): Boolean {
        val target = File(context.filesDir, STARTER_SAMPLE_IMAGE_PATH)
        if (target.exists() && target.length() > 0L) {
            return true
        }
        return runCatching {
            target.parentFile?.mkdirs()
            context.resources.openRawResource(R.raw.markleaf_sample_cover).use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            target.length() > 0L
        }.getOrDefault(false)
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

    private val DEFAULT_STARTER_NOTE_CONTENTS = listOf(
        """
        # Welcome to Markleaf

        Markleaf is a quiet, local-first Markdown notebook for Android. It opens fast, stays out of the way, and keeps your writing as plain text you own.

        ## A tiny tour

        - Open **A Beautiful Markdown Canvas** to see the writing surface.
        - Open **Daily Writing Ritual** for a journal-style example.
        - Open **Project Brief** to see tasks, links, and structure.
        - Open **Local Folder Mirror** when you want files outside the app.

        > [!TIP]
        > These are regular notes. Edit them, export them, move them to trash, or delete them when you no longer need the tour.

        #start #guide
        """.trimIndent(),
        """
        # A Beautiful Markdown Canvas

        ![Markleaf sample canvas]($STARTER_SAMPLE_IMAGE_PATH)

        Markdown stays readable as text, then becomes calm and polished in **Preview**.

        ## What this note demonstrates

        - **Bold**, _italic_, ~~strikethrough~~, and `inline code`
        - Headings, lists, checklists, quotes, dividers, code blocks, tables, callouts, footnotes, links, and images
        - Live syntax styling while you type

        > [!NOTE]
        > Switch between Edit and Preview from the top bar. The note is still just Markdown.

        | Element | Use it for |
        | --- | --- |
        | `#tag` | organization |
        | `[[Project Brief]]` | local note links |
        | `![](...)` | image attachments |

        ```kotlin
        fun markleaf() = "local-first markdown"
        ```

        A small footnote keeps details nearby without interrupting the paragraph.[^1]

        [^1]: Footnotes, callouts, tables, and code blocks are all rendered locally.

        #markdown #showcase
        """.trimIndent(),
        """
        # Daily Writing Ritual

        ## Morning page

        The goal is not to write more. The goal is to make the first sentence easy.

        - [x] Capture one thought
        - [ ] Turn one task into a note
        - [ ] Link related work to [[Project Brief]]

        > Keep the note small enough that you will actually return to it.

        ## Evening close

        What moved today?

        1. One useful decision
        2. One open question
        3. One thing to leave for tomorrow

        #journal #writing
        """.trimIndent(),
        """
        # Project Brief

        This note shows how Markleaf can hold a small project without becoming heavy.

        ## Outcome

        Ship a clean sample notebook that teaches by being useful.

        ## Plan

        - [x] Show Markdown syntax beautifully
        - [x] Include an image attachment
        - [ ] Try search with `local-first`
        - [ ] Open backlinks from **Daily Writing Ritual**

        ## Notes

        Related: [[Daily Writing Ritual]] and [[Tags, Search, and Backlinks]]

        #project/markleaf #planning
        """.trimIndent(),
        """
        # Tags, Search, and Backlinks

        Type tags directly in the body: #project, #writing, #privacy, #local-first.

        ## Search ideas

        Try searching for:

        - `local-first`
        - `folder mirror`
        - `Project Brief`

        ## Backlinks

        Wikilinks use `[[Note Title]]`. When another note links here, Markleaf can show that relationship locally. No account or server is involved.

        See also [[Project Brief]].

        #organize #search
        """.trimIndent(),
        """
        # Local Folder Mirror

        Markleaf does not need its own cloud. Instead, you can choose a folder and let Android or your sync tool handle that folder.

        ## What happens

        - Markleaf writes each note as a Markdown file.
        - Frontmatter keeps the stable `markleaf_id`.
        - Attachments stay beside the mirrored notes.
        - The app still declares no INTERNET permission.

        ## Why it matters

        Your notes remain readable in other Markdown tools, and sync stays your choice.

        #privacy #folder-mirror #local-first
        """.trimIndent()
    )
}
