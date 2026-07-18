package com.markleaf.notes.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasscodeBackoffTest {
    @Test
    fun theFirstFewSlipsAreFree() {
        (0..PasscodeBackoff.FREE_ATTEMPTS).forEach { failures ->
            assertEquals(
                "expected no lockout after $failures failures",
                0L,
                PasscodeBackoff.lockoutMillisFor(failures)
            )
        }
    }

    @Test
    fun theFirstPenalizedFailureWaitsTheBaseInterval() {
        assertEquals(
            PasscodeBackoff.BASE_LOCKOUT_MILLIS,
            PasscodeBackoff.lockoutMillisFor(PasscodeBackoff.FREE_ATTEMPTS + 1)
        )
    }

    @Test
    fun eachFurtherFailureDoublesTheWaitUntilTheCap() {
        val first = PasscodeBackoff.lockoutMillisFor(PasscodeBackoff.FREE_ATTEMPTS + 1)
        val second = PasscodeBackoff.lockoutMillisFor(PasscodeBackoff.FREE_ATTEMPTS + 2)
        val third = PasscodeBackoff.lockoutMillisFor(PasscodeBackoff.FREE_ATTEMPTS + 3)
        assertEquals(first * 2, second)
        assertEquals(second * 2, third)
    }

    @Test
    fun theWaitIsCappedSoTheOwnerIsNeverLockedOutForever() {
        listOf(20, 50, 1_000, Int.MAX_VALUE).forEach { failures ->
            val lockout = PasscodeBackoff.lockoutMillisFor(failures)
            assertEquals(
                "expected the cap after $failures failures",
                PasscodeBackoff.MAX_LOCKOUT_MILLIS,
                lockout
            )
        }
    }

    @Test
    fun noDeadlineMeansAttemptsAreAllowed() {
        assertEquals(false, PasscodeBackoff.isLockedOut(retryAtMillis = 0L, now = 0L))
        assertEquals(false, PasscodeBackoff.isLockedOut(retryAtMillis = 0L, now = 9_999L))
    }

    @Test
    fun attemptsAreRefusedUntilTheDeadlinePasses() {
        val deadline = 10_000L
        assertEquals(true, PasscodeBackoff.isLockedOut(deadline, now = deadline - 1))
        // At the deadline the gate reopens — the wait is over, not still running.
        assertEquals(false, PasscodeBackoff.isLockedOut(deadline, now = deadline))
        assertEquals(false, PasscodeBackoff.isLockedOut(deadline, now = deadline + 1))
    }

    @Test
    fun freeFailuresPersistNoDeadline() {
        (0 until PasscodeBackoff.FREE_ATTEMPTS).forEach { previous ->
            assertEquals(
                "failure ${previous + 1} should still be free",
                0L,
                PasscodeBackoff.retryAtAfterFailure(previous, now = 5_000L)
            )
        }
    }

    @Test
    fun thePenalizedFailureDeadlineIsRelativeToNow() {
        val now = 5_000L
        assertEquals(
            now + PasscodeBackoff.BASE_LOCKOUT_MILLIS,
            PasscodeBackoff.retryAtAfterFailure(PasscodeBackoff.FREE_ATTEMPTS, now)
        )
        assertEquals(
            now + PasscodeBackoff.BASE_LOCKOUT_MILLIS * 2,
            PasscodeBackoff.retryAtAfterFailure(PasscodeBackoff.FREE_ATTEMPTS + 1, now)
        )
    }

    @Test
    fun theWaitNeverGoesNegativeOrShrinks() {
        var previous = 0L
        (0..80).forEach { failures ->
            val lockout = PasscodeBackoff.lockoutMillisFor(failures)
            assertTrue("negative lockout at $failures failures", lockout >= 0L)
            assertTrue("lockout shrank at $failures failures", lockout >= previous)
            previous = lockout
        }
    }
}
