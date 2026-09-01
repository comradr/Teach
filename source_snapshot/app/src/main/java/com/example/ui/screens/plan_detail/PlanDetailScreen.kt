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
fun PlanDetailScreen(
    planId: Long,
    navController: NavHostController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var plan by remember { mutableStateOf<LessonPlanEntity?>(null) }
    var isPlanLoading by remember { mutableStateOf(true) }
    var isGeneratingActivity by remember { mutableStateOf(false) }
    
    LaunchedEffect(planId) {
        val db = com.example.data.local.PlannerDatabase.getDatabase(context)
        val loadedPlan = db.plannerDao().getPlanById(planId)
        if (loadedPlan != null) {
            plan = loadedPlan.copy(content = com.example.LessonTextSanitizer.sanitizeGeminiMarkdown(loadedPlan.content))
        } else {
            plan = null
        }
        isPlanLoading = false
    }

    if (isPlanLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                com.example.ui.components.mascot.PlannerMascot(
                    com.example.ui.components.mascot.MascotState.Loading,
                    modifier = Modifier.size(132.dp)
                )
                Text("Открываю план…", style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }

    if (plan == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                com.example.ui.components.mascot.PlannerMascot(
                    com.example.ui.components.mascot.MascotState.Error,
                    modifier = Modifier.size(132.dp)
                )
                Text("План не найден", style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        com.example.ui.components.SoftCard(
            containerColor = com.example.ui.theme.PlannerColors.LavenderSoft
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.example.ui.components.mascot.PlannerMascot(
                    com.example.ui.components.mascot.MascotState.Success,
                    modifier = Modifier.size(84.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        plan!!.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Готовый план урока",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    coroutineScope.launch {
                        val db = com.example.data.local.PlannerDatabase.getDatabase(context)
                        val newFav = !plan!!.isFavorite
                        db.plannerDao().updateFavoriteStatus(planId, newFav)
                        plan = plan?.copy(isFavorite = newFav)
                    }
                }) {
                    Icon(
                        if (plan!!.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (plan!!.isFavorite) "Убрать из избранного" else "Добавить в избранное",
                        tint = if (plan!!.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        com.example.ui.LessonTimer(durationMinutes = plan!!.durationMinutes)
        
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilledTonalButton(
                onClick = {
                    isGeneratingActivity = true
                    coroutineScope.launch {
                        try {
                            val activityResult = com.example.generateFiveMinuteActivity(context, plan!!.content)
                            if (activityResult is com.example.GeminiExtraResult.Success) {
                                val newContent = com.example.upsertMarkdownSection(plan!!.content, "# ⚡ Пятиминутка", activityResult.text)
                                val db = com.example.data.local.PlannerDatabase.getDatabase(context)
                                db.plannerDao().updatePlanContent(plan!!.id, newContent)
                                plan = plan?.copy(content = newContent)
                            } else {
                                android.widget.Toast.makeText(context, (activityResult as com.example.GeminiExtraResult.Error).message, android.widget.Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Не удалось сохранить пятиминутку. Повторите попытку.", android.widget.Toast.LENGTH_LONG).show()
                        } finally {
                            isGeneratingActivity = false
                        }
                    }
                },
                enabled = !isGeneratingActivity
            ) {
                if (isGeneratingActivity) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Пятиминутка")
            }
            
            FilledTonalButton(
                onClick = {
                    isGeneratingActivity = true
                    coroutineScope.launch {
                        try {
                            val presentationResult = com.example.generatePresentationOutline(context, plan!!.content)
                            if (presentationResult is com.example.GeminiExtraResult.Success) {
                                val newContent = com.example.upsertMarkdownSection(plan!!.content, "# 📊 Структура презентации", presentationResult.text)
                                val db = com.example.data.local.PlannerDatabase.getDatabase(context)
                                db.plannerDao().updatePlanContent(plan!!.id, newContent)
                                plan = plan?.copy(content = newContent)
                            } else {
                                android.widget.Toast.makeText(context, (presentationResult as com.example.GeminiExtraResult.Error).message, android.widget.Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Не удалось сохранить структуру презентации. Повторите попытку.", android.widget.Toast.LENGTH_LONG).show()
                        } finally {
                            isGeneratingActivity = false
                        }
                    }
                },
                enabled = !isGeneratingActivity
            ) {
                if (isGeneratingActivity) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(androidx.compose.material.icons.Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Презентация")
            }
            
            var hwPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
            var hwTargetClass by remember { mutableStateOf<String?>(null) }
            val isTwoClassPlan = com.example.LessonPromptBuilder.looksLikeTwoClassPlan(plan!!.content)
            val handleHwImage = { uri: android.net.Uri ->
                coroutineScope.launch {
                    isGeneratingActivity = true
                    var optimized: DraftLessonImage? = null
                    try {
                        optimized = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            ImageOptimizer.optimizeImage(context, uri)
                        }
                        
                        if (optimized != null) {
                            val base64Image = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                ImageOptimizer.getBase64FromPath(optimized.path)
                            }
                            if (base64Image != null) {
                                val hwResult = com.example.generateHomeworkFromImage(
                                    context,
                                    plan!!.content,
                                    listOf(Pair("image/jpeg", base64Image)),
                                    targetClass = hwTargetClass
                                )
                                if (hwResult is com.example.GeminiExtraResult.Success) {
                                    val newContent = com.example.upsertMarkdownSection(plan!!.content, "# 📚 Домашнее задание по фото учебника", hwResult.text)
                                    val db = com.example.data.local.PlannerDatabase.getDatabase(context)
                                    db.plannerDao().updatePlanContent(plan!!.id, newContent)
                                    plan = plan?.copy(content = newContent)
                                } else {
                                    android.widget.Toast.makeText(context, (hwResult as com.example.GeminiExtraResult.Error).message, android.widget.Toast.LENGTH_LONG).show()
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Не удалось прочитать подготовленное фото", android.widget.Toast.LENGTH_LONG).show()
                            }
                        } else {
                            android.widget.Toast.makeText(context, "Ошибка обработки фото", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Не удалось обработать фотографию. Попробуйте другое изображение.", android.widget.Toast.LENGTH_SHORT).show()
                    } finally {
                        isGeneratingActivity = false
                        ImageOptimizer.cleanUpTempCameraFiles(context)
                        optimized?.let { java.io.File(it.path).delete() }
                        // Also, if optimized was created just for HW, we shouldn't keep it forever, but deleting here is fine if we can track it.
                        // Actually, we don't need to save the HW image permanently, we can delete the optimized one too.
                    }
                }
            }

            val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri: android.net.Uri? ->
                uri?.let { handleHwImage(it) }
            }

            val takePictureLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture()
            ) { success: Boolean ->
                if (success) {
                    hwPhotoUri?.let { handleHwImage(it) }
                }
            }

            var expandedMenu by remember { mutableStateOf(false) }
            Box {
                FilledTonalButton(
                    onClick = { expandedMenu = true },
                    enabled = !isGeneratingActivity
                ) {
                    if (isGeneratingActivity) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("ДЗ по фото")
                }
                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    fun chooseGallery(targetClass: String?) {
                        expandedMenu = false
                        hwTargetClass = targetClass
                        imagePickerLauncher.launch("image/*")
                    }
                    fun chooseCamera(targetClass: String?) {
                        expandedMenu = false
                        hwTargetClass = targetClass
                        val tempFile = java.io.File(context.cacheDir, "temp_camera_${System.currentTimeMillis()}.jpg")
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "com.aistudio.lessonplanner.xyazqw.fileprovider", tempFile)
                        hwPhotoUri = uri
                        takePictureLauncher.launch(uri)
                    }

                    if (isTwoClassPlan) {
                        DropdownMenuItem(
                            text = { Text("Класс 1 — из галереи") },
                            onClick = { chooseGallery("Класс 1") },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Класс 1 — сделать фото") },
                            onClick = { chooseCamera("Класс 1") },
                            leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Класс 2 — из галереи") },
                            onClick = { chooseGallery("Класс 2") },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Класс 2 — сделать фото") },
                            onClick = { chooseCamera("Класс 2") },
                            leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Выбрать из галереи") },
                            onClick = { chooseGallery(null) },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Сделать фото") },
                            onClick = { chooseCamera(null) },
                            leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) }
                        )
                    }
                }
            }

            PlanDocumentActions(plan = plan!!)
        }
        HorizontalDivider()
        androidx.compose.foundation.text.selection.SelectionContainer {
            com.example.ui.components.StructuredPlanViewer(markdown = plan!!.content)
        }
    }
}
