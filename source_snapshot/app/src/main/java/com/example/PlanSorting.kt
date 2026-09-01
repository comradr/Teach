package com.example

import com.example.data.local.LessonPlanEntity

enum class PlanSort(val label: String) {
    Newest("Сначала новые"),
    Oldest("Сначала старые"),
    Name("По названию"),
    Subject("По предмету"),
    Grade("По классу")
}

fun sortPlans(plans: List<LessonPlanEntity>, sort: PlanSort): List<LessonPlanEntity> = when (sort) {
    PlanSort.Newest -> plans.sortedByDescending { it.timestamp }
    PlanSort.Oldest -> plans.sortedBy { it.timestamp }
    PlanSort.Name -> plans.sortedBy { it.title.lowercase() }
    PlanSort.Subject -> plans.sortedBy { extractPlanSubject(it).lowercase() }
    PlanSort.Grade -> plans.sortedWith(compareBy({ extractPlanGrade(it) }, { it.title.lowercase() }))
}

fun extractPlanSubject(plan: LessonPlanEntity): String {
    val labeled = Regex("(?im)^\\s*(?:[-*]\\s*)?(?:\\*\\*)?Предмет(?:\\*\\*)?\\s*:\\s*([^\\n.]+)")
        .find(plan.content)?.groupValues?.get(1)?.trim()
    if (!labeled.isNullOrBlank()) return labeled
    return Regex("(?im)Класс\\s*\\d+[^:]*:\\s*([^\\n.]+)")
        .find(plan.content)?.groupValues?.get(1)?.trim().orEmpty().ifBlank { plan.title }
}

fun extractPlanGrade(plan: LessonPlanEntity): Int {
    val text = plan.title + "\n" + plan.content
    val patterns = listOf(
        Regex("(?im)^\\s*(?:[-*]\\s*)?(?:\\*\\*)?Класс(?:ы)?(?:\\*\\*)?\\s*:\\s*(\\d+)"),
        Regex("(?i)(\\d+)\\s*(?:[-–]?[а-яё]+\\s*)?класс"),
        Regex("(?i)класс(?:ы)?\\D{0,12}(\\d+)")
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        pattern.find(text)?.groupValues?.get(1)?.toIntOrNull()
    } ?: Int.MAX_VALUE
}
