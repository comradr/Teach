package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LessonTemplateEntity
import com.example.data.local.PlannerDatabase
import com.example.data.local.PlannerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LessonTemplateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PlannerRepository(
        PlannerDatabase.getDatabase(application).plannerDao()
    )

    val templates: StateFlow<List<LessonTemplateEntity>> = repository.allTemplates.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun save(template: LessonTemplateEntity) {
        if (template.name.isBlank()) return
        viewModelScope.launch { repository.upsertTemplate(template) }
    }

    fun delete(template: LessonTemplateEntity) {
        viewModelScope.launch { repository.deleteTemplate(template) }
    }
}
