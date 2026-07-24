package com.markleaf.notes.ui.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Guards the sync status labels against the defect that shipped them: both
 * Settings and the Sync Center built "3시간 전" from Korean literals, so German,
 * English, Spanish, French and Japanese devices all read the Korean.
 *
 * The buckets are pinned against an explicit [NOW] rather than the wall clock —
 * with `System.currentTimeMillis()` inside the helper, a fixed input drifts
 * across a bucket boundary between the render and the assertion.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class ElapsedTimeLabelTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun englishReadsEachBucket() {
        assertEquals(
            listOf(
                "just now",
                "just now",
                "1 minute ago",
                "59 minutes ago",
                "1 hour ago",
                "23 hours ago",
                "1 day ago",
                "5 days ago"
            ),
            labelsForEachBucket()
        )
    }

    /** German has to inflect: `vor 1 Minute` but `vor 59 Minuten`. */
    @Test
    @Config(sdk = [33], qualifiers = "de-rDE-w360dp-h640dp-mdpi")
    fun germanReadsGermanNotKorean() {
        assertEquals(
            listOf(
                "gerade eben",
                "gerade eben",
                "vor 1 Minute",
                "vor 59 Minuten",
                "vor 1 Stunde",
                "vor 23 Stunden",
                "vor 1 Tag",
                "vor 5 Tagen"
            ),
            labelsForEachBucket()
        )
    }

    /** The "just now" case carries an apostrophe that has to survive escaping. */
    @Test
    @Config(sdk = [33], qualifiers = "fr-rFR-w360dp-h640dp-mdpi")
    fun frenchKeepsItsApostrophe() {
        assertEquals("à l'instant", labelsForEachBucket().first())
    }

    /** Korean keeps exactly what it read before the strings were extracted. */
    @Test
    @Config(sdk = [33], qualifiers = "ko-rKR-w360dp-h640dp-mdpi")
    fun koreanIsUnchanged() {
        assertEquals(
            listOf(
                "방금 전",
                "방금 전",
                "1분 전",
                "59분 전",
                "1시간 전",
                "23시간 전",
                "1일 전",
                "5일 전"
            ),
            labelsForEachBucket()
        )
    }

    /** A timestamp from the future (clock skew across devices) stays at "just now". */
    @Test
    fun futureTimestampDoesNotFallThroughToDays() {
        assertEquals("just now", labelsFor(-90 * MINUTE).single())
    }

    private fun labelsForEachBucket(): List<String> = labelsFor(
        0,
        59 * SECOND,
        MINUTE,
        59 * MINUTE + 59 * SECOND,
        HOUR,
        23 * HOUR + 59 * MINUTE,
        DAY,
        5 * DAY + 3 * HOUR
    )

    private fun labelsFor(vararg elapsed: Long): List<String> {
        val labels = mutableListOf<String>()
        composeRule.setContent {
            labels.clear()
            for (millis in elapsed) {
                labels += elapsedTimeLabel(epochMillis = NOW - millis, now = NOW)
            }
        }
        composeRule.waitForIdle()
        return labels.toList()
    }

    private companion object {
        /** Any fixed instant; the helper only ever looks at the difference. */
        const val NOW = 1_700_000_000_000L

        const val SECOND = 1_000L
        const val MINUTE = 60 * SECOND
        const val HOUR = 60 * MINUTE
        const val DAY = 24 * HOUR
    }
}
