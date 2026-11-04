package com.markleaf.notes.util

import com.markleaf.notes.core.markdown.FencedCodeBlocks
import java.util.regex.Pattern

object TagParser {
    // Match #tag where the tag is preceded by whitespace or the start of the
    // content (so URL fragments like `…com#frag` are never treated as tags) and
    // the tag body is made up only of valid tag characters. Restricting the body
    // to the valid character set — rather than "everything up to whitespace" —
    // means trailing punctuation (`#work,` `#shopping.`) and list separators are
    // left out instead of poisoning the whole tag. `\p{L}`/`\p{N}` are Unicode
    // categories, so tags in any script (Korean, Japanese, Chinese, German
    // umlauts, …) are recognised, not just Latin + Hangul.
    private val TAG_PATTERN = Pattern.compile("""(^|\s)#(\p{L}[\p{L}\p{N}_/-]*|_[\p{L}\p{N}_/-]*)""")

    fun parseTags(content: String): List<String> {
        val tags = LinkedHashSet<String>()
        val code = codeMask(content)

        val matcher = TAG_PATTERN.matcher(content)
        while (matcher.find()) {
            val tag = matcher.group(2) ?: continue
            // The `#` sits just before the tag body.
            if (code[matcher.start(2) - 1]) continue
            if (isValidTagName(tag)) {
                tags.add(tag)
            }
        }

        return tags.toList()
    }

    /**
     * Which characters are code: fenced blocks, then inline spans outside them,
     * so `#include` in a C snippet or a `` `#fff` `` colour is not a tag (#262).
     * A mask rather than stripping the code out, so the text around a span keeps
     * the boundaries it had: stripping `` `x` `` from ``a `x`#b`` would leave
     * `a #b`, a tag that was never there.
     *
     * Fences are found as the editor's highlighter finds them. Inline spans
     * follow the preview (CommonMark) where it matters for tags: a run of N
     * backticks opens a span that only a run of exactly N closes, so
     * ``` ``a`b`` #work `c` ``` is two spans with a tag between them; a run
     * with no partner is text; and a backslash-escaped backtick is text, so
     * `` \` #work \` `` keeps its tag (#571 review). One difference is kept
     * from the highlighter: a span ends at the end of its line.
     */
    private fun codeMask(content: String): BooleanArray {
        val code = BooleanArray(content.length)
        if ('`' !in content && '~' !in content) return code
        for (fence in FencedCodeBlocks.ranges(content)) {
            java.util.Arrays.fill(code, fence.first, fence.last + 1, true)
        }
        fun runEnd(from: Int): Int {
            var end = from
            while (end < content.length && content[end] == '`' && !code[end]) end++
            return end
        }
        var i = 0
        while (i < content.length) {
            when {
                code[i] -> i++
                // An escaped character is text, a backtick included. Inside a
                // span a backslash is literal, which the search below respects.
                content[i] == '\\' -> i += 2
                content[i] == '`' -> {
                    val openEnd = runEnd(i)
                    val length = openEnd - i
                    val lineEnd = content.indexOf('\n', openEnd).let { if (it < 0) content.length else it }
                    var close = -1
                    var k = openEnd
                    while (k < lineEnd) {
                        if (content[k] == '`' && !code[k]) {
                            val end = runEnd(k)
                            if (end - k == length) {
                                close = end
                                break
                            }
                            k = end
                        } else {
                            k++
                        }
                    }
                    if (close >= 0) {
                        java.util.Arrays.fill(code, i, close, true)
                        i = close
                    } else {
                        i = openEnd
                    }
                }
                else -> i++
            }
        }
        return code
    }

    private val SEGMENT_REGEX = Regex("""[\p{L}_][\p{L}\p{N}_-]*""")

    private fun isValidTagName(tag: String): Boolean {
        if (tag.isEmpty()) return false
        // A hierarchical tag is one or more `/`-separated segments. Each segment
        // must start with a letter (any script) or underscore and may contain
        // letters, digits, underscores, or hyphens. Empty segments are rejected
        // so things like `#parent/` or `#a//b` do not slip through.
        val segments = tag.split('/')
        if (segments.isEmpty()) return false
        return segments.all { it.matches(SEGMENT_REGEX) }
    }

    fun normalizeTagName(tag: String): String {
        return tag.trim().lowercase()
    }
}
