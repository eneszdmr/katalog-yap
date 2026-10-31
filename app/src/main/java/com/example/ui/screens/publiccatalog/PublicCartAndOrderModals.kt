package com.example.ui.screens.publiccatalog

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.ShoppingBag
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.i18n.TrStrings
import com.example.model.CustomerCartItem
import com.example.model.Order
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYBottomSheet
import com.example.ui.components.KYButton
import com.example.ui.components.KYEmptyState
import com.example.ui.components.KYInput
import com.example.ui.components.KYModal
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

/**
 * SEPET ALT ÇEKMECESİ:
 * - Ürünler, varyant bilgisi, adet artır/azalt (+ / -), toplam tutar ve "Siparişi Gönder" butonu
 */
@Composable
fun PublicCartBottomSheet(
    isVisible: Boolean,
    cartItems: List<CustomerCartItem>,
    currencySymbol: String,
    onUpdateQuantity: (key: String, newQty: Int) -> Unit,
    onProceedToOrderForm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val totalAmount = remember(cartItems) {
        cartItems.sumOf { it.unitPrice * it.quantity }
    }
    val totalItemCount = remember(cartItems) {
        cartItems.sumOf { it.quantity }
    }

    KYBottomSheet(
        isVisible = true,
        title = "${TrStrings.PublicCatalog.CART_TITLE} ($totalItemCount)",
        subtitle = "Seçtiğiniz ürünleri kontrol edin ve siparişi tamamlayın",
        onDismiss = onDismiss
    ) {
        if (cartItems.isEmpty()) {
            KYEmptyState(
                icon = Icons.Outlined.ShoppingBag,
                title = TrStrings.PublicCatalog.CART_EMPTY_TITLE,
                description = TrStrings.PublicCatalog.CART_EMPTY_DESC
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("public_cart_sheet"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cartItems, key = { it.key }) { item ->
                        Surface(
                            shape = ButtonRoundedXl,
                            color = SurfaceWhite,
                            border = BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = item.product.imageUrl,
                                    contentDescription = item.product.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(ButtonRoundedXl)
                                        .background(SurfaceMuted)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.product.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.variantSummary.isNotBlank()) {
                                        Text(
                                            text = item.variantSummary,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${(item.unitPrice * item.quantity).roundToInt()} $currencySymbol",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = PrimaryBlue
                                    )
                                }

                                // Adet Artır / Azalt (- / +)
                                Surface(
                                    shape = ButtonRoundedXl,
                                    color = SurfaceMuted
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onUpdateQuantity(item.key, item.quantity - 1) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("cart_qty_dec_${item.product.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (item.quantity <= 1) {
                                                    Icons.Outlined.DeleteOutline
                                                } else {
                                                    Icons.Outlined.Remove
                                                },
                                                contentDescription = "Azalt",
                                                tint = if (item.quantity <= 1) DangerRed else TextPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = item.quantity.toString(),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        IconButton(
                                            onClick = { onUpdateQuantity(item.key, item.quantity + 1) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("cart_qty_inc_${item.product.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = "Artır",
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderLight)

                // Toplam Tutar Şeridi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = TrStrings.PublicCatalog.TOTAL_AMOUNT_LABEL,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${totalAmount.roundToInt()} $currencySymbol",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryBlue
                    )
                }

                // "Siparişi Gönder" Butonu
                KYButton(
                    text = TrStrings.PublicCatalog.BTN_PROCEED_TO_ORDER,
                    onClick = onProceedToOrderForm,
                    variant = ButtonVariant.PRIMARY,
                    leadingIcon = Icons.Outlined.Send,
                    modifier = Modifier.fillMaxWidth(),
                    tag = "btn_cart_proceed_to_order"
                )
            }
        }
    }
}

/**
 * SİPARİŞ FORMU ALT ÇEKMECESİ:
 * - Ad soyad, telefon, adres, not
 * - Gönderilince siparişi "Yeni" durumuyla kaydeder, WhatsApp mesajını oluşturup wa.me ile açar ve başarı ekranını gösterir.
 */
@Composable
fun PublicOrderFormBottomSheet(
    isVisible: Boolean,
    cartItems: List<CustomerCartItem>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSubmitOrder: (fullName: String, phone: String, address: String, note: String) -> Unit
) {
    if (!isVisible) return

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }

    val totalAmount = remember(cartItems) {
        cartItems.sumOf { it.unitPrice * it.quantity }
    }

    KYBottomSheet(
        isVisible = true,
        title = TrStrings.PublicCatalog.ORDER_FORM_TITLE,
        subtitle = TrStrings.PublicCatalog.ORDER_FORM_SUBTITLE,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .testTag("public_order_form_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KYInput(
                value = fullName,
                onValueChange = {
                    fullName = it
                    if (nameError != null && it.isNotBlank()) nameError = null
                },
                label = TrStrings.PublicCatalog.INPUT_FULL_NAME,
                placeholder = TrStrings.PublicCatalog.PLACEHOLDER_FULL_NAME,
                leadingIcon = Icons.Outlined.Person,
                errorText = nameError,
                tag = "input_order_customer_name"
            )

            KYInput(
                value = phone,
                onValueChange = {
                    phone = it
                    if (phoneError != null && it.isNotBlank()) phoneError = null
                },
                label = TrStrings.PublicCatalog.INPUT_PHONE,
                placeholder = TrStrings.PublicCatalog.PLACEHOLDER_PHONE,
                leadingIcon = Icons.Outlined.Phone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                visualTransformation = com.example.util.PhoneVisualTransformation(),
                errorText = phoneError,
                tag = "input_order_customer_phone"
            )

            KYInput(
                value = address,
                onValueChange = {
                    address = it
                    if (addressError != null && it.isNotBlank()) addressError = null
                },
                label = TrStrings.PublicCatalog.INPUT_ADDRESS,
                placeholder = TrStrings.PublicCatalog.PLACEHOLDER_ADDRESS,
                leadingIcon = Icons.Outlined.LocationOn,
                singleLine = false,
                minLines = 2,
                maxLines = 4,
                errorText = addressError,
                tag = "input_order_customer_address"
            )

            KYInput(
                value = note,
                onValueChange = { note = it },
                label = TrStrings.PublicCatalog.INPUT_NOTE,
                placeholder = TrStrings.PublicCatalog.PLACEHOLDER_NOTE,
                singleLine = false,
                minLines = 2,
                maxLines = 3,
                tag = "input_order_customer_note"
            )

            // Sipariş Özeti Kutusu
            Surface(
                shape = ButtonRoundedXl,
                color = PrimaryBlueSubtle,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${cartItems.sumOf { it.quantity }} Ürün • Sipariş Toplamı",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "${totalAmount.roundToInt()} $currencySymbol",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryBlue
                    )
                }
            }

            KYButton(
                text = TrStrings.PublicCatalog.BTN_SUBMIT_ORDER,
                onClick = {
                    var valid = true
                    if (fullName.trim().length < 2) {
                        nameError = TrStrings.PublicCatalog.ERROR_NAME_REQUIRED
                        valid = false
                    }
                    if (phone.trim().length < 7) {
                        phoneError = TrStrings.PublicCatalog.ERROR_PHONE_REQUIRED
                        valid = false
                    }
                    if (address.trim().length < 4) {
                        addressError = TrStrings.PublicCatalog.ERROR_ADDRESS_REQUIRED
                        valid = false
                    }
                    if (valid) {
                        onSubmitOrder(
                            fullName.trim(),
                            phone.trim(),
                            address.trim(),
                            note.trim()
                        )
                    }
                },
                variant = ButtonVariant.PRIMARY,
                leadingIcon = Icons.Outlined.Send,
                modifier = Modifier.fillMaxWidth(),
                tag = "btn_submit_customer_order"
            )
        }
    }
}

/**
 * SİPARİŞ BAŞARI EKRANI MODALI:
 * - Sipariş durumu ("Yeni"), sipariş numarası, WhatsApp mesaj özeti ve tekrar gönderme butonu.
 */
@Composable
fun PublicOrderSuccessModal(
    order: Order?,
    whatsappMessage: String,
    onReopenWhatsApp: () -> Unit,
    onCloseToCatalog: () -> Unit
) {
    if (order == null) return

    KYModal(
        isVisible = true,
        title = TrStrings.PublicCatalog.SUCCESS_TITLE,
        description = TrStrings.PublicCatalog.SUCCESS_DESC,
        confirmText = TrStrings.PublicCatalog.BTN_BACK_TO_CATALOG,
        onConfirm = onCloseToCatalog,
        onDismiss = onCloseToCatalog
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("order_success_modal"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SuccessGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(36.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KYBadge(
                    text = "Sipariş #${order.id.takeLast(6).uppercase()}",
                    variant = BadgeVariant.PRIMARY
                )
                KYBadge(
                    text = "Durum: ${order.status.labelTr}",
                    variant = BadgeVariant.SUCCESS
                )
            }

            Surface(
                shape = ButtonRoundedXl,
                color = SurfaceMuted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = whatsappMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(12.dp)
                )
            }

            KYButton(
                text = TrStrings.PublicCatalog.BTN_REOPEN_WHATSAPP,
                onClick = onReopenWhatsApp,
                variant = ButtonVariant.SECONDARY,
                leadingIcon = Icons.Outlined.Send,
                modifier = Modifier.fillMaxWidth(),
                tag = "btn_reopen_order_whatsapp"
            )
        }
    }
}

/**
 * Satıcıya gidecek hazır WhatsApp sipariş mesajını oluşturur ve wa.me ile açar.
 */
object CustomerWhatsAppOrderHelper {

    fun buildSellerOrderWhatsAppMessage(
        storeName: String,
        catalogTitle: String,
        order: Order
    ): String {
        return buildString {
            append("🛍️ *YENİ SİPARİŞ — $storeName*\n")
            append("📒 Katalog: $catalogTitle\n")
            append("🔖 Sipariş No: #${order.id.takeLast(6).uppercase()}\n\n")
            append("📦 *Sipariş Edilen Ürünler:*\n")
            order.items.forEachIndexed { idx, item ->
                val lineTotal = (item.unitPrice * item.quantity).roundToInt()
                val variantPart = if (item.selectedVariantSummary.isNotBlank()) {
                    " (${item.selectedVariantSummary})"
                } else ""
                append("${idx + 1}. ${item.productName}$variantPart\n")
                append("   ${item.quantity} adet x ${item.unitPrice.roundToInt()} ${order.currency} = *$lineTotal ${order.currency}*\n")
            }
            append("\n💰 *TOPLAM TUTAR: ${order.totalAmount.roundToInt()} ${order.currency}*\n\n")
            append("👤 *Müşteri Bilgileri:*\n")
            append("• Ad Soyad: ${order.customerName}\n")
            append("• Telefon: ${order.customerPhone}\n")
            append("• Adres: ${order.customerAddress}\n")
            if (order.customerNote.isNotBlank()) {
                append("• Not: ${order.customerNote}\n")
            }
        }
    }

    fun openWhatsAppToSeller(
        context: Context,
        sellerWhatsappPhone: String,
        message: String
    ) {
        val cleanPhone = sellerWhatsappPhone.filter { it.isDigit() }.ifBlank { "905324102030" }
        val encoded = Uri.encode(message)
        val waUri = Uri.parse("https://wa.me/$cleanPhone?text=$encoded")
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
            context.startActivity(Intent.createChooser(fallback, "WhatsApp Siparişi Gönder"))
        }
    }
}
