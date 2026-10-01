package com.markleaf.notes.shortcut

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.markleaf.notes.MainActivity
import com.markleaf.notes.R
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.widget.QuickNoteWidget
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce

/**
 * The menu a launcher shows on a long press of Markleaf's icon
 * (`docs/ANDROID_PLATFORM_PLAN.md` F1).
 *
 * Two entries are always there — a new note and search. The two most recently
 * edited notes join them only when the user turns that on in Settings → Privacy,
 * because a shortcut's label is the note's title and the launcher shows it on the
 * home screen, and some launchers list shortcuts in their own search as well.
 *
 * Everything is published from code with [ShortcutManagerCompat.setDynamicShortcuts].
 * A static `shortcuts.xml` would need its target package written out literally, and
 * the debug build's package is `com.markleaf.notes.debug` (#319). The cost is that
 * the menu appears only after Markleaf has been opened once.
 *
 * A dynamic shortcut can be dragged onto the home screen, where it becomes a pinned
 * shortcut the app can neither remove nor hide. Those follow the widgets' rule
 * (`SingleNoteWidget`): a note that is locked, in the trash or gone is not shown.
 * The app relabels such a pinned shortcut "Unavailable" and disables it, which greys
 * it out and replaces a tap with a message — the relabelling matters because a
 * disabled shortcut otherwise keeps showing the title. If the note comes back, the
 * shortcut is enabled again under the note's current title.
 */
internal object LauncherShortcuts {

    /** Opens the Search screen. Routed by `MainActivity` like the widget actions. */
    const val ACTION_SEARCH = "com.markleaf.notes.ACTION_SEARCH"

    /** Which shortcut started the activity, so it can be reported as used. */
    const val EXTRA_SHORTCUT_ID = "shortcut_id"

    const val ID_NEW_NOTE = "new_note"
    const val ID_SEARCH = "search"
    private const val NOTE_ID_PREFIX = "note:"

    /** Two notes beside the two fixed entries; launchers show four comfortably. */
    const val MAX_RECENT_NOTES = 2

    /** Waits out a run of autosaves (a title typed letter by letter) before republishing. */
    private const val SETTLE_MS = 1_000L

    fun noteShortcutId(noteId: String): String = NOTE_ID_PREFIX + noteId

    fun noteIdOf(shortcutId: String): String? =
        shortcutId.takeIf { it.startsWith(NOTE_ID_PREFIX) }?.removePrefix(NOTE_ID_PREFIX)

    /** A note as the launcher menu shows it. */
    data class RecentNote(val noteId: String, val title: String)

    /**
     * The notes that belong in the menu, newest edit first. None unless [enabled].
     * Locked, trashed, archived notes and sync conflict copies never qualify, even
     * though the list this is given already leaves most of them out.
     */
    fun recentNotes(notes: List<Note>, enabled: Boolean): List<RecentNote> {
        if (!enabled) return emptyList()
        return notes
            .filter { !it.locked && !it.trashed && !it.archived && !it.isConflictCopy }
            .sortedByDescending { it.updatedAt }
            .take(MAX_RECENT_NOTES)
            .map { RecentNote(it.id, it.title) }
    }

    /** Whether a pinned shortcut to [note] may stay usable. Archived notes may. */
    fun pinnedNoteAllowed(note: Note?): Boolean =
        note != null && !note.locked && !note.trashed

    /**
     * Keeps the menu and any pinned note shortcuts current for as long as the
     * caller collects — `MainActivity` does while it is started, which is the only
     * time notes change: every write path runs inside the app's own screens.
     */
    @OptIn(FlowPreview::class)
    suspend fun follow(
        context: Context,
        recentEnabled: Flow<Boolean>,
        notes: Flow<List<Note>>,
        lookUp: suspend (String) -> Note?
    ) {
        var published: List<RecentNote>? = null
        combine(recentEnabled, notes) { enabled, list -> recentNotes(list, enabled) }
            .debounce(SETTLE_MS)
            .collect { recent ->
                // Pinned first: a note that has just been unlocked still has its
                // disabled pinned shortcut, and the platform refuses to publish a
                // dynamic shortcut under a disabled id until it is enabled again.
                reconcilePinned(context, lookUp)
                // Republished once per collection as well, so a language change
                // (which recreates the activity) relabels the fixed entries.
                if (recent != published) {
                    publish(context, recent)
                    published = recent
                }
            }
    }

    /**
     * Replaces the whole dynamic set, so a note that has left the list leaves the
     * menu in the same call. A failure here is a launcher problem and must not
     * reach the screen that changed the note, so it is swallowed — the next change
     * tries again.
     */
    fun publish(context: Context, recent: List<RecentNote>) {
        runCatching {
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts(context, recent))
        }
    }

    internal fun shortcuts(context: Context, recent: List<RecentNote>): List<ShortcutInfoCompat> =
        buildList {
            add(
                fixed(
                    context,
                    id = ID_NEW_NOTE,
                    label = context.getString(R.string.new_note),
                    icon = R.drawable.ic_shortcut_new_note,
                    action = QuickNoteWidget.ACTION_CREATE_NOTE,
                    rank = 0
                )
            )
            add(
                fixed(
                    context,
                    id = ID_SEARCH,
                    label = context.getString(R.string.search),
                    icon = R.drawable.ic_shortcut_search,
                    action = ACTION_SEARCH,
                    rank = 1
                )
            )
            recent.forEachIndexed { index, note -> add(noteShortcut(context, note, rank = 2 + index)) }
        }

    private fun fixed(
        context: Context,
        id: String,
        label: String,
        icon: Int,
        action: String,
        rank: Int
    ): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithResource(context, icon))
            .setIntent(entryIntent(context, action, id))
            .setRank(rank)
            .build()

    private fun noteShortcut(context: Context, note: RecentNote, rank: Int): ShortcutInfoCompat {
        val id = noteShortcutId(note.noteId)
        val label = note.title.ifBlank { context.getString(R.string.untitled) }
        return ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_note))
            .setIntent(
                entryIntent(context, QuickNoteWidget.ACTION_OPEN_NOTE, id)
                    .putExtra(QuickNoteWidget.EXTRA_NOTE_ID, note.noteId)
            )
            .setRank(rank)
            .build()
    }

    private fun entryIntent(context: Context, action: String, shortcutId: String): Intent =
        Intent(action)
            .setClass(context, MainActivity::class.java)
            .putExtra(EXTRA_SHORTCUT_ID, shortcutId)

    /**
     * Pinned note shortcuts outlive the menu entry they were dragged from, so they
     * are checked against the note itself: disabled while it is locked, trashed or
     * deleted, enabled and relabelled otherwise.
     */
    suspend fun reconcilePinned(context: Context, lookUp: suspend (String) -> Note?) {
        val pinned = runCatching {
            ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
        }.getOrNull() ?: return
        val changes = pinnedChanges(pinned.map { PinnedShortcut(it.id, it.isEnabled) }, lookUp)
        runCatching {
            if (changes.disable.isNotEmpty()) {
                // A disabled shortcut is greyed out but keeps its label, and the
                // label is the note's title. Overwrite it while it can still be
                // updated, so a locked note's title leaves the home screen too.
                ShortcutManagerCompat.updateShortcuts(context, changes.disable.map { redacted(context, it) })
                ShortcutManagerCompat.disableShortcuts(
                    context,
                    changes.disable,
                    context.getString(R.string.shortcut_note_unavailable)
                )
            }
            if (changes.refresh.isNotEmpty()) {
                val shortcuts = changes.refresh.map { noteShortcut(context, it, rank = 0) }
                // Enabled first: an update is not applied to a disabled shortcut.
                ShortcutManagerCompat.enableShortcuts(context, shortcuts)
                ShortcutManagerCompat.updateShortcuts(context, shortcuts)
            }
        }
    }

    /** [shortcutId]'s note shortcut with nothing of the note left in its label. */
    private fun redacted(context: Context, shortcutId: String): ShortcutInfoCompat {
        val label = context.getString(R.string.shortcut_note_hidden_label)
        return ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIntent(entryIntent(context, QuickNoteWidget.ACTION_OPEN_NOTE, shortcutId))
            .build()
    }

    /** A pinned shortcut as the launcher reports it. */
    data class PinnedShortcut(val id: String, val enabled: Boolean)

    /** What [reconcilePinned] will do: switch these off, and re-enable and relabel those. */
    data class PinnedChanges(val disable: List<String>, val refresh: List<RecentNote>)

    /**
     * The decision behind [reconcilePinned], apart from the platform calls. Only
     * note shortcuts are judged; one already disabled is not disabled again.
     */
    suspend fun pinnedChanges(
        pinned: List<PinnedShortcut>,
        lookUp: suspend (String) -> Note?
    ): PinnedChanges {
        val disable = mutableListOf<String>()
        val refresh = mutableListOf<RecentNote>()
        for (shortcut in pinned) {
            val noteId = noteIdOf(shortcut.id) ?: continue
            val note = lookUp(noteId)
            if (note != null && pinnedNoteAllowed(note)) {
                refresh += RecentNote(noteId, note.title)
            } else if (shortcut.enabled) {
                disable += shortcut.id
            }
        }
        return PinnedChanges(disable, refresh)
    }

    /** Lets the launcher rank the menu by what is actually used. */
    fun reportUsed(context: Context, intent: Intent?) {
        val id = intent?.getStringExtra(EXTRA_SHORTCUT_ID) ?: return
        runCatching { ShortcutManagerCompat.reportShortcutUsed(context, id) }
    }
}
