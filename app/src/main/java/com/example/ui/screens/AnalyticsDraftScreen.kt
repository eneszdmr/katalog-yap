package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.TrStrings
import com.example.model.AnalyticsDateRange
import com.example.model.OrderStatus
import com.example.services.GeminiProductService
import com.example.services.LocalStorageDataService
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYButton
import com.example.ui.components.KYCard
import com.example.ui.components.KYToastData
import com.example.ui.components.KYToastType
import com.example.ui.components.KYTopBar
import com.example.ui.screens.analytics.CatalogVisitorBar
import com.example.ui.screens.analytics.CatalogVisitorsBarChartCard
import com.example.ui.screens.analytics.CustomerInterestTableCard
import com.example.ui.screens.analytics.DailyVisitorPoint
import com.example.ui.screens.analytics.DailyVisitorsLineChartCard
import com.example.ui.screens.analytics.ProductAnalyticsRow
import com.example.ui.screens.analytics.ReferrerDistributionCard
import com.example.ui.screens.analytics.ReferrerStat
import com.example.ui.screens.analytics.TopProductsSectionCard
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val KYPrimary = PrimaryBlue
private val KYAccent = AccentAmber
private val KYBorder = BorderLight
private val KYSuccess = SuccessGreen
private val KYTextPrimary = TextPrimary
private val KYTextSecondary = TextSecondary

@Composable
fun AnalyticsDraftScreen(
    onOpenDesignSystem: () -> Unit = {},
    onProductClick: (String) -> Unit = {},
    onShowToast: (KYToastData) -> Unit = {}
) {
    val context = LocalContext.current
    val dataService = remember { LocalStorageDataService.getInstance(context) }
    val scope = rememberCoroutineScope()

    val catalogs by dataService.catalogs.collectAsState()
    val products by dataService.products.collectAsState()
    val orders by dataService.orders.collectAsState()
    val analyticsEvents by dataService.catalogAnalyticsEvents.collectAsState()
    val settings by dataService.settings.collectAsState()

    // Eğer eski oturumdan kalma boş analitik koleksiyonu varsa örnek veriyi otomatik üret
    LaunchedEffect(analyticsEvents.isEmpty()) {
        if (analyticsEvents.isEmpty()) {
            dataService.seedSampleAnalyticsEvents()
        }
    }

    var selectedRange by remember { mutableStateOf(AnalyticsDateRange.DAYS_7) }
    var customStartDaysAgo by remember { mutableIntStateOf(14) }
    var customEndDaysAgo by remember { mutableIntStateOf(0) }

    val now = remember(analyticsEvents, orders, selectedRange, customStartDaysAgo, customEndDaysAgo) {
        System.currentTimeMillis()
    }
    val dayMs = 86_400_000L

    // Seçilen tarih aralığına göre başlangıç ve bitiş zaman damgaları
    val (rangeStartMs, rangeEndMs, activeDaysCount) = remember(
        selectedRange,
        customStartDaysAgo,
        customEndDaysAgo,
        now
    ) {
        when (selectedRange) {
            AnalyticsDateRange.TODAY -> {
                Triple(now - dayMs, now + 60_000L, 1)
            }
            AnalyticsDateRange.DAYS_7 -> {
                Triple(now - 7L * dayMs, now + 60_000L, 7)
            }
            AnalyticsDateRange.DAYS_30 -> {
                Triple(now - 30L * dayMs, now + 60_000L, 30)
            }
            AnalyticsDateRange.CUSTOM -> {
                val startDays = maxOf(customStartDaysAgo, customEndDaysAgo + 1)
                val endDays = minOf(customStartDaysAgo - 1, customEndDaysAgo).coerceAtLeast(0)
                val daysSpan = (startDays - endDays).coerceAtLeast(1)
                Triple(now - startDays * dayMs, now - endDays * dayMs + 60_000L, daysSpan)
            }
        }
    }

    // 1) Koleksiyonları tarih aralığına göre filtrele: visits, productViews, addToCart, orders
    val filteredEvents = remember(analyticsEvents, rangeStartMs, rangeEndMs) {
        analyticsEvents.filter { it.createdAt in rangeStartMs..rangeEndMs }
    }
    val visits = remember(filteredEvents) {
        filteredEvents.filter { it.eventType == "visit" }
    }
    val productViews = remember(filteredEvents) {
        filteredEvents.filter { it.eventType == "productView" }
    }
    val addToCartEvents = remember(filteredEvents) {
        filteredEvents.filter { it.eventType == "addToCart" }
    }
    val filteredOrders = remember(orders, rangeStartMs, rangeEndMs) {
        orders.filter { it.createdAt in rangeStartMs..rangeEndMs && it.status != OrderStatus.CANCELLED }
    }

    // 2) Üst Metrik Kartları (Toplam ziyaretçi, Benzersiz ziyaretçi, Sipariş sayısı, Ciro, Dönüşüm oranı)
    val totalVisitsCount = visits.size
    val uniqueVisitorsCount = remember(visits) {
        visits.map { it.sessionId }.distinct().size
    }
    val orderCount = filteredOrders.size
    val totalRevenue = remember(filteredOrders) {
        filteredOrders.sumOf { it.totalAmount }
    }
    val conversionRate = remember(orderCount, uniqueVisitorsCount) {
        if (uniqueVisitorsCount > 0) {
            (orderCount.toDouble() / uniqueVisitorsCount.toDouble()) * 100.0
        } else {
            0.0
        }
    }

    val currencySymbol = settings.currencySymbol.ifBlank { "₺" }

    // 3) Grafik 1: Günlük ziyaretçi çizgi grafiği verisi
    val dailyVisitorPoints = remember(
        visits,
        selectedRange,
        customStartDaysAgo,
        customEndDaysAgo,
        now
    ) {
        val localeTr = Locale("tr", "TR")
        val shortFmt = SimpleDateFormat("dd MMM", localeTr)
        val fullFmt = SimpleDateFormat("dd MMMM yyyy", localeTr)

        if (selectedRange == AnalyticsDateRange.TODAY) {
            // Bugün seçildiğinde son 24 saati 6 zaman diliminde göster
            val slotHours = listOf(20, 16, 12, 8, 4, 0)
            slotHours.map { hoursAgo ->
                val slotEnd = now - (hoursAgo * 3_600_000L)
                val slotStart = slotEnd - (4 * 3_600_000L)
                val slotVisits = visits.filter { it.createdAt in slotStart..slotEnd }
                val hourFmt = SimpleDateFormat("HH:00", localeTr)
                val label = hourFmt.format(Date(slotEnd))
                DailyVisitorPoint(
                    label = label,
                    fullDateLabel = "Bugün $label dilimi",
                    totalVisits = slotVisits.size,
                    uniqueVisitors = slotVisits.map { it.sessionId }.distinct().size
                )
            }
        } else {
            val startDayOffset = when (selectedRange) {
                AnalyticsDateRange.DAYS_7 -> 6
                AnalyticsDateRange.DAYS_30 -> 29
                AnalyticsDateRange.CUSTOM -> maxOf(customStartDaysAgo - 1, customEndDaysAgo)
                else -> 6
            }
            val endDayOffset = when (selectedRange) {
                AnalyticsDateRange.CUSTOM -> minOf(customStartDaysAgo - 1, customEndDaysAgo).coerceAtLeast(0)
                else -> 0
            }

            (startDayOffset downTo endDayOffset).map { dayOffset ->
                val dayEnd = now - (dayOffset * dayMs)
                val dayStart = dayEnd - dayMs
                val dayVisits = visits.filter { it.createdAt in dayStart..dayEnd }
                val dateObj = Date(dayEnd)
                DailyVisitorPoint(
                    label = shortFmt.format(dateObj),
                    fullDateLabel = fullFmt.format(dateObj),
                    totalVisits = dayVisits.size,
                    uniqueVisitors = dayVisits.map { it.sessionId }.distinct().size
                )
            }
        }
    }

    // 4) Grafik 2: Katalog bazında ziyaretçi çubuk grafiği verisi
    val catalogBars = remember(catalogs, visits, filteredOrders) {
        catalogs.map { cat ->
            val catVisits = visits.filter { it.catalogId == cat.id }
            val catOrders = filteredOrders.count { it.catalogId == cat.id }
            val shortName = cat.title
                .split(" ")
                .take(2)
                .joinToString(" ")
                .take(14)
            CatalogVisitorBar(
                catalogId = cat.id,
                shortTitle = shortName,
                fullTitle = cat.title,
                totalVisits = catVisits.size,
                uniqueVisitors = catVisits.map { it.sessionId }.distinct().size,
                orderCount = catOrders
            )
        }.sortedByDescending { it.totalVisits }
    }

    // 5) Ürün bazında Görüntülenme (productViews), Sepete Ekleme (addToCart) ve Sipariş (orders) hesaplaması
    val productAnalyticsRows = remember(products, catalogs, productViews, addToCartEvents, filteredOrders) {
        val catalogMap = catalogs.associateBy { it.id }
        val viewsByProduct = productViews.groupingBy { it.productId }.eachCount()
        val cartAddsByProduct = addToCartEvents.groupBy { it.productId }
            .mapValues { (_, evts) -> evts.sumOf { it.quantity.coerceAtLeast(1) } }
        val ordersByProduct = mutableMapOf<String, Int>()
        filteredOrders.forEach { order ->
            order.items.forEach { item ->
                ordersByProduct[item.productId] =
                    (ordersByProduct[item.productId] ?: 0) + item.quantity.coerceAtLeast(1)
            }
        }

        products.map { prod ->
            val catTitle = catalogMap[prod.catalogId]?.title ?: "Genel Katalog"
            ProductAnalyticsRow(
                productId = prod.id,
                productName = prod.name,
                imageUrl = prod.imageUrl,
                catalogTitle = catTitle,
                price = prod.price,
                currency = prod.currency.ifBlank { currencySymbol },
                viewCount = viewsByProduct[prod.id] ?: 0,
                addToCartCount = cartAddsByProduct[prod.id] ?: 0,
                orderCount = ordersByProduct[prod.id] ?: 0
            )
        }
    }

    // "En çok görüntülenen ürünler" (Top 10)
    val top10ViewedProducts = remember(productAnalyticsRows) {
        productAnalyticsRows
            .sortedWith(compareByDescending<ProductAnalyticsRow> { it.viewCount }.thenByDescending { it.addToCartCount })
            .take(10)
    }

    // "En çok sepete eklenen ürünler" (Top 10)
    val topAddedToCartProducts = remember(productAnalyticsRows) {
        productAnalyticsRows
            .filter { it.addToCartCount > 0 }
            .sortedWith(compareByDescending<ProductAnalyticsRow> { it.addToCartCount }.thenByDescending { it.viewCount })
            .take(10)
            .ifEmpty { productAnalyticsRows.take(5) }
    }

    // "Müşteri ilgisi" tablosu satırları
    val customerInterestRows = remember(productAnalyticsRows) {
        productAnalyticsRows.sortedWith(
            compareByDescending<ProductAnalyticsRow> { it.viewCount }
                .thenByDescending { it.addToCartCount }
                .thenByDescending { it.orderCount }
        )
    }

    // 6) Kaynak Dağılımı (Referrer: WhatsApp, Doğrudan link, Diğer)
    val referrerStats = remember(visits) {
        val total = visits.size.coerceAtLeast(1)
        val whatsappCount = visits.count { it.referrer.equals("whatsapp", ignoreCase = true) }
        val directCount = visits.count { it.referrer.equals("direct", ignoreCase = true) }
        val otherCount = (visits.size - whatsappCount - directCount).coerceAtLeast(0)

        listOf(
            ReferrerStat(
                key = "whatsapp",
                label = TrStrings.Analytics.REFERRER_WHATSAPP,
                count = whatsappCount,
                percentage = (whatsappCount.toFloat() / total.toFloat()) * 100f,
                color = Color(0xFF16A34A)
            ),
            ReferrerStat(
                key = "direct",
                label = TrStrings.Analytics.REFERRER_DIRECT,
                count = directCount,
                percentage = (directCount.toFloat() / total.toFloat()) * 100f,
                color = KYPrimary
            ),
            ReferrerStat(
                key = "other",
                label = TrStrings.Analytics.REFERRER_OTHER,
                count = otherCount,
                percentage = (otherCount.toFloat() / total.toFloat()) * 100f,
                color = KYAccent
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_analytics")
    ) {
        KYTopBar(
            title = TrStrings.Analytics.TITLE,
            subtitle = TrStrings.Analytics.SUBTITLE,
            rightAction = {
                IconButton(
                    onClick = {
                        scope.launch {
                            dataService.seedSampleAnalyticsEvents()
                            onShowToast(
                                KYToastData(
                                    message = TrStrings.Analytics.TOAST_SEEDED,
                                    type = KYToastType.SUCCESS
                                )
                            )
                        }
                    },
                    modifier = Modifier.testTag("analytics_seed_demo_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = TrStrings.Analytics.SEED_DEMO_DATA,
                        tint = KYPrimary
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1) TARİH ARALIĞI SEÇİCİ: Bugün, 7 gün, 30 gün, Özel
            item {
                DateRangeSelectorSection(
                    selectedRange = selectedRange,
                    onRangeSelect = { selectedRange = it },
                    customStartDaysAgo = customStartDaysAgo,
                    customEndDaysAgo = customEndDaysAgo,
                    onCustomStartChange = { newStart ->
                        customStartDaysAgo = newStart.coerceIn(2, 30)
                        if (customEndDaysAgo >= customStartDaysAgo) {
                            customEndDaysAgo = (customStartDaysAgo - 1).coerceAtLeast(0)
                        }
                    },
                    onCustomEndChange = { newEnd ->
                        customEndDaysAgo = newEnd.coerceIn(0, 28)
                        if (customStartDaysAgo <= customEndDaysAgo) {
                            customStartDaysAgo = (customEndDaysAgo + 1).coerceAtMost(30)
                        }
                    },
                    activeDaysCount = activeDaysCount
                )
            }

            // 2) ÜST METRİK KARTLARI (5 Metrik: Toplam ziyaretçi, Benzersiz ziyaretçi, Sipariş sayısı, Ciro, Dönüşüm oranı)
            item {
                TopMetricsGridSection(
                    totalVisits = totalVisitsCount,
                    uniqueVisitors = uniqueVisitorsCount,
                    orderCount = orderCount,
                    revenueFormatted = "${formatAmountTr(totalRevenue)} $currencySymbol",
                    conversionFormatted = "%${String.format(Locale("tr", "TR"), "%.1f", conversionRate)}"
                )
            }

            // 2.5) ✨ GEMINI AI ANALİTİK ÖZETİ ("Bu hafta ne oldu?" — 3-4 maddelik Türkçe içgörü ve öneri)
            item {
                WeeklyAiAnalyticsSummaryCard(
                    periodLabel = selectedRange.labelTr,
                    totalVisits = totalVisitsCount,
                    uniqueVisitors = uniqueVisitorsCount,
                    orderCount = orderCount,
                    revenueFormatted = "${formatAmountTr(totalRevenue)} $currencySymbol",
                    conversionFormatted = "%${String.format(Locale("tr", "TR"), "%.1f", conversionRate)}",
                    topCatalogTitle = catalogBars.firstOrNull()?.fullTitle ?: "Genel Katalog",
                    topViewedProduct = top10ViewedProducts.firstOrNull()?.let { "${it.productName} (${it.viewCount} görüntülenme, ${it.addToCartCount} sepete ekleme)" } ?: "Yok",
                    topCartProduct = topAddedToCartProducts.firstOrNull()?.let { "${it.productName} (${it.addToCartCount} sepete ekleme, ${it.orderCount} sipariş)" } ?: "Yok",
                    whatsappSharePercent = referrerStats.firstOrNull { it.key == "whatsapp" }?.percentage?.toInt() ?: 0
                )
            }

            // 3) GRAFİK 1: GÜNLÜK ZİYARETÇİ ÇİZGİ GRAFİĞİ
            item {
                DailyVisitorsLineChartCard(points = dailyVisitorPoints)
            }

            // 4) GRAFİK 2: KATALOG BAZINDA ZİYARETÇİ ÇUBUK GRAFİĞİ
            item {
                CatalogVisitorsBarChartCard(bars = catalogBars)
            }

            // 5) KAYNAK DAĞILIMI (WhatsApp, Doğrudan link, Diğer)
            item {
                ReferrerDistributionCard(
                    stats = referrerStats,
                    totalVisits = totalVisitsCount
                )
            }

            // 6) EN ÇOK GÖRÜNTÜLENEN ÜRÜNLER LİSTESİ (TOP 10)
            item {
                TopProductsSectionCard(
                    title = TrStrings.Analytics.TOP_VIEWED_TITLE,
                    subtitle = TrStrings.Analytics.TOP_VIEWED_SUBTITLE,
                    icon = Icons.Default.Visibility,
                    accentColor = KYPrimary,
                    items = top10ViewedProducts,
                    metricSelector = { it.viewCount },
                    unitLabel = TrStrings.Analytics.UNIT_VIEWS,
                    onProductClick = onProductClick,
                    testTag = "top_viewed_products_card"
                )
            }

            // 7) EN ÇOK SEPETE EKLENEN ÜRÜNLER LİSTESİ
            item {
                TopProductsSectionCard(
                    title = TrStrings.Analytics.TOP_CART_TITLE,
                    subtitle = TrStrings.Analytics.TOP_CART_SUBTITLE,
                    icon = Icons.Default.ShoppingCart,
                    accentColor = Color(0xFFD97706),
                    items = topAddedToCartProducts,
                    metricSelector = { it.addToCartCount },
                    unitLabel = TrStrings.Analytics.UNIT_CART_ADDS,
                    onProductClick = onProductClick,
                    testTag = "top_cart_products_card"
                )
            }

            // 8) MÜŞTERİ İLGİSİ BÖLÜMÜ (TABLO)
            item {
                CustomerInterestTableCard(
                    rows = customerInterestRows,
                    onProductClick = onProductClick
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DateRangeSelectorSection(
    selectedRange: AnalyticsDateRange,
    onRangeSelect: (AnalyticsDateRange) -> Unit,
    customStartDaysAgo: Int,
    customEndDaysAgo: Int,
    onCustomStartChange: (Int) -> Unit,
    onCustomEndChange: (Int) -> Unit,
    activeDaysCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(1.dp, KYBorder, RoundedCornerShape(14.dp))
                .padding(4.dp)
                .testTag("analytics_date_range_selector"),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AnalyticsDateRange.entries.forEach { range ->
                val isSelected = selectedRange == range
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) KYPrimary else Color.Transparent)
                        .clickable { onRangeSelect(range) }
                        .testTag("date_range_${range.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range.labelTr,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) Color.White else KYTextSecondary
                    )
                }
            }
        }

        if (selectedRange == AnalyticsDateRange.CUSTOM) {
            KYCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_date_range_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = KYPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = TrStrings.Analytics.CUSTOM_RANGE_TITLE,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = KYTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Başlangıç Günü Seçici
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, KYBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = TrStrings.Analytics.CUSTOM_START_DAYS_AGO,
                                style = MaterialTheme.typography.labelSmall,
                                color = KYTextSecondary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = { onCustomStartChange(customStartDaysAgo - 1) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Azalt",
                                        tint = KYTextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "$customStartDaysAgo gün",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = KYPrimary
                                )
                                IconButton(
                                    onClick = { onCustomStartChange(customStartDaysAgo + 1) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Artır",
                                        tint = KYTextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Bitiş Günü Seçici
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, KYBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = TrStrings.Analytics.CUSTOM_END_DAYS_AGO,
                                style = MaterialTheme.typography.labelSmall,
                                color = KYTextSecondary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = { onCustomEndChange(customEndDaysAgo - 1) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Azalt",
                                        tint = KYTextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = if (customEndDaysAgo == 0) "Bugün" else "$customEndDaysAgo gün",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = KYPrimary
                                )
                                IconButton(
                                    onClick = { onCustomEndChange(customEndDaysAgo + 1) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Artır",
                                        tint = KYTextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${TrStrings.Analytics.CUSTOM_ACTIVE_SUMMARY_PREFIX} $activeDaysCount ${TrStrings.Analytics.CUSTOM_ACTIVE_SUMMARY_SUFFIX}",
                    style = MaterialTheme.typography.labelSmall,
                    color = KYTextSecondary
                )
            }
        }
    }
}

@Composable
private fun TopMetricsGridSection(
    totalVisits: Int,
    uniqueVisitors: Int,
    orderCount: Int,
    revenueFormatted: String,
    conversionFormatted: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analytics_top_metrics_section")
    ) {
        // İlk satır: Toplam ziyaretçi & Benzersiz ziyaretçi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnalyticsMetricCard(
                label = TrStrings.Analytics.METRIC_TOTAL_VISITS,
                value = formatNumberTr(totalVisits),
                icon = Icons.Default.Visibility,
                iconTint = KYPrimary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_card_total_visits")
            )
            AnalyticsMetricCard(
                label = TrStrings.Analytics.METRIC_UNIQUE_VISITORS,
                value = formatNumberTr(uniqueVisitors),
                icon = Icons.Default.Group,
                iconTint = Color(0xFF8B5CF6),
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_card_unique_visitors")
            )
        }

        // İkinci satır: Sipariş sayısı & Ciro
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnalyticsMetricCard(
                label = TrStrings.Analytics.METRIC_ORDER_COUNT,
                value = formatNumberTr(orderCount),
                icon = Icons.Default.ShoppingBag,
                iconTint = Color(0xFFD97706),
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_card_order_count")
            )
            AnalyticsMetricCard(
                label = TrStrings.Analytics.METRIC_REVENUE,
                value = revenueFormatted,
                icon = Icons.Default.Payments,
                iconTint = KYSuccess,
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_card_revenue")
            )
        }

        // Üçüncü satır: Dönüşüm oranı (Tam genişlik vurgulu kart)
        KYCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("metric_card_conversion_rate")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(KYAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Percent,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = TrStrings.Analytics.METRIC_CONVERSION_RATE,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = KYTextSecondary
                        )
                        Text(
                            text = "Benzersiz ziyaretçi başına sipariş oranı",
                            style = MaterialTheme.typography.labelSmall,
                            color = KYTextSecondary
                        )
                    }
                }
                Text(
                    text = conversionFormatted,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = KYPrimary
                )
            }
        }
    }
}

@Composable
private fun AnalyticsMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    KYCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = KYTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = KYTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 4) ANALİTİK ÖZETİ ("Bu hafta ne oldu?"):
 * Mağaza metriklerini Gemini'ye gönderip 3-4 maddelik Türkçe içgörü ve aksiyon önerisi üretir.
 * Yükleme durumu, hata yönetimi ve tekrar deneme butonu içerir.
 */
@Composable
private fun WeeklyAiAnalyticsSummaryCard(
    periodLabel: String,
    totalVisits: Int,
    uniqueVisitors: Int,
    orderCount: Int,
    revenueFormatted: String,
    conversionFormatted: String,
    topCatalogTitle: String,
    topViewedProduct: String,
    topCartProduct: String,
    whatsappSharePercent: Int
) {
    val scope = rememberCoroutineScope()
    var insights by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun fetchWeeklyAiInsights() {
        if (isLoading) return
        scope.launch {
            isLoading = true
            errorMessage = null

            val metricsPrompt = """
                Dönem: $periodLabel
                Toplam Ziyaret: $totalVisits
                Benzersiz Ziyaretçi: $uniqueVisitors
                Sipariş Sayısı: $orderCount
                Toplam Ciro: $revenueFormatted
                Dönüşüm Oranı: $conversionFormatted
                En Çok Ziyaret Edilen Katalog: $topCatalogTitle
                En Çok Görüntülenen Ürün: $topViewedProduct
                En Çok Sepete Eklenen Ürün: $topCartProduct
                WhatsApp Trafik Payı: %$whatsappSharePercent
            """.trimIndent()

            val result = GeminiProductService.generateWeeklyAnalyticsSummary(metricsPrompt)
            isLoading = false

            result.onSuccess { list ->
                insights = list
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Analitik özeti alınamadı."
            }
        }
    }

    KYCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analytics_weekly_ai_summary_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryBlueSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = KYPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Gemini AI Analitik Özeti",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = KYTextPrimary
                        )
                        Text(
                            text = "Metriklerinizden 3-4 maddelik Türkçe içgörü ve büyüme önerisi",
                            style = MaterialTheme.typography.labelSmall,
                            color = KYTextSecondary
                        )
                    }
                }
            }

            KYButton(
                text = if (isLoading) {
                    "Metrikler Analiz Ediliyor..."
                } else if (insights.isNotEmpty()) {
                    "✨ Bu hafta ne oldu? (Yeniden Analiz Et)"
                } else {
                    "✨ Bu hafta ne oldu?"
                },
                onClick = { fetchWeeklyAiInsights() },
                variant = if (insights.isEmpty()) ButtonVariant.PRIMARY else ButtonVariant.SECONDARY,
                leadingIcon = Icons.Default.AutoAwesome,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                tag = "btn_analytics_weekly_ai_summary"
            )

            // Yükleme Durumu (Loading State)
            if (isLoading) {
                Surface(
                    shape = ButtonRoundedXl,
                    color = PrimaryBlueSubtle,
                    border = BorderStroke(1.dp, KYPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("analytics_ai_loading")
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
                            color = KYPrimary,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Gemini AI mağaza metriklerinizi inceliyor ve öneriler hazırlıyor...",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = KYPrimary
                        )
                    }
                }
            }

            // Hata Yönetimi ve Tekrar Deneme Butonu (Error State + Retry)
            if (errorMessage != null) {
                Surface(
                    shape = ButtonRoundedXl,
                    color = DangerRedLight,
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("analytics_ai_error_banner")
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
                            onClick = { fetchWeeklyAiInsights() },
                            variant = ButtonVariant.SECONDARY,
                            leadingIcon = Icons.Outlined.Refresh,
                            modifier = Modifier.fillMaxWidth(),
                            tag = "btn_retry_analytics_ai_summary"
                        )
                    }
                }
            }

            // Üretilen 3-4 Maddelik Türkçe İçgörü ve Öneri Listesi
            if (insights.isNotEmpty() && !isLoading) {
                HorizontalDivider(color = KYBorder)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("analytics_ai_insights_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    insights.forEachIndexed { idx, itemText ->
                        Surface(
                            shape = ButtonRoundedXl,
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, KYBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("analytics_ai_insight_item_$idx")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(KYPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SurfaceWhite
                                    )
                                }
                                Text(
                                    text = itemText,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = KYTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatNumberTr(value: Int): String {
    val symbols = DecimalFormatSymbols(Locale("tr", "TR")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    return DecimalFormat("#,###", symbols).format(value)
}

private fun formatAmountTr(value: Double): String {
    val symbols = DecimalFormatSymbols(Locale("tr", "TR")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    return DecimalFormat("#,###", symbols).format(value.toLong())
}
