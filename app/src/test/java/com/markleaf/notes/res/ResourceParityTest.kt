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
            "src/main/res/values-es/strings.xml"
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

    @Test
    fun localizedStarterNotesExist() {
        listOf(
            "src/main/res/raw/starter_notes.md",
            "src/main/res/raw-ko/starter_notes.md",
            "src/main/res/raw-es/starter_notes.md"
        ).forEach { path ->
            val file = File(path)
            assertTrue("$path should exist", file.exists())
            assertEquals(4, file.readText().split("---markleaf-note---").size)
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
