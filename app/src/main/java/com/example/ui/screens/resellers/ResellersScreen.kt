package com.example.ui.screens.resellers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.TrStrings
import com.example.model.Catalog
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.Reseller
import com.example.model.ResellerPriceMode
import com.example.services.LocalStorageDataService
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYButton
import com.example.ui.components.KYCard
import com.example.ui.components.KYEmptyState
import com.example.ui.components.KYInput
import com.example.ui.components.KYModal
import com.example.ui.components.KYToastData
import com.example.ui.components.KYToastType
import com.example.ui.components.KYTopBar
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentAmberLight
import com.example.ui.theme.BackgroundSlate
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
import java.util.UUID

data class ResellerSalesStat(
    val resellerId: String,
    val resellerName: String,
    val priceSummary: String,
    val orderCount: Int,
    val totalItemsSold: Int,
    val totalRevenue: Double,
    val currency: String
)

/**
 * AYARLAR > BAYİLER EKRANI (/resellers):
 * - Bayi ekle (ad, telefon), her bayiye hangi katalogların görünür olduğunu seç.
 * - Bayiye özel link üret (/c/bayi-kod); bu linkte bayi fiyatları veya bayinin eklediği marj uygulansın.
 * - Bayi bazında satış özeti (sipariş adedi, satılan ürün adedi ve toplam ciro) sun.
 */
@Composable
fun ResellersScreen(
    onBackClick: () -> Unit,
    onOpenPublicCatalog: (String) -> Unit,
    onShowToast: (KYToastData) -> Unit,
    openAddOnLaunch: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataService = remember { LocalStorageDataService.getInstance(context) }
    val scope = rememberCoroutineScope()

    val resellers by dataService.resellers.collectAsState()
    val catalogs by dataService.catalogs.collectAsState()
    val orders by dataService.orders.collectAsState()
    val settings by dataService.settings.collectAsState()
    val currency = settings.currencySymbol.ifBlank { "₺" }

    var showAddOrEditModal by remember(openAddOnLaunch) { mutableStateOf(openAddOnLaunch) }
    var editingReseller by remember { mutableStateOf<Reseller?>(null) }
    var deletingReseller by remember { mutableStateOf<Reseller?>(null) }

    BackHandler {
        onBackClick()
    }

    // Bayi Bazında Satış Özeti Hesaplama
    val resellerSalesStats = remember(resellers, orders, currency) {
        val validOrders = orders.filter { it.status != OrderStatus.CANCELLED && it.isResellerOrder }
        resellers.map { reseller ->
            val matchingOrders = validOrders.filter {
                it.resellerId == reseller.id ||
                    it.resellerName.equals(reseller.name, ignoreCase = true)
            }
            ResellerSalesStat(
                resellerId = reseller.id,
                resellerName = reseller.name,
                priceSummary = reseller.formatPriceRuleSummary(currency),
                orderCount = matchingOrders.size,
                totalItemsSold = matchingOrders.sumOf { order -> order.items.sumOf { it.quantity } },
                totalRevenue = matchingOrders.sumOf { it.totalAmount },
                currency = currency
            )
        }.sortedByDescending { it.totalRevenue }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundSlate)
            .testTag("screen_resellers")
    ) {
        KYTopBar(
            title = TrStrings.Resellers.TITLE,
            subtitle = "${resellers.size} kayıtlı bayi • Özel fiyat & katalog erişimi",
            onBackClick = onBackClick,
            rightAction = {
                KYButton(
                    text = TrStrings.Resellers.BTN_ADD_RESELLER,
                    onClick = {
                        editingReseller = null
                        showAddOrEditModal = true
                    },
                    variant = ButtonVariant.PRIMARY,
                    leadingIcon = Icons.Filled.Add,
                    tag = "btn_top_add_reseller"
                )
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("resellers_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1) BAYİ BAZINDA SATIŞ ÖZETİ KARTI
            item {
                ResellerSalesSummaryCard(
                    stats = resellerSalesStats,
                    currency = currency
                )
            }

            // 2) BAYİ LİSTESİ VEYA BOŞ DURUM
            if (resellers.isEmpty()) {
                item {
                    KYEmptyState(
                        icon = Icons.Outlined.Groups,
                        title = TrStrings.Resellers.EMPTY_TITLE,
                        description = TrStrings.Resellers.EMPTY_DESC,
                        actionLabel = TrStrings.Resellers.BTN_ADD_RESELLER,
                        onActionClick = {
                            editingReseller = null
                            showAddOrEditModal = true
                        }
                    )
                }
            } else {
                items(resellers, key = { it.id }) { reseller ->
                    val stat = resellerSalesStats.find { it.resellerId == reseller.id }
                    ResellerCardItem(
                        reseller = reseller,
                        allCatalogs = catalogs,
                        currency = currency,
                        salesStat = stat,
                        onCopySpecialLink = { link ->
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Bayi Katalog Linki", link))
                            onShowToast(
                                KYToastData(
                                    message = "${TrStrings.Resellers.TOAST_RESELLER_LINK_COPIED}: $link",
                                    type = KYToastType.SUCCESS
                                )
                            )
                        },
                        onSendWhatsApp = { link ->
                            val visibleNames = catalogs
                                .filter { it.id in reseller.visibleCatalogIds }
                                .joinToString(", ") { it.title }
                                .ifBlank { "Tüm Kataloglar" }
                            val msg = buildString {
                                appendLine("Merhaba *${reseller.name}*,")
                                appendLine("Size özel bayi fiyatlı dijital ürün kataloğumuz hazır:")
                                appendLine("🔗 $link")
                                appendLine("📚 Erişime Açık Kataloglar: $visibleNames")
                                appendLine("💰 Fiyatlandırma: ${reseller.formatPriceRuleSummary(currency)}")
                            }
                            val digits = reseller.phone.filter { it.isDigit() }
                            val waUrl = if (digits.isNotBlank()) {
                                "https://wa.me/$digits?text=${Uri.encode(msg)}"
                            } else {
                                "https://wa.me/?text=${Uri.encode(msg)}"
                            }
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)))
                            }
                        },
                        onPreviewResellerPortal = {
                            onOpenPublicCatalog(reseller.shortCode)
                        },
                        onEditClick = {
                            editingReseller = reseller
                            showAddOrEditModal = true
                        },
                        onDeleteClick = {
                            deletingReseller = reseller
                        }
                    )
                }
            }
        }
    }

    // Bayi Ekle / Düzenle Modalı
    AddOrEditResellerModal(
        isVisible = showAddOrEditModal,
        editingReseller = editingReseller,
        allCatalogs = catalogs,
        currency = currency,
        onDismiss = {
            showAddOrEditModal = false
            editingReseller = null
        },
        onSave = { savedReseller ->
            scope.launch {
                dataService.saveReseller(savedReseller)
                showAddOrEditModal = false
                editingReseller = null
                onShowToast(
                    KYToastData(
                        message = TrStrings.Resellers.TOAST_RESELLER_SAVED,
                        type = KYToastType.SUCCESS
                    )
                )
            }
        }
    )

    // Bayi Silme Onay Modalı
    KYModal(
        isVisible = deletingReseller != null,
        title = "Bayi Kaydı Silinsin mi?",
        description = "${deletingReseller?.name.orEmpty()} bayisi ve özel erişim bağlantısı silinecek.",
        confirmText = TrStrings.Common.DELETE,
        cancelText = TrStrings.Common.CANCEL,
        onConfirm = {
            val target = deletingReseller
            if (target != null) {
                scope.launch {
                    dataService.deleteReseller(target.id)
                    deletingReseller = null
                    onShowToast(
                        KYToastData(
                            message = TrStrings.Resellers.TOAST_RESELLER_DELETED,
                            type = KYToastType.INFO
                        )
                    )
                }
            }
        },
        onDismiss = { deletingReseller = null }
    )
}

/**
 * Bayi Bazında Satış Özeti Kartı (Bayi ve Sipariş listesiyle doğrudan çağrım):
 */
@Composable
fun ResellerSalesSummaryCard(
    resellers: List<Reseller>,
    orders: List< com.example.model.Order >,
    currency: String = "₺",
    modifier: Modifier = Modifier
) {
    val stats = remember(resellers, orders, currency) {
        val validOrders = orders.filter { it.status != OrderStatus.CANCELLED && it.isResellerOrder }
        resellers.map { reseller ->
            val matchingOrders = validOrders.filter {
                it.resellerId == reseller.id ||
                    it.resellerName.equals(reseller.name, ignoreCase = true)
            }
            ResellerSalesStat(
                resellerId = reseller.id,
                resellerName = reseller.name,
                priceSummary = reseller.formatPriceRuleSummary(currency),
                orderCount = matchingOrders.size,
                totalItemsSold = matchingOrders.sumOf { order -> order.items.sumOf { it.quantity } },
                totalRevenue = matchingOrders.sumOf { it.totalAmount },
                currency = currency
            )
        }.sortedByDescending { it.totalRevenue }
    }
    ResellerSalesSummaryCard(
        stats = stats,
        currency = currency,
        modifier = modifier
    )
}

/**
 * Bayi Bazında Satış Özeti Kartı:
 * Her bayinin sipariş sayısını, sattığı ürün adedini ve toplam cirosunu tablo halinde sunar.
 */
@Composable
fun ResellerSalesSummaryCard(
    stats: List<ResellerSalesStat>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val totalResellerOrders = stats.sumOf { it.orderCount }
    val totalResellerItems = stats.sumOf { it.totalItemsSold }
    val totalResellerRevenue = stats.sumOf { it.totalRevenue }

    KYCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reseller_sales_summary_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentAmberLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Analytics,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = TrStrings.Resellers.SALES_SUMMARY_TITLE,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = TrStrings.Resellers.SALES_SUMMARY_SUBTITLE,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            KYBadge(
                text = "$totalResellerOrders Bayi Siparişi",
                variant = BadgeVariant.ACCENT
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Üst Toplam KPI Şeridi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = ButtonRoundedXl,
                color = PrimaryBlueSubtle,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Bayi Siparişleri",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "$totalResellerOrders Sipariş",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryBlue
                    )
                }
            }
            Surface(
                shape = ButtonRoundedXl,
                color = SurfaceMuted,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Satılan Ürün",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "$totalResellerItems Adet",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            }
            Surface(
                shape = ButtonRoundedXl,
                color = SuccessGreenLight,
                modifier = Modifier.weight(1.2f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Toplam Bayi Cirosu",
                        style = MaterialTheme.typography.labelSmall,
                        color = SuccessGreen
                    )
                    Text(
                        text = "${totalResellerRevenue.toInt()} $currency",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = SuccessGreen
                    )
                }
            }
        }

        if (stats.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderLight)
            Spacer(modifier = Modifier.height(8.dp))

            stats.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("reseller_summary_row_${item.resellerId}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.4f)) {
                        Text(
                            text = item.resellerName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.orderCount} sipariş • ${item.totalItemsSold} ürün • ${item.priceSummary}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${item.totalRevenue.toInt()} $currency",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryBlue
                    )
                }
                if (idx < stats.lastIndex) {
                    HorizontalDivider(color = SurfaceMuted)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResellerCardItem(
    reseller: Reseller,
    allCatalogs: List<Catalog>,
    currency: String,
    salesStat: ResellerSalesStat?,
    onCopySpecialLink: (String) -> Unit,
    onSendWhatsApp: (String) -> Unit,
    onPreviewResellerPortal: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val specialLink = "https://katalogyap.app/c/${reseller.shortCode}"
    val visibleCatalogs = remember(allCatalogs, reseller.visibleCatalogIds) {
        allCatalogs.filter { it.id in reseller.visibleCatalogIds }
    }

    KYCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reseller_card_${reseller.id}")
    ) {
        // Üst Satır: Bayi Adı, Telefon ve Düzenle/Sil Butonları
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlueSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = reseller.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        KYBadge(
                            text = "Bayi",
                            variant = BadgeVariant.ACCENT
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = reseller.phone.ifBlank { "Telefon belirtilmedi" },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_edit_reseller_${reseller.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = TrStrings.Common.EDIT,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_delete_reseller_${reseller.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = TrStrings.Common.DELETE,
                        tint = DangerRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Fiyat / Marj Rozeti & Satış Özeti Rozeti
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KYBadge(
                text = reseller.formatPriceRuleSummary(currency),
                variant = BadgeVariant.PRIMARY
            )
            if (salesStat != null) {
                KYBadge(
                    text = "${salesStat.orderCount} sipariş • ${salesStat.totalRevenue.toInt()} $currency",
                    variant = BadgeVariant.SUCCESS
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Görünür Kataloglar Bölümü
        Text(
            text = "${TrStrings.Resellers.LABEL_VISIBLE_CATALOGS} (${visibleCatalogs.size} Katalog)",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (visibleCatalogs.isEmpty()) {
            Text(
                text = "Tüm aktif kataloglar görünür",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                visibleCatalogs.forEach { cat ->
                    Surface(
                        shape = CircleShape,
                        color = SurfaceMuted,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MenuBook,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = cat.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bayiye Özel Link Kutusu (/c/bayi-...)
        Surface(
            shape = ButtonRoundedXl,
            color = PrimaryBlueSubtle.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reseller_link_box_${reseller.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = TrStrings.Resellers.SPECIAL_LINK_TITLE,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = PrimaryBlue
                    )
                    Text(
                        text = specialLink,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Aksiyon Butonları: Kopyala, WhatsApp'ta Gönder, Bayi Görünümünü Aç
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KYButton(
                text = TrStrings.Resellers.BTN_COPY_RESELLER_LINK,
                onClick = { onCopySpecialLink(specialLink) },
                variant = ButtonVariant.SECONDARY,
                leadingIcon = Icons.Outlined.ContentCopy,
                modifier = Modifier.weight(1f),
                tag = "btn_copy_reseller_link_${reseller.id}"
            )
            KYButton(
                text = "WhatsApp",
                onClick = { onSendWhatsApp(specialLink) },
                variant = ButtonVariant.SECONDARY,
                leadingIcon = Icons.Outlined.Share,
                modifier = Modifier.weight(1f),
                tag = "btn_whatsapp_reseller_${reseller.id}"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        KYButton(
            text = TrStrings.Resellers.BTN_OPEN_RESELLER_VIEW,
            onClick = onPreviewResellerPortal,
            variant = ButtonVariant.PRIMARY,
            leadingIcon = Icons.Outlined.OpenInNew,
            modifier = Modifier.fillMaxWidth(),
            tag = "btn_open_reseller_portal_${reseller.id}"
        )
    }
}

/**
 * Yeni Bayi Ekle / Düzenle Modalı:
 * - Ad, Telefon
 * - Fiyat / Marj Kuralı (Yüzde Marj, Sabit Marj, Bayi İskontosu) ve Değeri
 * - Görünür Katalogların Seçimi (her bayiye hangi katalogların görünür olduğunu seç)
 */
@Composable
fun AddOrEditResellerModal(
    isVisible: Boolean,
    editingReseller: Reseller?,
    allCatalogs: List<Catalog>,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (Reseller) -> Unit
) {
    if (!isVisible) return

    var name by remember(editingReseller) { mutableStateOf(editingReseller?.name.orEmpty()) }
    var phone by remember(editingReseller) { mutableStateOf(editingReseller?.phone.orEmpty()) }
    var priceMode by remember(editingReseller) {
        mutableStateOf(editingReseller?.priceMode ?: ResellerPriceMode.RESELLER_ADD_MARGIN_PERCENT)
    }
    var marginText by remember(editingReseller) {
        mutableStateOf((editingReseller?.marginValue?.toInt() ?: 20).toString())
    }
    var nameError by remember { mutableStateOf<String?>(null) }

    val selectedCatalogIds = remember(editingReseller, allCatalogs) {
        mutableStateListOf<String>().apply {
            val initial = editingReseller?.visibleCatalogIds.orEmpty()
            if (initial.isNotEmpty()) {
                addAll(initial)
            } else {
                addAll(allCatalogs.map { it.id })
            }
        }
    }

    KYModal(
        isVisible = true,
        title = if (editingReseller == null) {
            TrStrings.Resellers.MODAL_ADD_TITLE
        } else {
            TrStrings.Resellers.MODAL_EDIT_TITLE
        },
        confirmText = TrStrings.Common.SAVE,
        cancelText = TrStrings.Common.CANCEL,
        onConfirm = {
            if (name.isBlank()) {
                nameError = "Lütfen bayi adını girin"
            } else {
                val id = editingReseller?.id ?: "res-${UUID.randomUUID().toString().take(6)}"
                val slugPart = name.trim()
                    .lowercase()
                    .replace("ş", "s")
                    .replace("ğ", "g")
                    .replace("ü", "u")
                    .replace("ö", "o")
                    .replace("ç", "c")
                    .replace("ı", "i")
                    .replace(Regex("[^a-z0-9]+"), "-")
                    .trim('-')
                    .take(12)
                    .ifBlank { "ozel" }
                val shortCode = editingReseller?.shortCode?.takeIf { it.isNotBlank() }
                    ?: "bayi-$slugPart"
                val parsedMargin = marginText.replace(",", ".").toDoubleOrNull() ?: 20.0

                onSave(
                    Reseller(
                        id = id,
                        name = name.trim(),
                        phone = phone.trim().ifBlank { "+90 532 000 00 00" },
                        shortCode = shortCode,
                        visibleCatalogIds = selectedCatalogIds.toList(),
                        priceMode = priceMode,
                        marginValue = parsedMargin,
                        isActive = true,
                        createdAt = editingReseller?.createdAt ?: System.currentTimeMillis()
                    )
                )
            }
        },
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KYInput(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError != null) nameError = null
                },
                label = TrStrings.Resellers.INPUT_NAME_LABEL,
                placeholder = TrStrings.Resellers.INPUT_NAME_PLACEHOLDER,
                leadingIcon = Icons.Outlined.Storefront,
                errorText = nameError,
                tag = "input_reseller_name"
            )

            KYInput(
                value = phone,
                onValueChange = { phone = it },
                label = TrStrings.Resellers.INPUT_PHONE_LABEL,
                placeholder = TrStrings.Resellers.INPUT_PHONE_PLACEHOLDER,
                leadingIcon = Icons.Outlined.Phone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                tag = "input_reseller_phone"
            )

            Text(
                text = TrStrings.Resellers.LABEL_PRICE_RULE,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ResellerPriceMode.entries.forEach { mode ->
                    val isSelected = priceMode == mode
                    Surface(
                        onClick = { priceMode = mode },
                        shape = CircleShape,
                        color = if (isSelected) PrimaryBlue else SurfaceMuted,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) PrimaryBlue else BorderLight
                        ),
                        modifier = Modifier.testTag("reseller_mode_${mode.name}")
                    ) {
                        Text(
                            text = mode.labelTr,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) SurfaceWhite else TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            KYInput(
                value = marginText,
                onValueChange = { marginText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = when (priceMode) {
                    ResellerPriceMode.RESELLER_ADD_MARGIN_PERCENT -> "Eklenecek Bayi Marjı (%)"
                    ResellerPriceMode.RESELLER_ADD_MARGIN_FIXED -> "Eklenecek Sabit Marj ($currency)"
                    ResellerPriceMode.DISCOUNT_FOR_RESELLER -> "Bayi İskonto Oranı (%)"
                },
                placeholder = "20",
                leadingIcon = Icons.Outlined.Percent,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                tag = "input_reseller_margin"
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${TrStrings.Resellers.LABEL_VISIBLE_CATALOGS} (${selectedCatalogIds.size}/${allCatalogs.size})",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
            Text(
                text = TrStrings.Resellers.DESC_VISIBLE_CATALOGS,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                allCatalogs.forEach { cat ->
                    val isChecked = cat.id in selectedCatalogIds
                    Surface(
                        onClick = {
                            if (isChecked) {
                                if (selectedCatalogIds.size > 1) {
                                    selectedCatalogIds.remove(cat.id)
                                }
                            } else {
                                selectedCatalogIds.add(cat.id)
                            }
                        },
                        shape = ButtonRoundedXl,
                        color = if (isChecked) PrimaryBlueSubtle else SurfaceWhite,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isChecked) PrimaryBlue else BorderLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reseller_cat_check_${cat.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isChecked) {
                                    Icons.Filled.CheckCircle
                                } else {
                                    Icons.Outlined.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = if (isChecked) PrimaryBlue else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.title,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${cat.productCount} ürün",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
