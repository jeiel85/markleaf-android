package com.markleaf.notes.shortcut

import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.MainActivity
import com.markleaf.notes.R
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.widget.QuickNoteWidget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * The launcher's long-press menu (F1): what goes in it, what each entry opens, and
 * that a note which may not be shown — locked, in the trash, gone — is neither
 * listed nor left usable as a shortcut someone pinned to the home screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LauncherShortcutsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clear() {
        ShortcutManagerCompat.removeAllDynamicShortcuts(context)
    }

    private fun note(
        id: String,
        updated: Long,
        title: String = "Note $id",
        locked: Boolean = false,
        trashed: Boolean = false,
        archived: Boolean = false,
        conflict: Boolean = false
    ) = Note(
        id = id,
        title = title,
        contentMarkdown = "",
        excerpt = "",
        createdAt = Instant.ofEpochSecond(0),
        updatedAt = Instant.ofEpochSecond(updated),
        locked = locked,
        trashed = trashed,
        archived = archived,
        isConflictCopy = conflict
    )

    private fun dynamicIds(): List<String> =
        ShortcutManagerCompat.getDynamicShortcuts(context).sortedBy { it.rank }.map { it.id }

    @Test
    fun noRecentNotesUnlessTheUserTurnedThemOn() {
        val notes = listOf(note("a", 1), note("b", 2))

        assertEquals(emptyList<LauncherShortcuts.RecentNote>(), LauncherShortcuts.recentNotes(notes, enabled = false))
    }

    @Test
    fun recentNotesAreTheTwoNewestEditsThatMayBeShown() {
        val notes = listOf(
            note("old", 1),
            note("newest-locked", 9, locked = true),
            note("trashed", 8, trashed = true),
            note("archived", 7, archived = true),
            note("conflict", 6, conflict = true),
            note("second", 4),
            note("first", 5)
        )

        assertEquals(
            listOf("first", "second"),
            LauncherShortcuts.recentNotes(notes, enabled = true).map { it.noteId }
        )
    }

    @Test
    fun aNoteShortcutIdCarriesTheNoteAndTheFixedOnesDoNot() {
        assertEquals("abc", LauncherShortcuts.noteIdOf(LauncherShortcuts.noteShortcutId("abc")))
        assertNull(LauncherShortcuts.noteIdOf(LauncherShortcuts.ID_NEW_NOTE))
        assertNull(LauncherShortcuts.noteIdOf(LauncherShortcuts.ID_SEARCH))
    }

    @Test
    fun theMenuIsNewNoteThenSearchThenTheRecentNotes() {
        LauncherShortcuts.publish(
            context,
            listOf(LauncherShortcuts.RecentNote("n1", "Groceries"), LauncherShortcuts.RecentNote("n2", ""))
        )

        assertEquals(
            listOf("new_note", "search", "note:n1", "note:n2"),
            dynamicIds()
        )
    }

    @Test
    fun eachEntryOpensMainActivityWithTheActionItsDestinationAnswersTo() {
        val shortcuts = LauncherShortcuts.shortcuts(
            context,
            listOf(LauncherShortcuts.RecentNote("n1", "Groceries"))
        ).associateBy { it.id }

        val newNote = shortcuts.getValue("new_note").intent
        assertEquals(QuickNoteWidget.ACTION_CREATE_NOTE, newNote.action)
        assertEquals(MainActivity::class.java.name, newNote.component?.className)
        assertEquals(context.packageName, newNote.component?.packageName)

        assertEquals(LauncherShortcuts.ACTION_SEARCH, shortcuts.getValue("search").intent.action)

        val open = shortcuts.getValue("note:n1").intent
        assertEquals(QuickNoteWidget.ACTION_OPEN_NOTE, open.action)
        assertEquals("n1", open.getStringExtra(QuickNoteWidget.EXTRA_NOTE_ID))
        assertEquals("note:n1", open.getStringExtra(LauncherShortcuts.EXTRA_SHORTCUT_ID))
    }

    @Test
    fun aNoteWithNoTitleIsLabelledUntitled() {
        val shortcut = LauncherShortcuts.shortcuts(context, listOf(LauncherShortcuts.RecentNote("n1", "")))
            .single { it.id == "note:n1" }

        assertEquals(context.getString(R.string.untitled), shortcut.shortLabel.toString())
    }

    @Test
    fun turningRecentNotesOffTakesThemOutOfTheMenu() {
        LauncherShortcuts.publish(context, listOf(LauncherShortcuts.RecentNote("n1", "Groceries")))
        LauncherShortcuts.publish(context, emptyList())

        assertEquals(listOf("new_note", "search"), dynamicIds())
    }

    @Test
    fun aPinnedArchivedNoteStaysUsableButALockedTrashedOrMissingOneDoesNot() {
        assertTrue(LauncherShortcuts.pinnedNoteAllowed(note("a", 1, archived = true)))
        assertFalse(LauncherShortcuts.pinnedNoteAllowed(note("l", 1, locked = true)))
        assertFalse(LauncherShortcuts.pinnedNoteAllowed(note("t", 1, trashed = true)))
        assertFalse(LauncherShortcuts.pinnedNoteAllowed(null))
    }

    // Robolectric's ShortcutManager keeps disabled pinned shortcuts in a map of its
    // own without flagging them, so disabling cannot be observed through it. The
    // decision is tested here; the platform call is checked on a device.
    @Test
    fun aPinnedShortcutToALockedTrashedOrDeletedNoteIsSwitchedOff() = runTest {
        val notes = mapOf(
            "locked" to note("locked", 1, locked = true),
            "trashed" to note("trashed", 1, trashed = true)
        )
        val pinned = listOf("locked", "trashed", "gone").map {
            LauncherShortcuts.PinnedShortcut(LauncherShortcuts.noteShortcutId(it), enabled = true)
        }

        val changes = LauncherShortcuts.pinnedChanges(pinned) { notes[it] }

        assertEquals(listOf("note:locked", "note:trashed", "note:gone"), changes.disable)
        assertEquals(emptyList<LauncherShortcuts.RecentNote>(), changes.refresh)
    }

    @Test
    fun aPinnedShortcutToANoteThatCameBackIsReEnabledUnderItsCurrentTitle() = runTest {
        val pinned = listOf(LauncherShortcuts.PinnedShortcut("note:n1", enabled = false))

        val changes = LauncherShortcuts.pinnedChanges(pinned) { id -> note(id, 1, title = "Diary") }

        assertEquals(emptyList<String>(), changes.disable)
        assertEquals(listOf(LauncherShortcuts.RecentNote("n1", "Diary")), changes.refresh)
    }

    @Test
    fun anAlreadyDisabledShortcutIsNotDisabledAgainAndTheFixedOnesAreLeftAlone() = runTest {
        val pinned = listOf(
            LauncherShortcuts.PinnedShortcut("note:gone", enabled = false),
            LauncherShortcuts.PinnedShortcut(LauncherShortcuts.ID_NEW_NOTE, enabled = true),
            LauncherShortcuts.PinnedShortcut(LauncherShortcuts.ID_SEARCH, enabled = true)
        )

        val changes = LauncherShortcuts.pinnedChanges(pinned) { null }

        assertEquals(LauncherShortcuts.PinnedChanges(emptyList(), emptyList()), changes)
    }

    @Test
    fun aPinnedShortcutFollowsTheNotesNewTitle() = runTest {
        pin("note:n1", "Draft")

        LauncherShortcuts.reconcilePinned(context) { id -> note(id, 1, title = "Final") }

        assertEquals("Final", pinned("note:n1").shortLabel.toString())
    }

    @Test
    fun aPinnedShortcutToALockedNoteNoLongerCarriesItsTitle() = runTest {
        pin("note:n1", "Diary")

        LauncherShortcuts.reconcilePinned(context) { id -> note(id, 1, title = "Diary", locked = true) }

        assertEquals(
            context.getString(R.string.shortcut_note_hidden_label),
            pinned("note:n1").shortLabel.toString()
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun followingTheNotesRepublishesWhenTheSettingOrTheNotesChange() = runTest {
        val enabled = MutableStateFlow(false)
        val notes = MutableStateFlow(listOf(note("n1", 1), note("n2", 2)))
        val job = launch {
            LauncherShortcuts.follow(context, enabled, notes) { null }
        }

        advanceTimeBy(1_500); runCurrent()
        assertEquals(listOf("new_note", "search"), dynamicIds())

        enabled.value = true
        advanceTimeBy(1_500); runCurrent()
        assertEquals(listOf("new_note", "search", "note:n2", "note:n1"), dynamicIds())

        // Locking the newest note takes it out of the menu at once.
        notes.value = listOf(note("n1", 1), note("n2", 2, locked = true))
        advanceTimeBy(1_500); runCurrent()
        assertEquals(listOf("new_note", "search", "note:n1"), dynamicIds())

        job.cancel()
    }

    @Test
    fun leavingTheAppRightAfterLockingANoteStillTakesItOffTheLauncher() {
        // Review of #489: the started-only collector debounces, so Home pressed inside
        // that second cancelled the update. The pass run on stop does not depend on it.
        LauncherShortcuts.publish(
            context,
            listOf(LauncherShortcuts.RecentNote("n1", "Diary"), LauncherShortcuts.RecentNote("n2", "Other"))
        )
        // A separate pinned note: Robolectric's ShortcutManager updates only one of
        // its maps when an id is both dynamic and pinned, which a device does not.
        pin("note:n3", "Journal")
        val now = listOf(
            note("n1", 3, title = "Diary", locked = true),
            note("n2", 1, title = "Other"),
            note("n3", 2, title = "Journal", trashed = true)
        )

        kotlinx.coroutines.runBlocking {
            LauncherShortcuts.syncWhenLeaving(
                context,
                recentEnabled = { true },
                notes = { now },
                lookUp = { id -> now.firstOrNull { it.id == id } }
            ).join()
        }

        assertEquals(listOf("new_note", "search", "note:n2"), dynamicIds())
        assertEquals(
            context.getString(R.string.shortcut_note_hidden_label),
            pinned("note:n3").shortLabel.toString()
        )
    }

    private fun pin(id: String, title: String) {
        val manager = context.getSystemService(ShortcutManager::class.java)
        val info = ShortcutInfo.Builder(context, id)
            .setShortLabel(title)
            .setIntent(android.content.Intent(QuickNoteWidget.ACTION_OPEN_NOTE).setClass(context, MainActivity::class.java))
            .build()
        manager.requestPinShortcut(info, null)
    }

    private fun pinned(id: String): ShortcutInfo =
        context.getSystemService(ShortcutManager::class.java).pinnedShortcuts.single { it.id == id }
}
