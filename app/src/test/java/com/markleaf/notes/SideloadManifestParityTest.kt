package com.markleaf.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `AndroidManifest-sideload.xml`이 `AndroidManifest.xml`과 `INTERNET` 한 줄만 다른지 고정한다.
 *
 * 왜 이 테스트가 있는가: 사이드로드 빌드는 `markleaf.updater` 속성이 켜질 때 main 매니페스트를
 * 이 사본으로 **교체**한다(D074, `docs/AGENT_SPEC.md` §15.9). `sourceSets.main.manifest.srcFile`은
 * 병합이 아니라 교체라서 전체 사본이 필요하고, 사본이 조용히 낡는 것이 이 방식의 **유일한 실패
 * 양식**이다 — 누가 `AndroidManifest.xml`에 액티비티나 프로바이더를 추가하면 사이드로드 빌드만
 * 그것을 잃고, 아무 빌드도 빨개지지 않은 채 사용자에게만 드러난다. 사람 눈으로 두 파일을 대조하는
 * 규칙은 결국 잊히므로 검사로 고정한다. 플레이버였다면 AGP가 병합해 줘서 필요 없었을 검사이고,
 * 이것이 속성 게이트를 고른 대가다.
 *
 * 왜 XML 파서가 아니라 줄 비교인가: 사본은 손으로 유지되므로 의미가 같은지가 아니라 **글자가
 * 같은지**를 봐야 한다. 의미 비교는 들여쓰기·주석·순서 표류를 통과시키고, 그 표류가 다음 사고의
 * 씨앗이 된다. 그래서 사이드로드 매니페스트에는 설명 주석조차 두지 않았다 — 설명은 여기 있다.
 */
class SideloadManifestParityTest {

    @Test
    fun sideloadManifestIsMainManifestPlusExactlyTheInternetPermission() {
        // Input : app 모듈 기준 상대 경로의 두 매니페스트 (단위 테스트의 작업 디렉터리가 app 모듈)
        // Output: 사본이 본 매니페스트 + INTERNET 한 줄이 아니면 실패
        // 핵심 로직: 사본에서 INTERNET 줄을 빼면 본 매니페스트와 줄 단위로 완전히 같아야 한다.
        val main = File(MAIN_MANIFEST).readLines()
        val sideload = File(SIDELOAD_MANIFEST).readLines()

        val internetLines = sideload.filter { it.contains(INTERNET_PERMISSION) }
        assertEquals(
            "$SIDELOAD_MANIFEST must declare $INTERNET_PERMISSION exactly once",
            1,
            internetLines.size
        )

        assertEquals(
            "$SIDELOAD_MANIFEST drifted from $MAIN_MANIFEST. " +
                "두 파일은 $INTERNET_PERMISSION 한 줄만 달라야 한다 — " +
                "매니페스트에 무언가를 추가했다면 양쪽 모두에 반영해야 한다.",
            main,
            sideload.filterNot { it.contains(INTERNET_PERMISSION) }
        )
    }

    @Test
    fun storeManifestDeclaresNoInternetPermission() {
        // 제품 약속 그 자체다. README 8개 언어, docs/privacy.*.html 8개, 그리고 앱이 첫 실행에
        // 심는 starter_notes.md 8개 로케일이 이 문장을 말한다. 사이드로드 예외를 만든 뒤에도
        // 스토어 산출물에서는 그대로여야 하므로, 문서가 아니라 여기서 지킨다.
        val main = File(MAIN_MANIFEST).readText()

        assertTrue(
            "$MAIN_MANIFEST must not declare $INTERNET_PERMISSION — " +
                "스토어·F-Droid·Play 산출물의 권한 목록은 바뀌지 않는다 (docs/AGENT_SPEC.md §15.9)",
            !main.contains(INTERNET_PERMISSION)
        )
    }

    private companion object {
        const val MAIN_MANIFEST = "src/main/AndroidManifest.xml"
        const val SIDELOAD_MANIFEST = "src/main/AndroidManifest-sideload.xml"
        const val INTERNET_PERMISSION = "android.permission.INTERNET"
    }
}
