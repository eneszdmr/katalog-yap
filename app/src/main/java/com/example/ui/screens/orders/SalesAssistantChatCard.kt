package com.example.ui.screens.orders

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Order
import com.example.services.GeminiProductService
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYButton
import com.example.ui.components.KYCard
import com.example.ui.components.KYInput
import com.example.ui.components.KYToastData
import com.example.ui.components.KYToastType
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 3) SATIŞ ASİSTANI (Gemini AI):
 * Siparişler ekranında müşteri mesajına veya sipariş notuna cevap önerisi üreten
 * küçük sohbet kutusu. Yükleme durumu, hata yönetimi ve tekrar deneme butonu içerir.
 */
@Composable
fun SalesAssistantChatCard(
    orders: List<Order>,
    focusedOrder: Order? = null,
    onShowToast: (KYToastData) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isExpanded by remember { mutableStateOf(true) }

    val defaultInitialMessage = remember(focusedOrder, orders) {
        focusedOrder?.customerNote?.takeIf { it.isNotBlank() }
            ?: orders.firstOrNull { it.customerNote.isNotBlank() }?.customerNote
            ?: "Merhaba, siparişim ne zaman kargoya verilir? Hediye paketi yapabilir misiniz?"
    }

    var customerMessage by remember(defaultInitialMessage) { mutableStateOf(defaultInitialMessage) }
    var suggestedReply by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val quickSampleQuestions = remember(orders, focusedOrder) {
        buildList {
            if (focusedOrder != null && focusedOrder.customerNote.isNotBlank()) {
                add("Sipariş Notu: ${focusedOrder.customerNote}")
            }
            orders.filter { it.customerNote.isNotBlank() }.take(2).forEach { ord ->
                add("${ord.customerName}: ${ord.customerNote}")
            }
            add("Kargom ne zaman çıkar?")
            add("Beden değişimi yapabilir miyim?")
            add("Toplu alımda indirim var mı?")
        }.distinct()
    }

    fun buildOrderContextSummary(): String {
        val target = focusedOrder ?: orders.firstOrNull()
        return if (target != null) {
            val itemsSummary = target.items.joinToString(", ") { "${it.quantity}x ${it.productName}" }
            "Sipariş No: ${target.orderNumber}, Müşteri: ${target.customerName}, Durum: ${target.status.labelTr}, Ürünler: $itemsSummary, Toplam: ${target.totalAmount.roundToInt()} ${target.currency}"
        } else {
            "Aktif sipariş sayısı: ${orders.size}. Standart kargo süresi 1-2 iş günüdür, değişim ve hediye paketi desteğimiz mevcuttur."
        }
    }

    fun triggerGenerateReply() {
        if (isLoading) return
        scope.launch {
            isLoading = true
            errorMessage = null
            val result = GeminiProductService.generateSalesAssistantReply(
                customerMessage = customerMessage,
                orderContextSummary = buildOrderContextSummary()
            )
            isLoading = false

            result.onSuccess { reply ->
                suggestedReply = reply
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Cevap önerisi oluşturulamadı."
            }
        }
    }

    KYCard(
        modifier = modifier.fillMaxWidth(),
        tag = "sales_assistant_chat_card"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Üst Başlık Çubuğu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlueSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "✨ AI Satış Asistanı",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Müşteri mesajına saniyeler içinde cevap önerisi üretin",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("btn_toggle_sales_assistant")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (isExpanded) "Daralt" else "Genişlet",
                        tint = TextSecondary
                    )
                }
            }

            if (isExpanded) {
                HorizontalDivider(color = BorderLight)

                // Hızlı Müşteri Sorusu / Sipariş Notu Çipleri
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickSampleQuestions.forEachIndexed { idx, sample ->
                        Surface(
                            onClick = {
                                customerMessage = sample.substringAfter(": ").ifBlank { sample }
                                errorMessage = null
                            },
                            shape = BadgeRoundedPill,
                            color = SurfaceMuted,
                            border = BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.testTag("sales_assistant_sample_chip_$idx")
                        ) {
                            Text(
                                text = sample,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Müşteri Mesajı Giriş Kutusu + Üret Butonu
                KYInput(
                    value = customerMessage,
                    onValueChange = {
                        customerMessage = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = "Müşteri Mesajı veya Sorusu",
                    placeholder = "Müşterinin yazdığı mesajı buraya girin...",
                    singleLine = false,
                    maxLines = 3,
                    tag = "input_sales_assistant_message"
                )

                KYButton(
                    text = if (isLoading) {
                        "Cevap Üretiliyor..."
                    } else {
                        "✨ Cevap Önerisi Üret"
                    },
                    onClick = { triggerGenerateReply() },
                    variant = ButtonVariant.PRIMARY,
                    leadingIcon = Icons.AutoMirrored.Outlined.Send,
                    enabled = !isLoading && customerMessage.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    tag = "btn_generate_sales_reply"
                )

                // Yükleme Durumu (Loading State)
                if (isLoading) {
                    Surface(
                        shape = ButtonRoundedXl,
                        color = PrimaryBlueSubtle,
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sales_assistant_loading")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PrimaryBlue,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Gemini AI müşteri mesajına uygun cevap önerisi hazırlıyor...",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = PrimaryBlue
                            )
                        }
                    }
                }

                // Hata Durumu ve Tekrar Dene Butonu (Error Handling + Retry)
                if (errorMessage != null) {
                    Surface(
                        shape = ButtonRoundedXl,
                        color = DangerRedLight,
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sales_assistant_error_banner")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = DangerRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DangerRed,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            KYButton(
                                text = "Tekrar Dene",
                                onClick = { triggerGenerateReply() },
                                variant = ButtonVariant.SECONDARY,
                                leadingIcon = Icons.Outlined.Refresh,
                                modifier = Modifier.fillMaxWidth(),
                                tag = "btn_retry_sales_reply"
                            )
                        }
                    }
                }

                // Üretilen Cevap Önerisi Kutusu
                val currentReply = suggestedReply
                if (!currentReply.isNullOrBlank() && !isLoading) {
                    Surface(
                        shape = ButtonRoundedXl,
                        color = SuccessGreenLight.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sales_assistant_reply_box")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Önerilen Müşteri Yanıtı",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SuccessGreen
                                )
                                Surface(
                                    onClick = { triggerGenerateReply() },
                                    shape = BadgeRoundedPill,
                                    color = SurfaceWhite,
                                    border = BorderStroke(1.dp, BorderLight),
                                    modifier = Modifier.testTag("btn_regenerate_sales_reply")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Refresh,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Yeniden Üret",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = PrimaryBlue
                                        )
                                    }
                                }
                            }

                            Text(
                                text = currentReply,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                KYButton(
                                    text = "Kopyala",
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("Müşteri Yanıtı", currentReply))
                                        onShowToast(
                                            KYToastData(
                                                message = "Cevap önerisi panoya kopyalandı",
                                                type = KYToastType.SUCCESS
                                            )
                                        )
                                    },
                                    variant = ButtonVariant.SECONDARY,
                                    leadingIcon = Icons.Outlined.ContentCopy,
                                    modifier = Modifier.weight(1f),
                                    tag = "btn_copy_sales_reply"
                                )

                                KYButton(
                                    text = "WhatsApp'ta Gönder",
                                    onClick = {
                                        val targetPhone = (focusedOrder?.customerPhone ?: orders.firstOrNull()?.customerPhone)
                                            .orEmpty()
                                            .filter { it.isDigit() }
                                        val encoded = Uri.encode(currentReply)
                                        val waUrl = if (targetPhone.isNotBlank()) {
                                            "https://wa.me/$targetPhone?text=$encoded"
                                        } else {
                                            "https://wa.me/?text=$encoded"
                                        }
                                        runCatching {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        }
                                    },
                                    variant = ButtonVariant.PRIMARY,
                                    leadingIcon = Icons.AutoMirrored.Outlined.Chat,
                                    modifier = Modifier.weight(1f),
                                    tag = "btn_whatsapp_sales_reply"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
