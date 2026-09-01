package com.example

import org.junit.Assert.*
import org.junit.Test

class LessonPlanValidatorTest {
    private fun validPlan(): String = """
        # План урока
        | Время | Действия учителя | Класс 1 | Класс 2 | Кто работает самостоятельно |
        |---|---|---|---|---|
        | 0–5 | Организует начало | Общая разминка | Общая разминка | Никто |
        | 5–12 | Объясняет классу 1 | Работа с учителем | Самостоятельная работа | Класс 2 |
        | 12–20 | Работает с классом 2 | Самостоятельная работа | Работа с учителем | Класс 1 |
        | 20–30 | Проверяет класс 1 | Работа с учителем | Самостоятельная работа | Класс 2 |
        | 30–40 | Проверяет класс 2 | Самостоятельная работа | Работа с учителем | Класс 1 |
        | 40–45 | Подводит итог | Рефлексия | Рефлексия | Никто |

        ## Карточка — ученику
        ### Класс 1
        Задание.
        ### Класс 2
        Задание.

        ## Ответы учителю
        Ответы.
    """.trimIndent()

    @Test
    fun validMksPlanPasses() {
        val result = LessonPlanValidator.validate(validPlan(), true, 45, 10, 12)
        assertTrue(result.issues.joinToString(), result.isValid)
        assertEquals(6, result.parsedTimelineRows)
    }

    @Test
    fun detectsGapAndWrongEnd() {
        val plan = validPlan()
            .replace("| 12–20 |", "| 14–20 |")
            .replace("| 40–45 |", "| 40–44 |")
        val result = LessonPlanValidator.validate(plan, true, 45, 10, 12)
        assertTrue(result.issues.any { it.contains("разрыв", ignoreCase = true) })
        assertTrue(result.issues.any { it.contains("заканчивается", ignoreCase = true) })
    }

    @Test
    fun detectsContinuousIndependentWorkAcrossRows() {
        val plan = validPlan().replace(
            "| 12–20 | Работает с классом 2 | Самостоятельная работа | Работа с учителем | Класс 1 |",
            "| 12–20 | Продолжает с классом 1 | Работа с учителем | Самостоятельная работа | Класс 2 |"
        )
        val result = LessonPlanValidator.validate(plan, true, 45, 10, 12)
        assertTrue(result.issues.any { it.contains("Класс 2") && it.contains("25 мин") && it.contains("максимум") })
    }

    @Test
    fun detectsMissingRequiredSections() {
        val plan = validPlan()
            .replace("## Карточка — ученику", "## Материалы")
            .replace("## Ответы учителю", "## Решения")
        val result = LessonPlanValidator.validate(plan, true, 45, 10, 12)
        assertTrue(result.issues.any { it.contains("Карточка") })
        assertTrue(result.issues.any { it.contains("Ответы учителю") })
    }

    @Test
    fun oneClassDoesNotRequireMksTimeline() {
        val plan = """
            # План
            Текст.
            ## Карточка — ученику
            Задание.
            ## Ответы учителю
            Ответ.
        """.trimIndent()
        val result = LessonPlanValidator.validate(plan, false, 45)
        assertTrue(result.issues.joinToString(), result.isValid)
    }
}
