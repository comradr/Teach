package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanDocumentActionsTest {
    @Test
    fun printableCardsUseTwoColumnNativePrintLayout() {
        val html = cardsMarkdownToHtml("## Карточка ученику\n- Задание 1<br>Подсказка\n")

        assertTrue(html.contains("grid-template-columns: repeat(2, 1fr)"))
        assertTrue(html.contains("<h2>Карточка ученику</h2>"))
        assertTrue(html.contains("<li>Задание 1<br>Подсказка</li>"))
        assertFalse(html.contains("&lt;br&gt;"))
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

    @Test
    fun fullPlanPrintConvertsBreakTagsInsideTableCells() {
        val html = fullPlanMarkdownToHtml(
            "Русский язык",
            """
                ## Ход урока
                | Время | Этап урока | Действия учителя | Деятельность учеников | Материалы |
                |---|---|---|---|---|
                | 3–10 | Минутка | **На доске:** П п<br>Запишите *пары букв*.<br />Объясните _правило_. | Пишут | Доска |
            """.trimIndent()
        )

        assertFalse(html.contains("&lt;br", ignoreCase = true))
        assertTrue(html.contains("П п<br>Запишите <i>пары букв</i>.<br>Объясните <i>правило</i>."))
        assertTrue(html.contains("<col style=\"width:8%\">"))
        assertTrue(html.contains("<col style=\"width:31%\">"))
        assertTrue(html.contains("table-layout: fixed"))
        assertTrue(html.contains("thead { display: table-header-group; }"))
    }

    @Test
    fun fullPlanPrintDecodesCommonEntitiesOnce() {
        val html = fullPlanMarkdownToHtml("План", "Тема:&nbsp;имена &amp; фамилии")

        assertFalse(html.contains("&nbsp;"))
        assertTrue(html.contains("Тема: имена &amp; фамилии"))
    }
}
