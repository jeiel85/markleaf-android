package com.markleaf.notes.data.sync

import com.markleaf.notes.core.text.NoteNames

/**
 * Turns a note title into a safe, human-readable mirror filename — the basis of
 * the "the file is named after the note" behaviour (#134).
 *
 * Pure string logic so it can be unit-tested without Android. Two concerns:
 *  - [sanitizeBase] strips anything a synced filesystem (incl. Windows/exFAT on
 *    a phone's SD card, or a Drive/Dropbox mount) would reject, caps the length,
 *    and never returns an empty or reserved name.
 *  - [uniqueName] disambiguates two notes that sanitize to the same base by
 *    appending " (2)", " (3)", … — checked against the caller's folder.
 */
object MirrorFileNames {

    /** Cap the title-derived base so very long titles don't blow past path limits. */
    const val MAX_BASE_LENGTH = NoteNames.MAX_LENGTH

    private const val FALLBACK_BASE = "untitled"

    /**
     * Sanitize [title] into a filename base (no extension). Always returns a
     * non-empty, filesystem-safe string. The rules are [NoteNames.clean]'s, so a
     * name the app accepted under the File name title rule comes back unchanged.
     */
    fun sanitizeBase(title: String): String = NoteNames.clean(title).ifEmpty { FALLBACK_BASE }

    /** `<base>.<ext>` with no collision check — the plain target name. */
    fun fileName(base: String, ext: String): String = "$base.$ext"

    /**
     * True when [fileName] is the *undisambiguated* name for [base] —
     * `Notes.md` matches base `Notes`, `Notes (2).md` does not.
     *
     * Used to recognise a note's own file when its frontmatter id has gone
     * missing (#213), so the mirror rewrites it instead of forking a ` (2)`
     * copy on every save. Case-insensitive, because a synced folder can land on
     * a case-insensitive filesystem (exFAT SD card, Windows share) where
     * `notes.md` and `Notes.md` are the same file.
     */
    fun isPlainNameFor(fileName: String, base: String): Boolean =
        fileName.substringBeforeLast('.', fileName).equals(base, ignoreCase = true)

    /**
     * The first `<base>.<ext>` / `<base> (2).<ext>` / … that [isTaken] reports
     * free. [isTaken] should return true for names already used by a *different*
     * note in the folder (the note's own existing file is not a collision).
     */
    fun uniqueName(base: String, ext: String, isTaken: (String) -> Boolean): String {
        val first = fileName(base, ext)
        if (!isTaken(first)) return first
        var n = 2
        while (true) {
            val candidate = "$base ($n).$ext"
            if (!isTaken(candidate)) return candidate
            n++
        }
    }
}
