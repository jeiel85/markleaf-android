package com.markleaf.notes.feature.editor

import androidx.compose.ui.text.input.TextFieldValue
import com.markleaf.notes.core.markdown.MarkdownEditActions

internal enum class EditorFormattingAction {
    BOLD,
    ITALIC,
    STRIKETHROUGH,
    INLINE_CODE,
    LINK,
    HEADING,
    BULLET_LIST,
    ORDERED_LIST,
    CHECKLIST,
    QUOTE,
    CODE_BLOCK,
    DIVIDER,
    IMAGE
}

internal sealed interface EditorFormattingResult {
    data class Edited(val value: TextFieldValue) : EditorFormattingResult
    data object PickImage : EditorFormattingResult
}

internal fun EditorFormattingAction.applyTo(value: TextFieldValue): EditorFormattingResult = when (this) {
    EditorFormattingAction.BOLD -> EditorFormattingResult.Edited(MarkdownEditActions.bold(value))
    EditorFormattingAction.ITALIC -> EditorFormattingResult.Edited(MarkdownEditActions.italic(value))
    EditorFormattingAction.STRIKETHROUGH -> EditorFormattingResult.Edited(
        MarkdownEditActions.strikethrough(value)
    )
    EditorFormattingAction.INLINE_CODE -> EditorFormattingResult.Edited(MarkdownEditActions.inlineCode(value))
    EditorFormattingAction.LINK -> EditorFormattingResult.Edited(MarkdownEditActions.markdownLink(value))
    EditorFormattingAction.HEADING -> EditorFormattingResult.Edited(MarkdownEditActions.heading(value))
    EditorFormattingAction.BULLET_LIST -> EditorFormattingResult.Edited(MarkdownEditActions.bulletList(value))
    EditorFormattingAction.ORDERED_LIST -> EditorFormattingResult.Edited(MarkdownEditActions.orderedList(value))
    EditorFormattingAction.CHECKLIST -> EditorFormattingResult.Edited(MarkdownEditActions.checkbox(value))
    EditorFormattingAction.QUOTE -> EditorFormattingResult.Edited(MarkdownEditActions.blockquote(value))
    EditorFormattingAction.CODE_BLOCK -> EditorFormattingResult.Edited(MarkdownEditActions.codeBlock(value))
    EditorFormattingAction.DIVIDER -> EditorFormattingResult.Edited(MarkdownEditActions.horizontalRule(value))
    EditorFormattingAction.IMAGE -> EditorFormattingResult.PickImage
}
