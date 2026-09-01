package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PlannerDatabase
import com.example.data.local.PlannerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed class PlannerUiState {
    object Idle : PlannerUiState()
    object Loading : PlannerUiState()
    data class Success(val resultText: String, val warnings: List<String> = emptyList()) : PlannerUiState()
    data class Error(val message: String) : PlannerUiState()
    data class Saved(val planId: Long) : PlannerUiState()
}

class LessonPlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PlannerRepository
    private val _uiState = MutableStateFlow<PlannerUiState>(PlannerUiState.Idle)
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init {
        val plannerDao = PlannerDatabase.getDatabase(application).plannerDao()
        repository = PlannerRepository(plannerDao)
    }
    
    fun generateLessonPlan(
        context: android.content.Context,
        useSecondClass: Boolean,
        classASubject: String,
        classAGrade: String,
        classATopic: String,
        classALimit: String,
        classBSubject: String,
        classBGrade: String,
        classBTopic: String,
        classBLimit: String,
        planMode: String = "Для себя",
        lessonType: String = "Комбинированный",
        classAImages: List<DraftLessonImage> = emptyList(),
        classBImages: List<DraftLessonImage> = emptyList(),
        duration: Int = 45,
        additionalInstructions: String = ""
    ) {
        if (classASubject.isBlank() || (useSecondClass && classBSubject.isBlank())) {
            _uiState.value = PlannerUiState.Error("Пожалуйста, выберите предметы.")
            return
        }

        val hasValidImageA = classAImages.any { File(it.path).let { file -> file.exists() && file.length() > 0L } }
        val hasValidImageB = classBImages.any { File(it.path).let { file -> file.exists() && file.length() > 0L } }
        val hasMaterialA = classATopic.isNotBlank() || hasValidImageA
        val hasMaterialB = classBTopic.isNotBlank() || hasValidImageB

        if (!useSecondClass && !hasMaterialA) {
            _uiState.value = PlannerUiState.Error("Пожалуйста, заполните тему урока или прикрепите фото учебника.")
            return
        }
        
        if (useSecondClass && (!hasMaterialA || !hasMaterialB)) {
            _uiState.value = PlannerUiState.Error("Пожалуйста, заполните темы уроков для обоих классов или прикрепите фото.")
            return
        }

        if (duration <= 0) {
            _uiState.value = PlannerUiState.Error("Длительность урока должна быть больше 0 минут.")
            return
        }

        if (useSecondClass) {
            fun validLimit(value: String): Boolean {
                if (value.isBlank()) return true
                val minutes = value.toIntOrNull() ?: return false
                return minutes in 1..duration
            }
            if (!validLimit(classALimit) || !validLimit(classBLimit)) {
                _uiState.value = PlannerUiState.Error("Максимум самостоятельной работы должен быть числом от 1 до длительности урока.")
                return
            }
        }

        _history.value = emptyList()
        viewModelScope.launch {
            _uiState.value = PlannerUiState.Loading

            val allImages = (if (useSecondClass) classAImages + classBImages else classAImages)
                .filter { File(it.path).let { file -> file.exists() && file.length() > 0L } }
            if (allImages.isNotEmpty()) {
                val ok = withContext(Dispatchers.IO) {
                    ImageOptimizer.ensureBudget(context, allImages)
                }
                if (!ok) {
                    _uiState.value = PlannerUiState.Error("Не удалось подготовить фотографии. Попробуйте удалить одно из изображений.")
                    return@launch
                }
            }

            val systemPrompt = LessonPromptBuilder.buildLessonSystemPrompt(useSecondClass)
            val userPrompt = LessonPromptBuilder.buildLessonUserPrompt(
                useSecondClass, classASubject, classAGrade, classATopic, classALimit, 
                classBSubject, classBGrade, classBTopic, classBLimit, planMode, lessonType, duration, additionalInstructions
            )

            val parts = withContext(Dispatchers.IO) {
                GeminiLessonPartsBuilder.buildParts(useSecondClass, classAImages, classBImages, userPrompt)
            }

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = parts)),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
            )

            val apiKey = GeminiApiKeyProvider.get(getApplication())
            if (!GeminiApiKeyProvider.isUsable(apiKey)) {
                _uiState.value = PlannerUiState.Error("Ключ Gemini не настроен. Нажмите на верхнего маскота и сохраните личный API-ключ.")
                return@launch
            }

            val hasImages = classAImages.isNotEmpty() || (useSecondClass && classBImages.isNotEmpty())
            when (val result = executeGeminiRequest(apiKey, request, hasImages)) {
                is GeminiExtraResult.Error -> _uiState.value = PlannerUiState.Error(result.message)
                is GeminiExtraResult.Success -> {
                    val response = result.text
                    val classALimitMinutes = classALimit.toIntOrNull()
                    val classBLimitMinutes = classBLimit.toIntOrNull()
                    val validation = LessonPlanValidator.validate(
                        markdown = response,
                        useSecondClass = useSecondClass,
                        lessonDuration = duration,
                        classALimit = classALimitMinutes,
                        classBLimit = classBLimitMinutes
                    )

                    if (validation.isValid) {
                        _uiState.value = PlannerUiState.Success(response)
                    } else {
                        val repaired = tryRepairGeneratedPlan(
                            originalPlan = response,
                            issues = validation.issues,
                            useSecondClass = useSecondClass,
                            duration = duration,
                            classALimit = classALimitMinutes,
                            classBLimit = classBLimitMinutes,
                            apiKey = apiKey
                        )
                        _uiState.value = repaired
                    }
                }
            }
        }
    }
    
    private suspend fun tryRepairGeneratedPlan(
        originalPlan: String,
        issues: List<String>,
        useSecondClass: Boolean,
        duration: Int,
        classALimit: Int?,
        classBLimit: Int?,
        apiKey: String
    ): PlannerUiState.Success {
        val issueList = issues.joinToString("\n") { "- $it" }
        val modeRules = if (useSecondClass) {
            buildString {
                appendLine("- временные интервалы непрерывные, без пересечений и разрывов;")
                appendLine("- ${if (classALimit != null) "Класс 1 не остаётся самостоятельно более $classALimit минут подряд" else "для Класса 1 сохраняй разумные короткие самостоятельные блоки"};")
                appendLine("- ${if (classBLimit != null) "Класс 2 не остаётся самостоятельно более $classBLimit минут подряд" else "для Класса 2 сохраняй разумные короткие самостоятельные блоки"};")
                append("- сохрани таблицу `| Время | Действия учителя | Класс 1 | Класс 2 | Кто работает самостоятельно |`;")
            }
        } else {
            "- это урок ОДНОГО класса: не добавляй второй класс и схему МКШ;"
        }
        val repairPrompt = """
            Проверь и исправь готовый план урока. Не придумывай новый урок с нуля и не меняй без необходимости темы, задания и фактическое содержание.

            Автоматическая проверка обнаружила:
            $issueList

            Требования к исправлению:
            - итоговая длительность урока ровно $duration минут;
            $modeRules
            - обязательно сохрани разделы `## Карточка — ученику` и `## Ответы учителю`;
            - верни ПОЛНЫЙ исправленный план в Markdown, без комментариев до и после него.

            Исходный план:
            $originalPlan
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = repairPrompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "Ты методист-корректор. Исправляй только обнаруженные логические и структурные нарушения плана.")))
        )

        return when (val result = executeGeminiRequest(apiKey, request)) {
            is GeminiExtraResult.Error -> {
                PlannerUiState.Success(originalPlan, issues.map { "Автопроверка: $it" })
            }
            is GeminiExtraResult.Success -> {
                val repaired = result.text
                val secondValidation = LessonPlanValidator.validate(
                    markdown = repaired,
                    useSecondClass = useSecondClass,
                    lessonDuration = duration,
                    classALimit = classALimit,
                    classBLimit = classBLimit
                )
                if (secondValidation.isValid) {
                    PlannerUiState.Success(repaired)
                } else {
                    PlannerUiState.Success(repaired, secondValidation.issues.map { "Автопроверка: $it" })
                }
            }
        }
    }

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    fun regenerateSection(
        currentPlan: String,
        sectionTitle: String,
        instructions: String,
        useSecondClass: Boolean
    ) {
        val section = findMarkdownSection(currentPlan, sectionTitle)
        if (section == null) {
            _uiState.value = PlannerUiState.Error("Раздел «$sectionTitle» не найден. Введите название существующего заголовка плана.")
            return
        }

        viewModelScope.launch {
            _uiState.value = PlannerUiState.Loading
            
            val prompt = """
                Перепиши только один фрагмент плана урока.
                Фрагмент: "$sectionTitle"
                
                Текущий текст фрагмента:
                ${section.text}
                
                Пожелания учителя: $instructions
                
                Режим урока: ${if (useSecondClass) "МКШ (два разных класса)" else "Один класс. НЕ добавляй задания для второго класса!"}
                
                Верни ТОЛЬКО новый текст этого фрагмента в формате Markdown, без остального плана и без вступлений.
                Сохрани исходный Markdown-заголовок фрагмента первой строкой.
                """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = Content(parts = listOf(Part(text = "Ты опытный методист. Возвращай только изменённый фрагмент.")))
            )

            val apiKey = GeminiApiKeyProvider.get(getApplication())
            if (!GeminiApiKeyProvider.isUsable(apiKey)) {
                _uiState.value = PlannerUiState.Error("Ключ Gemini не настроен. Нажмите на верхнего маскота и сохраните личный API-ключ.")
                return@launch
            }

            when (val result = executeGeminiRequest(apiKey, request)) {
                is GeminiExtraResult.Error -> _uiState.value = PlannerUiState.Error(result.message)
                is GeminiExtraResult.Success -> {
                    val response = result.text
                    val newHistory = _history.value.toMutableList()
                    newHistory.add(currentPlan)
                    _history.value = newHistory
                    
                    val newPlan = replaceMarkdownSection(currentPlan, section, response)
                    _uiState.value = PlannerUiState.Success(newPlan)
                }
            }
        }
    }
    
    fun undoLastChange() {
        if (_history.value.isNotEmpty()) {
            val last = _history.value.last()
            _history.value = _history.value.dropLast(1)
            _uiState.value = PlannerUiState.Success(last)
        }
    }

    fun updateGeneratedPlan(newText: String) {
        _uiState.value = PlannerUiState.Success(newText)
    }

    fun saveLessonPlan(folderId: Long, title: String, content: String, durationMinutes: Int = 45) {
        viewModelScope.launch {
            try {
                val id = repository.insertLessonPlan(folderId, title, content, durationMinutes)
                _uiState.value = PlannerUiState.Saved(id)
            } catch(e: Exception) {
                _uiState.value = PlannerUiState.Error("Не удалось сохранить план. Проверьте свободное место и повторите попытку.")
            }
        }
    }

    fun resetState() {
        _history.value = emptyList()
        _uiState.value = PlannerUiState.Idle
    }
}
