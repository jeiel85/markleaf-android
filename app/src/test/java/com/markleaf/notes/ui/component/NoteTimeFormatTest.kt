package com.markleaf.notes.ui.component

import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The full date and time in the "Note information" sheet (#593), and the list's short form. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class NoteTimeFormatTest {

    private val app = ApplicationProvider.getApplicationContext<Context>()
    private val afternoon = Instant.parse("2026-11-16T14:05:00Z")

    private fun contextIn(locale: Locale): Context =
        app.createConfigurationContext(Configuration(app.resources.configuration).apply { setLocale(locale) })

    private fun useClock(hours: String) {
        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, hours)
    }

    @Test
    fun twentyFourHourClockWritesTheHourAsIs() {
        useClock("24")
        val text = formatNoteDateTime(contextIn(Locale.US), afternoon, ZoneOffset.UTC)

        assertTrue(text, text.contains("14:05"))
        assertTrue(text, text.contains("2026"))
        assertTrue(text, text.contains("Nov"))
        assertTrue(text, text.contains("16"))
    }

    @Test
    fun twelveHourClockAddsTheDayPeriod() {
        useClock("12")
        val text = formatNoteDateTime(contextIn(Locale.US), afternoon, ZoneOffset.UTC)

        assertTrue(text, text.contains("2:05"))
        assertTrue(text, text.contains("PM"))
        assertFalse(text, text.contains("14:05"))
    }

    @Test
    fun theDateIsWrittenInTheAppLanguage() {
        useClock("24")
        val text = formatNoteDateTime(contextIn(Locale.KOREA), afternoon, ZoneOffset.UTC)

        assertTrue(text, text.contains("2026년"))
        assertTrue(text, text.contains("16일"))
        assertTrue(text, text.contains("14:05"))
    }

    @Test
    fun theTimeIsTheDevicesZone() {
        useClock("24")
        val seoul = ZoneOffset.ofHours(9)
        val text = formatNoteDateTime(contextIn(Locale.US), afternoon, seoul)

        // 14:05 UTC is 23:05 in Seoul, still on the 16th.
        assertTrue(text, text.contains("23:05"))
        val utc = formatNoteDateTime(contextIn(Locale.US), afternoon, ZoneOffset.UTC)
        assertFalse(utc, utc.contains("23:05"))
    }

    // The list's short form (#262, v2.74.0): it wrote "15:02" on a 12-hour
    // device while Note information wrote "3:02 PM" for the same moment.

    private val todayInUtc = afternoon.atZone(ZoneOffset.UTC).toLocalDate()

    @Test
    fun theListsTimeTodayFollowsATwelveHourClock() {
        useClock("12")
        val text = formatUpdatedTime(contextIn(Locale.US), afternoon, ZoneOffset.UTC, todayInUtc)

        assertTrue(text, text.contains("2:05"))
        assertTrue(text, text.contains("PM"))
        assertFalse(text, text.contains("14:05"))
    }

    @Test
    fun theListsTimeTodayFollowsATwentyFourHourClock() {
        useClock("24")
        val text = formatUpdatedTime(contextIn(Locale.US), afternoon, ZoneOffset.UTC, todayInUtc)

        assertTrue(text, text.contains("14:05"))
        assertFalse(text, text.contains("PM"))
    }

    @Test
    fun theListsTimeYesterdayFollowsTheClockToo() {
        useClock("12")
        val text = formatUpdatedTime(
            contextIn(Locale.US), afternoon, ZoneOffset.UTC, todayInUtc.plusDays(1)
        )

        assertTrue(text, text.contains("2:05"))
        assertTrue(text, text.contains("PM"))
    }

    @Test
    fun theListAndNoteInformationAgreeOnTheTime() {
        for (clock in listOf("12", "24")) {
            useClock(clock)
            val context = contextIn(Locale.US)
            val list = formatUpdatedTime(context, afternoon, ZoneOffset.UTC, todayInUtc)
            val sheet = formatNoteDateTime(context, afternoon, ZoneOffset.UTC)
            val time = if (clock == "12") "2:05" else "14:05"

            assertTrue("$clock: $list", list.contains(time))
            assertTrue("$clock: $sheet", sheet.contains(time))
        }
    }

    /**
     * Every language the app ships in, both clocks: the pattern comes from ICU
     * for each locale, and one that the formatter could not read would throw
     * when the sheet opens.
     */
    @Test
    fun everyAppLanguageFormatsOnBothClocks() {
        val tags = listOf("en", "ko", "ja", "zh", "de", "fr", "es", "hr", "ru", "vi", "sk", "zh-TW")
        for (clock in listOf("12", "24")) {
            useClock(clock)
            for (tag in tags) {
                val text = formatNoteDateTime(contextIn(Locale.forLanguageTag(tag)), afternoon, ZoneOffset.UTC)
                assertTrue("$tag/$clock: $text", text.contains("2026"))
                val list = formatUpdatedTime(
                    contextIn(Locale.forLanguageTag(tag)), afternoon, ZoneOffset.UTC, todayInUtc
                )
                assertTrue("$tag/$clock: $list", list.contains("05"))
            }
        }
    }
}
