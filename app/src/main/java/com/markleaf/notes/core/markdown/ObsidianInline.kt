package com.markleaf.notes.core.markdown

import org.commonmark.node.AbstractVisitor
import org.commonmark.node.CustomNode
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.Text
import org.commonmark.parser.Parser
import org.commonmark.parser.PostProcessor

/**
 * `==highlighted text==` (#608, the Obsidian form). Its one child is the
 * highlighted [Text], so anything that reads a block as plain text — headings,
 * table cells, export — still sees the words.
 */
internal class HighlightNode : CustomNode()

/**
 * Two pieces of Obsidian syntax the preview shows the way Obsidian does (#608):
 *
 * - `==text==` becomes a [HighlightNode].
 * - `%%comment%%` is hidden. Inside a line it is cut out of the text; a
 *   paragraph that is nothing but one comment, even over several lines, is
 *   dropped. A comment with a blank line inside it is not recognised — the
 *   blank line has already split it into two paragraphs, and guessing across
 *   them could hide text the author meant to show.
 *
 * Both are matched within one text node, so they do not reach across other
 * inline syntax (`==**bold**==` stays as typed). Code spans and code blocks are
 * never touched: their text is not a [Text] node.
 *
 * Listed after [WikilinkExtension] and before autolinks, for the same reason
 * that one is listed early: an address inside a highlight is part of it.
 */
internal object ObsidianInlineExtension : Parser.ParserExtension {
    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.postProcessor(ObsidianInlinePostProcessor)
    }
}

private object ObsidianInlinePostProcessor : PostProcessor {
    override fun process(node: Node): Node {
        val commentParagraphs = mutableListOf<Paragraph>()
        node.accept(object : AbstractVisitor() {
            override fun visit(paragraph: Paragraph) {
                if (isWholeComment(paragraph)) commentParagraphs += paragraph else visitChildren(paragraph)
            }
        })
        commentParagraphs.forEach(Node::unlink)

        val texts = mutableListOf<Text>()
        node.accept(object : AbstractVisitor() {
            override fun visit(text: Text) {
                val literal = text.literal
                if (literal.contains("%%") || literal.contains("==")) texts += text
            }
        })
        texts.forEach(::rewrite)
        return node
    }

    /** True when the paragraph's text, read across its line breaks, is one `%%…%%`. */
    private fun isWholeComment(paragraph: Paragraph): Boolean {
        val sb = StringBuilder()
        var child = paragraph.firstChild
        while (child != null) {
            when (child) {
                is Text -> sb.append(child.literal)
                is SoftLineBreak, is HardLineBreak -> sb.append('\n')
                else -> return false
            }
            child = child.next
        }
        val text = sb.trim()
        return text.length >= 4 && text.startsWith("%%") && text.endsWith("%%") &&
            text.indexOf("%%", 2) == text.length - 2
    }

    private fun rewrite(text: Text) {
        val withoutComments = COMMENT_REGEX.replace(text.literal, "")
        val matches = HIGHLIGHT_REGEX.findAll(withoutComments).toList()
        if (matches.isEmpty()) {
            if (withoutComments.isEmpty()) text.unlink() else text.literal = withoutComments
            return
        }
        var cursor = 0
        for (match in matches) {
            if (match.range.first > cursor) text.insertBefore(Text(withoutComments.substring(cursor, match.range.first)))
            text.insertBefore(HighlightNode().apply { appendChild(Text(match.groupValues[1])) })
            cursor = match.range.last + 1
        }
        if (cursor < withoutComments.length) text.insertBefore(Text(withoutComments.substring(cursor)))
        text.unlink()
    }

    private val COMMENT_REGEX = Regex("""%%.*?%%""")

    /** Not empty, and not starting or ending with a space, like `**…**`. */
    private val HIGHLIGHT_REGEX = Regex("""==(?=[^\s=])([^\n]*?[^\s=])==""")
}
