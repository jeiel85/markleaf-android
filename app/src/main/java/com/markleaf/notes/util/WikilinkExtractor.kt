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
        return WIKILINK_REGEX.findAll(content).map { target(it.groupValues[1]) }.filter { it.isNotEmpty() }.toList()
    }

    /** True when [text] contains at least one well-formed wikilink. */
    fun hasAny(text: String): Boolean = WIKILINK_REGEX.containsMatchIn(text)

    /** The part before an optional display alias, shared by preview and backlinks. */
    fun target(body: String): String = body.substringBefore('|').trim()

    /** Display text after the first pipe, falling back to the target. */
    fun label(body: String): String = body.substringAfter('|', "").trim().ifEmpty { target(body) }

    /** Normalize for case-insensitive matching against note titles. */
    fun normalize(title: String): String = title.trim().lowercase()
}
