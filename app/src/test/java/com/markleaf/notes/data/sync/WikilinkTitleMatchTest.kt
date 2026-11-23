package com.markleaf.notes.data.sync

import com.markleaf.notes.data.local.dao.NoteTitleRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [wikilinkTitleMatch] — the note a `[[title]]` tap opens once no title matches
 * exactly (#602). It must agree with backlinks, which already treat
 * `[[project brief]]` as a link to "Project Brief".
 */
class WikilinkTitleMatchTest {

    private fun row(id: String, title: String, locked: Boolean = false, updatedAt: Long = 0) =
        NoteTitleRow(id = id, title = title, locked = locked, updatedAt = updatedAt)

    @Test
    fun capitalsThatDifferStillFindTheNote() {
        val rows = listOf(row("brief", "Project Brief"), row("other", "Daily Writing Ritual"))

        assertEquals("brief", wikilinkTitleMatch("project brief", rows)?.id)
        assertEquals("brief", wikilinkTitleMatch("PROJECT BRIEF", rows)?.id)
    }

    @Test
    fun capitalsOutsideAsciiAreFoldedToo() {
        // SQLite's NOCASE would miss these, which is why the match is in Kotlin.
        val rows = listOf(row("ru", "Заметка"), row("de", "Über uns"))

        assertEquals("ru", wikilinkTitleMatch("заметка", rows)?.id)
        assertEquals("de", wikilinkTitleMatch("über UNS", rows)?.id)
    }

    @Test
    fun surroundingSpacesInTheTitleDoNotMatter() {
        assertEquals("a", wikilinkTitleMatch("plan", listOf(row("a", "  Plan ")))?.id)
    }

    @Test
    fun aDifferentTitleIsNoMatch() {
        assertNull(wikilinkTitleMatch("Project", listOf(row("brief", "Project Brief"))))
        assertNull(wikilinkTitleMatch("   ", listOf(row("blank", ""))))
    }

    @Test
    fun anUnlockedNoteBeatsALockedOne() {
        val rows = listOf(
            row("locked", "plan", locked = true, updatedAt = 9),
            row("open", "PLAN", updatedAt = 1)
        )

        assertEquals("open", wikilinkTitleMatch("Plan", rows)?.id)
    }

    @Test
    fun onlyALockedMatchIsReturnedAsLocked() {
        val match = wikilinkTitleMatch("diary", listOf(row("d", "Diary", locked = true)))

        assertEquals(true, match?.locked)
    }

    @Test
    fun amongSeveralTheMostRecentlyEditedWins() {
        val rows = listOf(row("old", "Plan", updatedAt = 1), row("new", "PLAN", updatedAt = 5))

        assertEquals("new", wikilinkTitleMatch("plan", rows)?.id)
    }
}
