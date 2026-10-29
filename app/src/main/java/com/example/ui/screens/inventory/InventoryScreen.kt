package com.example.ui.screens.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import com.example.ui.screens.catalogs.AddToCatalogBottomSheet
import com.example.ui.screens.catalogs.CopyProductsToNewCatalogModal
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.i18n.TrStrings
import com.example.model.InventoryFilter
import com.example.model.Product
import com.example.model.StockStatus
import com.example.services.LocalStorageDataService
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYButton
import com.example.ui.components.KYEmptyState
import com.example.ui.components.KYInput
import com.example.ui.components.KYModal
import com.example.ui.components.KYToastData
import com.example.ui.components.KYToastType
import com.example.ui.components.KYTopBar
import com.example.ui.theme.BackgroundSlate
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.CardRounded2xl
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmberDark
import com.example.ui.theme.WarningAmberLight
import kotlinx.coroutines.launch

/**
 * STOK YÖNETİMİ EKRANI (/inventory):
 * - Tüm ürünleri tablo/liste olarak gösterir: görsel, ad, SKU, stok adedi, durum.
 * - Satır içinde hızlı stok güncelleme (+/- butonları ve doğrudan yazma).
 * - Filtreler: Tümü, Stokta, Az kalan (eşik ayarlanabilir, varsayılan 5), Tükendi.
 * - Toplu işlem: seçilenlere stok ata, "Tükendi olarak işaretle".
 * - Excel/CSV'den toplu ürün içe aktarma (sütun eşleştirme + önizleme) ve mevcut ürünleri CSV dışa aktarma.
 */
@Composable
fun InventoryScreen(
    initialFilter: InventoryFilter = InventoryFilter.ALL,
    openImportOnLaunch: Boolean = false,
    onBackClick: () -> Unit,
    onProductClick: (String) -> Unit,
    onShowToast: (KYToastData) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataService = remember { LocalStorageDataService.getInstance(context) }
    val scope = rememberCoroutineScope()

    val products by dataService.products.collectAsState()
    val catalogs by dataService.catalogs.collectAsState()
    val settings by dataService.settings.collectAsState()
    val threshold = settings.lowStockThreshold.coerceAtLeast(1)

    var selectedFilter by remember(initialFilter) { mutableStateOf(initialFilter) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedProductIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var showBulkAssignModal by remember { mutableStateOf(false) }
    var bulkAssignInput by remember { mutableStateOf("25") }
    var showCsvImportModal by remember(openImportOnLaunch) { mutableStateOf(openImportOnLaunch) }
    var showCreateCatalogModal by remember { mutableStateOf(false) }
    var showAddToCatalogSheet by remember { mutableStateOf(false) }

    BackHandler {
        if (selectedProductIds.isNotEmpty()) {
            selectedProductIds = emptySet()
        } else {
            onBackClick()
        }
    }

    // Ürünlerin güncel eşiğe göre stok durumunu değerlendiren yardımcı
    fun effectiveStatus(product: Product): StockStatus {
        return when {
            !product.inStock || product.stockCount <= 0 || product.stockStatus == StockStatus.OUT_OF_STOCK ->
                StockStatus.OUT_OF_STOCK
            product.stockCount <= threshold ->
                StockStatus.LOW_STOCK
            else ->
                StockStatus.IN_STOCK
        }
    }

    val countAll = products.size
    val countInStock = remember(products, threshold) {
        products.count { effectiveStatus(it) == StockStatus.IN_STOCK }
    }
    val countLowStock = remember(products, threshold) {
        products.count { effectiveStatus(it) == StockStatus.LOW_STOCK }
    }
    val countOutOfStock = remember(products, threshold) {
        products.count { effectiveStatus(it) == StockStatus.OUT_OF_STOCK }
    }

    val filteredProducts by remember(products, selectedFilter, searchQuery, threshold) {
        derivedStateOf {
            products.filter { prod ->
                val status = effectiveStatus(prod)
                val matchesFilter = when (selectedFilter) {
                    InventoryFilter.ALL -> true
                    InventoryFilter.IN_STOCK -> status == StockStatus.IN_STOCK
                    InventoryFilter.LOW_STOCK -> status == StockStatus.LOW_STOCK
                    InventoryFilter.OUT_OF_STOCK -> status == StockStatus.OUT_OF_STOCK
                }
                val q = searchQuery.trim()
                val matchesSearch = q.isBlank() ||
                    prod.name.contains(q, ignoreCase = true) ||
                    prod.sku.contains(q, ignoreCase = true) ||
                    prod.subCategory.contains(q, ignoreCase = true)
                matchesFilter && matchesSearch
            }
        }
    }

    val allFilteredSelected = filteredProducts.isNotEmpty() &&
        filteredProducts.all { it.id in selectedProductIds }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundSlate)
            .testTag("screen_inventory")
    ) {
        KYTopBar(
            title = TrStrings.Inventory.TITLE,
            subtitle = "${products.size} ürün • Eşik: ≤$threshold adet",
            onBackClick = onBackClick,
            rightAction = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        onClick = { showCsvImportModal = true },
                        shape = RoundedCornerShape(10.dp),
                        color = PrimaryBlueSubtle,
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)),
                        modifier = Modifier.testTag("btn_inventory_import_csv")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileUpload,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "İçe Aktar",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            InventoryCsvExporter.exportProductsToCsvFileAndShare(
                                context = context,
                                products = products,
                                catalogs = catalogs
                            )
                            onShowToast(
                                KYToastData(
                                    message = TrStrings.Inventory.TOAST_CSV_EXPORT_SUCCESS,
                                    type = KYToastType.SUCCESS
                                )
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceMuted,
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.testTag("btn_inventory_export_csv")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "CSV",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        )

        // Üst Arama + Filtre Çipleri + Ayarlanabilir Az Kalan Eşiği
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KYInput(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = TrStrings.Inventory.SEARCH_PLACEHOLDER,
                leadingIcon = Icons.Outlined.Search,
                tag = "input_inventory_search"
            )

            // Filtre Çipleri: Tümü, Stokta, Az kalan, Tükendi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("inventory_filter_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filterSpecs = listOf(
                    Triple(InventoryFilter.ALL, TrStrings.Inventory.FILTER_ALL, countAll),
                    Triple(InventoryFilter.IN_STOCK, TrStrings.Inventory.FILTER_IN_STOCK, countInStock),
                    Triple(InventoryFilter.LOW_STOCK, TrStrings.Inventory.FILTER_LOW_STOCK, countLowStock),
                    Triple(InventoryFilter.OUT_OF_STOCK, TrStrings.Inventory.FILTER_OUT_OF_STOCK, countOutOfStock)
                )

                filterSpecs.forEach { (filter, label, count) ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        onClick = { selectedFilter = filter },
                        shape = BadgeRoundedPill,
                        color = if (isSelected) PrimaryBlue else SurfaceMuted,
                        border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderLight),
                        modifier = Modifier.testTag("inventory_filter_${filter.name.lowercase()}")
                    ) {
                        Text(
                            text = "$label ($count)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) SurfaceWhite else TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Az Kalan Eşiği Ayarlayıcı + Tümünü Seç Satırı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            selectedProductIds = if (allFilteredSelected) {
                                selectedProductIds - filteredProducts.map { it.id }.toSet()
                            } else {
                                selectedProductIds + filteredProducts.map { it.id }.toSet()
                            }
                        }
                        .padding(end = 8.dp)
                        .testTag("btn_inventory_select_all")
                ) {
                    Checkbox(
                        checked = allFilteredSelected,
                        onCheckedChange = { checked ->
                            selectedProductIds = if (checked) {
                                selectedProductIds + filteredProducts.map { it.id }.toSet()
                            } else {
                                selectedProductIds - filteredProducts.map { it.id }.toSet()
                            }
                        },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (selectedProductIds.isEmpty()) {
                            TrStrings.Inventory.SELECT_ALL
                        } else {
                            "${selectedProductIds.size} ${TrStrings.Inventory.SELECTED_SUFFIX}"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (selectedProductIds.isNotEmpty()) PrimaryBlue else TextSecondary
                    )
                }

                // Az kalan eşiği ayarlayıcı (varsayılan 5)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = WarningAmberLight.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, WarningAmberDark.copy(alpha = 0.25f)),
                    modifier = Modifier.testTag("inventory_threshold_stepper")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = null,
                            tint = WarningAmberDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${TrStrings.Inventory.THRESHOLD_CHIP_PREFIX} $threshold",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = WarningAmberDark
                        )
                        Surface(
                            onClick = {
                                if (threshold > 1) {
                                    scope.launch { dataService.updateLowStockThreshold(threshold - 1) }
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceWhite,
                            modifier = Modifier
                                .size(22.dp)
                                .testTag("btn_threshold_decrease")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Remove,
                                    contentDescription = "Eşiği Azalt",
                                    tint = WarningAmberDark,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Surface(
                            onClick = {
                                scope.launch { dataService.updateLowStockThreshold(threshold + 1) }
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceWhite,
                            modifier = Modifier
                                .size(22.dp)
                                .testTag("btn_threshold_increase")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Eşiği Artır",
                                    tint = WarningAmberDark,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Seçili Ürünler Varsa Toplu İşlem Çubuğu (Stok Ata / Tükendi Olarak İşaretle / Yeni Katalog Oluştur / Kataloğa Ekle)
        if (selectedProductIds.isNotEmpty()) {
            Surface(
                color = PrimaryBlueSubtle,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inventory_bulk_actions_bar")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KYButton(
                            text = TrStrings.Inventory.BTN_BULK_ASSIGN_STOCK,
                            onClick = {
                                bulkAssignInput = "25"
                                showBulkAssignModal = true
                            },
                            variant = ButtonVariant.PRIMARY,
                            leadingIcon = Icons.Outlined.Edit,
                            modifier = Modifier.weight(1f),
                            tag = "btn_bulk_assign_stock"
                        )
                        KYButton(
                            text = TrStrings.Inventory.BTN_BULK_MARK_OUT_OF_STOCK,
                            onClick = {
                                val ids = selectedProductIds
                                scope.launch {
                                    dataService.markProductsOutOfStock(ids)
                                    selectedProductIds = emptySet()
                                    onShowToast(
                                        KYToastData(
                                            message = TrStrings.Inventory.TOAST_BULK_OUT_OF_STOCK,
                                            type = KYToastType.WARNING
                                        )
                                    )
                                }
                            },
                            variant = ButtonVariant.SECONDARY,
                            leadingIcon = Icons.Outlined.Block,
                            modifier = Modifier.weight(1.1f),
                            tag = "btn_bulk_mark_out_of_stock"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KYButton(
                            text = TrStrings.CatalogDetail.ACTION_CREATE_NEW_CATALOG,
                            onClick = { showCreateCatalogModal = true },
                            variant = ButtonVariant.SECONDARY,
                            leadingIcon = Icons.Outlined.CreateNewFolder,
                            modifier = Modifier.weight(1.1f),
                            tag = "btn_inventory_create_new_catalog"
                        )
                        KYButton(
                            text = TrStrings.CatalogDetail.ACTION_ADD_TO_CATALOG_SHEET,
                            onClick = { showAddToCatalogSheet = true },
                            variant = ButtonVariant.SECONDARY,
                            leadingIcon = Icons.Outlined.FolderCopy,
                            modifier = Modifier.weight(1f),
                            tag = "btn_inventory_add_to_catalog_sheet"
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = BorderLight)

        // Tablo Sütun Başlığı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceMuted)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = TrStrings.Inventory.COL_PRODUCT,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary,
                modifier = Modifier.weight(1.6f)
            )
            Text(
                text = TrStrings.Inventory.COL_STOCK_COUNT,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1.15f)
            )
            Text(
                text = TrStrings.Inventory.COL_STATUS,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.75f)
            )
        }

        HorizontalDivider(color = BorderLight)

        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                KYEmptyState(
                    icon = Icons.Outlined.Inventory2,
                    title = "Bu filtrede ürün bulunamadı",
                    description = "Arama terimini veya stok filtresini değiştirerek tüm ürünlerinizi görüntüleyebilirsiniz.",
                    actionLabel = "Tüm Ürünleri Göster",
                    onActionClick = {
                        selectedFilter = InventoryFilter.ALL
                        searchQuery = ""
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("inventory_product_list"),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    val status = effectiveStatus(product)
                    val isSelected = product.id in selectedProductIds

                    InventoryProductTableRow(
                        product = product,
                        status = status,
                        isSelected = isSelected,
                        onToggleSelect = {
                            selectedProductIds = if (isSelected) {
                                selectedProductIds - product.id
                            } else {
                                selectedProductIds + product.id
                            }
                        },
                        onProductClick = { onProductClick(product.id) },
                        onUpdateStockCount = { newCount ->
                            scope.launch {
                                dataService.updateProductStockCount(product.id, newCount)
                            }
                        }
                    )
                }
            }
        }
    }

    // Toplu Stok Atama Modalı
    KYModal(
        isVisible = showBulkAssignModal,
        title = TrStrings.Inventory.MODAL_BULK_STOCK_TITLE,
        description = "${selectedProductIds.size} ürün seçildi. ${TrStrings.Inventory.MODAL_BULK_STOCK_DESC}",
        confirmText = TrStrings.Inventory.BTN_BULK_ASSIGN_STOCK,
        onConfirm = {
            val targetCount = bulkAssignInput.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
            val ids = selectedProductIds
            scope.launch {
                dataService.bulkAssignProductStock(ids, targetCount)
                showBulkAssignModal = false
                selectedProductIds = emptySet()
                onShowToast(
                    KYToastData(
                        message = TrStrings.Inventory.TOAST_BULK_STOCK_UPDATED,
                        type = KYToastType.SUCCESS
                    )
                )
            }
        },
        onDismiss = { showBulkAssignModal = false }
    ) {
        KYInput(
            value = bulkAssignInput,
            onValueChange = { input ->
                bulkAssignInput = input.filter { it.isDigit() }
            },
            label = TrStrings.Inventory.INPUT_BULK_STOCK_LABEL,
            placeholder = "Örn: 25",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            tag = "input_bulk_assign_stock"
        )
    }

    // Excel / CSV Toplu Ürün İçe Aktarma Modalı (Dosya yükle + Sütun Eşleştirme + Önizleme + İçe Aktar)
    BulkCsvImportModal(
        isVisible = showCsvImportModal,
        catalogs = catalogs,
        currencySymbol = settings.currencySymbol.ifBlank { "₺" },
        lowStockThreshold = threshold,
        onDismiss = { showCsvImportModal = false },
        onConfirmImport = { targetCatalogId, importedProducts ->
            scope.launch {
                val count = dataService.bulkImportProducts(targetCatalogId, importedProducts)
                showCsvImportModal = false
                onShowToast(
                    KYToastData(
                        message = "$count ${TrStrings.Inventory.TOAST_CSV_IMPORT_SUCCESS}",
                        type = KYToastType.SUCCESS
                    )
                )
            }
        }
    )

    // Seçili Ürünlerden Yeni Katalog Oluşturma Modalı
    CopyProductsToNewCatalogModal(
        isVisible = showCreateCatalogModal,
        selectedCount = selectedProductIds.size,
        onDismiss = { showCreateCatalogModal = false },
        onConfirmCreateCatalog = { newTitle ->
            val ids = selectedProductIds
            scope.launch {
                val created = dataService.copyProductsToNewCatalog(ids, newTitle)
                showCreateCatalogModal = false
                selectedProductIds = emptySet()
                if (created != null) {
                    onShowToast(
                        KYToastData(
                            message = "'${created.title}' oluşturuldu (${ids.size} ürün kopyalandı)",
                            type = KYToastType.SUCCESS
                        )
                    )
                }
            }
        }
    )

    // Kataloğa Ekle Sheet'i (Mevcut Katalog Seç veya Yeni Oluştur + Farklı Kataloglardan Ürün Seç)
    AddToCatalogBottomSheet(
        isVisible = showAddToCatalogSheet,
        initialSelectedProductIds = selectedProductIds,
        allCatalogs = catalogs,
        allProducts = products,
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
}

@Composable
private fun InventoryProductTableRow(
    product: Product,
    status: StockStatus,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onProductClick: () -> Unit,
    onUpdateStockCount: (Int) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var textStock by remember(product.id, product.stockCount) {
        mutableStateOf(product.stockCount.toString())
    }

    LaunchedEffect(product.stockCount) {
        textStock = product.stockCount.toString()
    }

    Surface(
        shape = CardRounded2xl,
        color = if (isSelected) PrimaryBlueSubtle.copy(alpha = 0.55f) else SurfaceWhite,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) PrimaryBlue else BorderLight
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_row_${product.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue),
                modifier = Modifier.size(32.dp)
            )

            // 1) Görsel + Ürün Adı + SKU
            Row(
                modifier = Modifier
                    .weight(1.55f)
                    .clickable { onProductClick() }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceMuted)
                )
                Column {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = product.sku.ifBlank { "SKU-YOK" },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${product.price.toInt()} ${product.currency}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = PrimaryBlue
                    )
                }
            }

            // 2) Satır İçinde Hızlı Stok Güncelleme (- / Doğrudan Yazma / +)
            Row(
                modifier = Modifier.weight(1.15f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        val next = (product.stockCount - 1).coerceAtLeast(0)
                        textStock = next.toString()
                        onUpdateStockCount(next)
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceMuted,
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("btn_stock_minus_${product.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Stok Azalt",
                            tint = TextPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Doğrudan stok adedi yazma alanı
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BackgroundSlate)
                        .border(1.dp, BorderLight, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = textStock,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() }.take(5)
                            textStock = clean
                            val parsed = clean.toIntOrNull()
                            if (parsed != null) {
                                onUpdateStockCount(parsed)
                            }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(PrimaryBlue),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val finalCount = textStock.toIntOrNull() ?: 0
                                textStock = finalCount.toString()
                                onUpdateStockCount(finalCount)
                                focusManager.clearFocus()
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_stock_${product.id}")
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Surface(
                    onClick = {
                        val next = product.stockCount + 1
                        textStock = next.toString()
                        onUpdateStockCount(next)
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryBlueSubtle,
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("btn_stock_plus_${product.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Stok Artır",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // 3) Stok Durum Rozeti (Stokta / Az kaldı / Tükendi)
            Box(
                modifier = Modifier.weight(0.75f),
                contentAlignment = Alignment.CenterEnd
            ) {
                val badgeVariant = when (status) {
                    StockStatus.IN_STOCK -> BadgeVariant.SUCCESS
                    StockStatus.LOW_STOCK -> BadgeVariant.WARNING
                    StockStatus.OUT_OF_STOCK -> BadgeVariant.DANGER
                }
                KYBadge(
                    text = status.labelTr,
                    variant = badgeVariant
                )
            }
        }
    }
}
