package com.example.ui.screens.catalogs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.i18n.TrStrings
import com.example.model.Catalog
import com.example.model.Product
import com.example.model.ProductVariantGroup
import com.example.model.StockStatus
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYButton
import com.example.ui.components.KYInput
import com.example.ui.components.KYModal
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.PrimaryBlue
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import com.example.services.DescriptionTone
import com.example.services.GeminiProductService
import com.example.ui.components.KYBottomSheet
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Ürün Ekleme veya Düzenleme Modalı.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddOrEditProductModal(
    isVisible: Boolean,
    catalogId: String,
    currencySymbol: String,
    editingProduct: Product?,
    onDismiss: () -> Unit,
    onSaveProduct: (Product) -> Unit
) {
    if (!isVisible) return

    var name by remember(editingProduct) { mutableStateOf(editingProduct?.name.orEmpty()) }
    var priceText by remember(editingProduct) {
        mutableStateOf(editingProduct?.price?.toInt()?.toString().orEmpty())
    }
    var originalPriceText by remember(editingProduct) {
        mutableStateOf(editingProduct?.originalPrice?.toInt()?.toString().orEmpty())
    }
    var subCategory by remember(editingProduct) {
        mutableStateOf(editingProduct?.subCategory?.ifBlank { "Yeni Gelenler" } ?: "Yeni Gelenler")
    }
    var sku by remember(editingProduct) {
        mutableStateOf(editingProduct?.sku?.ifBlank { "KY-${(100..999).random()}" } ?: "KY-${(100..999).random()}")
    }
    var description by remember(editingProduct) {
        mutableStateOf(editingProduct?.description.orEmpty())
    }
    var selectedStockStatus by remember(editingProduct) {
        mutableStateOf(editingProduct?.stockStatus ?: StockStatus.IN_STOCK)
    }
    var pickedImageUri by remember(editingProduct) {
        mutableStateOf(editingProduct?.imageUrl.orEmpty())
    }
    var nameError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    var selectedTone by remember { mutableStateOf(DescriptionTone.CORPORATE) }
    var isImprovingDesc by remember { mutableStateOf(false) }
    var descAiError by remember { mutableStateOf<String?>(null) }

    fun triggerImproveDescriptionInModal() {
        if (isImprovingDesc) return
        scope.launch {
            isImprovingDesc = true
            descAiError = null
            val result = GeminiProductService.improveProductDescription(
                productName = name,
                currentDescription = description,
                category = subCategory,
                tone = selectedTone
            )
            isImprovingDesc = false
            result.onSuccess { improved ->
                description = improved
            }.onFailure { err ->
                descAiError = err.localizedMessage ?: "Açıklama iyileştirilemedi."
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedImageUri = uri.toString()
        }
    }

    KYModal(
        isVisible = true,
        title = if (editingProduct == null) {
            TrStrings.CatalogDetail.MODAL_ADD_PRODUCT_TITLE
        } else {
            TrStrings.CatalogDetail.MODAL_EDIT_PRODUCT_TITLE
        },
        confirmText = TrStrings.Common.SAVE,
        cancelText = TrStrings.Common.CANCEL,
        onConfirm = {
            if (name.isBlank()) {
                nameError = "Lütfen ürün adını girin"
            } else {
                val parsedPrice = priceText.replace(",", ".").toDoubleOrNull() ?: 450.0
                val parsedOrig = originalPriceText.replace(",", ".").toDoubleOrNull()
                val prodId = editingProduct?.id ?: UUID.randomUUID().toString()
                val mainImage = pickedImageUri.ifBlank {
                    "https://picsum.photos/seed/$prodId/600/600"
                }

                val product = Product(
                    id = prodId,
                    catalogId = catalogId,
                    name = name.trim(),
                    description = description.trim().ifBlank { "Özel koleksiyon ürünü." },
                    price = parsedPrice,
                    originalPrice = parsedOrig?.takeIf { it > parsedPrice },
                    currency = currencySymbol,
                    imageUrl = mainImage,
                    galleryUrls = editingProduct?.galleryUrls?.takeIf { it.isNotEmpty() } ?: listOf(
                        mainImage,
                        "https://picsum.photos/seed/$prodId-2/600/600",
                        "https://picsum.photos/seed/$prodId-3/600/600"
                    ),
                    subCategory = subCategory.trim().ifBlank { "Genel" },
                    stockStatus = selectedStockStatus,
                    stockCount = when (selectedStockStatus) {
                        StockStatus.IN_STOCK -> 25
                        StockStatus.LOW_STOCK -> 4
                        StockStatus.OUT_OF_STOCK -> 0
                    },
                    inStock = selectedStockStatus != StockStatus.OUT_OF_STOCK,
                    sku = sku.trim().ifBlank { "SKU-100" },
                    variants = editingProduct?.variants?.takeIf { it.isNotEmpty() } ?: listOf(
                        ProductVariantGroup("Beden", listOf("S", "M", "L", "XL")),
                        ProductVariantGroup("Renk", listOf("Standart", "Siyah", "Ekru"))
                    ),
                    sortOrder = editingProduct?.sortOrder ?: 0
                )
                onSaveProduct(product)
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
                label = TrStrings.CatalogDetail.INPUT_PROD_NAME,
                placeholder = "Örn: Keten Kruvaze Ceket",
                errorText = nameError,
                tag = "input_product_name"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KYInput(
                    value = priceText,
                    onValueChange = { priceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = TrStrings.CatalogDetail.INPUT_PROD_PRICE,
                    placeholder = "890",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    tag = "input_product_price"
                )
                KYInput(
                    value = originalPriceText,
                    onValueChange = { originalPriceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = "Eski Fiyat",
                    placeholder = "1150",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    tag = "input_product_original_price"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KYInput(
                    value = subCategory,
                    onValueChange = { subCategory = it },
                    label = TrStrings.CatalogDetail.INPUT_PROD_SUBCATEGORY,
                    placeholder = "Üst Giyim",
                    modifier = Modifier.weight(1f),
                    tag = "input_product_subcategory"
                )
                KYInput(
                    value = sku,
                    onValueChange = { sku = it },
                    label = TrStrings.CatalogDetail.INPUT_PROD_SKU,
                    placeholder = "VR-101",
                    modifier = Modifier.weight(1f),
                    tag = "input_product_sku"
                )
            }

            Text(
                text = TrStrings.CatalogDetail.INPUT_PROD_STOCK_STATUS,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StockStatus.entries.forEach { status ->
                    val isSelected = selectedStockStatus == status
                    KYBadge(
                        text = status.labelTr,
                        variant = when {
                            !isSelected -> BadgeVariant.NEUTRAL
                            status == StockStatus.IN_STOCK -> BadgeVariant.SUCCESS
                            status == StockStatus.LOW_STOCK -> BadgeVariant.WARNING
                            else -> BadgeVariant.DANGER
                        },
                        modifier = Modifier.clickable { selectedStockStatus = status }
                    )
                }
            }

            KYInput(
                value = description,
                onValueChange = { description = it },
                label = TrStrings.CatalogDetail.INPUT_PROD_DESC,
                placeholder = "Kumaş, materyal ve ürün özellikleri...",
                singleLine = false,
                tag = "input_product_desc"
            )

            // ✨ Açıklamayı İyileştir (Kurumsal, Samimi, Kısa)
            Surface(
                shape = ButtonRoundedXl,
                color = PrimaryBlueSubtle.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("modal_ai_description_improver")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DescriptionTone.entries.forEach { tone ->
                            val isSelected = selectedTone == tone
                            Surface(
                                onClick = { selectedTone = tone },
                                shape = BadgeRoundedPill,
                                color = if (isSelected) PrimaryBlue else SurfaceWhite,
                                border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderLight),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("modal_tone_chip_${tone.name.lowercase()}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = tone.labelTr,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) SurfaceWhite else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    if (isImprovingDesc) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = PrimaryBlue,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "'${selectedTone.labelTr}' tonunda açıklama iyileştiriliyor...",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlue
                            )
                        }
                    }

                    if (descAiError != null) {
                        Surface(
                            shape = ButtonRoundedXl,
                            color = DangerRedLight,
                            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = DangerRed,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = descAiError.orEmpty(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DangerRed,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    onClick = { triggerImproveDescriptionInModal() },
                                    shape = BadgeRoundedPill,
                                    color = SurfaceWhite,
                                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                                    modifier = Modifier.testTag("modal_btn_retry_improve_desc")
                                ) {
                                    Text(
                                        text = "Tekrar Dene",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = DangerRed,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    KYButton(
                        text = if (isImprovingDesc) {
                            "İyileştiriliyor..."
                        } else {
                            "✨ Açıklamayı iyileştir (${selectedTone.labelTr})"
                        },
                        onClick = { triggerImproveDescriptionInModal() },
                        variant = ButtonVariant.SECONDARY,
                        leadingIcon = Icons.Outlined.AutoAwesome,
                        enabled = !isImprovingDesc,
                        modifier = Modifier.fillMaxWidth(),
                        tag = "modal_btn_improve_description"
                    )
                }
            }

            KYButton(
                text = if (pickedImageUri.isBlank()) {
                    "Ürün Görseli Seç (veya Otomatik)"
                } else {
                    "Görsel Seçildi (Değiştir)"
                },
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                variant = ButtonVariant.SECONDARY,
                leadingIcon = Icons.Outlined.AddPhotoAlternate,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Çoklu Seçimde Stok Durumu Güncelleme Modalı.
 */
@Composable
fun UpdateStockModal(
    isVisible: Boolean,
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirmStockStatus: (StockStatus) -> Unit
) {
    if (!isVisible) return

    var chosenStatus by remember { mutableStateOf(StockStatus.IN_STOCK) }

    KYModal(
        isVisible = true,
        title = "${TrStrings.CatalogDetail.MODAL_STOCK_UPDATE_TITLE} ($selectedCount)",
        description = TrStrings.CatalogDetail.MODAL_STOCK_UPDATE_DESC,
        confirmText = TrStrings.Common.SAVE,
        onConfirm = { onConfirmStockStatus(chosenStatus) },
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StockStatus.entries.forEach { status ->
                val isSelected = chosenStatus == status
                Surface(
                    onClick = { chosenStatus = status },
                    shape = ButtonRoundedXl,
                    color = if (isSelected) PrimaryBlueSubtle else SurfaceWhite,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) PrimaryBlue else BorderLight
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ProductStockBadge(status = status)
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Çoklu Seçimde Ürünleri Başka Kataloğa Taşıma Modalı.
 */
@Composable
fun MoveProductsToCatalogModal(
    isVisible: Boolean,
    availableCatalogs: List<Catalog>,
    onDismiss: () -> Unit,
    onConfirmMove: (targetCatalogId: String) -> Unit
) {
    if (!isVisible) return

    var selectedTargetId by remember(availableCatalogs) {
        mutableStateOf(availableCatalogs.firstOrNull()?.id.orEmpty())
    }

    KYModal(
        isVisible = true,
        title = TrStrings.CatalogDetail.MODAL_MOVE_PRODS_TITLE,
        description = TrStrings.CatalogDetail.MODAL_MOVE_PRODS_DESC,
        confirmText = "Taşı",
        onConfirm = {
            if (selectedTargetId.isNotBlank()) {
                onConfirmMove(selectedTargetId)
            }
        },
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            availableCatalogs.forEach { cat ->
                val isSelected = cat.id == selectedTargetId
                Surface(
                    onClick = { selectedTargetId = cat.id },
                    shape = ButtonRoundedXl,
                    color = if (isSelected) PrimaryBlueSubtle else SurfaceWhite,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) PrimaryBlue else BorderLight
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = cat.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (isSelected) PrimaryBlue else TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Çoklu Seçimde Seçilen Ürünlerden "Yeni Katalog Oluştur" Modalı.
 * Kullanıcıya yeni katalog adını sorar ve seçili ürünleri yeni kataloğa kopyalar.
 */
@Composable
fun CopyProductsToNewCatalogModal(
    isVisible: Boolean,
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirmCreateCatalog: (String) -> Unit
) {
    if (!isVisible) return

    var newCatalogTitle by remember { mutableStateOf("Özel Seçki Kataloğu") }
    var titleError by remember { mutableStateOf<String?>(null) }

    KYModal(
        isVisible = true,
        title = "${TrStrings.CatalogDetail.MODAL_NEW_CAT_FROM_PRODS_TITLE} ($selectedCount Ürün)",
        description = TrStrings.CatalogDetail.MODAL_NEW_CAT_FROM_PRODS_DESC,
        confirmText = TrStrings.Common.CREATE,
        onConfirm = {
            if (newCatalogTitle.isBlank()) {
                titleError = "Lütfen yeni katalog adını girin"
            } else {
                onConfirmCreateCatalog(newCatalogTitle.trim())
            }
        },
        onDismiss = onDismiss
    ) {
        KYInput(
            value = newCatalogTitle,
            onValueChange = {
                newCatalogTitle = it
                if (titleError != null) titleError = null
            },
            label = TrStrings.Catalogs.INPUT_CATALOG_NAME_LABEL,
            placeholder = "Örn: VIP Müşteri Seçkisi",
            errorText = titleError,
            tag = "input_new_catalog_from_products_title"
        )
    }
}

/**
 * "Kataloğa ekle" BottomSheet:
 * - Mevcut katalog seç veya yeni katalog oluştur.
 * - Farklı kataloglardan ürün seçerek tek bir yeni katalog (veya mevcut kataloğa ekleme) yapma akışını destekler.
 */
@Composable
fun AddToCatalogBottomSheet(
    isVisible: Boolean,
    initialSelectedProductIds: Set<String>,
    allCatalogs: List<Catalog>,
    allProducts: List<Product>,
    showCrossCatalogPickerByDefault: Boolean = false,
    onDismiss: () -> Unit,
    onCreateNewCatalogWithProducts: (newCatalogTitle: String, productIds: Set<String>) -> Unit,
    onAddProductsToExistingCatalog: (targetCatalogId: String, productIds: Set<String>) -> Unit
) {
    if (!isVisible) return

    var createNewMode by remember { mutableStateOf(true) }
    var newCatalogTitle by remember { mutableStateOf("Özel Seçki Kataloğu") }
    var titleError by remember { mutableStateOf<String?>(null) }
    var selectedExistingCatalogId by remember(allCatalogs) {
        mutableStateOf(allCatalogs.firstOrNull()?.id.orEmpty())
    }
    var showCrossPicker by remember(showCrossCatalogPickerByDefault) {
        mutableStateOf(showCrossCatalogPickerByDefault || initialSelectedProductIds.isEmpty())
    }
    var filterCatalogId by remember { mutableStateOf("ALL") }

    val pickedProductIds = remember(initialSelectedProductIds) {
        mutableStateListOf<String>().apply {
            addAll(initialSelectedProductIds)
        }
    }

    val distinctSelectedCatalogCount = remember(pickedProductIds.toList(), allProducts) {
        allProducts
            .filter { it.id in pickedProductIds }
            .map { it.catalogId }
            .distinct()
            .size
    }

    val filteredPickerProducts = remember(allProducts, filterCatalogId) {
        if (filterCatalogId == "ALL") {
            allProducts
        } else {
            allProducts.filter { it.catalogId == filterCatalogId }
        }
    }

    KYBottomSheet(
        isVisible = true,
        title = TrStrings.CatalogDetail.SHEET_ADD_TO_CATALOG_TITLE,
        subtitle = TrStrings.CatalogDetail.SHEET_ADD_TO_CATALOG_SUBTITLE,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Üst Mod Seçici: "Yeni Katalog Oluştur" veya "Mevcut Katalog Seç"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ButtonRoundedXl)
                    .background(SurfaceMuted)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = { createNewMode = true },
                    shape = ButtonRoundedXl,
                    color = if (createNewMode) SurfaceWhite else androidx.compose.ui.graphics.Color.Transparent,
                    shadowElevation = if (createNewMode) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_mode_create_new_catalog")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = if (createNewMode) PrimaryBlue else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = TrStrings.CatalogDetail.MODE_CREATE_NEW_CATALOG,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (createNewMode) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (createNewMode) PrimaryBlue else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    onClick = { createNewMode = false },
                    shape = ButtonRoundedXl,
                    color = if (!createNewMode) SurfaceWhite else androidx.compose.ui.graphics.Color.Transparent,
                    shadowElevation = if (!createNewMode) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_mode_select_existing_catalog")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderCopy,
                            contentDescription = null,
                            tint = if (!createNewMode) PrimaryBlue else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = TrStrings.CatalogDetail.MODE_SELECT_EXISTING_CATALOG,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (!createNewMode) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (!createNewMode) PrimaryBlue else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Seçilen moda göre: Yeni Katalog Adı Girişi VEYA Mevcut Katalog Listesi
            if (createNewMode) {
                KYInput(
                    value = newCatalogTitle,
                    onValueChange = {
                        newCatalogTitle = it
                        if (titleError != null) titleError = null
                    },
                    label = TrStrings.Catalogs.INPUT_CATALOG_NAME_LABEL,
                    placeholder = "Örn: Özel Sezon Seçkisi",
                    errorText = titleError,
                    tag = "sheet_input_new_catalog_name"
                )
            } else {
                Text(
                    text = "Eklenecek Kataloğu Seçin",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 170.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    allCatalogs.forEach { cat ->
                        val isSelected = cat.id == selectedExistingCatalogId
                        Surface(
                            onClick = { selectedExistingCatalogId = cat.id },
                            shape = ButtonRoundedXl,
                            color = if (isSelected) PrimaryBlueSubtle else SurfaceWhite,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) PrimaryBlue else BorderLight
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sheet_select_existing_cat_${cat.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) PrimaryBlue else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${cat.productCount} ürün",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderLight)

            // Farklı Kataloglardan Ürün Seçme Başlığı ve Aç/Kapat Butonu
            Surface(
                onClick = { showCrossPicker = !showCrossPicker },
                shape = ButtonRoundedXl,
                color = PrimaryBlueSubtle.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_toggle_cross_catalog_picker")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CollectionsBookmark,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = TrStrings.CatalogDetail.CROSS_CATALOG_PICKER_TITLE,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                text = "${pickedProductIds.size} ürün seçili ($distinctSelectedCatalogCount farklı katalogdan)",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlue
                            )
                        }
                    }
                    KYBadge(
                        text = if (showCrossPicker) "Gizle" else "Ürün Seç",
                        variant = BadgeVariant.PRIMARY
                    )
                }
            }

            if (showCrossPicker) {
                // Katalog Filtre Çipleri
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isAll = filterCatalogId == "ALL"
                    Surface(
                        onClick = { filterCatalogId = "ALL" },
                        shape = CircleShape,
                        color = if (isAll) PrimaryBlue else SurfaceMuted,
                        modifier = Modifier.testTag("cross_picker_cat_all")
                    ) {
                        Text(
                            text = "Tüm Kataloglar (${allProducts.size})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isAll) SurfaceWhite else TextPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    allCatalogs.forEach { cat ->
                        val isCatSelected = filterCatalogId == cat.id
                        val countInCat = allProducts.count { it.catalogId == cat.id }
                        Surface(
                            onClick = { filterCatalogId = cat.id },
                            shape = CircleShape,
                            color = if (isCatSelected) PrimaryBlue else SurfaceMuted,
                            modifier = Modifier.testTag("cross_picker_cat_${cat.id}")
                        ) {
                            Text(
                                text = "${cat.title} ($countInCat)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = if (isCatSelected) SurfaceWhite else TextPrimary,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Çoklu Katalog Ürün Seçim Listesi
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .testTag("cross_catalog_products_list"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredPickerProducts, key = { it.id }) { prod ->
                        val isPicked = prod.id in pickedProductIds
                        val sourceCatTitle = allCatalogs.find { it.id == prod.catalogId }?.title ?: "Katalog"
                        Surface(
                            onClick = {
                                if (isPicked) {
                                    pickedProductIds.remove(prod.id)
                                } else {
                                    pickedProductIds.add(prod.id)
                                }
                            },
                            shape = ButtonRoundedXl,
                            color = if (isPicked) PrimaryBlueSubtle else SurfaceWhite,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isPicked) PrimaryBlue else BorderLight
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cross_picker_prod_${prod.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPicked) {
                                        Icons.Filled.CheckCircle
                                    } else {
                                        Icons.Outlined.RadioButtonUnchecked
                                    },
                                    contentDescription = null,
                                    tint = if (isPicked) PrimaryBlue else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                AsyncImage(
                                    model = prod.imageUrl,
                                    contentDescription = prod.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(ButtonRoundedXl)
                                        .background(SurfaceMuted)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = sourceCatTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${prod.price.toInt()} ${prod.currency}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PrimaryBlue
                                )
                            }
                        }
                    }
                }
            }

            KYButton(
                text = if (createNewMode) {
                    "${TrStrings.CatalogDetail.ACTION_CREATE_NEW_CATALOG} (${pickedProductIds.size} Ürün)"
                } else {
                    "Seçili Kataloğa Kopyala (${pickedProductIds.size} Ürün)"
                },
                onClick = {
                    if (pickedProductIds.isEmpty()) return@KYButton
                    if (createNewMode) {
                        if (newCatalogTitle.isBlank()) {
                            titleError = "Lütfen yeni katalog adını girin"
                        } else {
                            onCreateNewCatalogWithProducts(
                                newCatalogTitle.trim(),
                                pickedProductIds.toSet()
                            )
                        }
                    } else if (selectedExistingCatalogId.isNotBlank()) {
                        onAddProductsToExistingCatalog(
                            selectedExistingCatalogId,
                            pickedProductIds.toSet()
                        )
                    }
                },
                variant = ButtonVariant.PRIMARY,
                enabled = pickedProductIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                tag = "btn_confirm_add_to_catalog_sheet"
            )
        }
    }
}
