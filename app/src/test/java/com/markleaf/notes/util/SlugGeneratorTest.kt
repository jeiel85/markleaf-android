package com.markleaf.notes.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SlugGeneratorTest {

    @Test
    fun `generateSlug converts to lowercase`() {
        val input = "HELLO World"
        val slug = SlugGenerator.generateSlug(input)
        assertEquals("hello-world", slug)
    }

    @Test
    fun `generateSlug replaces spaces with hyphens`() {
        val input = "hello world test"
        val slug = SlugGenerator.generateSlug(input)
        assertEquals("hello-world-test", slug)
    }

    @Test
    fun `generateSlug removes special characters`() {
        val input = "hello!@#$%^&*()world"
        val slug = SlugGenerator.generateSlug(input)
        assertEquals("helloworld", slug)
    }

    @Test
    fun `generateSlug handles multiple hyphens`() {
        val input = "hello---world"
        val slug = SlugGenerator.generateSlug(input)
        assertEquals("hello-world", slug)
    }

    @Test
    fun `generateSlug trims hyphens from ends`() {
        val input = "-hello world-"
        val slug = SlugGenerator.generateSlug(input)
        assertEquals("hello-world", slug)
    }

    @Test
    fun `generateSlug handles Korean text`() {
        val input = "안녕 세계"
        val slug = SlugGenerator.generateSlug(input)
        assertEquals("안녕-세계", slug)
    }

    @Test
    fun `generateSlug handles empty string`() {
        val input = ""
        val slug = SlugGenerator.generateSlug(input)
        assertEquals("", slug)
    }
}
