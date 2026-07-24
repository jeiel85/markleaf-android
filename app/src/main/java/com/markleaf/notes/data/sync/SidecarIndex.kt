package com.markleaf.notes.data.sync

import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

/**
 * The mapping between notes and their mirror files, kept *beside* the notes
 * instead of inside them (#216).
 *
 * Markleaf's default is a `---` frontmatter block at the top of every file,
 * carrying the note's id and a few flags. It is robust — it survives renames,
 * and a folder of such files can be reconstructed anywhere — but it is also
 * text the user never wrote sitting in their notes. This is the opt-in
 * alternative: the `.md` files hold nothing but what was typed, and the
 * bookkeeping lives in a hidden index file.
 *
 * ## Why there is no timestamp here
 *
 * The obvious design keeps an id map and lets the existing reconcile decide
 * what is newer. It cannot: with no frontmatter there is no `updated_at`, so
 * [NoteFolderMirror.effectiveFileTimestamp] falls back to the filesystem mtime
 * — and a sync client that re-downloads a file bumps its mtime without changing
 * a byte. Every file would look newer than its note on every pass, which is a
 * conflict storm rather than a sync, and it would hit hardest on exactly the
 * setups this mode is for.
 *
 * So the index stores [SidecarEntry.contentHash] — a digest of what Markleaf
 * last wrote to that file — and the question becomes "is this file still what
 * we wrote", which needs no clock at all.
 *
 * ## Why one file per device
 *
 * A single shared index would be written by every device, and a sync client
 * resolving that as a conflict would leave one device's view of the folder
 * silently wrong — reintroducing the duplicate-note bug (#140) in a new place.
 * Each device therefore writes only [fileNameFor] its own id and never touches
 * another's; [merge] reads them all, so a second device still learns the first
 * device's mapping rather than starting blind.
 */
object SidecarIndex {

    /** Current on-disk schema. Bumped only if the entry shape changes. */
    const val SCHEMA_VERSION = 1

    private const val PREFIX = ".markleaf-index-"
    private const val SUFFIX = ".json"

    /** The index file this device owns. Never write to any other. */
    fun fileNameFor(deviceId: String): String = "$PREFIX$deviceId$SUFFIX"

    /** Whether [name] is an index file — ours or another device's. */
    fun isIndexFile(name: String?): Boolean {
        val n = name ?: return false
        return n.startsWith(PREFIX) && n.endsWith(SUFFIX) && n.length > PREFIX.length + SUFFIX.length
    }

    /** The device id inside an index filename, or null if [name] is not one. */
    fun deviceIdOf(name: String?): String? {
        if (!isIndexFile(name)) return null
        return name!!.substring(PREFIX.length, name.length - SUFFIX.length)
    }

    /**
     * SHA-256 of [content], hex-encoded.
     *
     * Taken over the exact text written to (or read from) the file, so the
     * comparison is byte-for-byte honest. Truncating it would save a few
     * kilobytes across a large folder and buy a collision risk that is not
     * worth reasoning about.
     */
    fun hashOf(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        return buildString(digest.size * 2) {
            for (byte in digest) append("%02x".format(byte))
        }
    }

    /**
     * Serialize [entries] as the index [deviceId] owns.
     *
     * Written one entry per line rather than minified: this file lands in the
     * user's own folder, and a person who opens it to see what Markleaf put
     * there deserves to be able to read it.
     */
    fun encode(deviceId: String, entries: Collection<SidecarEntry>): String {
        val array = JSONArray()
        // Sorted so a run that changes nothing produces a byte-identical file —
        // otherwise every save would look like a change to whatever syncs the
        // folder, which is the opposite of what this mode is for.
        for (entry in entries.sortedBy { it.noteId }) {
            array.put(
                JSONObject().apply {
                    put(KEY_ID, entry.noteId)
                    put(KEY_FILE, entry.fileName)
                    put(KEY_HASH, entry.contentHash)
                    put(KEY_CREATED, entry.createdAtMillis)
                    put(KEY_PINNED, entry.pinned)
                    put(KEY_ARCHIVED, entry.archived)
                }
            )
        }
        return buildString {
            append("{\n")
            append("  \"$KEY_VERSION\": $SCHEMA_VERSION,\n")
            append("  \"$KEY_DEVICE\": ${JSONObject.quote(deviceId)},\n")
            append("  \"$KEY_ENTRIES\": [\n")
            for (i in 0 until array.length()) {
                append("    ").append(array.getJSONObject(i).toString())
                if (i < array.length() - 1) append(",")
                append("\n")
            }
            append("  ]\n")
            append("}\n")
        }
    }

    /**
     * Parse an index file. Returns null when [raw] is not one we can read —
     * malformed, truncated mid-sync, or written by a schema we do not know.
     *
     * Null is deliberately not an error the caller has to handle specially: an
     * unreadable index means "we know nothing about these files", which falls
     * back to matching by filename. Guessing at a half-parsed index is how a
     * note would get attached to the wrong file.
     */
    fun decode(raw: String): ParsedIndex? = runCatching {
        val root = JSONObject(raw)
        val version = root.optInt(KEY_VERSION, 0)
        // A newer schema may mean anything; refuse rather than misread it.
        if (version <= 0 || version > SCHEMA_VERSION) return@runCatching null
        val deviceId = root.optString(KEY_DEVICE).takeIf { it.isNotEmpty() }
            ?: return@runCatching null
        val array = root.optJSONArray(KEY_ENTRIES) ?: JSONArray()
        val entries = buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString(KEY_ID).takeIf { it.isNotEmpty() } ?: continue
                val file = obj.optString(KEY_FILE).takeIf { it.isNotEmpty() } ?: continue
                val hash = obj.optString(KEY_HASH).takeIf { it.isNotEmpty() } ?: continue
                add(
                    SidecarEntry(
                        noteId = id,
                        fileName = file,
                        contentHash = hash,
                        createdAtMillis = obj.optLong(KEY_CREATED, 0L),
                        pinned = obj.optBoolean(KEY_PINNED, false),
                        archived = obj.optBoolean(KEY_ARCHIVED, false)
                    )
                )
            }
        }
        ParsedIndex(deviceId = deviceId, entries = entries)
    }.getOrNull()

    /**
     * Combine every index in the folder into one view, keyed by note id.
     *
     * [ownDeviceId]'s entries win outright. The hash answers "is this file still
     * what *we* wrote", so another device's hash cannot stand in for ours —
     * it describes what that device wrote, which is a different question. Other
     * devices' entries are used only where we have none, which is exactly the
     * case that matters: a note this device has never seen, whose id would
     * otherwise be unknowable and which would import as a brand-new duplicate.
     */
    fun merge(ownDeviceId: String, indexes: List<ParsedIndex>): Map<String, SidecarEntry> {
        val merged = LinkedHashMap<String, SidecarEntry>()
        for (index in indexes.filter { it.deviceId != ownDeviceId }) {
            for (entry in index.entries) merged[entry.noteId] = entry
        }
        for (index in indexes.filter { it.deviceId == ownDeviceId }) {
            for (entry in index.entries) merged[entry.noteId] = entry
        }
        return merged
    }

    /**
     * The merged view keyed by filename, for walking the folder — where a
     * filename is what we hold and the note id is what we need.
     *
     * Two notes cannot share a name in one folder (the mirror's collision guard
     * sees to that), but two *devices* can disagree about one note's name after
     * a rename. Later wins, and [merge] has already put our own entries last.
     */
    fun byFileName(merged: Map<String, SidecarEntry>): Map<String, SidecarEntry> =
        merged.values.associateBy { it.fileName.lowercase() }

    private const val KEY_VERSION = "version"
    private const val KEY_DEVICE = "device"
    private const val KEY_ENTRIES = "entries"
    private const val KEY_ID = "id"
    private const val KEY_FILE = "file"
    private const val KEY_HASH = "hash"
    private const val KEY_CREATED = "created"
    private const val KEY_PINNED = "pinned"
    private const val KEY_ARCHIVED = "archived"
}

/**
 * One note's bookkeeping: which file holds it, what we last wrote there, and
 * the flags that used to travel in the frontmatter.
 *
 * [createdAtMillis], [pinned] and [archived] live here because they have
 * nowhere else to go once the block is gone. That is the honest cost of this
 * mode: a folder read without its index yields notes dated today and unpinned.
 */
data class SidecarEntry(
    val noteId: String,
    val fileName: String,
    val contentHash: String,
    val createdAtMillis: Long,
    val pinned: Boolean,
    val archived: Boolean
)

/** One index file's contents. */
data class ParsedIndex(
    val deviceId: String,
    val entries: List<SidecarEntry>
)
