package com.markleaf.notes.feature.editor

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update

/**
 * The editor's debounced autosave gate (#262).
 *
 * The editor bumps a trigger on every edit and formatting action; this gate
 * turns a burst of triggers into **one save of the latest text**. Two
 * properties are load-bearing and both live here so they can be tested:
 *
 * 1. **Restart cancels the pending save.** Each [requestSave] restarts the
 *    timer, so a second tap inside the debounce window cancels the first
 *    save rather than queueing it — a stale save can never land after a newer
 *    one and "revert" a change the user already saw take.
 * 2. **Content is read when the timer fires, not when the trigger arrived.**
 *    [readContent] runs after the delay, so the persisted text is always the
 *    text as of the last edit, even if the user typed during the quiet
 *    second.
 *
 * The caller owns the actual persistence ([save]); this class owns only the
 * timing and ordering. [run] is a long-lived collector — launch it once per
 * note and keep it alive for the note's lifetime.
 */
internal class DebouncedSaver(
    private val debounceMillis: Long,
    private val readContent: () -> String,
    private val save: suspend (String) -> Unit
) {
    private val triggers = MutableStateFlow(0)

    /** Bump the trigger. Safe from any thread; only the fact that it changed matters. */
    fun requestSave() {
        triggers.update { it + 1 }
    }

    suspend fun run() {
        triggers.collectLatest { count ->
            if (count > 0) {
                delay(debounceMillis)
                save(readContent())
            }
        }
    }
}