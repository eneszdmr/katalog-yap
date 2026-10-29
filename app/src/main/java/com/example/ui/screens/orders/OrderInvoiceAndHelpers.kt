package com.example.ui.screens.orders

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FiberNew
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.StoreSettings
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.WarningAmberDark
import com.example.ui.theme.AccentAmberLight
import com.example.util.ShareExportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class OrderStatusVisual(
    val textColor: Color,
    val containerColor: Color,
    val borderColor: Color,
    val icon: ImageVector
)

fun getOrderStatusVisual(status: OrderStatus): OrderStatusVisual {
    return when (status) {
        OrderStatus.NEW -> OrderStatusVisual(
            textColor = PrimaryBlue,
            containerColor = PrimaryBlueSubtle,
            borderColor = PrimaryBlue.copy(alpha = 0.35f),
            icon = Icons.Outlined.FiberNew
        )
        OrderStatus.APPROVED -> OrderStatusVisual(
            textColor = Color(0xFF0369A1),
            containerColor = Color(0xFFE0F2FE),
            borderColor = Color(0xFF0284C7).copy(alpha = 0.35f),
            icon = Icons.Outlined.Verified
        )
        OrderStatus.PREPARING -> OrderStatusVisual(
            textColor = WarningAmberDark,
            containerColor = AccentAmberLight,
            borderColor = WarningAmberDark.copy(alpha = 0.35f),
            icon = Icons.Outlined.Inventory2
        )
        OrderStatus.SHIPPED -> OrderStatusVisual(
            textColor = Color(0xFF6D28D9),
            containerColor = Color(0xFFEDE9FE),
            borderColor = Color(0xFF7C3AED).copy(alpha = 0.35f),
            icon = Icons.Outlined.LocalShipping
        )
        OrderStatus.DELIVERED -> OrderStatusVisual(
            textColor = SuccessGreen,
            containerColor = SuccessGreenLight,
            borderColor = SuccessGreen.copy(alpha = 0.35f),
            icon = Icons.Outlined.CheckCircle
        )
        OrderStatus.CANCELLED -> OrderStatusVisual(
            textColor = DangerRed,
            containerColor = DangerRedLight,
            borderColor = DangerRed.copy(alpha = 0.35f),
            icon = Icons.Outlined.Cancel
        )
    }
}

@Composable
fun OrderStatusBadge(
    status: OrderStatus,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    tag: String = "order_status_badge_${status.name}"
) {
    val visual = getOrderStatusVisual(status)
    Surface(
        shape = BadgeRoundedPill,
        color = visual.containerColor,
        border = BorderStroke(1.dp, visual.borderColor),
        modifier = modifier.testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showIcon) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = null,
                    tint = visual.textColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = status.labelTr,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = visual.textColor
            )
        }
    }
}

fun formatOrderDateTr(timestamp: Long): String {
    return runCatching {
        val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("tr", "TR"))
        sdf.format(Date(timestamp))
    }.getOrDefault("Bugün")
}

object OrderInvoiceHelper {

    /**
     * Müşteriye sipariş durumu ve özetiyle doğrudan WhatsApp (wa.me) mesajı açar.
     */
    fun openCustomerWhatsApp(
        context: Context,
        order: Order,
        store: StoreSettings
    ) {
        val rawDigits = order.customerPhone.filter { it.isDigit() }
        val normalizedPhone = when {
            rawDigits.startsWith("90") && rawDigits.length >= 12 -> rawDigits
            rawDigits.startsWith("0") && rawDigits.length == 11 -> "9$rawDigits"
            rawDigits.length == 10 -> "90$rawDigits"
            rawDigits.isNotEmpty() -> rawDigits
            else -> "905320000000"
        }

        val itemsSummary = order.items.joinToString("\n") { item ->
            val variantPart = if (item.selectedVariantSummary.isNotBlank()) " (${item.selectedVariantSummary})" else ""
            "• ${item.quantity}x ${item.productName}$variantPart — ${(item.unitPrice * item.quantity).roundToInt()} ${order.currency}"
        }

        val message = buildString {
            appendLine("Merhaba *${order.customerName}*,")
            appendLine("*${store.businessName}* mağazamızdan verdiğiniz *${order.orderNumber}* numaralı siparişiniz hakkında bilgilendirme:")
            appendLine()
            appendLine("📌 *Sipariş Durumu:* ${order.status.labelTr}")
            appendLine("📦 *Ürünleriniz:*")
            appendLine(itemsSummary)
            appendLine("💰 *Toplam Tutar:* ${order.totalAmount.roundToInt()} ${order.currency}")
            if (order.customerAddress.isNotBlank()) {
                appendLine("📍 *Teslimat Adresi:* ${order.customerAddress}")
            }
            appendLine()
            append("Herhangi bir sorunuz olursa bu mesaj üzerinden bize ulaşabilirsiniz.")
        }

        val encoded = Uri.encode(message)
        val waUri = Uri.parse("https://wa.me/$normalizedPhone?text=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, waUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    /**
     * Sipariş Fişi / Faturasını PDF olarak üretir, kaydeder ve paylaşım ekranını açar.
     */
    suspend fun generateOrderInvoicePdf(
        context: Context,
        order: Order,
        store: StoreSettings
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Arka plan
            paint.color = AndroidColor.parseColor("#F8FAFC")
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paint)

            // Üst Lacivert/Mavi Başlık Bandı
            paint.color = AndroidColor.parseColor("#2563EB")
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 115f, paint)

            // Mağaza Adı & Fatura Başlığı
            paint.color = AndroidColor.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 22f
            canvas.drawText(store.businessName.ifBlank { "KATALOG YAP MAĞAZA" }, 36f, 48f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 12f
            paint.color = AndroidColor.parseColor("#DBEAFE")
            canvas.drawText("SİPARİŞ BİLGİ FİŞİ / PROFORMA FATURA", 36f, 72f, paint)
            canvas.drawText("WhatsApp: ${store.phoneWhatsapp}  •  ${store.city}", 36f, 92f, paint)

            // Sağ Üst Sipariş No & Tarih Kutusu
            paint.color = AndroidColor.parseColor("#1D4ED8")
            canvas.drawRoundRect(RectF(370f, 22f, 559f, 94f), 12f, 12f, paint)
            paint.color = AndroidColor.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 15f
            canvas.drawText("Sipariş: ${order.orderNumber}", 386f, 48f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 11f
            canvas.drawText("Tarih: ${formatOrderDateTr(order.createdAt)}", 386f, 68f, paint)
            canvas.drawText("Durum: ${order.status.labelTr.uppercase()}", 386f, 84f, paint)

            // Müşteri Bilgileri Kartı
            var y = 140f
            paint.color = AndroidColor.WHITE
            canvas.drawRoundRect(RectF(36f, y, 559f, y + 118f), 14f, 14f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = AndroidColor.parseColor("#E2E8F0")
            canvas.drawRoundRect(RectF(36f, y, 559f, y + 118f), 14f, 14f, paint)
            paint.style = Paint.Style.FILL

            paint.color = AndroidColor.parseColor("#2563EB")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 12f
            canvas.drawText("MÜŞTERİ VE TESLİMAT BİLGİLERİ", 52f, y + 24f, paint)

            paint.color = AndroidColor.parseColor("#0F172A")
            paint.textSize = 13f
            canvas.drawText("Ad Soyad: ${order.customerName}", 52f, y + 46f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 12f
            canvas.drawText("Telefon: ${order.customerPhone}", 320f, y + 46f, paint)

            paint.color = AndroidColor.parseColor("#334155")
            val addrText = "Adres: ${order.customerAddress.ifBlank { "Belirtilmedi" }}"
            canvas.drawText(addrText.take(80), 52f, y + 70f, paint)

            val noteText = "Sipariş Notu: ${order.customerNote.ifBlank { "-" }}"
            paint.color = AndroidColor.parseColor("#64748B")
            canvas.drawText(noteText.take(85), 52f, y + 94f, paint)

            // Ürün Tablosu Başlığı
            y = 280f
            paint.color = AndroidColor.parseColor("#0F172A")
            canvas.drawRoundRect(RectF(36f, y, 559f, y + 32f), 8f, 8f, paint)
            paint.color = AndroidColor.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 11f
            canvas.drawText("ÜRÜN ADI & VARYANT", 50f, y + 20f, paint)
            canvas.drawText("ADET", 355f, y + 20f, paint)
            canvas.drawText("BİRİM FİYAT", 415f, y + 20f, paint)
            canvas.drawText("TUTAR", 500f, y + 20f, paint)

            y += 38f
            order.items.forEachIndexed { idx, item ->
                val rowHeight = 46f
                paint.color = if (idx % 2 == 0) AndroidColor.WHITE else AndroidColor.parseColor("#F1F5F9")
                canvas.drawRoundRect(RectF(36f, y, 559f, y + rowHeight), 6f, 6f, paint)

                paint.color = AndroidColor.parseColor("#0F172A")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 12f
                canvas.drawText(item.productName.take(40), 50f, y + 19f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = AndroidColor.parseColor("#64748B")
                paint.textSize = 10f
                val variantLine = item.selectedVariantSummary.ifBlank { "Standart Seçenek" }
                canvas.drawText(variantLine.take(52), 50f, y + 35f, paint)

                paint.color = AndroidColor.parseColor("#0F172A")
                paint.textSize = 12f
                canvas.drawText("${item.quantity} adet", 355f, y + 26f, paint)
                canvas.drawText("${item.unitPrice.roundToInt()} ${order.currency}", 415f, y + 26f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val lineTotal = (item.unitPrice * item.quantity).roundToInt()
                canvas.drawText("$lineTotal ${order.currency}", 495f, y + 26f, paint)

                y += rowHeight + 4f
            }

            // Genel Toplam Kutusu
            y += 14f
            paint.color = AndroidColor.parseColor("#EFF6FF")
            canvas.drawRoundRect(RectF(310f, y, 559f, y + 54f), 12f, 12f, paint)
            paint.style = Paint.Style.STROKE
            paint.color = AndroidColor.parseColor("#BFDBFE")
            canvas.drawRoundRect(RectF(310f, y, 559f, y + 54f), 12f, 12f, paint)
            paint.style = Paint.Style.FILL

            paint.color = AndroidColor.parseColor("#1E3A8A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 13f
            canvas.drawText("GENEL TOPLAM:", 328f, y + 32f, paint)

            paint.color = AndroidColor.parseColor("#2563EB")
            paint.textSize = 18f
            canvas.drawText("${order.totalAmount.roundToInt()} ${order.currency}", 450f, y + 33f, paint)

            // Alt Bilgi
            paint.color = AndroidColor.parseColor("#94A3B8")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 10f
            canvas.drawText(
                "Bu belge KATALOG YAP dijital katalog sistemi üzerinden otomatik oluşturulmuştur.",
                36f,
                pageHeight - 28f,
                paint
            )

            pdfDocument.finishPage(page)

            val exportDir = File(context.cacheDir, "shared_exports").apply { mkdirs() }
            val cleanNo = order.orderNumber.replace("[^A-Za-z0-9]".toRegex(), "").lowercase()
            val outFile = File(exportDir, "siparis_fisi_${cleanNo}.pdf")
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            outFile
        }.getOrNull()
    }

    fun shareInvoicePdf(context: Context, file: File, order: Order) {
        ShareExportGenerator.shareFileViaSystemSheet(
            context = context,
            file = file,
            mimeType = "application/pdf",
            chooserTitle = "${order.orderNumber} Sipariş Fişi / Fatura",
            extraText = "${order.orderNumber} numaralı sipariş fişi (${order.customerName})"
        )
    }
}
