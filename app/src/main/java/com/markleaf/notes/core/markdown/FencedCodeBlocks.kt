package com.markleaf.notes.core.markdown

/**
 * Where a note's fenced code blocks are, shared by the editor's highlighter and
 * by title extraction so both agree on what is code. Pure text in, ranges out:
 * no Compose, so `core.text` can use it without pulling in the highlighter.
 */
internal object FencedCodeBlocks {

    /**
     * Character ranges of fenced code blocks, by CommonMark's rules for the
     * part that matters here: a fence is three or more backticks or tildes,
     * indented at most three spaces, closed by a fence of the same character
     * at least as long, and a fence nobody closes runs to the end of the note
     * — as the preview reads it. The old single regex knew only ``` and needed
     * a closing fence, so `~~~` blocks were never styled.
     */
    fun ranges(text: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var openAt = -1
        var fenceChar = ' '
        var fenceLength = 0
        var lineStart = 0
        while (lineStart <= text.length) {
            val newline = text.indexOf('\n', lineStart)
            val lineEnd = if (newline < 0) text.length else newline
            val fence = FENCE_LINE_REGEX.matchEntire(text.substring(lineStart, lineEnd))
            if (fence != null) {
                val run = fence.groupValues[1]
                val info = fence.groupValues[2]
                if (openAt < 0) {
                    // A backtick fence's info string may not contain a backtick.
                    if (run[0] == '~' || '`' !in info) {
                        openAt = lineStart
                        fenceChar = run[0]
                        fenceLength = run.length
                    }
                } else if (run[0] == fenceChar && run.length >= fenceLength && info.isBlank()) {
                    ranges += openAt until lineEnd
                    openAt = -1
                }
            }
            if (newline < 0) break
            lineStart = newline + 1
        }
        if (openAt >= 0 && openAt < text.length) ranges += openAt until text.length
        return ranges
    }

    private val FENCE_LINE_REGEX = Regex("""^ {0,3}(`{3,}|~{3,})(.*)$""")
}
