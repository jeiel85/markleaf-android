package com.markleaf.notes.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The matching behind Settings search (#517), on hand-built entries so each
 * rule is checked against strings chosen for it rather than whatever the
 * translations say this week.
 */
class SettingsSearchTest {
    private fun entry(title: String, vararg more: String, item: SettingsItem? = SettingsItem.THEME) =
        SettingsSearchEntry(SettingsShortcut.APPEARANCE, item, title, listOf(title) + more)

    private fun titles(query: String, entries: List<SettingsSearchEntry>) =
        SettingsSearch.search(query, entries).map { it.title }

    @Test
    fun aBlankQueryFindsNothing() {
        val entries = listOf(entry("Theme"))
        assertTrue(SettingsSearch.search("", entries).isEmpty())
        assertTrue(SettingsSearch.search("   ", entries).isEmpty())
    }

    @Test
    fun caseDoesNotMatter() {
        assertEquals(listOf("Theme"), titles("THEME", listOf(entry("Theme"))))
    }

    @Test
    fun anOptionLabelFindsItsSetting() {
        val entries = listOf(entry("Theme", "Follow the system", "Light", "Dark"), entry("Font"))
        assertEquals(listOf("Theme"), titles("dark", entries))
    }

    @Test
    fun everyWordHasToBeThereButNotInOneString() {
        val entries = listOf(
            entry("Font size", "How large the writing is"),
            entry("Font", "Which typeface the writing uses")
        )
        assertEquals(listOf("Font size"), titles("font large", entries))
        assertEquals(listOf("Font size", "Font"), titles("writing font", entries))
    }

    @Test
    fun settingsNamedForTheQueryComeBeforeOnesThatOnlyMentionIt() {
        val entries = listOf(
            entry("Show note previews", "Under each title, a line of the note"),
            entry("Note title", "Where a note's title comes from")
        )
        assertEquals(listOf("Note title", "Show note previews"), titles("title", entries))
    }

    @Test
    fun latinAccentsAreIgnoredBothWays() {
        assertEquals(listOf("Thème"), titles("theme", listOf(entry("Thème"))))
        assertEquals(listOf("Theme"), titles("thème", listOf(entry("Theme"))))
        assertEquals(listOf("Cỡ chữ"), titles("co chu", listOf(entry("Cỡ chữ"))))
    }

    @Test
    fun kanaVoicingMarksStillCount() {
        // が and か are different words; dropping the mark would merge them.
        assertTrue(titles("か", listOf(entry("ガイド"))).isEmpty())
        assertEquals(listOf("ガイド"), titles("ガ", listOf(entry("ガイド"))))
    }

    @Test
    fun aHangulSyllableStillBeingTypedMatches() {
        // Mid-composition the field holds "서" on the way to "설".
        assertEquals(listOf("설정"), titles("서", listOf(entry("설정"))))
    }

    @Test
    fun theCatalogueHasEverySectionAndEverySettingInPageOrder() {
        val entries = SettingsSearch.entries(
            text = { "s$it" },
            sectionTitle = { "heading ${it.name}" }
        )
        val sections = entries.filter { it.item == null }.map { it.section }
        val items = entries.mapNotNull { it.item }
        assertEquals(SettingsShortcut.entries, sections)
        assertEquals(SettingsItem.entries, items)
        // A setting's section only moves forward down the page.
        assertEquals(items.map { it.section.ordinal }.sorted(), items.map { it.section.ordinal })
    }

    @Test
    fun aSectionIsFoundByItsChipWordToo() {
        val entries = SettingsSearch.entries(
            text = { if (it == SettingsShortcut.SYNC.label) "Sync" else "x$it" },
            sectionTitle = { if (it == SettingsShortcut.SYNC) "Multi-device mirror" else it.name }
        )
        val hit = SettingsSearch.search("sync", entries).first()
        assertEquals(SettingsShortcut.SYNC, hit.section)
        assertEquals(null, hit.item)
        assertEquals("Multi-device mirror", hit.title)
    }
}
