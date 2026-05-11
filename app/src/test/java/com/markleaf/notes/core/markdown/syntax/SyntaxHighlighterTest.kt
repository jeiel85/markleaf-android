package com.markleaf.notes.core.markdown.syntax

import com.markleaf.notes.core.markdown.syntax.SyntaxHighlighter.TokenType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyntaxHighlighterTest {

    private fun typesIn(code: String, lang: String?): List<TokenType> =
        SyntaxHighlighter.tokenize(code, lang).map { it.type }

    private fun joinedText(code: String, lang: String?): String =
        SyntaxHighlighter.tokenize(code, lang).joinToString("") { it.text }

    @Test
    fun unknownLanguage_returnsSingleTextToken() {
        val result = SyntaxHighlighter.tokenize("anything goes", "klingon")
        assertEquals(1, result.size)
        assertEquals(TokenType.TEXT, result.first().type)
        assertEquals("anything goes", result.first().text)
    }

    @Test
    fun nullLanguage_returnsSingleTextToken() {
        val result = SyntaxHighlighter.tokenize("plain code", null)
        assertEquals(listOf(TokenType.TEXT), result.map { it.type })
    }

    @Test
    fun roundTrip_alwaysPreservesOriginalText() {
        // Every tokenization must concatenate back to the original input
        // (otherwise the preview would silently drop user content).
        val samples = listOf(
            "kotlin" to "fun main() { println(\"hi\") }",
            "python" to "# hi\ndef foo(x): return x * 2",
            "json" to """{"a": 1, "b": true, "c": null}""",
            "sql" to "SELECT * FROM notes WHERE id = '1';"
        )
        for ((lang, code) in samples) {
            assertEquals("round trip $lang", code, joinedText(code, lang))
        }
    }

    @Test
    fun kotlin_recognizesFunAndStringAndComment() {
        val code = "// hello\nfun add(a: Int): Int = a + 1"
        val types = typesIn(code, "kotlin").toSet()
        assertTrue("expected COMMENT token", TokenType.COMMENT in types)
        assertTrue("expected KEYWORD token (fun/Int)", TokenType.KEYWORD in types)
    }

    @Test
    fun kotlin_doesNotColorKeywordInsideString() {
        // The literal `"fun"` inside a string must NOT come back as KEYWORD —
        // strings win the overlap resolution.
        val tokens = SyntaxHighlighter.tokenize(""" val s = "fun day" """, "kotlin")
        val funToken = tokens.find { it.text.contains("fun") }
        assertEquals(TokenType.STRING, funToken?.type)
    }

    @Test
    fun python_hashIsCommentNotKeyword() {
        val types = typesIn("# comment\nprint(1)", "python")
        assertTrue(TokenType.COMMENT in types)
        assertTrue(TokenType.FUNCTION in types)
    }

    @Test
    fun python_decoratorIsHighlighted() {
        val tokens = SyntaxHighlighter.tokenize("@dataclass\nclass Foo: pass", "python")
        val decorator = tokens.find { it.text.startsWith("@") }
        assertEquals(TokenType.TYPE, decorator?.type)
    }

    @Test
    fun json_recognizesAllPrimitives() {
        val types = typesIn("""{"a": 1, "b": true, "c": null}""", "json").toSet()
        assertTrue(TokenType.STRING in types)
        assertTrue(TokenType.NUMBER in types)
        assertTrue(TokenType.KEYWORD in types)
    }

    @Test
    fun yaml_keyBeforeColonIsFunctionToken() {
        val tokens = SyntaxHighlighter.tokenize("title: Hello\ntags: [draft]", "yaml")
        // First non-text token should be the key "title".
        val firstKey = tokens.firstOrNull { it.text == "title" }
        assertEquals(TokenType.FUNCTION, firstKey?.type)
    }

    @Test
    fun xml_tagsAndAttributesHighlighted() {
        val tokens = SyntaxHighlighter.tokenize("""<a href="x">b</a>""", "xml")
        assertTrue(tokens.any { it.text.startsWith("<a") && it.type == TokenType.KEYWORD })
        assertTrue(tokens.any { it.text == "href" && it.type == TokenType.FUNCTION })
        assertTrue(tokens.any { it.text == "\"x\"" && it.type == TokenType.STRING })
    }

    @Test
    fun sql_uppercaseKeywordsHighlighted() {
        val types = typesIn("SELECT * FROM notes WHERE id = 1", "sql")
        assertTrue(TokenType.KEYWORD in types)
        assertTrue(TokenType.NUMBER in types)
    }

    @Test
    fun shell_dollarVariableIsType() {
        // Bash variable references outside of quotes get TYPE coloring.
        // (Inside quotes, the STRING rule wins — that's the expected design.)
        val code = "echo ${'$'}HOME && cd ${'$'}{PATH}"
        val tokens = SyntaxHighlighter.tokenize(code, "bash")
        val dollarTokens = tokens.filter { it.text.startsWith("$") }
        assertTrue(
            "expected at least one TYPE token for shell variable reference",
            dollarTokens.any { it.type == TokenType.TYPE }
        )
    }
}
