package com.markleaf.notes.core.markdown

import com.markleaf.notes.util.WikilinkExtractor
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.CustomNode
import org.commonmark.node.Node
import org.commonmark.node.Text
import org.commonmark.parser.Parser
import org.commonmark.parser.PostProcessor

/**
 * A `[[Target]]` / `[[Target|Label]]` wikilink in the parsed tree. [body] is
 * what sits between the brackets.
 *
 * It has no children on purpose: nothing that walks text nodes afterwards —
 * the autolink extension above all — can reach inside it.
 */
internal class WikilinkNode(val body: String) : CustomNode() {
    val target: String get() = WikilinkExtractor.target(body)
    val label: String get() = WikilinkExtractor.label(body)

    /** The source form, for the flat text a heading or table cell is read as. */
    val source: String get() = "[[$body]]"
}

/**
 * Turns wikilinks into [WikilinkNode]s before anything else post-processes
 * the tree.
 *
 * They used to be found later, by splitting each text node while rendering.
 * With GFM autolinks that was too late: the autolink extension had already cut
 * the address out of `[[www.example.com notes]]` or `[[Contacts|me@example.com]]`,
 * leaving `[[`, a link and `]]` as three nodes in which no whole wikilink is
 * left to find (review of #492). List this extension ahead of
 * `AutolinkExtension` — post-processors run in the order they are added.
 */
internal object WikilinkExtension : Parser.ParserExtension {
    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.postProcessor(WikilinkPostProcessor)
    }
}

private object WikilinkPostProcessor : PostProcessor {
    override fun process(node: Node): Node {
        val texts = mutableListOf<Text>()
        node.accept(object : AbstractVisitor() {
            override fun visit(text: Text) {
                if (WikilinkExtractor.hasAny(text.literal)) texts += text
            }
        })
        texts.forEach(::split)
        return node
    }

    private fun split(text: Text) {
        val literal = text.literal
        var cursor = 0
        for (match in WikilinkExtractor.WIKILINK_REGEX.findAll(literal)) {
            if (match.range.first > cursor) text.insertBefore(Text(literal.substring(cursor, match.range.first)))
            text.insertBefore(WikilinkNode(match.groupValues[1]))
            cursor = match.range.last + 1
        }
        if (cursor < literal.length) text.insertBefore(Text(literal.substring(cursor)))
        text.unlink()
    }
}
