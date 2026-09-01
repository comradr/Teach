@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SectionTitle
import com.example.ui.components.SoftCard
import com.example.ui.components.mascot.MascotState
import com.example.ui.components.mascot.PlannerMascot
import com.example.ui.theme.PlannerColors
import com.example.ui.theme.PlannerMotion
import com.example.ui.theme.PlannerSpacing

@Composable
fun PlannerHero(
    onTemplates: () -> Unit,
    onClear: () -> Unit,
    onMascotClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mascotInteraction = remember { MutableInteractionSource() }
    val mascotPressed by mascotInteraction.collectIsPressedAsState()
    val mascotScale by animateFloatAsState(
        targetValue = if (mascotPressed) 0.97f else 1f,
        animationSpec = androidx.compose.animation.core.tween(PlannerMotion.Press),
        label = "mascotPress"
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PlannerColors.LavenderSoft),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(PlannerColors.PrimarySoft, PlannerColors.SecondarySoft))
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().heightIn(min = 132.dp)
            ) {
                val compact = maxWidth < 340.dp
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth(if (compact) 0.52f else 0.48f)
                        .padding(top = 4.dp)
                ) {
                    Text(
                        "МКШ · ПЛАНИРОВЩИК",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "Планя",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = if (compact) 27.sp else 30.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Помогает готовить уроки и бережёт твоё время",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Image(
                    painter = painterResource(com.example.R.drawable.mascot_plania_desk),
                    contentDescription = "Планя проверяет тетради; нажмите, чтобы открыть настройки Gemini",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth(if (compact) 0.56f else 0.59f)
                        .heightIn(min = 124.dp, max = 154.dp)
                        .scale(mascotScale)
                        .clickable(
                            interactionSource = mascotInteraction,
                            indication = null,
                            onClick = onMascotClick
                        )
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onTemplates) {
                    Icon(Icons.Default.ViewModule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Шаблоны")
                }
                OutlinedButton(onClick = onClear) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Очистить")
                }
            }
        }
    }
}

@Composable
fun GeneralSettingsCard(
    useSecondClass: Boolean,
    onSecondClassChange: (Boolean) -> Unit,
    planMode: String,
    onPlanModeChange: (String) -> Unit,
    lessonDuration: String,
    onDurationChange: (String) -> Unit
) {
    SoftCard(containerColor = MaterialTheme.colorScheme.surface) {
        SectionTitle("Формат урока", "Основные параметры без лишней прокрутки")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !useSecondClass,
                onClick = { onSecondClassChange(false) },
                label = { Text("Один класс", maxLines = 2) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = useSecondClass,
                onClick = { onSecondClassChange(true) },
                label = { Text("Два класса / МКШ", maxLines = 2) },
                modifier = Modifier.weight(1f)
            )
        }
        Text("Режим плана", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Для себя", "Официальный").forEach { mode ->
                FilterChip(
                    selected = planMode == mode,
                    onClick = { onPlanModeChange(mode) },
                    label = { Text(mode, maxLines = 1) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Text("Длительность", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("35", "40", "45").forEach { duration ->
                FilterChip(
                    selected = lessonDuration == duration,
                    onClick = { onDurationChange(duration) },
                    label = { Text("$duration мин") }
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ClassSectionCard(
    title: String,
    accentColor: Color,
    subject: String,
    onSubjectChange: (String) -> Unit,
    grade: String,
    onGradeChange: (String) -> Unit,
    topic: String,
    onTopicChange: (String) -> Unit,
    subjects: List<String>,
    grades: List<String>,
    images: List<DraftLessonImage>,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemoveImage: (DraftLessonImage) -> Unit,
    onVoiceInput: () -> Unit,
    isError: Boolean
) {
    var subjectExpanded by remember { mutableStateOf(false) }
    var gradeExpanded by remember { mutableStateOf(false) }
    val sectionColor = if (accentColor == PlannerColors.Secondary) {
        PlannerColors.ClassTwoSoft
    } else {
        PlannerColors.ClassOneSoft
    }
    SoftCard(
        containerColor = sectionColor,
        border = Brush.linearGradient(listOf(accentColor.copy(alpha = 0.38f), PlannerColors.OutlineSoft))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 6.dp, height = 34.dp)
                    .background(accentColor, RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Тема и материалы класса",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ExposedDropdownMenuBox(
            expanded = subjectExpanded,
            onExpandedChange = { subjectExpanded = !subjectExpanded }
        ) {
            OutlinedTextField(
                value = subject,
                onValueChange = onSubjectChange,
                label = { Text("Предмет") },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                readOnly = true,
                isError = isError && subject.isBlank(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(subjectExpanded) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = plannerFieldColors(accentColor)
            )
            ExposedDropdownMenu(
                expanded = subjectExpanded,
                onDismissRequest = { subjectExpanded = false }
            ) {
                subjects.forEach { option ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSubjectChange(option)
                            subjectExpanded = false
                        }
                    )
                }
            }
        }
        ExposedDropdownMenuBox(
            expanded = gradeExpanded,
            onExpandedChange = { gradeExpanded = !gradeExpanded }
        ) {
            OutlinedTextField(
                value = grade,
                onValueChange = onGradeChange,
                label = { Text("Класс") },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                readOnly = true,
                isError = isError && grade.isBlank(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(gradeExpanded) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = plannerFieldColors(accentColor)
            )
            ExposedDropdownMenu(
                expanded = gradeExpanded,
                onDismissRequest = { gradeExpanded = false }
            ) {
                grades.forEach { option ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onGradeChange(option)
                            gradeExpanded = false
                        }
                    )
                }
            }
        }
        OutlinedTextField(
            value = topic,
            onValueChange = onTopicChange,
            label = { Text("Тема урока") },
            modifier = Modifier.fillMaxWidth(),
            isError = isError && topic.isBlank() && images.isEmpty(),
            trailingIcon = {
                IconButton(onClick = onVoiceInput) {
                    Icon(Icons.Default.Mic, contentDescription = "Ввести тему голосом")
                }
            },
            minLines = 1,
            maxLines = 3,
            shape = RoundedCornerShape(16.dp),
            colors = plannerFieldColors(accentColor)
        )
        PhotosSection(
            title = "Материалы для $title",
            images = images,
            onLaunchCamera = onCamera,
            onLaunchGallery = onGallery,
            onRemoveImage = onRemoveImage
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsCard(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    lessonType: String,
    onLessonTypeChange: (String) -> Unit,
    lessonTypes: List<String>,
    additionalInstructions: String,
    onAdditionalInstructionsChange: (String) -> Unit,
    showLimits: Boolean,
    classALimit: String,
    onClassALimitChange: (String) -> Unit,
    classBLimit: String,
    onClassBLimitChange: (String) -> Unit
) {
    var typeExpanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = androidx.compose.animation.core.tween(PlannerMotion.Standard),
        label = "advanced_arrow"
    )
    SoftCard(
        modifier = Modifier.animateContentSize(
            animationSpec = androidx.compose.animation.core.tween(PlannerMotion.Expand)
        ),
        containerColor = PlannerColors.SurfaceSoft
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onExpandedChange(!expanded) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Дополнительные настройки", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Свернуть" else "Развернуть",
                modifier = Modifier.rotate(rotation)
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = lessonType,
                        onValueChange = onLessonTypeChange,
                        label = { Text("Тип урока") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        shape = RoundedCornerShape(16.dp),
                        colors = plannerFieldColors(MaterialTheme.colorScheme.primary)
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        lessonTypes.forEach { option ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onLessonTypeChange(option)
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = additionalInstructions,
                    onValueChange = onAdditionalInstructionsChange,
                    label = { Text("Дополнительные пожелания") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    colors = plannerFieldColors(MaterialTheme.colorScheme.primary)
                )
                if (showLimits) {
                    OutlinedTextField(
                        value = classALimit,
                        onValueChange = onClassALimitChange,
                        label = { Text("Самостоятельно — класс 1") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        suffix = { Text("мин") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        colors = plannerFieldColors(MaterialTheme.colorScheme.primary)
                    )
                    OutlinedTextField(
                        value = classBLimit,
                        onValueChange = onClassBLimitChange,
                        label = { Text("Самостоятельно — класс 2") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        suffix = { Text("мин") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        colors = plannerFieldColors(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun plannerFieldColors(accentColor: Color) = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = PlannerColors.SurfaceMuted,
    focusedBorderColor = accentColor.copy(alpha = 0.72f),
    unfocusedBorderColor = PlannerColors.OutlineSoft,
    focusedLabelColor = accentColor,
    cursorColor = accentColor
)

@Composable
fun GenerationMessage(
    state: MascotState,
    title: String,
    message: String,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    SoftCard(modifier = modifier, containerColor = containerColor) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlannerMascot(state, modifier = Modifier.size(86.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
