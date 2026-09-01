package com.example

import com.example.ui.components.splitPlanSections
import org.junit.Assert.assertEquals
import org.junit.Test

class StructuredPlanViewerTest {
    @Test
    fun splitsMarkdownAtHeadingsWithoutLosingContent() {
        val markdown = """
            # План
            Введение
            ## Цели
            Цель
            ## Ход урока
            | Время | Работа |
            |---|---|
            | 0–5 | Начало |
        """.trimIndent()

        val sections = splitPlanSections(markdown)

        assertEquals(3, sections.size)
        assertEquals(true, sections[2].contains("| 0–5 | Начало |"))
    }
}
