package com.example

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LessonDraft(
    val useSecondClass: Boolean = false,
    val classASubject: String = "",
    val classAGrade: String = "",
    val classATopic: String = "",
    val classBSubject: String = "",
    val classBGrade: String = "",
    val classBTopic: String = "",
    val classAIndependentWorkLimit: String = "",
    val classBIndependentWorkLimit: String = "",
    val lessonDuration: String = "45",
    val additionalInstructions: String = "",
    val attachedImages: List<Pair<String, String>> = emptyList(), // For backwards compatibility
    val classAImages: List<DraftLessonImage> = emptyList(),
    val classBImages: List<DraftLessonImage> = emptyList(),
    val generatedPlan: String = "",
    val planMode: String = "Для себя",
    val lessonType: String = "Комбинированный"
)

class LessonDraftStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lesson_draft", Context.MODE_PRIVATE)
    
    fun saveDraft(draft: LessonDraft) {
        val json = Json.encodeToString(draft)
        prefs.edit().putString("draft_data", json).apply()
    }

    fun getDraft(): LessonDraft {
        val json = prefs.getString("draft_data", null)
        return if (json != null) {
            try {
                // Ignore unknown keys is needed to not crash on old draft
                val jsonParser = Json { ignoreUnknownKeys = true }
                jsonParser.decodeFromString<LessonDraft>(json)
            } catch (e: Exception) {
                LessonDraft()
            }
        } else {
            LessonDraft()
        }
    }

    fun clearDraft() {
        prefs.edit().remove("draft_data").apply()
    }
}
