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
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.Catalog
import com.example.model.Product
import com.example.model.ShareSettings
import com.example.model.StoreSettings
import com.example.model.WatermarkPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
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

    const val WEB_BASE_URL = "https://constantinopolishotel.store"

    fun normalizeTurkishSlug(raw: String): String {
        return raw.trim()
            .replace("İ", "i")
            .replace("I", "i")
            .replace("ı", "i")
            .replace("Ş", "s")
            .replace("ş", "s")
            .replace("Ğ", "g")
            .replace("ğ", "g")
            .replace("Ü", "u")
            .replace("ü", "u")
            .replace("Ö", "o")
            .replace("ö", "o")
            .replace("Ç", "c")
            .replace("ç", "c")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9_-]+"), "-")
            .trim('-')
    }

    fun generateFullShortUrl(
        shortPath: String,
        settings: ShareSettings,
        catalogSlugOrId: String = ""
    ): String {
        val rawKey = catalogSlugOrId.trim()
            .ifBlank {
                shortPath.removePrefix("/?c=")
                    .removePrefix("?c=")
                    .removePrefix("/c/klk/")
                    .removePrefix("/c/")
                    .removePrefix("/p/")
                    .trim('/')
            }
        val cleanKey = normalizeTurkishSlug(rawKey).ifBlank { "katalog" }

        val base = "$WEB_BASE_URL/?c=$cleanKey"
        val params = mutableListOf<String>()
        if (!settings.showPrices) params.add("p=0")
        if (settings.isDealerShare) params.add("b=1")
        if (settings.hasPassword && settings.password.isNotBlank()) params.add("lock=1")
        return if (params.isEmpty()) base else "$base&${params.joinToString("&")}"
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
     * Görseli hedef kutu (boxRect) içerisine en-boy oranını (aspect ratio) KESİNLİKLE bozmadan,
     * çekip sündürmeden (fit-center) yuvarlatılmış köşeli bir çerçeve içinde çizer.
     */
    private fun drawBitmapFitInsideRoundedBox(
        canvas: Canvas,
        bmp: Bitmap,
        boxRect: RectF,
        cornerRadius: Float = 10f,
        bgColor: Int = Color.parseColor("#F1F5F9"),
        borderColor: Int = Color.parseColor("#E2E8F0"),
        innerPadding: Float = 4f
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = bgColor
        }
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, bgPaint)

        val bw = bmp.width.toFloat().coerceAtLeast(1f)
        val bh = bmp.height.toFloat().coerceAtLeast(1f)
        val availW = (boxRect.width() - innerPadding * 2f).coerceAtLeast(1f)
        val availH = (boxRect.height() - innerPadding * 2f).coerceAtLeast(1f)

        val scale = minOf(availW / bw, availH / bh)
        val drawW = bw * scale
        val drawH = bh * scale
        val cx = boxRect.centerX()
        val cy = boxRect.centerY()
        val dstRect = RectF(
            cx - drawW / 2f,
            cy - drawH / 2f,
            cx + drawW / 2f,
            cy + drawH / 2f
        )

        val saveCount = canvas.save()
        val clipPath = android.graphics.Path().apply {
            addRoundRect(boxRect, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
        }
        canvas.clipPath(clipPath)

        val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(bmp, null, dstRect, bmpPaint)
        canvas.restoreToCount(saveCount)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = borderColor
        }
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, borderPaint)
    }

    /**
     * 2) BROŞÜR (PDF) OLARAK PAYLAŞ:
     * Kapakta işletme logosu ve katalog adı, her ürün için en-boy oranı korunmuş görsel + ad + fiyat içeren
     * tek sayfalık veya çok sayfalı, Türkçe karakterleri (ı, İ, ş, Ş, ğ, Ğ, ç, Ç, ö, Ö, ü, Ü) eksiksiz
     * ve ferah harf aralığıyla (letterSpacing) işleyen yüksek çözünürlüklü PDF üretir.
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
            val renderScale = 2.5f // Yüksek çözünürlüklü (Retina/Baskı) rasterizasyon: Türkçe font ve görsel kalitesi için
            val bmpWidth = (pageWidth * renderScale).roundToInt()
            val bmpHeight = (pageHeight * renderScale).roundToInt()

            val itemsToRender = if (singlePageMode) products.take(6) else products.take(24)
            val bitmapsMap = mutableMapOf<String, Bitmap>()
            itemsToRender.forEach { prod ->
                val bmp = ImageCompressionUtil.loadBitmapForGemini(context, prod.imageUrl, maxDimension = 960)
                if (bmp != null) {
                    bitmapsMap[prod.id] = bmp
                }
            }
            val storeLogoBmp = if (store.logoUri.isNotBlank()) {
                ImageCompressionUtil.loadBitmapForGemini(context, store.logoUri, maxDimension = 300)
            } else null

            val primaryGreen = Color.parseColor("#10B981")
            val darkAnthracite = Color.parseColor("#0F172A")
            val headerSlate = Color.parseColor("#1E293B")
            val mutedSlate = Color.parseColor("#64748B")
            val lightBg = Color.parseColor("#F8FAFC")
            val cardBorder = Color.parseColor("#E2E8F0")
            val amberColor = Color.parseColor("#F59E0B")

            val typefaceNormal = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            val typefaceBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

            val chunks = if (singlePageMode) {
                listOf(itemsToRender)
            } else {
                itemsToRender.chunked(6).ifEmpty { listOf(emptyList()) }
            }

            chunks.forEachIndexed { pageIndex, pageProducts ->
                val pageBitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(pageBitmap).apply {
                    scale(renderScale, renderScale)
                }

                val paint = Paint(
                    Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG
                ).apply {
                    letterSpacing = 0.025f
                }

                // Sayfa arka planı
                paint.style = Paint.Style.FILL
                paint.color = lightBg
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paint)

                // Üst Kapak / Başlık Bandı (Kurumsal Antrasit & Yeşil Vurgu)
                val headerHeight = if (pageIndex == 0) 168f else 92f
                paint.shader = LinearGradient(
                    0f, 0f, pageWidth.toFloat(), headerHeight,
                    darkAnthracite, headerSlate,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), headerHeight, paint)
                paint.shader = null

                // Alt yeşil kurumsal şerit
                paint.color = primaryGreen
                canvas.drawRect(0f, headerHeight - 4f, pageWidth.toFloat(), headerHeight, paint)

                // İşletme Logosu veya Monogramı (Dairesel)
                val logoBox = RectF(30f, 26f, 82f, 78f)
                if (storeLogoBmp != null) {
                    drawBitmapFitInsideRoundedBox(
                        canvas = canvas,
                        bmp = storeLogoBmp,
                        boxRect = logoBox,
                        cornerRadius = 26f,
                        bgColor = Color.WHITE,
                        borderColor = primaryGreen,
                        innerPadding = 2f
                    )
                } else {
                    val initials = store.businessName
                        .trim()
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercase(Locale("tr", "TR")) }
                        .ifBlank { "KY" }

                    paint.color = primaryGreen
                    canvas.drawCircle(56f, 52f, 26f, paint)
                    paint.color = Color.WHITE
                    paint.typeface = typefaceBold
                    paint.textSize = 17f
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(initials, 56f, 58f, paint)
                }

                // İşletme Adı ve Katalog Başlığı
                paint.textAlign = Paint.Align.LEFT
                paint.color = Color.parseColor("#CBD5E1")
                paint.textSize = 12f
                paint.typeface = typefaceNormal
                val headerSub = buildString {
                    append(store.businessName.ifBlank { "Katalog Yap Mağaza" })
                    if (store.phoneWhatsapp.isNotBlank()) {
                        append("  •  ${store.phoneWhatsapp}")
                    }
                }
                canvas.drawText(headerSub.take(54), 96f, 42f, paint)

                paint.color = Color.WHITE
                paint.textSize = if (pageIndex == 0) 21f else 16.5f
                paint.typeface = typefaceBold
                canvas.drawText(
                    catalogTitle.take(38),
                    96f,
                    70f,
                    paint
                )

                if (pageIndex == 0) {
                    paint.textSize = 11.5f
                    paint.typeface = typefaceNormal
                    paint.color = Color.parseColor("#E2E8F0")
                    canvas.drawText(
                        catalogDescription.ifBlank { "Dijital Ürün Kataloğu & Fiyat Broşürü" }.take(66),
                        96f,
                        95f,
                        paint
                    )

                    // Durum rozeti (Bayi / Fiyat durumu / Ürün sayısı)
                    paint.color = if (settings.isDealerShare) amberColor else primaryGreen
                    val badgeRect = RectF(96f, 114f, 336f, 140f)
                    canvas.drawRoundRect(badgeRect, 12f, 12f, paint)
                    paint.color = if (settings.isDealerShare) darkAnthracite else Color.WHITE
                    paint.typeface = typefaceBold
                    paint.textSize = 10f
                    val badgeLabel = when {
                        settings.isDealerShare -> "BAYİ ÖZEL KATALOĞU • ${products.size} ÜRÜN"
                        !settings.showPrices -> "ÖZEL KOLEKSİYON • ${products.size} ÜRÜN"
                        else -> "GÜNCEL BROŞÜR • ${products.size} ÜRÜN"
                    }
                    canvas.drawText(badgeLabel, 110f, 131f, paint)
                }

                // Ürün Kartları (2 sütun x 3 satır grid)
                val startY = headerHeight + 20f
                val cardW = 258f
                val cardH = 196f
                val colGap = 19f
                val rowGap = 14f
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

                    // Ürün Görseli (En-boy oranı korunarak, sündürülmeden çizilir)
                    val imgRect = RectF(left + 10f, top + 10f, left + cardW - 10f, top + 118f)
                    val bmp = bitmapsMap[product.id]
                    if (bmp != null) {
                        drawBitmapFitInsideRoundedBox(
                            canvas = canvas,
                            bmp = bmp,
                            boxRect = imgRect,
                            cornerRadius = 10f,
                            bgColor = Color.parseColor("#F8FAFC"),
                            borderColor = Color.parseColor("#E2E8F0"),
                            innerPadding = 4f
                        )
                    } else {
                        paint.color = Color.parseColor("#ECFDF5")
                        canvas.drawRoundRect(imgRect, 10f, 10f, paint)
                        paint.color = primaryGreen
                        paint.typeface = typefaceBold
                        paint.textSize = 11.5f
                        paint.textAlign = Paint.Align.CENTER
                        canvas.drawText(
                            product.subCategory.ifBlank { "Ürün Görseli Yok" }.take(22),
                            imgRect.centerX(),
                            imgRect.centerY() + 4f,
                            paint
                        )
                        paint.textAlign = Paint.Align.LEFT
                    }

                    // Ürün Adı
                    paint.color = darkAnthracite
                    paint.typeface = typefaceBold
                    paint.textSize = 12f
                    canvas.drawText(
                        product.name.take(26),
                        left + 12f,
                        top + 139f,
                        paint
                    )

                    // SKU ve Kategori
                    paint.color = mutedSlate
                    paint.typeface = typefaceNormal
                    paint.textSize = 9.8f
                    val skuCatText = buildString {
                        if (product.sku.isNotBlank()) append("SKU: ${product.sku}")
                        if (product.subCategory.isNotBlank()) {
                            if (isNotEmpty()) append("  •  ")
                            append(product.subCategory)
                        }
                    }.ifBlank { "Standart Ürün" }
                    canvas.drawText(
                        skuCatText.take(34),
                        left + 12f,
                        top + 156f,
                        paint
                    )

                    // Fiyat (veya Fiyatlar Gizli / 0 ise Fiyat Sorunuz)
                    paint.typeface = typefaceBold
                    paint.textSize = 13.5f
                    paint.color = primaryGreen
                    val priceText = if (settings.showPrices && product.price > 0.0) {
                        val adjusted = settings.calculateAdjustedPrice(product.price)
                        "${adjusted.roundToInt()} ${product.currency}"
                    } else {
                        "Fiyat Sorunuz"
                    }
                    canvas.drawText(priceText, left + 12f, top + 180f, paint)
                }

                // Alt Bilgi (Footer)
                paint.color = mutedSlate
                paint.typeface = typefaceNormal
                paint.textSize = 9.5f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(
                    "${store.businessName.ifBlank { "KATALOG YAP" }}  •  Sayfa ${pageIndex + 1} / ${chunks.size}",
                    pageWidth / 2f,
                    pageHeight - 16f,
                    paint
                )

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val pdfPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                page.canvas.drawBitmap(
                    pageBitmap,
                    Rect(0, 0, bmpWidth, bmpHeight),
                    RectF(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat()),
                    pdfPaint
                )
                pdfDocument.finishPage(page)
                pageBitmap.recycle()
            }

            val dir = File(context.cacheDir, "shared_files").apply {
                if (!exists()) mkdirs()
            }
            val safeName = normalizeTurkishSlug(catalogTitle)
                .replace("-", "_")
                .ifBlank { "katalog_brosur" }
            val outFile = File(dir, "${safeName}_brosur.pdf")
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
                out.flush()
            }
            pdfDocument.close()
            savePdfToPublicDownloads(context, outFile, "${safeName}_brosur.pdf")
            outFile
        }.getOrNull()
    }

    /**
     * 3) GÖRSEL OLARAK PAYLAŞ (Canvas İşleme):
     * Ürün görselini en-boy oranını bozmadan seçilen bilgilerle (başlık, fiyat, SKU, ek not) ve
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
        val typefaceNormal = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        val typefaceBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
            letterSpacing = 0.025f
        }

        // 1) Arka plan ve Ürün Görseli (En-boy oranını koruyarak çizer)
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)

        val sourceBmp = ImageCompressionUtil.loadBitmapForGemini(context, imageUrl, maxDimension = 1080)
        if (sourceBmp != null) {
            val bw = sourceBmp.width.toFloat().coerceAtLeast(1f)
            val bh = sourceBmp.height.toFloat().coerceAtLeast(1f)
            val scale = minOf(sizePx.toFloat() / bw, sizePx.toFloat() / bh)
            val drawW = bw * scale
            val drawH = bh * scale
            val dstRect = RectF(
                (sizePx - drawW) / 2f,
                (sizePx - drawH) / 2f,
                (sizePx + drawW) / 2f,
                (sizePx + drawH) / 2f
            )
            val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(sourceBmp, null, dstRect, bmpPaint)
        }

        // 2) Alt Karartma Gradyanı (Bilgi alanları açıksa okunaklılığı artırır)
        if (showTitle || showPrice || showSku || extraNote.isNotBlank()) {
            val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, sizePx * 0.52f,
                    0f, sizePx.toFloat(),
                    Color.TRANSPARENT,
                    Color.argb(230, 15, 23, 42),
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
            paint.typeface = typefaceBold
            paint.textSize = 26f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(extraNote.take(44), 80f, cursorY - 18f, paint)
            cursorY -= 78f
        }

        if (showSku && skuText.isNotBlank()) {
            paint.color = Color.parseColor("#CBD5E1")
            paint.typeface = typefaceNormal
            paint.textSize = 26f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Stok Kodu (SKU): $skuText", 56f, cursorY, paint)
            cursorY -= 46f
        }

        if (showPrice && priceText.isNotBlank()) {
            val priceBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#10B981")
            }
            paint.typeface = typefaceBold
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
            paint.typeface = typefaceBold
            paint.textSize = 46f
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
                color = Color.argb(alphaInt, 16, 185, 129)
            }
            canvas.drawCircle(left + 42f, top + 42f, 28f, circlePaint)

            val initials = storeName.trim().split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercase(Locale("tr", "TR")) }
                .ifBlank { "KY" }

            val wmTextPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                color = Color.argb(alphaInt, 255, 255, 255)
                typeface = typefaceBold
                textSize = 22f
                letterSpacing = 0.025f
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

    fun savePdfToPublicDownloads(
        context: Context,
        sourcePdfFile: File,
        displayFileName: String
    ): Uri? {
        return runCatching {
            val cleanFileName = if (displayFileName.endsWith(".pdf", ignoreCase = true)) {
                displayFileName
            } else {
                "$displayFileName.pdf"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, cleanFileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/KatalogYap")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(sourcePdfFile).use { input ->
                            input.copyTo(out)
                        }
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                uri
            } else {
                val downloadsDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "KatalogYap"
                ).apply { mkdirs() }
                val destFile = File(downloadsDir, cleanFileName)
                sourcePdfFile.copyTo(destFile, overwrite = true)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
            }
        }.getOrNull()
    }

    fun openPdfFile(
        context: Context,
        file: File,
        chooserTitle: String = "PDF Görüntüle"
    ): Boolean {
        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(viewIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        }.getOrElse {
            shareFileViaSystemSheet(
                context = context,
                file = file,
                mimeType = "application/pdf",
                chooserTitle = chooserTitle
            )
            false
        }
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
            context.startActivity(Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}
