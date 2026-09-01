package com.example

import org.junit.Assert.assertTrue
import org.junit.Test

class PlanDocumentActionsTest {
    @Test
    fun printableCardsUseTwoColumnNativePrintLayout() {
        val html = cardsMarkdownToHtml("## Карточка ученику\n- Задание 1\n")

        assertTrue(html.contains("grid-template-columns: repeat(2, 1fr)"))
        assertTrue(html.contains("<h2>Карточка ученику</h2>"))
        assertTrue(html.contains("<li>Задание 1</li>"))
    }

    @Test
    fun fullPlanPrintRendersNativeTableAndEscapesHtml() {
        val html = fullPlanMarkdownToHtml(
            "Математика <1 класс>",
            "# Урок\n| Этап | Время |\n|---|---|\n| Начало | **5 мин** |\n- Задание"
        )

        assertTrue(html.contains("Математика &lt;1 класс&gt;"))
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("<th>Этап</th>"))
        assertTrue(html.contains("<td><b>5 мин</b></td>"))
        assertTrue(html.contains("<li>Задание</li>"))
    }
}
