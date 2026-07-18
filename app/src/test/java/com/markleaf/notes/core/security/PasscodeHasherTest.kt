package com.markleaf.notes.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasscodeHasherTest {

    @Test
    fun `verify accepts the correct passcode`() {
        val salt = PasscodeHasher.newSaltBase64()
        val hash = PasscodeHasher.hash("1234", salt)

        assertTrue(PasscodeHasher.verify("1234", salt, hash))
    }

    @Test
    fun `verify rejects a wrong passcode`() {
        val salt = PasscodeHasher.newSaltBase64()
        val hash = PasscodeHasher.hash("1234", salt)

        assertFalse(PasscodeHasher.verify("12345", salt, hash))
        assertFalse(PasscodeHasher.verify("", salt, hash))
        assertFalse(PasscodeHasher.verify("4321", salt, hash))
    }

    @Test
    fun `hash is deterministic for the same passcode and salt`() {
        val salt = PasscodeHasher.newSaltBase64()

        assertEquals(
            PasscodeHasher.hash("open sesame", salt),
            PasscodeHasher.hash("open sesame", salt)
        )
    }

    @Test
    fun `same passcode under different salts yields different hashes`() {
        val a = PasscodeHasher.newSaltBase64()
        val b = PasscodeHasher.newSaltBase64()

        assertNotEquals(a, b)
        assertNotEquals(
            PasscodeHasher.hash("same", a),
            PasscodeHasher.hash("same", b)
        )
    }

    @Test
    fun `verify rejects the right passcode under a different salt`() {
        val salt = PasscodeHasher.newSaltBase64()
        val hash = PasscodeHasher.hash("secret", salt)
        val otherSalt = PasscodeHasher.newSaltBase64()

        assertFalse(PasscodeHasher.verify("secret", otherSalt, hash))
    }

    @Test
    fun `unicode passcodes round-trip`() {
        val salt = PasscodeHasher.newSaltBase64()
        val hash = PasscodeHasher.hash("비밀번호🔒", salt)

        assertTrue(PasscodeHasher.verify("비밀번호🔒", salt, hash))
        assertFalse(PasscodeHasher.verify("비밀번호", salt, hash))
    }
}
