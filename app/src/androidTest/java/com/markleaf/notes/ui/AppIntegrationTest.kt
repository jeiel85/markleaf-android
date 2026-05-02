package com.markleaf.notes.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.markleaf.notes.MainActivity
import com.markleaf.notes.R
import org.junit.Rule
import org.junit.Test

class AppIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val context get() = composeTestRule.activity

    @Test
    fun testCreateAndSaveNoteFlow() {
        // 1. 앱 실행 후 'Add Note' 버튼(FAB) 클릭
        val addNoteLabel = context.getString(R.string.add_note)
        composeTestRule.onNodeWithContentDescription(addNoteLabel).performClick()

        // 2. 에디터에서 텍스트 입력
        val testContent = "# Integration Test\nThis is a test note."
        val noteContentLabel = context.getString(R.string.note_content)
        composeTestRule.onNodeWithContentDescription(noteContentLabel).performTextReplacement(testContent)

        // 3. 자동 저장을 기다린 후 뒤로가기
        // (저장 로직은 debounce 1초이므로 넉넉히 대기)
        composeTestRule.mainClock.advanceTimeBy(1500L)
        val backLabel = context.getString(R.string.back)
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()

        // 4. 목록 화면에서 작성한 노트 제목이 표시되는지 확인
        composeTestRule.onNodeWithText("Integration Test").assertIsDisplayed()
        
        // 5. 생성된 노트 클릭하여 내용 확인
        composeTestRule.onNodeWithText("Integration Test").performClick()
        composeTestRule.onNodeWithText(testContent).assertIsDisplayed()
    }

    @Test
    fun testSearchNavigation() {
        // 1. 메인 화면에서 검색 아이콘 클릭
        val searchLabel = context.getString(R.string.search)
        composeTestRule.onNodeWithContentDescription(searchLabel).performClick()

        // 2. 검색 화면으로 이동했는지 확인 (Placeholder 텍스트로 확인)
        val searchHint = context.getString(R.string.search_notes_hint)
        composeTestRule.onNodeWithText(searchHint).assertIsDisplayed()
    }
}
