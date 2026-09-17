package com.markleaf.notes.feature.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class FindRangesTest {

    @Test
    fun `empty query returns empty list`() {
        val result = findAllRanges("hello world", "")
        assertEquals(emptyList<IntRange>(), result)
    }

    @Test
    fun `empty text returns empty list`() {
        val result = findAllRanges("", "hello")
        assertEquals(emptyList<IntRange>(), result)
    }

    @Test
    fun `finds a single match`() {
        val result = findAllRanges("hello world", "world")
        assertEquals(listOf(6 until 11), result)
    }

    @Test
    fun `finds multiple non-overlapping matches`() {
        val result = findAllRanges("aa bb aa cc aa", "aa")
        assertEquals(listOf(0 until 2, 6 until 8, 12 until 14), result)
    }

    @Test
    fun `is case insensitive`() {
        val result = findAllRanges("Hello WORLD hello", "hello")
        assertEquals(listOf(0 until 5, 12 until 17), result)
    }

    @Test
    fun `non-existent query returns empty list`() {
        val result = findAllRanges("hello world", "xyz")
        assertEquals(emptyList<IntRange>(), result)
    }

    @Test
    fun `ranges stay on the original text when lowercasing changes its length`() {
        // "İ".lowercase() is two chars ("i" + combining dot), so offsets taken
        // from a lowercased copy land one position late for every such
        // character before the match — replace would edit the wrong text.
        val text = "İstanbul apple"
        val result = findAllRanges(text, "apple")
        assertEquals(listOf(9 until 14), result)
        assertEquals("apple", text.substring(result.single()))
    }

    @Test
    fun `a match at the end after a length-changing character stays in bounds`() {
        // With lowercased offsets this range ran past the end of the text,
        // which the editor then used as a selection and replace target.
        val text = "İİ ab"
        val result = findAllRanges(text, "ab")
        assertEquals(listOf(3 until 5), result)
        assertEquals("ab", text.substring(result.single()))
    }

    @Test
    fun `the dotted capital I is found by its lowercase query`() {
        // İ(0) z(1) m(2) i(3) r(4): both the capital and the plain i match.
        // The lowercased copy put the second one at 4, which is the "r".
        val text = "İzmir"
        val result = findAllRanges(text, "i")
        assertEquals(listOf(0 until 1, 3 until 4), result)
        assertEquals("i", text.substring(result[1]))
    }

    @Test
    fun `does not return overlapping matches`() {
        val result = findAllRanges("aaaa", "aa")
        assertEquals(listOf(0 until 2, 2 until 4), result)
    }
}
