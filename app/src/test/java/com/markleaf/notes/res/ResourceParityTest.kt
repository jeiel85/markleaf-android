package com.markleaf.notes.res

import com.markleaf.notes.LocaleManifest
import com.markleaf.notes.data.onboarding.StarterNotesSeeder
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
                assertEquals(6, file.readText().split(StarterNotesSeeder.STARTER_NOTE_SEPARATOR).size)
            }
    }

    /**
     * Input: every `<string>` and `<plurals>` item in each translated `strings.xml`.
     * Output: fails naming the resource whose format specifiers differ from the source.
     *
     * Why a unit test when `lintRelease` already checks this: lint runs only in CI,
     * minutes in, while a translation reads as plain prose to every local gate. The
     * Vietnamese `100% cục bộ` passed `verify-locales.ps1`, this class and
     * `assembleDebug`, then failed `lintRelease` (#262 `## v2.45.0`, fixed by hand in
     * 075d065): `c` is a real `java.util.Formatter` conversion, so `% c` is a
     * space flag plus `%c`, a specifier the English source does not have. `100% local`
     * is harmless only because `l` is not a conversion — which is why the check is on
     * *valid* specifiers rather than on every `%`.
     *
     * A plural item may drop its number (`one` reading "a minute ago"), so plurals
     * only have to stay within the source's `other` item; plain strings must match.
     */
    @Test
    fun translatedFormatSpecifiersMatchTheSource() {
        val source = formatSpecifiers("src/main/res/${LocaleManifest.source.resDir}/strings.xml")
        val mismatches = LocaleManifest.translated.flatMap { locale ->
            val path = "src/main/res/${locale.resDir}/strings.xml"
            formatSpecifiers(path).mapNotNull { (key, specs) ->
                val isPlural = key.startsWith(PLURAL_PREFIX)
                val sourceKey = if (isPlural) key.substringBeforeLast('/') + "/other" else key
                val expected = source[sourceKey] ?: return@mapNotNull null
                val ok = if (isPlural) expected.containsAll(specs) else specs == expected
                if (ok) null else "$path $key: $specs, source has $expected"
            }
        }
        assertTrue(
            "Format specifiers differ from the source (a literal % before a conversion letter " +
                "reads as a specifier; reword it or write %%):\n" + mismatches.joinToString("\n"),
            mismatches.isEmpty()
        )
    }

    /** Pins the detector itself, so a regex change cannot quietly turn the check above off. */
    @Test
    fun formatSpecifierDetectorSeesWhatLintSees() {
        assertEquals(listOf("1\$c"), specifiersIn("Xử lý dữ liệu 100% cục bộ"))
        assertEquals(emptyList<String>(), specifiersIn("100% local data processing"))
        assertEquals(emptyList<String>(), specifiersIn("100% 로컬 데이터 처리"))
        assertEquals(emptyList<String>(), specifiersIn("50%% off%n"))
        assertEquals(listOf("1\$d", "2\$s"), specifiersIn("Replaced %1\$d of %2\$s"))
        assertEquals(listOf("1\$s", "2\$d"), specifiersIn("%s and %d"))
    }

    private fun formatSpecifiers(path: String): Map<String, List<String>> {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File(path))
        val result = mutableMapOf<String, List<String>>()
        val strings = document.getElementsByTagName("string")
        for (i in 0 until strings.length) {
            val node = strings.item(i)
            if (node.attributes.getNamedItem("formatted")?.nodeValue == "false") continue
            result[node.attributes.getNamedItem("name").nodeValue] = specifiersIn(node.textContent)
        }
        val plurals = document.getElementsByTagName("plurals")
        for (i in 0 until plurals.length) {
            val node = plurals.item(i)
            val name = node.attributes.getNamedItem("name").nodeValue
            val items = node.childNodes
            for (j in 0 until items.length) {
                val item = items.item(j)
                if (item.nodeName != "item") continue
                val quantity = item.attributes.getNamedItem("quantity").nodeValue
                result["$PLURAL_PREFIX$name/$quantity"] = specifiersIn(item.textContent)
            }
        }
        return result
    }

    private fun specifiersIn(text: String): List<String> {
        var implicit = 0
        return FORMAT_SPECIFIER.findAll(text).mapNotNull { match ->
            val conversion = match.groupValues[2]
            if (conversion == "%" || conversion == "n") return@mapNotNull null
            val position = match.groupValues[1].ifEmpty { (++implicit).toString() }
            "$position\$$conversion"
        }.sorted().toList()
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

    private companion object {
        const val PLURAL_PREFIX = "plural:"

        // java.util.Formatter's specifier grammar, conversions limited to the valid
        // ones. Matched against what lintRelease did: `% c` failed it, `100% local`
        // and `100% 로컬` pass it on main.
        val FORMAT_SPECIFIER =
            Regex("""%(?:(\d+)\$)?[-#+ 0,(<]*\d*(?:\.\d+)?([tT][a-zA-Z]|[bBhHsScCdoxXeEfgGaA%n])""")
    }
}
