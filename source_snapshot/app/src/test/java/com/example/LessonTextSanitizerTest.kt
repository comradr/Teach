package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LessonTextSanitizerTest {

    @Test
    fun `sanitize simple inline math with variables`() {
        assertEquals("13 + 9 = ___", LessonTextSanitizer.sanitizeGeminiMarkdown("${'$'}13 + 9 = \\text{___}${'$'}"))
    }

    @Test
    fun `sanitize times operator`() {
        assertEquals("5 × 4 = 20", LessonTextSanitizer.sanitizeGeminiMarkdown("${'$'}5 \\times 4 = 20${'$'}"))
    }

    @Test
    fun `sanitize div operator`() {
        assertEquals("20 ÷ 4 = 5", LessonTextSanitizer.sanitizeGeminiMarkdown("${'$'}20 \\div 4 = 5${'$'}"))
    }

    @Test
    fun `sanitize fraction`() {
        assertEquals("3/4", LessonTextSanitizer.sanitizeGeminiMarkdown("\\frac{3}{4}"))
        assertEquals("12/5", LessonTextSanitizer.sanitizeGeminiMarkdown("\\frac{12}{5}"))
    }

    @Test
    fun `sanitize boxed`() {
        assertEquals("42", LessonTextSanitizer.sanitizeGeminiMarkdown("\\boxed{42}"))
    }

    @Test
    fun `sanitize neq`() {
        assertEquals("x ≠ 5", LessonTextSanitizer.sanitizeGeminiMarkdown("${'$'}x \\neq 5${'$'}"))
    }

    @Test
    fun `sanitize markdown preservation`() {
        val input = "## Карточка — ученику\n**Пример:** ${'$'}4 + 5 = \\text{___}${'$'}"
        val expected = "## Карточка — ученику\n**Пример:** 4 + 5 = ___"
        assertEquals(expected, LessonTextSanitizer.sanitizeGeminiMarkdown(input))
    }
    
    @Test
    fun `test no latex left behind`() {
        val rawResult = "## Ответы\n1. ${'$'}\\frac{1}{2}${'$'}\n2. ${'$'}x \\times y${'$'}\n3. \\boxed{answer}"
        val sanitized = LessonTextSanitizer.sanitizeGeminiMarkdown(rawResult)
        
        assertFalse(sanitized.contains("\\text{"))
        assertFalse(sanitized.contains("\\frac{"))
        assertFalse(sanitized.contains("\\boxed{"))
        assertFalse(sanitized.contains("\\times"))
        assertFalse(sanitized.contains("\\div"))
        
        // Checking for paired dollar signs using regex
        val hasDollarPair = Regex("""\$.+?\$""").containsMatchIn(sanitized)
        assertFalse("Should not contain dollar pairs", hasDollarPair)
    }

    @Test
    fun `preserve single currency dollar`() {
        val input = "Цена: ${'$'}5"
        assertEquals("Цена: ${'$'}5", LessonTextSanitizer.sanitizeGeminiMarkdown(input))
    }

    @Test
    fun `sanitize leq and geq without trailing q`() {
        assertEquals("x ≤ 5", LessonTextSanitizer.sanitizeGeminiMarkdown("x \\leq 5"))
        assertEquals("x ≥ 5", LessonTextSanitizer.sanitizeGeminiMarkdown("x \\geq 5"))
    }
}
