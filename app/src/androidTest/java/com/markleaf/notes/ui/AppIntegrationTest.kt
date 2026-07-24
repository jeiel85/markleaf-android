package com.markleaf.notes.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.markleaf.notes.R
import com.markleaf.notes.TestHostActivity
import com.markleaf.notes.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppIntegrationTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: AppDatabase
    private var preExistingNoteIds: Set<String> = emptySet()
    private var scenario: ActivityScenario<TestHostActivity>? = null

    @Before
    fun setUp() {
        // 앱이 실제로 쓰는 데이터베이스를 그대로 쓴다. `EditorScreen` 은 주입받은
        // repository 가 아니라 `AppDatabase.getInstance(context)` 에서 자기 것을
        // 만들기 때문에(EditorScreen.kt), 인메모리 DB 를 NavHost 에만 주입하면
        // 목록과 에디터가 서로 다른 DB 를 보게 된다. 그러면 FAB 이 만든 노트를
        // 에디터의 자동 저장이 찾지 못해 조용히 아무것도 저장하지 않는다 —
        // 이 테스트가 오래 실패해 온 진짜 이유다(#235, #239).
        database = AppDatabase.getInstance(context)
        preExistingNoteIds = runBlocking { database.noteDao().getAllNotes() }
            .map { it.id }
            .toSet()
        TestHostActivity.content = markleafTestContent(database)
        scenario = ActivityScenario.launch(TestHostActivity::class.java)
        composeTestRule.waitForIdle()
    }

    @After
    fun tearDown() {
        scenario?.close()
        TestHostActivity.content = null
        // 공용 DB 를 쓰므로 이 테스트가 만든 노트만 골라 지운다. 기기에 이미
        // 있던 노트는 건드리지 않는다.
        runBlocking {
            database.noteDao().getAllNotes()
                .filterNot { it.id in preExistingNoteIds }
                .forEach { database.noteDao().deleteForever(it.id) }
        }
    }

    @Test
    fun testCreateAndSaveNoteFlow() {
        // 1. 앱 실행 후 'Add Note' 버튼(FAB) 클릭
        val addNoteLabel = context.getString(R.string.add_note)
        composeTestRule.onNodeWithContentDescription(addNoteLabel).performClick()

        // 2. 에디터에서 텍스트 입력
        val testContent = "# Integration Test\nThis is a test note."
        val noteContentLabel = context.getString(R.string.note_content)
        composeTestRule.onNodeWithContentDescription(noteContentLabel).performTextReplacement(testContent)

        // 3. 자동 저장을 기다린 후 뒤로가기.
        // 저장은 1초 디바운스다. 여기서 mainClock 을 앞으로 돌리는 것은 의미가 없다 —
        // 액티비티를 이 룰이 띄우지 않았으므로(createEmptyComposeRule + ActivityScenario)
        // 앞으로 돌려지는 클록이 이 컴포지션을 구동하는 클록이라는 보장이 없다.
        // 그래서 저장이 실제로 DB 에 닿았는지를 조건으로 기다린다(#235).
        composeTestRule.waitUntil(timeoutMillis = 10_000L) {
            runBlocking { database.noteDao().getNoteByTitle("Integration Test") } != null
        }
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
