package com.markleaf.notes.update

import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * 사이드로드 빌드가 하는 **유일한** 네트워크 동작: 정적 JSON 하나를 GET 한다.
 *
 * 노트·태그·첨부·메타데이터·식별자·사용 기록은 어떤 빌드에서도 전송하지 않는다
 * (`docs/AGENT_SPEC.md` §15.9). 요청에 붙는 것은 URL과 표준 헤더뿐이고, 쿼리스트링도
 * 쿠키도 붙이지 않는다.
 *
 * `api.github.com` 대신 GitHub Pages의 정적 파일을 읽는 이유: GitHub의 비인증 REST 한도는
 * 시간당 60건인데 그 한도가 앱이 아니라 **IP에** 붙는다. 통신사 NAT 뒤에서는 남이 쓴 한도에
 * 우리 확인이 막힌다.
 */
internal class UpdateChecker(
    private val manifestUrl: String = DEFAULT_MANIFEST_URL,
) {

    /**
     * Input : 없음 (URL은 생성자 고정)
     * Output: 온전한 [UpdateManifest], 또는 무엇이 잘못되든 `null`
     *
     * 핵심 로직: **모든 실패는 null이다.** 사용자가 요청한 적 없는 배경 확인이므로, 오프라인·DNS
     * 실패·타임아웃·5xx·깨진 본문이 전부 "이번엔 조용히 넘어간다"로 수렴한다. 확인 실패를 알리는
     * 것은 소음이고, 예외를 올려보내면 호출부마다 같은 catch를 다시 쓰게 된다.
     *
     * 본문 길이를 제한하는 이유: 이 JSON은 몇백 바이트다. 그보다 크면 우리가 올린 파일이 아니며,
     * 응답이 끝나지 않는 연결에 메모리를 내주지 않는다.
     */
    fun fetch(): UpdateManifest? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(manifestUrl).openConnection() as HttpsURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
                useCaches = false
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                null
            } else {
                UpdateManifestParser.parse(connection.readBodyWithin(MAX_BODY_BYTES))
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun HttpURLConnection.readBodyWithin(limit: Int): String =
        inputStream.bufferedReader().use { reader ->
            val buffer = CharArray(limit)
            val read = reader.read(buffer, 0, limit)
            if (read <= 0) "" else String(buffer, 0, read)
        }

    internal companion object {
        const val DEFAULT_MANIFEST_URL = "https://jeiel85.github.io/markleaf-android/update.json"
        private const val TIMEOUT_MILLIS = 10_000
        private const val MAX_BODY_BYTES = 64 * 1024
    }
}
