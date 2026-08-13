package com.markleaf.notes.feature.editor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The debounce contract behind the editor's autosave (#262).
 *
 * The screen bumps [DebouncedSaver.requestSave] on every edit and formatting
 * action, including rapid taps on a preview checkbox. Two properties must hold
 * or a user-visible change could be reverted by a stale save:
 *
 * 1. **A burst coalesces into one save.** A second request inside the debounce
 *    window cancels the pending save instead of queueing it, so an older text
 *    can never land after a newer one.
 * 2. **The saved text is the text at fire time, not at request time.** The
 *    content is read after the delay, so typing during the quiet second is
 *    included in the single save.
 *
 * These are timing/ordering properties, so the tests drive the virtual clock
 * of `runTest` rather than a device: the collector runs in `backgroundScope`
 * and the test advances time explicitly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DebouncedSaverTest {

    @Test
    fun rapidRequests_coalesceIntoSingleSaveOfTheLatestText() = runTest {
        val saved = mutableListOf<String>()
        val content = mutableListOf("")
        val saver = DebouncedSaver(
            debounceMillis = 1_000,
            readContent = { content.last() },
            save = { saved += it }
        )
        backgroundScope.launch { saver.run() }

        // A burst of preview checkbox taps inside the one-second window: the
        // user flips a task on, off, and on again faster than the debounce.
        content[0] = "- [x] a"
        saver.requestSave()
        advanceTimeBy(200)
        content[0] = "- [ ] a"
        saver.requestSave()
        advanceTimeBy(200)
        content[0] = "- [x] a"
        saver.requestSave()
        advanceTimeBy(1_100)
        runCurrent()

        // Exactly one save, and it is the text as of the last tap — the
        // intermediate states were never persisted, so nothing can "revert".
        assertEquals(listOf("- [x] a"), saved)
    }

    @Test
    fun contentIsReadWhenTheTimerFires_notWhenTheRequestArrived() = runTest {
        val saved = mutableListOf<String>()
        val content = mutableListOf("")
        val saver = DebouncedSaver(
            debounceMillis = 1_000,
            readContent = { content.last() },
            save = { saved += it }
        )
        backgroundScope.launch { saver.run() }

        content[0] = "first"
        saver.requestSave()
        // The user keeps typing during the quiet second; the save must pick up
        // the final text, not the text that was current when the edit landed.
        advanceTimeBy(500)
        content[0] = "first second"
        advanceTimeBy(600)
        runCurrent()

        assertEquals(listOf("first second"), saved)
    }

    @Test
    fun noRequests_noSave() = runTest {
        var saves = 0
        val saver = DebouncedSaver(
            debounceMillis = 1_000,
            readContent = { "content" },
            save = { saves++ }
        )
        backgroundScope.launch { saver.run() }

        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(0, saves)
    }

    @Test
    fun aRequestAfterASaveFiresAnotherSave() = runTest {
        val saved = mutableListOf<String>()
        val content = mutableListOf("")
        val saver = DebouncedSaver(
            debounceMillis = 1_000,
            readContent = { content.last() },
            save = { saved += it }
        )
        backgroundScope.launch { saver.run() }

        content[0] = "one"
        saver.requestSave()
        advanceTimeBy(1_100)
        runCurrent()
        assertEquals(listOf("one"), saved)

        // A later, separate edit is its own save — coalescing only applies
        // within a burst, not across quiet periods.
        content[0] = "two"
        saver.requestSave()
        advanceTimeBy(1_100)
        runCurrent()
        assertEquals(listOf("one", "two"), saved)
    }

    @Test
    fun aRequestBeforeRunStarts_isStillSaved() = runTest {
        val saved = mutableListOf<String>()
        val saver = DebouncedSaver(
            debounceMillis = 1_000,
            readContent = { "content" },
            save = { saved += it }
        )
        // The screen only starts the collector once the note is loaded, but an
        // edit can land before that (e.g. an image picker result). The pending
        // trigger must not be lost when the collector starts.
        saver.requestSave()
        backgroundScope.launch { saver.run() }

        advanceTimeBy(1_100)
        runCurrent()

        assertEquals(listOf("content"), saved)
    }
}