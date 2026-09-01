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
fun FoldersScreen(
    viewModel: PlannerFoldersViewModel = viewModel(),
    navController: NavHostController
) {
    val folders by viewModel.allFolders.collectAsState()
    val archivedFolders by viewModel.archivedFolders.collectAsState()
    val folderPlanCounts by viewModel.folderPlanCounts.collectAsState()
    val globalSearchResults by viewModel.globalSearchResults.collectAsState()
    var globalSearchQuery by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var folderToDelete by remember { mutableStateOf<Long?>(null) }
    var folderToRename by remember { mutableStateOf<com.example.data.local.FolderEntity?>(null) }
    var renameFolderName by remember { mutableStateOf("") }
    var showArchived by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(globalSearchQuery) {
        kotlinx.coroutines.delay(250)
        viewModel.searchGlobally(globalSearchQuery)
    }
    
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val db = com.example.data.local.PlannerDatabase.getDatabase(context)
                val bm = com.example.data.local.BackupManager(context, db)
                val success = bm.createBackup(it)
                showMessage = if (success) "Резервная копия создана" else "Ошибка при создании"
            }
        }
    }
    
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val db = com.example.data.local.PlannerDatabase.getDatabase(context)
                val bm = com.example.data.local.BackupManager(context, db)
                val success = bm.restoreBackup(it)
                showMessage = if (success) "Данные успешно восстановлены" else "Ошибка при восстановлении"
            }
        }
    }

    
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Новая папка") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Название папки") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFolderName.isNotBlank()) {
                        viewModel.addFolder(newFolderName)
                        newFolderName = ""
                        showAddDialog = false
                    }
                }) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showMessage != null) {
        AlertDialog(
            onDismissRequest = { showMessage = null },
            title = { Text("Информация") },
            text = { Text(showMessage!!) },
            confirmButton = {
                Button(onClick = { showMessage = null }) { Text("ОК") }
            }
        )
    }

    if (folderToRename != null) {
        AlertDialog(
            onDismissRequest = { folderToRename = null },
            title = { Text("Переименовать папку") },
            text = {
                OutlinedTextField(
                    value = renameFolderName,
                    onValueChange = { renameFolderName = it },
                    label = { Text("Новое название") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameFolderName.isNotBlank()) {
                        viewModel.renameFolder(folderToRename!!.id, renameFolderName)
                        folderToRename = null
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { folderToRename = null }) { Text("Отмена") }
            }
        )
    }
    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Удалить папку?") },
            text = { Text("Вы уверены, что хотите удалить эту папку и все планы в ней?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteFolder(folderToDelete!!)
                    folderToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) { Text("Отмена") }
            }
        )
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                text = { Text("Новая папка") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var showBackupMenu by remember { mutableStateOf(false) }
            com.example.ui.components.SoftCard(
                containerColor = com.example.ui.theme.PlannerColors.LavenderSoft
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    com.example.ui.components.SectionTitle(
                        title = if (showArchived) "Архив папок" else "Мои папки",
                        subtitle = if (showArchived) "Здесь хранятся архивированные папки" else "Организуйте планы так, как удобно",
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        IconButton(onClick = { showBackupMenu = true }) {
                            Icon(Icons.Default.CloudSync, contentDescription = "Резервное копирование")
                        }
                        DropdownMenu(
                            expanded = showBackupMenu,
                            onDismissRequest = { showBackupMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Создать резервную копию") },
                                onClick = {
                                    showBackupMenu = false
                                    exportLauncher.launch("lesson_planner_backup.json")
                                },
                                leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Восстановить из копии") },
                                onClick = {
                                    showBackupMenu = false
                                    importLauncher.launch("application/json")
                                },
                                leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) }
                            )
                        }
                    }
                }
                FilledTonalButton(onClick = { showArchived = !showArchived }) {
                    Icon(
                        if (showArchived) Icons.Default.FolderOpen else Icons.Default.Inventory2,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (showArchived) "Вернуться к папкам" else "Показать архив")
                }
            }

            OutlinedTextField(
                value = globalSearchQuery,
                onValueChange = { globalSearchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (globalSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { globalSearchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Очистить поиск")
                        }
                    }
                },
                placeholder = { Text("Искать во всех папках") }
            )

            if (globalSearchQuery.isNotBlank()) {
                if (globalSearchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("По этому запросу планов не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(globalSearchResults, key = { it.id }) { result ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Папка: ${result.folderName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                com.example.ui.components.PlanCard(
                                    plan = result.asEntity(),
                                    onOpen = { navController.navigate("plan_detail/${result.id}") },
                                    onToggleFavorite = { viewModel.toggleFavorite(result.id, !result.isFavorite) },
                                    onMore = { navController.navigate("plan_detail/${result.id}") }
                                )
                            }
                        }
                    }
                }
            } else {
                val currentFolders = if (showArchived) archivedFolders else folders
                if (currentFolders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.example.ui.components.SoftCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            com.example.ui.components.mascot.PlannerMascot(
                                if (showArchived) com.example.ui.components.mascot.MascotState.Empty
                                else com.example.ui.components.mascot.MascotState.FoldersEmpty,
                                modifier = Modifier.size(136.dp)
                            )
                            Text(
                                if (showArchived) "Архив пока пуст" else "Здесь пока нет папок",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (showArchived) "Архивированные папки появятся здесь" else "Создайте первую папку для своих планов",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(currentFolders, key = { it.id }) { folder ->
                        var menuExpanded by remember(folder.id) { mutableStateOf(false) }
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                navController.navigate("folder_plans/${folder.id}")
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        folder.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        buildString {
                                            append(formatPlanCount(folderPlanCounts[folder.id] ?: 0))
                                            if (showArchived) append(" · в архиве")
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Действия с папкой")
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Переименовать") },
                                            onClick = {
                                                menuExpanded = false
                                                folderToRename = folder
                                                renameFolderName = folder.name
                                            },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(if (showArchived) "Вернуть из архива" else "Архивировать") },
                                            onClick = {
                                                menuExpanded = false
                                                viewModel.archiveFolder(folder.id, !showArchived)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Удалить") },
                                            onClick = {
                                                menuExpanded = false
                                                folderToDelete = folder.id
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
            }
        }
    }
}

internal fun formatPlanCount(count: Int): String {
    val suffix = when {
        count % 100 in 11..14 -> "планов"
        count % 10 == 1 -> "план"
        count % 10 in 2..4 -> "плана"
        else -> "планов"
    }
    return "$count $suffix"
}
