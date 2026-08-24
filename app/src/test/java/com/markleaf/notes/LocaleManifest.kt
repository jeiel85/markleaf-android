package com.markleaf.notes

import java.io.File
import org.junit.Assert.assertTrue

/**
 * The language list, read from `config/locales.tsv` rather than repeated here.
 *
 * The list used to live in seven places — two of them in this test source set —
 * and a language missing from one of them was a surface no gate looked at, so
 * it shipped untested and green. Both #294 (Chinese) and #329 (Croatian)
 * arrived that way and were caught by hand. Tests that need the list ask for it
 * here; `scripts/verify-locales.ps1` checks the list against the files on disk
 * in both directions.
 */
object LocaleManifest {

    data class Entry(
        val code: String,
        val store: String,
        val isSource: Boolean,
        val hasStarterNotes: Boolean
    ) {
        /** `values` for the language the app is written in, `values-<code>` for the rest. */
        val resDir: String get() = if (isSource) "values" else "values-$code"

        /** `raw` for the source language, `raw-<code>` for the rest. */
        val rawDir: String get() = if (isSource) "raw" else "raw-$code"
    }

    /** Unit tests run with the app module as their working directory. */
    private val manifestFile = File("../config/locales.tsv")

    val entries: List<Entry> by lazy { read() }

    /** The language the app is written in — its resources carry no locale code. */
    val source: Entry get() = entries.single { it.isSource }

    /** Every language the app is translated into, source excluded. */
    val translated: List<Entry> get() = entries.filterNot { it.isSource }

    /** Locale codes as they appear in `values-<code>`: `fr`, `es`, … */
    val translatedCodes: List<String> get() = translated.map { it.code }

    private fun read(): List<Entry> {
        assertTrue(
            "Expected the locale manifest at ${manifestFile.absolutePath}. Unit tests run " +
                "with the app module as their working directory, so the path is relative to it.",
            manifestFile.isFile
        )
        val entries = manifestFile.readLines()
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .map { row ->
                val fields = row.split(Regex("\\s+"))
                require(fields.size == 4) {
                    "config/locales.tsv rows are `code store source starter`, but read: $row"
                }
                Entry(
                    code = fields[0],
                    store = fields[1],
                    isSource = fields[2].toBoolean(row),
                    hasStarterNotes = fields[3].toBoolean(row)
                )
            }
        require(entries.count { it.isSource } == 1) {
            "config/locales.tsv must mark exactly one language as the source language."
        }
        return entries
    }

    private fun String.toBoolean(row: String): Boolean = when (this) {
        "yes" -> true
        "no" -> false
        else -> throw IllegalArgumentException("config/locales.tsv wants yes/no, read '$this' in: $row")
    }
}
