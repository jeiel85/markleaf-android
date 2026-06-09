package com.markleaf.notes.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * Full-text search index over [NoteEntity].
 *
 * **Tokenizer choice — default (`simple`).** v2.16 switched this table to the
 * `unicode61` tokenizer for nicer CJK matching (#65), but that tokenizer is NOT
 * compiled into every device's system SQLite. On builds that lack it (some custom
 * / hardened ROMs, e.g. GrapheneOS) the `CREATE VIRTUAL TABLE ... USING FTS4(...
 * tokenize=unicode61 ...)` fails at DB creation with `unknown tokenizer` and the
 * app crashes on first launch (#135). The `simple` tokenizer ships in every SQLite
 * build, so reverting to it makes the index crash-proof everywhere.
 *
 * Search quality impact is small: queries route through [NoteDao.searchNotesFts]
 * with prefix (`token*`) matching, so space-separated scripts — including Korean —
 * still match. Only space-less Japanese/Chinese loses the marginal `unicode61`
 * polish, which is the v2.16-pre behaviour the app shipped with for its whole life.
 *
 * **Why FTS4, not FTS5.** Room only generates `@Fts3` / `@Fts4` schemas; there is
 * no `@Fts5` annotation. Migrating to raw FTS5 would mean dropping Room's content
 * sync machinery and managing INSERT/UPDATE/DELETE triggers by hand. For an OSS
 * notes app the marginal gains (auxiliary tables, BM25 in core) don't outweigh
 * losing Room's content-table integration.
 */
@Fts4(contentEntity = NoteEntity::class)
@Entity(tableName = "notes_fts")
data class NoteFtsEntity(
    val title: String,
    val contentMarkdown: String,
    val excerpt: String
)
