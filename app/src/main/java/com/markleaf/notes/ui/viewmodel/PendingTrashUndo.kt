package com.markleaf.notes.ui.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The Undo offered after notes were moved to the trash, kept by the screen's
 * view model rather than by the snackbar's coroutine (#262, v2.74.0).
 *
 * The snackbar used to run on the list's composition scope, so opening a note
 * or Settings before it timed out cancelled it, and coming back showed nothing:
 * the notes were safe in the trash, but the quick way back was gone without a
 * sign. The view model lives as long as the screen's back stack entry, so the
 * screen shows the snackbar again for whatever is still pending when it
 * returns. An Undo is cleared once the snackbar has run to its end — tapped
 * or timed out — or when a newer move replaces it.
 */
class PendingTrashUndo {

    /** One move to the trash. [serial] keeps two moves of the same notes apart. */
    data class Move(val noteIds: List<String>, val serial: Long)

    private val _move = MutableStateFlow<Move?>(null)
    val move: StateFlow<Move?> = _move
    private var serial = 0L

    fun offer(noteIds: List<String>) {
        _move.value = Move(noteIds, ++serial)
    }

    /**
     * The snackbar for [move] has finished; true when it is still the pending
     * one, i.e. the caller should act on the answer. A stale answer — from a
     * snackbar a newer move already replaced — returns false.
     */
    fun settle(move: Move): Boolean {
        if (_move.value != move) return false
        _move.value = null
        return true
    }
}
