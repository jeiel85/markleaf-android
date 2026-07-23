package com.markleaf.notes.data.sync

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
    const val MAX_BASE_LENGTH = 120

    private const val FALLBACK_BASE = "untitled"

    // Backslash / : * ? " < > | are illegal on Windows/exFAT; control chars break
    // many filesystems. Each is replaced with a space; spaces and hyphens in
    // titles are kept so names read naturally. \p{Cntrl} and \s are ASCII POSIX
    // classes — identical on JVM and Android, no locale/Unicode-property drift.
    private val ILLEGAL = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
    private val WHITESPACE = Regex("\\s+")

    // Windows reserved device names — illegal as a whole filename stem regardless
    // of extension. Rare for a note title, but cheap to guard.
    private val RESERVED = buildSet {
        addAll(listOf("CON", "PRN", "AUX", "NUL"))
        for (i in 1..9) { add("COM$i"); add("LPT$i") }
    }

    /**
     * Sanitize [title] into a filename base (no extension). Always returns a
     * non-empty, filesystem-safe string.
     */
    fun sanitizeBase(title: String): String {
        var s = ILLEGAL.replace(title, " ")
        s = WHITESPACE.replace(s, " ").trim()
        // Windows forbids trailing dots/spaces on a name component.
        s = s.trimEnd('.', ' ')
        if (s.length > MAX_BASE_LENGTH) {
            s = s.take(MAX_BASE_LENGTH).trimEnd('.', ' ')
        }
        if (s.isEmpty()) return FALLBACK_BASE
        if (s.uppercase() in RESERVED) return "_$s"
        return s
    }

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
