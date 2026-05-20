package com.markleaf.notes.core.markdown

import com.markleaf.notes.core.markdown.syntax.SyntaxHighlighter
import org.junit.Test

/**
 * Repro net for the crash flagged by @dking08 on fdroiddata MR !38659:
 * "When notes include a codeblock (like ```kotlin```) the app crashes."
 *
 * Each test exercises a different fenced-code shape that a real user is
 * plausible to type. If a path throws, the test name pins down which
 * shape is the offender.
 */
class FencedCodeBlockEdgeCasesTest {

    private fun parseAndTokenize(markdown: String) {
        val lines = SimpleMarkdownPreview.parse(markdown)
        // Replay what MarkdownPreviewList does: for each CODE_BLOCK line,
        // run the preview-time syntax highlighter with the language hint.
        lines.filter { it.type == PreviewLineType.CODE_BLOCK }.forEach { line ->
            SyntaxHighlighter.tokenize(line.text, line.extra)
        }
    }

    @Test
    fun emptyCodeBlock_withLanguage() {
        // exactly the shape dking08 quoted: ```kotlin\n```
        parseAndTokenize("```kotlin\n```")
    }

    @Test
    fun emptyCodeBlock_withTrailingSpaceLang() {
        parseAndTokenize("```kotlin \n```")
    }

    @Test
    fun emptyCodeBlock_noLanguage() {
        parseAndTokenize("```\n```")
    }

    @Test
    fun unclosedFence_withLanguage() {
        // forgot the closing fence
        parseAndTokenize("```kotlin\nfun main() { println(\"hi\") }\n")
    }

    @Test
    fun unclosedFence_atEndOfFile_noNewline() {
        parseAndTokenize("```kotlin")
    }

    @Test
    fun codeBlock_withUnknownLanguage() {
        parseAndTokenize("```ocaml\nlet x = 1\n```")
    }

    @Test
    fun codeBlock_blankBodyWhitespaceOnly() {
        parseAndTokenize("```kotlin\n   \n\n```")
    }

    @Test
    fun multipleConsecutiveCodeBlocks() {
        parseAndTokenize("```kotlin\nfun a() {}\n```\n\n```python\ndef b(): pass\n```")
    }

    @Test
    fun codeBlock_withBackticksInside_languageOnly() {
        // user types ```kotlin``` on a single line
        parseAndTokenize("```kotlin```")
    }

    @Test
    fun codeBlock_followedByHeading() {
        parseAndTokenize("```kotlin\nfun a() {}\n```\n# heading after")
    }
}
