package com.markleaf.notes.feature.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What Find opens with for a note tapped in Search (#553). */
class InitialFindTermTest {

    @Test
    fun `a blank query opens no find`() {
        assertNull(initialFindTerm("pear tart", ""))
        assertNull(initialFindTerm("pear tart", "   "))
    }

    @Test
    fun `the whole query is kept when the note contains it`() {
        assertEquals("pear tart", initialFindTerm("A pear tart recipe", "  pear tart "))
    }

    @Test
    fun `matching ignores case like find does`() {
        assertEquals("PEAR", initialFindTerm("a pear", "PEAR"))
    }

    @Test
    fun `words found apart fall back to the first word the note has`() {
        // Search matches the words separately, so the phrase need not be there.
        assertEquals("tart", initialFindTerm("tart first, and later a Pear", "plum tart pear"))
    }

    @Test
    fun `a prefix of a longer word still counts`() {
        assertEquals("pea", initialFindTerm("peanut butter", "pea"))
    }

    @Test
    fun `a tag query keeps its hash`() {
        assertEquals("#journal", initialFindTerm("Today #journal", "#journal"))
    }

    @Test
    fun `no word in the note opens no find`() {
        assertNull(initialFindTerm("apples only", "plum tart"))
    }
}
