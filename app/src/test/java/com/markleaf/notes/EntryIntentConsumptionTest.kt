package com.markleaf.notes

import android.content.Intent
import android.os.Bundle
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The launching intent is a one-shot request (a widget tap, a share, the Notes-role
 * action) and has to be acted on once. Android hands a recreated activity the same
 * intent again — on a rotation, a theme change, the process coming back — and
 * acting on it a second time imported a share twice and threw the screen you were
 * writing on over to a fresh blank note. Found by a review of the Notes-role
 * change (#481); the widget and share paths had the same replay before it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EntryIntentConsumptionTest {

    private val intent = Intent("android.intent.action.CREATE_NOTE")

    private fun saved(consumed: Boolean) = Bundle().apply {
        putBoolean(STATE_ENTRY_INTENT_CONSUMED, consumed)
    }

    @Test
    fun aFirstLaunchHandsTheIntentThrough() {
        assertSame(intent, intent.unlessConsumedBy(null))
    }

    @Test
    fun aRecreationAfterTheIntentWasActedOnDoesNotSeeItAgain() {
        assertNull(intent.unlessConsumedBy(saved(consumed = true)))
    }

    @Test
    fun aRequestNotYetActedOnIsHandedThrough() {
        // What onNewIntent leaves behind before recreate(): the request is new, so the
        // instance it builds must act on it.
        assertSame(intent, intent.unlessConsumedBy(saved(consumed = false)))
    }

    @Test
    fun savedStateThatNeverMentionedTheIntentIsAFirstLaunch() {
        assertSame(intent, intent.unlessConsumedBy(Bundle()))
    }

    @Test
    fun anActivityThatHasLaunchedSavesThatItActedOnTheIntent() {
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent).setup()
        val state = Bundle()

        controller.saveInstanceState(state)

        assertTrue(
            "the state a rotation restores from must say the intent was acted on",
            state.getBoolean(STATE_ENTRY_INTENT_CONSUMED)
        )
        controller.close()
    }
}
