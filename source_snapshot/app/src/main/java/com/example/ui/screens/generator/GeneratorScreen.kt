package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.togetherWith
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
import com.example.data.local.LessonTemplateEntity
import com.example.ui.FavoritesScreen
import com.example.ui.components.dialogs.GeminiApiKeyDialog
import com.example.ui.components.dialogs.LessonTemplateManagerDialog
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LessonPlannerScreen(
    modifier: Modifier = Modifier,
    viewModel: LessonPlannerViewModel = viewModel(),
    foldersViewModel: PlannerFoldersViewModel = viewModel(),
    templatesViewModel: LessonTemplateViewModel = viewModel(),
    navController: NavHostController
) {
    var classASubject by remember { mutableStateOf("") }
    var lastGeneratedPlan by remember { mutableStateOf("") }
    var classAGrade by remember { mutableStateOf("") }
    var classATopic by remember { mutableStateOf("") }
    val context = LocalContext.current

    var classBSubject by remember { mutableStateOf("") }
    var classBGrade by remember { mutableStateOf("") }
    var classBTopic by remember { mutableStateOf("") }

    var useSecondClass by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var classAIndependentWorkLimit by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("15") }
    var classBIndependentWorkLimit by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("15") }
    var planMode by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("Для себя") }
    var additionalInstructions by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var lessonDuration by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("45") }
    val draftStore = remember { LessonDraftStore(context) }
    var draftLoaded by remember { mutableStateOf(false) }

    
    var showSaveDialog by remember { mutableStateOf(false) }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var planTitle by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val folders by foldersViewModel.allFolders.collectAsState()
    val savedTemplates by templatesViewModel.templates.collectAsState()
    var showTemplatesDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    var expandedASubject by remember { mutableStateOf(false) }
    var expandedAGrade by remember { mutableStateOf(false) }
    var expandedBSubject by remember { mutableStateOf(false) }
    var expandedBGrade by remember { mutableStateOf(false) }
    var expandedLessonType by remember { mutableStateOf(false) }
    var lessonType by remember { mutableStateOf("Комбинированный") }
    var advancedExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    val subjects = listOf("Математика", "Русский язык", "Литературное чтение", "Литература", "Окружающий мир", "История", "География", "Биология", "Информатика", "Иностранный язык", "Изобразительное искусство", "Технология", "Музыка", "Физкультура")
    var classAImages by remember { mutableStateOf<List<DraftLessonImage>>(emptyList()) }
    var classBImages by remember { mutableStateOf<List<DraftLessonImage>>(emptyList()) }
    val removeDraftImage = { image: DraftLessonImage ->
        val file = java.io.File(image.path)
        if (file.exists() && file.absolutePath.contains("draft_images")) {
            file.delete()
        }
    }

    val handleOptimizedImage = { optimizedImage: DraftLessonImage?, targetClass: String? ->
        if (optimizedImage != null) {
            when (targetClass) {
                "A" -> if (classAImages.size < MAX_MATERIAL_PHOTOS_PER_CLASS) {
                    classAImages = classAImages + optimizedImage
                } else removeDraftImage(optimizedImage)
                "B" -> if (classBImages.size < MAX_MATERIAL_PHOTOS_PER_CLASS) {
                    classBImages = classBImages + optimizedImage
                } else removeDraftImage(optimizedImage)
            }
        } else {
            android.widget.Toast.makeText(context, "Ошибка подготовки фото", android.widget.Toast.LENGTH_SHORT).show()
        }
        ImageOptimizer.cleanUpTempCameraFiles(context)
    }

    val mediaActions = rememberGeneratorMediaActions(
        onVoiceResult = { target, spoken ->
            if (target == "A") classATopic = spoken else if (target == "B") classBTopic = spoken
            isError = false
        },
        onImagePrepared = handleOptimizedImage
    )
    val grades = listOf("1 класс", "2 класс", "3 класс", "4 класс", "5 класс")
    val lessonTypes = listOf("Комбинированный", "Изучение нового материала", "Урок закрепления", "Урок обобщения", "Контрольный урок")

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (!draftLoaded) {
            val draft = draftStore.getDraft()
            useSecondClass = draft.useSecondClass
            classASubject = draft.classASubject
            classAGrade = draft.classAGrade
            classATopic = draft.classATopic
            classBSubject = draft.classBSubject
            classBGrade = draft.classBGrade
            classBTopic = draft.classBTopic
            classAIndependentWorkLimit = draft.classAIndependentWorkLimit
            classBIndependentWorkLimit = draft.classBIndependentWorkLimit
            lessonDuration = draft.lessonDuration
            additionalInstructions = draft.additionalInstructions
            planMode = draft.planMode
            lessonType = draft.lessonType
            if (draft.generatedPlan.isNotEmpty()) {
                viewModel.updateGeneratedPlan(draft.generatedPlan)
            }
            val restoredA = draft.classAImages.filter { image ->
                java.io.File(image.path).let { it.exists() && it.length() > 0L }
            }.take(MAX_MATERIAL_PHOTOS_PER_CLASS)
            val restoredB = draft.classBImages.filter { image ->
                java.io.File(image.path).let { it.exists() && it.length() > 0L }
            }.take(MAX_MATERIAL_PHOTOS_PER_CLASS)
            if (restoredA.isNotEmpty()) {
                classAImages = restoredA
            } else if (draft.attachedImages.isNotEmpty()) {
                classAImages = draft.attachedImages.mapNotNull { legacy ->
                    runCatching {
                        val tempId = java.util.UUID.randomUUID().toString()
                        val tempFile = java.io.File(ImageOptimizer.getDraftsDir(context), "legacy_$tempId.jpg")
                        val bytes = android.util.Base64.decode(legacy.second, android.util.Base64.DEFAULT)
                        java.io.FileOutputStream(tempFile).use { it.write(bytes) }
                        DraftLessonImage(tempId, tempFile.absolutePath)
                    }.getOrNull()
                }.take(MAX_MATERIAL_PHOTOS_PER_CLASS)
            }
            classBImages = restoredB
            draftLoaded = true
        }
    }

    LaunchedEffect(
        useSecondClass, classASubject, classAGrade, classATopic,
        classBSubject, classBGrade, classBTopic,
        classAIndependentWorkLimit, classBIndependentWorkLimit,
        lessonDuration, additionalInstructions, planMode, lessonType,
        classAImages, classBImages, lastGeneratedPlan
    ) {
        if (draftLoaded) {
            kotlinx.coroutines.delay(500)
            val generatedPlanText = lastGeneratedPlan
            val draft = com.example.LessonDraft(
                useSecondClass = useSecondClass,
                classASubject = classASubject,
                classAGrade = classAGrade,
                classATopic = classATopic,
                classBSubject = classBSubject,
                classBGrade = classBGrade,
                classBTopic = classBTopic,
                classAIndependentWorkLimit = classAIndependentWorkLimit,
                classBIndependentWorkLimit = classBIndependentWorkLimit,
                lessonDuration = lessonDuration,
                additionalInstructions = additionalInstructions,
                planMode = planMode,
                lessonType = lessonType,
                generatedPlan = generatedPlanText,
                classAImages = classAImages,
                classBImages = classBImages,
                attachedImages = emptyList()
            )
            draftStore.saveDraft(draft)
        }
    }
    
    var isEditingPlan by remember { mutableStateOf(false) }
    var editingPlanContent by remember { mutableStateOf("") }

    if (showTemplatesDialog) {
        LessonTemplateManagerDialog(
            templates = savedTemplates,
            currentForm = LessonTemplateEntity(
                name = "",
                useSecondClass = useSecondClass,
                classASubject = classASubject,
                classAGrade = classAGrade,
                classATopic = classATopic,
                classBSubject = classBSubject,
                classBGrade = classBGrade,
                classBTopic = classBTopic,
                classAIndependentWorkLimit = classAIndependentWorkLimit,
                classBIndependentWorkLimit = classBIndependentWorkLimit,
                planMode = planMode,
                additionalInstructions = additionalInstructions,
                lessonDuration = lessonDuration,
                lessonType = lessonType
            ),
            subjects = subjects,
            grades = grades,
            lessonTypes = lessonTypes,
            onApply = { template ->
                useSecondClass = template.useSecondClass
                classASubject = template.classASubject
                classAGrade = template.classAGrade
                classATopic = template.classATopic
                classBSubject = template.classBSubject
                classBGrade = template.classBGrade
                classBTopic = template.classBTopic
                classAIndependentWorkLimit = template.classAIndependentWorkLimit
                classBIndependentWorkLimit = template.classBIndependentWorkLimit
                planMode = template.planMode
                additionalInstructions = template.additionalInstructions
                lessonDuration = template.lessonDuration
                lessonType = template.lessonType
                advancedExpanded = template.additionalInstructions.isNotBlank() || template.useSecondClass
                isError = false
            },
            onSave = templatesViewModel::save,
            onDelete = templatesViewModel::delete,
            onDismiss = { showTemplatesDialog = false }
        )
    }

    if (showApiKeyDialog) {
        GeminiApiKeyDialog(onDismiss = { showApiKeyDialog = false })
    }

    
    LaunchedEffect(uiState) {
        if (uiState is PlannerUiState.Success) {
            lastGeneratedPlan = (uiState as PlannerUiState.Success).resultText
        }
        if (uiState is PlannerUiState.Saved) {
            val savedId = (uiState as PlannerUiState.Saved).planId
            showSaveDialog = false
            
            viewModel.resetState()
            lastGeneratedPlan = ""
            classASubject = ""
            classAGrade = ""
            classATopic = ""
            classBSubject = ""
            classBGrade = ""
            classBTopic = ""
            planTitle = ""
            selectedFolderId = null
            classAImages.forEach(removeDraftImage)
            classBImages.forEach(removeDraftImage)
            classAImages = emptyList()
            classBImages = emptyList()
            isEditingPlan = false
            editingPlanContent = ""
            
            navController.navigate("plan_detail/$savedId") {
                popUpTo("generator") { inclusive = false }
            }
        }
    }
    

    if (showSaveDialog && uiState is PlannerUiState.Success) {
        var newFolderName by remember { mutableStateOf("") }
        var isCreatingFolder by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Сохранить план") },
            text = {
                Column {
                    OutlinedTextField(
                        value = planTitle,
                        onValueChange = { planTitle = it },
                        label = { Text("Название плана") }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Выберите папку:")
                    
                    if (isCreatingFolder) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newFolderName,
                                onValueChange = { newFolderName = it },
                                label = { Text("Имя новой папки") },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                if (newFolderName.isNotBlank()) {
                                    foldersViewModel.addFolder(newFolderName)
                                    isCreatingFolder = false
                                    newFolderName = ""
                                }
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Создать")
                            }
                        }
                    } else {
                        TextButton(onClick = { isCreatingFolder = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Создать новую папку")
                        }
                    }
                    
                    if (folders.isEmpty() && !isCreatingFolder) {
                        Text("Нет папок.", color = MaterialTheme.colorScheme.error)
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(folders.size) { i ->
                                val folder = folders[i]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedFolderId = folder.id }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedFolderId == folder.id,
                                        onClick = { selectedFolderId = folder.id }
                                    )
                                    Text(folder.name)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val folderId = selectedFolderId
                        if (folderId != null && planTitle.isNotBlank()) {
                            val finalContent = if (isEditingPlan) editingPlanContent else (uiState as PlannerUiState.Success).resultText
                            viewModel.saveLessonPlan(
                                folderId = folderId,
                                title = planTitle,
                                content = finalContent,
                                durationMinutes = lessonDuration.toIntOrNull() ?: 45
                            )
                        }
                    },
                    enabled = selectedFolderId != null && planTitle.isNotBlank() && uiState !is PlannerUiState.Loading
                ) {
                    if (uiState is PlannerUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Сохранить")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Отмена")
                }
            }
        )

    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PlannerHero(
            onTemplates = { showTemplatesDialog = true },
            onMascotClick = { showApiKeyDialog = true },
            onClear = {
                draftStore.clearDraft()
                classASubject = ""
                classAGrade = ""
                classATopic = ""
                classBSubject = ""
                classBGrade = ""
                classBTopic = ""
                useSecondClass = false
                classAIndependentWorkLimit = "15"
                classBIndependentWorkLimit = "15"
                planMode = "Для себя"
                additionalInstructions = ""
                lessonDuration = "45"
                classAImages.forEach(removeDraftImage)
                classBImages.forEach(removeDraftImage)
                classAImages = emptyList()
                classBImages = emptyList()
                lastGeneratedPlan = ""
                editingPlanContent = ""
                isEditingPlan = false
                lessonType = "Комбинированный"
                advancedExpanded = false
                isError = false
                viewModel.resetState()
            }
        )

        GeneralSettingsCard(
            useSecondClass = useSecondClass,
            onSecondClassChange = { useSecondClass = it },
            planMode = planMode,
            onPlanModeChange = { planMode = it },
            lessonDuration = lessonDuration,
            onDurationChange = { lessonDuration = it }
        )

        ClassSectionCard(
            title = if (useSecondClass) "Класс 1" else "Класс",
            accentColor = com.example.ui.theme.PlannerColors.Primary,
            subject = classASubject,
            onSubjectChange = { classASubject = it; isError = false },
            grade = classAGrade,
            onGradeChange = { classAGrade = it; isError = false },
            topic = classATopic,
            onTopicChange = { classATopic = it; isError = false },
            subjects = subjects,
            grades = grades,
            images = classAImages,
            onCamera = { mediaActions.launchCamera("A") },
            onGallery = { mediaActions.launchGallery("A", remainingMaterialPhotoSlots(classAImages.size)) },
            onRemoveImage = { image ->
                classAImages = classAImages.filter { it.id != image.id }
                removeDraftImage(image)
            },
            onVoiceInput = { mediaActions.launchVoice("A") },
            isError = isError
        )

        androidx.compose.animation.AnimatedVisibility(
            visible = useSecondClass,
            enter = androidx.compose.animation.fadeIn(
                androidx.compose.animation.core.tween(com.example.ui.theme.PlannerMotion.Fade)
            ) + androidx.compose.animation.expandVertically(
                androidx.compose.animation.core.tween(com.example.ui.theme.PlannerMotion.Expand)
            ),
            exit = androidx.compose.animation.fadeOut(
                androidx.compose.animation.core.tween(com.example.ui.theme.PlannerMotion.Fade)
            ) + androidx.compose.animation.shrinkVertically(
                androidx.compose.animation.core.tween(com.example.ui.theme.PlannerMotion.Expand)
            )
        ) {
            ClassSectionCard(
                title = "Класс 2",
                accentColor = com.example.ui.theme.PlannerColors.Secondary,
                subject = classBSubject,
                onSubjectChange = { classBSubject = it; isError = false },
                grade = classBGrade,
                onGradeChange = { classBGrade = it; isError = false },
                topic = classBTopic,
                onTopicChange = { classBTopic = it; isError = false },
                subjects = subjects,
                grades = grades,
                images = classBImages,
                onCamera = { mediaActions.launchCamera("B") },
                onGallery = { mediaActions.launchGallery("B", remainingMaterialPhotoSlots(classBImages.size)) },
                onRemoveImage = { image ->
                    classBImages = classBImages.filter { it.id != image.id }
                    removeDraftImage(image)
                },
                onVoiceInput = { mediaActions.launchVoice("B") },
                isError = isError
            )
        }

        AdvancedSettingsCard(
            expanded = advancedExpanded,
            onExpandedChange = { advancedExpanded = it },
            lessonType = lessonType,
            onLessonTypeChange = { lessonType = it },
            lessonTypes = lessonTypes,
            additionalInstructions = additionalInstructions,
            onAdditionalInstructionsChange = { additionalInstructions = it },
            showLimits = useSecondClass,
            classALimit = classAIndependentWorkLimit,
            onClassALimitChange = { classAIndependentWorkLimit = it },
            classBLimit = classBIndependentWorkLimit,
            onClassBLimitChange = { classBIndependentWorkLimit = it }
        )

        com.example.ui.components.PressScaleButton(
            onClick = {
                if (
                    classASubject.isBlank() ||
                    classAGrade.isBlank() ||
                    (useSecondClass && (classBSubject.isBlank() || classBGrade.isBlank()))
                ) {
                    isError = true
                    android.widget.Toast.makeText(
                        context,
                        "Заполните предметы и классы",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewModel.generateLessonPlan(
                        context = context,
                        useSecondClass = useSecondClass,
                        classASubject = classASubject,
                        classAGrade = classAGrade,
                        classATopic = classATopic,
                        classALimit = classAIndependentWorkLimit,
                        classBSubject = classBSubject,
                        classBGrade = classBGrade,
                        classBTopic = classBTopic,
                        classBLimit = classBIndependentWorkLimit,
                        planMode = planMode,
                        lessonType = lessonType,
                        classAImages = classAImages,
                        classBImages = classBImages,
                        duration = lessonDuration.toIntOrNull() ?: 45,
                        additionalInstructions = additionalInstructions
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = uiState !is PlannerUiState.Loading
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (uiState is PlannerUiState.Loading) "Генерирую план…" else "Сгенерировать план урока")
        }

        androidx.compose.animation.AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                androidx.compose.animation.fadeIn(
                    androidx.compose.animation.core.tween(com.example.ui.theme.PlannerMotion.Fade)
                ) togetherWith androidx.compose.animation.fadeOut(
                    androidx.compose.animation.core.tween(com.example.ui.theme.PlannerMotion.Fade)
                )
            },
            contentKey = { it::class },
            label = "generation_state"
        ) { state ->
            when (state) {
                PlannerUiState.Loading -> {
                    GenerationMessage(
                        state = com.example.ui.components.mascot.MascotState.Loading,
                        title = "Продумываю структуру урока…",
                        message = "Собираю задания и проверяю временной план. Проценты не показываю, потому что Gemini не сообщает точный прогресс.",
                        containerColor = com.example.ui.theme.PlannerColors.LavenderSoft
                    )
                }
                is PlannerUiState.Error -> {
                    GenerationMessage(
                        state = com.example.ui.components.mascot.MascotState.Error,
                        title = "Не получилось сгенерировать план",
                        message = state.message,
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                }
                is PlannerUiState.Success -> {
                    com.example.ui.components.SoftCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            com.example.ui.components.mascot.PlannerMascot(
                                com.example.ui.components.mascot.MascotState.Success,
                                modifier = Modifier.size(92.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("План готов ✨", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Можно отредактировать, проверить фрагмент или сохранить в папку",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val history = viewModel.history.collectAsState().value
                            if (history.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { viewModel.undoLastChange() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Undo, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Отменить замену")
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    if (isEditingPlan) {
                                        viewModel.updateGeneratedPlan(editingPlanContent)
                                        isEditingPlan = false
                                    } else {
                                        editingPlanContent = state.resultText
                                        isEditingPlan = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    if (isEditingPlan) Icons.Default.Check else Icons.Default.Edit,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isEditingPlan) "Готово" else "Редактировать")
                            }
                            Button(
                                onClick = { showSaveDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Сохранить")
                            }
                        }

                        if (state.warnings.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = com.example.ui.theme.PlannerColors.WarningContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "Автопроверка нашла замечания",
                                        fontWeight = FontWeight.Bold,
                                        color = com.example.ui.theme.PlannerColors.Warning
                                    )
                                    state.warnings.forEach { warning ->
                                        Text("• $warning", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        if (isEditingPlan) {
                            OutlinedTextField(
                                value = editingPlanContent,
                                onValueChange = { editingPlanContent = it },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp),
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                com.example.ui.components.StructuredPlanViewer(
                                    markdown = state.resultText
                                )
                            }
                            var regenerateSectionTitle by remember { mutableStateOf("") }
                            var regenerateInstruction by remember { mutableStateOf("") }
                            Text(
                                "Перегенерировать один раздел",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = regenerateSectionTitle,
                                onValueChange = { regenerateSectionTitle = it },
                                label = { Text("Название раздела, например «Ход урока»") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = regenerateInstruction,
                                onValueChange = { regenerateInstruction = it },
                                label = { Text("Что нужно изменить?") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )
                            Button(
                                onClick = {
                                    viewModel.regenerateSection(
                                        currentPlan = state.resultText,
                                        sectionTitle = regenerateSectionTitle,
                                        instructions = regenerateInstruction,
                                        useSecondClass = useSecondClass
                                    )
                                    regenerateSectionTitle = ""
                                    regenerateInstruction = ""
                                },
                                enabled = regenerateSectionTitle.isNotBlank() &&
                                    regenerateInstruction.isNotBlank(),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Заменить раздел")
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}
