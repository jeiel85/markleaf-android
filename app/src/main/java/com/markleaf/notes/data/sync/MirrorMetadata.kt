package com.markleaf.notes.data.sync

import android.net.Uri
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.SyncMetadataMode

/**
 * Where a mirror pass keeps the note↔file link (#216).
 *
 * A sealed pair rather than a mode flag plus a nullable device id, so "sidecar
 * mode with no device id" — an index file nobody owns — cannot be expressed.
 */
sealed interface MirrorMetadata {
    /** A `---` header in each file. The default, and what every folder written before #216 holds. */
    data object Frontmatter : MirrorMetadata

    /** A hidden index owned by [deviceId]; the note files carry only their text. */
    data class Sidecar(val deviceId: String) : MirrorMetadata
}

/**
 * The sync folder as a [Uri], or null when folder sync is off or the persisted
 * value no longer parses.
 *
 * Both guards are needed wherever the folder is touched: sync may be
 * unconfigured, and the stored string is opaque text that a revoked or
 * rewritten SAF grant can leave unparseable. Nine call sites spelled them out
 * separately, in three different shapes, which made the one call site carrying
 * an *extra* condition (the editor's `!note.locked`) hard to pick out (#158).
 */
fun AppSettings.syncFolderUriOrNull(): Uri? =
    syncFolderUri?.let { raw -> runCatching { Uri.parse(raw) }.getOrNull() }

/**
 * Where this pass should keep the note↔file link, from settings (#216).
 *
 * Sidecar mode needs a device id to name the index it owns. The id is created
 * when the mode is switched on, so a missing one here means something went
 * wrong upstream — and the honest response is the default behaviour, not an
 * index file with no owner that a second device could never tell apart.
 */
fun AppSettings.mirrorMetadata(): MirrorMetadata {
    val deviceId = syncDeviceId
    return if (syncMetadataMode == SyncMetadataMode.SIDECAR && !deviceId.isNullOrBlank()) {
        MirrorMetadata.Sidecar(deviceId)
    } else {
        MirrorMetadata.Frontmatter
    }
}
