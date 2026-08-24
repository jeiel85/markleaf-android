package com.markleaf.notes.res

import com.markleaf.notes.LocaleManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ResourceParityTest {
    @Test
    fun localizedStringResourcesContainAllDefaultKeys() {
        val defaultKeys = stringNames("src/main/res/${LocaleManifest.source.resDir}/strings.xml")

        LocaleManifest.translated.map { "src/main/res/${it.resDir}/strings.xml" }.forEach { path ->
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
     * Which languages have starter notes is the `starter` column of
     * `config/locales.tsv`, and `zh` is deliberately `no` there: the Chinese
     * contribution (#294) covers `strings.xml` only, so a Chinese device falls
     * back to the English `raw/starter_notes.md` on first launch — degraded,
     * but working.
     *
     * Writing that down is the point, and the column is now the place it is
     * written: an omission left silent reads as an oversight the next time
     * someone counts the directories. `scripts/verify-locales.ps1` checks the
     * other direction — a `raw-<code>` directory whose column says `no`.
     */
    @Test
    fun localizedStarterNotesExist() {
        LocaleManifest.entries
            .filter { it.hasStarterNotes }
            .map { "src/main/res/${it.rawDir}/starter_notes.md" }
            .forEach { path ->
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
