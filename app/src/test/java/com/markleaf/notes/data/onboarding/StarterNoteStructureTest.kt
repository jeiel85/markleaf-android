package com.markleaf.notes.data.onboarding

import com.markleaf.notes.LocaleManifest
import com.markleaf.notes.core.markdown.CalloutKind
import com.markleaf.notes.core.text.TitleExtractor
import com.markleaf.notes.util.TagParser
import com.markleaf.notes.util.WikilinkExtractor
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks that every language's `starter_notes.md` still has the Markdown
 * structure the app reads, not just the right number of notes.
 *
 * A translation sees the whole file as text, so structure gets translated or
 * reflowed along with the prose. The Slovak contribution (#453) arrived with
 * `[!NOTE]` as `[!POZNÁMKA]` (rendered as a plain quote), a wikilink
 * hard-wrapped across two lines (not a link at all), a tag line reading
 * `#ochrana súkromia` (one tag and a stray word), and a tour entry pointing at
 * a title that no note had. `ResourceParityTest.localizedStarterNotesExist`
 * only counts the notes, so all four would have shipped green.
 *
 * The rules use the app's own parsers — [CalloutKind.parse],
 * [WikilinkExtractor], [TagParser], [TitleExtractor] — so this test accepts
 * exactly what the app accepts.
 */
class StarterNoteStructureTest {

    @Test
    fun calloutsUseTypesThePreviewRecognizes() {
        assertNoProblems("callouts the preview would render as a plain quote") { note ->
            CALLOUT_MARKER.findAll(withoutCode(note.content))
                .map { it.groupValues[1] }
                .filter { CalloutKind.parse(it) == null }
                .map { "[!$it] — callout types are keywords; keep NOTE, TIP, IMPORTANT, WARNING or CAUTION" }
                .toList()
        }
    }

    @Test
    fun wikilinksAreWholeAndPointAtAStarterNote() {
        assertNoProblems("wikilinks that are broken or point at no starter note") { note ->
            val body = withoutCode(note.content)
            val links = WikilinkExtractor.extract(body)
            val problems = mutableListOf<String>()
            // A `[[` the extractor doesn't close on the same line is plain text,
            // not a link — usually a title hard-wrapped by a formatter.
            if (body.split("[[").size - 1 != links.size) {
                problems += "a [[ that doesn't close on the same line — keep each wikilink on one line"
            }
            links.filterNot { WikilinkExtractor.normalize(it) in note.file.normalizedTitles }
                .forEach { problems += "[[$it]] — no note in this file has that title" }
            problems
        }
    }

    @Test
    fun tagLinesHoldOnlyTags() {
        assertNoProblems("tag lines with words that are not tags") { note ->
            // Every starter note ends with its tag line. A tag can't contain a
            // space, so a translated multi-word tag leaves words behind.
            val line = note.content.lines().last { it.isNotBlank() }
            val words = line.trim().split(Regex("\\s+")).distinct()
            if (TagParser.parseTags(line).size == words.size && words.all { it.startsWith("#") }) {
                emptyList()
            } else {
                listOf("\"$line\" — tags can't contain spaces; join the words with - or _")
            }
        }
    }

    @Test
    fun theTourNamesTheOtherStarterNotesByTitle() {
        assertNoProblems("tour entries in the first note that match no note title") { note ->
            if (note.index != 0) return@assertNoProblems emptyList()
            BOLD.findAll(note.content)
                .map { it.groupValues[1] }
                .filterNot { WikilinkExtractor.normalize(it) in note.file.normalizedTitles }
                .map { "**$it** — the tour should use the exact title of the note it points to" }
                .toList()
        }
    }

    private class StarterFile(val path: String, contents: List<String>) {
        val notes: List<StarterNote> = contents.mapIndexed { index, content -> StarterNote(this, index, content) }

        /** Titles the way the seeder derives them, normalized the way wikilinks match them. */
        val normalizedTitles: Set<String> =
            contents.map { WikilinkExtractor.normalize(TitleExtractor.extractTitle(it)) }.toSet()
    }

    private class StarterNote(val file: StarterFile, val index: Int, val content: String)

    /** Runs [check] over every note of every language, then fails once with the full list. */
    private fun assertNoProblems(what: String, check: (StarterNote) -> List<String>) {
        val problems = starterFiles().flatMap { file ->
            file.notes.flatMap { note ->
                check(note).map { "${file.path} (note ${note.index + 1}): $it" }
            }
        }
        assertTrue(
            buildString {
                appendLine("Starter notes have $what:")
                problems.forEach { appendLine("  - $it") }
            },
            problems.isEmpty()
        )
    }

    private fun starterFiles(): List<StarterFile> =
        LocaleManifest.entries
            .filter { it.hasStarterNotes }
            .map { entry ->
                val path = "src/main/res/${entry.rawDir}/starter_notes.md"
                val file = File(path)
                assertTrue("$path should exist", file.isFile)
                // The same split the seeder does.
                StarterFile(
                    path,
                    file.readText()
                        .split(StarterNotesSeeder.STARTER_NOTE_SEPARATOR)
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                )
            }

    /** Code isn't rendered as Markdown, so a `[[` or `[!` inside it is an example, not structure. */
    private fun withoutCode(content: String): String =
        content.replace(FENCED_CODE, "").replace(INLINE_CODE, "")

    private companion object {
        // Any `[!…]` marker, not just ASCII letters: `[!POZNÁMKA]` is exactly
        // the translation the preview's own `[A-Za-z]+` pattern would skip.
        val CALLOUT_MARKER = Regex("""(?m)^\s*>\s*\[!([^\]\n]+)]""")
        val BOLD = Regex("""\*\*(.+?)\*\*""")
        val FENCED_CODE = Regex("""(?s)```.*?```""")
        val INLINE_CODE = Regex("""`[^`\n]*`""")
    }
}
