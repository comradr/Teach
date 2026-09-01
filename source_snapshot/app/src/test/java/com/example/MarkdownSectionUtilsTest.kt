package com.example

import org.junit.Assert.*
import org.junit.Test

class MarkdownSectionUtilsTest {
    @Test
    fun nestedHeadingsBelongToParentSection() {
        val plan = """
            # План
            ## Ход урока
            ### Этап 1
            Текст А
            ### Этап 2
            Текст Б
            ## Карточка — ученику
            Задание
        """.trimIndent()

        val section = findMarkdownSection(plan, "Ход урока")
        assertNotNull(section)
        assertTrue(section!!.text.contains("### Этап 1"))
        assertTrue(section.text.contains("### Этап 2"))
        assertFalse(section.text.contains("Карточка"))
    }

    @Test
    fun replacementTouchesOnlySelectedSection() {
        val plan = """
            # План
            ## Первый
            одинаковый текст
            ## Второй
            одинаковый текст
            ## Третий
            конец
        """.trimIndent()

        val second = findMarkdownSection(plan, "Второй")!!
        val changed = replaceMarkdownSection(plan, second, "новый текст")

        assertTrue(changed.contains("## Первый\nодинаковый текст"))
        assertTrue(changed.contains("## Второй\nновый текст"))
        assertFalse(changed.contains("## Второй\nодинаковый текст"))
        assertTrue(changed.contains("## Третий\nконец"))
    }
}
