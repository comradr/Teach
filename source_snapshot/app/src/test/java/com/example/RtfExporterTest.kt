package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RtfExporterTest {
    @Test
    fun `markdown table becomes native rtf table`() {
        val rtf = markdownToRtf("""
            | Время | Класс 1 | Класс 2 |
            |---|---|---|
            | 0–5 | Проверка | Работа |
        """.trimIndent())

        assertTrue(rtf.contains("\\trowd"))
        assertTrue(rtf.contains("\\cellx"))
        assertTrue(rtf.contains("\\row"))
        assertFalse(rtf.contains("|---|"))
    }

    @Test
    fun `headings lists and cyrillic remain editable`() {
        val rtf = markdownToRtf("# План\n- **Цель:** повторение")
        assertTrue(rtf.contains("\\fs36"))
        assertTrue(rtf.contains("\\bullet"))
        assertTrue(rtf.contains("\\u1055?"))
        assertTrue(rtf.contains("\\b "))
    }
}
