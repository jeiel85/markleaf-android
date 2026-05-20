package com.markleaf.notes.core.markdown.syntax

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression net for the v2.15.2 codeblock-preview crash reported by
 * @dking08 on fdroiddata MR !38659.
 *
 * The bug: a regex in [SyntaxHighlighter]'s SHELL_RULES used a bare `}` to
 * close a `\$\{...}` group. JVM's `java.util.regex` (used by Robolectric
 * and JUnit on the host) accepts that, so every unit and Roborazzi test
 * passed. Android's ICU regex engine rejects it with
 * `PatternSyntaxException`, which threw during [SyntaxHighlighter]'s
 * static initializer — and because static-init failure is permanent, the
 * very first attempt to render any fenced code block in preview crashed
 * the editor with `ExceptionInInitializerError`.
 *
 * This instrumented test runs on a real Android runtime so the ICU regex
 * path is exercised. If a future change to the rule list ships a regex
 * that JVM accepts but ICU rejects, this test catches it before users do.
 */
@RunWith(AndroidJUnit4::class)
class SyntaxHighlighterAndroidTest {

    @Test
    fun staticInit_andEveryLanguageTokenizes_onIcuRegex() {
        // Exercise every language so any rule with a JVM/ICU regex mismatch
        // surfaces — not just the SHELL_RULES one that originally regressed.
        val samples = mapOf(
            "kotlin" to "fun main() { println(\"hi\") }",
            "java" to "class A { void b() {} }",
            "python" to "def foo(x):\n    return x * 2",
            "javascript" to "const a = `tpl ${'$'}{x}`;",
            "typescript" to "interface A { x: number }",
            "bash" to "echo \$HOME \${PATH}",
            "sh" to "for x in 1 2; do echo \$x; done",
            "json" to """{"a": 1, "b": true, "c": null}""",
            "yaml" to "title: Hello\ntags: [draft]",
            "xml" to """<a href="x">b</a>""",
            "html" to """<div class="x">y</div>""",
            "sql" to "SELECT * FROM notes WHERE id = 1",
            null to "no language hint"
        )
        for ((lang, code) in samples) {
            // tokenize() must not throw. The result's text concatenation must
            // also round-trip to the original input — otherwise the preview
            // would silently drop user content.
            val tokens = SyntaxHighlighter.tokenize(code, lang)
            val roundTrip = tokens.joinToString("") { it.text }
            check(roundTrip == code) {
                "round-trip failed for lang=$lang: '$code' -> '$roundTrip'"
            }
        }
    }
}
