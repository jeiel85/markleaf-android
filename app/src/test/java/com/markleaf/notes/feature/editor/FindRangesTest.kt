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
    fun `does not return overlapping matches`() {
        val result = findAllRanges("aaaa", "aa")
        assertEquals(listOf(0 until 2, 2 until 4), result)
    }
}
