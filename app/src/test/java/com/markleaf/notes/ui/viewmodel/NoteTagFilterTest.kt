package com.markleaf.notes.ui.viewmodel

import com.markleaf.notes.domain.model.Note
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Covers [noteMatchesTag], the tag-rail filter predicate. Matching goes through
 * TagParser, so it must be case-insensitive, Unicode-aware, and treat a parent
 * tag as also selecting its children (Bear's nested-tag behaviour).
 */
class NoteTagFilterTest {

    private fun note(body: String) = Note(
        id = "n",
        title = "",
        contentMarkdown = body,
        excerpt = "",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH
    )

    @Test
    fun `matches an exact tag`() {
        assertTrue(noteMatchesTag(note("a note #project here"), "project"))
    }

    @Test
    fun `parent tag matches a child-tagged note`() {
        assertTrue(noteMatchesTag(note("plan #project/site"), "project"))
    }

    @Test
    fun `child tag matches the same nested tag`() {
        assertTrue(noteMatchesTag(note("plan #project/site"), "project/site"))
    }

    @Test
    fun `child filter does not match a parent-only note`() {
        assertFalse(noteMatchesTag(note("plan #project"), "project/site"))
    }

    @Test
    fun `prefix is not a loose substring match`() {
        // "#projects" must not satisfy a "project" filter.
        assertFalse(noteMatchesTag(note("see #projects"), "project"))
    }

    @Test
    fun `matching is case-insensitive`() {
        assertTrue(noteMatchesTag(note("a #Work item"), "work"))
    }

    @Test
    fun `korean nested tag matches its parent`() {
        assertTrue(noteMatchesTag(note("메모 #프로젝트/현장"), "프로젝트"))
    }

    @Test
    fun `note without the tag does not match`() {
        assertFalse(noteMatchesTag(note("untagged body"), "project"))
    }

    @Test
    fun `empty tag never matches`() {
        assertFalse(noteMatchesTag(note("a #project note"), ""))
    }
}
