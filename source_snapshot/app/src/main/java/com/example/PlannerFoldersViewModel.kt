package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FolderEntity
import com.example.data.local.LessonPlanEntity
import com.example.data.local.PlanSearchResult
import com.example.data.local.PlannerDatabase
import com.example.data.local.PlannerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class PlannerFoldersViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PlannerRepository

    val allFolders: StateFlow<List<FolderEntity>>
    val archivedFolders: StateFlow<List<FolderEntity>>
    val folderPlanCounts: StateFlow<Map<Long, Int>>
    private val _globalSearchResults = MutableStateFlow<List<PlanSearchResult>>(emptyList())
    val globalSearchResults = _globalSearchResults.asStateFlow()
    private var searchJob: Job? = null

    init {
        val plannerDao = PlannerDatabase.getDatabase(application).plannerDao()
        repository = PlannerRepository(plannerDao)
        allFolders = repository.allFolders.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        archivedFolders = repository.archivedFolders.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        folderPlanCounts = repository.folderPlanCounts
            .map { counts -> counts.associate { it.folderId to it.planCount } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    }

    fun searchGlobally(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _globalSearchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            repository.searchAllPlans(query).collect { _globalSearchResults.value = it }
        }
    }

    fun addFolder(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) {
            val existing = repository.getFolderByName(name)
            if (existing == null) {
                repository.insertFolder(name)
            }
        }
    }

    fun deleteFolder(folderId: Long) = viewModelScope.launch {
        repository.deleteFolder(folderId)
    }
    
    fun archiveFolder(folderId: Long, isArchived: Boolean) = viewModelScope.launch {
        repository.updateFolderArchiveStatus(folderId, isArchived)
    }
    
    fun renameFolder(folderId: Long, newName: String) = viewModelScope.launch {
        if (newName.isNotBlank()) {
            repository.updateFolderName(folderId, newName)
        }
    }

    fun toggleFavorite(planId: Long, isFavorite: Boolean) = viewModelScope.launch {
        repository.updateFavoriteStatus(planId, isFavorite)
    }
}
