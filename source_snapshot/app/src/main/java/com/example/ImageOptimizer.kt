package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Serializable
data class DraftLessonImage(
    val id: String,
    val path: String
)

object ImageOptimizer {
    
    data class CompressStep(val maxSide: Int, val quality: Int)
    
    val STEPS = listOf(
        CompressStep(2048, 85),
        CompressStep(2048, 78),
        CompressStep(1800, 75),
        CompressStep(1600, 70)
    )
    
    var BUDGET_BYTES = 12L * 1024 * 1024 // 12 MB
    
    fun getDraftsDir(context: Context): File {
        val dir = File(context.filesDir, "draft_images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun optimizeImage(context: Context, uri: Uri, stepIndex: Int = 0): DraftLessonImage? {
        return try {
            val step = STEPS[stepIndex.coerceIn(0, STEPS.size - 1)]
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            
            var width = options.outWidth
            var height = options.outHeight
            
            val maxSide = step.maxSide
            var inSampleSize = 1
            
            // Fix inSampleSize calculation: based on the longest side
            while (Math.max(width / (inSampleSize * 2), height / (inSampleSize * 2)) >= maxSide) {
                inSampleSize *= 2
            }
            
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            
            var bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            } ?: return null
            
            // Handle EXIF orientation
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.preScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.preScale(-1f, 1f) }
                }
                
                if (orientation != ExifInterface.ORIENTATION_NORMAL) {
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                }
            }
            
            // Exact resize to maxSide
            width = bitmap.width
            height = bitmap.height
            
            if (width > maxSide || height > maxSide) {
                val ratio = Math.min(maxSide.toFloat() / width, maxSide.toFloat() / height)
                val newWidth = Math.round(width * ratio)
                val newHeight = Math.round(height * ratio)
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            }
            
            val id = UUID.randomUUID().toString()
            val fileName = "optimized_$id.jpg"
            val file = File(getDraftsDir(context), fileName)
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, step.quality, out)
            }
            bitmap.recycle()
            
            DraftLessonImage(id, file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // Returns true if within budget, false if budget exceeded even after max compression
    fun ensureBudget(context: Context, images: List<DraftLessonImage>): Boolean {
        var stepIndex = 0
        while (stepIndex < STEPS.size) {
            var totalBytes = 0L
            for (img in images) {
                val f = File(img.path)
                if (f.exists()) totalBytes += f.length()
            }
            if (totalBytes <= BUDGET_BYTES) return true
            
            stepIndex++
            if (stepIndex >= STEPS.size) return false // reached max compression
            
            for (img in images) {
                val f = File(img.path)
                if (f.exists()) {
                    val newImg = optimizeImage(context, Uri.fromFile(f), stepIndex)
                    if (newImg != null) {
                        val newF = File(newImg.path)
                        var success = false
                        try {
                            val backup = File(f.absolutePath + ".bak")
                            f.renameTo(backup)
                            newF.copyTo(f, overwrite = true)
                            
                            if (f.exists() && f.length() > 0) {
                                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeFile(f.absolutePath, opts)
                                if (opts.outWidth > 0 && opts.outHeight > 0) {
                                    success = true
                                }
                            }
                            
                            if (success) {
                                backup.delete()
                            } else {
                                if (f.exists()) f.delete()
                                backup.renameTo(f)
                            }
                        } catch(e: Exception) {
                            val backup = File(f.absolutePath + ".bak")
                            if (backup.exists()) {
                                if (f.exists()) f.delete()
                                backup.renameTo(f)
                            }
                        }
                        newF.delete()
                    }
                }
            }
        }
        return false
    }

    fun getBase64FromPath(path: String): String? {
        return try {
            val file = File(path)
            if (!file.exists()) return null
            val bytes = file.readBytes()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun cleanUpTempCameraFiles(context: Context) {
        context.cacheDir.listFiles { _, name -> name.startsWith("temp_camera_") }?.forEach { it.delete() }
    }
}
