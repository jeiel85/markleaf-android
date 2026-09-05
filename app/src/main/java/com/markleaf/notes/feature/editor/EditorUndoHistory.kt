package com.markleaf.notes.feature.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

internal enum class UndoAction { UNDO, REDO }

/**
 * The hardware-keyboard undo keymap, kept beside the stack it drives for the
 * same reason the formatting one lives beside its actions.
 *
 * Ctrl+Shift+Z and Ctrl+Y both redo: the first is what Android and Linux
 * editors use, the second is what a keyboard carried over from Windows will
 * reach for. Meta is accepted alongside Ctrl so a Mac-style tablet keyboard
 * behaves the same way, matching [toFormattingAction].
 */
internal fun undoShortcutFor(key: Key, shiftPressed: Boolean): UndoAction? = when (key) {
    Key.Z -> if (shiftPressed) UndoAction.REDO else UndoAction.UNDO
    Key.Y -> if (shiftPressed) null else UndoAction.REDO
    else -> null
}

/** Resolves a key event to an undo action, or null when it is not one. */
internal fun KeyEvent.toUndoAction(): UndoAction? =
    if (isCtrlPressed || isMetaPressed) undoShortcutFor(key, isShiftPressed) else null

/** How many editing steps the editor can walk back through. */
private const val MAX_ENTRIES = 100

/**
 * Total characters the stack may hold. Each step keeps a whole copy of the
 * note, so a long note plus a long stack is real memory — this caps it at
 * roughly a megabyte of text and drops the oldest steps first.
 */
private const val MAX_RETAINED_CHARS = 1_000_000

/** Never trim below this, or there would be nothing left to undo to. */
private const val MIN_ENTRIES = 2

/** A pause longer than this ends the current typing run. */
private const val COALESCE_WINDOW_MS = 700L

/** The largest single change that still counts as typing rather than an edit. */
private const val COALESCE_MAX_SPAN = 8

/** How much typing may collapse into one step before a new one is started. */
private const val COALESCE_MAX_RUN = 48

/** One point the editor can be returned to. */
internal data class EditorUndoSnapshot(val text: String, val selection: TextRange) {
    fun toValue(): TextFieldValue = TextFieldValue(
        text = text,
        selection = TextRange(
            selection.start.coerceIn(0, text.length),
            selection.end.coerceIn(0, text.length)
        )
    )
}

/**
 * The editor's undo/redo stack (#360).
 *
 * Markleaf autosaves, so an accidental "select all and type" used to be
 * permanent the moment the debounce fired — there was no earlier version
 * anywhere to go back to. This keeps the recent ones in memory for the life of
 * the open note.
 *
 * The screen does not push steps by hand at each of its edit sites; it feeds
 * every value the text field takes to [record] and this class decides what is a
 * step. That is deliberate — an edit path added later (a new formatting action,
 * a new completion) is undoable without anyone remembering to wire it up.
 *
 * What counts as one step:
 *  - A run of ordinary typing collapses into a single step while it stays
 *    small, unbroken and within [COALESCE_WINDOW_MS] of the last keystroke.
 *    Newlines, pauses, and runs past [COALESCE_MAX_RUN] characters end it, so
 *    undo walks back in paragraphs rather than in one erasing jump. So do a
 *    switch between inserting and deleting, and a caret that moved somewhere
 *    else first — a run is one continuous act of typing in one place.
 *  - Anything larger — a paste, a replace-all, a formatting action, typing over
 *    a selection — is its own step, which is the case the report was about.
 *  - Moving the caret is not a step. The stack tracks text; a selection-only
 *    change just refreshes where the current step's caret sits.
 *
 * Not thread-safe: it is driven from the composition, on the main thread.
 */
internal class EditorUndoHistory(
    private val maxEntries: Int = MAX_ENTRIES,
    private val maxRetainedChars: Int = MAX_RETAINED_CHARS,
    private val coalesceWindowMillis: Long = COALESCE_WINDOW_MS,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val entries = mutableListOf<EditorUndoSnapshot>()

    /** Where in [entries] the editor currently sits; -1 until seeded. */
    private var index = -1
    private var retainedChars = 0

    /** True while the newest entry is an open typing run more may merge into. */
    private var runOpen = false
    private var runKind = ChangeKind.REPLACE
    private var runStartedAt = 0L
    private var runChars = 0

    /** Set by [beginNewStep]: the next text change stands alone, both ways. */
    private var isolateNextStep = false

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    /**
     * Makes [value] the baseline: the state undo can never go past. Called when
     * a note is loaded, so opening a note does not offer to undo into the empty
     * text the field held before the row arrived.
     */
    fun reset(value: TextFieldValue) {
        entries.clear()
        retainedChars = 0
        add(value.toSnapshot())
        index = 0
        endRun()
        isolateNextStep = false
        syncFlags()
    }

    /**
     * Declares that the next text change is an action of its own, not typing.
     *
     * Only the app can tell the difference. A formatting action that inserts
     * `"# "`, a quick insert, a completion — from the value stream these look
     * exactly like two keystrokes, so without this the keystroke that follows
     * one merges into it and a single undo takes back both.
     *
     * Called by the screen immediately before it applies such an edit.
     * Forgetting it degrades to a merge rather than losing a step, which is why
     * this is a hint rather than the recording mechanism.
     */
    fun beginNewStep() {
        endRun()
        isolateNextStep = true
    }

    /**
     * Feeds the text field's current value in. Cheap and idempotent for values
     * that carry no text change, so the caller can hand it every value.
     */
    fun record(value: TextFieldValue) {
        val current = entries.getOrNull(index)
        if (current == null) {
            reset(value)
            return
        }
        if (current.text == value.text) {
            // Caret moved, text did not. Keep the caret fresh on the current
            // step so a later undo/redo lands where the user actually is, and
            // close the typing run: what is typed after the caret has been
            // moved is a new act of writing, not a continuation of the last.
            if (current.selection != value.selection) {
                replace(index, current.copy(selection = value.selection))
                endRun()
            }
            return
        }
        val timestamp = now()
        val change = TextChange.between(current.text, value.text)
        val continuesRun = runOpen &&
            index == entries.lastIndex &&
            timestamp - runStartedAt <= coalesceWindowMillis &&
            change.kind == runKind &&
            change.isTypingSized &&
            change.continuesFrom(current.selection) &&
            runChars + change.weight <= COALESCE_MAX_RUN
        if (continuesRun) {
            replace(index, value.toSnapshot())
            runChars += change.weight
        } else {
            // Editing after an undo abandons what was undone, the same as every
            // other editor: the redo tail is no longer reachable.
            if (index < entries.lastIndex) dropRedoTail()
            add(value.toSnapshot())
            index = entries.lastIndex
            if (change.isTypingSized && !isolateNextStep) {
                runOpen = true
                runKind = change.kind
                runChars = change.weight
            } else {
                endRun()
            }
            isolateNextStep = false
            trim()
        }
        if (runOpen) runStartedAt = timestamp
        syncFlags()
    }

    /** The previous state, or null when there is nothing left to undo. */
    fun undo(): TextFieldValue? {
        if (index <= 0) return null
        index--
        endRun()
        syncFlags()
        return entries[index].toValue()
    }

    /** The state an [undo] stepped out of, or null when there is none. */
    fun redo(): TextFieldValue? {
        if (index < 0 || index >= entries.lastIndex) return null
        index++
        endRun()
        syncFlags()
        return entries[index].toValue()
    }

    private fun add(snapshot: EditorUndoSnapshot) {
        entries += snapshot
        retainedChars += snapshot.text.length
    }

    private fun replace(at: Int, snapshot: EditorUndoSnapshot) {
        retainedChars += snapshot.text.length - entries[at].text.length
        entries[at] = snapshot
    }

    private fun dropRedoTail() {
        while (entries.lastIndex > index) {
            retainedChars -= entries.removeAt(entries.lastIndex).text.length
        }
    }

    private fun trim() {
        while (
            entries.size > MIN_ENTRIES &&
            (entries.size > maxEntries || retainedChars > maxRetainedChars)
        ) {
            retainedChars -= entries.removeAt(0).text.length
            index--
        }
    }

    private fun endRun() {
        runOpen = false
        runKind = ChangeKind.REPLACE
        runChars = 0
    }

    private fun syncFlags() {
        canUndo = index > 0
        canRedo = index >= 0 && index < entries.lastIndex
    }

    private fun TextFieldValue.toSnapshot() = EditorUndoSnapshot(text, selection)
}

private enum class ChangeKind { INSERT, DELETE, REPLACE }

/** The single contiguous span that differs between two versions of the text. */
private data class TextChange(val start: Int, val removed: String, val inserted: String) {
    val weight: Int = maxOf(removed.length, inserted.length)

    /**
     * REPLACE covers typing or pasting over a selection and every formatting
     * transformation. Those are never part of a typing run however small they
     * are: merging one would make undo skip past the text it replaced, which is
     * the whole complaint in miniature.
     */
    val kind: ChangeKind = when {
        removed.isEmpty() -> ChangeKind.INSERT
        inserted.isEmpty() -> ChangeKind.DELETE
        else -> ChangeKind.REPLACE
    }

    /**
     * A change small enough, and local enough, to be part of a typing run. A
     * newline on either side ends the run so undo stops at paragraph edges, and
     * the span allowance leaves room for an IME committing a word at once.
     */
    val isTypingSized: Boolean = kind != ChangeKind.REPLACE &&
        weight <= COALESCE_MAX_SPAN &&
        !removed.contains('\n') &&
        !inserted.contains('\n')

    /**
     * True when this change happened at the caret the previous state left
     * behind. Moving the caret and typing somewhere else is a new step, even
     * within the time window — otherwise one undo would take back two edits
     * made in different places.
     */
    fun continuesFrom(previous: TextRange): Boolean = previous.collapsed && when (kind) {
        ChangeKind.INSERT -> start == previous.start
        // Backspace removes up to the caret; forward delete removes from it.
        ChangeKind.DELETE -> start == previous.start || start + removed.length == previous.start
        ChangeKind.REPLACE -> false
    }

    companion object {
        fun between(old: String, new: String): TextChange {
            val shortest = minOf(old.length, new.length)
            var prefix = 0
            while (prefix < shortest && old[prefix] == new[prefix]) prefix++
            var suffix = 0
            while (
                suffix < shortest - prefix &&
                old[old.length - 1 - suffix] == new[new.length - 1 - suffix]
            ) {
                suffix++
            }
            return TextChange(
                start = prefix,
                removed = old.substring(prefix, old.length - suffix),
                inserted = new.substring(prefix, new.length - suffix)
            )
        }
    }
}
