package com.markleaf.notes.res

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ResourceParityTest {
    @Test
    fun localizedStringResourcesContainAllDefaultKeys() {
        val defaultKeys = stringNames("src/main/res/values/strings.xml")

        listOf(
            "src/main/res/values-ko/strings.xml",
            "src/main/res/values-es/strings.xml",
            "src/main/res/values-ja/strings.xml",
            "src/main/res/values-de/strings.xml",
            "src/main/res/values-fr/strings.xml",
            "src/main/res/values-zh/strings.xml"
        ).forEach { path ->
            val localizedKeys = stringNames(path)
            assertEquals(
                "Resource count mismatch for $path",
                defaultKeys.size,
                localizedKeys.size
            )
            assertEquals(
                "Missing or extra string resources in $path",
                defaultKeys,
                localizedKeys
            )
        }
    }

    /**
     * `raw-zh` is deliberately not in this list. The Chinese contribution (#294)
     * covers `strings.xml` only, so a Chinese device falls back to the English
     * `raw/starter_notes.md` on first launch — degraded, but working.
     *
     * Writing that down is the point. This list is the only record of which
     * locales have starter notes, so an omission left silent reads as an
     * oversight the next time someone counts the directories. Add `raw-zh` here
     * when the file lands.
     */
    @Test
    fun localizedStarterNotesExist() {
        listOf(
            "src/main/res/raw/starter_notes.md",
            "src/main/res/raw-ko/starter_notes.md",
            "src/main/res/raw-es/starter_notes.md",
            "src/main/res/raw-de/starter_notes.md",
            "src/main/res/raw-ja/starter_notes.md",
            "src/main/res/raw-fr/starter_notes.md"
        ).forEach { path ->
            val file = File(path)
            assertTrue("$path should exist", file.exists())
            assertEquals(6, file.readText().split("---markleaf-note---").size)
        }
    }

    private fun stringNames(path: String): Set<String> {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File(path))
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length)
            .map { index -> nodes.item(index).attributes.getNamedItem("name").nodeValue }
            .toSortedSet()
    }
}
