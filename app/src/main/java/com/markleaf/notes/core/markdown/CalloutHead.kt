package com.markleaf.notes.core.markdown

import org.commonmark.node.AbstractVisitor
import org.commonmark.node.BlockQuote
import org.commonmark.node.Code
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.Text

/**
 * The first line of a GitHub/Obsidian-style callout: `> [!TYPE]`, optionally
 * followed by a fold marker (`-` or `+`) and a title.
 *
 * [title] is empty when the head line carries none; the renderer then labels
 * the callout by its type.
 */
internal data class CalloutHead(val type: String, val title: String) {

    companion object {
        private val HEAD_REGEX = Regex("""^\[!([A-Za-z]+)]([+-])?""")

        /**
         * When [quote] opens with a callout head, removes the whole head line
         * from the tree and returns it; otherwise leaves [quote] alone and
         * returns null. The preview and the PDF export both read callouts
         * through this, so they agree on what is one.
         *
         * The head is matched on the paragraph's leading text node: commonmark
         * merges the bracket and the word around it back into one node after
         * failing to read them as a link, so `[!NOTE]` arrives whole.
         *
         * Whatever follows the head on its line is the title, as in Obsidian.
         * It used to stay behind as the first line of the body, and a fold
         * marker leaked into the body as a stray `-`. Folding itself is not
         * offered: the marker is dropped and the callout shows open.
         */
        fun take(quote: BlockQuote): CalloutHead? {
            val paragraph = quote.firstChild as? Paragraph ?: return null
            val lead = paragraph.firstChild as? Text ?: return null
            val match = HEAD_REGEX.find(lead.literal) ?: return null
            lead.literal = lead.literal.substring(match.range.last + 1)
            val title = StringBuilder()
            var node: Node? = lead
            while (node != null && node !is SoftLineBreak && node !is HardLineBreak) {
                val next = node.next
                title.append(flatText(node))
                node.unlink()
                node = next
            }
            node?.unlink()
            if (paragraph.firstChild == null) paragraph.unlink()
            return CalloutHead(type = match.groupValues[1], title = title.toString().trim())
        }

        private fun flatText(node: Node): String {
            if (node is Text) return node.literal
            if (node is Code) return node.literal
            val sb = StringBuilder()
            node.accept(object : AbstractVisitor() {
                override fun visit(text: Text) {
                    sb.append(text.literal)
                }

                override fun visit(code: Code) {
                    sb.append(code.literal)
                }
            })
            return sb.toString()
        }
    }
}
