package com.markleaf.notes.update

import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * 사이드로드 빌드의 두 네트워크 동작 중 **자동으로** 도는 쪽: 정적 JSON 하나를 GET 한다.
 * 다른 하나(사용자가 설치를 탭했을 때만 도는 업데이트 파일 자체의 GET)는 [UpdateDownloader]다.
 *
 * 노트·태그·첨부·메타데이터·식별자·사용 기록은 어떤 빌드에서도, 어느 GET에도 실리지 않는다
 * (`docs/AGENT_SPEC.md` §15.9). 요청에 붙는 것은 URL과 표준 헤더뿐이고, 쿼리스트링도
 * 쿠키도 붙이지 않는다.
 *
 * `api.github.com`을 부르지 않는 이유: GitHub의 비인증 REST 한도는 시간당 60건인데 그 한도가
 * 앱이 아니라 **IP에** 붙는다. 통신사 NAT 뒤에서는 남이 쓴 한도에 우리 확인이 막힌다. 우리가
 * 읽는 것은 REST API가 아니라 릴리스 자산 하나이고, 스키마도 우리가 통제한다.
 *
 * 그 자산을 GitHub Pages(`docs/update.json`)가 아니라 Release에 두는 이유는 D075다. 요약하면
 * `main`이 보호 브랜치라 태그 런이 `docs/`에 커밋을 밀 수 없다.
 */
internal class UpdateChecker(
    private val manifestUrl: String = DEFAULT_MANIFEST_URL,
) {

    /**
     * Input : 없음 (URL은 생성자 고정)
     * Output: 응답 본문 문자열, 또는 무엇이 잘못되든 `null`
     *
     * 핵심 로직: **모든 실패는 null이다.** 사용자가 요청한 적 없는 배경 확인이므로, 오프라인·DNS
     * 실패·타임아웃·5xx·깨진 본문이 전부 "이번엔 조용히 넘어간다"로 수렴한다. 확인 실패를 알리는
     * 것은 소음이고, 예외를 올려보내면 호출부마다 같은 catch를 다시 쓰게 된다.
     *
     * 본문 길이를 제한하는 이유: 이 JSON은 몇백 바이트다. 그보다 크면 우리가 올린 파일이 아니며,
     * 응답이 끝나지 않는 연결에 메모리를 내주지 않는다.
     *
     * 파싱하지 않고 원문을 돌려주는 이유: 확인은 하루 1회지만 배너는 그 사이에도 떠 있어야 한다.
     * 원문을 그대로 캐시해 두면 다음 실행에서 네트워크 없이 같은 판단을 다시 할 수 있다.
     */
    fun fetchRaw(): String? {
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
                connection.readBodyWithin(MAX_BODY_BYTES)
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
        /**
         * `releases/latest/download/<자산 이름>`은 **가장 최근 릴리스의** 같은 이름 자산으로
         * 302를 돌려주는 고정 주소다. 그래서 이 상수는 버전이 올라가도 그대로다 —
         * 앱에 박힌 주소를 릴리스마다 고쳐야 한다면 그 자체가 다음 사고다.
         *
         * `HttpURLConnection`은 https → https 리다이렉트를 기본으로 따라간다.
         */
        const val DEFAULT_MANIFEST_URL =
            "https://github.com/jeiel85/markleaf-android/releases/latest/download/update.json"
        private const val TIMEOUT_MILLIS = 10_000
        private const val MAX_BODY_BYTES = 64 * 1024
    }
}
