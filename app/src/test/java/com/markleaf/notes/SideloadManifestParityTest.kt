package com.markleaf.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `AndroidManifest-sideload.xml`이 `AndroidManifest.xml`에 **정확히 이 저장소가 아는 블록만**
 * 더한 것인지 고정한다.
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
 * 씨앗이 된다.
 *
 * **C단계(2026-09-14)에서 추가분이 한 줄에서 두 블록으로 늘었다.** `INTERNET`만 필터링하던
 * 원래 검사로는 `REQUEST_INSTALL_PACKAGES`와 `UpdateInstallReceiver` 선언을 놓친다. 그래서
 * "허용된 추가분 목록에서 정확히 이 블록들을 빼면 main과 완전히 같다"로 일반화했다 — 이 사본에
 * 설명 주석을 둔 것도 이 시점부터다(리시버 블록); 그 주석까지 통째로 허용 목록에 넣어 정확히
 * 대조하므로, 주석 표류도 여전히 잡는다.
 */
class SideloadManifestParityTest {

    @Test
    fun sideloadManifestIsMainManifestPlusExactlyTheKnownExtraBlocks() {
        // Input : app 모듈 기준 상대 경로의 두 매니페스트 (단위 테스트의 작업 디렉터리가 app 모듈)
        // Output: 알려진 블록들을 뺀 사본이 본 매니페스트와 줄 단위로 완전히 같지 않으면 실패
        // 핵심 로직: 각 블록을 사본에서 정확히 한 번, 연속된 줄로 찾아 제거한다. 순서·중복·위치
        // 이동은 모두 실패로 잡는다 — remove가 "그 블록이 정확히 그 형태로 있었다"를 전제하기
        // 때문이다.
        val main = File(MAIN_MANIFEST).readLines()
        var remaining = File(SIDELOAD_MANIFEST).readLines()

        for (block in KNOWN_EXTRA_BLOCKS) {
            remaining = remaining.removeContiguousBlockOnce(block)
        }

        assertEquals(
            "$SIDELOAD_MANIFEST drifted from $MAIN_MANIFEST. " +
                "알려진 추가 블록만 뺐을 때 완전히 같아야 한다 — " +
                "매니페스트에 무언가를 추가했다면 양쪽 모두에 반영하거나, " +
                "사이드로드 전용이라면 이 테스트의 KNOWN_EXTRA_BLOCKS에도 반영해야 한다.",
            main,
            remaining
        )
    }

    @Test
    fun storeManifestDeclaresNeitherInternetNorInstallPackagesPermission() {
        // 제품 약속 그 자체다. README 8개 언어, docs/privacy.*.html 8개, 그리고 앱이 첫 실행에
        // 심는 starter_notes.md 7개 로케일이 이 문장을 말한다. 사이드로드 예외를 만든 뒤에도
        // 스토어 산출물에서는 그대로여야 하므로, 문서가 아니라 여기서 지킨다.
        //
        // REQUEST_INSTALL_PACKAGES도 같은 이유로 여기서 막는다 — 이 권한이 스토어 산출물에
        // 새는 것은 INTERNET이 새는 것과 같은 종류의 사고다: 둘 다 "이 산출물에는 업데이터
        // 코드가 없다"는 §15.9의 전제를 깬다.
        val main = File(MAIN_MANIFEST).readText()

        assertTrue(
            "$MAIN_MANIFEST must not declare $INTERNET_PERMISSION — " +
                "스토어·F-Droid·Play 산출물의 권한 목록은 바뀌지 않는다 (docs/AGENT_SPEC.md §15.9)",
            !main.contains(INTERNET_PERMISSION)
        )
        assertTrue(
            "$MAIN_MANIFEST must not declare $INSTALL_PACKAGES_PERMISSION — " +
                "설치 위임은 사이드로드 전용 기능이다 (docs/AGENT_SPEC.md §15.9)",
            !main.contains(INSTALL_PACKAGES_PERMISSION)
        )
    }

    /**
     * Input : 줄 목록, 그 안에서 정확히 한 번 연속으로 나타나야 하는 부분 목록
     * Output: 그 부분 목록을 뺀 나머지
     *
     * 핵심 로직: 부분 목록이 없거나 두 번 이상 나오면 이 테스트 자체가 실패해야 한다 — "빠뜨림"과
     * "중복"도 표류이기 때문이다. `List.indexOfSlice`가 표준 라이브러리에 없어 직접 찾는다.
     */
    private fun List<String>.removeContiguousBlockOnce(block: List<String>): List<String> {
        val matches = (0..size - block.size).count { start ->
            subList(start, start + block.size) == block
        }
        assertEquals(
            "'${block.firstOrNull { it.isNotBlank() }}'로 시작하는 블록이 $SIDELOAD_MANIFEST 안에 " +
                "정확히 한 번 연속으로 있어야 한다(실제 $matches 회).",
            1,
            matches
        )
        val start = indices.first { subList(it, it + block.size) == block }
        return subList(0, start) + subList(start + block.size, size)
    }

    private companion object {
        const val MAIN_MANIFEST = "src/main/AndroidManifest.xml"
        const val SIDELOAD_MANIFEST = "src/main/AndroidManifest-sideload.xml"
        const val INTERNET_PERMISSION = "android.permission.INTERNET"
        const val INSTALL_PACKAGES_PERMISSION = "android.permission.REQUEST_INSTALL_PACKAGES"

        // 사본에 실제로 들어 있는 그대로 — 어긋나면 이 상수를 고치는 것이 아니라, 사본이
        // 의도한 대로인지부터 확인해야 한다.
        val PERMISSION_BLOCK = listOf(
            "    <uses-permission android:name=\"android.permission.INTERNET\" />",
            "    <uses-permission android:name=\"android.permission.REQUEST_INSTALL_PACKAGES\" />"
        )
        val INSTALL_RECEIVER_BLOCK = listOf(
            "",
            "        <!-- PackageInstaller 세션이 STATUS_PENDING_USER_ACTION을 콜백으로 돌려줄 때만",
            "             불린다(D073, C단계). intent-filter가 없는 이유: UpdateInstaller가 이 컴포넌트를",
            "             명시적 Intent로만 가리키고, 시스템 액션 문자열로 호출되는 일이 없다. -->",
            "        <receiver",
            "            android:name=\".update.UpdateInstallReceiver\"",
            "            android:exported=\"false\" />"
        )
        val KNOWN_EXTRA_BLOCKS = listOf(PERMISSION_BLOCK, INSTALL_RECEIVER_BLOCK)
    }
}
