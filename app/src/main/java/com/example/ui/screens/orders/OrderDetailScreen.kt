package com.example.ui.screens.orders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.auth.LocalAuthContext
import com.example.i18n.TrStrings
import com.example.model.Order
import com.example.model.OrderItem
import com.example.model.OrderStatus
import com.example.model.OrderTimelineEntry
import com.example.services.LocalStorageDataService
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYButton
import com.example.ui.components.KYCard
import com.example.ui.components.KYEmptyState
import com.example.ui.components.KYModal
import com.example.ui.components.KYToastData
import com.example.ui.components.KYToastType
import com.example.ui.components.KYTopBar
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentAmberDark
import com.example.ui.theme.AccentAmberLight
import com.example.ui.theme.BackgroundSlate
import com.example.ui.theme.BadgeRoundedPill
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * SİPARİŞ DETAYI EKRANI (/orders/:id):
 * - Ürün listesi (görsel, ad, varyant, adet, fiyat) + Genel Toplam
 * - Müşteri bilgileri (ad soyad, telefon), teslimat adresi, sipariş notu
 * - Durum değiştirme dropdown'u (değişince zaman çizelgesine kayıt eklenir)
 * - Zaman çizelgesi (Sipariş geçmişi ve durum güncellemeleri)
 * - Aksiyonlar: "Müşteriye WhatsApp yaz", "Faturayı/Fişi PDF indir", "Sipariş iptal et"
 */
@Composable
fun OrderDetailScreen(
    orderId: String,
    onBackClick: () -> Unit,
    onShowToast: (KYToastData) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    val dataService = remember { LocalStorageDataService.getInstance(context) }
    val scope = rememberCoroutineScope()
    val authContext = LocalAuthContext.current
    val store = authContext.storeSettings

    val orders by dataService.orders.collectAsState()
    val order = remember(orders, orderId) {
        orders.find { it.id == orderId || it.orderNumber.equals(orderId, ignoreCase = true) }
    }

    var showStatusDropdown by remember { mutableStateOf(false) }
    var showCancelConfirmModal by remember { mutableStateOf(false) }
    var isGeneratingInvoice by remember { mutableStateOf(false) }

    if (order == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundSlate)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            KYEmptyState(
                icon = Icons.Outlined.ShoppingBag,
                title = "Sipariş bulunamadı",
                description = "Bu sipariş silinmiş veya mevcut değil.",
                actionLabel = TrStrings.Navigation.BACK,
                onActionClick = onBackClick
            )
        }
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_order_detail"),
        containerColor = BackgroundSlate,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            KYTopBar(
                title = "${TrStrings.Orders.DETAIL_TITLE} ${order.orderNumber}",
                subtitle = "${formatOrderDateTr(order.createdAt)} • ${order.catalogTitle}",
                onBackClick = onBackClick,
                rightAction = {
                    OrderStatusBadge(
                        status = order.status,
                        tag = "order_detail_header_badge"
                    )
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1) DURUM DEĞİŞTİRME DROPDOWN KARTI (Değişince zaman çizelgesine kayıt eklenir)
            item {
                KYCard(tag = "order_status_management_card") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = TrStrings.Orders.SECTION_STATUS_CHANGE,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = TrStrings.Orders.TIMELINE_SUBTITLE,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Durum Değiştirme Dropdown Seçicisi
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val currentVisual = getOrderStatusVisual(order.status)
                        Surface(
                            onClick = { showStatusDropdown = true },
                            shape = ButtonRoundedXl,
                            color = currentVisual.containerColor,
                            border = BorderStroke(1.5.dp, currentVisual.borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dropdown_order_status_trigger")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = currentVisual.icon,
                                        contentDescription = null,
                                        tint = currentVisual.textColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = TrStrings.Orders.STATUS_DROPDOWN_LABEL,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = order.status.labelTr,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = currentVisual.textColor
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = TrStrings.Orders.STATUS_DROPDOWN_LABEL,
                                    tint = currentVisual.textColor
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showStatusDropdown,
                            onDismissRequest = { showStatusDropdown = false },
                            modifier = Modifier
                                .background(SurfaceWhite)
                                .testTag("dropdown_order_status_menu")
                        ) {
                            OrderStatus.entries.forEach { statusOption ->
                                val isCurrent = order.status == statusOption
                                val optVisual = getOrderStatusVisual(statusOption)
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = optVisual.icon,
                                            contentDescription = null,
                                            tint = optVisual.textColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = statusOption.labelTr,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isCurrent) optVisual.textColor else TextPrimary
                                        )
                                    },
                                    trailingIcon = {
                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = optVisual.textColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        showStatusDropdown = false
                                        if (statusOption != order.status) {
                                            scope.launch {
                                                dataService.updateOrderStatus(order.id, statusOption)
                                                onShowToast(
                                                    KYToastData(
                                                        message = "${TrStrings.Orders.TOAST_STATUS_UPDATED}: ${statusOption.labelTr}",
                                                        type = KYToastType.SUCCESS
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("status_option_${statusOption.name}")
                                )
                            }
                        }
                    }

                    // Bayi üzerinden gelen siparişlerde "Bayi" rozeti / bilgi kutusu
                    if (order.isResellerOrder) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = ButtonRoundedXl,
                            color = AccentAmberLight,
                            border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("order_detail_reseller_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = BadgeRoundedPill,
                                    color = AccentAmberDark
                                ) {
                                    Text(
                                        text = TrStrings.Resellers.ORDER_BADGE_RESELLER,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SurfaceWhite,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (order.resellerName.isNotBlank()) {
                                        "Bu sipariş '${order.resellerName}' bayisine özel katalog linki üzerinden oluşturuldu."
                                    } else {
                                        "Bu sipariş bayi katalog linki üzerinden oluşturuldu."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Otomatik Stok Düşümü Bilgisi
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = ButtonRoundedXl,
                        color = if (order.stockDeducted) SuccessGreenLight else SurfaceMuted,
                        border = BorderStroke(
                            1.dp,
                            if (order.stockDeducted) SuccessGreen.copy(alpha = 0.3f) else BorderLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Inventory2,
                                contentDescription = null,
                                tint = if (order.stockDeducted) SuccessGreen else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (order.stockDeducted) {
                                    TrStrings.Orders.STATUS_AUTO_STOCK_NOTE
                                } else {
                                    "Sipariş iptal edildiği için düşülen ürün stokları geri yüklendi."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (order.stockDeducted) SuccessGreen else TextSecondary
                            )
                        }
                    }
                }
            }

            // 2) SİPARİŞ EDİLEN ÜRÜN LİSTESİ (Görsel, Ad, Varyant, Adet, Fiyat) + TOPLAM
            item {
                OrderProductsCard(order = order)
            }

            // 3) MÜŞTERİ BİLGİLERİ, ADRES VE NOT
            item {
                OrderCustomerDetailsCard(order = order)
            }

            // 3.5) AI SATIŞ ASİSTANI (Müşteri Mesajına Cevap Önerisi Sohbet Kutusu)
            item {
                SalesAssistantChatCard(
                    orders = orders,
                    focusedOrder = order,
                    onShowToast = onShowToast
                )
            }

            // 4) SİPARİŞ AKSİYONLARI ("Müşteriye WhatsApp yaz", "Faturayı/Fişi PDF indir", "Sipariş iptal et")
            item {
                KYCard(tag = "order_actions_card") {
                    Text(
                        text = TrStrings.Orders.SECTION_ACTIONS,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Aksiyon: Müşteriye WhatsApp yaz
                    KYButton(
                        text = TrStrings.Orders.BTN_WHATSAPP_CUSTOMER,
                        onClick = {
                            OrderInvoiceHelper.openCustomerWhatsApp(
                                context = context,
                                order = order,
                                store = store
                            )
                        },
                        variant = ButtonVariant.PRIMARY,
                        leadingIcon = Icons.AutoMirrored.Outlined.Chat,
                        modifier = Modifier.fillMaxWidth(),
                        tag = "btn_order_whatsapp_customer"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Aksiyon: Faturayı/Fişi PDF indir
                    KYButton(
                        text = if (isGeneratingInvoice) "PDF Hazırlanıyor..." else TrStrings.Orders.BTN_DOWNLOAD_INVOICE_PDF,
                        onClick = {
                            if (!isGeneratingInvoice) {
                                scope.launch {
                                    isGeneratingInvoice = true
                                    val pdfFile = OrderInvoiceHelper.generateOrderInvoicePdf(
                                        context = context,
                                        order = order,
                                        store = store
                                    )
                                    isGeneratingInvoice = false
                                    if (pdfFile != null) {
                                        onShowToast(
                                            KYToastData(
                                                message = "${TrStrings.Orders.TOAST_INVOICE_DOWNLOADED} (${pdfFile.name})",
                                                type = KYToastType.SUCCESS
                                            )
                                        )
                                        OrderInvoiceHelper.shareInvoicePdf(
                                            context = context,
                                            file = pdfFile,
                                            order = order
                                        )
                                    }
                                }
                            }
                        },
                        variant = ButtonVariant.SECONDARY,
                        leadingIcon = Icons.Outlined.PictureAsPdf,
                        modifier = Modifier.fillMaxWidth(),
                        tag = "btn_order_download_invoice_pdf"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Aksiyon: Sipariş iptal et
                    val isCancelled = order.status == OrderStatus.CANCELLED
                    Surface(
                        onClick = {
                            if (!isCancelled) {
                                showCancelConfirmModal = true
                            }
                        },
                        enabled = !isCancelled,
                        shape = ButtonRoundedXl,
                        color = if (isCancelled) SurfaceMuted else SurfaceWhite,
                        border = BorderStroke(
                            1.dp,
                            if (isCancelled) BorderLight else DangerRed.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_order_cancel")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 13.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Cancel,
                                contentDescription = null,
                                tint = if (isCancelled) TextSecondary else DangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCancelled) {
                                    TrStrings.Orders.BTN_ALREADY_CANCELLED
                                } else {
                                    TrStrings.Orders.BTN_CANCEL_ORDER
                                },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (isCancelled) TextSecondary else DangerRed
                            )
                        }
                    }
                }
            }

            // 5) SİPARİŞ ZAMAN ÇİZELGESİ (Timeline)
            item {
                OrderTimelineCard(timeline = order.timeline)
            }
        }
    }

    // Sipariş İptal Onay Modalı
    KYModal(
        isVisible = showCancelConfirmModal,
        title = TrStrings.Orders.MODAL_CANCEL_TITLE,
        description = TrStrings.Orders.MODAL_CANCEL_DESC,
        confirmText = TrStrings.Orders.MODAL_CANCEL_CONFIRM,
        cancelText = TrStrings.Common.CANCEL,
        onConfirm = {
            showCancelConfirmModal = false
            scope.launch {
                dataService.updateOrderStatus(order.id, OrderStatus.CANCELLED)
                onShowToast(
                    KYToastData(
                        message = TrStrings.Orders.TOAST_ORDER_CANCELLED,
                        type = KYToastType.INFO
                    )
                )
            }
        },
        onDismiss = { showCancelConfirmModal = false }
    )
}

@Composable
private fun OrderProductsCard(order: Order) {
    val totalItemQuantity = order.items.sumOf { it.quantity }
    KYCard(tag = "order_products_card") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = TrStrings.Orders.SECTION_PRODUCTS,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                text = "$totalItemQuantity ${TrStrings.Orders.ITEM_COUNT_SUFFIX}",
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryBlue
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            order.items.forEachIndexed { idx, item ->
                OrderProductRowItem(
                    item = item,
                    currency = order.currency,
                    index = idx
                )
                if (idx < order.items.lastIndex) {
                    HorizontalDivider(color = SurfaceMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = BorderLight)
        Spacer(modifier = Modifier.height(12.dp))

        // Genel Toplam Satırı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ButtonRoundedXl)
                .background(PrimaryBlueSubtle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = TrStrings.Orders.LABEL_TOTAL_AMOUNT,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                text = "${order.totalAmount.roundToInt()} ${order.currency}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = PrimaryBlue,
                modifier = Modifier.testTag("order_detail_total_amount")
            )
        }
    }
}

@Composable
private fun OrderProductRowItem(
    item: OrderItem,
    currency: String,
    index: Int
) {
    val lineTotal = (item.unitPrice * item.quantity).roundToInt()
    val effectiveImg = item.imageUrl.ifBlank {
        "https://picsum.photos/seed/${item.productId.ifBlank { "prod-$index" }}/300/300"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_product_row_$index"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = effectiveImg,
            contentDescription = item.productName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(ButtonRoundedXl)
                .background(SurfaceMuted)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.productName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.selectedVariantSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.selectedVariantSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.quantity} adet × ${item.unitPrice.roundToInt()} $currency",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "$lineTotal $currency",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = PrimaryBlue
        )
    }
}

@Composable
private fun OrderCustomerDetailsCard(order: Order) {
    KYCard(tag = "order_customer_info_card") {
        Text(
            text = TrStrings.Orders.SECTION_CUSTOMER,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        CustomerMetaRow(
            icon = Icons.Outlined.Person,
            label = TrStrings.Orders.LABEL_CUSTOMER_NAME,
            value = order.customerName
        )
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = SurfaceMuted)
        Spacer(modifier = Modifier.height(10.dp))

        CustomerMetaRow(
            icon = Icons.Outlined.Phone,
            label = TrStrings.Orders.LABEL_CUSTOMER_PHONE,
            value = order.customerPhone
        )
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = SurfaceMuted)
        Spacer(modifier = Modifier.height(10.dp))

        CustomerMetaRow(
            icon = Icons.Outlined.LocationOn,
            label = TrStrings.Orders.LABEL_CUSTOMER_ADDRESS,
            value = order.customerAddress.ifBlank { "Teslimat adresi belirtilmedi" }
        )
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = SurfaceMuted)
        Spacer(modifier = Modifier.height(10.dp))

        CustomerMetaRow(
            icon = Icons.AutoMirrored.Outlined.Notes,
            label = TrStrings.Orders.LABEL_CUSTOMER_NOTE,
            value = order.customerNote.ifBlank { TrStrings.Orders.LABEL_NO_NOTE }
        )
    }
}

@Composable
private fun CustomerMetaRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(PrimaryBlueSubtle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun OrderTimelineCard(timeline: List<OrderTimelineEntry>) {
    val sortedTimeline = remember(timeline) {
        timeline.sortedByDescending { it.timestamp }
    }

    KYCard(tag = "order_timeline_card") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${TrStrings.Orders.SECTION_TIMELINE} (${sortedTimeline.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            sortedTimeline.forEachIndexed { index, entry ->
                val visual = getOrderStatusVisual(entry.status)
                val isLatest = index == 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timeline_entry_$index"),
                    verticalAlignment = Alignment.Top
                ) {
                    // Sol Zaman Çizelgesi Noktası ve Çizgisi
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(28.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isLatest) 24.dp else 18.dp)
                                .clip(CircleShape)
                                .background(if (isLatest) visual.containerColor else SurfaceMuted),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isLatest) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(visual.textColor)
                            )
                        }
                        if (index < sortedTimeline.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(38.dp)
                                    .background(BorderLight)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (isLatest) FontWeight.Bold else FontWeight.SemiBold
                                ),
                                color = if (isLatest) TextPrimary else TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatOrderDateTr(entry.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryBlue
                        )
                        if (entry.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = entry.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
