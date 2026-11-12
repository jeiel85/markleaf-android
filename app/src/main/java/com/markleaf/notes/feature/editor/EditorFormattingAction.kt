package com.markleaf.notes.feature.editor

import androidx.annotation.StringRes
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.input.TextFieldValue
import com.markleaf.notes.R
import com.markleaf.notes.core.markdown.MarkdownEditActions
import java.time.LocalDate

internal enum class EditorFormattingAction {
    BOLD,
    ITALIC,
    STRIKETHROUGH,
    INLINE_CODE,
    LINK,
    WIKILINK,
    HEADING,
    BULLET_LIST,
    ORDERED_LIST,
    CHECKLIST,
    QUOTE,
    CODE_BLOCK,
    TABLE,
    CALLOUT,
    DIVIDER,
    IMAGE,
    DATE
}

internal sealed interface EditorFormattingResult {
    data class Edited(val value: TextFieldValue) : EditorFormattingResult
    data object PickImage : EditorFormattingResult

    /**
     * The action declined on purpose and the text is unchanged; [message]
     * says why, so a refusal doesn't look like a tap the panel dropped (#262).
     */
    data class Refused(@StringRes val message: Int) : EditorFormattingResult
}

/**
 * The single hardware-keyboard formatting keymap.
 *
 * Both the editor text field and the expanded formatting panel resolve
 * shortcuts through [toFormattingAction] so the two dispatch paths cannot
 * drift as actions evolve.
 *
 * Bare Ctrl+S is intentionally NOT bound: writers reflex-hit it to "save",
 * and since Markleaf auto-saves we must not turn that keystroke into a
 * strikethrough that mangles text — strikethrough requires the explicit Shift.
 */
internal fun formattingShortcutFor(key: Key, shiftPressed: Boolean): EditorFormattingAction? = when (key) {
    Key.B -> EditorFormattingAction.BOLD
    Key.I -> EditorFormattingAction.ITALIC
    Key.K -> EditorFormattingAction.LINK
    Key.S -> if (shiftPressed) EditorFormattingAction.STRIKETHROUGH else null
    else -> null
}

/**
 * Resolves a key event to a formatting action, or null when it is not a
 * formatting shortcut. Accepts Ctrl (typical Android external keyboards) or
 * Meta/Cmd (Mac-style tablet keyboards) so both feel native.
 */
internal fun KeyEvent.toFormattingAction(): EditorFormattingAction? =
    if (isCtrlPressed || isMetaPressed) formattingShortcutFor(key, isShiftPressed) else null

/**
 * [today] is injected rather than read inside so a test can pin the panel's
 * date row and `/date` to the same string; every caller in the app takes the
 * default. [tableHeaders] is injected because it comes from string resources,
 * which this file has no context to read; the editor passes the app
 * language's labels.
 */
internal fun EditorFormattingAction.applyTo(
    value: TextFieldValue,
    today: LocalDate = LocalDate.now(),
    tableHeaders: MarkdownEditActions.TableHeaders = MarkdownEditActions.TableHeaders.ENGLISH
): EditorFormattingResult = when (this) {
    EditorFormattingAction.BOLD -> EditorFormattingResult.Edited(MarkdownEditActions.bold(value))
    EditorFormattingAction.ITALIC -> EditorFormattingResult.Edited(MarkdownEditActions.italic(value))
    EditorFormattingAction.STRIKETHROUGH -> EditorFormattingResult.Edited(
        MarkdownEditActions.strikethrough(value)
    )
    EditorFormattingAction.INLINE_CODE -> EditorFormattingResult.Edited(MarkdownEditActions.inlineCode(value))
    EditorFormattingAction.LINK -> EditorFormattingResult.Edited(MarkdownEditActions.markdownLink(value))
    EditorFormattingAction.WIKILINK -> EditorFormattingResult.Edited(MarkdownEditActions.wikilink(value))
    EditorFormattingAction.HEADING -> EditorFormattingResult.Edited(MarkdownEditActions.heading(value))
    EditorFormattingAction.BULLET_LIST -> EditorFormattingResult.Edited(MarkdownEditActions.bulletList(value))
    EditorFormattingAction.ORDERED_LIST -> EditorFormattingResult.Edited(MarkdownEditActions.orderedList(value))
    EditorFormattingAction.CHECKLIST -> EditorFormattingResult.Edited(MarkdownEditActions.checkbox(value))
    EditorFormattingAction.QUOTE -> EditorFormattingResult.Edited(MarkdownEditActions.blockquote(value))
    EditorFormattingAction.CODE_BLOCK -> EditorFormattingResult.Edited(MarkdownEditActions.codeBlock(value))
    EditorFormattingAction.TABLE -> EditorFormattingResult.Edited(MarkdownEditActions.table(value, tableHeaders))
    EditorFormattingAction.CALLOUT -> if (MarkdownEditActions.calloutWouldNest(value)) {
        EditorFormattingResult.Refused(R.string.callout_already_in_selection)
    } else {
        EditorFormattingResult.Edited(MarkdownEditActions.callout(value))
    }
    EditorFormattingAction.DIVIDER -> EditorFormattingResult.Edited(MarkdownEditActions.horizontalRule(value))
    EditorFormattingAction.IMAGE -> EditorFormattingResult.PickImage
    EditorFormattingAction.DATE -> EditorFormattingResult.Edited(MarkdownEditActions.date(value, today))
}
