package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * İstemci tarafı görsel sıkıştırma (max 1280px) ve Gemini için Bitmap yükleme yardımcıları.
 */
object ImageCompressionUtil {

    private const val MAX_DIMENSION_PX = 1280
    private const val JPEG_QUALITY = 82

    /**
     * Galeriden seçilen bir görsel URI'sini okuyup en uzun kenarı maksimum 1280px olacak şekilde
     * yeniden boyutlandırır, JPEG olarak sıkıştırıp yerel depolamaya kaydeder ve URI dizgesini döner.
     */
    suspend fun compressUriToMax1280(context: Context, sourceUri: Uri): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1) Sadece boyutları oku (inJustDecodeBounds)
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOptions)
                }

                val rawWidth = boundsOptions.outWidth
                val rawHeight = boundsOptions.outHeight
                if (rawWidth <= 0 || rawHeight <= 0) return@runCatching null

                // 2) Uygun inSampleSize hesapla
                var sampleSize = 1
                while (max(rawWidth / sampleSize, rawHeight / sampleSize) > MAX_DIMENSION_PX * 2) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val decoded = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                } ?: return@runCatching null

                // 3) Tam olarak max 1280px sınırına ölçekle
                val scaled = scaleBitmapMax1280(decoded)
                if (scaled !== decoded) {
                    decoded.recycle()
                }

                saveBitmapToFileUri(context, scaled)
            }.getOrNull()
        }

    /**
     * Kameradan çekilen Bitmap'i en uzun kenarı maksimum 1280px olacak şekilde sıkıştırır
     * ve yerel dosya URI dizgesi olarak döner.
     */
    suspend fun compressBitmapToMax1280(context: Context, bitmap: Bitmap): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val scaled = scaleBitmapMax1280(bitmap)
                saveBitmapToFileUri(context, scaled)
            }.getOrNull()
        }

    /**
     * Gemini API çağrısı için yerel URI (`file://`, `content://`) veya uzak URL (`https://`)
     * kaynağından maksimum 1280px boyutunda bir [Bitmap] yükler.
     */
    suspend fun loadBitmapForGemini(context: Context, imageSource: String): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                when {
                    imageSource.startsWith("file://") -> {
                        val path = Uri.parse(imageSource).path ?: return@runCatching null
                        val decoded = BitmapFactory.decodeFile(path) ?: return@runCatching null
                        scaleBitmapMax1280(decoded)
                    }

                    imageSource.startsWith("content://") -> {
                        val uri = Uri.parse(imageSource)
                        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        } ?: return@runCatching null
                        scaleBitmapMax1280(decoded)
                    }

                    else -> {
                        val loader = ImageLoader(context)
                        val request = ImageRequest.Builder(context)
                            .data(imageSource)
                            .allowHardware(false)
                            .size(MAX_DIMENSION_PX, MAX_DIMENSION_PX)
                            .build()
                        val result = loader.execute(request)
                        val drawable = (result as? SuccessResult)?.drawable
                        val bmp = (drawable as? BitmapDrawable)?.bitmap ?: return@runCatching null
                        scaleBitmapMax1280(bmp)
                    }
                }
            }.getOrNull()
        }

    private fun scaleBitmapMax1280(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val longestSide = max(width, height)
        if (longestSide <= MAX_DIMENSION_PX) {
            return source
        }
        val ratio = MAX_DIMENSION_PX.toFloat() / longestSide.toFloat()
        val targetW = (width * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    private fun saveBitmapToFileUri(context: Context, bitmap: Bitmap): String {
        val dir = File(context.filesDir, "product_images").apply {
            if (!exists()) mkdirs()
        }
        val outFile = File(dir, "prod_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
        FileOutputStream(outFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.flush()
        }
        return Uri.fromFile(outFile).toString()
    }
}
