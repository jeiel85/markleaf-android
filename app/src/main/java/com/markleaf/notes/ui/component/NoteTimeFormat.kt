package com.markleaf.notes.ui.component

import android.content.Context
import android.text.format.DateFormat
import com.markleaf.notes.R
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.TimeZone

/**
 * The short date under each row of the notes list, and of the archive since
 * #592: "Today 14:05", "Yesterday 09:12", "3 days ago", then the date.
 *
 * The time follows the device's 12- or 24-hour clock, as [formatNoteDateTime]
 * does: the list wrote "15:02" while Note information wrote "3:02 PM" for the
 * same moment on a 12-hour device (#262, v2.74.0).
 */
internal fun formatUpdatedTime(
    context: Context,
    instant: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone)
): String {
    val date = instant.atZone(zone).toLocalDate()
    val daysFromToday = ChronoUnit.DAYS.between(date, today)

    return when {
        daysFromToday <= 0L ->
            context.getString(R.string.relative_today, formatClockTime(context, instant, zone))
        daysFromToday == 1L ->
            context.getString(R.string.relative_yesterday, formatClockTime(context, instant, zone))
        daysFromToday in 2..7 ->
            context.getString(R.string.relative_days_ago, daysFromToday.toInt())
        else -> {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
            date.format(dateFormatter)
        }
    }
}

/** Hours and minutes in the app's language and the device's 12- or 24-hour clock. */
private fun formatClockTime(context: Context, instant: Instant, zone: ZoneId): String =
    formatWithSkeleton(context, instant, zone, if (DateFormat.is24HourFormat(context)) "HHmm" else "hmma")

private fun formatWithSkeleton(context: Context, instant: Instant, zone: ZoneId, skeleton: String): String {
    val locale = context.resources.configuration.locales[0]
    // ICU writes the pattern, so ICU formats it: java.time reads most of the
    // same letters but not all of them, and a locale whose best pattern used one
    // of the others would throw here.
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return SimpleDateFormat(pattern, locale)
        .apply { timeZone = TimeZone.getTimeZone(zone) }
        .format(Date.from(instant))
}

/**
 * A full date and time for the "Note information" sheet (#593), where the
 * point is the exact moment rather than how long ago it was: the list's "3 days
 * ago" is what the reader is trying to get past.
 *
 * Spelled in the app's language and in the device's 12- or 24-hour clock, the
 * way Android's own date pickers would write it.
 */
internal fun formatNoteDateTime(
    context: Context,
    instant: Instant,
    zone: ZoneId = ZoneId.systemDefault()
): String {
    val skeleton = if (DateFormat.is24HourFormat(context)) "yMMMdHHmm" else "yMMMdhmma"
    return formatWithSkeleton(context, instant, zone, skeleton)
}
