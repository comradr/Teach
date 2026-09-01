package com.example.data.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.withTransaction

@Serializable
data class BackupData(
    val folders: List<FolderData>,
    val plans: List<PlanData>,
    val templates: List<TemplateData> = emptyList()
)

@Serializable
data class FolderData(
    val oldId: Long,
    val name: String,
    val isArchived: Boolean
)

@Serializable
data class PlanData(
    val oldFolderId: Long,
    val title: String,
    val content: String,
    val isFavorite: Boolean,
    val tags: String,
    val timestamp: Long,
    val durationMinutes: Int = 45
)

@Serializable
data class TemplateData(
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
    val lessonType: String = "Комбинированный"
)

class BackupManager(private val context: Context, private val database: PlannerDatabase) {
    suspend fun createBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val backup = database.withTransaction {
                val dao = database.plannerDao()
                val folders = dao.getAllFoldersSnapshot().map { FolderData(it.id, it.name, it.isArchived) }
                val plans = dao.getAllPlansSnapshot().map { plan ->
                    PlanData(plan.folderId, plan.title, plan.content, plan.isFavorite, plan.tags, plan.timestamp, plan.durationMinutes)
                }
                val templates = dao.getAllTemplates().first().map { template ->
                    TemplateData(
                        template.name, template.useSecondClass,
                        template.classASubject, template.classAGrade, template.classATopic,
                        template.classBSubject, template.classBGrade, template.classBTopic,
                        template.classAIndependentWorkLimit, template.classBIndependentWorkLimit,
                        template.planMode, template.additionalInstructions, template.lessonDuration, template.lessonType
                    )
                }
                BackupData(folders, plans, templates)
            }
            val json = Json.encodeToString(backup)

            context.contentResolver.openOutputStream(uri)?.use {
                it.write(json.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use {
                val reader = BufferedReader(InputStreamReader(it))
                reader.readText()
            } ?: return@withContext false

            val parser = Json { this.ignoreUnknownKeys = true }
            val backup = parser.decodeFromString<BackupData>(json)

            database.withTransaction {
                val dao = database.plannerDao()
                val folderIdMap = mutableMapOf<Long, Long>()

                for (folder in backup.folders) {
                    val existing = dao.getFolderByName(folder.name)
                    val newId = if (existing != null) {
                        if (existing.isArchived != folder.isArchived) {
                            dao.updateFolderArchiveStatus(existing.id, folder.isArchived)
                        }
                        existing.id
                    } else {
                        dao.insertFolder(FolderEntity(name = folder.name, isArchived = folder.isArchived))
                    }
                    folderIdMap[folder.oldId] = newId
                }

                for (plan in backup.plans) {
                    val newFolderId = folderIdMap[plan.oldFolderId]
                    if (newFolderId != null && !dao.planExists(newFolderId, plan.title, plan.timestamp)) {
                        val entity = LessonPlanEntity(
                            folderId = newFolderId,
                            title = plan.title,
                            content = plan.content,
                            isFavorite = plan.isFavorite,
                            tags = plan.tags,
                            timestamp = plan.timestamp,
                            durationMinutes = plan.durationMinutes
                        )
                        dao.insertLessonPlan(entity)
                    }
                }

                for (template in backup.templates) {
                    dao.upsertTemplate(LessonTemplateEntity(
                        name = template.name,
                        useSecondClass = template.useSecondClass,
                        classASubject = template.classASubject,
                        classAGrade = template.classAGrade,
                        classATopic = template.classATopic,
                        classBSubject = template.classBSubject,
                        classBGrade = template.classBGrade,
                        classBTopic = template.classBTopic,
                        classAIndependentWorkLimit = template.classAIndependentWorkLimit,
                        classBIndependentWorkLimit = template.classBIndependentWorkLimit,
                        planMode = template.planMode,
                        additionalInstructions = template.additionalInstructions,
                        lessonDuration = template.lessonDuration,
                        lessonType = template.lessonType
                    ))
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
