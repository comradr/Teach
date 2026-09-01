package com.example.ui.components.dialogs

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.GeminiApiKeyProvider

@Composable
fun GeminiApiKeyDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var key by rememberSaveable { mutableStateOf("") }
    var keyVisible by rememberSaveable { mutableStateOf(false) }
    var hasLocalKey by rememberSaveable { mutableStateOf(GeminiApiKeyProvider.hasLocalKey(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ключ Gemini") },
        text = {
            Column {
                Text(
                    if (hasLocalKey) {
                        "Личный ключ сохранён на этом устройстве. Введите новый ключ, чтобы заменить его."
                    } else {
                        "Вставьте личный API-ключ Gemini. Он будет храниться только на этом устройстве."
                    }
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("API-ключ") },
                    placeholder = { Text("Вставьте ключ") },
                    visualTransformation = if (keyVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(
                                imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (keyVisible) "Скрыть ключ" else "Показать ключ"
                            )
                        }
                    }
                )
                if (hasLocalKey) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            GeminiApiKeyProvider.clearLocal(context)
                            hasLocalKey = false
                            key = ""
                            Toast.makeText(
                                context,
                                "Личный ключ удалён. Используется ключ сборки.",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Удалить личный ключ")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = GeminiApiKeyProvider.isUsable(key),
                onClick = {
                    if (GeminiApiKeyProvider.saveLocal(context, key)) {
                        Toast.makeText(context, "Ключ сохранён", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Не удалось сохранить ключ", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
