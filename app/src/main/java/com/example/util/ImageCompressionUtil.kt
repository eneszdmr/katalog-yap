package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * İstemci tarafında görsel sıkıştırma ve Base64/Data-URI dönüştürme yardımcısı:
 * - Görsel kalitesini bozmadan uzun kenarı en fazla 960px olacak şekilde yerel önbelleğe kaydeder.
 * - Veritabanı (MariaDB + Firebase Firestore) ve web katalog (`constantinopolishotel.store`)
 *   paylaşımı için uzun kenarı 720px ve boyutu <= 42 KB (~56 KB Base64) olacak şekilde
 *   kademeli kalite optimizasyonuyla `data:image/jpeg;base64,...` üretir.
 */
object ImageCompressionUtil {

    private const val MAX_DIMENSION_PX = 960
    private const val JPEG_QUALITY = 80
    private const val DB_MAX_DIMENSION_PX = 720
    private const val DB_JPEG_QUALITY = 74
    private const val MAX_DB_BINARY_BYTES = 42_000 // ~56 KB Base64 (hızlı yükleme + tüm sütun tipleriyle uyum)

    // Aynı yerel görselin tekrar tekrar Base64'e çevrilmesini önleyen bellek içi önbellek
    private val dataUriCache = ConcurrentHashMap<String, String>()

    private fun openStreamForUriOrPath(context: Context, rawUriOrPath: String): InputStream? {
        val trimmed = rawUriOrPath.trim()
        if (trimmed.isEmpty()) return null
        return runCatching {
            when {
                trimmed.startsWith("file://", ignoreCase = true) -> {
                    val path = Uri.parse(trimmed).path ?: trimmed.removePrefix("file://")
                    val file = File(path)
                    if (file.exists() && file.canRead()) FileInputStream(file) else null
                }
                trimmed.startsWith("/") -> {
                    val file = File(trimmed)
                    if (file.exists() && file.canRead()) FileInputStream(file) else null
                }
                else -> {
                    context.contentResolver.openInputStream(Uri.parse(trimmed))
                }
            }
        }.getOrNull()
    }

    /**
     * Verilen Bitmap'i web ve veritabanı için yüksek netlikte fakat <= 42 KB olacak şekilde JPEG'e sıkıştırır.
     */
    private fun compressBitmapForDatabase(source: Bitmap): ByteArray {
        val dbBitmap = scaleDownWithoutRecyclingSource(source, DB_MAX_DIMENSION_PX)
        var quality = DB_JPEG_QUALITY
        var bytes = ByteArrayOutputStream().use { bos ->
            dbBitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
            bos.toByteArray()
        }
        while (bytes.size > MAX_DB_BINARY_BYTES && quality >= 52) {
            quality -= 10
            bytes = ByteArrayOutputStream().use { bos ->
                dbBitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
                bos.toByteArray()
            }
        }
        if (bytes.size > MAX_DB_BINARY_BYTES) {
            val smaller = scaleDownWithoutRecyclingSource(dbBitmap, 540)
            bytes = ByteArrayOutputStream().use { bos ->
                smaller.compress(Bitmap.CompressFormat.JPEG, 58, bos)
                bos.toByteArray()
            }
            if (smaller !== dbBitmap && smaller !== source) {
                smaller.recycle()
            }
        }
        if (dbBitmap !== source) {
            dbBitmap.recycle()
        }
        return bytes
    }

    /**
     * Galeri veya dosya URI'sinden gelen görseli kaliteyi bozmadan sıkıştırır ve
     * uygulama içi `filesDir/product_images` klasörüne kaydedip `file://` URI döner.
     * Ayrıca Base64 önbelleğini de hazırlar ki veritabanına anında kaydedilebilsin.
     */
    suspend fun compressUriToMax1280(
        context: Context,
        sourceUri: Uri,
        maxDimension: Int = MAX_DIMENSION_PX,
        quality: Int = JPEG_QUALITY
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val rawStr = sourceUri.toString()
            // 1) Sadece boyutları oku
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            openStreamForUriOrPath(context, rawStr)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val srcWidth = boundsOptions.outWidth
            val srcHeight = boundsOptions.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) {
                return@runCatching rawStr
            }

            // 2) Uygun inSampleSize hesapla
            var sampleSize = 1
            val longestSide = max(srcWidth, srcHeight)
            while (longestSide / (sampleSize * 2) >= maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedBitmap = openStreamForUriOrPath(context, rawStr)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@runCatching rawStr

            // 3) Uzun kenar > maxDimension ise tam hedefe ölçekle
            val scaledBitmap = scaleDownIfNeeded(decodedBitmap, maxDimension)
            val fileUri = saveCompressedBitmapToInternalFile(context, scaledBitmap, quality)

            // Veritabanı (MariaDB + Firestore) için Base64 önbelleğini de sıcak tut
            val dbBytes = compressBitmapForDatabase(scaledBitmap)
            scaledBitmap.recycle()

            if (dbBytes.isNotEmpty()) {
                val dataUri = "data:image/jpeg;base64," + Base64.encodeToString(dbBytes, Base64.NO_WRAP)
                dataUriCache[fileUri] = dataUri
                dataUriCache[rawStr] = dataUri
            }

            fileUri
        }.getOrElse {
            sourceUri.toString()
        }
    }

    /**
     * Kamera çekiminden gelen Bitmap'i sıkıştırır ve `file://` URI döner.
     */
    suspend fun compressBitmapToMax1280(
        context: Context,
        sourceBitmap: Bitmap,
        maxDimension: Int = MAX_DIMENSION_PX,
        quality: Int = JPEG_QUALITY
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val scaled = scaleDownWithoutRecyclingSource(sourceBitmap, maxDimension)
            val fileUri = saveCompressedBitmapToInternalFile(context, scaled, quality)
            val dbBytes = compressBitmapForDatabase(scaled)
            if (scaled !== sourceBitmap) {
                scaled.recycle()
            }
            if (dbBytes.isNotEmpty()) {
                val dataUri = "data:image/jpeg;base64," + Base64.encodeToString(dbBytes, Base64.NO_WRAP)
                dataUriCache[fileUri] = dataUri
            }
            fileUri
        }.getOrNull()
    }

    /**
     * Yerel `file://`, `/data/...` veya `content://` görselini MariaDB ve Firebase Firestore'da
     * ve müşteri web sayfasında (`constantinopolishotel.store`) hızlıca açılacak şekilde
     * optimize edilmiş `data:image/jpeg;base64,...` formatına dönüştürür.
     * Eğer görsel zaten `http://`, `https://` veya `data:image/` ise olduğu gibi döner.
     */
    fun toWebAndDbCompatibleUrl(context: Context, rawUrlOrUri: String): String {
        val trimmed = rawUrlOrUri.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }
        if (trimmed.startsWith("data:image/", ignoreCase = true)) {
            return trimmed
        }

        dataUriCache[trimmed]?.let { return it }

        return runCatching {
            // Önce dosyanın doğrudan boyutunu kontrol et; zaten <= 42 KB ise direkt oku
            if (trimmed.startsWith("file://", ignoreCase = true) || trimmed.startsWith("/")) {
                val path = if (trimmed.startsWith("file://", ignoreCase = true)) {
                    Uri.parse(trimmed).path ?: trimmed.removePrefix("file://")
                } else {
                    trimmed
                }
                val file = File(path)
                if (file.exists() && file.length() in 1..MAX_DB_BINARY_BYTES.toLong()) {
                    val bytes = file.readBytes()
                    val encoded = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                    dataUriCache[trimmed] = encoded
                    return@runCatching encoded
                }
            }

            // Decode edip DB_MAX_DIMENSION_PX (720px) ve <= 42 KB hedefiyle sıkıştır
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStreamForUriOrPath(context, trimmed)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            val srcW = boundsOptions.outWidth
            val srcH = boundsOptions.outHeight
            if (srcW <= 0 || srcH <= 0) return@runCatching trimmed

            var sampleSize = 1
            val longest = max(srcW, srcH)
            while (longest / (sampleSize * 2) >= DB_MAX_DIMENSION_PX) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val decoded = openStreamForUriOrPath(context, trimmed)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@runCatching trimmed

            val compressedBytes = compressBitmapForDatabase(decoded)
            decoded.recycle()

            if (compressedBytes.isNotEmpty()) {
                val encoded = "data:image/jpeg;base64," + Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
                dataUriCache[trimmed] = encoded
                encoded
            } else {
                trimmed
            }
        }.getOrDefault(trimmed)
    }

    private fun scaleDownWithoutRecyclingSource(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val longest = max(width, height)
        if (longest <= maxDimension) {
            return bitmap
        }
        val ratio = maxDimension.toFloat() / longest.toFloat()
        val targetWidth = (width * ratio).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val scaled = scaleDownWithoutRecyclingSource(bitmap, maxDimension)
        if (scaled !== bitmap) {
            bitmap.recycle()
        }
        return scaled
    }

    private fun saveCompressedBitmapToInternalFile(
        context: Context,
        bitmap: Bitmap,
        quality: Int
    ): String {
        val dir = File(context.filesDir, "product_images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val outFile = File(dir, "prod_${UUID.randomUUID()}.jpg")
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(60, 95), fos)
            fos.flush()
        }
        return Uri.fromFile(outFile).toString()
    }

    /**
     * Gemini AI analizleri ve PDF/Canvas broşür çizimleri için yerel URI, Data-URI veya HTTP görselini Bitmap olarak yükler.
     */
    suspend fun loadBitmapForGemini(
        context: Context,
        imageUrlOrUri: String,
        maxDimension: Int = 768
    ): Bitmap? = withContext(Dispatchers.IO) {
        val trimmed = imageUrlOrUri.trim()
        if (trimmed.isEmpty()) return@withContext null
        runCatching {
            when {
                trimmed.startsWith("data:image/", ignoreCase = true) -> {
                    val base64Part = trimmed.substringAfter("base64,", "")
                    if (base64Part.isNotEmpty()) {
                        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching null
                        scaleDownIfNeeded(decoded, maxDimension)
                    } else null
                }
                trimmed.startsWith("http://", ignoreCase = true) ||
                    trimmed.startsWith("https://", ignoreCase = true) -> {
                    val conn = (java.net.URL(trimmed).openConnection() as java.net.HttpURLConnection).apply {
                        connectTimeout = 5000
                        readTimeout = 7000
                        doInput = true
                    }
                    conn.inputStream.use { stream ->
                        val decoded = BitmapFactory.decodeStream(stream) ?: return@runCatching null
                        scaleDownIfNeeded(decoded, maxDimension)
                    }
                }
                else -> {
                    openStreamForUriOrPath(context, trimmed)?.use { stream ->
                        val decoded = BitmapFactory.decodeStream(stream) ?: return@runCatching null
                        scaleDownIfNeeded(decoded, maxDimension)
                    }
                }
            }
        }.getOrNull()
    }

    /**
     * Mağaza logosunu (`content://`, `file://` veya `data:image/...`) kalıcı olarak
     * `filesDir/store_logos/` dizinine kaydeder ve her zaman geçerli bir `file://` (veya `https://`)
     * URI döner. Aynı zamanda MariaDB/Web için Base64 önbelleğini günceller.
     * Böylece kullanıcı çıkış yapıp tekrar giriş yaptığında mağaza logosu ASLA kaybolmaz.
     */
    fun persistStoreLogo(
        context: Context,
        rawLogoUri: String,
        accountKey: String = "default_store"
    ): String {
        val trimmed = rawLogoUri.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }

        val safeKey = accountKey.replace(Regex("[^a-zA-Z0-9_-]"), "").ifBlank { "store" }
        val logosDir = File(context.filesDir, "store_logos").apply { mkdirs() }

        // Zaten store_logos altında mevcut ve okunabilir bir dosya ise olduğu gibi koru
        if (trimmed.startsWith("file://", ignoreCase = true)) {
            val existingPath = Uri.parse(trimmed).path ?: trimmed.removePrefix("file://")
            val existingFile = File(existingPath)
            if (existingFile.exists() && existingFile.length() > 0L && existingFile.parentFile?.name == "store_logos") {
                return trimmed
            }
        }

        return runCatching {
            // 1) Eğer data:image/...;base64,... ise byte dizisini çözüp kalıcı dosyaya yaz
            if (trimmed.startsWith("data:image/", ignoreCase = true) && trimmed.contains("base64,")) {
                val base64Part = trimmed.substringAfter("base64,", "")
                val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                if (bytes.isNotEmpty()) {
                    val outFile = File(logosDir, "logo_${safeKey}.jpg")
                    FileOutputStream(outFile).use { fos ->
                        fos.write(bytes)
                        fos.flush()
                    }
                    val fileUri = Uri.fromFile(outFile).toString()
                    dataUriCache[fileUri] = trimmed
                    return@runCatching fileUri
                }
            }

            // 2) content:// veya geçici file:// ise Bitmap olarak oku, 512px'e ölçekle ve kalıcı dosyaya kaydet
            val decoded = openStreamForUriOrPath(context, trimmed)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return@runCatching trimmed

            val scaled = scaleDownWithoutRecyclingSource(decoded, 512)
            val dbBytes = compressBitmapForDatabase(scaled)
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()

            if (dbBytes.isNotEmpty()) {
                val outFile = File(logosDir, "logo_${safeKey}_${System.currentTimeMillis() % 100000}.jpg")
                FileOutputStream(outFile).use { fos ->
                    fos.write(dbBytes)
                    fos.flush()
                }
                val fileUri = Uri.fromFile(outFile).toString()
                val dataUri = "data:image/jpeg;base64," + Base64.encodeToString(dbBytes, Base64.NO_WRAP)
                dataUriCache[fileUri] = dataUri
                dataUriCache[trimmed] = dataUri
                fileUri
            } else {
                trimmed
            }
        }.getOrDefault(trimmed)
    }

    /**
     * MariaDB'den okunan `data:image/jpeg;base64,...` görsellerini uygulama içinde (Coil)
     * anında ve sorunsuz gösterilmesi için yerel önbellek dosyasına (`file://`) çevirir.
     */
    fun ensureRenderableLocalUri(
        context: Context,
        rawUrlOrUri: String,
        prefix: String = "img"
    ): String {
        val trimmed = rawUrlOrUri.trim()
        if (trimmed.isEmpty()) return ""
        if (!trimmed.startsWith("data:image/", ignoreCase = true) || !trimmed.contains("base64,")) {
            return trimmed
        }
        return runCatching {
            val base64Part = trimmed.substringAfter("base64,", "")
            if (base64Part.isEmpty()) return@runCatching trimmed
            val hash = (base64Part.hashCode().toLong() and 0xFFFFFFFFL).toString(16)
            val cacheDir = File(context.filesDir, "mariadb_images").apply { mkdirs() }
            val targetFile = File(cacheDir, "${prefix}_${hash}_${base64Part.length}.jpg")
            if (!targetFile.exists() || targetFile.length() == 0L) {
                val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                if (bytes.isNotEmpty()) {
                    targetFile.writeBytes(bytes)
                }
            }
            if (targetFile.exists() && targetFile.length() > 0L) {
                val fileUri = Uri.fromFile(targetFile).toString()
                dataUriCache[fileUri] = trimmed
                fileUri
            } else {
                trimmed
            }
        }.getOrDefault(trimmed)
    }
}

