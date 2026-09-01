package com.example

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class GeneratorMediaActions(
    val launchVoice: (String) -> Unit,
    val launchGallery: (targetClass: String, remainingSlots: Int) -> Unit,
    val launchCamera: (String) -> Unit
)

@Composable
fun rememberGeneratorMediaActions(
    onVoiceResult: (targetClass: String, text: String) -> Unit,
    onImagePrepared: (image: DraftLessonImage?, targetClass: String?) -> Unit
): GeneratorMediaActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var voiceTarget by remember { mutableStateOf<String?>(null) }
    var photoTarget by remember { mutableStateOf<String?>(null) }
    var galleryRemainingSlots by remember { mutableStateOf(0) }
    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val currentVoiceResult by rememberUpdatedState(onVoiceResult)
    val currentImagePrepared by rememberUpdatedState(onImagePrepared)

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()
            val target = voiceTarget
            if (!text.isNullOrBlank() && target != null) currentVoiceResult(target, text)
        }
        voiceTarget = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val target = photoTarget
        val selectedUris = uris.take(galleryRemainingSlots.coerceAtLeast(0))
        if (uris.size > selectedUris.size) {
            Toast.makeText(context, "Можно добавить до $MAX_MATERIAL_PHOTOS_PER_CLASS фото на один класс", Toast.LENGTH_SHORT).show()
        }
        scope.launch {
            selectedUris.forEach { uri ->
                currentImagePrepared(withContext(Dispatchers.IO) { ImageOptimizer.optimizeImage(context, uri) }, target)
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { uri ->
            val target = photoTarget
            scope.launch {
                currentImagePrepared(withContext(Dispatchers.IO) { ImageOptimizer.optimizeImage(context, uri) }, target)
            }
        }
    }

    return remember(voiceLauncher, galleryLauncher, cameraLauncher) {
        GeneratorMediaActions(
            launchVoice = { target ->
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Назовите тему урока")
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    voiceTarget = target
                    voiceLauncher.launch(intent)
                } else Toast.makeText(context, "На устройстве нет службы голосового ввода", Toast.LENGTH_SHORT).show()
            },
            launchGallery = { target, remainingSlots ->
                if (remainingSlots > 0) {
                    photoTarget = target
                    galleryRemainingSlots = remainingSlots
                    galleryLauncher.launch("image/*")
                }
            },
            launchCamera = { target ->
                photoTarget = target
                val file = File(context.cacheDir, "temp_camera_${System.currentTimeMillis()}.jpg")
                FileProvider.getUriForFile(context, "com.aistudio.lessonplanner.xyazqw.fileprovider", file).also {
                    cameraUri = it
                    cameraLauncher.launch(it)
                }
            }
        )
    }
}
