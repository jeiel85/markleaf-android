package com.markleaf.notes.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the rule that decides when the NavHost's own slide steps aside for the
 * note → editor container transform, and the corner maths of the container.
 *
 * Both are pure on purpose: a wrong answer here does not crash, it just makes the
 * editor slide sideways *and* unfold at once, or appear from nowhere — which no
 * other test in the suite would notice.
 */
class NoteTransitionTest {

    private val searchRoute = "${NavRoutes.SEARCH}?query={query}"

    private fun hop(
        initialRoute: String?,
        initialNoteId: String? = null,
        targetRoute: String?,
        targetNoteId: String? = null,
        origin: NoteOrigin?
    ) = isContainerHop(initialRoute, initialNoteId, targetRoute, targetNoteId, origin)

    // --- isContainerHop -----------------------------------------------------

    @Test
    fun listOpensTheTappedNoteAsAHop() {
        assertTrue(
            hop(
                initialRoute = NavRoutes.NOTES,
                targetRoute = NavRoutes.EDITOR, targetNoteId = "a",
                origin = NoteOrigin("a", NoteSource.LIST)
            )
        )
    }

    @Test
    fun theEditorReturningToItsSourceIsAHopToo() {
        assertTrue(
            hop(
                initialRoute = NavRoutes.EDITOR, initialNoteId = "a",
                targetRoute = NavRoutes.NOTES,
                origin = NoteOrigin("a", NoteSource.LIST)
            )
        )
    }

    @Test
    fun aGridTileIsASourceOnTheListRoute() {
        assertTrue(
            hop(
                initialRoute = NavRoutes.NOTES,
                targetRoute = NavRoutes.EDITOR, targetNoteId = "a",
                origin = NoteOrigin("a", NoteSource.TILE)
            )
        )
        assertTrue(
            hop(
                initialRoute = NavRoutes.EDITOR, initialNoteId = "a",
                targetRoute = NavRoutes.NOTES,
                origin = NoteOrigin("a", NoteSource.TILE)
            )
        )
    }

    @Test
    fun fabOpensTheNewNoteOnTheListRoute() {
        assertTrue(
            hop(
                initialRoute = NavRoutes.NOTES,
                targetRoute = NavRoutes.EDITOR, targetNoteId = "new",
                origin = NoteOrigin("new", NoteSource.FAB)
            )
        )
    }

    @Test
    fun searchAndArchiveAreSourcesOnTheirOwnRoutes() {
        assertTrue(
            hop(
                initialRoute = searchRoute,
                targetRoute = NavRoutes.EDITOR, targetNoteId = "a",
                origin = NoteOrigin("a", NoteSource.SEARCH)
            )
        )
        assertTrue(
            hop(
                initialRoute = NavRoutes.ARCHIVE,
                targetRoute = NavRoutes.EDITOR, targetNoteId = "a",
                origin = NoteOrigin("a", NoteSource.ARCHIVE)
            )
        )
    }

    @Test
    fun noOriginMeansNoHop() {
        // A widget, a share intent or the "reopen last note" launch opens the
        // editor without a tapped row — nothing to grow out of.
        assertFalse(
            hop(
                initialRoute = NavRoutes.NOTES,
                targetRoute = NavRoutes.EDITOR, targetNoteId = "a",
                origin = null
            )
        )
    }

    @Test
    fun aDifferentNoteIsNotTheHop() {
        // Wikilink to another note from the origin's editor: editor -> editor.
        assertFalse(
            hop(
                initialRoute = NavRoutes.EDITOR, initialNoteId = "a",
                targetRoute = NavRoutes.EDITOR, targetNoteId = "b",
                origin = NoteOrigin("a", NoteSource.LIST)
            )
        )
        // Same origin, but the editor on screen is the wikilinked note, going back
        // to the origin's editor rather than to the list.
        assertFalse(
            hop(
                initialRoute = NavRoutes.EDITOR, initialNoteId = "b",
                targetRoute = NavRoutes.EDITOR, targetNoteId = "a",
                origin = NoteOrigin("a", NoteSource.LIST)
            )
        )
        // Opening some other note while an older origin is still remembered.
        assertFalse(
            hop(
                initialRoute = NavRoutes.NOTES,
                targetRoute = NavRoutes.EDITOR, targetNoteId = "b",
                origin = NoteOrigin("a", NoteSource.LIST)
            )
        )
    }

    @Test
    fun aStaleOriginFromAnotherScreenIsNotTheHop() {
        // Tapped a search result earlier; now the same note is opened from the
        // list route (a widget while the app was open). The remembered source
        // is not on screen, so its key has nothing to pair with.
        assertFalse(
            hop(
                initialRoute = NavRoutes.NOTES,
                targetRoute = NavRoutes.EDITOR, targetNoteId = "a",
                origin = NoteOrigin("a", NoteSource.SEARCH)
            )
        )
        assertFalse(
            hop(
                initialRoute = NavRoutes.EDITOR, initialNoteId = "a",
                targetRoute = NavRoutes.NOTES,
                origin = NoteOrigin("a", NoteSource.ARCHIVE)
            )
        )
    }

    @Test
    fun unrelatedDestinationsAreNeverAHop() {
        val origin = NoteOrigin("a", NoteSource.LIST)
        assertFalse(hop(NavRoutes.NOTES, null, NavRoutes.SETTINGS, null, origin))
        assertFalse(hop(NavRoutes.SETTINGS, null, NavRoutes.NOTES, null, origin))
        assertFalse(hop(NavRoutes.EDITOR, "a", NavRoutes.SETTINGS, null, origin))
        assertFalse(hop(NavRoutes.NOTES, null, searchRoute, null, origin))
    }

    // --- NoteSource keys ----------------------------------------------------

    @Test
    fun sourcesDoNotShareKeys() {
        // Navigation keeps the outgoing and incoming destinations composed
        // together; a shared key would pair a list row with its search twin.
        val keys = listOf(
            NoteSource.LIST.keyFor("a"),
            NoteSource.TILE.keyFor("a"),
            NoteSource.SEARCH.keyFor("a"),
            NoteSource.ARCHIVE.keyFor("a")
        )
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun theFabKeyDoesNotDependOnTheNote() {
        // The FAB is drawn before the note exists.
        assertEquals(NoteSource.FAB.keyFor("x"), NoteSource.FAB.keyFor("y"))
        assertNotEquals(NoteSource.FAB.keyFor("x"), NoteSource.LIST.keyFor("x"))
    }

    // --- containerCornerRadiusPx --------------------------------------------

    private val card = 12f
    private val windowW = 360f
    private val windowH = 800f

    private fun radius(w: Float, h: Float) =
        containerCornerRadiusPx(card, w, h, windowW, windowH)

    @Test
    fun aRowKeepsItsCornersWhileItIsStillShort() {
        // Full width already, so only height decides: 80/800 is 10% covered.
        assertEquals(card * 0.9f, radius(windowW, 80f), 0.001f)
    }

    @Test
    fun aTileNeedsBothAxesBeforeItsCornersRelax() {
        // Full height but a fifth of the width: the limiting axis is the width.
        assertEquals(card * 0.8f, radius(72f, windowH), 0.001f)
    }

    @Test
    fun cornersAreSquareOnceTheContainerFillsTheWindow() {
        assertEquals(0f, radius(windowW, windowH), 0f)
        // A spring can overshoot the window by a hair; the radius must not go
        // negative (a negative corner draws inverted notches).
        assertEquals(0f, radius(windowW * 1.02f, windowH * 1.02f), 0f)
    }

    @Test
    fun cornersNeverGrowPastTheCardsOwn() {
        assertEquals(card, radius(0f, 0f), 0f)
    }

    @Test
    fun anUnknownWindowKeepsTheCardRadius() {
        assertEquals(card, containerCornerRadiusPx(card, 100f, 100f, 0f, 0f), 0f)
    }

    @Test
    fun cornersRelaxMonotonicallyAsTheContainerGrows() {
        var previous = Float.MAX_VALUE
        for (step in 0..20) {
            val t = step / 20f
            val current = radius(windowW * t, windowH * t)
            assertTrue("radius grew at t=$t: $current > $previous", current <= previous)
            previous = current
        }
    }
}
