package com.markleaf.notes.core.security

/**
 * Backoff policy for repeated wrong "Locked notes" passcode attempts (#156).
 *
 * PBKDF2 already makes each guess expensive, but without a lockout an attacker
 * with the device in hand can keep guessing a short passcode indefinitely. The
 * first few slips are free — a typo shouldn't punish the owner — after which
 * each additional consecutive failure doubles the wait, up to a cap.
 *
 * The deadline is wall-clock based and persisted, so force-stopping the app
 * cannot clear an in-progress backoff. Someone who can change the system clock
 * can still shorten the wait; that is accepted, because the Locked space is a
 * UI-visibility gate rather than encryption at rest, and the same person could
 * read the database directly.
 */
object PasscodeBackoff {
    /** Consecutive failures tolerated before any wait is imposed. */
    const val FREE_ATTEMPTS = 4

    /** Wait after the first non-free failure. */
    const val BASE_LOCKOUT_MILLIS = 30_000L

    /** Upper bound so the owner is never locked out for an unreasonable time. */
    const val MAX_LOCKOUT_MILLIS = 300_000L

    /**
     * How long to refuse attempts after [consecutiveFailures] consecutive wrong
     * passcodes. Returns 0 while the failure count is still within
     * [FREE_ATTEMPTS].
     */
    fun lockoutMillisFor(consecutiveFailures: Int): Long {
        val penalized = consecutiveFailures - FREE_ATTEMPTS
        if (penalized <= 0) return 0L
        // Shift instead of pow, and cap the exponent before shifting so a large
        // failure count cannot overflow into a negative wait.
        val steps = (penalized - 1).coerceAtMost(20)
        val scaled = BASE_LOCKOUT_MILLIS shl steps
        return scaled.coerceAtMost(MAX_LOCKOUT_MILLIS)
    }

    /**
     * Whether attempts are currently refused. [retryAtMillis] is the persisted
     * deadline, 0 when no backoff is running.
     */
    fun isLockedOut(retryAtMillis: Long, now: Long): Boolean = retryAtMillis > now

    /**
     * The deadline to persist after a wrong passcode, given how many consecutive
     * failures preceded it. Returns 0 while attempts are still free, which the
     * caller stores as "no backoff".
     */
    fun retryAtAfterFailure(previousFailures: Int, now: Long): Long {
        val lockout = lockoutMillisFor(previousFailures + 1)
        return if (lockout > 0L) now + lockout else 0L
    }
}
