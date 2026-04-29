package com.markleaf.notes.core.text

import java.time.Instant

object TitleExtractor {
    
    /**
     * Extract title from markdown content.
     * Priority:
     * 1. First Markdown heading
     * 2. First non-empty line
     * 3. Fallback: "Untitled"
     */
    fun extractTitle(content: String): String {
        if (content.isBlank()) return "Untitled"
        
        val lines = content.lines().map { it.trim() }
        
        // Priority 1: First Markdown heading
        val heading = lines.firstOrNull { line ->
            line.startsWith("#") && line.length > 1 && (line[1] == ' ' || line[1] == '#')
        }
        if (heading != null) {
            return heading
                .replace(Regex("#+\\s*"), "")
                .trim()
                .take(80)
                .ifEmpty { "Untitled" }
        }
        
        // Priority 2: First non-empty line
        val firstNonEmpty = lines.firstOrNull { it.isNotEmpty() }
        if (firstNonEmpty != null) {
            return firstNonEmpty.take(80)
        }
        
        return "Untitled"
    }
    
    /**
     * Generate excerpt from markdown content (first 100 chars)
     */
    fun generateExcerpt(content: String, maxLength: Int = 100): String {
        val clean = content
            .replace(Regex("#+\\s*"), "") // Remove headings
            .replace(Regex("\\*+|__|~~|`+"), "") // Remove bold, italic, strikethrough, code
            .trim()
        
        return if (clean.length <= maxLength) clean else clean.take(maxLength) + "..."
    }
}
