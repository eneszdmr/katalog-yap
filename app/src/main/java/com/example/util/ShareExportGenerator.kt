package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.Catalog
import com.example.model.Product
import com.example.model.ShareSettings
import com.example.model.StoreSettings
import com.example.model.WatermarkPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Paylaşım Akışı Yardımcıları:
 * 1) Benzersiz kısa link üretimi (/c/abc123) ve wa.me WhatsApp paylaşım mesajı
 * 2) Katalog ve ürünlerden tek sayfalık veya çok sayfalı PDF broşür üretimi (PdfDocument)
 * 3) Görsel üzerine başlık, fiyat, SKU, ek not ve 5 konumlu (4 köşe + orta) şeffaf logo filigranı
 *    işleyen Canvas motoru ve sistem paylaşımı (FileProvider)
 */
object ShareExportGenerator {

    /**
     * Katalog, ürün veya çoklu katalog koleksiyonu için benzersiz kısa kod üretir (örn: /c/abc123).
     */
    fun generateShortPath(seedId: String, prefix: String = "c"): String {
        val chars = "abcdefghjkmnpqrstuvwxyz23456789"
        val hash = abs(seedId.hashCode())
        val code = buildString {
            var temp = hash + 104729
            repeat(6) {
                append(chars[temp % chars.length])
                temp /= chars.length
                if (temp == 0) temp = seedId.length * (it + 7) + 31
            }
        }
        return "/$prefix/$code"
    }

    fun generateFullShortUrl(shortPath: String, settings: ShareSettings): String {
        val base = "https://katalogyap.app$shortPath"
        val params = mutableListOf<String>()
        if (!settings.showPrices) params.add("p=0")
        if (settings.isDealerShare) params.add("b=1")
        if (settings.hasPassword && settings.password.isNotBlank()) params.add("lock=1")
        return if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
    }

    /**
     * WhatsApp (wa.me) hazır paylaşım mesajını oluşturur ve WhatsApp / tarayıcıyı açar.
     */
    fun shareToWhatsApp(
        context: Context,
        title: String,
        shortUrl: String,
        store: StoreSettings,
        settings: ShareSettings,
        extraSummary: String = ""
    ) {
        val message = buildString {
            append("*${store.businessName}* — *$title*\n")
            if (extraSummary.isNotBlank()) {
                append("$extraSummary\n")
            }
            if (settings.isDealerShare) {
                append("✨ *Bayi Özel Fiyatlı Katalog*\n")
            } else if (!settings.showPrices) {
                append("📌 *Fiyat bilgisi için mesaj gönderebilirsiniz.*\n")
            }
            if (settings.hasPassword && settings.password.isNotBlank()) {
                append("🔒 Giriş Şifresi: *${settings.password}*\n")
            }
            if (settings.hasExpiryDate && settings.expiryDateText.isNotBlank()) {
                append("⏳ Geçerlilik Tarihi: ${settings.expiryDateText}\n")
            }
            append("\n👉 Kataloğu incelemek ve sipariş vermek için tıklayın:\n$shortUrl")
        }

        val encoded = Uri.encode(message)
        val waUri = Uri.parse("https://wa.me/?text=$encoded")
        val viewIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(viewIntent)
        }.onFailure {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(fallback, title))
        }
    }

    /**
     * 2) BROŞÜR (PDF) OLARAK PAYLAŞ:
     * Kapakta işletme logosu ve katalog adı, her ürün için görsel + ad + fiyat içeren
     * tek sayfalık veya çok sayfalı PDF üretir.
     */
    suspend fun generateBrochurePdf(
        context: Context,
        catalogTitle: String,
        catalogDescription: String,
        products: List<Product>,
        store: StoreSettings,
        settings: ShareSettings,
        singlePageMode: Boolean
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 genişlik (pt)
            val pageHeight = 842 // A4 yükseklik (pt)

            // İlk 8 ürünün görsellerini önceden yükle
            val itemsToRender = if (singlePageMode) products.take(6) else products.take(18)
            val bitmapsMap = mutableMapOf<String, Bitmap>()
            itemsToRender.forEach { prod ->
                val bmp = ImageCompressionUtil.loadBitmapForGemini(context, prod.imageUrl)
                if (bmp != null) {
                    bitmapsMap[prod.id] = bmp
                }
            }

            val primaryBlue = Color.parseColor("#2563EB")
            val darkSlate = Color.parseColor("#0F172A")
            val mutedSlate = Color.parseColor("#64748B")
            val lightBg = Color.parseColor("#F8FAFC")
            val cardBorder = Color.parseColor("#E2E8F0")
            val amberColor = Color.parseColor("#F59E0B")

            val chunks = if (singlePageMode) {
                listOf(itemsToRender)
            } else {
                itemsToRender.chunked(6).ifEmpty { listOf(emptyList()) }
            }

            chunks.forEachIndexed { pageIndex, pageProducts ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val paint = Paint(Paint.ANTI_ALIAS_FLAG)

                // Sayfa arka planı
                paint.color = lightBg
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paint)

                // Üst Kapak / Başlık Bandı
                val headerHeight = if (pageIndex == 0) 168f else 92f
                paint.shader = LinearGradient(
                    0f, 0f, pageWidth.toFloat(), headerHeight,
                    primaryBlue, Color.parseColor("#1D4ED8"),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), headerHeight, paint)
                paint.shader = null

                // İşletme Monogram Logosu (Dairesel)
                val initials = store.businessName
                    .trim()
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
                    .ifBlank { "KY" }

                paint.color = Color.WHITE
                canvas.drawCircle(56f, 52f, 26f, paint)
                paint.color = primaryBlue
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 18f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(initials, 56f, 58f, paint)

                // İşletme Adı ve Katalog Başlığı
                paint.textAlign = Paint.Align.LEFT
                paint.color = Color.WHITE
                paint.textSize = 13f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(
                    "${store.businessName}  •  ${store.phoneWhatsapp}",
                    96f,
                    42f,
                    paint
                )

                paint.textSize = if (pageIndex == 0) 22f else 17f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(
                    catalogTitle.take(36),
                    96f,
                    70f,
                    paint
                )

                if (pageIndex == 0) {
                    paint.textSize = 11.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.color = Color.parseColor("#E0E7FF")
                    canvas.drawText(
                        catalogDescription.ifBlank { "Dijital Ürün Kataloğu & Fiyat Broşürü" }.take(68),
                        96f,
                        95f,
                        paint
                    )

                    // Durum rozeti (Bayi / Fiyat durumu / Ürün sayısı)
                    paint.color = amberColor
                    val badgeRect = RectF(96f, 114f, 320f, 140f)
                    canvas.drawRoundRect(badgeRect, 12f, 12f, paint)
                    paint.color = darkSlate
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 10.5f
                    val badgeLabel = when {
                        settings.isDealerShare -> "BAYİ ÖZEL KATALOĞU • ${products.size} ÜRÜN"
                        !settings.showPrices -> "ÖZEL KOLEKSİYON • ${products.size} ÜRÜN"
                        else -> "GÜNCEL BROŞÜR • ${products.size} ÜRÜN"
                    }
                    canvas.drawText(badgeLabel, 110f, 131f, paint)
                }

                // Ürün Kartları (2 sütun x 3 satır grid)
                val startY = headerHeight + 22f
                val cardW = 258f
                val cardH = 192f
                val colGap = 19f
                val rowGap = 16f
                val marginX = 30f

                pageProducts.forEachIndexed { idx, product ->
                    val col = idx % 2
                    val row = idx / 2
                    val left = marginX + col * (cardW + colGap)
                    val top = startY + row * (cardH + rowGap)
                    val cardRect = RectF(left, top, left + cardW, top + cardH)

                    // Kart Beyaz Arka Plan & Çerçeve
                    paint.style = Paint.Style.FILL
                    paint.color = Color.WHITE
                    canvas.drawRoundRect(cardRect, 14f, 14f, paint)

                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    paint.color = cardBorder
                    canvas.drawRoundRect(cardRect, 14f, 14f, paint)
                    paint.style = Paint.Style.FILL

                    // Ürün Görseli (Sol taraf veya üst bant)
                    val imgRect = RectF(left + 10f, top + 10f, left + cardW - 10f, top + 114f)
                    val bmp = bitmapsMap[product.id]
                    if (bmp != null) {
                        val srcRect = Rect(0, 0, bmp.width, bmp.height)
                        canvas.drawBitmap(bmp, srcRect, imgRect, paint)
                    } else {
                        paint.color = Color.parseColor("#DBEAFE")
                        canvas.drawRoundRect(imgRect, 10f, 10f, paint)
                        paint.color = primaryBlue
                        paint.textSize = 12f
                        paint.textAlign = Paint.Align.CENTER
                        canvas.drawText(product.subCategory, imgRect.centerX(), imgRect.centerY() + 4f, paint)
                        paint.textAlign = Paint.Align.LEFT
                    }

                    // Ürün Adı
                    paint.color = darkSlate
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 12.5f
                    canvas.drawText(
                        product.name.take(26),
                        left + 12f,
                        top + 136f,
                        paint
                    )

                    // SKU ve Kategori
                    paint.color = mutedSlate
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.textSize = 10f
                    canvas.drawText(
                        "SKU: ${product.sku}  •  ${product.subCategory}",
                        left + 12f,
                        top + 153f,
                        paint
                    )

                    // Fiyat (veya Fiyatlar Gizli)
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 14f
                    paint.color = primaryBlue
                    val priceText = if (settings.showPrices) {
                        val adjusted = settings.calculateAdjustedPrice(product.price)
                        "${adjusted.roundToInt()} ${product.currency}"
                    } else {
                        "Fiyat Sorunuz"
                    }
                    canvas.drawText(priceText, left + 12f, top + 176f, paint)
                }

                // Alt Bilgi (Footer)
                paint.color = mutedSlate
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 9.5f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(
                    "KATALOG YAP ile oluşturuldu  •  Sayfa ${pageIndex + 1} / ${chunks.size}",
                    pageWidth / 2f,
                    pageHeight - 18f,
                    paint
                )

                pdfDocument.finishPage(page)
            }

            val dir = File(context.cacheDir, "shared_files").apply {
                if (!exists()) mkdirs()
            }
            val safeName = catalogTitle.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .ifBlank { "katalog_brosur" }
            val outFile = File(dir, "${safeName}_brosur.pdf")
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
                out.flush()
            }
            pdfDocument.close()
            outFile
        }.getOrNull()
    }

    /**
     * 3) GÖRSEL OLARAK PAYLAŞ (Canvas İşleme):
     * Ürün görselini seçilen bilgilerle (başlık, fiyat, SKU, ek not) ve
     * 5 konumlu (4 köşe + orta) şeffaf logo filigranı ile Canvas üzerinde oluşturur.
     */
    suspend fun renderProductShareCardBitmap(
        context: Context,
        title: String,
        priceText: String,
        skuText: String,
        imageUrl: String,
        storeName: String,
        showTitle: Boolean,
        showPrice: Boolean,
        showSku: Boolean,
        showWatermark: Boolean,
        watermarkPosition: WatermarkPosition,
        watermarkOpacity: Float,
        extraNote: String
    ): Bitmap = withContext(Dispatchers.IO) {
        val sizePx = 1080
        val outBitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1) Arka plan ve Ürün Görseli
        paint.color = Color.parseColor("#1E293B")
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)

        val sourceBmp = ImageCompressionUtil.loadBitmapForGemini(context, imageUrl)
        if (sourceBmp != null) {
            val srcRect = Rect(0, 0, sourceBmp.width, sourceBmp.height)
            val dstRect = Rect(0, 0, sizePx, sizePx)
            canvas.drawBitmap(sourceBmp, srcRect, dstRect, paint)
        }

        // 2) Alt Karartma Gradyanı (Bilgi alanları açıksa okunaklılığı artırır)
        if (showTitle || showPrice || showSku || extraNote.isNotBlank()) {
            val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, sizePx * 0.52f,
                    0f, sizePx.toFloat(),
                    Color.TRANSPARENT,
                    Color.argb(225, 15, 23, 42),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, sizePx * 0.52f, sizePx.toFloat(), sizePx.toFloat(), gradPaint)
        }

        // 3) Alt Bilgi Katmanı (Başlık, Fiyat, SKU, Ek Not)
        var cursorY = sizePx - 64f

        if (extraNote.isNotBlank()) {
            val noteBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F59E0B")
            }
            val noteRect = RectF(56f, cursorY - 54f, sizePx - 56f, cursorY)
            canvas.drawRoundRect(noteRect, 18f, 18f, noteBgPaint)

            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 26f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(extraNote.take(44), 80f, cursorY - 18f, paint)
            cursorY -= 78f
        }

        if (showSku && skuText.isNotBlank()) {
            paint.color = Color.parseColor("#CBD5E1")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 26f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Stok Kodu (SKU): $skuText", 56f, cursorY, paint)
            cursorY -= 46f
        }

        if (showPrice && priceText.isNotBlank()) {
            val priceBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#2563EB")
            }
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 44f
            val textWidth = paint.measureText(priceText)
            val badgeRect = RectF(56f, cursorY - 64f, 56f + textWidth + 56f, cursorY + 8f)
            canvas.drawRoundRect(badgeRect, 20f, 20f, priceBadgePaint)

            paint.color = Color.WHITE
            canvas.drawText(priceText, 84f, cursorY - 12f, paint)
            cursorY -= 88f
        }

        if (showTitle && title.isNotBlank()) {
            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 48f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(title.take(28), 56f, cursorY, paint)
        }

        // 4) Logo Filigranı (4 Köşe + Orta, Şeffaflık Ayarlı)
        if (showWatermark) {
            val alphaInt = (watermarkOpacity.coerceIn(0.2f, 1.0f) * 255).roundToInt()
            val badgeW = 340f
            val badgeH = 84f
            val margin = 44f

            val left = when (watermarkPosition) {
                WatermarkPosition.TOP_LEFT, WatermarkPosition.BOTTOM_LEFT -> margin
                WatermarkPosition.TOP_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> sizePx - badgeW - margin
                WatermarkPosition.CENTER -> (sizePx - badgeW) / 2f
            }
            val top = when (watermarkPosition) {
                WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> margin
                WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> sizePx - badgeH - margin - 240f
                WatermarkPosition.CENTER -> (sizePx - badgeH) / 2f
            }

            val wmRect = RectF(left, top, left + badgeW, top + badgeH)
            val wmBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb((alphaInt * 0.85f).roundToInt(), 15, 23, 42)
            }
            canvas.drawRoundRect(wmRect, 42f, 42f, wmBgPaint)

            // Filigran dairesel ikon
            val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(alphaInt, 37, 99, 235)
            }
            canvas.drawCircle(left + 42f, top + 42f, 28f, circlePaint)

            val initials = storeName.trim().split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
                .ifBlank { "KY" }

            val wmTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(alphaInt, 255, 255, 255)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 22f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(initials, left + 42f, top + 50f, wmTextPaint)

            wmTextPaint.textAlign = Paint.Align.LEFT
            wmTextPaint.textSize = 24f
            canvas.drawText(storeName.take(16), left + 82f, top + 50f, wmTextPaint)
        }

        outBitmap
    }

    suspend fun saveBitmapToCacheFile(context: Context, bitmap: Bitmap, fileNamePrefix: String): File? =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "shared_files").apply {
                    if (!exists()) mkdirs()
                }
                val file = File(dir, "${fileNamePrefix}_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }
                file
            }.getOrNull()
        }

    fun shareFileViaSystemSheet(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String,
        extraText: String = ""
    ) {
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                if (extraText.isNotBlank()) {
                    putExtra(Intent.EXTRA_TEXT, extraText)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
        }
    }
}
