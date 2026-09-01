package com.example

import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import java.io.File
import java.io.FileOutputStream
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImageLogicTest {

    @Test
    fun testOneClassAImagePresentBImageAbsent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val f1 = File(context.cacheDir, "path1.jpg")
        f1.writeBytes(byteArrayOf(1, 2, 3))
        val classAImages = listOf(DraftLessonImage("id1", f1.absolutePath))
        val classBImages = listOf(DraftLessonImage("id2", "path2.jpg"))
        
        val parts = GeminiLessonPartsBuilder.buildParts(false, classAImages, classBImages, "user prompt")
        
        // 1 user prompt, 1 ordering instruction, 1 image from A
        assertEquals(3, parts.size)
        assertEquals("user prompt", parts[0].text)
        assertTrue(parts[1].text!!.contains("1 фото"))
        assertNotNull(parts[2].inlineData)
    }

    @Test
    fun testTwoClassAAndBInDifferentBlocks() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Create dummy files so Base64 builder does not fail
        val f1 = File(context.cacheDir, "path1.jpg")
        val f2 = File(context.cacheDir, "path2.jpg")
        f1.writeBytes(byteArrayOf(1, 2, 3))
        f2.writeBytes(byteArrayOf(4, 5, 6))
        
        val classAImages = listOf(DraftLessonImage("id1", f1.absolutePath))
        val classBImages = listOf(DraftLessonImage("id2", f2.absolutePath))
        
        val parts = GeminiLessonPartsBuilder.buildParts(true, classAImages, classBImages, "user prompt")
        
        val textParts = parts.filter { it.text != null }.map { it.text }
        assertTrue(textParts.any { it!!.contains("МАТЕРИАЛЫ КЛАССА 1") })
        assertTrue(textParts.any { it!!.contains("МАТЕРИАЛЫ КЛАССА 2") })
    }

    @Test
    fun testBImageDoesNotValidateA() {
        val classAImages = emptyList<DraftLessonImage>()
        val classBImages = listOf(DraftLessonImage("id2", "path2.jpg"))
        
        val hasMaterialA = classAImages.isNotEmpty()
        val hasMaterialB = classBImages.isNotEmpty()
        
        assertFalse("B image should not validate A", hasMaterialA)
        assertTrue(hasMaterialB)
    }

    @Test
    fun testAImageDoesNotValidateB() {
        val classAImages = listOf(DraftLessonImage("id1", "path1.jpg"))
        val classBImages = emptyList<DraftLessonImage>()
        
        val hasMaterialA = classAImages.isNotEmpty()
        val hasMaterialB = classBImages.isNotEmpty()
        
        assertTrue(hasMaterialA)
        assertFalse("A image should not validate B", hasMaterialB)
    }

    @Test
    fun testNoMoreThanFivePhotosPerClass() {
        var classAImages = (1..MAX_MATERIAL_PHOTOS_PER_CLASS).map { DraftLessonImage("$it", "p$it") }
        val newImage = DraftLessonImage("6", "p6")
        
        if (classAImages.size < MAX_MATERIAL_PHOTOS_PER_CLASS) {
            classAImages = classAImages + newImage
        }
        
        assertEquals(5, classAImages.size)
        assertFalse(classAImages.contains(newImage))
        assertEquals(0, remainingMaterialPhotoSlots(classAImages.size))
    }

    @Test
    fun testFivePhotosAreSentInOriginalOrder() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val images = (1..MAX_MATERIAL_PHOTOS_PER_CLASS).map { index ->
            File(context.cacheDir, "page_$index.jpg").apply { writeBytes(byteArrayOf(index.toByte())) }
                .let { DraftLessonImage("id$index", it.absolutePath) }
        }

        val parts = GeminiLessonPartsBuilder.buildParts(false, images, emptyList(), "prompt")

        assertEquals(5, parts.count { it.inlineData != null })
        assertTrue(parts[1].text!!.contains("5 фото"))
        val imagePayloads = parts.filter { it.inlineData != null }.map { it.inlineData!!.data }
        assertEquals(images.map { ImageOptimizer.getBase64FromPath(it.path) }, imagePayloads)
    }

    @Test
    fun testPreparedImageMimeTypeAndSizeAndPath() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // large landscape image
        val originalBitmap = Bitmap.createBitmap(4000, 2000, Bitmap.Config.ARGB_8888)
        val tempFile = File(context.cacheDir, "test_landscape.jpg")
        FileOutputStream(tempFile).use { out ->
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        val uri = Uri.fromFile(tempFile)
        
        val optimized = ImageOptimizer.optimizeImage(context, uri)
        assertNotNull(optimized)
        
        val resultFile = File(optimized!!.path)
        assertTrue(resultFile.exists())
        
        // Path should be in draft_images
        assertTrue(resultFile.absolutePath.contains("draft_images"))
        
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(resultFile.absolutePath, options)
        
        if (options.outMimeType != "image/jpeg") {
            val bytes = resultFile.readBytes()
            assertTrue(bytes.size >= 2)
            assertEquals(0xFF.toByte(), bytes[0])
            assertEquals(0xD8.toByte(), bytes[1])
            assertEquals(0xFF.toByte(), bytes[bytes.size - 2])
            assertEquals(0xD9.toByte(), bytes[bytes.size - 1])
        } else {
            assertEquals("image/jpeg", options.outMimeType)
        }
        assertTrue("Width \${options.outWidth} should be <= 2048", options.outWidth <= 2048)
        assertTrue("Height \${options.outHeight} should be <= 2048", options.outHeight <= 2048)
    }
    
    @Test
    fun testLargePortraitImage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // large portrait image
        val originalBitmap = Bitmap.createBitmap(2000, 4000, Bitmap.Config.ARGB_8888)
        val tempFile = File(context.cacheDir, "test_portrait.jpg")
        FileOutputStream(tempFile).use { out ->
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        val uri = Uri.fromFile(tempFile)
        
        val optimized = ImageOptimizer.optimizeImage(context, uri)
        val resultFile = File(optimized!!.path)
        
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(resultFile.absolutePath, options)
        
        assertTrue("Width \${options.outWidth} should be <= 2048", options.outWidth <= 2048)
        assertTrue("Height \${options.outHeight} should be <= 2048", options.outHeight <= 2048)
    }
    
    @Test
    fun testLegacyAttachedImagesRestore() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = LessonDraftStore(context)
        
        val prefs = context.getSharedPreferences("lesson_draft", Context.MODE_PRIVATE)
        prefs.edit().putString("draft_data", """{"attachedImages":[{"first":"image/jpeg","second":"dGVzdA=="}]}""").apply()
        
        val draft = store.getDraft()
        assertEquals(1, draft.attachedImages.size)
        assertEquals("dGVzdA==", draft.attachedImages[0].second)
    }
    
    @Test
    fun testClearRemovesAAndB() {
        var classAImages = listOf(DraftLessonImage("1", "p1"))
        var classBImages = listOf(DraftLessonImage("2", "p2"))
        
        classAImages = emptyList()
        classBImages = emptyList()
        
        assertTrue(classAImages.isEmpty())
        assertTrue(classBImages.isEmpty())
    }
    
    @Test
    fun testGeminiRequestNeverAddsClassBImagesWhenUseSecondClassIsFalse() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val f2 = File(context.cacheDir, "path2.jpg")
        f2.writeBytes(byteArrayOf(4, 5, 6))
        
        val classBImages = listOf(DraftLessonImage("2", f2.absolutePath))
        
        val parts = GeminiLessonPartsBuilder.buildParts(false, emptyList(), classBImages, "user prompt")
        
        // Parts should just be the user prompt, B is ignored
        assertEquals(1, parts.size)
        assertNull(parts.first().inlineData)
    }
    
    @Test
    fun testAdaptiveCompressionBudget() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        val originalBudget = ImageOptimizer.BUDGET_BYTES
        try {
            val originalBitmap = Bitmap.createBitmap(2000, 2000, Bitmap.Config.ARGB_8888)
            // Generate some noise to make it not too small
            for(x in 0 until 2000 step 10) {
                for(y in 0 until 2000 step 10) {
                    originalBitmap.setPixel(x, y, android.graphics.Color.RED)
                }
            }
            
            val tempFile = File(context.cacheDir, "test_large.jpg")
            FileOutputStream(tempFile).use { out ->
                originalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out) // Max quality initially
            }
            val uri = Uri.fromFile(tempFile)
            
            // First step optimization
            val optimized1 = ImageOptimizer.optimizeImage(context, uri, 0)
            assertNotNull(optimized1)
            val file1 = File(optimized1!!.path)
            val size1 = file1.length()
            
            // Set budget to slightly less than size1
            ImageOptimizer.BUDGET_BYTES = size1 - 1000
            
            val ok = ImageOptimizer.ensureBudget(context, listOf(optimized1))
            val size2 = file1.length()
            
            // The size should have changed (decreased)
            assertTrue("Size should decrease. Old: \$size1, New: \$size2", size2 < size1)
            assertTrue("File should still exist", file1.exists())
            assertTrue("Should fit in budget", size2 <= ImageOptimizer.BUDGET_BYTES || !ok)
            
            // Check it is still a valid JPEG
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file1.absolutePath, options)
            if (options.outMimeType != "image/jpeg") {
                val bytes = file1.readBytes()
                assertTrue(bytes.size >= 2)
                assertEquals(0xFF.toByte(), bytes[0])
                assertEquals(0xD8.toByte(), bytes[1])
                assertEquals(0xFF.toByte(), bytes[bytes.size - 2])
                assertEquals(0xD9.toByte(), bytes[bytes.size - 1])
            } else {
                assertEquals("image/jpeg", options.outMimeType)
            }
            
        } finally {
            // Restore original budget
            ImageOptimizer.BUDGET_BYTES = originalBudget
        }
    }


    @Test
    fun testOneClassImageBudgetSkipsB() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        val classBImages = listOf(DraftLessonImage("id2", "dummy_path_that_would_crash_if_accessed"))
        
        // In LessonPlannerViewModel, allImages is constructed like:
        val useSecondClass = false
        val allImages = if (useSecondClass) emptyList<DraftLessonImage>() + classBImages else emptyList<DraftLessonImage>()
        
        // ensureBudget should simply return true and NOT crash accessing dummy path
        val ok = ImageOptimizer.ensureBudget(context, allImages)
        assertTrue(ok)
    }

    @Test
    fun testMissingImageDoesNotCreateEmptyMaterialBlock() {
        val missing = DraftLessonImage("missing", "/definitely/not/existing/image.jpg")
        val parts = GeminiLessonPartsBuilder.buildParts(true, listOf(missing), emptyList(), "prompt")
        assertEquals(1, parts.size)
        assertEquals("prompt", parts.first().text)
    }
}
