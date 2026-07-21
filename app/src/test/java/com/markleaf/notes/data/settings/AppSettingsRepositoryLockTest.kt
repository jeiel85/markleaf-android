package com.markleaf.notes.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.markleaf.notes.core.security.PasscodeBackoff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Covers the repository glue around [PasscodeBackoff] — persisting and
 * clearing the failure streak and the retry deadline, which used to be
 * verified only by hand on a device (#158).
 *
 * Why an in-memory [DataStore]: the production `preferencesDataStore` delegate
 * caches one instance per JVM (so Robolectric's per-method data dirs can never
 * work with it), and DataStore 1.0's file writes replace the target via
 * `File.renameTo`, which fails on Windows for every write after the first —
 * even with a fresh file per test. The repository therefore takes an
 * injectable [DataStore], and these tests exercise the real transform logic
 * over an in-memory implementation. Durability of the file itself is the
 * library's guarantee, not this repository's; what is ours — which keys are
 * written, cleared, and consulted, and in what order — is exactly what runs
 * here, identically on Windows and Linux.
 */
class AppSettingsRepositoryLockTest {

    /** Minimal faithful [DataStore]: serialized transforms over a state flow. */
    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        private val mutex = Mutex()
        override val data: Flow<Preferences> = state

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences
        ): Preferences = mutex.withLock {
            val updated = transform(state.value)
            state.value = updated
            updated
        }
    }

    private val store = InMemoryPreferencesDataStore()
    private val repo = AppSettingsRepository(store)

    private val now = 1_000_000L
    private val lockoutDeadline = now + PasscodeBackoff.BASE_LOCKOUT_MILLIS

    private suspend fun failUntilLockedOut(repository: AppSettingsRepository) {
        repeat(PasscodeBackoff.FREE_ATTEMPTS + 1) {
            repository.attemptLockPasscode("wrong", now = now)
        }
    }

    @Test
    fun freeAttemptsStayFreeAndTheNextFailureStartsLockout() = runTest {
        repo.setLockPasscode("4711")

        repeat(PasscodeBackoff.FREE_ATTEMPTS) { attempt ->
            assertEquals(
                "attempt ${attempt + 1} should still be free",
                LockPasscodeAttempt.Wrong(retryAtMillis = 0L),
                repo.attemptLockPasscode("wrong", now = now)
            )
        }

        assertEquals(
            LockPasscodeAttempt.Wrong(retryAtMillis = lockoutDeadline),
            repo.attemptLockPasscode("wrong", now = now)
        )
        assertEquals(lockoutDeadline, repo.lockPasscodeRetryAtMillis())
    }

    @Test
    fun activeLockoutRefusesWithoutCheckingEvenTheCorrectPasscode() = runTest {
        repo.setLockPasscode("4711")
        failUntilLockedOut(repo)

        assertEquals(
            LockPasscodeAttempt.LockedOut(lockoutDeadline),
            repo.attemptLockPasscode("4711", now = lockoutDeadline - 1)
        )
        assertEquals(
            LockPasscodeAttempt.Success,
            repo.attemptLockPasscode("4711", now = lockoutDeadline + 1)
        )
    }

    @Test
    fun successClearsTheStreakSoTheNextFailureIsFreeAgain() = runTest {
        repo.setLockPasscode("4711")
        failUntilLockedOut(repo)

        val afterLockout = lockoutDeadline + 1
        assertEquals(
            LockPasscodeAttempt.Success,
            repo.attemptLockPasscode("4711", now = afterLockout)
        )

        // A fresh failure after success must be free — the streak was reset.
        assertEquals(
            LockPasscodeAttempt.Wrong(retryAtMillis = 0L),
            repo.attemptLockPasscode("wrong", now = afterLockout)
        )
        assertEquals(0L, repo.lockPasscodeRetryAtMillis())
    }

    @Test
    fun lockoutSurvivesARepositoryRecreationOverTheSameStore() = runTest {
        repo.setLockPasscode("4711")
        failUntilLockedOut(repo)

        // A new repository over the same store — the glue must read the
        // persisted deadline back rather than track it in memory.
        val second = AppSettingsRepository(store)
        assertEquals(lockoutDeadline, second.lockPasscodeRetryAtMillis())
        assertEquals(
            LockPasscodeAttempt.LockedOut(lockoutDeadline),
            second.attemptLockPasscode("4711", now = lockoutDeadline - 1)
        )
    }

    @Test
    fun settingANewPasscodeClearsTheBackoff() = runTest {
        repo.setLockPasscode("4711")
        failUntilLockedOut(repo)

        repo.setLockPasscode("8090")

        // Still inside what would have been the lockout window — but setting a
        // passcode proves owner intent and starts a clean streak.
        assertEquals(0L, repo.lockPasscodeRetryAtMillis())
        assertEquals(
            LockPasscodeAttempt.Success,
            repo.attemptLockPasscode("8090", now = now + 1)
        )
    }

    @Test
    fun clearingThePasscodeRemovesHashAndBackoffTogether() = runTest {
        repo.setLockPasscode("4711")
        failUntilLockedOut(repo)

        repo.clearLockPasscode()

        assertFalse(repo.verifyLockPasscode("4711"))
        assertEquals(0L, repo.lockPasscodeRetryAtMillis())
        assertFalse(repo.settings.first().lockPasscodeSet)
    }

    @Test
    fun verifyReturnsFalseWhenNoPasscodeWasEverSet() = runTest {
        assertFalse(repo.verifyLockPasscode("anything"))
        assertEquals(0L, repo.lockPasscodeRetryAtMillis())
    }
}
