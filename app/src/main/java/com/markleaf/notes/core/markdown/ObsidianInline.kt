package com.markleaf.notes.core.markdown

import org.commonmark.node.AbstractVisitor
import org.commonmark.node.CustomNode
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Node
import org.commonmark.node.Nodes
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.Text
import org.commonmark.parser.Parser
import org.commonmark.parser.PostProcessor
import org.commonmark.parser.beta.InlineContentParser
import org.commonmark.parser.beta.InlineContentParserFactory
import org.commonmark.parser.beta.InlineParserState
import org.commonmark.parser.beta.ParsedInline
import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.parser.delimiter.DelimiterRun
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.html.HtmlNodeRendererContext
import org.commonmark.renderer.html.HtmlRenderer

/** `==highlighted text==` (#608, the Obsidian form). Its children are the highlighted inlines. */
internal class HighlightNode : CustomNode()

/** A `%%comment%%` (#608). Removed again before the tree leaves the parser. */
private class CommentNode : CustomNode()

/**
 * Two pieces of Obsidian syntax, read the way Obsidian reads them (#608):
 *
 * - `==text==` becomes a [HighlightNode], through a delimiter processor — the
 *   same machinery as `~~strikethrough~~` — so `\=\=` stays literal, emphasis
 *   inside it still works, and unmatched `==` costs nothing extra.
 * - `%%comment%%` is hidden, inside a line or as a whole paragraph over
 *   several lines. It is parsed as inline content, after backslash escapes, so
 *   `\%\%` stays literal text. A paragraph left with nothing but the comment
 *   is dropped. A comment with a blank line inside is two paragraphs by then
 *   and is not recognised.
 *
 * Code spans and code blocks are untouched: neither is inline content.
 *
 * Also an [HtmlRenderer.HtmlRendererExtension], so the PDF export — which
 * renders HTML from its own parser — draws highlights as `<mark>` and, sharing
 * this parser extension, leaves comments out as the preview does.
 */
internal object ObsidianInlineExtension : Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {
    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.customDelimiterProcessor(HighlightDelimiterProcessor)
        parserBuilder.customInlineContentParserFactory(CommentParserFactory)
        parserBuilder.postProcessor(CommentRemover)
    }

    override fun extend(rendererBuilder: HtmlRenderer.Builder) {
        rendererBuilder.nodeRendererFactory { context -> HighlightHtmlRenderer(context) }
    }
}

private object HighlightDelimiterProcessor : DelimiterProcessor {
    override fun getOpeningCharacter(): Char = '='
    override fun getClosingCharacter(): Char = '='
    override fun getMinLength(): Int = 2

    override fun process(openingRun: DelimiterRun, closingRun: DelimiterRun): Int {
        // Exactly `==` on both sides, as `~~` is for strikethrough: `===` is
        // more likely an arrow or a rule of text than a highlight.
        if (openingRun.length() != 2 || closingRun.length() != 2) return 0
        val opener = openingRun.opener
        val highlight = HighlightNode()
        for (node in Nodes.between(opener, closingRun.closer)) highlight.appendChild(node)
        opener.insertAfter(highlight)
        return 2
    }
}

private object CommentParserFactory : InlineContentParserFactory {
    override fun getTriggerCharacters(): Set<Char> = setOf('%')
    override fun create(): InlineContentParser = CommentParser
}

private object CommentParser : InlineContentParser {
    override fun tryParse(inlineParserState: InlineParserState): ParsedInline? {
        val scanner = inlineParserState.scanner()
        if (!scanner.next("%%")) return ParsedInline.none()
        // To the next `%%`, across the paragraph's lines. Each `%%` closes the
        // one before it, so only a final unmatched opener scans to the end.
        while (scanner.find('%') != -1) {
            if (scanner.next("%%")) return ParsedInline.of(CommentNode(), scanner.position())
            scanner.next()
        }
        return ParsedInline.none()
    }
}

private object CommentRemover : PostProcessor {
    override fun process(node: Node): Node {
        val comments = mutableListOf<CommentNode>()
        node.accept(object : AbstractVisitor() {
            override fun visit(customNode: CustomNode) {
                if (customNode is CommentNode) comments += customNode else visitChildren(customNode)
            }
        })
        // Each paragraph is checked once, after all its comments are gone:
        // checking after every removal rescanned the blank text each one left
        // behind, quadratic for a paragraph of many comments (Codex on #612).
        val touched = LinkedHashSet<Paragraph>()
        for (comment in comments) {
            (comment.parent as? Paragraph)?.let(touched::add)
            comment.unlink()
        }
        for (paragraph in touched) if (paragraph.isBlank()) paragraph.unlink()
        return node
    }

    private fun Paragraph.isBlank(): Boolean {
        var child = firstChild
        while (child != null) {
            when (child) {
                is Text -> if (child.literal.isNotBlank()) return false
                is SoftLineBreak, is HardLineBreak -> Unit
                else -> return false
            }
            child = child.next
        }
        return true
    }
}

private class HighlightHtmlRenderer(private val context: HtmlNodeRendererContext) : NodeRenderer {
    override fun getNodeTypes(): Set<Class<out Node>> = setOf(HighlightNode::class.java)

    override fun render(node: Node) {
        val html = context.writer
        html.tag("mark")
        var child = node.firstChild
        while (child != null) {
            val next = child.next
            context.render(child)
            child = next
        }
        html.tag("/mark")
    }
}
