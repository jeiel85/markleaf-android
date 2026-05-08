package com.markleaf.notes.util

/**
 * Extracts `[[Title]]` wiki-link targets from a note's body. Returns the
 * raw target text in source order; deduplication and normalization is the
 * caller's responsibility (see `LocalNoteLinkRepository`).
 *
 * Permitted target characters: anything except `[`, `]`, `\n` — the same
 * set Obsidian / Bear use, so notes round-trip cleanly between apps.
 */
object WikilinkExtractor {
    private val WIKILINK_REGEX = Regex("""\[\[([^\[\]\n]+?)]]""")

    /** Sequence of every wikilink target in source order, including duplicates. */
    fun extract(content: String): List<String> {
        return WIKILINK_REGEX.findAll(content).map { it.groupValues[1].trim() }.toList()
    }

    /** True when [text] contains at least one well-formed wikilink. */
    fun hasAny(text: String): Boolean = WIKILINK_REGEX.containsMatchIn(text)

    /** Normalize for case-insensitive matching against note titles. */
    fun normalize(title: String): String = title.trim().lowercase()
}
