package com.markleaf.notes.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.markleaf.notes.R

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS
private const val DAY_MS = 24 * HOUR_MS

/**
 * "just now" / "3 minutes ago" for the sync status rows.
 *
 * Settings and the Sync Center each carried a private copy of this that built
 * the label out of Korean literals, so every other locale read "Last synced:
 * 3시간 전" — the same defect as the hardcoded conflict-copy suffix in #217.
 * One shared composable now, resolved through `<plurals>` so en/de/es/fr get
 * singular and plural forms while ja/ko get their single form.
 *
 * [now] is a parameter rather than a `System.currentTimeMillis()` call in the
 * body so a test can pin the moment and exercise each bucket; the buckets are
 * relative to it and would otherwise drift between assertion and render.
 */
@Composable
fun elapsedTimeLabel(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    val deltaMs = (now - epochMillis).coerceAtLeast(0L)
    return when {
        deltaMs < MINUTE_MS -> stringResource(R.string.elapsed_just_now)
        deltaMs < HOUR_MS -> {
            val minutes = (deltaMs / MINUTE_MS).toInt()
            pluralStringResource(R.plurals.elapsed_minutes_ago, minutes, minutes)
        }
        deltaMs < DAY_MS -> {
            val hours = (deltaMs / HOUR_MS).toInt()
            pluralStringResource(R.plurals.elapsed_hours_ago, hours, hours)
        }
        else -> {
            val days = (deltaMs / DAY_MS).toInt()
            pluralStringResource(R.plurals.elapsed_days_ago, days, days)
        }
    }
}
