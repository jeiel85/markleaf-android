package com.markleaf.notes.data.sync

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [wikilinkNoteTarget] — the title a tapped `[[…]]` opens (#608). Before, a
 * link to a heading created a new note titled "Note#Heading".
 */
class WikilinkNoteTargetTest {

    private fun resolve(target: String, titles: Set<String>) =
        runBlocking { wikilinkNoteTarget(target) { it in titles } }

    @Test
    fun aHeadingLinkOpensItsNote() {
        assertEquals("Note", resolve("Note#Heading", setOf("Note")))
        assertEquals("Note", resolve("Note#^block-id", setOf("Note")))
    }

    @Test
    fun aTitleThatContainsAHashStillWins() {
        assertEquals("C# basics", resolve("C# basics", setOf("C# basics", "C")))
    }

    @Test
    fun aHeadingLinkToAMissingNoteNamesTheNoteToCreate() {
        assertEquals("New idea", resolve("New idea#Draft", emptySet()))
    }

    @Test
    fun aSameNoteHeadingLinkNamesNoNote() {
        assertEquals("", resolve("#Heading", emptySet()))
    }

    @Test
    fun aLinkWithoutAHashIsUnchanged() {
        assertEquals("Plain", resolve("Plain", emptySet()))
    }
}
