package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LessonPlanEntity
import com.example.data.local.PlannerDatabase
import com.example.data.local.PlannerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class LessonPlanListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PlannerRepository
    private val _plans = MutableStateFlow<List<LessonPlanEntity>>(emptyList())
    val plans: StateFlow<List<LessonPlanEntity>> = _plans.asStateFlow()
    private var loadJob: Job? = null

    init {
        val plannerDao = PlannerDatabase.getDatabase(application).plannerDao()
        repository = PlannerRepository(plannerDao)
    }

    fun loadPlans(folderId: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getPlansForFolder(folderId).collect {
                _plans.value = it
            }
        }
    }

    fun loadFavoritePlans() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getFavoritePlans().collect {
                _plans.value = it
            }
        }
    }

    fun toggleFavorite(planId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.updateFavoriteStatus(planId, isFavorite)
        }
    }

    fun deletePlan(planId: Long) {
        viewModelScope.launch {
            repository.deletePlan(planId)
        }
    }
    
    fun renamePlan(planId: Long, newTitle: String) {
        viewModelScope.launch {
            if (newTitle.isNotBlank()) {
                repository.updatePlanTitle(planId, newTitle)
            }
        }
    }
    
    fun updatePlanContent(planId: Long, newContent: String) {
        viewModelScope.launch {
            repository.updatePlanContent(planId, newContent)
        }
    }
}
