package com.markleaf.notes.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The note → editor container transform, driven through a real NavHost with the
 * production helpers ([noteSharedBounds], [NoteContainerTarget]) and the
 * production hop rule ([opensOrClosesNoteContainer]).
 *
 * The clock is stepped by hand, so this pins two things without judging how the
 * motion looks (that is a device question):
 *
 *  - It *settles*. With `None` on both screens' NavHost transitions, the shared
 *    pair and the content fade are all that drive the hop; the tests check the
 *    editor ends up shown, the list is gone, and back reverses it.
 *  - It is *paired*. Settling alone cannot tell a working pair from a broken one:
 *    an editor whose key matches nothing settles just as happily, it merely
 *    covers the window from its first frame instead of growing out of the row.
 *    So one note ("b") is deliberately given a key that cannot match, and the
 *    hop of a properly paired note is required to outlast it on the same clock.
 *    Where semantics bounds or pixel capture would be the natural probe, neither
 *    works here — the container's layout size is fixed and a paused clock makes
 *    `captureToImage` time out — but the outgoing list's disposal time does.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class NoteContainerTransitionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navController: NavHostController
    private var origin by mutableStateOf<NoteOrigin?>(null)
    private var rowAVisible by mutableStateOf(true)

    private fun setUpHarness() {
        composeRule.setContent {
            MarkleafTheme(darkTheme = false, dynamicColor = false) {
                val nav = rememberNavController().also { navController = it }
                SharedTransitionLayout {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                        NavHost(
                            navController = nav,
                            startDestination = NavRoutes.NOTES,
                            // The host's slide is replaced by a fade here: what
                            // matters is that the hop rule switches to None.
                            enterTransition = {
                                if (opensOrClosesNoteContainer(origin)) EnterTransition.None
                                else fadeIn(tween(280))
                            },
                            exitTransition = {
                                if (opensOrClosesNoteContainer(origin)) ExitTransition.None
                                else fadeOut(tween(280))
                            },
                            popEnterTransition = {
                                if (opensOrClosesNoteContainer(origin)) EnterTransition.None
                                else fadeIn(tween(280))
                            },
                            popExitTransition = {
                                if (opensOrClosesNoteContainer(origin)) ExitTransition.None
                                else fadeOut(tween(280))
                            }
                        ) {
                            composable(NavRoutes.NOTES) {
                                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                                    Column(Modifier.fillMaxSize()) {
                                        for (id in listOf("a", "b", "c")) {
                                            if (id == "a" && !rowAVisible) continue
                                            Text(
                                                text = "Row ${id.uppercase()}",
                                                modifier = Modifier
                                                    .noteSharedBounds(id)
                                                    .fillMaxWidth()
                                                    .height(72.dp)
                                                    .clickable {
                                                        origin = NoteOrigin(id, NoteSource.LIST)
                                                        nav.navigate(NavRoutes.editorRoute(id))
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                            composable(NavRoutes.EDITOR) { entry ->
                                val id = entry.arguments?.getString("noteId")
                                val current = origin
                                val body: @Composable () -> Unit = {
                                    Text("Editor ${id?.uppercase()}", Modifier.fillMaxSize())
                                }
                                if (id != null && current != null && current.noteId == id) {
                                    NoteContainerTarget(
                                        // Note "b" is the control: its key cannot match its
                                        // row, so it stands in for a pair that failed to form.
                                        sharedKey = current.source.keyFor(id) +
                                            if (id == UNPAIRED_NOTE) "-unpaired" else "",
                                        startSurface = current.source.startSurface(MaterialTheme.colorScheme),
                                        animatedVisibilityScope = this,
                                        content = body
                                    )
                                } else {
                                    body()
                                }
                            }
                        }
                    }
                }
            }
        }
        composeRule.mainClock.autoAdvance = false
    }

    private fun settle() = composeRule.mainClock.advanceTimeBy(1_500)

    @Test
    fun tappingARowOpensTheEditorAndTheListLeavesOnceItSettles() {
        setUpHarness()
        composeRule.onNodeWithText("Row A").assertIsDisplayed()

        composeRule.onNodeWithText("Row A").performClick()
        settle()

        composeRule.onNodeWithText("Editor A").assertIsDisplayed()
        // With None on both sides, a list that never left would be the tell that
        // the shared pair did not register.
        composeRule.onNodeWithText("Row A").assertDoesNotExist()
        composeRule.onNodeWithText("Row B").assertDoesNotExist()
    }

    @Test
    fun goingBackReturnsToTheSameListAndDropsTheEditor() {
        setUpHarness()
        composeRule.onNodeWithText("Row A").performClick()
        settle()

        composeRule.runOnUiThread { navController.popBackStack() }
        settle()

        composeRule.onNodeWithText("Row A").assertIsDisplayed()
        composeRule.onNodeWithText("Row B").assertIsDisplayed()
        composeRule.onNodeWithText("Editor A").assertDoesNotExist()
    }

    @Test
    fun aReturnWhoseSourceIsGoneStillLandsOnTheList() {
        setUpHarness()
        composeRule.onNodeWithText("Row A").performClick()
        settle()

        // The edit moved the note out of the list (or out of the search results):
        // when the editor closes there is no row to shrink into.
        composeRule.runOnUiThread { rowAVisible = false }
        composeRule.runOnUiThread { navController.popBackStack() }
        settle()

        composeRule.onNodeWithText("Row B").assertIsDisplayed()
        composeRule.onNodeWithText("Row A").assertDoesNotExist()
        composeRule.onNodeWithText("Editor A").assertDoesNotExist()
    }

    @Test
    fun theListAndTheEditorAreBothOnScreenPartWayThrough() {
        setUpHarness()
        composeRule.onNodeWithText("Row A").performClick()
        composeRule.mainClock.advanceTimeBy(80)

        // Mid-hop nothing has been dropped yet: the outgoing list is still
        // composed under the growing editor.
        composeRule.onNodeWithText("Editor A").assertExists()
        composeRule.onNodeWithText("Row B").assertExists()

        settle()
        composeRule.onNodeWithText("Editor A").assertIsDisplayed()
    }

    /** Milliseconds until the outgoing list ("Row C" is never shared) is disposed. */
    private fun millisUntilListIsGone(): Int {
        var elapsed = 0
        while (elapsed < 2_000 &&
            composeRule.onAllNodesWithText("Row C").fetchSemanticsNodes().isNotEmpty()
        ) {
            composeRule.mainClock.advanceTimeBy(FRAME_MS)
            elapsed += FRAME_MS.toInt()
        }
        return elapsed
    }

    @Test
    fun aPairedHopRunsLongerThanOneWhoseSharedBoundsNeverFormedAPair() {
        setUpHarness()

        composeRule.onNodeWithText("Row A").performClick()
        val paired = millisUntilListIsGone()
        composeRule.runOnUiThread { navController.popBackStack() }
        settle()

        composeRule.onNodeWithText("Row B").performClick()
        val unpaired = millisUntilListIsGone()

        // The pair drives the bounds spring, which outlasts the content fades that
        // are all an unpaired editor has. Measured on the same clock in the same
        // test, so this needs no absolute duration and survives retuning the
        // spring; it fails when the source and target keys stop agreeing —
        // the editor would then cover the window from its first frame.
        assertTrue(
            "paired hop ended after ${paired}ms, unpaired after ${unpaired}ms",
            paired >= unpaired + 3 * FRAME_MS
        )
    }

    @Test
    fun aNoteOpenedWithoutATappedRowStillOpensThroughTheNormalNavigation() {
        setUpHarness()
        // A widget or share intent: navigation with no origin recorded.
        composeRule.runOnUiThread { navController.navigate(NavRoutes.editorRoute("a")) }
        settle()

        composeRule.onNodeWithText("Editor A").assertIsDisplayed()
        composeRule.onNodeWithText("Row A").assertDoesNotExist()
        assertEquals(null, origin)
    }

    @Test
    fun backThenAnotherRowOpensThatNoteCleanly() {
        setUpHarness()
        composeRule.onNodeWithText("Row A").performClick()
        settle()
        composeRule.runOnUiThread { navController.popBackStack() }
        settle()

        composeRule.onNodeWithText("Row B").performClick()
        settle()

        composeRule.onNodeWithText("Editor B").assertIsDisplayed()
        composeRule.onNodeWithText("Editor A").assertDoesNotExist()
        composeRule.onNodeWithText("Row B").assertDoesNotExist()
    }

    private companion object {
        const val UNPAIRED_NOTE = "b"
        const val FRAME_MS = 16L
    }
}
