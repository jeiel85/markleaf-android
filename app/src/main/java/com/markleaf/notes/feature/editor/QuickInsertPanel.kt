package com.markleaf.notes.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R

internal data class QuickInsertDisplayItem(
    val command: QuickInsertCommand,
    val label: String,
    val syntax: String
)

@Composable
internal fun quickInsertDisplayItems(): List<QuickInsertDisplayItem> = listOf(
    QuickInsertDisplayItem(QuickInsertCommand.HEADING_1, stringResource(R.string.quick_insert_heading_1), "#"),
    QuickInsertDisplayItem(QuickInsertCommand.HEADING_2, stringResource(R.string.quick_insert_heading_2), "##"),
    QuickInsertDisplayItem(QuickInsertCommand.HEADING_3, stringResource(R.string.quick_insert_heading_3), "###"),
    QuickInsertDisplayItem(QuickInsertCommand.BULLET_LIST, stringResource(R.string.quick_insert_bullet_list), "-"),
    QuickInsertDisplayItem(QuickInsertCommand.NUMBERED_LIST, stringResource(R.string.quick_insert_numbered_list), "1."),
    QuickInsertDisplayItem(QuickInsertCommand.CHECKLIST, stringResource(R.string.quick_insert_checklist), "- [ ]"),
    QuickInsertDisplayItem(QuickInsertCommand.QUOTE, stringResource(R.string.quick_insert_quote), ">"),
    QuickInsertDisplayItem(QuickInsertCommand.CODE_BLOCK, stringResource(R.string.quick_insert_code_block), "```"),
    QuickInsertDisplayItem(QuickInsertCommand.DIVIDER, stringResource(R.string.quick_insert_divider), "---"),
    QuickInsertDisplayItem(QuickInsertCommand.TABLE, stringResource(R.string.quick_insert_table), "| |"),
    QuickInsertDisplayItem(QuickInsertCommand.CALLOUT, stringResource(R.string.quick_insert_callout), "> [!NOTE]"),
    QuickInsertDisplayItem(QuickInsertCommand.WIKILINK, stringResource(R.string.quick_insert_wikilink), "[[ ]]"),
    QuickInsertDisplayItem(QuickInsertCommand.IMAGE, stringResource(R.string.quick_insert_image), "![]()"),
    QuickInsertDisplayItem(QuickInsertCommand.DATE, stringResource(R.string.quick_insert_date), "yyyy-MM-dd")
)

@Composable
internal fun QuickInsertPanel(
    items: List<QuickInsertDisplayItem>,
    selectedIndex: Int,
    onPick: (QuickInsertCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = stringResource(R.string.quick_insert_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                itemsIndexed(items, key = { _, item -> item.command }) { index, item ->
                    QuickInsertRow(
                        item = item,
                        selected = index == selectedIndex,
                        onClick = { onPick(item.command) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickInsertRow(
    item: QuickInsertDisplayItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics { this.selected = selected }
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = iconFor(item.command),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(contentAlignment = Alignment.CenterEnd) {
            Text(
                text = item.syntax,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = contentColor
            )
        }
    }
}

private fun iconFor(command: QuickInsertCommand): ImageVector = when (command) {
    QuickInsertCommand.HEADING_1,
    QuickInsertCommand.HEADING_2,
    QuickInsertCommand.HEADING_3 -> Icons.Default.Title
    QuickInsertCommand.BULLET_LIST -> Icons.AutoMirrored.Filled.FormatListBulleted
    QuickInsertCommand.NUMBERED_LIST -> Icons.Default.FormatListNumbered
    QuickInsertCommand.CHECKLIST -> Icons.Default.CheckBox
    QuickInsertCommand.QUOTE -> Icons.Default.FormatQuote
    QuickInsertCommand.CODE_BLOCK -> Icons.Default.DataObject
    QuickInsertCommand.DIVIDER -> Icons.Default.HorizontalRule
    QuickInsertCommand.TABLE -> Icons.Default.TableChart
    QuickInsertCommand.CALLOUT -> Icons.Default.Info
    QuickInsertCommand.WIKILINK -> Icons.Default.Link
    QuickInsertCommand.IMAGE -> Icons.Default.Image
    QuickInsertCommand.DATE -> Icons.Default.CalendarMonth
}
