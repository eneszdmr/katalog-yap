package com.example.ui.screens

import com.example.model.Product


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.i18n.TrStrings
import com.example.model.Order
import com.example.model.OrderItem
import com.example.model.OrderStatus
import com.example.services.LocalStorageDataService
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYButton
import com.example.ui.components.KYCard
import com.example.ui.components.KYInput
import com.example.ui.components.KYToastData
import com.example.ui.components.KYToastType
import com.example.ui.components.KYTopBar
import com.example.ui.screens.orders.OrderStatusBadge
import com.example.ui.screens.orders.SalesAssistantChatCard
import com.example.ui.screens.orders.formatOrderDateTr
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.BackgroundSlate
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.CardRounded2xl
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import androidx.compose.material.icons.outlined.Storefront
import com.example.ui.screens.resellers.ResellerSalesSummaryCard
import com.example.ui.theme.AccentAmberDark
import com.example.ui.theme.AccentAmberLight
import kotlin.math.roundToInt

/**
 * SİPARİŞLER SEKMESİ (/orders):
 * - Üstte durum filtreleri: Tümü, Yeni, Onaylandı, Hazırlanıyor, Kargoda, Teslim edildi, İptal + Bayi filtresi.
 * - Bayi bazında satış özeti ve bayi üzerinden gelen siparişlerde "Bayi" etiketi.
 * - Arama: Müşteri adı, bayi adı veya sipariş no.
 * - Sipariş kartı: Sipariş no, müşteri adı, tarih, ürün sayısı, toplam tutar, durum rozeti, Bayi rozeti.
 * - Karta tıklanınca Sipariş Detayı (/orders/:id) açılır.
 * - Yeni sipariş geldiğinde stok takibi açık ürünlerin stoğunu otomatik düşürür.
 * - Boş durum ekranı içerir.
 */
@Composable
fun OrdersDraftScreen(
    onOrderClick: (String) -> Unit = {},
    onOpenDesignSystem: () -> Unit = {},
    onShowToast: (KYToastData) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataService = remember { LocalStorageDataService.getInstance(context) }
    val scope = rememberCoroutineScope()

    val orders by dataService.orders.collectAsState()
    val products by dataService.products.collectAsState()
    val catalogs by dataService.catalogs.collectAsState()
    val resellers by dataService.resellers.collectAsState()

    // Seçili Durum Filtresi (null = "Tümü")
    var selectedStatusFilter by remember { mutableStateOf<OrderStatus?>(null) }
    var onlyResellerOrders by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Durum bazlı sipariş sayıları
    val statusCounts by remember(orders) {
        derivedStateOf {
            OrderStatus.entries.associateWith { st ->
                orders.count { it.status == st }
            }
        }
    }

    val resellerOrderCount by remember(orders) {
        derivedStateOf { orders.count { it.isResellerOrder } }
    }

    // Arama (müşteri adı, bayi adı veya sipariş no), bayi filtresi ve durum filtresine göre siparişler
    val filteredOrders by remember(orders, selectedStatusFilter, onlyResellerOrders, searchQuery) {
        derivedStateOf {
            val q = searchQuery.trim()
            orders.filter { order ->
                val matchesStatus = selectedStatusFilter == null || order.status == selectedStatusFilter
                val matchesReseller = !onlyResellerOrders || order.isResellerOrder
                val matchesSearch = q.isBlank() ||
                    order.customerName.contains(q, ignoreCase = true) ||
                    order.resellerName.contains(q, ignoreCase = true) ||
                    order.orderNumber.contains(q, ignoreCase = true) ||
                    order.id.contains(q, ignoreCase = true)
                matchesStatus && matchesReseller && matchesSearch
            }.sortedByDescending { it.createdAt }
        }
    }

    fun simulateIncomingCustomerOrder() {
        scope.launch {
            val targetProduct = products.firstOrNull { it.trackStock && it.stockCount > 0 }
                ?: products.firstOrNull()
            val targetCatalog = catalogs.find { it.id == targetProduct?.catalogId } ?: catalogs.firstOrNull()

            if (targetProduct != null) {
                val beforeStock = targetProduct.stockCount
                val randomSuffix = (1050..9999).random()
                val orderId = "SIP-$randomSuffix"
                val demoCustomers = listOf(
                    "Deniz Korkmaz" to "+90 532 614 88 21",
                    "Ece Aydın" to "+90 544 812 34 90",
                    "Kaan Çelik" to "+90 533 295 17 64"
                )
                val (custName, custPhone) = demoCustomers.random()

                val newOrder = Order(
                    id = orderId,
                    orderNumber = "#$orderId",
                    catalogId = targetCatalog?.id ?: targetProduct.catalogId,
                    catalogTitle = targetCatalog?.title ?: "2025 İlkbahar / Yaz Koleksiyonu",
                    customerName = custName,
                    customerPhone = custPhone,
                    customerAddress = "Bağdat Cad. Suadiye Mah. No: 74 Kadıköy / İstanbul",
                    customerNote = "Katalog linkinden hızlı sipariş olarak gönderildi.",
                    items = listOf(
                        OrderItem(
                            productId = targetProduct.id,
                            productName = targetProduct.name,
                            quantity = 1,
                            unitPrice = targetProduct.price,
                            selectedVariantSummary = "Beden: M • Renk: Ekru",
                            imageUrl = targetProduct.imageUrl
                        )
                    ),
                    totalAmount = targetProduct.price,
                    currency = targetProduct.currency,
                    status = OrderStatus.NEW,
                    createdAt = System.currentTimeMillis()
                )

                dataService.createCustomerOrder(newOrder)
                val afterStock = (beforeStock - 1).coerceAtLeast(0)
                onShowToast(
                    KYToastData(
                        message = "Yeni sipariş (#$orderId) geldi! '${targetProduct.name}' stoğu $beforeStock → $afterStock adede düşürüldü.",
                        type = KYToastType.SUCCESS
                    )
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundSlate)
            .testTag("screen_orders")
    ) {
        KYTopBar(
            title = TrStrings.Orders.TITLE,
            subtitle = "${orders.size} toplam sipariş • ${orders.count { it.status == OrderStatus.NEW }} yeni",
            rightAction = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Canlı Yeni Sipariş + Otomatik Stok Düşürme Test Butonu
                    Surface(
                        onClick = { simulateIncomingCustomerOrder() },
                        shape = ButtonRoundedXl,
                        color = PrimaryBlueSubtle,
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)),
                        modifier = Modifier.testTag("btn_simulate_new_order")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Test Siparişi",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue
                            )
                        }
                    }

                    if (orders.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    dataService.clearAllOrders()
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("btn_clear_orders_empty_state")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = TrStrings.Orders.BTN_CLEAR_ORDERS,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        )

        // ARAMA KUTUSU VE ÜST DURUM FİLTRELERİ
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Arama: Müşteri adı veya sipariş no
            KYInput(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = TrStrings.Orders.SEARCH_PLACEHOLDER,
                leadingIcon = Icons.Outlined.Search,
                tag = "input_search_orders"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Durum Filtreleri: Tümü, Yeni, Onaylandı, Hazırlanıyor, Kargoda, Teslim edildi, İptal
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("orders_status_filter_row")
            ) {
                // "Tümü" Çipi
                item(key = "ALL") {
                    val isSelected = selectedStatusFilter == null && !onlyResellerOrders
                    StatusFilterChip(
                        label = "${TrStrings.Orders.FILTER_ALL} (${orders.size})",
                        isSelected = isSelected,
                        onClick = {
                            selectedStatusFilter = null
                            onlyResellerOrders = false
                        },
                        tag = "order_filter_ALL"
                    )
                }

                // "Bayi" Siparişleri Filtre Çipi
                item(key = "RESELLER") {
                    StatusFilterChip(
                        label = "${TrStrings.Resellers.ORDER_BADGE_RESELLER} ($resellerOrderCount)",
                        isSelected = onlyResellerOrders,
                        onClick = { onlyResellerOrders = !onlyResellerOrders },
                        tag = "order_filter_RESELLER"
                    )
                }

                // 6 Durum Çipi (Yeni, Onaylandı, Hazırlanıyor, Kargoda, Teslim edildi, İptal)
                items(OrderStatus.entries, key = { it.name }) { status ->
                    val isSelected = selectedStatusFilter == status
                    val count = statusCounts[status] ?: 0
                    StatusFilterChip(
                        label = "${status.labelTr} ($count)",
                        isSelected = isSelected,
                        onClick = {
                            selectedStatusFilter = if (isSelected) null else status
                        },
                        tag = "order_filter_${status.name}"
                    )
                }
            }
        }

        // SİPARİŞ LİSTESİ VEYA BOŞ DURUM EKRANI
        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                OrdersEmptyStateCard(
                    onLoadSampleOrders = {
                        scope.launch {
                            dataService.seedSampleOrders()
                            onShowToast(
                                KYToastData(
                                    message = TrStrings.Orders.TOAST_SAMPLE_ORDERS_LOADED,
                                    type = KYToastType.SUCCESS
                                )
                            )
                        }
                    },
                    onCreateDemoOrder = { simulateIncomingCustomerOrder() }
                )
            }
        } else if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                KYCard(tag = "orders_filter_empty_state") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = TrStrings.Orders.EMPTY_FILTER_TITLE,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = TrStrings.Orders.EMPTY_FILTER_DESCRIPTION,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        KYButton(
                            text = TrStrings.Orders.BTN_RESET_FILTERS,
                            onClick = {
                                selectedStatusFilter = null
                                onlyResellerOrders = false
                                searchQuery = ""
                            },
                            variant = ButtonVariant.SECONDARY,
                            tag = "btn_reset_order_filters"
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("orders_list"),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // AI Satış Asistanı (Müşteri mesajına cevap önerisi üreten sohbet kutusu)
                item(key = "orders_sales_assistant_chat_card") {
                    SalesAssistantChatCard(
                        orders = orders,
                        onShowToast = onShowToast
                    )
                }

                // Bayi Bazında Satış Özeti Kartı
                if (resellers.isNotEmpty()) {
                    item(key = "orders_reseller_sales_summary") {
                        ResellerSalesSummaryCard(
                            resellers = resellers,
                            orders = orders
                        )
                    }
                }

                items(filteredOrders, key = { it.id }) { order ->
                    OrderListCard(
                        order = order,
                        products = products,
                        onClick = { onOrderClick(order.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        onClick = onClick,
        shape = BadgeRoundedPill,
        color = if (isSelected) PrimaryBlue else SurfaceWhite,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) PrimaryBlue else BorderLight
        ),
        modifier = Modifier.testTag(tag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) SurfaceWhite else TextSecondary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/**
 * SİPARİŞ KARTI:
 * - Sipariş no, müşteri adı, tarih, ürün sayısı, toplam tutar, durum rozeti ve Bayi etiketi.
 */
@Composable
private fun OrderListCard(
    order: Order,
    products: List<Product> = emptyList(),
    onClick: () -> Unit
) {
    val totalItemQuantity = remember(order.items) {
        order.items.sumOf { it.quantity }.coerceAtLeast(order.items.size)
    }

    Surface(
        onClick = onClick,
        shape = CardRounded2xl,
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderLight),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Üst Satır: Sipariş No + Tarih + Bayi Etiketi  |  Durum Rozeti
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = ButtonRoundedXl,
                        color = PrimaryBlueSubtle
                    ) {
                        Text(
                            text = order.orderNumber,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    // Bayi üzerinden gelen siparişlerde "Bayi" etiketi
                    if (order.isResellerOrder) {
                        Surface(
                            shape = BadgeRoundedPill,
                            color = AccentAmberLight,
                            border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.45f)),
                            modifier = Modifier.testTag("order_reseller_badge_${order.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Storefront,
                                    contentDescription = null,
                                    tint = AccentAmberDark,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (order.resellerName.isNotBlank()) {
                                        "${TrStrings.Resellers.ORDER_BADGE_RESELLER}: ${order.resellerName}"
                                    } else {
                                        TrStrings.Resellers.ORDER_BADGE_RESELLER
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AccentAmberDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatOrderDateTr(order.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                OrderStatusBadge(
                    status = order.status,
                    tag = "order_card_badge_${order.id}"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Orta Satır: Müşteri Adı + Katalog Adı + Ürün Küçük Görselleri
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceMuted),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = order.customerName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = order.items.joinToString(", ") { "${it.quantity}x ${it.productName}" },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Ürün Görsel Önizlemeleri
                if (order.items.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        order.items.take(3).forEachIndexed { _, item ->
                            val matched = products.find { it.id == item.productId || it.name.trim().equals(item.productName.trim(), ignoreCase = true) }
                            val imgUrl = when {
                                !matched?.imageUrl.isNullOrBlank() -> matched!!.imageUrl
                                item.imageUrl.isNotBlank() && !item.imageUrl.contains("picsum.photos/seed/") -> item.imageUrl
                                !matched?.galleryUrls.isNullOrEmpty() -> matched!!.galleryUrls.first()
                                else -> item.imageUrl.takeIf { !it.contains("picsum.photos/seed/") }.orEmpty()
                            }
                            if (imgUrl.isNotBlank()) {
                                AsyncImage(
                                    model = imgUrl,
                                    contentDescription = item.productName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, SurfaceWhite, CircleShape)
                                        .background(SurfaceMuted)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, SurfaceWhite, CircleShape)
                                        .background(SurfaceMuted),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Inventory2,
                                        contentDescription = item.productName,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SurfaceMuted)
            Spacer(modifier = Modifier.height(10.dp))

            // Alt Satır: Ürün Sayısı | Toplam Tutar + Detay Ok İkonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingBag,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$totalItemQuantity ${TrStrings.Orders.ITEM_COUNT_SUFFIX}",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${order.totalAmount.roundToInt()} ${order.currency}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = TrStrings.Orders.CARD_DETAIL_HINT,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Siparişler Boş Durum Ekranı (İllüstrasyonlu)
 */
@Composable
private fun OrdersEmptyStateCard(
    onLoadSampleOrders: () -> Unit,
    onCreateDemoOrder: () -> Unit
) {
    KYCard(tag = "orders_empty_state") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlueSubtle)
                )
                Icon(
                    imageVector = Icons.Outlined.ShoppingBag,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(42.dp)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(AccentAmber),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = SurfaceWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = TrStrings.Orders.EMPTY_TITLE,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = TrStrings.Orders.EMPTY_DESCRIPTION,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            KYButton(
                text = TrStrings.Orders.BTN_LOAD_SAMPLE_ORDERS,
                onClick = onLoadSampleOrders,
                variant = ButtonVariant.PRIMARY,
                leadingIcon = Icons.Outlined.ReceiptLong,
                modifier = Modifier.fillMaxWidth(),
                tag = "btn_load_sample_orders"
            )

            Spacer(modifier = Modifier.height(8.dp))

            KYButton(
                text = TrStrings.Orders.BTN_CREATE_DEMO_ORDER,
                onClick = onCreateDemoOrder,
                variant = ButtonVariant.SECONDARY,
                leadingIcon = Icons.Filled.Add,
                modifier = Modifier.fillMaxWidth(),
                tag = "btn_create_demo_order_empty"
            )
        }
    }
}
