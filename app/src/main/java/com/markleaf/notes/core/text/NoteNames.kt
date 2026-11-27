package com.markleaf.notes.core.text

/**
 * What a note's name may be when the name is also its file name — the
 * [NoteTitleSource.FILE_NAME] rule (#429, #611).
 *
 * The filename rules themselves live here, and `MirrorFileNames.sanitizeBase`
 * is built on [clean], so a name accepted by [problemWith] comes back from the
 * synced folder exactly as it was typed. If the two drifted apart, `a/b` would
 * be written as `a b.md` and the next import would rename the note to "a b".
 *
 * Pure string logic, so it is unit-tested without Android.
 */
object NoteNames {

    /** Longest name, matching the mirror's cap on a file name's base. */
    const val MAX_LENGTH = 120

    // Backslash / : * ? " < > | are illegal on Windows/exFAT; control chars break
    // many filesystems. \p{Cntrl} and \s are ASCII POSIX classes — identical on
    // JVM and Android, no locale/Unicode-property drift.
    private val ILLEGAL = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
    private val WHITESPACE = Regex("\\s+")

    // Windows reserved device names — illegal as a whole filename stem regardless
    // of extension.
    private val RESERVED = buildSet {
        addAll(listOf("CON", "PRN", "AUX", "NUL"))
        for (i in 1..9) { add("COM$i"); add("LPT$i") }
    }

    /**
     * Why a typed name cannot be used: reserved, taken by another note, or —
     * [FOLDER_KEPT_ANOTHER] — accepted here but not by the sync folder, which
     * kept the file under another name (see [nameTheFolderKept]).
     */
    enum class Problem { RESERVED, TAKEN, FOLDER_KEPT_ANOTHER }

    /**
     * [raw] made safe as a file name base: illegal characters become spaces,
     * whitespace runs collapse, trailing dots and spaces go, the length is
     * capped, and a reserved device name gets a leading underscore. May be
     * empty — callers decide what an empty name means.
     */
    fun clean(raw: String): String {
        var s = ILLEGAL.replace(raw, " ")
        s = WHITESPACE.replace(s, " ").trim()
        // Windows forbids trailing dots/spaces on a name component.
        s = s.trimEnd('.', ' ')
        if (s.length > MAX_LENGTH) {
            s = s.take(MAX_LENGTH).trimEnd('.', ' ')
        }
        if (s.uppercase() in RESERVED) return "_$s"
        return s
    }

    /**
     * What the name field keeps of what was typed: the characters a file name
     * cannot hold are dropped as they arrive, and the length is capped. Trailing
     * dots and spaces are left alone here — the person may still be typing — and
     * go when the name is committed through [clean].
     */
    fun filterTyped(typed: String): String =
        typed.replace(ILLEGAL, "").take(MAX_LENGTH)

    /**
     * Why [name] (already through [clean]) cannot name a note, or null when it
     * can. [otherNames] are the names of every other note, trashed ones included
     * — a trashed note keeps its file — compared ignoring case, because a synced
     * folder may sit on a case-insensitive filesystem.
     */
    fun problemWith(name: String, otherNames: Collection<String>): Problem? {
        if (name.startsWith("_") && name.drop(1).uppercase() in RESERVED) return Problem.RESERVED
        if (otherNames.any { it.equals(name, ignoreCase = true) }) return Problem.TAKEN
        return null
    }

    /** A file name without its extension — `Frankenstein.md` → `Frankenstein`. */
    fun fromFileName(fileName: String): String = fileName.substringBeforeLast('.', fileName)

    /**
     * The name the sync folder kept when a rename to [wanted] did not take —
     * its file still called what it was, because the provider refused the
     * rename, or given a " (2)" because a file that is not a note held the
     * name — or null when the file is called [wanted]. The note then takes the
     * file's name: stamped as in sync under [wanted], the next import would
     * read the old name as a rename made in the folder and quietly undo this
     * one (D088).
     */
    fun nameTheFolderKept(wanted: String, fileName: String): String? =
        fromFileName(fileName).takeIf { it != wanted }
}
