package com.example.data.local

import kotlinx.coroutines.flow.Flow

class PlannerRepository(private val plannerDao: PlannerDao) {

    val allFolders: Flow<List<FolderEntity>> = plannerDao.getAllFolders()
    val archivedFolders: Flow<List<FolderEntity>> = plannerDao.getArchivedFolders()
    val allTemplates: Flow<List<LessonTemplateEntity>> = plannerDao.getAllTemplates()
    val folderPlanCounts: Flow<List<FolderPlanCount>> = plannerDao.getFolderPlanCounts()

    suspend fun insertFolder(name: String): Long {
        return plannerDao.insertFolder(FolderEntity(name = name))
    }
    
    suspend fun updateFolderArchiveStatus(folderId: Long, isArchived: Boolean) {
        plannerDao.updateFolderArchiveStatus(folderId, isArchived)
    }

    suspend fun getFolderByName(name: String): FolderEntity? {
        return plannerDao.getFolderByName(name)
    }

    suspend fun insertLessonPlan(folderId: Long, title: String, content: String, durationMinutes: Int = 45): Long {
        return plannerDao.insertLessonPlan(
            LessonPlanEntity(
                folderId = folderId,
                title = title,
                content = content,
                durationMinutes = durationMinutes
            )
        )
    }

    fun getPlansForFolder(folderId: Long): Flow<List<LessonPlanEntity>> {
        return plannerDao.getPlansForFolder(folderId)
    }

    fun searchAllPlans(query: String): Flow<List<PlanSearchResult>> = plannerDao.searchAllPlans(query.trim())

    suspend fun getPlanById(planId: Long): LessonPlanEntity? {
        return plannerDao.getPlanById(planId)
    }

    suspend fun updateFavoriteStatus(planId: Long, isFavorite: Boolean) {
        plannerDao.updateFavoriteStatus(planId, isFavorite)
    }
    
    suspend fun updatePlanTags(planId: Long, tags: String) {
        plannerDao.updatePlanTags(planId, tags)
    }

    fun getFavoritePlans(): Flow<List<LessonPlanEntity>> {
        return plannerDao.getFavoritePlans()
    }

    
    suspend fun updateFolderName(folderId: Long, name: String) {
        plannerDao.updateFolderName(folderId, name)
    }

    suspend fun updatePlanTitle(planId: Long, title: String) {
        plannerDao.updatePlanTitle(planId, title)
    }

    suspend fun updatePlanContent(planId: Long, content: String) {
        plannerDao.updatePlanContent(planId, content)
    }

    suspend fun deletePlan(planId: Long)
 {
        plannerDao.deletePlan(planId)
    }

    suspend fun deleteFolder(folderId: Long) {
        plannerDao.deleteFolder(folderId)
    }

    suspend fun upsertTemplate(template: LessonTemplateEntity): Long =
        plannerDao.upsertTemplate(template.copy(name = template.name.trim(), updatedAt = System.currentTimeMillis()))

    suspend fun deleteTemplate(template: LessonTemplateEntity) {
        plannerDao.deleteTemplate(template)
    }
}
