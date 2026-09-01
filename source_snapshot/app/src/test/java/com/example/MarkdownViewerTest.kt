package com.example

import com.example.ui.normalizeDisplayMarkdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownViewerTest {
    @Test
    fun topLevelHeadingIsReducedForPhoneDisplay() {
        val normalized = normalizeDisplayMarkdown("# Очень длинный заголовок\n## Раздел")

        assertEquals("## Очень длинный заголовок\n## Раздел", normalized)
    }

    @Test
    fun markdownTablesRemainUntouched() {
        val table = "| Время | Этап |\n|---|---|\n| 0–5 | Начало |"

        assertTrue(normalizeDisplayMarkdown(table).contains("| 0–5 | Начало |"))
    }
}
