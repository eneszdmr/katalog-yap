package com.example.ui.screens.catalogs

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.auth.LocalAuthContext
import com.example.i18n.TrStrings
import com.example.model.BusinessType
import com.example.model.CurrencyOption
import com.example.model.Product
import com.example.model.ProductVariantGroup
import com.example.model.StockStatus
import com.example.services.DescriptionTone
import com.example.services.GeminiProductService
import com.example.services.LocalStorageDataService
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYButton
import com.example.ui.components.KYCard
import com.example.ui.components.KYInput
import com.example.ui.components.KYToastData
import com.example.ui.components.KYToastType
import com.example.ui.components.KYTopBar
import com.example.ui.theme.AccentAmberLight
import com.example.ui.theme.BackgroundSlate
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmberDark
import com.example.util.ImageCompressionUtil
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ÜRÜN EKLEME / DÜZENLEME EKRANI (/products/new ve /products/:id/edit):
 * - Çoklu görsel yükleme (max 10, sürükle/sırala, ilki kapak), kamera & galeri, max 1280px sıkıştırma
 * - Gemini AI ile doldur ("✨ Yapay zeka ile doldur"): ürün adı, Türkçe 2-3 cümle açıklama, kategori ve etiketler
 * - Ürün adı (zorunlu), açıklama (çok satırlı)
 * - Fiyat (zorunlu), indirimli fiyat (opsiyonel), para birimi
 * - SKU/ürün kodu, kategori ve alt kategori (var olanlardan seç veya yeni oluştur)
 * - Stok yönetimi: stok takibi açma/kapama anahtarı, adet, "Tükendi" durumu
 * - Varyantlar: Beden, Renk vb. özellik grupları, her varyant için ek fiyat ve stok
 * - Gizli not (müşteriye görünmez)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductFormScreen(
    productId: String? = null,
    initialCatalogId: String? = null,
    onBackClick: () -> Unit,
    onSavedSuccess: (catalogId: String) -> Unit,
    onShowToast: (KYToastData) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    val dataService = remember { LocalStorageDataService.getInstance(context) }
    val scope = rememberCoroutineScope()
    val authContext = LocalAuthContext.current
    val store = authContext.storeSettings

    val allProducts by dataService.products.collectAsState()
    val allCatalogs by dataService.catalogs.collectAsState()

    val editingProduct = remember(allProducts, productId) {
        productId?.let { id -> allProducts.find { it.id == id } }
    }
    val isEditMode = editingProduct != null

    val resolvedCatalogId = editingProduct?.catalogId
        ?: initialCatalogId?.takeIf { it.isNotBlank() }
        ?: allCatalogs.firstOrNull()?.id
        ?: "cat-ilkbahar-yaz"

    // --- FORM STATE ---
    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    var isCompressingImage by remember { mutableStateOf(false) }
    var isAiLoading by remember { mutableStateOf(false) }
    var aiAutoFillError by remember { mutableStateOf<String?>(null) }

    // Açıklama Yazarı (Gemini AI - Kurumsal, Samimi, Kısa)
    var selectedDescriptionTone by remember { mutableStateOf(DescriptionTone.CORPORATE) }
    var isImprovingDescription by remember { mutableStateOf(false) }
    var descriptionAiError by remember { mutableStateOf<String?>(null) }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Normal Satış Fiyatı ve Opsiyonel İndirimli Fiyat
    var regularPriceInput by remember { mutableStateOf("") }
    var discountedPriceInput by remember { mutableStateOf("") }
    var selectedCurrencySymbol by remember {
        mutableStateOf(store.currencySymbol.ifBlank { CurrencyOption.TRY.symbol })
    }

    var sku by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(BusinessType.CLOTHING.labelTr) }
    var subCategory by remember { mutableStateOf("Üst Giyim") }
    var showNewCategoryField by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }
    var showNewSubCategoryField by remember { mutableStateOf(false) }
    var newSubCategoryInput by remember { mutableStateOf("") }

    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var tagInput by remember { mutableStateOf("") }

    // Stok Yönetimi
    var trackStock by remember { mutableStateOf(true) }
    var stockCountInput by remember { mutableStateOf("25") }
    var isOutOfStock by remember { mutableStateOf(false) }

    // Varyantlar & Gizli Not
    var variantGroups by remember { mutableStateOf<List<ProductVariantGroup>>(emptyList()) }
    var secretNote by remember { mutableStateOf("") }

    // Doğrulama Hataları
    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    // Düzenleme modundaysa mevcut ürünü forma yükle
    var initialized by remember(productId) { mutableStateOf(false) }
    LaunchedEffect(editingProduct) {
        if (!initialized && editingProduct != null) {
            val prod = editingProduct
            images = if (prod.galleryUrls.isNotEmpty()) {
                prod.galleryUrls.take(10)
            } else if (prod.imageUrl.isNotBlank()) {
                listOf(prod.imageUrl)
            } else {
                emptyList()
            }
            name = prod.name
            description = prod.description

            // Eğer originalPrice varsa, originalPrice normal fiyattır ve price indirimli fiyattır
            if (prod.originalPrice != null && prod.originalPrice > prod.price) {
                regularPriceInput = prod.originalPrice.toInt().toString()
                discountedPriceInput = prod.price.toInt().toString()
            } else {
                regularPriceInput = prod.price.toInt().toString()
                discountedPriceInput = ""
            }
            selectedCurrencySymbol = prod.currency.ifBlank { "₺" }
            sku = prod.sku
            category = prod.category.ifBlank { BusinessType.CLOTHING.labelTr }
            subCategory = prod.subCategory.ifBlank { "Genel" }
            tags = prod.tags
            trackStock = prod.trackStock
            stockCountInput = prod.stockCount.toString()
            isOutOfStock = prod.stockStatus == StockStatus.OUT_OF_STOCK || !prod.inStock
            variantGroups = prod.variants
            secretNote = prod.secretNote
            initialized = true
        } else if (!initialized && productId == null) {
            sku = "KY-${(100..999).random()}"
            initialized = true
        }
    }

    // Mevcut kategoriler ve alt kategoriler
    val availableCategories = remember(allProducts, category) {
        val base = BusinessType.entries.map { it.labelTr }
        val fromProds = allProducts.map { it.category }.filter { it.isNotBlank() }
        (base + fromProds + listOf(category)).filter { it.isNotBlank() }.distinct()
    }

    val availableSubCategories = remember(allProducts, subCategory) {
        val defaults = listOf("Üst Giyim", "Alt Giyim", "Elbise", "Dış Giyim", "Aksesuar", "Dekoratif Obje", "Genel")
        val fromProds = allProducts.map { it.subCategory }.filter { it.isNotBlank() }
        (defaults + fromProds + listOf(subCategory)).filter { it.isNotBlank() }.distinct()
    }

    // Galeri Seçici (Çoklu Görsel, İstemci Tarafında Max 1280px Sıkıştırma)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val remainingSlots = (10 - images.size).coerceAtLeast(0)
            val selectedUris = uris.take(remainingSlots)
            scope.launch {
                isCompressingImage = true
                val compressedUrls = selectedUris.mapNotNull { uri ->
                    ImageCompressionUtil.compressUriToMax1280(context, uri) ?: uri.toString()
                }
                images = (images + compressedUrls).take(10)
                isCompressingImage = false
            }
        }
    }

    // Kamera Çekimi (İstemci Tarafında Max 1280px Sıkıştırma)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null && images.size < 10) {
            scope.launch {
                isCompressingImage = true
                val savedUri = ImageCompressionUtil.compressBitmapToMax1280(context, bitmap)
                if (savedUri != null) {
                    images = (images + savedUri).take(10)
                }
                isCompressingImage = false
            }
        }
    }

    // Gemini AI ile Otomatik Doldurma İşlevi
    fun triggerGeminiAutoFill() {
        val coverImage = images.firstOrNull() ?: return
        scope.launch {
            isAiLoading = true
            aiAutoFillError = null
            val result = GeminiProductService.generateProductDetailsFromImage(context, coverImage)
            isAiLoading = false

            result.onSuccess { suggestion ->
                name = suggestion.productName
                nameError = null
                description = suggestion.shortDescription
                if (suggestion.category.isNotBlank()) {
                    category = suggestion.category
                }
                if (suggestion.subCategory.isNotBlank()) {
                    subCategory = suggestion.subCategory
                }
                if (suggestion.tags.isNotEmpty()) {
                    tags = (tags + suggestion.tags).distinct()
                }
                onShowToast(
                    KYToastData(
                        message = TrStrings.ProductForm.AI_SUCCESS_TOAST,
                        type = KYToastType.SUCCESS
                    )
                )
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Bilinmeyen hata"
                aiAutoFillError = msg
                onShowToast(
                    KYToastData(
                        message = "${TrStrings.ProductForm.AI_ERROR_TOAST}$msg",
                        type = KYToastType.ERROR
                    )
                )
            }
        }
    }

    // Gemini AI ile Açıklamayı İyileştirme İşlevi (Kurumsal, Samimi, Kısa)
    fun triggerImproveDescription() {
        if (isImprovingDescription) return
        scope.launch {
            isImprovingDescription = true
            descriptionAiError = null
            val result = GeminiProductService.improveProductDescription(
                productName = name.ifBlank { editingProduct?.name.orEmpty() },
                currentDescription = description,
                category = category,
                tone = selectedDescriptionTone
            )
            isImprovingDescription = false

            result.onSuccess { improved ->
                description = improved
                onShowToast(
                    KYToastData(
                        message = "Açıklama '${selectedDescriptionTone.labelTr}' tonunda iyileştirildi!",
                        type = KYToastType.SUCCESS
                    )
                )
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Açıklama iyileştirilemedi."
                descriptionAiError = msg
                onShowToast(
                    KYToastData(
                        message = msg,
                        type = KYToastType.ERROR
                    )
                )
            }
        }
    }

    // Kaydetme ve Form Doğrulama İşlevi
    fun validateAndSaveProduct() {
        val cleanName = name.trim()
        val parsedRegularPrice = regularPriceInput.replace(",", ".").toDoubleOrNull()
        val parsedDiscountPrice = discountedPriceInput.replace(",", ".").toDoubleOrNull()

        var hasError = false
        if (cleanName.isBlank()) {
            nameError = TrStrings.ProductForm.ERROR_NAME_REQUIRED
            hasError = true
        } else {
            nameError = null
        }

        // Price is optional, default to 0.0 if not provided
        val rawPrice = parsedRegularPrice ?: 0.0
        val rawDiscountPrice = parsedDiscountPrice ?: 0.0

        val effectivePrice: Double
        val effectiveOriginalPrice: Double?

        if (rawDiscountPrice > 0.0 && rawDiscountPrice < rawPrice) {
            effectivePrice = rawDiscountPrice
            effectiveOriginalPrice = rawPrice
        } else {
            effectivePrice = rawPrice
            effectiveOriginalPrice = null
        }

        val parsedStockCount = if (isOutOfStock) {
            0
        } else if (trackStock) {
            stockCountInput.toIntOrNull()?.coerceAtLeast(0) ?: 25
        } else {
            999
        }

        val resolvedStockStatus = when {
            isOutOfStock || (trackStock && parsedStockCount == 0) -> StockStatus.OUT_OF_STOCK
            trackStock && parsedStockCount in 1..5 -> StockStatus.LOW_STOCK
            else -> StockStatus.IN_STOCK
        }

        val finalImages = if (images.isNotEmpty()) {
            images
        } else {
            val seed = cleanName.lowercase().replace(" ", "-")
            listOf("https://picsum.photos/seed/$seed/800/800")
        }

        val productToSave = Product(
            id = editingProduct?.id ?: "prod-${UUID.randomUUID().toString().take(8)}",
            catalogId = resolvedCatalogId,
            name = cleanName,
            description = description.trim().ifBlank { "$cleanName özel koleksiyon ürünü." },
            price = effectivePrice,
            originalPrice = effectiveOriginalPrice,
            currency = selectedCurrencySymbol,
            imageUrl = finalImages.first(),
            galleryUrls = finalImages,
            category = category.trim().ifBlank { "Giyim" },
            subCategory = subCategory.trim().ifBlank { "Genel" },
            tags = tags,
            trackStock = trackStock,
            stockStatus = resolvedStockStatus,
            stockCount = parsedStockCount,
            inStock = resolvedStockStatus != StockStatus.OUT_OF_STOCK,
            sku = sku.trim().ifBlank { "KY-${(100..999).random()}" },
            variants = variantGroups,
            secretNote = secretNote.trim(),
            sortOrder = editingProduct?.sortOrder ?: 0,
            createdAt = editingProduct?.createdAt ?: System.currentTimeMillis()
        )

        scope.launch {
            dataService.saveProduct(productToSave)
            onShowToast(
                KYToastData(
                    message = TrStrings.ProductForm.TOAST_SAVED_SUCCESS,
                    type = KYToastType.SUCCESS
                )
            )
            onSavedSuccess(resolvedCatalogId)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_product_form"),
        containerColor = BackgroundSlate,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            KYTopBar(
                title = if (isEditMode) TrStrings.ProductForm.TITLE_EDIT else TrStrings.ProductForm.TITLE_NEW,
                onBackClick = onBackClick,
                rightAction = {
                    IconButton(
                        onClick = { validateAndSaveProduct() },
                        modifier = Modifier.testTag("btn_top_save_product")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = TrStrings.Common.SAVE,
                            tint = PrimaryBlue
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = SurfaceWhite,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    HorizontalDivider(color = BorderLight)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KYButton(
                            text = TrStrings.Common.CANCEL,
                            onClick = onBackClick,
                            variant = ButtonVariant.SECONDARY,
                            modifier = Modifier.weight(0.4f),
                            tag = "btn_product_form_cancel"
                        )
                        KYButton(
                            text = TrStrings.ProductForm.BTN_SAVE_PRODUCT,
                            onClick = { validateAndSaveProduct() },
                            variant = ButtonVariant.PRIMARY,
                            leadingIcon = Icons.Outlined.Check,
                            modifier = Modifier.weight(0.6f),
                            tag = "btn_product_form_save"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1) ÇOKLU GÖRSEL YÜKLEME + GEMINI AI BUTONU
            item {
                ProductImagesUploaderSection(
                    images = images,
                    isCompressing = isCompressingImage,
                    isAiLoading = isAiLoading,
                    onPickGallery = {
                        if (images.size >= 10) {
                            onShowToast(
                                KYToastData(
                                    message = TrStrings.ProductForm.MAX_IMAGES_WARNING,
                                    type = KYToastType.INFO
                                )
                            )
                        } else {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    },
                    onOpenCamera = {
                        if (images.size >= 10) {
                            onShowToast(
                                KYToastData(
                                    message = TrStrings.ProductForm.MAX_IMAGES_WARNING,
                                    type = KYToastType.INFO
                                )
                            )
                        } else {
                            cameraLauncher.launch(null)
                        }
                    },
                    onAddDemoImage = {
                        if (images.size < 10) {
                            val demoUrl = "https://picsum.photos/seed/katalog-prod-${System.currentTimeMillis() % 1000}/800/800"
                            images = (images + demoUrl).take(10)
                        }
                    },
                    onMoveImage = { fromIdx, toIdx ->
                        val mutable = images.toMutableList()
                        val item = mutable.removeAt(fromIdx)
                        mutable.add(toIdx, item)
                        images = mutable
                    },
                    onMakeCover = { idx ->
                        if (idx in images.indices && idx > 0) {
                            val mutable = images.toMutableList()
                            val chosen = mutable.removeAt(idx)
                            mutable.add(0, chosen)
                            images = mutable
                        }
                    },
                    onRemoveImage = { idx ->
                        images = images.filterIndexed { i, _ -> i != idx }
                    },
                    onAiFillClick = { triggerGeminiAutoFill() }
                )
            }

            // Yükleme sırasında Skeleton Loader gösterimi
            if (isAiLoading) {
                item {
                    AiFormSkeletonCard()
                }
            }

            // Görselden AI otomatik doldurma hatası ve Tekrar Dene butonu
            if (aiAutoFillError != null) {
                item {
                    Surface(
                        shape = ButtonRoundedXl,
                        color = DangerRedLight,
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_autofill_error_banner")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
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
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = aiAutoFillError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = DangerRed,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            KYButton(
                                text = "Tekrar Dene",
                                onClick = { triggerGeminiAutoFill() },
                                variant = ButtonVariant.SECONDARY,
                                leadingIcon = Icons.Outlined.Refresh,
                                modifier = Modifier.fillMaxWidth(),
                                tag = "btn_retry_ai_autofill"
                            )
                        }
                    }
                }
            }

            // 2) TEMEL BİLGİLER: Ürün Adı (Zorunlu), Açıklama (Çok Satırlı) ve "Açıklamayı İyileştir" (Kurumsal, Samimi, Kısa)
            item {
                KYCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = TrStrings.ProductForm.SECTION_BASIC,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    KYInput(
                        value = name,
                        onValueChange = {
                            name = it
                            if (nameError != null && it.isNotBlank()) nameError = null
                        },
                        label = TrStrings.ProductForm.LABEL_NAME,
                        placeholder = TrStrings.ProductForm.PLACEHOLDER_NAME,
                        errorText = nameError,
                        tag = "input_product_form_name"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    KYInput(
                        value = description,
                        onValueChange = { description = it },
                        label = TrStrings.ProductForm.LABEL_DESCRIPTION,
                        placeholder = TrStrings.ProductForm.PLACEHOLDER_DESCRIPTION,
                        singleLine = false,
                        maxLines = 5,
                        tag = "input_product_form_description"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ✨ AÇIKLAMA YAZARI ("Açıklamayı iyileştir" + Ton Seçenekleri: Kurumsal, Samimi, Kısa)
                    Surface(
                        shape = ButtonRoundedXl,
                        color = PrimaryBlueSubtle.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_description_improver_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AI Açıklama Yazarı",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = PrimaryBlue
                                    )
                                }
                                Text(
                                    text = "Ton seçin",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }

                            // Ton Seçenekleri: Kurumsal, Samimi, Kısa
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DescriptionTone.entries.forEach { tone ->
                                    val isSelected = selectedDescriptionTone == tone
                                    Surface(
                                        onClick = { selectedDescriptionTone = tone },
                                        shape = BadgeRoundedPill,
                                        color = if (isSelected) PrimaryBlue else SurfaceWhite,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) PrimaryBlue else BorderLight
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("tone_chip_${tone.name.lowercase()}")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                                        ) {
                                            Text(
                                                text = tone.labelTr,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (isSelected) SurfaceWhite else TextPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            // Yükleme Durumu
                            if (isImprovingDescription) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .testTag("description_ai_loading"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = PrimaryBlue,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "'${selectedDescriptionTone.labelTr}' tonunda açıklama yazılıyor...",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = PrimaryBlue
                                    )
                                }
                            }

                            // Hata Yönetimi ve Tekrar Dene Butonu
                            if (descriptionAiError != null) {
                                Surface(
                                    shape = ButtonRoundedXl,
                                    color = DangerRedLight,
                                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("description_ai_error_banner")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
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
                                            text = descriptionAiError.orEmpty(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DangerRed,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            onClick = { triggerImproveDescription() },
                                            shape = BadgeRoundedPill,
                                            color = SurfaceWhite,
                                            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                                            modifier = Modifier.testTag("btn_retry_improve_description")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Refresh,
                                                    contentDescription = null,
                                                    tint = DangerRed,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Tekrar Dene",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = DangerRed
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            KYButton(
                                text = if (isImprovingDescription) {
                                    "İyileştiriliyor..."
                                } else {
                                    "✨ Açıklamayı iyileştir (${selectedDescriptionTone.labelTr})"
                                },
                                onClick = { triggerImproveDescription() },
                                variant = ButtonVariant.SECONDARY,
                                leadingIcon = Icons.Outlined.AutoAwesome,
                                enabled = !isImprovingDescription,
                                modifier = Modifier.fillMaxWidth(),
                                tag = "btn_improve_description"
                            )
                        }
                    }
                }
            }

            // 3) FİYATLANDIRMA: Fiyat (Zorunlu), İndirimli Fiyat (Opsiyonel), Para Birimi
            item {
                KYCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = TrStrings.ProductForm.SECTION_PRICING,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KYInput(
                            value = regularPriceInput,
                            onValueChange = {
                                regularPriceInput = it
                                if (priceError != null && it.isNotBlank()) priceError = null
                            },
                            label = "${TrStrings.ProductForm.LABEL_PRICE} ($selectedCurrencySymbol)",
                            placeholder = TrStrings.ProductForm.PLACEHOLDER_PRICE,
                            errorText = priceError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            tag = "input_product_form_price"
                        )

                        KYInput(
                            value = discountedPriceInput,
                            onValueChange = { discountedPriceInput = it },
                            label = TrStrings.ProductForm.LABEL_DISCOUNT_PRICE,
                            placeholder = TrStrings.ProductForm.PLACEHOLDER_DISCOUNT_PRICE,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            tag = "input_product_form_discount_price"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = TrStrings.ProductForm.LABEL_CURRENCY,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyOption.entries.forEach { option ->
                            val isSelected = selectedCurrencySymbol == option.symbol
                            Surface(
                                onClick = { selectedCurrencySymbol = option.symbol },
                                shape = ButtonRoundedXl,
                                color = if (isSelected) PrimaryBlue else SurfaceMuted,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) PrimaryBlue else BorderLight
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("currency_chip_${option.code}")
                            ) {
                                Text(
                                    text = "${option.code} (${option.symbol})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) SurfaceWhite else TextPrimary,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4) SKU, ANA KATEGORİ, ALT KATEGORİ VE ETİKETLER
            item {
                KYCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = TrStrings.ProductForm.SECTION_CATEGORY_SKU,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    KYInput(
                        value = sku,
                        onValueChange = { sku = it },
                        label = TrStrings.ProductForm.LABEL_SKU,
                        placeholder = TrStrings.ProductForm.PLACEHOLDER_SKU,
                        tag = "input_product_form_sku"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Ana Kategori (Var olanlardan seç veya yeni oluştur)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = TrStrings.ProductForm.LABEL_CATEGORY,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Surface(
                            onClick = { showNewCategoryField = !showNewCategoryField },
                            shape = BadgeRoundedPill,
                            color = PrimaryBlueSubtle
                        ) {
                            Text(
                                text = TrStrings.ProductForm.BTN_NEW_CATEGORY,
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableCategories.forEach { catItem ->
                            val isSelected = category.equals(catItem, ignoreCase = true)
                            Surface(
                                onClick = { category = catItem },
                                shape = BadgeRoundedPill,
                                color = if (isSelected) PrimaryBlue else SurfaceMuted,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) PrimaryBlue else BorderLight
                                )
                            ) {
                                Text(
                                    text = catItem,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) SurfaceWhite else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    if (showNewCategoryField) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newCategoryInput,
                                onValueChange = { newCategoryInput = it },
                                placeholder = { Text(TrStrings.ProductForm.PLACEHOLDER_NEW_CATEGORY) },
                                singleLine = true,
                                shape = ButtonRoundedXl,
                                modifier = Modifier.weight(1f)
                            )
                            KYButton(
                                text = "Seç",
                                onClick = {
                                    if (newCategoryInput.isNotBlank()) {
                                        category = newCategoryInput.trim()
                                        newCategoryInput = ""
                                        showNewCategoryField = false
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Alt Kategori (Var olanlardan seç veya yeni oluştur)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = TrStrings.ProductForm.LABEL_SUBCATEGORY,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Surface(
                            onClick = { showNewSubCategoryField = !showNewSubCategoryField },
                            shape = BadgeRoundedPill,
                            color = PrimaryBlueSubtle
                        ) {
                            Text(
                                text = TrStrings.ProductForm.BTN_NEW_CATEGORY,
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableSubCategories.forEach { subItem ->
                            val isSelected = subCategory.equals(subItem, ignoreCase = true)
                            Surface(
                                onClick = { subCategory = subItem },
                                shape = BadgeRoundedPill,
                                color = if (isSelected) PrimaryBlue else SurfaceMuted,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) PrimaryBlue else BorderLight
                                )
                            ) {
                                Text(
                                    text = subItem,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) SurfaceWhite else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    if (showNewSubCategoryField) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newSubCategoryInput,
                                onValueChange = { newSubCategoryInput = it },
                                placeholder = { Text(TrStrings.ProductForm.PLACEHOLDER_NEW_SUBCATEGORY) },
                                singleLine = true,
                                shape = ButtonRoundedXl,
                                modifier = Modifier.weight(1f)
                            )
                            KYButton(
                                text = "Seç",
                                onClick = {
                                    if (newSubCategoryInput.isNotBlank()) {
                                        subCategory = newSubCategoryInput.trim()
                                        newSubCategoryInput = ""
                                        showNewSubCategoryField = false
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Etiketler (Gemini AI önerileri veya kullanıcı girişi)
                    Text(
                        text = TrStrings.ProductForm.LABEL_TAGS,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (tags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.forEach { tag ->
                                Surface(
                                    shape = BadgeRoundedPill,
                                    color = PrimaryBlueSubtle,
                                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = PrimaryBlue
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { tags = tags - tag },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = "Etiketi Kaldır",
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tagInput,
                            onValueChange = { tagInput = it },
                            placeholder = { Text(TrStrings.ProductForm.PLACEHOLDER_TAG_INPUT) },
                            singleLine = true,
                            shape = ButtonRoundedXl,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            onClick = {
                                val cleanTag = tagInput.trim().removePrefix("#")
                                if (cleanTag.isNotBlank()) {
                                    tags = (tags + cleanTag).distinct()
                                    tagInput = ""
                                }
                            },
                            shape = ButtonRoundedXl,
                            color = PrimaryBlue,
                            modifier = Modifier.size(48.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val cleanTag = tagInput.trim().removePrefix("#")
                                    if (cleanTag.isNotBlank()) {
                                        tags = (tags + cleanTag).distinct()
                                        tagInput = ""
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Etiket Ekle",
                                    tint = SurfaceWhite
                                )
                            }
                        }
                    }
                }
            }

            // 5) STOK YÖNETİMİ: Stok Takibi Anahtarı, Adet, "Tükendi" Durumu
            item {
                KYCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = TrStrings.ProductForm.SECTION_STOCK,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Stok Takibi Açma / Kapama Anahtarı
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = TrStrings.ProductForm.LABEL_TRACK_STOCK,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = TrStrings.ProductForm.DESC_TRACK_STOCK,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = trackStock,
                            onCheckedChange = { trackStock = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SurfaceWhite,
                                checkedTrackColor = PrimaryBlue
                            ),
                            modifier = Modifier.testTag("switch_track_stock")
                        )
                    }

                    if (trackStock && !isOutOfStock) {
                        Spacer(modifier = Modifier.height(12.dp))
                        KYInput(
                            value = stockCountInput,
                            onValueChange = { stockCountInput = it },
                            label = TrStrings.ProductForm.LABEL_STOCK_COUNT,
                            placeholder = TrStrings.ProductForm.PLACEHOLDER_STOCK_COUNT,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            tag = "input_product_form_stock_count"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderLight)
                    Spacer(modifier = Modifier.height(12.dp))

                    // "Tükendi" Durumu Anahtarı
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = TrStrings.ProductForm.LABEL_OUT_OF_STOCK,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isOutOfStock) DangerRed else TextPrimary
                            )
                            Text(
                                text = TrStrings.ProductForm.DESC_OUT_OF_STOCK,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = isOutOfStock,
                            onCheckedChange = { isOutOfStock = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SurfaceWhite,
                                checkedTrackColor = DangerRed
                            ),
                            modifier = Modifier.testTag("switch_out_of_stock")
                        )
                    }
                }
            }

            // 6) VARYANTLAR (Beden, Renk vb. + Ek Fiyat & Stok)
            item {
                ProductVariantsEditorSection(
                    variantGroups = variantGroups,
                    currencySymbol = selectedCurrencySymbol,
                    onUpdateGroups = { variantGroups = it }
                )
            }

            // 7) GİZLİ NOT (Müşteriye Görünmez)
            item {
                KYCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = WarningAmberDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = TrStrings.ProductForm.SECTION_SECRET_NOTE,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = TrStrings.ProductForm.SECRET_NOTE_SUBTITLE,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    KYInput(
                        value = secretNote,
                        onValueChange = { secretNote = it },
                        placeholder = TrStrings.ProductForm.PLACEHOLDER_SECRET_NOTE,
                        singleLine = false,
                        maxLines = 4,
                        tag = "input_product_form_secret_note"
                    )
                }
            }
        }
    }
}
