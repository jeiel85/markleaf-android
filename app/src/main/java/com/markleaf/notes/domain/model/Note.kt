package com.markleaf.notes.domain.model

import java.time.Instant

data class Note(
    val id: String,
    val title: String,
    val contentMarkdown: String,
    val excerpt: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val trashed: Boolean = false,
    val deletedAt: Instant? = null,
    val tags: List<Tag> = emptyList(),
    val sortOrder: Int = 0,
    /** Set by [com.markleaf.notes.data.sync.NoteFolderMirror] when this note's
     *  body was last copied in from the sync folder. Used as the conflict
     *  baseline: if [updatedAt] is later than this, the local copy has been
     *  edited since the last sync and an incoming newer file is treated as a
     *  conflict (kept as a duplicate) rather than silently overwriting. */
    val lastImportedAt: Instant? = null,
    /** The newest mirror-file timestamp the reconcile has already dealt with for
     *  this note. Purely reconcile bookkeeping — it exists so settling a sync
     *  conflict does not have to move [updatedAt], which is the user's "when did
     *  I last change this" and drives the notes list order. Before it, the only
     *  way to stop a conflict repeating every pass was to push [updatedAt] past
     *  the file, which quietly reordered a note nobody had edited. */
    val remoteSeenAt: Instant? = null,
    /** When true the note is in the passcode-gated "Locked notes" space: hidden
     *  from every normal list, search, tag view and sync/export path until the
     *  user unlocks that space. UI-visibility gate only — not encryption. */
    val locked: Boolean = false,
    /** When true this note is the remote side of a sync conflict, kept alongside
     *  the local note so the user can merge by hand. The Sync Center lists these.
     *  A flag rather than a title suffix: detection used to be a `LIKE` match on
     *  a hardcoded Korean string, which meant every language saw a Korean title
     *  and the title could never be translated without breaking detection. */
    val isConflictCopy: Boolean = false
)
