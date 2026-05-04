package com.markleaf.notes.core.markdown

/**
 * Utility class for parsing and tracking checklist progress in Markdown content.
 */
object ChecklistParser {

    /**
     * Regex pattern to match checklist items: - [ ] or - [x]
     */
    private val CHECKLIST_ITEM_REGEX = Regex("""^[\s]*-\s*\[([xX\s])\]\s*(.+)$""")

    /**
     * Data class representing checklist progress.
     */
    data class ChecklistProgress(
        val total: Int,
        val checked: Int,
        val unchecked: Int,
        val percentage: Int
    ) {
        companion object {
            val EMPTY = ChecklistProgress(0, 0, 0, 0)
        }
    }

    /**
     * Parse markdown content and calculate checklist progress.
     *
     * @param content The markdown content to parse
     * @return ChecklistProgress containing total, checked, unchecked counts and percentage
     */
    fun parseProgress(content: String): ChecklistProgress {
        val lines = content.lines()
        var total = 0
        var checked = 0

        lines.forEach { line ->
            val match = CHECKLIST_ITEM_REGEX.find(line.trim())
            if (match != null) {
                total++
                val checkMark = match.groupValues[1]
                if (checkMark.isNotBlank() && checkMark.lowercase() == "x") {
                    checked++
                }
            }
        }

        val unchecked = total - checked
        val percentage = if (total > 0) (checked * 100 / total) else 0

        return ChecklistProgress(total, checked, unchecked, percentage)
    }

    /**
     * Check if the content contains any checklist items.
     *
     * @param content The markdown content to check
     * @return true if the content contains at least one checklist item
     */
    fun hasChecklists(content: String): Boolean {
        return content.lines().any { line ->
            CHECKLIST_ITEM_REGEX.matches(line.trim())
        }
    }

    /**
     * Get the number of checklist items in the content.
     *
     * @param content The markdown content to parse
     * @return The total number of checklist items
     */
    fun getChecklistCount(content: String): Int {
        return content.lines().count { line ->
            CHECKLIST_ITEM_REGEX.matches(line.trim())
        }
    }
}