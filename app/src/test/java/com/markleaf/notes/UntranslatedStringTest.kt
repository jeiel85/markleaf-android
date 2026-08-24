package com.markleaf.notes

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Catches locale strings that still hold the English source text.
 *
 * Android lint cannot do this. `MissingTranslation` only fires when a `<string>`
 * is *absent*, and a present-but-untranslated value is indistinguishable from
 * one that is legitimately identical — a brand name, a URL, a format-only
 * string, or a word that simply coincides. That is how `sync_title` shipped
 * reading "Multi-device sync (folder mirror)" on French devices while
 * `lintRelease` stayed green (#161).
 *
 * So anything identical to `values/` has to be named below as deliberate, and
 * [allowlistEntriesAreStillNeeded] fails as soon as an entry stops being needed,
 * so the list cannot rot into a blanket exemption that hides the next
 * regression.
 */
class UntranslatedStringTest {

    @Test
    fun localeStringsAreNotStillEnglish() {
        val english = readStrings(resDir().resolve("values/strings.xml"))
        val offenders = mutableListOf<String>()

        for (locale in LOCALES) {
            val allowed = STRUCTURALLY_IDENTICAL + coincidencesFor(locale)
            for ((key, value) in readStrings(stringsFor(locale))) {
                if (key !in allowed && english[key] == value) {
                    offenders += "values-$locale  $key = \"$value\""
                }
            }
        }

        assertTrue(
            buildString {
                appendLine("These locale strings still hold the English source text.")
                appendLine("Translate them — or, if the value really is identical in that")
                appendLine("language, add the key to this test's allowlist with a reason:")
                offenders.sorted().forEach { appendLine("  - $it") }
            },
            offenders.isEmpty()
        )
    }

    @Test
    fun allowlistEntriesAreStillNeeded() {
        val english = readStrings(resDir().resolve("values/strings.xml"))
        val stale = mutableListOf<String>()

        // A coincidence list for a language the app no longer ships is the same
        // kind of rot: it exempts nothing, and it reads as if that language were
        // still checked here.
        for (locale in LANGUAGE_COINCIDENCES.keys - LOCALES.toSet()) {
            stale += "values-$locale  (not in config/locales.tsv)"
        }

        for (locale in LOCALES) {
            val translated = readStrings(stringsFor(locale))
            for (key in STRUCTURALLY_IDENTICAL + coincidencesFor(locale)) {
                val value = translated[key]
                when {
                    value == null -> stale += "values-$locale  $key (no longer exists)"
                    english[key] != value -> stale += "values-$locale  $key (now translated)"
                }
            }
        }

        assertTrue(
            buildString {
                appendLine("These allowlist entries are no longer identical to English.")
                appendLine("Remove them, so the allowlist keeps naming real exceptions rather")
                appendLine("than silently exempting strings that could regress later:")
                stale.sorted().forEach { appendLine("  - $it") }
            },
            stale.isEmpty()
        )
    }

    private fun resDir(): File {
        val res = File("src/main/res")
        assertTrue(
            "Expected to run with the app module as the working directory, " +
                "but ${res.absolutePath} does not exist",
            res.isDirectory
        )
        return res
    }

    private fun stringsFor(locale: String) = resDir().resolve("values-$locale/strings.xml")

    /** Flattens a resource file to `name` -> value, with plurals as `name[quantity]`. */
    private fun readStrings(file: File): Map<String, String> {
        val root = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
            .documentElement
            .childNodes
        val values = mutableMapOf<String, String>()
        for (i in 0 until root.length) {
            val element = root.item(i) as? Element ?: continue
            val name = element.getAttribute("name")
            if (name.isEmpty()) continue
            when (element.tagName) {
                "string" -> values[name] = element.textContent
                "plurals", "string-array" -> {
                    val items = element.getElementsByTagName("item")
                    for (j in 0 until items.length) {
                        val item = items.item(j) as Element
                        val key = item.getAttribute("quantity").ifEmpty { j.toString() }
                        values["$name[$key]"] = item.textContent
                    }
                }
            }
        }
        return values
    }

    private companion object {
        /** From `config/locales.tsv`; see [LocaleManifest]. */
        val LOCALES = LocaleManifest.translatedCodes

        /**
         * A language with no list has no exemptions — the strictest reading, so
         * a new language's genuine coincidences fail here and get looked at
         * rather than being waved through by a placeholder entry.
         */
        fun coincidencesFor(locale: String): Set<String> =
            LANGUAGE_COINCIDENCES[locale] ?: emptySet()

        /**
         * Identical in every language because they are not prose: the app name,
         * brand names, URLs, a license title, and format-only strings.
         */
        val STRUCTURALLY_IDENTICAL = setOf(
            "app_name",
            "export_pdf_job_name",
            "oss_fdroid_label",
            "oss_fdroid_url",
            "oss_license_url",
            "oss_license_value",
            "oss_source_url",
            "settings_markdown",
            "tag_result_format",
            "theme_material_you"
        )

        /**
         * Words that genuinely coincide with English in one language. Each entry
         * is a translator's decision, not an oversight — so a new one belongs
         * here only after checking the word really is correct in that language.
         */
        val LANGUAGE_COINCIDENCES = mapOf(
            // French spells these the same as English.
            "fr" to setOf(
                "callout_important",
                "callout_note",
                "font_sans",
                "font_serif",
                "formatting_structure",
                "image_alt_dialog_label",
                "matching_notes",
                "notes_title",
                "quick_insert_image",
                "settings_open_source",
                "tag_note_count_format[one]",
                "tag_note_count_format[other]",
                "version_format"
            ),
            // Typography terms kept in their English form.
            "es" to setOf(
                "font_sans",
                "font_serif"
            ),
            // German keeps these as established loanwords: "Tags", "Checkbox",
            // "App", "Passcode", "Inline". "Version" is also the German word, so
            // application_id_format stays English here even though fr/es/ja/ko
            // translate it — German has no adjacent translated sibling to clash with.
            "de" to setOf(
                "application_id_format",
                "checkbox",
                "font_sans",
                "font_serif",
                "formatting_inline",
                "locked_passcode_label",
                "matching_tags",
                "settings_app",
                "tag_suggestions_title",
                "tags",
                "version_format"
            ),
            // ja, ko and zh have no entry: nothing in them coincides with the
            // English source, and an absent list means no exemptions.
            //
            // Croatian borrows the typography vocabulary: "font" is the Croatian
            // word, and "Sans"/"Serif" name the typefaces rather than describe them.
            "hr" to setOf(
                "font_label",
                "font_sans",
                "font_serif"
            )
        )
    }
}
