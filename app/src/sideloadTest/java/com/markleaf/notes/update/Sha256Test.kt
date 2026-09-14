package com.markleaf.notes.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.charset.StandardCharsets

/**
 * `Sha256`은 `java.security.MessageDigest` + `File`만 쓰는 순수 JVM 코드라 Robolectric이
 * 필요 없다 — `UpdateManifestParserTest`(org.json 때문에 Robolectric 필요)와 다른 이유로
 * 게이트 뒤(`src/sideloadTest/java`)에 있다: 대상 클래스가 `src/sideload/java`에만 있어서다.
 *
 * 기대값은 전부 `hashlib.sha256(...).hexdigest()`로 로컬에서 직접 계산해 확인한 것이다
 * (표준 SHA-256 구현이므로 어느 언어로 계산해도 같아야 한다) — 기억이나 추정이 아니다.
 */
class Sha256Test {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun ofMatchesTheStandardDigestForAnEmptyFile() {
        val file = tempFolder.newFile()
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Sha256.of(file)
        )
    }

    @Test
    fun ofMatchesTheStandardDigestForKnownContent() {
        val file = tempFolder.newFile()
        file.writeBytes("abc".toByteArray(StandardCharsets.US_ASCII))
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Sha256.of(file)
        )
    }

    @Test
    fun ofHandlesContentLargerThanTheInternalBuffer() {
        // 내부 버퍼가 64 KiB다. 그보다 큰 파일에서 스트리밍 루프가 청크 경계를 제대로
        // 넘는지는 작은 입력만으로는 확인되지 않는다.
        val file = tempFolder.newFile()
        val oneMegabyte = ByteArray(1_000_000) { (it % 251).toByte() }
        file.writeBytes(oneMegabyte)

        val viaStreamingImplementation = Sha256.of(file)
        val viaWholeArrayDigest = java.security.MessageDigest
            .getInstance("SHA-256")
            .digest(oneMegabyte)
            .joinToString(separator = "") { "%02x".format(it) }

        assertEquals(viaWholeArrayDigest, viaStreamingImplementation)
    }

    @Test
    fun matchesIgnoresCase() {
        val file = tempFolder.newFile()
        file.writeBytes("markleaf".toByteArray(StandardCharsets.US_ASCII))
        val expected = "8137924e1f65d8714e7bd6d38e90ac3e098af1d843680fdc78a3416cf31cf658"

        assertTrue(Sha256.matches(file, expected))
        assertTrue(Sha256.matches(file, expected.uppercase()))
    }

    @Test
    fun matchesRejectsAWrongHash() {
        // 다운로드가 손상되면 해시가 우연히 맞아떨어질 일은 없어야 한다는, 이 대조의 존재
        // 이유 자체를 확인한다.
        val file = tempFolder.newFile()
        file.writeBytes("markleaf".toByteArray(StandardCharsets.US_ASCII))

        assertFalse(Sha256.matches(file, "0".repeat(64)))
    }
}
