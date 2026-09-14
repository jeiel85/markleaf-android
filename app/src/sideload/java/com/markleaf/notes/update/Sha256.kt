package com.markleaf.notes.update

import java.io.File
import java.security.MessageDigest

/**
 * 다운로드한 APK가 manifest의 `sha256`과 같은 파일인지 대조한다.
 *
 * 서명 검증은 결국 OS가 한다 — 다른 키로 서명된 APK는 설치 단계에서
 * `INSTALL_FAILED_UPDATE_INCOMPATIBLE`로 거부된다(`docs/RELEASE.md`의 동일 인증서 설명).
 * 그런데 그 실패는 설치 확인 팝업까지 다 띄운 **뒤에** 나므로, 사용자 입장에서는 "다운로드는
 * 됐는데 설치가 이유 없이 거부된다"로 보인다. 여기서 먼저 걸러 두면 그런 혼란스러운 실패를
 * 겪기 전에 "다운로드가 손상됐다"는 분명한 이유로 멈춘다(`docs/UPDATE_STRATEGY_EVALUATION.md`
 * "C단계 구현 세부").
 *
 * Android/Kotlin API에만 기대는 순수 함수라 게이트 뒤에 있을 이유는 없지만, 이 파일을 쓰는
 * 유일한 코드(`UpdateDownloader`)가 게이트 뒤에 있으므로 같은 자리에 둔다.
 */
internal object Sha256 {

    /**
     * Input : 해시를 구할 파일
     * Output: 소문자 16진수 SHA-256 해시 문자열(64자)
     *
     * 핵심 로직: 파일 전체를 한 번에 메모리에 올리지 않고 스트리밍으로 다이제스트에 먹인다 —
     * APK가 몇 MB뿐이라 지금은 상관없지만, 스트리밍이 기본이면 파일이 커져도 안전하다.
     */
    fun of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    /**
     * Input : 파일, manifest가 적어 둔 기대 해시
     * Output: 대조 결과
     *
     * 핵심 로직: 대소문자를 무시한다 — hex 표기는 관례상 소문자지만 강제된 표준은 아니다.
     */
    fun matches(file: File, expectedHex: String): Boolean =
        of(file).equals(expectedHex, ignoreCase = true)

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte -> "%02x".format(byte) }

    private const val BUFFER_SIZE = 64 * 1024
}
