package com.markleaf.notes.update

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * C단계의 유일한 새 네트워크 동작: manifest의 `apkUrl`을 통째로 받는다.
 *
 * `UpdateChecker`와 짝을 이루되 하나로 합치지 않은 이유: 그쪽은 몇백 바이트짜리 JSON을
 * 매일 자동으로 받고, 이쪽은 몇 MB짜리 실행 파일을 **사용자가 탭했을 때만** 받는다. 실패
 * 시 사용자에게 보여줄 것이 없는 배경 작업과, 실패를 보여주고 재시도를 물어야 하는 사용자
 * 주도 작업은 성격이 다르다.
 *
 * 이 클래스는 `UpdateChecker`처럼 스레드를 스스로 정하지 않는다 — 호출부가 IO 디스패처에서
 * 부른다(`UpdateSurface.refresh`가 `UpdateChecker.fetchRaw`를 부르는 것과 같은 관례).
 */
internal class UpdateDownloader {

    sealed interface Result {
        data class Success(val file: File) : Result
        data object Failed : Result
    }

    /**
     * Input : 다운로드할 URL, 저장할 목적지 파일, manifest에 실린 기대 SHA-256
     * Output: 검증까지 끝난 파일, 또는 실패
     *
     * 핵심 로직: **다운로드가 끝나도 해시가 맞지 않으면 성공이 아니다.** 매 시도마다 목적지를
     * 먼저 지운다 — 이전 시도의 부분 파일이 남아 있으면 다음 시도의 결과와 뒤섞일 수 있다.
     * 재시도 상한(`MAX_ATTEMPTS`)을 두는 이유는 `UpdateChecker`와 달리 이 실패는 사용자에게
     * 보여야 하는데, 일시적인 네트워크 끊김 한 번으로 그 실패 화면을 띄우고 싶지 않아서다.
     * 실패가 반복되면(진짜 문제) 그때는 사용자에게 알리는 것이 맞다 — 그래서 상한이 있다.
     */
    fun download(url: String, destination: File, expectedSha256Hex: String): Result {
        repeat(MAX_ATTEMPTS) {
            destination.delete()
            if (attemptDownload(url, destination) && Sha256.matches(destination, expectedSha256Hex)) {
                return Result.Success(destination)
            }
        }
        destination.delete()
        return Result.Failed
    }

    /**
     * Input : URL, 목적지 파일
     * Output: 응답을 끝까지 받아 그 파일에 썼는지 여부(해시는 여기서 보지 않는다)
     *
     * 핵심 로직: `UpdateChecker.fetchRaw`와 같은 모양이다 — 모든 실패가 false로 수렴한다.
     * 여기서는 "왜" 실패했는지 구분하지 않는다: 네트워크 문제든 서버 문제든 사용자가 보는
     * 화면은 같고("다시 시도" 또는 "브라우저에서 열기"), 원인을 구분해 봐야 이 앱은 원격
     * 로그를 남기지 않으므로 그 정보를 어디에도 쓸 수 없다.
     */
    private fun attemptDownload(url: String, destination: File): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpsURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
                useCaches = false
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return false
            }
            destination.parentFile?.mkdirs()
            connection.inputStream.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }

    internal companion object {
        // UpdateChecker와 같은 상수값이다 — 이 GET도 같은 GitHub Releases 인프라를 향한다.
        // 다만 응답이 크므로(몇 MB) 이 타임아웃은 "연결"과 "한 번의 read()"에 대한 것이지
        // 전체 전송 시간의 상한이 아니다: 데이터가 계속 들어오는 한 계속 받는다.
        private const val TIMEOUT_MILLIS = 10_000
        private const val MAX_ATTEMPTS = 3
    }
}
