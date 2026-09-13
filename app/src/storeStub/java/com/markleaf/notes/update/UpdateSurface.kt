package com.markleaf.notes.update

import androidx.compose.runtime.Composable

/**
 * 스토어 배포 산출물(F-Droid·Play)이 얻는 이음매. **아무것도 하지 않는다.**
 *
 * 이 파일은 업데이터가 아니라 그 자리에 있는 빈 구멍이다. 실제 구현은 `src/sideload/java`의
 * 같은 이름 파일이고, 둘은 `markleaf.updater` 속성에 따라 **배타적으로** 컴파일된다
 * (D074, `docs/AGENT_SPEC.md` §15.9). 그래서 스토어 빌드에는 업데이트 확인 코드도,
 * 그것이 필요로 하는 INTERNET 권한도 존재하지 않는다.
 *
 * 런타임 플래그 하나로 끄지 않는 이유가 여기 있다: 그렇게 하면 업데이터 코드가 `main`에 남고,
 * R8이 지워도 소스를 읽는 사람에게는 보인다. 리뷰어와 privacy 문서 독자가 확인하는 것은 소스다.
 */
internal object UpdateSurface {

    /** 설정 화면의 앱 섹션에 붙는 행들. 스토어 빌드에서는 아무 행도 그리지 않는다. */
    @Composable
    fun SettingsRows() = Unit
}
