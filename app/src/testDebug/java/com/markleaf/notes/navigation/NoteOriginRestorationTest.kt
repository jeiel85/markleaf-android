package com.markleaf.notes.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A note opened from a tapped row keeps what the editor had saved when the phone
 * rotates (#499, v2.63.1).
 *
 * v2.63.0 made the editor keep its reading mode across a rotation and tested it
 * on [com.markleaf.notes.feature.editor.EditorScreen] alone, which passed — while
 * on a device a note opened from the list still came back in Edit on its first
 * rotation. The editor was not the problem; where the host put it was. A note
 * opened from a row is composed inside [NoteContainerTarget], and `remember`ed
 * origin that decides that was lost with the activity, so the rebuilt editor sat
 * one level shallower and `rememberSaveable` looked for its values under a
 * composition path it had never written to.
 *
 * So this drives the production pieces the host uses — [rememberNoteOrigin] and
 * [NoteEditorDestination] — inside a real NavHost and rebuilds the whole thing
 * from the saved-instance-state bundle with [StateRestorationTester], the path a
 * rotation takes. The editor body is a stand-in with one `rememberSaveable`
 * flag: the mechanism under test is how the host places its content, not what
 * the content is.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class NoteOriginRestorationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val restoration = StateRestorationTester(composeRule)
    private lateinit var navController: NavHostController

    private fun showHost() {
        restoration.setContent {
            MarkleafTheme(darkTheme = false, dynamicColor = false) {
                val nav = rememberNavController().also { navController = it }
                var origin by rememberNoteOrigin()
                SharedTransitionLayout {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                        NavHost(navController = nav, startDestination = NavRoutes.NOTES) {
                            composable(NavRoutes.NOTES) {
                                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                                    Column(Modifier.fillMaxSize()) {
                                        Text(
                                            text = "Row A",
                                            modifier = Modifier
                                                .noteSharedBounds("a")
                                                .fillMaxWidth()
                                                .height(72.dp)
                                                .clickable {
                                                    origin = NoteOrigin("a", NoteSource.LIST)
                                                    nav.navigate(NavRoutes.editorRoute("a"))
                                                }
                                        )
                                    }
                                }
                            }
                            composable(NavRoutes.EDITOR) { entry ->
                                NoteEditorDestination(
                                    noteId = entry.arguments?.getString("noteId"),
                                    origin = origin,
                                    animatedVisibilityScope = this
                                ) {
                                    ReadingModeStandIn()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** One saved flag, the way the editor keeps its reading mode. */
    @Composable
    private fun ReadingModeStandIn() {
        var preview by rememberSaveable { mutableStateOf(false) }
        Column(Modifier.fillMaxSize()) {
            Button(onClick = { preview = !preview }) { Text("Toggle") }
            Text(if (preview) "mode: preview" else "mode: edit")
        }
    }

    private fun settle() = composeRule.waitForIdle()

    private fun tap(text: String) {
        composeRule.onNodeWithText(text).performClick()
        settle()
    }

    private fun rotate() {
        restoration.emulateSavedInstanceStateRestore()
        settle()
    }

    @Test
    fun aNoteOpenedFromARowKeepsItsReadingModeAcrossARotation() {
        showHost()
        composeRule.onNodeWithText("Row A").performClick()
        settle()
        composeRule.onNodeWithText("mode: edit").assertIsDisplayed()

        tap("Toggle")
        composeRule.onNodeWithText("mode: preview").assertIsDisplayed()

        rotate()

        composeRule.onNodeWithText("mode: preview").assertIsDisplayed()
    }

    @Test
    fun itKeepsItThroughMoreThanOneRotation() {
        showHost()
        composeRule.onNodeWithText("Row A").performClick()
        settle()
        tap("Toggle")

        rotate()
        rotate()
        composeRule.onNodeWithText("mode: preview").assertIsDisplayed()

        // Back to Edit, then round again: the second rotation must save what the
        // first one restored, not only what the first composition wrote.
        tap("Toggle")
        rotate()
        composeRule.onNodeWithText("mode: edit").assertIsDisplayed()
    }

    @Test
    fun aNoteOpenedWithoutATappedRowKeepsItsReadingModeAcrossARotation() {
        showHost()
        // A widget or share intent: navigation with no origin recorded, so the
        // editor is never wrapped. It must stay unwrapped after the rotation too.
        composeRule.runOnUiThread { navController.navigate(NavRoutes.editorRoute("a")) }
        settle()
        tap("Toggle")
        composeRule.onNodeWithText("mode: preview").assertIsDisplayed()

        rotate()

        composeRule.onNodeWithText("mode: preview").assertIsDisplayed()
    }
}
