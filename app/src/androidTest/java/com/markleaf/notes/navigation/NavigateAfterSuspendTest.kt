package com.markleaf.notes.navigation

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.markleaf.notes.R
import com.markleaf.notes.TestHostActivity
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.ui.createInMemoryMarkleafDatabase
import com.markleaf.notes.ui.markleafTestContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Guards the one rule the navigation host has to obey: `NavController` is driven
 * from the main thread.
 *
 * Every entry point that opens a note first suspends into Room — creating the
 * note, or resolving where an incoming id should land — and Room resumes its
 * continuations on its own executor. The app's dispatcher happens to hop back to
 * the main thread, so this never misbehaved in production; under Compose's test
 * dispatcher it did not, and `navigate()` ran on a Room thread. That threw, left
 * the new back-stack entry stuck in `INITIALIZED`, and the next activity destroy
 * died with "State must be at least CREATED to move to DESTROYED" — taking the
 * rest of the instrumentation run with it. The whole suite aborted at 17 of 55
 * tests and had done for a long time (#235).
 *
 * The failure is at teardown, so these tests assert nothing: reaching the end of
 * the method without the process dying *is* the assertion. Deleting
 * `navigateOnMain` and running this class reproduces the crash.
 */
@RunWith(AndroidJUnit4::class)
class NavigateAfterSuspendTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun launch(): Pair<ActivityScenario<TestHostActivity>, AppDatabase> {
        val database = createInMemoryMarkleafDatabase()
        TestHostActivity.content = markleafTestContent(database)
        val scenario = ActivityScenario.launch(TestHostActivity::class.java)
        composeTestRule.waitForIdle()
        return scenario to database
    }

    private fun ActivityScenario<TestHostActivity>.finish(database: AppDatabase) {
        close()
        TestHostActivity.content = null
        database.close()
    }

    @Test
    fun destroyingTheHostFromTheListIsClean() {
        // Baseline. This passed even while the suite was broken, which is what
        // narrowed the cause to the create-a-note path.
        val (scenario, database) = launch()
        scenario.finish(database)
    }

    @Test
    fun destroyingTheHostWhileOnANewlyCreatedNoteIsClean() {
        // The FAB path: createNote() suspends into Room, and the navigate that
        // follows used to run on whatever thread Room resumed on.
        val (scenario, database) = launch()
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.add_note))
            .performClick()
        composeTestRule.waitForIdle()
        scenario.finish(database)
    }

    @Test
    fun destroyingTheHostAfterLeavingTheEditorIsClean() {
        // Same path, then popped: the entry the crash named is the one left
        // behind, so backing out first must not hide the problem either.
        val (scenario, database) = launch()
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.add_note))
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.back))
            .performClick()
        composeTestRule.waitForIdle()
        scenario.finish(database)
    }
}
