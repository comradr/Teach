package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.FolderEntity
import com.example.data.local.LessonPlanEntity
import com.example.ui.FavoritesScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPlansScreen(
    folderId: Long,
    viewModel: LessonPlanListViewModel = viewModel(),
    navController: NavHostController
) {
    LaunchedEffect(folderId) {
        viewModel.loadPlans(folderId)
    }
    val plans by viewModel.plans.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf(PlanSort.Newest) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var planToDelete by remember { mutableStateOf<Long?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var planToRename by remember { mutableStateOf<com.example.data.local.LessonPlanEntity?>(null) }
    var renamePlanTitle by remember { mutableStateOf("") }
    var renamePlanTags by remember { mutableStateOf("") }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val filteredPlans = plans.filter { it.title.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true) || it.tags.contains(searchQuery, ignoreCase = true) }
    val displayedPlans = remember(filteredPlans, selectedSort) { sortPlans(filteredPlans, selectedSort) }
    val df = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    
    if (planToRename != null) {
        AlertDialog(
            onDismissRequest = { planToRename = null },
            title = { Text("Свойства плана") },
            text = {
                Column {
                    OutlinedTextField(
                        value = renamePlanTitle,
                        onValueChange = { renamePlanTitle = it },
                        label = { Text("Название плана") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renamePlanTags,
                        onValueChange = { renamePlanTags = it },
                        label = { Text("Метки (через запятую)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (renamePlanTitle.isNotBlank()) {
                        viewModel.renamePlan(planToRename!!.id, renamePlanTitle)
                        val db = com.example.data.local.PlannerDatabase.getDatabase(context)
                        coroutineScope.launch {
                            db.plannerDao().updatePlanTags(planToRename!!.id, renamePlanTags)
                        }
                        planToRename = null
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { planToRename = null }) { Text("Отмена") }
            }
        )
    }
    if (planToDelete != null) {
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            title = { Text("Удалить план?") },
            text = { Text("Вы уверены, что хотите безвозвратно удалить этот план урока?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deletePlan(planToDelete!!)
                    planToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) { Text("Отмена") }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        com.example.ui.components.SoftCard(
            containerColor = com.example.ui.theme.PlannerColors.LavenderSoft
        ) {
            com.example.ui.components.SectionTitle(
                title = "Планы уроков",
                subtitle = "Поиск по названиям, тексту и меткам"
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Найти план") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Поиск") }
            )
            Box {
                IconButton(onClick = { sortMenuExpanded = true }) {
                    Icon(Icons.Default.Sort, contentDescription = "Сортировка: ${selectedSort.label}")
                }
                DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                    PlanSort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = { selectedSort = option; sortMenuExpanded = false },
                            leadingIcon = {
                                if (selectedSort == option) Icon(Icons.Default.Check, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }
        Text(selectedSort.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (plans.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    com.example.ui.components.mascot.PlannerMascot(
                        com.example.ui.components.mascot.MascotState.PlansEmpty,
                        modifier = Modifier.size(142.dp)
                    )
                    Text(
                        "В этой папке пока нет планов",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Сохранённые планы появятся здесь",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (filteredPlans.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    com.example.ui.components.mascot.PlannerMascot(
                        com.example.ui.components.mascot.MascotState.Guide,
                        modifier = Modifier.size(120.dp)
                    )
                    Text("По этому запросу ничего не найдено")
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(displayedPlans, key = { it.id }) { plan ->
                    var menuExpanded by remember(plan.id) { mutableStateOf(false) }
                    Box {
                        com.example.ui.components.PlanCard(
                            plan = plan,
                            onOpen = { navController.navigate("plan_detail/${plan.id}") },
                            onToggleFavorite = {
                                viewModel.toggleFavorite(plan.id, !plan.isFavorite)
                            },
                            onMore = { menuExpanded = true }
                        )
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Свойства") },
                                onClick = {
                                    menuExpanded = false
                                    planToRename = plan
                                    renamePlanTitle = plan.title
                                    renamePlanTags = plan.tags
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Удалить") },
                                onClick = {
                                    menuExpanded = false
                                    planToDelete = plan.id
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
