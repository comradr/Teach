package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isArchived: Boolean = false
)

@Entity(
    tableName = "lesson_plans",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("folderId")]
)
data class LessonPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long,
    val title: String,
    val content: String,
    val isFavorite: Boolean = false,
    val tags: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 45
)

@Entity(
    tableName = "lesson_templates",
    indices = [Index(value = ["name"], unique = true)]
)
data class LessonTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val useSecondClass: Boolean = false,
    val classASubject: String = "",
    val classAGrade: String = "",
    val classATopic: String = "",
    val classBSubject: String = "",
    val classBGrade: String = "",
    val classBTopic: String = "",
    val classAIndependentWorkLimit: String = "15",
    val classBIndependentWorkLimit: String = "15",
    val planMode: String = "Для себя",
    val additionalInstructions: String = "",
    val lessonDuration: String = "45",
    val lessonType: String = "Комбинированный",
    val updatedAt: Long = System.currentTimeMillis()
)

data class FolderPlanCount(
    val folderId: Long,
    val planCount: Int
)

data class PlanSearchResult(
    val id: Long,
    val folderId: Long,
    val title: String,
    val content: String,
    val isFavorite: Boolean,
    val tags: String,
    val timestamp: Long,
    val durationMinutes: Int,
    val folderName: String
) {
    fun asEntity() = LessonPlanEntity(id, folderId, title, content, isFavorite, tags, timestamp, durationMinutes)
}
