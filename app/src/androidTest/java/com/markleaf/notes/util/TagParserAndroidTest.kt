package com.markleaf.notes.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented coverage for [TagParser] on a real Android runtime.
 *
 * [TagParser] leans on the Unicode category classes `\p{L}` and `\p{N}` so that
 * tags in any script (Korean, Japanese, Chinese, German umlauts, …) are
 * recognised. Those classes are honoured by the host JVM's `java.util.regex`,
 * but Android's regex engine is ICU-backed — the same JVM/ICU divergence that
 * crashed the code-block highlighter in v2.15.2 (see [com.markleaf.notes.core
 * .markdown.syntax.SyntaxHighlighterAndroidTest]). This test runs the parser on
 * the device so a future Unicode-class regression is caught on the path users
 * actually hit, not only on the host.
 */
@RunWith(AndroidJUnit4::class)
class TagParserAndroidTest {

    @Test
    fun parsesTagsInsideBulletedLists_onIcuRegex() {
        // The case reported in issue #137: tags inside bulleted list items.
        val content = "- Buy milk #shopping\n- #todo\n- urgent #work, #personal"
        assertEquals(
            listOf("shopping", "todo", "work", "personal"),
            TagParser.parseTags(content)
        )
    }

    @Test
    fun parsesNonLatinTags_onIcuRegex() {
        val korean = TagParser.parseTags("- 메모 #업무 #중요")
        assertEquals(listOf("업무", "중요"), korean)

        val japanese = TagParser.parseTags("- メモ #仕事 #重要")
        assertEquals(listOf("仕事", "重要"), japanese)

        val chinese = TagParser.parseTags("- 笔记 #工作")
        assertEquals(listOf("工作"), chinese)

        val german = TagParser.parseTags("- Termin #Größe und #Tür")
        assertEquals(listOf("Größe", "Tür"), german)
    }

    @Test
    fun trailingPunctuationAndUrlFragmentsHandled_onIcuRegex() {
        val tags = TagParser.parseTags("- See https://example.com#section #shopping.")
        assertEquals(listOf("shopping"), tags)
        assertTrue("URL fragment must not be a tag", !tags.contains("section"))
    }
}
