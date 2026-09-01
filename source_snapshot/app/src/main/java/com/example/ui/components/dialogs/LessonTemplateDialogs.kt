@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.components.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.LessonTemplateEntity

@Composable
fun LessonTemplateManagerDialog(
    templates: List<LessonTemplateEntity>,
    currentForm: LessonTemplateEntity,
    subjects: List<String>,
    grades: List<String>,
    lessonTypes: List<String>,
    onApply: (LessonTemplateEntity) -> Unit,
    onSave: (LessonTemplateEntity) -> Unit,
    onDelete: (LessonTemplateEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var editor by remember { mutableStateOf<LessonTemplateEntity?>(null) }
    var deleteCandidate by remember { mutableStateOf<LessonTemplateEntity?>(null) }

    editor?.let { template ->
        LessonTemplateEditorDialog(
            initial = template,
            subjects = subjects,
            grades = grades,
            lessonTypes = lessonTypes,
            onSave = {
                onSave(it)
                editor = null
            },
            onDismiss = { editor = null }
        )
        return
    }

    deleteCandidate?.let { template ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Удалить шаблон?") },
            text = { Text("Шаблон «${template.name}» будет удалён. Планы и черновик не изменятся.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(template)
                    deleteCandidate = null
                }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("Отмена") }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Шаблоны уроков") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Сохраняйте заполненную форму и применяйте её одним нажатием. Фотографии в шаблон не входят.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { editor = currentForm.copy(id = 0, name = "") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Сохранить текущую форму")
                }
                if (templates.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "Шаблонов пока нет",
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(templates, key = { it.id }) { template ->
                            TemplateListItem(
                                template = template,
                                onApply = {
                                    onApply(template)
                                    onDismiss()
                                },
                                onEdit = { editor = template },
                                onDelete = { deleteCandidate = template }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

@Composable
private fun TemplateListItem(
    template: LessonTemplateEntity,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        template.name,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        buildString {
                            append(if (template.useSecondClass) "МКШ" else "Один класс")
                            if (template.classASubject.isNotBlank()) append(" • ${template.classASubject}")
                            append(" • ${template.lessonDuration} мин")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать шаблон")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить шаблон",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            OutlinedButton(onClick = onApply, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text("Применить")
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun LessonTemplateEditorDialog(
    initial: LessonTemplateEntity,
    subjects: List<String>,
    grades: List<String>,
    lessonTypes: List<String>,
    onSave: (LessonTemplateEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(initial.id, initial.updatedAt) { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Новый шаблон" else "Редактировать шаблон") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = { draft = draft.copy(name = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Название шаблона") },
                        singleLine = true
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !draft.useSecondClass,
                            onClick = { draft = draft.copy(useSecondClass = false) },
                            label = { Text("Один класс") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = draft.useSecondClass,
                            onClick = { draft = draft.copy(useSecondClass = true) },
                            label = { Text("Два класса") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Text("Класс 1", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    TemplateClassFields(
                        subject = draft.classASubject,
                        grade = draft.classAGrade,
                        topic = draft.classATopic,
                        subjects = subjects,
                        grades = grades,
                        onSubject = { draft = draft.copy(classASubject = it) },
                        onGrade = { draft = draft.copy(classAGrade = it) },
                        onTopic = { draft = draft.copy(classATopic = it) }
                    )
                }
                item {
                    AnimatedVisibility(visible = draft.useSecondClass) {
                        Column {
                            Text("Класс 2", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            TemplateClassFields(
                                subject = draft.classBSubject,
                                grade = draft.classBGrade,
                                topic = draft.classBTopic,
                                subjects = subjects,
                                grades = grades,
                                onSubject = { draft = draft.copy(classBSubject = it) },
                                onGrade = { draft = draft.copy(classBGrade = it) },
                                onTopic = { draft = draft.copy(classBTopic = it) }
                            )
                        }
                    }
                }
                item {
                    Text("Режим плана", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Для себя", "Официальный").forEach { mode ->
                            FilterChip(
                                selected = draft.planMode == mode,
                                onClick = { draft = draft.copy(planMode = mode) },
                                label = { Text(mode) }
                            )
                        }
                    }
                }
                item {
                    Text("Длительность", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("35", "40", "45").forEach { duration ->
                            FilterChip(
                                selected = draft.lessonDuration == duration,
                                onClick = { draft = draft.copy(lessonDuration = duration) },
                                label = { Text("$duration мин") }
                            )
                        }
                    }
                }
                item {
                    TemplateDropdown(
                        value = draft.lessonType,
                        label = "Тип урока",
                        items = lessonTypes,
                        onSelected = { draft = draft.copy(lessonType = it) }
                    )
                }
                item {
                    OutlinedTextField(
                        value = draft.additionalInstructions,
                        onValueChange = { draft = draft.copy(additionalInstructions = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Дополнительные пожелания") },
                        minLines = 2,
                        maxLines = 4
                    )
                }
                item {
                    AnimatedVisibility(visible = draft.useSecondClass) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = draft.classAIndependentWorkLimit,
                                onValueChange = { draft = draft.copy(classAIndependentWorkLimit = it.filter(Char::isDigit)) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Класс 1: самостоятельная работа, мин") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = draft.classBIndependentWorkLimit,
                                onValueChange = { draft = draft.copy(classBIndependentWorkLimit = it.filter(Char::isDigit)) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Класс 2: самостоятельная работа, мин") },
                                singleLine = true
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(draft.copy(name = draft.name.trim())) },
                enabled = draft.name.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Назад") } }
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TemplateClassFields(
    subject: String,
    grade: String,
    topic: String,
    subjects: List<String>,
    grades: List<String>,
    onSubject: (String) -> Unit,
    onGrade: (String) -> Unit,
    onTopic: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TemplateDropdown(subject, "Предмет", subjects, onSubject)
        TemplateDropdown(grade, "Класс", grades, onGrade)
        OutlinedTextField(
            value = topic,
            onValueChange = onTopic,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Тема урока") },
            singleLine = true
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TemplateDropdown(
    value: String,
    label: String,
    items: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
