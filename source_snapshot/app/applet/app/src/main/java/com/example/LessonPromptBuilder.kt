package com.example

object LessonPromptBuilder {

    fun buildLessonSystemPrompt(useSecondClass: Boolean): String {
        return if (useSecondClass) {
            """
            Ты — опытный школьный методист для 1–5 классов, в том числе для малокомплектной школы.
            ДВА КЛАССА ОДНОВРЕМЕННО.
            Используй принцип «качелей»: пока учитель работает с одним классом, второй выполняет самостоятельную работу.
            Структура должна чётко разделять этапы урока. Временная таблица должна учитывать время урока, темы каждого класса, ограничения самостоятельной работы и переключение внимания учителя.
            В конце обязательно сгенерируй готовые карточки с заданиями для каждого класса, которые можно распечатать и дать ученикам, и ответы учителю.
            Оформи план в формате Markdown.
            """.trimIndent()
        } else {
            """
            Ты — опытный школьный методист для 1–5 классов.
            ФОРМАТ УРОКА: ОДИН КЛАСС.
            Существует только выбранный класс, нельзя добавлять второй класс, нельзя придумывать вторую группу, нельзя использовать схему совмещённого урока, нельзя строить расписание «качели».
            Структура урока: | Время | Этап урока | Действия учителя | Деятельность учеников | Материалы/заметки |.
            В конце готовых материалов добавь `## Карточка — ученику` (полностью готовая карточка с заданиями для печати) и затем `## Ответы учителю`.
            Оформи план в формате Markdown.
            """.trimIndent()
        }
    }

    fun buildLessonUserPrompt(
        useSecondClass: Boolean,
        classASubject: String,
        classAGrade: String,
        classATopic: String,
        classALimit: String,
        classBSubject: String,
        classBGrade: String,
        classBTopic: String,
        classBLimit: String,
        planMode: String,
        lessonType: String,
        lessonDuration: Int,
        additionalInstructions: String
    ): String {
        val modeDesc = if (planMode == "Официальный") {
            "Формальный официальный план (с целями, задачами, УУД, этапами, формулировками для документации, но без избыточности)."
        } else {
            "Рабочий план «Для себя» (практичный, минимум бюрократии, чётко что говорит/делает учитель и дети)."
        }
        
        var prompt = "Формат плана: $modeDesc\n"
        prompt += "Тип урока: $lessonType\n"
        prompt += "Длительность урока: $lessonDuration минут.\n\n"

        if (useSecondClass) {
            prompt += "Класс 1: $classAGrade, Предмет: $classASubject\nТема: $classATopic\n"
            if (classALimit.isNotBlank()) prompt += "Ограничение самостоятельной работы: $classALimit минут.\n\n"
            
            prompt += "Класс 2: $classBGrade, Предмет: $classBSubject\nТема: $classBTopic\n"
            if (classBLimit.isNotBlank()) prompt += "Ограничение самостоятельной работы: $classBLimit минут.\n"
        } else {
            prompt += "Класс: $classAGrade, Предмет: $classASubject\nТема: $classATopic\n"
            if (classAGrade == "5 класс") {
                prompt += "Важно: Это 5 класс, задания не должны быть чрезмерно детскими (соответствовать возрасту и уровню).\n"
            }
        }

        if (additionalInstructions.isNotBlank()) {
            prompt += "\nДополнительные пожелания от учителя:\n$additionalInstructions\n"
        }

        return prompt
    }

    fun looksLikeTwoClassPlan(planContent: String): Boolean {
        val lowerContent = planContent.lowercase()
        
        // Search for structural signs of two different classes
        val structuralSigns = listOf(
            Regex("класс 1.*класс 2", RegexOption.DOT_MATCHES_ALL),
            Regex("класс а.*класс б", RegexOption.DOT_MATCHES_ALL),
            Regex("тема.*класса 1.*тема.*класса 2", RegexOption.DOT_MATCHES_ALL)
        )
        
        if (structuralSigns.any { it.containsMatchIn(lowerContent) }) {
            // Further verify it's a plan structure, not just a random sentence.
            // Look for headings or newlines before "класс"
            if (Regex("(^|\n)#*.*класс 1").containsMatchIn(lowerContent) && Regex("(^|\n)#*.*класс 2").containsMatchIn(lowerContent)) {
                return true
            }
            if (Regex("(^|\n)#*.*класс а").containsMatchIn(lowerContent) && Regex("(^|\n)#*.*класс б").containsMatchIn(lowerContent)) {
                return true
            }
        }
        
        // Also look for explicit mentions of "качели" combined with "класс 1" / "класс 2" in a structural way
        if (lowerContent.contains("качели") && lowerContent.contains("совмещенный урок")) {
            return true
        }

        return false
    }
}
