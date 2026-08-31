package com.markleaf.notes.core.markdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class MarkdownSyntaxVisualTransformation(
    private val colors: MarkdownSyntaxColors,
    private val fontScale: Float = 1f
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = MarkdownSyntaxHighlighter.highlight(text.text, colors, fontScale),
            offsetMapping = OffsetMapping.Identity
        )
    }
}
