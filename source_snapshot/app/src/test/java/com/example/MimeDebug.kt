package com.example

import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MimeDebugTest {
    @Test
    fun testMime() {
        val originalBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val tempFile = File.createTempFile("test_landscape", ".jpg")
        FileOutputStream(tempFile).use { out ->
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(tempFile.absolutePath, options)
        println("Mime Type: " + options.outMimeType)
    }
}
