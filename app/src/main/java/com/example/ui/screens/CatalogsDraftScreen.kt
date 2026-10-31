package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Merge
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.FloatingActionButton
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
import com.example.auth.LocalAuthContext
import com.example.i18n.TrStrings
import com.example.model.Catalog
import com.example.model.InventoryFilter
import com.example.model.StockStatus
import com.example.model.SubscriptionPlan
import com.example.services.LocalStorageDataService
import com.example.ui.screens.settings.SubscriptionUpgradeModal
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYButton
import com.example.ui.components.KYCard
import com.example.ui.components.KYInput
import com.example.ui.components.KYModal
import com.example.ui.components.KYToastData
import com.example.ui.components.KYToastType
import com.example.ui.screens.catalogs.AddTagModal
import com.example.ui.screens.catalogs.AddToCatalogBottomSheet
import com.example.ui.screens.catalogs.CatalogCardItem
import com.example.ui.screens.catalogs.CatalogProductsPreviewSheet
import com.example.ui.screens.catalogs.CreateCatalogOptionsSheet
import com.example.ui.screens.catalogs.CreateFromPhotosModal
import com.example.ui.screens.catalogs.CreateOrEditCatalogModal
import com.example.ui.screens.catalogs.ImportFromCsvModal
import com.example.ui.screens.catalogs.KYShareBottomSheet
import com.example.ui.screens.catalogs.MergeCatalogsModal
import com.example.ui.screens.catalogs.NotificationsBottomSheet
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.BackgroundSlate
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmberDark
import com.example.ui.theme.WarningAmberLight
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ANA EKRAN (/catalogues):
 * - ÜST ÇUBUK: Sol: işletme logosu ve adı. Sağ: bildirim ikonu (Çoklu seçimde: Etiket ekle, Birleştir, Sil).
 * - ARAMA: Katalog adına göre arama kutusu.
 * - ETİKET FİLTRESİ: Yatay kaydırmalı etiket çipleri (Tümü, Yeni Sezon, İndirimli vb.).
 * - KATALOG KARTLARI: 2 sütunlu grid, kapak görseli, ürün sayısı, ziyaretçi/sipariş metrikleri, Paylaş ve ⋯ menüsü, uzun basma ile çoklu seçim.
 * - BOŞ DURUM: İllüstrasyon + "İlk kataloğunu oluştur" butonu.
 * - SAĞ ALT: Yüzen (+) butonu ve 3 seçenekli bottom sheet.
 */
@Composable
fun CatalogsDraftScreen(
    onCatalogClick: (String) -> Unit,
    onOpenDesignSystem: () -> Unit,
    onShowToast: (KYToastData) -> Unit,
    onOpenPublicCatalog: ((String) -> Unit)? = null,
    onOpenInventory: (InventoryFilter) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataService = remember { LocalStorageDataService.getInstance(context) }
    val scope = rememberCoroutineScope()

    val authContext = LocalAuthContext.current
    val store by dataService.settings.collectAsState()

    val catalogs by dataService.catalogs.collectAsState()
    val products by dataService.products.collectAsState()
    val notifications by dataService.notifications.collectAsState()
    val liveViewersByCatalog by dataService.liveViewersByCatalog.collectAsState()

    val lowStockThreshold = store.lowStockThreshold.coerceAtLeast(1)
    val lowStockProducts = remember(products, lowStockThreshold) {
        products.filter {
            it.inStock && it.stockStatus != StockStatus.OUT_OF_STOCK && it.stockCount in 1..lowStockThreshold
        }
    }
    val outOfStockProducts = remember(products) {
        products.filter { !it.inStock || it.stockStatus == StockStatus.OUT_OF_STOCK || (it.trackStock && it.stockCount <= 0) }
    }
    var dismissLowStockBanner by remember { mutableStateOf(false) }

    // Arama & Etiket Filtresi State
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf(TrStrings.Catalogs.TAG_ALL) }

    // Çoklu Seçim (Uzun Basma) State
    var selectedCatalogIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isSelectionMode = selectedCatalogIds.isNotEmpty()

    BackHandler(enabled = isSelectionMode) {
        selectedCatalogIds = emptySet()
    }

    // Tüm mevcut etiketleri dinamik olarak birleştir
    val allTags by remember(catalogs) {
        derivedStateOf {
            val baseTags = listOf(
                TrStrings.Catalogs.TAG_ALL,
                TrStrings.Catalogs.DEFAULT_TAG_NEW_SEASON,
                TrStrings.Catalogs.DEFAULT_TAG_DISCOUNT,
                TrStrings.Catalogs.DEFAULT_TAG_BESTSELLER,
                TrStrings.Catalogs.DEFAULT_TAG_WHOLESALE
            )
            val customTags = catalogs.flatMap { it.tags }
            (baseTags + customTags).distinct()
        }
    }

    // Arama ve etikete göre filtrelenmiş kataloglar
    val filteredCatalogs by remember(catalogs, searchQuery, selectedTag) {
        derivedStateOf {
            catalogs.filter { catalog ->
                val matchesSearch = searchQuery.isBlank() ||
                    catalog.title.contains(searchQuery.trim(), ignoreCase = true) ||
                    catalog.description.contains(searchQuery.trim(), ignoreCase = true)
                val matchesTag = selectedTag == TrStrings.Catalogs.TAG_ALL ||
                    catalog.tags.any { it.equals(selectedTag, ignoreCase = true) }
                matchesSearch && matchesTag
            }
        }
    }

    // Modal ve BottomSheet Kontrolleri
    var showFabSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showCreateOrEditModal by remember { mutableStateOf(false) }
    var editingCatalog by remember { mutableStateOf<Catalog?>(null) }
    var showPhotosModal by remember { mutableStateOf(false) }
    var showCsvModal by remember { mutableStateOf(false) }
    var showCrossCatalogSheet by remember { mutableStateOf(false) }

    var taggingCatalogIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var deletingCatalogIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showMergeModal by remember { mutableStateOf(false) }

    var sharingCatalog by remember { mutableStateOf<Catalog?>(null) }
    var previewingCatalog by remember { mutableStateOf<Catalog?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundSlate)
            .testTag("screen_catalogs_draft")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1) ÜST ÇUBUK: Normal Mod veya Çoklu Seçim Aksiyon Çubuğu
            if (isSelectionMode) {
                MultiSelectTopBar(
                    selectedCount = selectedCatalogIds.size,
                    onClearSelection = { selectedCatalogIds = emptySet() },
                    onAddTagClick = { taggingCatalogIds = selectedCatalogIds },
                    onMergeClick = {
                        if (selectedCatalogIds.size < 2) {
                            onShowToast(
                                KYToastData(
                                    message = TrStrings.Catalogs.TOAST_MERGE_MIN_ERROR,
                                    type = KYToastType.WARNING
                                )
                            )
                        } else {
                            showMergeModal = true
                        }
                    },
                    onDeleteClick = { deletingCatalogIds = selectedCatalogIds }
                )
            } else {
                StoreHeaderTopBar(
                    businessName = store.businessName.ifBlank { "Vera Tasarım Butik" },
                    businessType = store.businessType,
                    logoUri = store.logoUri,
                    unreadNotificationCount = notifications.count { it.isUnread },
                    onNotificationClick = { showNotificationsSheet = true }
                )
            }

            // 1.5) STOK AZ KALINCA ANA EKRANDA UYARI BİLDİRİMİ
            if (!dismissLowStockBanner && (lowStockProducts.isNotEmpty() || outOfStockProducts.isNotEmpty())) {
                val sampleNames = (lowStockProducts + outOfStockProducts)
                    .take(2)
                    .joinToString(", ") { "${it.name} (${it.stockCount} adet)" }

                Surface(
                    shape = ButtonRoundedXl,
                    color = WarningAmberLight,
                    border = BorderStroke(1.dp, WarningAmberDark.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp)
                        .testTag("home_low_stock_alert_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = WarningAmberDark,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${lowStockProducts.size} ürün kritik stokta (≤$lowStockThreshold), ${outOfStockProducts.size} ürün tükendi",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = WarningAmberDark
                            )
                            if (sampleNames.isNotBlank()) {
                                Text(
                                    text = sampleNames,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarningAmberDark.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            onClick = {
                                val targetFilter = if (lowStockProducts.isNotEmpty()) {
                                    InventoryFilter.LOW_STOCK
                                } else {
                                    InventoryFilter.OUT_OF_STOCK
                                }
                                onOpenInventory(targetFilter)
                            },
                            shape = BadgeRoundedPill,
                            color = WarningAmberDark,
                            modifier = Modifier.testTag("btn_home_manage_low_stock")
                        ) {
                            Text(
                                text = TrStrings.Inventory.HOME_LOW_STOCK_MANAGE_BTN,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SurfaceWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        IconButton(
                            onClick = { dismissLowStockBanner = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = TrStrings.Common.CLOSE,
                                tint = WarningAmberDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 2) ARAMA KUTUSU & YATAY ETİKET FİLTRESİ
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                KYInput(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = TrStrings.Catalogs.SEARCH_PLACEHOLDER,
                    leadingIcon = Icons.Outlined.Search,
                    tag = "input_search_catalogs"
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tag_filter_row")
                ) {
                    items(allTags, key = { it }) { tag ->
                        val isSelected = selectedTag.equals(tag, ignoreCase = true)
                        Surface(
                            onClick = { selectedTag = tag },
                            shape = BadgeRoundedPill,
                            color = if (isSelected) PrimaryBlue else SurfaceWhite,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) PrimaryBlue else BorderLight
                            ),
                            modifier = Modifier.testTag("filter_chip_$tag")
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) SurfaceWhite else TextSecondary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 3) KATALOG KARTLARI (2 SÜTUNLU GRID) VEYA BOŞ DURUM
            if (catalogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CatalogsIllustrationEmptyState(
                        onCreateFirstCatalog = { showFabSheet = true },
                        onLoadSampleCatalogs = {
                            scope.launch {
                                dataService.seedSampleCatalogs()
                                onShowToast(
                                    KYToastData(
                                        message = "4 örnek katalog ve 38 ürün yüklendi",
                                        type = KYToastType.SUCCESS
                                    )
                                )
                            }
                        }
                    )
                }
            } else if (filteredCatalogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    KYCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = TrStrings.Catalogs.EMPTY_SEARCH_TITLE,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = TrStrings.Catalogs.EMPTY_SEARCH_DESCRIPTION,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            KYButton(
                                text = "Filtreleri Temizle",
                                onClick = {
                                    searchQuery = ""
                                    selectedTag = TrStrings.Catalogs.TAG_ALL
                                },
                                variant = ButtonVariant.SECONDARY
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("catalogs_grid"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 88.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCatalogs, key = { it.id }) { catalog ->
                        val firstProductCover = remember(products, catalog.id) {
                            products.firstOrNull { it.catalogId == catalog.id }?.imageUrl.orEmpty()
                        }
                        val effectiveCover = catalog.coverImageUrl.ifBlank { firstProductCover }
                        val isCardSelected = catalog.id in selectedCatalogIds

                        CatalogCardItem(
                            catalog = catalog,
                            coverUrl = effectiveCover,
                            isSelectionMode = isSelectionMode,
                            isSelected = isCardSelected,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedCatalogIds = if (isCardSelected) {
                                        selectedCatalogIds - catalog.id
                                    } else {
                                        selectedCatalogIds + catalog.id
                                    }
                                } else {
                                    onCatalogClick(catalog.id)
                                }
                            },
                            onLongClick = {
                                selectedCatalogIds = if (isCardSelected) {
                                    selectedCatalogIds - catalog.id
                                } else {
                                    selectedCatalogIds + catalog.id
                                }
                            },
                            onShareClick = { sharingCatalog = catalog },
                            onEditClick = {
                                editingCatalog = catalog
                                showCreateOrEditModal = true
                            },
                            onDuplicateClick = {
                                scope.launch {
                                    dataService.duplicateCatalog(catalog.id)
                                    onShowToast(
                                        KYToastData(
                                            message = TrStrings.Catalogs.TOAST_DUPLICATED,
                                            type = KYToastType.SUCCESS
                                        )
                                    )
                                }
                            },
                            onAddTagClick = {
                                taggingCatalogIds = setOf(catalog.id)
                            },
                            onToggleVisibilityClick = {
                                scope.launch {
                                    dataService.toggleCatalogVisibility(catalog.id)
                                    onShowToast(
                                        KYToastData(
                                            message = TrStrings.Catalogs.TOAST_VISIBILITY_CHANGED,
                                            type = KYToastType.INFO
                                        )
                                    )
                                }
                            },
                            onDeleteClick = {
                                deletingCatalogIds = setOf(catalog.id)
                            },
                            liveViewersCount = liveViewersByCatalog[catalog.id] ?: 0
                        )
                    }
                }
            }
        }

        // 4) SAĞ ALT YÜZEN (+) BUTONU (FAB)
        FloatingActionButton(
            onClick = { showFabSheet = true },
            containerColor = PrimaryBlue,
            contentColor = SurfaceWhite,
            shape = ButtonRoundedXl,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .size(56.dp)
                .testTag("fab_create_catalog")
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = TrStrings.Catalogs.FAB_SHEET_TITLE,
                modifier = Modifier.size(26.dp)
            )
        }
    }

    // --- ALT ÇEKMECELER VE MODALLAR ---

    // Abonelik Limit Yükseltme Modalı State
    var showUpgradeModal by remember { mutableStateOf(false) }
    var upgradeLimitReason by remember { mutableStateOf<String?>(null) }

    fun checkCatalogLimitOrRun(action: () -> Unit) {
        if (store.subscriptionPlan.isCatalogLimitExceeded(catalogs.size)) {
            val maxCat = store.subscriptionPlan.maxCatalogs ?: 2
            upgradeLimitReason = "${store.subscriptionPlan.titleTr} planda en fazla $maxCat katalog oluşturabilirsiniz (Mevcut: ${catalogs.size} katalog). Yeni katalog eklemek için Lite veya Premium plana yükseltin."
            showUpgradeModal = true
        } else {
            action()
        }
    }

    // Sağ Alt (+) FAB Seçenekleri BottomSheet
    CreateCatalogOptionsSheet(
        isVisible = showFabSheet,
        onDismiss = { showFabSheet = false },
        onSelectCreateEmpty = {
            checkCatalogLimitOrRun {
                editingCatalog = null
                showCreateOrEditModal = true
            }
        },
        onSelectCreateFromPhotos = {
            checkCatalogLimitOrRun {
                showPhotosModal = true
            }
        },
        onSelectImportCsv = {
            checkCatalogLimitOrRun {
                showCsvModal = true
            }
        },
        onSelectCreateCustomFromProducts = {
            checkCatalogLimitOrRun {
                showCrossCatalogSheet = true
            }
        }
    )

    SubscriptionUpgradeModal(
        isVisible = showUpgradeModal,
        limitReasonText = upgradeLimitReason,
        currentPlan = store.subscriptionPlan,
        initialSelectedPlan = SubscriptionPlan.LITE_MONTHLY,
        catalogCount = catalogs.size,
        productCount = products.size,
        onDismiss = { showUpgradeModal = false },
        onConfirmUpgrade = { newPlan ->
            scope.launch {
                dataService.upgradeSubscriptionPlan(newPlan)
                showUpgradeModal = false
                onShowToast(
                    KYToastData(
                        message = "Tebrikler! '${newPlan.titleTr}' planına yükseltildi. Artık yeni katalog oluşturabilirsiniz.",
                        type = KYToastType.SUCCESS
                    )
                )
            }
        }
    )

    // Farklı Kataloglardan Özel Katalog Oluştur / Kataloğa Ekle Sheet'i
    AddToCatalogBottomSheet(
        isVisible = showCrossCatalogSheet,
        initialSelectedProductIds = emptySet(),
        allCatalogs = catalogs,
        allProducts = products,
        showCrossCatalogPickerByDefault = true,
        onDismiss = { showCrossCatalogSheet = false },
        onCreateNewCatalogWithProducts = { newTitle, pickedIds ->
            scope.launch {
                val created = dataService.copyProductsToNewCatalog(pickedIds, newTitle)
                showCrossCatalogSheet = false
                if (created != null) {
                    onShowToast(
                        KYToastData(
                            message = "'${created.title}' oluşturuldu (${pickedIds.size} ürün kopyalandı)",
                            type = KYToastType.SUCCESS
                        )
                    )
                }
            }
        },
        onAddProductsToExistingCatalog = { targetCatId, pickedIds ->
            scope.launch {
                val count = dataService.copyProductsToCatalog(pickedIds, targetCatId)
                showCrossCatalogSheet = false
                onShowToast(
                    KYToastData(
                        message = "$count ürün seçili kataloğa eklendi",
                        type = KYToastType.SUCCESS
                    )
                )
            }
        }
    )

    // Boş Katalog Oluştur / Düzenle Modalı
    CreateOrEditCatalogModal(
        isVisible = showCreateOrEditModal,
        editingCatalog = editingCatalog,
        onDismiss = {
            showCreateOrEditModal = false
            editingCatalog = null
        },
        onSave = { title, description, tag ->
            scope.launch {
                val target = editingCatalog
                if (target == null) {
                    val newId = UUID.randomUUID().toString()
                    val tags = if (tag.isNotBlank()) listOf(tag) else listOf("Yeni Sezon")
                    val cleanSlug = title
                        .replace("İ", "i").replace("I", "i")
                        .replace("Ş", "s").replace("Ğ", "g")
                        .replace("Ü", "u").replace("Ö", "o").replace("Ç", "c")
                        .lowercase(java.util.Locale.ROOT)
                        .replace("i\u0307", "i")
                        .replace("ş", "s").replace("ğ", "g")
                        .replace("ü", "u").replace("ö", "o")
                        .replace("ç", "c").replace("ı", "i")
                        .replace(Regex("[^a-z0-9]+"), "-")
                        .trim('-')
                        .ifBlank { "katalog-${newId.take(6)}" }
                    val newCat = Catalog(
                        id = newId,
                        title = title,
                        description = description,
                        slug = cleanSlug,
                        coverImageUrl = "https://picsum.photos/seed/$newId/600/600",
                        tags = tags,
                        isActive = true,
                        productCount = 0,
                        viewCount = 0,
                        orderCount = 0
                    )
                    dataService.saveCatalog(newCat)
                    onShowToast(KYToastData(TrStrings.Catalogs.TOAST_CREATED, KYToastType.SUCCESS))
                } else {
                    val updatedTags = if (tag.isNotBlank()) {
                        (listOf(tag) + target.tags).distinct()
                    } else {
                        target.tags
                    }
                    dataService.saveCatalog(
                        target.copy(
                            title = title,
                            description = description,
                            tags = updatedTags
                        )
                    )
                    onShowToast(KYToastData(TrStrings.Catalogs.TOAST_UPDATED, KYToastType.SUCCESS))
                }
                showCreateOrEditModal = false
                editingCatalog = null
            }
        }
    )

    // Fotoğraflardan Katalog Oluştur Modalı (Gemini AI destekli)
    CreateFromPhotosModal(
        isVisible = showPhotosModal,
        onDismiss = { showPhotosModal = false },
        onCreateFromPhotos = { title, photoUrls ->
            scope.launch {
                dataService.createCatalogFromPhotos(title = title, photoUrls = photoUrls)
                showPhotosModal = false
                onShowToast(KYToastData(TrStrings.Catalogs.TOAST_CREATED, KYToastType.SUCCESS))
            }
        },
        onCreateFromAiDrafts = { title, drafts ->
            scope.launch {
                val created = dataService.createCatalogFromAiPhotoDrafts(title = title, drafts = drafts)
                showPhotosModal = false
                onShowToast(
                    KYToastData(
                        message = "'${created.title}' (${drafts.size} ürün) Gemini AI ile oluşturuldu!",
                        type = KYToastType.SUCCESS
                    )
                )
            }
        }
    )

    // Excel/CSV'den İçe Aktar Modalı
    ImportFromCsvModal(
        isVisible = showCsvModal,
        onDismiss = { showCsvModal = false },
        onImportCsv = { title, csvContent ->
            scope.launch {
                dataService.importCatalogFromCsv(title = title, csvContent = csvContent)
                showCsvModal = false
                onShowToast(KYToastData(TrStrings.Catalogs.TOAST_CREATED, KYToastType.SUCCESS))
            }
        }
    )

    // Etiket Ekle Modalı (Tekli veya Çoklu: mevcut etiketleri çip olarak gösterir, yeni etiket yazar, Uygula ile kaydeder)
    AddTagModal(
        isVisible = taggingCatalogIds.isNotEmpty(),
        existingTags = allTags.filter { it != TrStrings.Catalogs.TAG_ALL },
        initialSelectedTags = remember(taggingCatalogIds, catalogs) {
            catalogs.filter { it.id in taggingCatalogIds }.flatMap { it.tags }.distinct()
        },
        onDismiss = { taggingCatalogIds = emptySet() },
        onApplyTags = { appliedTags ->
            val targetIds = taggingCatalogIds
            scope.launch {
                dataService.applyTagsToCatalogs(targetIds, appliedTags)
                taggingCatalogIds = emptySet()
                selectedCatalogIds = emptySet()
                onShowToast(KYToastData(TrStrings.Catalogs.TOAST_TAG_ADDED, KYToastType.SUCCESS))
            }
        }
    )

    // Katalogları Birleştir Modalı (Çoklu Seçim)
    MergeCatalogsModal(
        isVisible = showMergeModal,
        selectedCount = selectedCatalogIds.size,
        defaultMergedTitle = "Birleşik Sezon Kataloğu",
        onDismiss = { showMergeModal = false },
        onConfirmMerge = { mergedTitle ->
            val targetIds = selectedCatalogIds
            scope.launch {
                dataService.mergeCatalogs(targetIds, mergedTitle)
                showMergeModal = false
                selectedCatalogIds = emptySet()
                onShowToast(KYToastData(TrStrings.Catalogs.TOAST_MERGED, KYToastType.SUCCESS))
            }
        }
    )

    // Silme Onay Modalı (Tekli veya Çoklu)
    KYModal(
        isVisible = deletingCatalogIds.isNotEmpty(),
        title = TrStrings.Catalogs.MODAL_DELETE_TITLE,
        description = "${deletingCatalogIds.size} katalog kalıcı olarak silinecek. ${TrStrings.Catalogs.MODAL_DELETE_DESC}",
        confirmText = TrStrings.Common.DELETE,
        cancelText = TrStrings.Common.CANCEL,
        onConfirm = {
            val targetIds = deletingCatalogIds
            scope.launch {
                dataService.deleteCatalogs(targetIds)
                deletingCatalogIds = emptySet()
                selectedCatalogIds = emptySet()
                onShowToast(KYToastData(TrStrings.Catalogs.TOAST_DELETED, KYToastType.INFO))
            }
        },
        onDismiss = { deletingCatalogIds = emptySet() }
    )

    // Kapsamlı Paylaşım Alt Çekmecesi (Link, PDF Broşür, Görsel Canvas, Çoklu Katalog + Ayarlar)
    KYShareBottomSheet(
        isVisible = sharingCatalog != null,
        catalog = sharingCatalog,
        product = null,
        onDismiss = { sharingCatalog = null },
        onShowToast = onShowToast,
        onOpenPublicCatalog = onOpenPublicCatalog
    )

    // Bildirimler Alt Çekmecesi
    NotificationsBottomSheet(
        isVisible = showNotificationsSheet,
        notifications = notifications,
        onMarkAllRead = {
            scope.launch { dataService.markAllNotificationsRead() }
        },
        onDismiss = { showNotificationsSheet = false }
    )

    // Katalog Ürünleri Hızlı Önizleme Alt Çekmecesi
    CatalogProductsPreviewSheet(
        catalog = previewingCatalog,
        products = remember(previewingCatalog, products) {
            val id = previewingCatalog?.id
            if (id == null) emptyList() else products.filter { it.catalogId == id }
        },
        onShareCatalog = { cat -> sharingCatalog = cat },
        onEditCatalog = { cat ->
            editingCatalog = cat
            showCreateOrEditModal = true
        },
        onDismiss = { previewingCatalog = null }
    )
}

/**
 * Üst Çubuk: Sol tarafta dairesel işletme logosu ve adı, sağ tarafta bildirim ikonu.
 */
@Composable
private fun StoreHeaderTopBar(
    businessName: String,
    businessType: String,
    logoUri: String,
    unreadNotificationCount: Int,
    onNotificationClick: () -> Unit
) {
    val initials = remember(businessName) {
        businessName
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "KY" }
    }

    Surface(
        color = SurfaceWhite,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("catalogs_top_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Sol: İşletme Logosu ve Adı
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlueSubtle)
                            .border(1.5.dp, PrimaryBlue.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (logoUri.isNotBlank()) {
                            AsyncImage(
                                model = logoUri,
                                contentDescription = businessName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = businessName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = businessType,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Sağ: Bildirim İkonu + Okunmamış Rozeti
                Box {
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_notifications")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = TrStrings.Catalogs.NOTIFICATIONS_TITLE,
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (unreadNotificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(AccentAmber),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unreadNotificationCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SurfaceWhite
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = BorderLight)
        }
    }
}

/**
 * Uzun Basınca Açılan Çoklu Seçim Üst Aksiyon Çubuğu:
 * "Etiket ekle", "Birleştir", "Sil".
 */
@Composable
private fun MultiSelectTopBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onAddTagClick: () -> Unit,
    onMergeClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        color = PrimaryBlueSubtle,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("multi_select_top_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClearSelection,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = TrStrings.Common.CLOSE,
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$selectedCount ${TrStrings.Catalogs.MULTI_SELECT_COUNT_SUFFIX}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryBlue
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Etiket ekle
                    Surface(
                        onClick = onAddTagClick,
                        shape = ButtonRoundedXl,
                        color = SurfaceWhite,
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.testTag("multi_action_add_tag")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Label,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = TrStrings.Catalogs.ACTION_ADD_TAG,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary
                            )
                        }
                    }

                    // Birleştir
                    Surface(
                        onClick = onMergeClick,
                        shape = ButtonRoundedXl,
                        color = SurfaceWhite,
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.testTag("multi_action_merge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Merge,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = TrStrings.Catalogs.ACTION_MERGE,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary
                            )
                        }
                    }

                    // Sil
                    Surface(
                        onClick = onDeleteClick,
                        shape = ButtonRoundedXl,
                        color = SurfaceWhite,
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                        modifier = Modifier.testTag("multi_action_delete")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = TrStrings.Catalogs.ACTION_DELETE,
                                style = MaterialTheme.typography.labelMedium,
                                color = DangerRed
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = PrimaryBlue.copy(alpha = 0.25f))
        }
    }
}

/**
 * Hiç Katalog Yoksa Gösterilen İllüstrasyonlu Boş Durum Kartı.
 */
@Composable
private fun CatalogsIllustrationEmptyState(
    onCreateFirstCatalog: () -> Unit,
    onLoadSampleCatalogs: () -> Unit
) {
    KYCard(tag = "catalogs_empty_state") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Katmanlı Katalog İllüstrasyonu
            Box(
                modifier = Modifier.size(104.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlueSubtle)
                )
                Icon(
                    imageVector = Icons.Outlined.MenuBook,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(44.dp)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(AccentAmber),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesomeMosaic,
                        contentDescription = null,
                        tint = SurfaceWhite,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = TrStrings.Catalogs.EMPTY_TITLE,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = TrStrings.Catalogs.EMPTY_DESCRIPTION,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            KYButton(
                text = TrStrings.Catalogs.FIRST_CATALOG_BUTTON,
                onClick = onCreateFirstCatalog,
                variant = ButtonVariant.PRIMARY,
                leadingIcon = Icons.Filled.Add,
                modifier = Modifier.fillMaxWidth(),
                tag = "btn_create_first_catalog"
            )

            Spacer(modifier = Modifier.height(8.dp))

            KYButton(
                text = TrStrings.Catalogs.LOAD_SAMPLE_CATALOGS,
                onClick = onLoadSampleCatalogs,
                variant = ButtonVariant.GHOST,
                modifier = Modifier.fillMaxWidth(),
                tag = "btn_load_sample_catalogs"
            )
        }
    }
}
