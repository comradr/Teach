package com.example

import org.junit.Test
import org.junit.Assert.*

class CardExtractorTest {
    @Test
    fun testExtractCards() {
        val content = """
            # План урока
            Текст
            ## Карточка — ученику
            Пример 1
            ## Ответы учителю
            Пример 2
        """.trimIndent()
        val cards = extractCardsForPrinting(content)
        assertTrue(cards.contains("Карточка"))
        assertTrue(cards.contains("Пример 1"))
        assertFalse(cards.contains("Ответы учителю"))
        assertFalse(cards.contains("Пример 2"))
    }

    @Test
    fun testNestedClassHeadingsStayInsideCard() {
        val content = """
            # План урока
            ## Карточка — ученику
            ### Класс 1
            1. Задание А
            ### Класс 2
            1. Задание Б
            ## Ответы учителю
            Ответы
        """.trimIndent()

        val cards = extractCardsForPrinting(content)
        assertTrue(cards.contains("### Класс 1"))
        assertTrue(cards.contains("Задание А"))
        assertTrue(cards.contains("### Класс 2"))
        assertTrue(cards.contains("Задание Б"))
        assertFalse(cards.contains("Ответы учителю"))
    }
}
