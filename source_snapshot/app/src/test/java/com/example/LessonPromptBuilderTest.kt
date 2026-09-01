package com.example

import org.junit.Test
import org.junit.Assert.*

class LessonPromptBuilderTest {
    @Test
    fun testBuildLessonSystemPrompt_twoClasses() {
        val prompt = LessonPromptBuilder.buildLessonSystemPrompt(useSecondClass = true)
        assertTrue(prompt.contains("ДВА КЛАССА ОДНОВРЕМЕННО"))
        assertTrue(prompt.contains("качелей"))
        assertTrue(prompt.contains("НЕ используй LaTeX"))
    }

    @Test
    fun testBuildLessonSystemPrompt_oneClass() {
        val prompt = LessonPromptBuilder.buildLessonSystemPrompt(useSecondClass = false)
        assertTrue(prompt.contains("ОДИН КЛАСС"))
        assertFalse(prompt.contains("качелей"))
        assertFalse(prompt.contains("принцип «качелей»"))
        assertTrue(prompt.contains("существует только выбранный класс", ignoreCase = true))
        assertTrue(prompt.contains("НЕ используй LaTeX"))
    }

    @Test
    fun testBuildLessonUserPrompt_twoClasses() {
        val prompt = LessonPromptBuilder.buildLessonUserPrompt(
            useSecondClass = true,
            classASubject = "Математика", classAGrade = "2 класс", classATopic = "Сложение", classALimit = "10",
            classBSubject = "Окружающий мир", classBLimit = "15", classBGrade = "3 класс", classBTopic = "Животные",
            planMode = "Для себя", lessonType = "Комбинированный", lessonDuration = 45, additionalInstructions = ""
        )
        assertTrue(prompt.contains("Класс 1:"))
        assertTrue(prompt.contains("Математика"))
        assertTrue(prompt.contains("Класс 2:"))
        assertTrue(prompt.contains("Окружающий мир"))
        assertTrue(prompt.contains("10 минут"))
        assertTrue(prompt.contains("15 минут"))
    }

    @Test
    fun testBuildLessonUserPrompt_oneClass() {
        val prompt = LessonPromptBuilder.buildLessonUserPrompt(
            useSecondClass = false,
            classASubject = "Математика", classAGrade = "2 класс", classATopic = "Сложение", classALimit = "888",
            classBSubject = "SECRET_SUBJECT", classBLimit = "999", classBGrade = "3 класс", classBTopic = "SECRET_TOPIC",
            planMode = "Для себя", lessonType = "Комбинированный", lessonDuration = 45, additionalInstructions = ""
        )
        assertTrue(prompt.contains("Класс:"))
        assertTrue(prompt.contains("Математика"))
        
        assertFalse(prompt.contains("SECRET_SUBJECT"))
        assertFalse(prompt.contains("SECRET_TOPIC"))
        assertFalse(prompt.contains("Класс 2"))
        assertFalse(prompt.contains("Класс Б"))
        assertFalse(prompt.contains("999"))
        assertFalse(prompt.contains("888"))
        assertFalse(prompt.contains("Ограничение самостоятельной работы"))
    }

    @Test
    fun testBuildLessonUserPrompt_oneClass_grade5() {
        val prompt = LessonPromptBuilder.buildLessonUserPrompt(
            useSecondClass = false,
            classASubject = "История", classAGrade = "5 класс", classATopic = "Древний Рим", classALimit = "10",
            classBSubject = "SECRET_SUBJECT", classBLimit = "15", classBGrade = "3 класс", classBTopic = "SECRET_TOPIC",
            planMode = "Для себя", lessonType = "Комбинированный", lessonDuration = 45, additionalInstructions = ""
        )
        assertTrue(prompt.contains("Класс:"))
        assertTrue(prompt.contains("5 класс"))
        assertTrue(prompt.contains("Древний Рим"))
        assertTrue(prompt.contains("чрезмерно детскими", ignoreCase = true))
    }

    @Test
    fun testLooksLikeTwoClassPlan_falsePositives() {
        assertFalse(LessonPromptBuilder.looksLikeTwoClassPlan("Математика, 2 класс. Повторение за 1 класс"))
        assertFalse(LessonPromptBuilder.looksLikeTwoClassPlan("какой-то текст 1 класс и 2 класс без структуры"))
        assertFalse(LessonPromptBuilder.looksLikeTwoClassPlan("Только 2 класс и больше ничего"))
        assertFalse(LessonPromptBuilder.looksLikeTwoClassPlan("Математика, 2 класс"))
    }
    
    @Test
    fun testLooksLikeTwoClassPlan_truePositives() {
        assertTrue(LessonPromptBuilder.looksLikeTwoClassPlan("# Класс 1\nТекст\n# Класс 2\nТекст"))
        assertTrue(LessonPromptBuilder.looksLikeTwoClassPlan("Урок\n## Класс А\nДети делают что-то\n## Класс Б\nДругие дети делают что-то"))
        assertTrue(LessonPromptBuilder.looksLikeTwoClassPlan("Тема класса 1: Сложение\nТема класса 2: Животные"))
    }
}
