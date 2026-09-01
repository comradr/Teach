package com.example

/**
 * Deterministic checks for generated lesson plans. Gemini is still responsible for the
 * pedagogical content, while this validator checks the parts that can be verified reliably:
 * timeline continuity, total duration, independent-work limits and required printable sections.
 */
data class LessonPlanValidation(
    val issues: List<String>,
    val parsedTimelineRows: Int = 0
) {
    val isValid: Boolean get() = issues.isEmpty()
}

object LessonPlanValidator {
    private data class TimelineRow(
        val start: Int,
        val end: Int,
        val independentClasses: Set<Int>
    )

    fun validate(
        markdown: String,
        useSecondClass: Boolean,
        lessonDuration: Int,
        classALimit: Int? = null,
        classBLimit: Int? = null
    ): LessonPlanValidation {
        val issues = mutableListOf<String>()

        if (markdown.isBlank()) {
            return LessonPlanValidation(listOf("План пустой."))
        }

        if (findMarkdownSections(markdown, "Карточка").isEmpty()) {
            issues += "Нет раздела «Карточка — ученику»."
        }
        if (findMarkdownSections(markdown, "Ответы учителю").isEmpty()) {
            issues += "Нет раздела «Ответы учителю»."
        }

        if (!useSecondClass) {
            return LessonPlanValidation(issues)
        }

        val timelineResult = parseTimeline(markdown)
        if (timelineResult == null) {
            issues += "Не найдена временная таблица МКШ с колонками «Время», «Класс 1» и «Класс 2»."
            return LessonPlanValidation(issues)
        }

        val (rows, parseIssues) = timelineResult
        issues += parseIssues
        if (rows.isEmpty()) {
            if (parseIssues.isEmpty()) issues += "Временная таблица МКШ не содержит этапов урока."
            return LessonPlanValidation(issues)
        }

        validateTimelineContinuity(rows, lessonDuration, issues)
        validateIndependentLimit(rows, classNumber = 1, classALimit, issues)
        validateIndependentLimit(rows, classNumber = 2, classBLimit, issues)

        return LessonPlanValidation(issues.distinct(), rows.size)
    }

    private fun parseTimeline(markdown: String): Pair<List<TimelineRow>, List<String>>? {
        val lines = markdown.lines()
        var headerIndex = -1
        var timeColumn = -1
        var independentColumn = -1
        var class1Column = -1
        var class2Column = -1

        for (i in lines.indices) {
            if (!looksLikeTableRow(lines[i])) continue
            val cells = splitTableRow(lines[i])
            val normalized = cells.map { normalizeHeader(it) }
            val t = normalized.indexOfFirst { it == "время" || it.startsWith("время ") }
            val c1 = normalized.indexOfFirst { it.contains("класс 1") || it.contains("1 класс") }
            val c2 = normalized.indexOfFirst { it.contains("класс 2") || it.contains("2 класс") }
            if (t >= 0 && c1 >= 0 && c2 >= 0) {
                headerIndex = i
                timeColumn = t
                class1Column = c1
                class2Column = c2
                independentColumn = normalized.indexOfFirst { it.contains("самостоят") }
                break
            }
        }

        if (headerIndex < 0) return null

        val rows = mutableListOf<TimelineRow>()
        val issues = mutableListOf<String>()
        var i = headerIndex + 1
        if (i < lines.size && isSeparatorRow(lines[i])) i++

        while (i < lines.size && looksLikeTableRow(lines[i])) {
            val cells = splitTableRow(lines[i])
            if (cells.all { it.isBlank() }) {
                i++
                continue
            }
            val requiredMaxIndex = maxOf(timeColumn, class1Column, class2Column, independentColumn)
            if (requiredMaxIndex >= cells.size) {
                issues += "В строке временной таблицы не хватает колонок: «${lines[i].trim()}»."
                i++
                continue
            }

            val interval = parseInterval(cells[timeColumn])
            if (interval == null) {
                issues += "Не удалось распознать интервал времени «${cells[timeColumn].trim()}». Используй формат 0–5, 5–10 и т. п."
                i++
                continue
            }

            val independentText = if (independentColumn >= 0) cells[independentColumn] else ""
            val independent = detectIndependentClasses(
                independentText = independentText,
                class1Activity = cells.getOrElse(class1Column) { "" },
                class2Activity = cells.getOrElse(class2Column) { "" }
            )
            rows += TimelineRow(interval.first, interval.second, independent)
            i++
        }

        return rows to issues
    }

    private fun validateTimelineContinuity(rows: List<TimelineRow>, duration: Int, issues: MutableList<String>) {
        val sorted = rows.sortedBy { it.start }
        if (sorted.first().start != 0) {
            issues += "Временная схема должна начинаться с 0 минуты, сейчас начинается с ${sorted.first().start}."
        }

        for (row in sorted) {
            if (row.end <= row.start) {
                issues += "Некорректный интервал ${row.start}–${row.end}: конец должен быть позже начала."
            }
            if (row.start < 0 || row.end > duration) {
                issues += "Интервал ${row.start}–${row.end} выходит за длительность урока $duration мин."
            }
        }

        for (index in 1 until sorted.size) {
            val previous = sorted[index - 1]
            val current = sorted[index]
            when {
                current.start > previous.end -> issues += "Есть разрыв во времени: после ${previous.start}–${previous.end} следующий этап начинается с ${current.start}."
                current.start < previous.end -> issues += "Есть пересечение интервалов: ${previous.start}–${previous.end} и ${current.start}–${current.end}."
            }
        }

        val lastEnd = sorted.maxOf { it.end }
        if (lastEnd != duration) {
            issues += "Временная схема заканчивается на $lastEnd минуте, а урок длится $duration мин."
        }
    }

    private fun validateIndependentLimit(
        rows: List<TimelineRow>,
        classNumber: Int,
        limit: Int?,
        issues: MutableList<String>
    ) {
        if (limit == null || limit <= 0) return
        val sorted = rows.sortedBy { it.start }
        var blockStart: Int? = null
        var blockEnd: Int? = null

        fun closeBlock() {
            val start = blockStart
            val end = blockEnd
            if (start != null && end != null) {
                val length = end - start
                if (length > limit) {
                    issues += "Класс $classNumber остаётся самостоятельно $length мин. подряд ($start–$end), максимум — $limit мин."
                }
            }
            blockStart = null
            blockEnd = null
        }

        for (row in sorted) {
            if (classNumber in row.independentClasses) {
                if (blockStart == null) {
                    blockStart = row.start
                    blockEnd = row.end
                } else if (row.start == blockEnd) {
                    blockEnd = row.end
                } else {
                    closeBlock()
                    blockStart = row.start
                    blockEnd = row.end
                }
            } else {
                closeBlock()
            }
        }
        closeBlock()
    }

    private fun parseInterval(value: String): Pair<Int, Int>? {
        val normalized = value
            .lowercase()
            .replace("минут", "")
            .replace("мин.", "")
            .replace("мин", "")
            .replace('—', '–')
            .replace('-', '–')
            .trim()
        val match = Regex("""(\d{1,3})\s*(?:–|до)\s*(\d{1,3})""").find(normalized) ?: return null
        val start = match.groupValues[1].toIntOrNull() ?: return null
        val end = match.groupValues[2].toIntOrNull() ?: return null
        return start to end
    }

    private fun detectIndependentClasses(
        independentText: String,
        class1Activity: String,
        class2Activity: String
    ): Set<Int> {
        val text = independentText.lowercase().replace('ё', 'е')
        if (text.contains("никто") || text == "-" || text == "—" || text.contains("нет")) return emptySet()

        val result = mutableSetOf<Int>()
        if (text.contains("оба") || text.contains("оба класса") || text.contains("1 и 2")) {
            result += 1
            result += 2
        }
        if (Regex("""(?:класс\s*1|1\s*класс)""").containsMatchIn(text)) result += 1
        if (Regex("""(?:класс\s*2|2\s*класс)""").containsMatchIn(text)) result += 2

        // Fallback if Gemini leaves the explicit column empty but labels the activity itself.
        if (result.isEmpty()) {
            if (class1Activity.contains("самостоят", ignoreCase = true)) result += 1
            if (class2Activity.contains("самостоят", ignoreCase = true)) result += 2
        }
        return result
    }

    private fun looksLikeTableRow(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("|") && trimmed.count { it == '|' } >= 2
    }

    private fun splitTableRow(line: String): List<String> =
        line.trim().trim('|').split('|').map { it.trim() }

    private fun isSeparatorRow(line: String): Boolean {
        if (!looksLikeTableRow(line)) return false
        return splitTableRow(line).all { cell ->
            val compact = cell.replace(" ", "")
            compact.isNotEmpty() && compact.all { it == '-' || it == ':' }
        }
    }

    private fun normalizeHeader(value: String): String = value
        .lowercase()
        .replace("**", "")
        .replace("__", "")
        .replace('ё', 'е')
        .trim()
}
