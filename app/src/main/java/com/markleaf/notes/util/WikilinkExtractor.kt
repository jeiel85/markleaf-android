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
    internal val WIKILINK_REGEX = Regex("""\[\[([^\[\]\n]+?)]]""")

    /**
     * Every wikilink target in source order, including duplicates.
     *
     * A target with a heading or block part (`[[Note#Heading]]`,
     * `[[Note#^id]]`, the Obsidian form, #608) also yields the note's own
     * title, so the link counts as a backlink to "Note". The whole target is
     * kept as well: a title may itself contain `#` ("C# basics"), and links to
     * it must not stop counting.
     */
    fun extract(content: String): List<String> {
        return WIKILINK_REGEX.findAll(content)
            .map { target(it.groupValues[1]) }
            .flatMap { target ->
                val note = noteTitle(target)
                if (note.isNotEmpty() && note != target) sequenceOf(target, note) else sequenceOf(target)
            }
            .filter { it.isNotEmpty() }
            .toList()
    }

    /**
     * The note a target names once its `#Heading` / `#^block` part is dropped:
     * `Note#Heading` → `Note`, `#Heading` → "" (a heading in the same note).
     * Callers try the whole target first, since a title may contain `#`.
     */
    fun noteTitle(target: String): String = target.substringBefore('#').trim()

    /** The heading part of `Note#Heading` / `#Heading`, or null without one. */
    fun heading(target: String): String? =
        target.substringAfter('#', "").trim().takeIf { it.isNotEmpty() && !it.startsWith('^') }

    /** True when [text] contains at least one well-formed wikilink. */
    fun hasAny(text: String): Boolean = WIKILINK_REGEX.containsMatchIn(text)

    /** The part before an optional display alias, shared by preview and backlinks. */
    fun target(body: String): String = body.substringBefore('|').trim()

    /** Display text after the first pipe, falling back to the target. */
    fun label(body: String): String = body.substringAfter('|', "").trim().ifEmpty { target(body) }

    /** Normalize for case-insensitive matching against note titles. */
    fun normalize(title: String): String = title.trim().lowercase()
}
