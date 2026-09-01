package com.example

import com.example.data.local.LessonPlanEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanSortingTest {
    private fun plan(id: Long, title: String, content: String, time: Long) =
        LessonPlanEntity(id, 1, title, content, timestamp = time)

    @Test fun `sorts by date and name`() {
        val plans = listOf(plan(1, "Яблоко", "", 10), plan(2, "Азбука", "", 20))
        assertEquals(listOf(2L, 1L), sortPlans(plans, PlanSort.Newest).map { it.id })
        assertEquals(listOf(2L, 1L), sortPlans(plans, PlanSort.Name).map { it.id })
    }

    @Test fun `extracts subject and grade for sorting`() {
        val math = plan(1, "Урок", "Предмет: Математика\nКласс: 3 класс", 0)
        val russian = plan(2, "Урок", "Предмет: Русский язык\nКласс: 2 класс", 0)
        assertEquals(listOf(1L, 2L), sortPlans(listOf(russian, math), PlanSort.Subject).map { it.id })
        assertEquals(listOf(2L, 1L), sortPlans(listOf(math, russian), PlanSort.Grade).map { it.id })
    }
}
