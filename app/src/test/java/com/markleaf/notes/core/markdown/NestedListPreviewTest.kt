package com.markleaf.notes.core.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression net for #339.
 *
 * The adapter used to walk only the *direct* `ListItem` children of a list and
 * build each row from the whole item subtree, so everything nested underneath
 * an item was concatenated into the parent's row with no separator at all:
 * `- a` with a nested `- b` came out as the single row `• ab`, and a nested
 * `- [ ] task` had no row of its own, therefore no checkbox, no visible state
 * and nothing to tap.
 *
 * Every test here asserts the same property from a different angle: one
 * [PreviewLine] per source item, carrying its own text, marker and depth.
 */
class NestedListPreviewTest {

    @Test
    fun `nested bullets each get their own row`() {
        val lines = SimpleMarkdownPreview.parse(
            """
            - top item
              - nested item
                - deeper item
            - second top
            """.trimIndent()
        )

        assertEquals(4, lines.size)
        assertEquals(
            listOf("top item", "nested item", "deeper item", "second top"),
            lines.map { it.text }
        )
        assertEquals(listOf(0, 1, 2, 0), lines.map { it.depth })
        lines.forEach { assertEquals(PreviewLineType.BULLET, it.type) }
    }

    @Test
    fun `nested ordered items keep their own numbering`() {
        val lines = SimpleMarkdownPreview.parse(
            """
            1. one
               1. one-a
               2. one-b
            2. two
            """.trimIndent()
        )

        assertEquals(4, lines.size)
        assertEquals(listOf("one", "one-a", "one-b", "two"), lines.map { it.text })
        assertEquals(listOf(0, 1, 1, 0), lines.map { it.depth })
        // Each list numbers itself: the sublist restarts at 1 rather than
        // continuing the outer sequence.
        assertEquals(listOf("1", "1", "2", "2"), lines.map { it.extra })
    }

    @Test
    fun `nested task items keep their own checkbox and state`() {
        val lines = SimpleMarkdownPreview.parse(
            """
            - [ ] parent
              - [x] child done
              - [ ] child todo
            """.trimIndent()
        )

        assertEquals(3, lines.size)
        assertEquals(
            listOf(
                PreviewLineType.CHECKBOX_TODO,
                PreviewLineType.CHECKBOX_DONE,
                PreviewLineType.CHECKBOX_TODO
            ),
            lines.map { it.type }
        )
        assertEquals(listOf("parent", "child done", "child todo"), lines.map { it.text })
        assertEquals(listOf(0, 1, 1), lines.map { it.depth })
    }

    @Test
    fun `tapping a nested task toggles the line it came from`() {
        val markdown = """
            - [ ] parent
              - [x] child done
              - [ ] child todo
        """.trimIndent()

        val lines = SimpleMarkdownPreview.parse(markdown)
        val childTodo = lines[2]
        assertNotNull("a nested task row must know its source line", childTodo.sourceLine)

        // The #219 guarantee has to hold at depth 1 too: exactly one character
        // changes, and it is the one the user pointed at.
        val toggled = MarkdownEditActions.toggleTaskAtLine(markdown, childTodo.sourceLine!!)
        assertEquals(
            """
            - [ ] parent
              - [x] child done
              - [x] child todo
            """.trimIndent(),
            toggled
        )
    }

    @Test
    fun `a continuation paragraph becomes its own row`() {
        val lines = SimpleMarkdownPreview.parse(
            """
            - first item

              continued paragraph of the same item

            - second item
            """.trimIndent()
        )

        assertEquals(3, lines.size)
        assertEquals(PreviewLineType.BULLET, lines[0].type)
        assertEquals("first item", lines[0].text)
        assertEquals(PreviewLineType.BODY, lines[1].type)
        assertEquals("continued paragraph of the same item", lines[1].text)
        assertEquals(1, lines[1].depth)
        assertEquals(PreviewLineType.BULLET, lines[2].type)
        assertEquals("second item", lines[2].text)
    }

    @Test
    fun `a quote inside a list item becomes its own row`() {
        val lines = SimpleMarkdownPreview.parse(
            """
            - item with a quote
              > quoted inside
            """.trimIndent()
        )

        assertEquals(2, lines.size)
        assertEquals(PreviewLineType.BULLET, lines[0].type)
        assertEquals("item with a quote", lines[0].text)
        assertEquals(PreviewLineType.BLOCKQUOTE, lines[1].type)
        assertEquals("quoted inside", lines[1].text)
        assertEquals(1, lines[1].depth)
    }

    @Test
    fun `an item that opens straight into a sublist still renders both`() {
        val lines = SimpleMarkdownPreview.parse(
            """
            -
              - child of an empty parent
            """.trimIndent()
        )

        assertEquals(2, lines.size)
        assertEquals("", lines[0].text)
        assertEquals(0, lines[0].depth)
        assertEquals("child of an empty parent", lines[1].text)
        assertEquals(1, lines[1].depth)
    }

    @Test
    fun `a flat list is unchanged`() {
        val lines = SimpleMarkdownPreview.parse(
            """
            - bullet
            - [ ] todo
            - [x] done
            """.trimIndent()
        )

        assertEquals(3, lines.size)
        assertEquals(listOf(0, 0, 0), lines.map { it.depth })
        assertEquals(listOf("bullet", "todo", "done"), lines.map { it.text })
        // A plain bullet still cannot point back at the source — only task rows
        // need that, and only they can be tapped.
        assertNull(lines[0].sourceLine)
        assertNotNull(lines[1].sourceLine)
    }

    @Test
    fun `a list with blank lines between its items is marked loose`() {
        val loose = SimpleMarkdownPreview.parse(
            """
            - one

            - two
            """.trimIndent()
        )
        val tight = SimpleMarkdownPreview.parse(
            """
            - one
            - two
            """.trimIndent()
        )

        // CommonMark reads the blank line as "space this out on purpose" and
        // keeps both items in one list. The rows have to carry that apart from
        // each other, or the blank line disappears in the rendering.
        assertEquals(listOf(true, true), loose.map { it.looseList })
        assertEquals(listOf(false, false), tight.map { it.looseList })
        assertEquals(loose.map { it.text }, tight.map { it.text })
    }

    @Test
    fun `looseness follows the list a row belongs to`() {
        val lines = SimpleMarkdownPreview.parse(
            """
            - tight one
            - tight two

              - nested after a blank line

              - nested two
            """.trimIndent()
        )

        // The outer list turned loose the moment one of its items held a blank
        // line, and the inner list is loose in its own right.
        assertEquals(listOf(0, 0, 1, 1), lines.map { it.depth })
        lines.forEach { assertEquals(true, it.looseList) }
    }

    @Test
    fun `a table or rule inside a list item is indented with it`() {
        val lines = SimpleMarkdownPreview.parse(
            """
            - item with a table

              | a | b |
              | --- | --- |
              | 1 | 2 |

            - item with a rule

              ---
            """.trimIndent()
        )

        // Every block a list item can hold has to inherit the item's depth, or
        // it renders flush against the left margin while the rows around it sit
        // indented.
        val byType = lines.associateBy { it.type }
        assertEquals(1, byType[PreviewLineType.TABLE]?.depth)
        assertEquals(1, byType[PreviewLineType.HORIZONTAL_RULE]?.depth)
    }

    @Test
    fun `body rows outside a list stay at depth zero`() {
        val lines = SimpleMarkdownPreview.parse(
            """
            # Heading

            A paragraph.

            - a list item
            """.trimIndent()
        )

        assertEquals(listOf(0, 0, 0), lines.map { it.depth })
    }

    /**
     * The renderer recurses once per nesting level, and a note can nest
     * without limit. Before [CommonMarkPreviewAdapter.MAX_BLOCK_DEPTH] the
     * only ceiling was the stack, so a deep enough list took the app down when
     * the note was opened — not a crafted-input worry alone, since a generated
     * outline or a file from another tool can carry nesting no person types.
     */
    @Test
    fun `nesting deeper than the ceiling is cut, not crashed`() {
        val markdown = nestedList(CommonMarkPreviewAdapter.MAX_BLOCK_DEPTH * 3)

        val lines = SimpleMarkdownPreview.parse(markdown)

        // Every level the renderer descends into, plus the one marker row that
        // stands in for everything below it.
        assertEquals(CommonMarkPreviewAdapter.MAX_BLOCK_DEPTH + 2, lines.size)
        assertEquals(CommonMarkPreviewAdapter.DEPTH_CUT_MARKER, lines.last().text)
        assertEquals(CommonMarkPreviewAdapter.MAX_BLOCK_DEPTH, lines.last().depth)
    }

    /** The ceiling must not touch the nesting a person actually writes. */
    @Test
    fun `nesting within the ceiling keeps every row`() {
        val markdown = nestedList(CommonMarkPreviewAdapter.MAX_BLOCK_DEPTH + 1)

        val lines = SimpleMarkdownPreview.parse(markdown)

        assertEquals(CommonMarkPreviewAdapter.MAX_BLOCK_DEPTH + 1, lines.size)
        assertEquals("item 0", lines.first().text)
        assertEquals("item ${CommonMarkPreviewAdapter.MAX_BLOCK_DEPTH}", lines.last().text)
    }

    /**
     * Past a few thousand levels commonmark's own visitors fault before the
     * renderer is reached, which no ceiling in this codebase can prevent — so
     * the property asserted is the one that matters to a person holding the
     * phone: the preview comes back with the note's text in it.
     *
     * Which representation it comes back as is deliberately not asserted. The
     * depth where the parser gives out is a property of the runtime's stack,
     * so a device with more room may still render structure here; both answers
     * are correct, and only crashing is not. 3,000 is past the measured fault
     * on the JVM test runner and short of the depth where the indentation
     * alone — quadratic in the level count — exhausts its heap.
     */
    @Test
    fun `nesting past the parser's own limit still renders the text`() {
        val lines = SimpleMarkdownPreview.parse(nestedList(3_000))

        assertTrue(lines.isNotEmpty())
        assertEquals("item 0", lines.first().text.trim().removePrefix("- "))
    }

    /** A list nested [levels] deep, one item per level. */
    private fun nestedList(levels: Int): String = buildString {
        repeat(levels) { level ->
            append(" ".repeat(level * 2)).append("- item ").append(level).append('\n')
        }
    }
}
