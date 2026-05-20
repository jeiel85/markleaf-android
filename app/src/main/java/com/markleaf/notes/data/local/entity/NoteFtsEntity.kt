package com.markleaf.notes.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * Full-text search index over [NoteEntity].
 *
 * **Tokenizer choice — unicode61.** SQLite's default `simple` tokenizer splits on
 * ASCII whitespace only, which leaves Korean/Japanese/Chinese notes effectively
 * unsearchable (the whole sentence becomes one token). `unicode61` honors Unicode
 * word boundaries and folds case across scripts, which is the practical win this
 * project needs from the FTS5-style ergonomics the v2.16 roadmap called for.
 *
 * **Why FTS4, not FTS5.** Room only generates `@Fts3` / `@Fts4` schemas; there is
 * no `@Fts5` annotation. Migrating to raw FTS5 would mean dropping Room's content
 * sync machinery and managing INSERT/UPDATE/DELETE triggers by hand. For an OSS
 * notes app the marginal gains (auxiliary tables, BM25 in core) don't outweigh
 * losing Room's content-table integration. FTS4 + unicode61 covers the actual
 * user pain.
 */
@Fts4(
    contentEntity = NoteEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    tokenizerArgs = ["remove_diacritics=2"]
)
@Entity(tableName = "notes_fts")
data class NoteFtsEntity(
    val title: String,
    val contentMarkdown: String,
    val excerpt: String
)
