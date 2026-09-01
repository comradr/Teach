package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class DraftRestoreTest {
    @Test
    fun testDraftRestoreLogic() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = com.example.LessonPlannerViewModel(app)
        
        val draft = LessonDraft(generatedPlan = "TEST PLAN")
        
        viewModel.updateGeneratedPlan(draft.generatedPlan)
        
        val uiState = viewModel.uiState.value
        assertTrue(uiState is PlannerUiState.Success)
        assertEquals("TEST PLAN", (uiState as PlannerUiState.Success).resultText)
        
        viewModel.resetState()
        assertTrue(viewModel.uiState.value is PlannerUiState.Idle)
    }
}
