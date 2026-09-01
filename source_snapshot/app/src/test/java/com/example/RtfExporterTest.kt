package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RtfExporterTest {
    @Test
    fun `markdown table becomes bordered native rtf table`() {
        val rtf = markdownToRtf("""
            | Время | Этап урока | Действия учителя | Деятельность учеников | Материалы |
            |---|---|---|---|---|
            | 0–5 | Начало | Проверка | Работа | Тетради |
        """.trimIndent())

        assertTrue(rtf.contains("\\trowd"))
        assertTrue(rtf.contains("\\intbl"))
        assertTrue(rtf.contains("\\cellx816"))
        assertTrue(rtf.contains("\\cellx10200"))
        assertTrue(rtf.contains("\\clbrdrt\\brdrs\\brdrw10"))
        assertTrue(rtf.contains("\\clbrdrr\\brdrs\\brdrw10"))
        assertTrue(rtf.contains("\\clcbpat2"))
        assertTrue(rtf.contains("\\row"))
        assertFalse(rtf.contains("|---|"))
    }

    @Test
    fun `html breaks and markdown emphasis are rendered instead of leaking`() {
        val rtf = markdownToRtf("""
            | Этап | Действия учителя | Ученики |
            |---|---|---|
            | Разминка | **На доске:** П п<br>Запишите *пары букв*.<br/>Объясните _правило_. | Работают |
        """.trimIndent())

        assertFalse(rtf.contains("<br", ignoreCase = true))
        assertTrue(rtf.contains("\\line "))
        assertTrue(rtf.contains("\\b "))
        assertTrue(rtf.contains("\\i "))
        assertFalse(rtf.contains("*пары букв*"))
        assertFalse(rtf.contains("_правило_"))
    }

    @Test
    fun `headings lists and cyrillic remain editable`() {
        val rtf = markdownToRtf("# План\n- **Цель:** повторение")
        assertTrue(rtf.contains("\\fs36"))
        assertTrue(rtf.contains("\\bullet"))
        assertTrue(rtf.contains("\\u1055?"))
        assertTrue(rtf.contains("\\b "))
    }

    @Test
    fun `common html entities do not leak into rtf`() {
        val rtf = markdownToRtf("Тема:&nbsp;имена &amp; фамилии")
        assertFalse(rtf.contains("&nbsp;"))
        assertFalse(rtf.contains("&amp;"))
        assertTrue(rtf.contains(" & "))
    }
}
