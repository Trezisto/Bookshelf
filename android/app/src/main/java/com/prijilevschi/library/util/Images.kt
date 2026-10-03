package com.prijilevschi.library.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

object Images {
    private const val MAX_SIZE = 1024

    /** Decodes the picked/taken photo (respecting EXIF rotation) and re-encodes it as a JPEG of at most 1024px. */
    suspend fun toCoverJpeg(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val (w, h) = info.size.width to info.size.height
            val scale = MAX_SIZE.toFloat() / maxOf(w, h)
            if (scale < 1f) decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
        }
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }

    /** Re-encodes downloaded image bytes as a cover JPEG of at most 1024px; null if they are not a usable image. */
    suspend fun toCoverJpeg(bytes: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
        if (bitmap.width < 50 || bitmap.height < 50) {
            bitmap.recycle() // e.g. the 1px placeholder some covers servers answer with
            return@withContext null
        }
        val scale = MAX_SIZE.toFloat() / maxOf(bitmap.width, bitmap.height)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else bitmap
        ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
    }

    /** A content:// Uri the camera app can write a new photo to. */
    fun newCameraUri(context: Context): Uri {
        val dir = File(context.cacheDir, "covers").apply { mkdirs() }
        val file = File(dir, "cover_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
