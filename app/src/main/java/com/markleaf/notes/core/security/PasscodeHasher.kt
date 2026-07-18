package com.markleaf.notes.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2 hashing for the "Locked notes" passcode (#155).
 *
 * This backs a *UI-visibility* gate, not encryption at rest — the note bodies
 * stay in the same Room DB as plain text. All the hash protects is the passcode
 * itself: someone who reads the DataStore file sees only a salted PBKDF2 digest,
 * never the passcode. Everything runs on-device with the platform crypto
 * provider (`PBKDF2WithHmacSHA256` and `java.util.Base64` are both available from
 * API 26, the project minSdk), so it stays fully offline and F-Droid friendly.
 *
 * Pure and deterministic given an explicit salt, so it is unit-testable on the
 * plain JVM without Robolectric.
 */
object PasscodeHasher {

    // 120k iterations keeps a brute-force attempt against the stored digest slow
    // while staying well under a second on a low-end API 26 device (callers run
    // it off the main thread anyway).
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    /** A fresh random salt, Base64-encoded for DataStore storage. */
    fun newSaltBase64(): String {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        SecureRandom().nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    /** Base64-encoded PBKDF2 digest of [passcode] with [saltBase64]. */
    fun hash(passcode: String, saltBase64: String): String {
        val salt = Base64.getDecoder().decode(saltBase64)
        val spec = PBEKeySpec(passcode.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val digest = factory.generateSecret(spec).encoded
            return Base64.getEncoder().encodeToString(digest)
        } finally {
            // Zero the passcode copy the spec kept, so it doesn't linger in memory.
            spec.clearPassword()
        }
    }

    /** Constant-time check that [passcode] hashes to [expectedHashBase64]. */
    fun verify(passcode: String, saltBase64: String, expectedHashBase64: String): Boolean {
        val actual = hash(passcode, saltBase64).toByteArray(Charsets.US_ASCII)
        val expected = expectedHashBase64.toByteArray(Charsets.US_ASCII)
        // MessageDigest.isEqual is constant-time on modern platforms, avoiding a
        // timing side channel that a plain String.equals would leak.
        return MessageDigest.isEqual(actual, expected)
    }
}
