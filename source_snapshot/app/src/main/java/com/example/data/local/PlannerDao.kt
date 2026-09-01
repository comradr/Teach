package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {
    @Insert
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("SELECT * FROM folders WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>
    
    @Query("SELECT * FROM folders WHERE isArchived = 1 ORDER BY name ASC")
    fun getArchivedFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY name ASC")
    suspend fun getAllFoldersSnapshot(): List<FolderEntity>

    @Query("SELECT folderId, COUNT(*) AS planCount FROM lesson_plans GROUP BY folderId")
    fun getFolderPlanCounts(): Flow<List<FolderPlanCount>>
    
    @Query("UPDATE folders SET isArchived = :isArchived WHERE id = :folderId")
    suspend fun updateFolderArchiveStatus(folderId: Long, isArchived: Boolean)

    @Query("SELECT * FROM folders WHERE name = :name LIMIT 1")
    suspend fun getFolderByName(name: String): FolderEntity?

    @Insert
    suspend fun insertLessonPlan(plan: LessonPlanEntity): Long

    @Query("SELECT * FROM lesson_plans WHERE folderId = :folderId ORDER BY timestamp DESC")
    fun getPlansForFolder(folderId: Long): Flow<List<LessonPlanEntity>>

    @Query("SELECT * FROM lesson_plans ORDER BY timestamp DESC")
    suspend fun getAllPlansSnapshot(): List<LessonPlanEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM lesson_plans WHERE folderId = :folderId AND title = :title AND timestamp = :timestamp)")
    suspend fun planExists(folderId: Long, title: String, timestamp: Long): Boolean

    @Query("""
        SELECT p.*, f.name AS folderName
        FROM lesson_plans p
        JOIN folders f ON f.id = p.folderId
        WHERE p.title LIKE '%' || :query || '%' COLLATE NOCASE
           OR p.content LIKE '%' || :query || '%' COLLATE NOCASE
           OR p.tags LIKE '%' || :query || '%' COLLATE NOCASE
           OR f.name LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY p.timestamp DESC
    """)
    fun searchAllPlans(query: String): Flow<List<PlanSearchResult>>

    @Query("SELECT * FROM lesson_plans WHERE id = :planId")
    suspend fun getPlanById(planId: Long): LessonPlanEntity?
    
    @Query("UPDATE lesson_plans SET isFavorite = :isFavorite WHERE id = :planId")
    suspend fun updateFavoriteStatus(planId: Long, isFavorite: Boolean)
    
    @Query("UPDATE lesson_plans SET content = :content WHERE id = :planId")
    suspend fun updatePlanContent(planId: Long, content: String)
    
    @Query("UPDATE lesson_plans SET tags = :tags WHERE id = :planId")
    suspend fun updatePlanTags(planId: Long, tags: String)
    
    @Query("SELECT * FROM lesson_plans WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoritePlans(): Flow<List<LessonPlanEntity>>
    
    @Query("DELETE FROM lesson_plans WHERE id = :planId")
    suspend fun deletePlan(planId: Long)
    
    
    @Query("UPDATE folders SET name = :name WHERE id = :folderId")
    suspend fun updateFolderName(folderId: Long, name: String)

    @Query("UPDATE lesson_plans SET title = :title WHERE id = :planId")
    suspend fun updatePlanTitle(planId: Long, title: String)
    
    @Query("DELETE FROM folders WHERE id = :folderId")

    suspend fun deleteFolder(folderId: Long)

    @Query("SELECT * FROM lesson_templates ORDER BY updatedAt DESC, name ASC")
    fun getAllTemplates(): Flow<List<LessonTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(template: LessonTemplateEntity): Long

    @Delete
    suspend fun deleteTemplate(template: LessonTemplateEntity)
}
