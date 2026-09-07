package com.markleaf.notes.widget

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The launcher can enter widget configuration without starting MainActivity.
 * Keep the first-run seed at that entry point so the note picker is not a
 * misleading empty state on a fresh install.
 */
class SingleNoteWidgetConfigureActivityTest {

    @Test
    fun `configuration activity seeds starter notes independently`() {
        val source = File(
            "src/main/java/com/markleaf/notes/widget/SingleNoteWidgetConfigureActivity.kt"
        ).readText()

        assertTrue(
            "Widget configuration must seed first-run starter notes before observing the picker.",
            source.contains("StarterNotesSeeder.seedIfNeeded")
        )
    }
}
