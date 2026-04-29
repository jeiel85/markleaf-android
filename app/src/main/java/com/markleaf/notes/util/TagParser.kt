package com.markleaf.notes.util

import java.util.regex.Pattern

object TagParser {
    // Match #tag where tag follows a space or start of line, and tag is not just #
    private val TAG_PATTERN = Pattern.compile("""(^|\s)#([^\s#][^\s]*)""")
    
    // Match Markdown headings: 1-6 # followed by space and text
    private val HEADING_PATTERN = Pattern.compile("""^#{1,6}\s+.+""", Pattern.MULTILINE)
    
    // Match URLs with fragments
    private val URL_PATTERN = Pattern.compile("""https?://[^\s]+#[^\s]*""")

    fun parseTags(content: String): List<String> {
        val tags = mutableSetOf<String>()
        val headingTags = mutableSetOf<String>()
        val urlFragments = mutableSetOf<String>()

        // Find headings and extract any tags that appear in them (to exclude)
        val headingMatcher = HEADING_PATTERN.matcher(content)
        while (headingMatcher.find()) {
            val heading = headingMatcher.group().trim()
            // Extract potential tags from heading (after # markers that are part of heading)
            val headingTagMatcher = TAG_PATTERN.matcher(heading)
            while (headingTagMatcher.find()) {
                headingTagMatcher.group(2)?.let { headingTags.add(it) }
            }
        }

        // Find URLs and extract fragments (to exclude)
        val urlMatcher = URL_PATTERN.matcher(content)
        while (urlMatcher.find()) {
            val url = urlMatcher.group()
            val fragmentStart = url.indexOf('#')
            if (fragmentStart >= 0 && fragmentStart + 1 < url.length) {
                urlFragments.add(url.substring(fragmentStart + 1))
            }
        }

        // Find all tags in content
        val tagMatcher = TAG_PATTERN.matcher(content)
        while (tagMatcher.find()) {
            val tag = tagMatcher.group(2) ?: continue
            
            // Skip if this tag appears in a heading
            if (headingTags.contains(tag)) {
                continue
            }
            
            // Skip if this tag is a URL fragment
            if (urlFragments.contains(tag)) {
                continue
            }
            
            // Validate tag name
            if (isValidTagName(tag)) {
                tags.add(tag)
            }
        }

        return tags.toList()
    }

    private fun isValidTagName(tag: String): Boolean {
        if (tag.isEmpty()) return false
        // Allow Korean characters, letters, digits, underscores, hyphens
        // Must not start with a digit
        return tag.matches(Regex("""[a-zA-Z가-힣_][a-zA-Z가-힣0-9_-]*"""))
    }

    fun normalizeTagName(tag: String): String {
        return tag.trim().lowercase()
    }
}
