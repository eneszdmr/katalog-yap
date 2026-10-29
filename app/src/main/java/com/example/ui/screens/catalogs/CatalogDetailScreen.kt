package com.example.ui.screens.catalogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.model.Product
import com.example.model.SubscriptionPlan
import com.example.services.LocalStorageDataService
import com.example.ui.screens.settings.SubscriptionUpgradeModal
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
import com.example.ui.theme.AccentAmberLight
import com.example.ui.theme.BackgroundSlate
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmberDark
import kotlinx.coroutines.launch

/**
 * KATALOG DETAY EKRANI (/catalogues/:id):
 * - ÜST: Kapak görseli, katalog adı (düzenlenebilir), açıklama, "Yayında" / "Gizli" rozeti.
 * - METRİK ŞERİDİ: Ziyaretçi, Sipariş, Ürün sayısı kartları.
 * - AKSİYON BUTONLARI: "Paylaş" (primary), "Yönet" (secondary).
 * - ÜRÜN LİSTESİ: Görünüm değiştirici (Grid 2 sütun, Liste, Büyük kart), alt kategori çipleri,
 *   arama kutusu, sıralama menüsü, uzun basma ile çoklu seçim, Yönet modunda sürükle-bırak sıralama,
 *   ve sağ alt (+) "Ürün ekle" FAB.
 */
@Composable
fun CatalogDetailScreen(
    catalogId: String,
    onBackClick: () -> Unit,
    onProductClick: (String) -> Unit,
    onAddProductClick: () -> Unit,
    onShowToast: (KYToastData) -> Unit,
    onOpenPublicCatalog: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataService = remember { LocalStorageDataService.getInstance(context) }
    val scope = rememberCoroutineScope()
    val authContext = LocalAuthContext.current
    val store = authContext.storeSettings

    val catalogs by dataService.catalogs.collectAsState()
    val allProducts by dataService.products.collectAsState()

    val catalog = remember(catalogs, catalogId) {
        catalogs.find { it.id == catalogId }
    }

    val catalogProducts = remember(allProducts, catalogId) {
        allProducts
            .filter { it.catalogId == catalogId }
            .sortedBy { it.sortOrder }
    }

    // Görünüm, Filtre, Arama ve Sıralama State'leri
    var viewMode by remember { mutableStateOf(ProductViewMode.GRID_2_COL) }
    var selectedSubCategory by remember { mutableStateOf(TrStrings.Catalogs.TAG_ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(ProductSortOption.CUSTOM) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Yönet Modu & Yerinde Başlık Düzenleme State
    var isManageMode by remember { mutableStateOf(false) }
    var isEditingTitleInline by remember { mutableStateOf(false) }
    var editedTitleText by remember(catalog?.title) { mutableStateOf(catalog?.title.orEmpty()) }
    var editedDescText by remember(catalog?.description) { mutableStateOf(catalog?.description.orEmpty()) }

    // Çoklu Seçim Modu (Ürüne Uzun Basınca)
    var selectedProductIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isSelectionMode = selectedProductIds.isNotEmpty()

    // Modallar & Alt Çekmeceler
    var showShareSheet by remember { mutableStateOf(false) }
    var showAddProductModal by remember { mutableStateOf(false) }
    var showCopyToNewCatalogModal by remember { mutableStateOf(false) }
    var showAddToCatalogSheet by remember { mutableStateOf(false) }
    var showMoveToCatalogModal by remember { mutableStateOf(false) }
    var showUpdateStockModal by remember { mutableStateOf(false) }
    var showDeleteConfirmModal by remember { mutableStateOf(false) }

    BackHandler {
        when {
            isSelectionMode -> selectedProductIds = emptySet()
            isManageMode -> isManageMode = false
            else -> onBackClick()
        }
    }

    if (catalog == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundSlate)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            KYEmptyState(
                icon = Icons.Outlined.MenuBook,
                title = "Katalog bulunamadı",
                description = "Bu katalog silinmiş veya taşınmış olabilir.",
                actionLabel = TrStrings.Navigation.BACK,
                onActionClick = onBackClick
            )
        }
        return
    }

    // Alt Kategoriler ("Tümü" varsayılan)
    val subCategories by remember(catalogProducts) {
        derivedStateOf {
            val dynamicSubs = catalogProducts.map { it.subCategory }.filter { it.isNotBlank() }.distinct()
            listOf(TrStrings.Catalogs.TAG_ALL) + dynamicSubs
        }
    }

    // Filtrelenmiş ve Sıralanmış Ürünler
    val displayedProducts by remember(
        catalogProducts,
        selectedSubCategory,
        searchQuery,
        sortOption,
        isManageMode
    ) {
        derivedStateOf {
            val filtered = catalogProducts.filter { prod ->
                val matchesCat = selectedSubCategory == TrStrings.Catalogs.TAG_ALL ||
                    prod.subCategory.equals(selectedSubCategory, ignoreCase = true)
                val matchesSearch = searchQuery.isBlank() ||
                    prod.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    prod.sku.contains(searchQuery.trim(), ignoreCase = true)
                matchesCat && matchesSearch
            }

            if (isManageMode) {
                filtered.sortedBy { it.sortOrder }
            } else {
                when (sortOption) {
                    ProductSortOption.CUSTOM -> filtered.sortedBy { it.sortOrder }
                    ProductSortOption.NEWEST -> filtered.sortedByDescending { it.createdAt }
                    ProductSortOption.PRICE_ASC -> filtered.sortedBy { it.price }
                    ProductSortOption.PRICE_DESC -> filtered.sortedByDescending { it.price }
                    ProductSortOption.NAME_AZ -> filtered.sortedBy { it.name.lowercase() }
                }
            }
        }
    }

    fun moveProductInOrder(productId: String, delta: Int) {
        val orderedIds = catalogProducts.map { it.id }.toMutableList()
        val currentIdx = orderedIds.indexOf(productId)
        if (currentIdx == -1) return
        val targetIdx = (currentIdx + delta).coerceIn(0, orderedIds.lastIndex)
        if (currentIdx == targetIdx) return

        val item = orderedIds.removeAt(currentIdx)
        orderedIds.add(targetIdx, item)

        scope.launch {
            dataService.reorderCatalogProducts(catalog.id, orderedIds)
        }
    }

    val effectiveCoverUrl = catalog.coverImageUrl.ifBlank {
        catalogProducts.firstOrNull()?.imageUrl.orEmpty()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundSlate)
            .testTag("screen_catalog_detail")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Çoklu Seçim Üst Aksiyon Çubuğu (Ürüne Uzun Basıldığında)
            if (isSelectionMode) {
                ProductMultiSelectTopBar(
                    selectedCount = selectedProductIds.size,
                    onClearSelection = { selectedProductIds = emptySet() },
                    onCreateNewCatalog = { showCopyToNewCatalogModal = true },
                    onOpenAddToCatalogSheet = { showAddToCatalogSheet = true },
                    onMoveToCatalog = { showMoveToCatalogModal = true },
                    onUpdateStock = { showUpdateStockModal = true },
                    onDeleteSelected = { showDeleteConfirmModal = true }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1) ÜST: Kapak Görseli, Katalog Adı (Düzenlenebilir), Açıklama ve "Yayında"/"Gizli" Rozeti
                item {
                    CatalogDetailHeroHeader(
                        title = catalog.title,
                        description = catalog.description,
                        coverUrl = effectiveCoverUrl,
                        isActive = catalog.isActive,
                        isEditingTitle = isEditingTitleInline,
                        editedTitle = editedTitleText,
                        editedDesc = editedDescText,
                        onEditedTitleChange = { editedTitleText = it },
                        onEditedDescChange = { editedDescText = it },
                        onStartEditTitle = {
                            editedTitleText = catalog.title
                            editedDescText = catalog.description
                            isEditingTitleInline = true
                        },
                        onSaveTitle = {
                            if (editedTitleText.isNotBlank()) {
                                scope.launch {
                                    dataService.saveCatalog(
                                        catalog.copy(
                                            title = editedTitleText.trim(),
                                            description = editedDescText.trim()
                                        )
                                    )
                                    isEditingTitleInline = false
                                    onShowToast(
                                        KYToastData(
                                            message = TrStrings.Catalogs.TOAST_UPDATED,
                                            type = KYToastType.SUCCESS
                                        )
                                    )
                                }
                            }
                        },
                        onCancelEditTitle = { isEditingTitleInline = false },
                        onToggleVisibility = {
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
                        onBackClick = onBackClick
                    )
                }

                // 2) METRİK ŞERİDİ: Ziyaretçi, Sipariş, Ürün Sayısı Kartları
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("catalog_metrics_strip"),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricStripCard(
                            icon = Icons.Outlined.Visibility,
                            label = TrStrings.CatalogDetail.METRIC_VISITORS,
                            value = catalog.viewCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        MetricStripCard(
                            icon = Icons.Outlined.ShoppingBag,
                            label = TrStrings.CatalogDetail.METRIC_ORDERS,
                            value = catalog.orderCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        MetricStripCard(
                            icon = Icons.Outlined.Inventory2,
                            label = TrStrings.CatalogDetail.METRIC_PRODUCTS,
                            value = catalogProducts.size.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 3) AKSİYON BUTONLARI: "Paylaş" (primary), "Yönet" (secondary)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KYButton(
                                text = TrStrings.CatalogDetail.BTN_SHARE,
                                onClick = { showShareSheet = true },
                                variant = ButtonVariant.PRIMARY,
                                leadingIcon = Icons.Outlined.Share,
                                modifier = Modifier.weight(1f),
                                tag = "btn_catalog_detail_share"
                            )
                            KYButton(
                                text = if (isManageMode) {
                                    TrStrings.CatalogDetail.BTN_MANAGE_DONE
                                } else {
                                    TrStrings.CatalogDetail.BTN_MANAGE
                                },
                                onClick = {
                                    isManageMode = !isManageMode
                                    if (isManageMode) {
                                        sortOption = ProductSortOption.CUSTOM
                                    }
                                },
                                variant = ButtonVariant.SECONDARY,
                                leadingIcon = if (isManageMode) Icons.Outlined.Check else Icons.Outlined.Tune,
                                modifier = Modifier.weight(1f),
                                tag = "btn_catalog_detail_manage"
                            )
                        }

                        if (isManageMode) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = ButtonRoundedXl,
                                color = AccentAmberLight,
                                border = BorderStroke(1.dp, WarningAmberDark.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = TrStrings.CatalogDetail.MANAGE_BANNER_HINT,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarningAmberDark,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                // 4) ÜRÜN LİSTESİ KONTROLLERİ:
                // - Görünüm Değiştirici (Grid 2 sütun, Liste, Büyük kart)
                // - Alt kategori (sub-category) çipleri
                // - Arama kutusu & Sıralama menüsü
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Görünüm Değiştirici + Sıralama Menüsü
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Grid / Liste / Büyük Kart Seçici
                            Surface(
                                shape = ButtonRoundedXl,
                                color = SurfaceWhite,
                                border = BorderStroke(1.dp, BorderLight)
                            ) {
                                Row(
                                    modifier = Modifier.padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    ViewModeChip(
                                        mode = ProductViewMode.GRID_2_COL,
                                        icon = Icons.Outlined.GridView,
                                        isSelected = viewMode == ProductViewMode.GRID_2_COL,
                                        onClick = { viewMode = ProductViewMode.GRID_2_COL }
                                    )
                                    ViewModeChip(
                                        mode = ProductViewMode.LIST,
                                        icon = Icons.AutoMirrored.Outlined.ViewList,
                                        isSelected = viewMode == ProductViewMode.LIST,
                                        onClick = { viewMode = ProductViewMode.LIST }
                                    )
                                    ViewModeChip(
                                        mode = ProductViewMode.LARGE_CARD,
                                        icon = Icons.Outlined.ViewAgenda,
                                        isSelected = viewMode == ProductViewMode.LARGE_CARD,
                                        onClick = { viewMode = ProductViewMode.LARGE_CARD }
                                    )
                                }
                            }

                            // Sıralama Menüsü (Yeni, Fiyat artan, Fiyat azalan, A-Z)
                            Box {
                                Surface(
                                    onClick = { showSortMenu = true },
                                    shape = ButtonRoundedXl,
                                    color = SurfaceWhite,
                                    border = BorderStroke(1.dp, BorderLight),
                                    modifier = Modifier.testTag("btn_sort_products")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Sort,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = sortOption.labelTr,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TextPrimary
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                    modifier = Modifier.background(SurfaceWhite)
                                ) {
                                    listOf(
                                        ProductSortOption.NEWEST,
                                        ProductSortOption.PRICE_ASC,
                                        ProductSortOption.PRICE_DESC,
                                        ProductSortOption.NAME_AZ,
                                        ProductSortOption.CUSTOM
                                    ).forEach { opt ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = opt.labelTr,
                                                    fontWeight = if (sortOption == opt) {
                                                        FontWeight.Bold
                                                    } else {
                                                        FontWeight.Normal
                                                    },
                                                    color = if (sortOption == opt) PrimaryBlue else TextPrimary
                                                )
                                            },
                                            onClick = {
                                                sortOption = opt
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Alt Kategori Çipleri (Yatay Kaydırmalı, "Tümü" Varsayılan)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("subcategory_chips_row")
                        ) {
                            items(subCategories, key = { it }) { subCat ->
                                val isSelected = selectedSubCategory.equals(subCat, ignoreCase = true)
                                Surface(
                                    onClick = { selectedSubCategory = subCat },
                                    shape = BadgeRoundedPill,
                                    color = if (isSelected) PrimaryBlue else SurfaceWhite,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) PrimaryBlue else BorderLight
                                    ),
                                    modifier = Modifier.testTag("subcat_chip_$subCat")
                                ) {
                                    Text(
                                        text = subCat,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) SurfaceWhite else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        // Arama Kutusu
                        KYInput(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = TrStrings.CatalogDetail.SEARCH_PRODUCTS_PLACEHOLDER,
                            leadingIcon = Icons.Outlined.Search,
                            tag = "input_search_products"
                        )
                    }
                }

                // 5) ÜRÜN KARTLARI LİSTESİ (Grid 2 Sütun / Liste / Büyük Kart)
                if (displayedProducts.isEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            KYEmptyState(
                                icon = Icons.Outlined.Inventory2,
                                title = "Ürün bulunamadı",
                                description = "Arama kriterlerinize uygun ürün yok veya bu katalog henüz boş.",
                                actionLabel = TrStrings.CatalogDetail.FAB_ADD_PRODUCT,
                                onActionClick = onAddProductClick
                            )
                        }
                    }
                } else {
                    when (viewMode) {
                        ProductViewMode.GRID_2_COL -> {
                            val chunkedRows = displayedProducts.chunked(2)
                            itemsIndexed(
                                items = chunkedRows,
                                key = { _, row -> row.joinToString("-") { it.id } }
                            ) { rowIndex, rowItems ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEachIndexed { colIdx, product ->
                                        val flatIdx = rowIndex * 2 + colIdx
                                        val isProdSelected = product.id in selectedProductIds
                                        ProductGridCard(
                                            product = product,
                                            isSelectionMode = isSelectionMode,
                                            isSelected = isProdSelected,
                                            isManageMode = isManageMode,
                                            canMoveUp = flatIdx > 0,
                                            canMoveDown = flatIdx < displayedProducts.lastIndex,
                                            onMoveUp = { moveProductInOrder(product.id, -1) },
                                            onMoveDown = { moveProductInOrder(product.id, 1) },
                                            onClick = {
                                                if (isSelectionMode) {
                                                    selectedProductIds = if (isProdSelected) {
                                                        selectedProductIds - product.id
                                                    } else {
                                                        selectedProductIds + product.id
                                                    }
                                                } else {
                                                    onProductClick(product.id)
                                                }
                                            },
                                            onLongClick = {
                                                selectedProductIds = if (isProdSelected) {
                                                    selectedProductIds - product.id
                                                } else {
                                                    selectedProductIds + product.id
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        ProductViewMode.LIST -> {
                            itemsIndexed(
                                items = displayedProducts,
                                key = { _, prod -> prod.id }
                            ) { index, product ->
                                val isProdSelected = product.id in selectedProductIds
                                ProductListRowCard(
                                    product = product,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isProdSelected,
                                    isManageMode = isManageMode,
                                    canMoveUp = index > 0,
                                    canMoveDown = index < displayedProducts.lastIndex,
                                    onMoveUp = { moveProductInOrder(product.id, -1) },
                                    onMoveDown = { moveProductInOrder(product.id, 1) },
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedProductIds = if (isProdSelected) {
                                                selectedProductIds - product.id
                                            } else {
                                                selectedProductIds + product.id
                                            }
                                        } else {
                                            onProductClick(product.id)
                                        }
                                    },
                                    onLongClick = {
                                        selectedProductIds = if (isProdSelected) {
                                            selectedProductIds - product.id
                                        } else {
                                            selectedProductIds + product.id
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }

                        ProductViewMode.LARGE_CARD -> {
                            itemsIndexed(
                                items = displayedProducts,
                                key = { _, prod -> prod.id }
                            ) { index, product ->
                                val isProdSelected = product.id in selectedProductIds
                                ProductLargeCard(
                                    product = product,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isProdSelected,
                                    isManageMode = isManageMode,
                                    canMoveUp = index > 0,
                                    canMoveDown = index < displayedProducts.lastIndex,
                                    onMoveUp = { moveProductInOrder(product.id, -1) },
                                    onMoveDown = { moveProductInOrder(product.id, 1) },
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedProductIds = if (isProdSelected) {
                                                selectedProductIds - product.id
                                            } else {
                                                selectedProductIds + product.id
                                            }
                                        } else {
                                            onProductClick(product.id)
                                        }
                                    },
                                    onLongClick = {
                                        selectedProductIds = if (isProdSelected) {
                                            selectedProductIds - product.id
                                        } else {
                                            selectedProductIds + product.id
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6) SAĞ ALT (+) YÜZEN BUTONU: "Ürün ekle" (/products/new ekranına yönlendirir, limit aşılırsa yükseltme modalı açar)
        ExtendedFloatingActionButton(
            onClick = {
                if (store.subscriptionPlan.isProductLimitExceeded(allProducts.size)) {
                    showAddProductModal = true // Uses upgrade modal below when limit exceeded
                } else {
                    onAddProductClick()
                }
            },
            containerColor = PrimaryBlue,
            contentColor = SurfaceWhite,
            shape = ButtonRoundedXl,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null
                )
            },
            text = {
                Text(
                    text = TrStrings.CatalogDetail.FAB_ADD_PRODUCT,
                    style = MaterialTheme.typography.labelLarge
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("fab_add_product")
        )
    }

    // --- MODALLAR VE ALT ÇEKMECELER ---

    // Ürün Limiti Aşıldığında Gösterilen Abonelik Yükseltme Modalı
    SubscriptionUpgradeModal(
        isVisible = showAddProductModal && store.subscriptionPlan.isProductLimitExceeded(allProducts.size),
        limitReasonText = "${store.subscriptionPlan.titleTr} planda en fazla ${store.subscriptionPlan.maxProducts ?: 50} ürün ekleyebilirsiniz (Mevcut: ${allProducts.size} ürün). Daha fazla ürün eklemek için planınızı yükseltin.",
        currentPlan = store.subscriptionPlan,
        initialSelectedPlan = SubscriptionPlan.LITE_MONTHLY,
        catalogCount = catalogs.size,
        productCount = allProducts.size,
        onDismiss = { showAddProductModal = false },
        onConfirmUpgrade = { newPlan ->
            scope.launch {
                dataService.upgradeSubscriptionPlan(newPlan)
                showAddProductModal = false
                onShowToast(
                    KYToastData(
                        message = "Tebrikler! '${newPlan.titleTr}' planına geçildi.",
                        type = KYToastType.SUCCESS
                    )
                )
                onAddProductClick()
            }
        }
    )

    // Katalog Kapsamlı Paylaşım Çekmecesi
    KYShareBottomSheet(
        isVisible = showShareSheet,
        catalog = catalog,
        product = null,
        onDismiss = { showShareSheet = false },
        onShowToast = onShowToast,
        onOpenPublicCatalog = onOpenPublicCatalog
    )

    // Yeni Ürün Ekleme Modalı
    AddOrEditProductModal(
        isVisible = showAddProductModal,
        catalogId = catalog.id,
        currencySymbol = store.currencySymbol.ifBlank { "₺" },
        editingProduct = null,
        onDismiss = { showAddProductModal = false },
        onSaveProduct = { newProduct ->
            scope.launch {
                dataService.saveProduct(newProduct)
                showAddProductModal = false
                onShowToast(
                    KYToastData(
                        message = TrStrings.CatalogDetail.TOAST_PRODUCT_SAVED,
                        type = KYToastType.SUCCESS
                    )
                )
            }
        }
    )

    // Seçili Ürünleri Yeni Kataloğa Ekleme ("Yeni katalog oluştur") Modalı
    CopyProductsToNewCatalogModal(
        isVisible = showCopyToNewCatalogModal,
        selectedCount = selectedProductIds.size,
        onDismiss = { showCopyToNewCatalogModal = false },
        onConfirmCreateCatalog = { newTitle ->
            val ids = selectedProductIds
            scope.launch {
                dataService.copyProductsToNewCatalog(ids, newTitle)
                showCopyToNewCatalogModal = false
                selectedProductIds = emptySet()
                onShowToast(
                    KYToastData(
                        message = TrStrings.CatalogDetail.TOAST_COPIED_TO_NEW_CATALOG,
                        type = KYToastType.SUCCESS
                    )
                )
            }
        }
    )

    // "Kataloğa ekle" BottomSheet (Mevcut katalog seç veya yeni oluştur + farklı kataloglardan ürün seç)
    AddToCatalogBottomSheet(
        isVisible = showAddToCatalogSheet,
        initialSelectedProductIds = selectedProductIds,
        allCatalogs = catalogs,
        allProducts = allProducts,
        showCrossCatalogPickerByDefault = false,
        onDismiss = { showAddToCatalogSheet = false },
        onCreateNewCatalogWithProducts = { newTitle, pickedIds ->
            scope.launch {
                val created = dataService.copyProductsToNewCatalog(pickedIds, newTitle)
                showAddToCatalogSheet = false
                selectedProductIds = emptySet()
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
                showAddToCatalogSheet = false
                selectedProductIds = emptySet()
                onShowToast(
                    KYToastData(
                        message = "$count ürün seçili kataloğa eklendi",
                        type = KYToastType.SUCCESS
                    )
                )
            }
        }
    )

    // Seçili Ürünleri Başka Kataloğa Taşıma Modalı
    MoveProductsToCatalogModal(
        isVisible = showMoveToCatalogModal,
        availableCatalogs = catalogs.filter { it.id != catalog.id },
        onDismiss = { showMoveToCatalogModal = false },
        onConfirmMove = { targetCatalogId ->
            val ids = selectedProductIds
            scope.launch {
                dataService.moveProductsToCatalog(ids, targetCatalogId)
                showMoveToCatalogModal = false
                selectedProductIds = emptySet()
                onShowToast(
                    KYToastData(
                        message = TrStrings.CatalogDetail.TOAST_MOVED_TO_CATALOG,
                        type = KYToastType.SUCCESS
                    )
                )
            }
        }
    )

    // Seçili Ürünlerin Stok Durumunu Güncelleme Modalı
    UpdateStockModal(
        isVisible = showUpdateStockModal,
        selectedCount = selectedProductIds.size,
        onDismiss = { showUpdateStockModal = false },
        onConfirmStockStatus = { status ->
            val ids = selectedProductIds
            scope.launch {
                dataService.updateProductsStockStatus(ids, status)
                showUpdateStockModal = false
                selectedProductIds = emptySet()
                onShowToast(
                    KYToastData(
                        message = TrStrings.CatalogDetail.TOAST_STOCK_UPDATED,
                        type = KYToastType.SUCCESS
                    )
                )
            }
        }
    )

    // Seçili Ürünleri Silme Onay Modalı
    KYModal(
        isVisible = showDeleteConfirmModal,
        title = "Seçili Ürünler Silinsin mi?",
        description = "${selectedProductIds.size} ürün bu katalogdan kalıcı olarak silinecek.",
        confirmText = TrStrings.Common.DELETE,
        cancelText = TrStrings.Common.CANCEL,
        onConfirm = {
            val ids = selectedProductIds
            scope.launch {
                dataService.deleteProducts(ids)
                showDeleteConfirmModal = false
                selectedProductIds = emptySet()
                onShowToast(
                    KYToastData(
                        message = TrStrings.CatalogDetail.TOAST_PRODUCTS_DELETED,
                        type = KYToastType.INFO
                    )
                )
            }
        },
        onDismiss = { showDeleteConfirmModal = false }
    )
}

@Composable
private fun CatalogDetailHeroHeader(
    title: String,
    description: String,
    coverUrl: String,
    isActive: Boolean,
    isEditingTitle: Boolean,
    editedTitle: String,
    editedDesc: String,
    onEditedTitleChange: (String) -> Unit,
    onEditedDescChange: (String) -> Unit,
    onStartEditTitle: () -> Unit,
    onSaveTitle: () -> Unit,
    onCancelEditTitle: () -> Unit,
    onToggleVisibility: () -> Unit,
    onBackClick: () -> Unit
) {
    Surface(
        color = SurfaceWhite,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Kapak Görseli + Üst Geri Butonu + Yayında/Gizli Rozeti
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.9f)
                    .background(SurfaceMuted)
            ) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.48f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.45f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SurfaceWhite.copy(alpha = 0.92f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("btn_catalog_detail_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = TrStrings.Navigation.BACK,
                                tint = TextPrimary
                            )
                        }
                    }

                    // "Yayında" / "Gizli" Rozeti (Tıklanınca değiştirilebilir)
                    KYBadge(
                        text = if (isActive) TrStrings.Common.ACTIVE else TrStrings.Common.HIDDEN,
                        variant = if (isActive) BadgeVariant.SUCCESS else BadgeVariant.NEUTRAL,
                        modifier = Modifier
                            .clickable { onToggleVisibility() }
                            .testTag("badge_catalog_status")
                    )
                }
            }

            // Katalog Adı (Düzenlenebilir) ve Açıklama
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (isEditingTitle) {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = onEditedTitleChange,
                        label = { Text(TrStrings.Catalogs.INPUT_CATALOG_NAME_LABEL) },
                        singleLine = true,
                        shape = ButtonRoundedXl,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inline_input_catalog_title")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editedDesc,
                        onValueChange = onEditedDescChange,
                        label = { Text(TrStrings.Catalogs.INPUT_CATALOG_DESC_LABEL) },
                        singleLine = false,
                        shape = ButtonRoundedXl,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        KYButton(
                            text = TrStrings.Common.CANCEL,
                            onClick = onCancelEditTitle,
                            variant = ButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f)
                        )
                        KYButton(
                            text = TrStrings.Common.SAVE,
                            onClick = onSaveTitle,
                            variant = ButtonVariant.PRIMARY,
                            leadingIcon = Icons.Outlined.Check,
                            modifier = Modifier.weight(1f),
                            tag = "btn_save_inline_title"
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onStartEditTitle,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("btn_edit_catalog_title")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = TrStrings.Common.EDIT,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricStripCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    KYCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlueSubtle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ViewModeChip(
    mode: ProductViewMode,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ButtonRoundedXl,
        color = if (isSelected) PrimaryBlueSubtle else Color.Transparent,
        modifier = Modifier.testTag("view_mode_${mode.name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = mode.labelTr,
                tint = if (isSelected) PrimaryBlue else TextSecondary,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = mode.labelTr,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) PrimaryBlue else TextSecondary
            )
        }
    }
}

/**
 * Ürüne Uzun Basınca Açılan Çoklu Seçim Üst Çubuğu:
 * "Yeni kataloğa ekle", "Başka kataloğa taşı", "Stok güncelle", "Sil".
 */
@Composable
private fun ProductMultiSelectTopBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onCreateNewCatalog: () -> Unit,
    onOpenAddToCatalogSheet: () -> Unit,
    onMoveToCatalog: () -> Unit,
    onUpdateStock: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    Surface(
        color = PrimaryBlueSubtle,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_multi_select_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClearSelection,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = TrStrings.Common.CLOSE,
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = "$selectedCount ürün seçildi",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MultiActionChip(
                    icon = Icons.Outlined.CreateNewFolder,
                    label = TrStrings.CatalogDetail.ACTION_CREATE_NEW_CATALOG,
                    onClick = onCreateNewCatalog,
                    tag = "prod_multi_new_catalog"
                )
                MultiActionChip(
                    icon = Icons.Outlined.FolderCopy,
                    label = TrStrings.CatalogDetail.ACTION_ADD_TO_CATALOG_SHEET,
                    onClick = onOpenAddToCatalogSheet,
                    tag = "prod_multi_add_to_catalog_sheet"
                )
                MultiActionChip(
                    icon = Icons.AutoMirrored.Outlined.DriveFileMove,
                    label = TrStrings.CatalogDetail.ACTION_MOVE_TO_CATALOG,
                    onClick = onMoveToCatalog,
                    tag = "prod_multi_move_catalog"
                )
                MultiActionChip(
                    icon = Icons.Outlined.Inventory2,
                    label = TrStrings.CatalogDetail.ACTION_UPDATE_STOCK,
                    onClick = onUpdateStock,
                    tag = "prod_multi_update_stock"
                )
                MultiActionChip(
                    icon = Icons.Outlined.DeleteOutline,
                    label = TrStrings.CatalogDetail.ACTION_DELETE_PRODUCTS,
                    isDanger = true,
                    onClick = onDeleteSelected,
                    tag = "prod_multi_delete"
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = PrimaryBlue.copy(alpha = 0.2f))
        }
    }
}

@Composable
private fun MultiActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tag: String,
    isDanger: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = ButtonRoundedXl,
        color = SurfaceWhite,
        border = BorderStroke(
            1.dp,
            if (isDanger) DangerRed.copy(alpha = 0.4f) else BorderLight
        ),
        modifier = Modifier.testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDanger) DangerRed else PrimaryBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isDanger) DangerRed else TextPrimary
            )
        }
    }
}
