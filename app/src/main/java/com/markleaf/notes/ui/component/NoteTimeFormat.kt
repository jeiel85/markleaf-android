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
 */
internal fun formatUpdatedTime(context: Context, instant: Instant): String {
    val zone = ZoneId.systemDefault()
    val now = LocalDate.now(zone)
    val date = instant.atZone(zone).toLocalDate()
    val time = instant.atZone(zone).toLocalTime()

    val daysFromToday = ChronoUnit.DAYS.between(date, now)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    return when {
        daysFromToday <= 0L ->
            context.getString(R.string.relative_today, time.format(timeFormatter))
        daysFromToday == 1L ->
            context.getString(R.string.relative_yesterday, time.format(timeFormatter))
        daysFromToday in 2..7 ->
            context.getString(R.string.relative_days_ago, daysFromToday.toInt())
        else -> {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
            date.format(dateFormatter)
        }
    }
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
    val locale = context.resources.configuration.locales[0]
    val skeleton = if (DateFormat.is24HourFormat(context)) "yMMMdHHmm" else "yMMMdhmma"
    // ICU writes the pattern, so ICU formats it: java.time reads most of the
    // same letters but not all of them, and a locale whose best pattern used one
    // of the others would throw here.
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return SimpleDateFormat(pattern, locale)
        .apply { timeZone = TimeZone.getTimeZone(zone) }
        .format(Date.from(instant))
}
