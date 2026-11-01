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
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.ui.theme.DangerRedLight
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
 * - Ürünler, kendi döviz tipleri (₺, $, €), adet artır/azalt (+ / -), stok aşım uyarısı,
 *   çoklu döviz toplam tutar (Örn: 100 $ + 250 ₺) ve "Siparişi Gönder" butonu
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

    val multiCurrencyTotalText = remember(cartItems, currencySymbol) {
        com.example.model.formatMultiCurrencyCartTotals(cartItems, currencySymbol)
    }
    val totalItemCount = remember(cartItems) {
        cartItems.sumOf { it.quantity }
    }
    val outOfStockItems = remember(cartItems) {
        cartItems.filter { it.isExceedingStock }
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
                if (outOfStockItems.isNotEmpty()) {
                    Surface(
                        shape = ButtonRoundedXl,
                        color = DangerRedLight,
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cart_stock_warning_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Stok Sınırı Aşıldı",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = DangerRed
                            )
                            outOfStockItems.forEach { overItem ->
                                Text(
                                    text = "• ${overItem.product.name}: Bu üründen istediğiniz sayıda yok (Mevcut stok: ${overItem.product.stockCount} adet, Sepette: ${overItem.quantity} adet).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DangerRed
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cartItems, key = { it.key }) { item ->
                        val itemCurrency = item.product.currency.ifBlank { currencySymbol }
                        Surface(
                            shape = ButtonRoundedXl,
                            color = if (item.isExceedingStock) DangerRedLight.copy(alpha = 0.35f) else SurfaceWhite,
                            border = BorderStroke(
                                1.dp,
                                if (item.isExceedingStock) DangerRed.copy(alpha = 0.55f) else BorderLight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(ButtonRoundedXl)
                                            .background(SurfaceMuted),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ShoppingBag,
                                            contentDescription = null,
                                            tint = TextSecondary.copy(alpha = 0.4f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        if (item.product.imageUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = item.product.imageUrl,
                                                contentDescription = item.product.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
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
                                            text = "${(item.unitPrice * item.quantity).roundToInt()} $itemCurrency",
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
                                                color = if (item.isExceedingStock) DangerRed else TextPrimary,
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

                                if (item.isExceedingStock) {
                                    Text(
                                        text = "⚠️ Bu üründen istediğiniz sayıda yok (Mevcut stok: ${item.product.stockCount} adet)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = DangerRed,
                                        modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderLight)

                // Toplam Tutar Şeridi (Çoklu Döviz Destekli: Örn 100 $ + 250 ₺)
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
                        text = multiCurrencyTotalText,
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
 * SİPARİŞ TALEBİ FORMU ALT ÇEKMECESİ:
 * - Seçilen ürünlerin listesi, adet (+ / -) düzenleme kontrolleri ve stok aşımı kontrolü
 * - Çoklu döviz toplam gösterimi (Örn: 100 $ + 250 ₺)
 * - Ad soyad, telefon, adres, not
 * - Gönderilince siparişi "Yeni" durumuyla kaydeder, WhatsApp mesajını oluşturup wa.me ile açar ve başarı ekranını gösterir.
 */
@Composable
fun PublicOrderFormBottomSheet(
    isVisible: Boolean,
    cartItems: List<CustomerCartItem>,
    currencySymbol: String,
    onUpdateQuantity: (key: String, newQty: Int) -> Unit = { _, _ -> },
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
    var stockErrorBanner by remember { mutableStateOf<String?>(null) }

    val multiCurrencyTotalText = remember(cartItems, currencySymbol) {
        com.example.model.formatMultiCurrencyCartTotals(cartItems, currencySymbol)
    }
    val outOfStockItems = remember(cartItems) {
        cartItems.filter { it.isExceedingStock }
    }
    val hasStockError = outOfStockItems.isNotEmpty()

    KYBottomSheet(
        isVisible = true,
        title = TrStrings.PublicCatalog.ORDER_FORM_TITLE,
        subtitle = TrStrings.PublicCatalog.ORDER_FORM_SUBTITLE,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .testTag("public_order_form_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1) SİPARİŞ EDİLECEK ÜRÜNLER VE ADET (+ / -) KONTROLLERİ
            Surface(
                shape = ButtonRoundedXl,
                color = SurfaceWhite,
                border = BorderStroke(1.dp, BorderLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("order_form_products_list")
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
                            text = "Sipariş Edilecek Ürünler (${cartItems.sumOf { it.quantity }} Adet)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = multiCurrencyTotalText,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryBlue
                        )
                    }

                    HorizontalDivider(color = BorderLight)

                    cartItems.forEach { item ->
                        val itemCurr = item.product.currency.ifBlank { currencySymbol }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("order_form_item_${item.product.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.product.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val variantText = if (item.variantSummary.isNotBlank()) " • ${item.variantSummary}" else ""
                                    Text(
                                        text = "${item.unitPrice.roundToInt()} $itemCurr x ${item.quantity} = ${(item.unitPrice * item.quantity).roundToInt()} $itemCurr$variantText",
                                        style = MaterialTheme.typography.labelSmall,
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
                                            onClick = {
                                                stockErrorBanner = null
                                                onUpdateQuantity(item.key, item.quantity - 1)
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("order_form_qty_dec_${item.product.id}")
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
                                            text = "${item.quantity} adet",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (item.isExceedingStock) DangerRed else TextPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        IconButton(
                                            onClick = {
                                                stockErrorBanner = null
                                                onUpdateQuantity(item.key, item.quantity + 1)
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("order_form_qty_inc_${item.product.id}")
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

                            if (item.isExceedingStock) {
                                Surface(
                                    shape = ButtonRoundedXl,
                                    color = DangerRedLight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .testTag("order_form_stock_error_${item.product.id}")
                                ) {
                                    Text(
                                        text = "⚠️ Bu üründen istediğiniz sayıda yok! (Stokta en fazla ${item.product.stockCount} adet mevcut, seçilen: ${item.quantity} adet)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = DangerRed,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (hasStockError || stockErrorBanner != null) {
                Surface(
                    shape = ButtonRoundedXl,
                    color = DangerRedLight,
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_form_stock_warning_banner")
                ) {
                    val msg = stockErrorBanner ?: outOfStockItems.joinToString("\n") { over ->
                        "• ${over.product.name}: Bu üründen istediğiniz sayıda yok (Mevcut stok: ${over.product.stockCount} adet)."
                    }
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = DangerRed,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

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

            // Sipariş Özeti Kutusu (Çoklu Döviz Destekli: Örn 100 $ + 250 ₺)
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
                        text = multiCurrencyTotalText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryBlue
                    )
                }
            }

            KYButton(
                text = TrStrings.PublicCatalog.BTN_SUBMIT_ORDER,
                onClick = {
                    var valid = true
                    if (cartItems.isEmpty()) {
                        stockErrorBanner = "Sepetinizde ürün bulunmuyor."
                        valid = false
                    } else if (hasStockError) {
                        val firstOver = outOfStockItems.first()
                        stockErrorBanner = "'${firstOver.product.name}' ürününden istediğiniz sayıda yok (Stokta en fazla ${firstOver.product.stockCount} adet mevcut). Lütfen adeti azaltın."
                        valid = false
                    }
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
                enabled = cartItems.isNotEmpty() && !hasStockError,
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
        order: Order,
        multiCurrencySummary: String = ""
    ): String {
        val groupedByCurrency = LinkedHashMap<String, Double>()
        order.items.forEach { item ->
            val curr = item.currency.ifBlank { order.currency.ifBlank { "₺" } }
            groupedByCurrency[curr] = (groupedByCurrency[curr] ?: 0.0) + (item.unitPrice * item.quantity)
        }
        val effectiveTotalLine = multiCurrencySummary.ifBlank {
            if (groupedByCurrency.isNotEmpty()) {
                groupedByCurrency.entries.joinToString(" + ") { (c, sum) -> "${sum.roundToInt()} $c" }
            } else {
                "${order.totalAmount.roundToInt()} ${order.currency}"
            }
        }
        return buildString {
            append("🛍️ *YENİ SİPARİŞ — $storeName*\n")
            append("📒 Katalog: $catalogTitle\n")
            append("🔖 Sipariş No: #${order.id.takeLast(6).uppercase()}\n\n")
            append("📦 *Sipariş Edilen Ürünler:*\n")
            order.items.forEachIndexed { idx, item ->
                val itemCurr = item.currency.ifBlank { order.currency.ifBlank { "₺" } }
                val lineTotal = (item.unitPrice * item.quantity).roundToInt()
                val variantPart = if (item.selectedVariantSummary.isNotBlank()) {
                    " (${item.selectedVariantSummary})"
                } else ""
                append("${idx + 1}. ${item.productName}$variantPart\n")
                append("   ${item.quantity} adet x ${item.unitPrice.roundToInt()} $itemCurr = *$lineTotal $itemCurr*\n")
            }
            append("\n💰 *TOPLAM TUTAR: $effectiveTotalLine*\n\n")
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
