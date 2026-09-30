package com.markleaf.notes

import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.widget.QuickNoteWidget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Markleaf as the device's Notes app (#481): the system's `CREATE_NOTE` action
 * has to reach [MainActivity], has to open a blank note like the widget's "+"
 * does, and must never make the whole app visible over the lock screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotesRoleIntentTest {

    @Test
    fun theSystemCreateNoteActionAsksForANewNote() {
        assertTrue(Intent("android.intent.action.CREATE_NOTE").requestsNewNote())
    }

    @Test
    fun theWidgetsNewNoteActionStillAsksForANewNote() {
        assertTrue(Intent(QuickNoteWidget.ACTION_CREATE_NOTE).requestsNewNote())
    }

    @Test
    fun otherIntentsDoNotAskForANewNote() {
        assertFalse(Intent().requestsNewNote())
        assertFalse(Intent(Intent.ACTION_MAIN).requestsNewNote())
        assertFalse(Intent(Intent.ACTION_SEND).requestsNewNote())
        assertFalse(Intent(Intent.ACTION_VIEW).requestsNewNote())
        assertFalse(Intent(QuickNoteWidget.ACTION_OPEN_NOTE).requestsNewNote())
    }

    @Test
    fun theSystemActionConstantMatchesThePlatformValue() {
        // Spelled as a string because Intent.ACTION_CREATE_NOTE is API 34 and
        // minSdk is 26. This is the one place the two are compared, on an SDK
        // where the constant exists.
        assertEquals(Intent.ACTION_CREATE_NOTE, ACTION_CREATE_NOTE_SYSTEM)
    }

    @Test
    fun theMergedManifestResolvesCreateNoteToMainActivity() {
        // The probe RoleManager runs to decide whether an app may hold the Notes
        // role: an activity with this action and the DEFAULT category.
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val probe = Intent(Intent.ACTION_CREATE_NOTE).addCategory(Intent.CATEGORY_DEFAULT)
            .setPackage(context.packageName)

        val matches = context.packageManager.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY)

        assertEquals(
            listOf(MainActivity::class.java.name),
            matches.map { it.activityInfo.name }
        )
    }

    @Test
    fun mainActivityIsNeverDeclaredToShowOverTheLockScreen() {
        // MainActivity is the whole app: the notes list, search, every note. The
        // Notes role's lock-screen entry needs showWhenLocked, and putting it here
        // would put all of that in front of anyone holding a locked phone. If
        // lock-screen capture is ever wanted, it goes in its own activity that can
        // create a note and show nothing else - not on this one.
        for (path in listOf(MAIN_MANIFEST, SIDELOAD_MANIFEST)) {
            val main = mainActivityElement(File(path))
            assertNull(
                "$path: MainActivity must not declare android:showWhenLocked",
                main.attributes.getNamedItem("android:showWhenLocked")
            )
            assertNull(
                "$path: MainActivity must not declare android:turnScreenOn",
                main.attributes.getNamedItem("android:turnScreenOn")
            )
        }
    }

    @Test
    fun bothManifestsDeclareTheCreateNoteFilterWithTheDefaultCategory() {
        for (path in listOf(MAIN_MANIFEST, SIDELOAD_MANIFEST)) {
            val filters = mainActivityElement(File(path)).getElementsByTagName("intent-filter")
            val found = (0 until filters.length).map { filters.item(it) as org.w3c.dom.Element }
                .any { filter ->
                    val actions = names(filter, "action")
                    val categories = names(filter, "category")
                    "android.intent.action.CREATE_NOTE" in actions &&
                        "android.intent.category.DEFAULT" in categories
                }
            assertTrue("$path: no CREATE_NOTE + DEFAULT intent-filter on MainActivity", found)
        }
    }

    private fun mainActivityElement(manifest: File): org.w3c.dom.Element {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifest)
        val activities = document.getElementsByTagName("activity")
        val main = (0 until activities.length).map { activities.item(it) as org.w3c.dom.Element }
            .firstOrNull { it.getAttribute("android:name") == ".MainActivity" }
        assertNotNull("${manifest.path}: MainActivity is not declared", main)
        return main!!
    }

    private fun names(filter: org.w3c.dom.Element, tag: String): Set<String> {
        val nodes = filter.getElementsByTagName(tag)
        return (0 until nodes.length).map { (nodes.item(it) as org.w3c.dom.Element).getAttribute("android:name") }.toSet()
    }

    private companion object {
        // The unit-test working directory is the app module; same paths
        // SideloadManifestParityTest reads.
        const val MAIN_MANIFEST = "src/main/AndroidManifest.xml"
        const val SIDELOAD_MANIFEST = "src/main/AndroidManifest-sideload.xml"
    }
}
