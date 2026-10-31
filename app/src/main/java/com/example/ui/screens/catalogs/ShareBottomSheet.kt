package com.example.ui.screens.catalogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.model.Catalog
import com.example.model.Product
import com.example.model.ShareEvent
import com.example.model.ShareOptionType
import com.example.model.ShareSettings
import com.example.model.WatermarkPosition
import com.example.services.LocalStorageDataService
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYBottomSheet
import com.example.ui.components.KYButton
import com.example.ui.components.KYInput
import com.example.ui.components.KYToastData
import com.example.ui.components.KYToastType
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentAmberLight
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.CardRounded2xl
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmberDark
import com.example.util.ShareExportGenerator
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

/**
 * KAPSAMLI PAYLAŞIM ALT ÇEKMECESİ (Katalog veya Ürün "Paylaş" Tıklanınca Açılır):
 *
 * SEÇENEKLER:
 * 1) "Link olarak paylaş": Benzersiz kısa link (/c/abc123), Link önizleme kartı, "Kopyala" ve "WhatsApp'ta gönder" (wa.me).
 * 2) "Broşür (PDF) olarak paylaş": Katalogdaki ürünlerden tek sayfalık veya çok sayfalı PDF üretir. Kapakta logo & katalog adı, ürün görseli + ad + fiyat. İndir ve Paylaş.
 * 3) "Görsel olarak paylaş": Ürün görselini seçilen bilgilerle (başlık, fiyat, SKU, ek not) ve logo filigranıyla (4 köşe + orta, şeffaflık) Canvas üzerinde işler ve paylaşır.
 * 4) "Birden fazla kataloğu tek linkle paylaş": Katalog seçici ile seçilenleri tek bir "koleksiyon" linkine birleştirir.
 *
 * PAYLAŞIM AYARLARI (Aynı Sheet'te):
 * - Fiyatları göster/gizle, şifre koruması, son kullanma tarihi, "Bayi olarak paylaş" (% veya sabit tutar marj).
 * - Her paylaşım olayını DataService üzerinde "shareEvents" olarak kaydeder.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KYShareBottomSheet(
    isVisible: Boolean,
    catalog: Catalog? = null,
    product: Product? = null,
    onDismiss: () -> Unit,
    onShowToast: (KYToastData) -> Unit,
    onOpenPublicCatalog: ((String) -> Unit)? = null
) {
    if (!isVisible || (catalog == null && product == null)) return

    val context = LocalContext.current
    val dataService = remember { LocalStorageDataService.getInstance(context) }
    val scope = rememberCoroutineScope()
    val authContext = LocalAuthContext.current
    val store = authContext.storeSettings

    val allCatalogs by dataService.catalogs.collectAsState()
    val allProducts by dataService.products.collectAsState()

    val targetCatalog = remember(catalog, product, allCatalogs) {
        val lookupId = catalog?.id ?: product?.catalogId
        allCatalogs.find { it.id == lookupId } ?: catalog ?: allCatalogs.firstOrNull()
    }

    var isSyncingToWeb by remember(targetCatalog?.id) { mutableStateOf(false) }
    var isSyncedToWeb by remember(targetCatalog?.id) { mutableStateOf(false) }

    // Paylaşım penceresi açıldığında kataloğun yayında (isActive = true) olduğundan ve
    // MariaDB + Firebase Firestore + Web vitriniyle anında senkronize edildiğinden emin ol
    LaunchedEffect(isVisible, targetCatalog?.id) {
        if (isVisible && targetCatalog != null) {
            isSyncingToWeb = true
            dataService.ensureCatalogPublishedAndSynced(targetCatalog.id)
            isSyncingToWeb = false
            isSyncedToWeb = true
        }
    }

    val catalogProducts = remember(targetCatalog, product, allProducts) {
        when {
            targetCatalog != null && product == null -> allProducts.filter { it.catalogId == targetCatalog.id }.sortedBy { it.sortOrder }
            catalog != null -> allProducts.filter { it.catalogId == catalog.id }.sortedBy { it.sortOrder }
            product != null -> listOf(product)
            else -> emptyList()
        }
    }

    // Aktif Paylaşım Seçeneği (1: Link, 2: PDF Broşür, 3: Görsel Canvas, 4: Çoklu Katalog Koleksiyonu)
    var selectedOption by remember { mutableStateOf(ShareOptionType.LINK) }

    // Paylaşım Ayarları (Fiyat göster/gizle, Şifre, Son kullanma tarihi, Bayi marjı)
    var shareSettings by remember { mutableStateOf(ShareSettings()) }

    // 2) PDF Broşür Durumu
    var singlePagePdf by remember { mutableStateOf(true) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    // 3) Görsel Olarak Paylaş (Canvas) Durumu
    var selectedCanvasProduct by remember(product, catalogProducts) {
        mutableStateOf(product ?: catalogProducts.firstOrNull())
    }
    var showWatermark by remember { mutableStateOf(true) }
    var watermarkPosition by remember { mutableStateOf(WatermarkPosition.TOP_RIGHT) }
    var watermarkOpacity by remember { mutableFloatStateOf(0.85f) }
    var showOverlayTitle by remember { mutableStateOf(true) }
    var showOverlayPrice by remember { mutableStateOf(true) }
    var showOverlaySku by remember { mutableStateOf(true) }
    var extraOverlayNote by remember { mutableStateOf("") }
    var isGeneratingImage by remember { mutableStateOf(false) }

    // 4) Çoklu Katalog Koleksiyonu Durumu
    var selectedCollectionCatalogIds by remember(targetCatalog, allCatalogs) {
        val initial = if (targetCatalog != null) {
            setOf(targetCatalog.id)
        } else {
            allCatalogs.take(2).map { it.id }.toSet()
        }
        mutableStateOf(initial)
    }
    var collectionTitle by remember(store.businessName) {
        mutableStateOf("${store.businessName} Özel Koleksiyonu")
    }

    val targetTitle = product?.name ?: targetCatalog?.title ?: store.businessName
    val targetId = product?.id ?: targetCatalog?.id ?: "default"
    val targetType = if (product != null) "product" else "catalog"
    val currencySymbol = product?.currency ?: store.currencySymbol.ifBlank { "₺" }

    val catalogKeyForWeb = remember(targetCatalog, targetId) {
        targetCatalog?.slug?.ifBlank { targetCatalog.id } ?: targetId
    }
    val shortPath = remember(targetId, product) {
        ShareExportGenerator.generateShortPath(
            seedId = targetId,
            prefix = if (product != null) "p" else "c"
        )
    }
    val fullShortUrl = remember(shortPath, shareSettings, catalogKeyForWeb) {
        ShareExportGenerator.generateFullShortUrl(
            shortPath = shortPath,
            settings = shareSettings,
            catalogSlugOrId = catalogKeyForWeb
        )
    }

    suspend fun ensureTargetCatalogSyncedAndResolveUrl(): String {
        val catId = targetCatalog?.id
        if (!catId.isNullOrBlank()) {
            if (!isSyncedToWeb || isSyncingToWeb) {
                isSyncingToWeb = true
                dataService.ensureCatalogPublishedAndSynced(catId)
                isSyncingToWeb = false
                isSyncedToWeb = true
            }
            val latestCat = dataService.catalogs.value.find { it.id == catId } ?: targetCatalog
            val latestKey = latestCat?.slug?.ifBlank { latestCat.id } ?: catalogKeyForWeb
            return ShareExportGenerator.generateFullShortUrl(
                shortPath = shortPath,
                settings = shareSettings,
                catalogSlugOrId = latestKey
            )
        }
        return fullShortUrl
    }

    fun recordEvent(
        channel: String,
        link: String = fullShortUrl,
        overrideType: String = targetType,
        overrideTitle: String = targetTitle,
        catIds: List<String> = listOfNotNull(targetCatalog?.id)
    ) {
        scope.launch {
            dataService.recordShareEvent(
                ShareEvent(
                    id = "share-${UUID.randomUUID().toString().take(8)}",
                    targetType = overrideType,
                    targetId = targetId,
                    targetTitle = overrideTitle,
                    channel = channel,
                    shortLink = link,
                    showPrices = shareSettings.showPrices,
                    isPasswordProtected = shareSettings.hasPassword && shareSettings.password.isNotBlank(),
                    expiryDate = if (shareSettings.hasExpiryDate) shareSettings.expiryDateText else null,
                    isDealerShare = shareSettings.isDealerShare,
                    dealerMarginType = if (shareSettings.isDealerShare) shareSettings.dealerMarginType.name else null,
                    dealerMarginValue = if (shareSettings.isDealerShare) shareSettings.dealerMarginValue else 0.0,
                    catalogIds = catIds
                )
            )
        }
    }

    KYBottomSheet(
        isVisible = true,
        title = if (product != null) TrStrings.Share.SHEET_TITLE_PRODUCT else TrStrings.Share.SHEET_TITLE_CATALOG,
        subtitle = targetTitle,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .verticalScroll(rememberScrollState())
                .testTag("ky_share_bottom_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 4 SEÇENEK SEKMESİ (Link, PDF Broşür, Görsel Kart, Çoklu Katalog)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ShareModeTabButton(
                    icon = Icons.Outlined.Link,
                    label = TrStrings.Share.TAB_LINK,
                    isSelected = selectedOption == ShareOptionType.LINK,
                    onClick = { selectedOption = ShareOptionType.LINK },
                    modifier = Modifier.weight(1f),
                    tag = "share_tab_link"
                )
                ShareModeTabButton(
                    icon = Icons.Outlined.PictureAsPdf,
                    label = TrStrings.Share.TAB_PDF,
                    isSelected = selectedOption == ShareOptionType.PDF_BROCHURE,
                    onClick = { selectedOption = ShareOptionType.PDF_BROCHURE },
                    modifier = Modifier.weight(1f),
                    tag = "share_tab_pdf"
                )
                ShareModeTabButton(
                    icon = Icons.Outlined.Image,
                    label = TrStrings.Share.TAB_IMAGE,
                    isSelected = selectedOption == ShareOptionType.IMAGE_CARD,
                    onClick = { selectedOption = ShareOptionType.IMAGE_CARD },
                    modifier = Modifier.weight(1f),
                    tag = "share_tab_image"
                )
                ShareModeTabButton(
                    icon = Icons.Outlined.CollectionsBookmark,
                    label = TrStrings.Share.TAB_COLLECTION,
                    isSelected = selectedOption == ShareOptionType.MULTI_CATALOG,
                    onClick = { selectedOption = ShareOptionType.MULTI_CATALOG },
                    modifier = Modifier.weight(1f),
                    tag = "share_tab_multi_catalog"
                )
            }

            // SEÇİLEN PAYLAŞIM MODUNUN İÇERİĞİ
            when (selectedOption) {
                // =========================================================================
                // 1) "Link olarak paylaş": Benzersiz kısa link (/c/abc123), Önizleme Kartı,
                //    "Kopyala" ve "WhatsApp'ta gönder" (wa.me linki ile hazır mesaj)
                // =========================================================================
                ShareOptionType.LINK -> {
                    LinkShareOptionContent(
                        shortPath = shortPath,
                        fullShortUrl = fullShortUrl,
                        title = targetTitle,
                        coverImageUrl = product?.imageUrl
                            ?: targetCatalog?.coverImageUrl?.ifBlank { catalogProducts.firstOrNull()?.imageUrl.orEmpty() }
                            ?: "",
                        storeName = store.businessName,
                        productCount = catalogProducts.size,
                        samplePrice = product?.price ?: catalogProducts.firstOrNull()?.price,
                        currencySymbol = currencySymbol,
                        settings = shareSettings,
                        onCopyClick = {
                            scope.launch {
                                val syncedUrl = ensureTargetCatalogSyncedAndResolveUrl()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText(targetTitle, syncedUrl))
                                recordEvent(channel = "link_copy", link = syncedUrl)
                                onShowToast(
                                    KYToastData(
                                        message = TrStrings.Catalogs.TOAST_LINK_COPIED,
                                        type = KYToastType.SUCCESS
                                    )
                                )
                            }
                        },
                        onWhatsAppClick = {
                            scope.launch {
                                val syncedUrl = ensureTargetCatalogSyncedAndResolveUrl()
                                recordEvent(channel = "whatsapp", link = syncedUrl)
                                ShareExportGenerator.shareToWhatsApp(
                                    context = context,
                                    title = targetTitle,
                                    shortUrl = syncedUrl,
                                    store = store,
                                    settings = shareSettings,
                                    extraSummary = if (product != null) {
                                        if (shareSettings.showPrices) {
                                            val p = shareSettings.calculateAdjustedPrice(product.price).roundToInt()
                                            "Fiyat: $p $currencySymbol • SKU: ${product.sku}"
                                        } else {
                                            "SKU: ${product.sku}"
                                        }
                                    } else {
                                        "${catalogProducts.size} ürünlük dijital katalog"
                                    }
                                )
                            }
                        },
                        onOpenPublicViewClick = if (onOpenPublicCatalog != null) {
                            {
                                scope.launch {
                                    ensureTargetCatalogSyncedAndResolveUrl()
                                    val latestCat = dataService.catalogs.value.find { it.id == targetCatalog?.id } ?: targetCatalog
                                    val shortCode = latestCat?.slug?.takeIf { it.isNotBlank() }
                                        ?: latestCat?.id
                                        ?: shortPath.substringAfterLast("/").ifBlank { "cat-1" }
                                    recordEvent(channel = "public_preview")
                                    onDismiss()
                                    onOpenPublicCatalog(shortCode)
                                }
                            }
                        } else null
                    )
                }

                // =========================================================================
                // 2) "Broşür (PDF) olarak paylaş": Tek sayfalık veya çok sayfalı PDF üret,
                //    Kapakta logo ve katalog adı, her ürün için görsel + ad + fiyat.
                //    "İndir" ve "Paylaş" butonları.
                // =========================================================================
                ShareOptionType.PDF_BROCHURE -> {
                    PdfBrochureShareContent(
                        catalogTitle = targetTitle,
                        storeName = store.businessName,
                        products = catalogProducts,
                        currencySymbol = currencySymbol,
                        settings = shareSettings,
                        singlePageMode = singlePagePdf,
                        isGenerating = isGeneratingPdf,
                        onTogglePageMode = { singlePagePdf = it },
                        onDownloadPdf = {
                            scope.launch {
                                isGeneratingPdf = true
                                val pdfFile = ShareExportGenerator.generateBrochurePdf(
                                    context = context,
                                    catalogTitle = targetTitle,
                                    catalogDescription = targetCatalog?.description.orEmpty(),
                                    products = catalogProducts,
                                    store = store,
                                    settings = shareSettings,
                                    singlePageMode = singlePagePdf
                                )
                                isGeneratingPdf = false
                                if (pdfFile != null) {
                                    recordEvent(channel = "pdf_download")
                                    onShowToast(
                                        KYToastData(
                                            message = "İndirilenler/KatalogYap/${pdfFile.name} konumuna kaydedildi",
                                            type = KYToastType.SUCCESS
                                        )
                                    )
                                    ShareExportGenerator.openPdfFile(
                                        context = context,
                                        file = pdfFile,
                                        chooserTitle = "$targetTitle PDF Broşür"
                                    )
                                }
                            }
                        },
                        onSharePdf = {
                            scope.launch {
                                isGeneratingPdf = true
                                val pdfFile = ShareExportGenerator.generateBrochurePdf(
                                    context = context,
                                    catalogTitle = targetTitle,
                                    catalogDescription = targetCatalog?.description.orEmpty(),
                                    products = catalogProducts,
                                    store = store,
                                    settings = shareSettings,
                                    singlePageMode = singlePagePdf
                                )
                                isGeneratingPdf = false
                                if (pdfFile != null) {
                                    recordEvent(channel = "pdf_share")
                                    ShareExportGenerator.shareFileViaSystemSheet(
                                        context = context,
                                        file = pdfFile,
                                        mimeType = "application/pdf",
                                        chooserTitle = "$targetTitle PDF Broşür",
                                        extraText = "$targetTitle PDF Ürün Broşürü — $fullShortUrl"
                                    )
                                }
                            }
                        }
                    )
                }

                // =========================================================================
                // 3) "Görsel olarak paylaş": Ürün görselini başlık, fiyat, SKU, ek not ve
                //    4 köşe + orta şeffaf logo filigranı ile Canvas üzerinde işle ve paylaş.
                // =========================================================================
                ShareOptionType.IMAGE_CARD -> {
                    val activeProd = selectedCanvasProduct ?: catalogProducts.firstOrNull()
                    val imgTitle = activeProd?.name ?: targetTitle
                    val imgSku = activeProd?.sku ?: "KY-101"
                    val imgUrl = activeProd?.imageUrl ?: targetCatalog?.coverImageUrl.orEmpty()
                    val effectivePriceText = if (!shareSettings.showPrices) {
                        "Fiyat Sorunuz"
                    } else if (activeProd != null) {
                        val adj = shareSettings.calculateAdjustedPrice(activeProd.price).roundToInt()
                        "$adj ${activeProd.currency}"
                    } else {
                        ""
                    }

                    ImageCanvasShareContent(
                        availableProducts = catalogProducts,
                        selectedProduct = activeProd,
                        onSelectProduct = { selectedCanvasProduct = it },
                        imageUrl = imgUrl,
                        title = imgTitle,
                        priceText = effectivePriceText,
                        skuText = imgSku,
                        storeName = store.businessName,
                        showWatermark = showWatermark,
                        onShowWatermarkChange = { showWatermark = it },
                        watermarkPosition = watermarkPosition,
                        onWatermarkPositionChange = { watermarkPosition = it },
                        watermarkOpacity = watermarkOpacity,
                        onWatermarkOpacityChange = { watermarkOpacity = it },
                        showTitle = showOverlayTitle,
                        onShowTitleChange = { showOverlayTitle = it },
                        showPrice = showOverlayPrice,
                        onShowPriceChange = { showOverlayPrice = it },
                        showSku = showOverlaySku,
                        onShowSkuChange = { showOverlaySku = it },
                        extraNote = extraOverlayNote,
                        onExtraNoteChange = { extraOverlayNote = it },
                        isGenerating = isGeneratingImage,
                        onShareImageClick = {
                            scope.launch {
                                isGeneratingImage = true
                                val bitmap = ShareExportGenerator.renderProductShareCardBitmap(
                                    context = context,
                                    title = imgTitle,
                                    priceText = effectivePriceText,
                                    skuText = imgSku,
                                    imageUrl = imgUrl,
                                    storeName = store.businessName,
                                    showTitle = showOverlayTitle,
                                    showPrice = showOverlayPrice && shareSettings.showPrices,
                                    showSku = showOverlaySku,
                                    showWatermark = showWatermark,
                                    watermarkPosition = watermarkPosition,
                                    watermarkOpacity = watermarkOpacity,
                                    extraNote = extraOverlayNote.trim()
                                )
                                val savedFile = ShareExportGenerator.saveBitmapToCacheFile(
                                    context = context,
                                    bitmap = bitmap,
                                    fileNamePrefix = "katalog_gorsel"
                                )
                                isGeneratingImage = false
                                if (savedFile != null) {
                                    recordEvent(channel = "image_share")
                                    onShowToast(
                                        KYToastData(
                                            message = TrStrings.Share.TOAST_IMAGE_SHARED,
                                            type = KYToastType.SUCCESS
                                        )
                                    )
                                    ShareExportGenerator.shareFileViaSystemSheet(
                                        context = context,
                                        file = savedFile,
                                        mimeType = "image/png",
                                        chooserTitle = imgTitle,
                                        extraText = "$imgTitle — $fullShortUrl"
                                    )
                                }
                            }
                        }
                    )
                }

                // =========================================================================
                // 4) "Birden fazla kataloğu tek linkle paylaş": Katalog seçici ile
                //    seçilenleri tek bir "koleksiyon" linkine birleştir.
                // =========================================================================
                ShareOptionType.MULTI_CATALOG -> {
                    val firstSelectedCatalogSlug = allCatalogs
                        .firstOrNull { it.id in selectedCollectionCatalogIds }
                        ?.let { it.slug.ifBlank { it.id } }
                        .orEmpty()
                    val collectionSeed = selectedCollectionCatalogIds.sorted().joinToString("-")
                        .ifBlank { "koleksiyon" }
                    val collectionShortPath = ShareExportGenerator.generateShortPath(
                        seedId = collectionSeed,
                        prefix = "c/klk"
                    )
                    val collectionUrl = ShareExportGenerator.generateFullShortUrl(
                        shortPath = collectionShortPath,
                        settings = shareSettings,
                        catalogSlugOrId = firstSelectedCatalogSlug
                    )

                    MultiCatalogCollectionShareContent(
                        allCatalogs = allCatalogs,
                        selectedCatalogIds = selectedCollectionCatalogIds,
                        collectionTitle = collectionTitle,
                        collectionShortUrl = collectionUrl,
                        onCollectionTitleChange = { collectionTitle = it },
                        onToggleCatalogSelection = { catId ->
                            selectedCollectionCatalogIds = if (catId in selectedCollectionCatalogIds) {
                                selectedCollectionCatalogIds - catId
                            } else {
                                selectedCollectionCatalogIds + catId
                            }
                        },
                        onCopyCollectionLink = {
                            if (selectedCollectionCatalogIds.isEmpty()) {
                                onShowToast(
                                    KYToastData(
                                        message = TrStrings.Share.ERROR_MIN_CATALOGS,
                                        type = KYToastType.ERROR
                                    )
                                )
                            } else {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText(collectionTitle, collectionUrl))
                                recordEvent(
                                    channel = "collection_link",
                                    link = collectionUrl,
                                    overrideType = "collection",
                                    overrideTitle = collectionTitle,
                                    catIds = selectedCollectionCatalogIds.toList()
                                )
                                onShowToast(
                                    KYToastData(
                                        message = TrStrings.Catalogs.TOAST_LINK_COPIED,
                                        type = KYToastType.SUCCESS
                                    )
                                )
                            }
                        },
                        onWhatsAppCollectionShare = {
                            if (selectedCollectionCatalogIds.isEmpty()) {
                                onShowToast(
                                    KYToastData(
                                        message = TrStrings.Share.ERROR_MIN_CATALOGS,
                                        type = KYToastType.ERROR
                                    )
                                )
                            } else {
                                val chosenTitles = allCatalogs
                                    .filter { it.id in selectedCollectionCatalogIds }
                                    .joinToString(", ") { it.title }
                                recordEvent(
                                    channel = "collection_whatsapp",
                                    link = collectionUrl,
                                    overrideType = "collection",
                                    overrideTitle = collectionTitle,
                                    catIds = selectedCollectionCatalogIds.toList()
                                )
                                ShareExportGenerator.shareToWhatsApp(
                                    context = context,
                                    title = collectionTitle,
                                    shortUrl = collectionUrl,
                                    store = store,
                                    settings = shareSettings,
                                    extraSummary = "${selectedCollectionCatalogIds.size} Katalog Bir Arada: $chosenTitles"
                                )
                            }
                        }
                    )
                }
            }

            // =========================================================================
            // PAYLAŞIM AYARLARI (Aynı Sheet İçinde)
            // =========================================================================
            ShareSettingsSection(
                settings = shareSettings,
                currencySymbol = currencySymbol,
                onSettingsChange = { shareSettings = it }
            )
        }
    }
}

@Composable
private fun ShareModeTabButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String
) {
    Surface(
        onClick = onClick,
        shape = ButtonRoundedXl,
        color = if (isSelected) PrimaryBlue else SurfaceMuted,
        border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderLight),
        modifier = modifier.testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) SurfaceWhite else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) SurfaceWhite else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 1) LİNK OLARAK PAYLAŞ BİLEŞENİ
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinkShareOptionContent(
    shortPath: String,
    fullShortUrl: String,
    title: String,
    coverImageUrl: String,
    storeName: String,
    productCount: Int,
    samplePrice: Double?,
    currencySymbol: String,
    settings: ShareSettings,
    onCopyClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onOpenPublicViewClick: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Benzersiz Kısa Link Kutusu (/c/abc123)
        Surface(
            shape = ButtonRoundedXl,
            color = PrimaryBlueSubtle,
            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("short_link_box")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = TrStrings.Share.SHORT_LINK_BADGE,
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryBlue
                        )
                        Text(
                            text = fullShortUrl,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                KYBadge(
                    text = shortPath,
                    variant = BadgeVariant.PRIMARY
                )
            }
        }

        // Link Önizleme Kartı
        Surface(
            shape = CardRounded2xl,
            color = SurfaceWhite,
            border = BorderStroke(1.dp, BorderLight),
            shadowElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("link_preview_card")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = TrStrings.Share.PREVIEW_CARD_LABEL,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = coverImageUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(ButtonRoundedXl)
                            .background(SurfaceMuted)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = storeName,
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryBlue
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val priceInfo = if (!settings.showPrices) {
                            TrStrings.Share.PRICE_HIDDEN_BADGE
                        } else if (samplePrice != null) {
                            val adj = settings.calculateAdjustedPrice(samplePrice).roundToInt()
                            "$adj $currencySymbol"
                        } else {
                            "$productCount ürün"
                        }
                        Text(
                            text = "$productCount ürün  •  $priceInfo",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Aktif Paylaşım Ayarı Rozetleri
                if (!settings.showPrices || settings.isDealerShare || settings.hasPassword || settings.hasExpiryDate) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!settings.showPrices) {
                            KYBadge(text = TrStrings.Share.PRICE_HIDDEN_BADGE, variant = BadgeVariant.NEUTRAL)
                        }
                        if (settings.isDealerShare) {
                            KYBadge(text = TrStrings.Share.DEALER_BADGE, variant = BadgeVariant.ACCENT)
                        }
                        if (settings.hasPassword && settings.password.isNotBlank()) {
                            KYBadge(
                                text = "${TrStrings.Share.PASSWORD_PROTECTED_BADGE}: ${settings.password}",
                                variant = BadgeVariant.PRIMARY
                            )
                        }
                        if (settings.hasExpiryDate && settings.expiryDateText.isNotBlank()) {
                            KYBadge(
                                text = "Süre: ${settings.expiryDateText}",
                                variant = BadgeVariant.NEUTRAL
                            )
                        }
                    }
                }
            }
        }

        // "Kopyala" ve "WhatsApp'ta gönder" Butonları
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KYButton(
                text = TrStrings.Share.BTN_COPY_LINK,
                onClick = onCopyClick,
                variant = ButtonVariant.SECONDARY,
                leadingIcon = Icons.Outlined.ContentCopy,
                modifier = Modifier.weight(1f),
                tag = "btn_share_copy_link"
            )
            KYButton(
                text = TrStrings.Share.BTN_SEND_WHATSAPP,
                onClick = onWhatsAppClick,
                variant = ButtonVariant.PRIMARY,
                leadingIcon = Icons.Outlined.Send,
                modifier = Modifier.weight(1.25f),
                tag = "btn_share_whatsapp"
            )
        }

        // Müşteri Katalog Sayfasını (/c/:shortId) Canlı Önizleme Butonu
        if (onOpenPublicViewClick != null) {
            KYButton(
                text = "${TrStrings.Share.BTN_OPEN_PUBLIC_VIEW} ($shortPath)",
                onClick = onOpenPublicViewClick,
                variant = ButtonVariant.GHOST,
                leadingIcon = Icons.Outlined.Share,
                modifier = Modifier.fillMaxWidth(),
                tag = "btn_open_public_catalog_view"
            )
        }
    }
}

/**
 * 2) BROŞÜR (PDF) OLARAK PAYLAŞ BİLEŞENİ
 */
@Composable
private fun PdfBrochureShareContent(
    catalogTitle: String,
    storeName: String,
    products: List<Product>,
    currencySymbol: String,
    settings: ShareSettings,
    singlePageMode: Boolean,
    isGenerating: Boolean,
    onTogglePageMode: (Boolean) -> Unit,
    onDownloadPdf: () -> Unit,
    onSharePdf: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Tek Sayfa / Çok Sayfalı Seçici
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = { onTogglePageMode(true) },
                shape = ButtonRoundedXl,
                color = if (singlePageMode) PrimaryBlue else SurfaceMuted,
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_pdf_single_page")
            ) {
                Text(
                    text = TrStrings.Share.PDF_SINGLE_PAGE,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (singlePageMode) SurfaceWhite else TextPrimary,
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                )
            }
            Surface(
                onClick = { onTogglePageMode(false) },
                shape = ButtonRoundedXl,
                color = if (!singlePageMode) PrimaryBlue else SurfaceMuted,
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_pdf_multi_page")
            ) {
                Text(
                    text = TrStrings.Share.PDF_MULTI_PAGE,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (!singlePageMode) SurfaceWhite else TextPrimary,
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                )
            }
        }

        // PDF Kapak + Ürünler Görsel Önizleme Kartı
        Surface(
            shape = CardRounded2xl,
            color = SurfaceWhite,
            border = BorderStroke(1.dp, BorderLight),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pdf_brochure_preview")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Kapak Bandı (Logo + Katalog Adı)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(PrimaryBlue, Color(0xFF1D4ED8))
                            )
                        )
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = storeName.take(2).uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = storeName,
                                style = MaterialTheme.typography.labelSmall,
                                color = SurfaceWhite.copy(alpha = 0.85f)
                            )
                            Text(
                                text = catalogTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = SurfaceWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        KYBadge(
                            text = if (singlePageMode) "1 Sayfa PDF" else "${(products.size + 5) / 6} Sayfa PDF",
                            variant = BadgeVariant.ACCENT
                        )
                    }
                }

                // Ürün Satırları Önizlemesi
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    products.take(3).forEach { prod ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = prod.imageUrl,
                                contentDescription = prod.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(ButtonRoundedXl)
                                    .background(SurfaceMuted)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = prod.name,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            val priceStr = if (settings.showPrices) {
                                "${settings.calculateAdjustedPrice(prod.price).roundToInt()} $currencySymbol"
                            } else {
                                "Fiyat Sorunuz"
                            }
                            Text(
                                text = priceStr,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue
                            )
                        }
                    }
                    if (products.size > 3) {
                        Text(
                            text = "+ ${products.size - 3} ürün daha PDF broşürde yer alacak",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        if (isGenerating) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                Text(
                    text = TrStrings.Share.PDF_GENERATING,
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryBlue
                )
            }
        }

        // "İndir" ve "Paylaş" Butonları
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KYButton(
                text = TrStrings.Share.BTN_DOWNLOAD_PDF,
                onClick = onDownloadPdf,
                enabled = !isGenerating,
                variant = ButtonVariant.SECONDARY,
                leadingIcon = Icons.Outlined.Download,
                modifier = Modifier.weight(1f),
                tag = "btn_download_pdf"
            )
            KYButton(
                text = TrStrings.Share.BTN_SHARE_PDF,
                onClick = onSharePdf,
                enabled = !isGenerating,
                variant = ButtonVariant.PRIMARY,
                leadingIcon = Icons.Outlined.Share,
                modifier = Modifier.weight(1f),
                tag = "btn_share_pdf"
            )
        }
    }
}

/**
 * 3) GÖRSEL OLARAK PAYLAŞ (Canvas Filigran, 4 Köşe + Orta, Şeffaflık, Bilgi Alanları, Ek Not)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImageCanvasShareContent(
    availableProducts: List<Product>,
    selectedProduct: Product?,
    onSelectProduct: (Product) -> Unit,
    imageUrl: String,
    title: String,
    priceText: String,
    skuText: String,
    storeName: String,
    showWatermark: Boolean,
    onShowWatermarkChange: (Boolean) -> Unit,
    watermarkPosition: WatermarkPosition,
    onWatermarkPositionChange: (WatermarkPosition) -> Unit,
    watermarkOpacity: Float,
    onWatermarkOpacityChange: (Float) -> Unit,
    showTitle: Boolean,
    onShowTitleChange: (Boolean) -> Unit,
    showPrice: Boolean,
    onShowPriceChange: (Boolean) -> Unit,
    showSku: Boolean,
    onShowSkuChange: (Boolean) -> Unit,
    extraNote: String,
    onExtraNoteChange: (String) -> Unit,
    isGenerating: Boolean,
    onShareImageClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Katalogdan paylaşılıyorsa hangi ürünün görselinin işleneceğini seçme şeridi
        if (availableProducts.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(availableProducts.take(10), key = { it.id }) { prod ->
                    val isSelected = prod.id == selectedProduct?.id
                    Surface(
                        onClick = { onSelectProduct(prod) },
                        shape = ButtonRoundedXl,
                        color = if (isSelected) PrimaryBlueSubtle else SurfaceWhite,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) PrimaryBlue else BorderLight
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = prod.imageUrl,
                                contentDescription = prod.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(ButtonRoundedXl)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = prod.name.take(14),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // CANLI GÖRSEL KARTI ÖNİZLEMESİ (Filigran konumu, şeffaflık, başlık, fiyat, SKU, ek not)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.25f)
                .clip(CardRounded2xl)
                .border(1.dp, BorderLight, CardRounded2xl)
                .background(Color(0xFF1E293B))
                .testTag("canvas_image_live_preview")
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Alt Gradyan
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.78f)
                            )
                        )
                    )
            )

            // Logo Filigranı (4 Köşe + Orta, Şeffaflık)
            if (showWatermark) {
                val wmAlign = when (watermarkPosition) {
                    WatermarkPosition.TOP_LEFT -> Alignment.TopStart
                    WatermarkPosition.TOP_RIGHT -> Alignment.TopEnd
                    WatermarkPosition.BOTTOM_LEFT -> Alignment.BottomStart
                    WatermarkPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                    WatermarkPosition.CENTER -> Alignment.Center
                }
                Surface(
                    shape = BadgeRoundedPill,
                    color = Color(0xFF0F172A).copy(alpha = (watermarkOpacity * 0.85f).coerceIn(0.15f, 0.95f)),
                    modifier = Modifier
                        .align(wmAlign)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = watermarkOpacity)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = storeName.take(1).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = SurfaceWhite.copy(alpha = watermarkOpacity)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = storeName.take(18),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SurfaceWhite.copy(alpha = watermarkOpacity)
                        )
                    }
                }
            }

            // Alt Bilgi Katmanı (Başlık, Fiyat, SKU, Ek Not)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (showTitle) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SurfaceWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showPrice && priceText.isNotBlank()) {
                        Surface(
                            shape = ButtonRoundedXl,
                            color = PrimaryBlue
                        ) {
                            Text(
                                text = priceText,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = SurfaceWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (showSku && skuText.isNotBlank()) {
                        Text(
                            text = "SKU: $skuText",
                            style = MaterialTheme.typography.labelSmall,
                            color = SurfaceWhite.copy(alpha = 0.85f)
                        )
                    }
                }
                if (extraNote.isNotBlank()) {
                    Surface(
                        shape = ButtonRoundedXl,
                        color = AccentAmber
                    ) {
                        Text(
                            text = extraNote,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // GÖRSEL KART AYARLARI (Logo Filigranı, 4 Köşe + Orta, Şeffaflık, Alanlar, Ek Not)
        Surface(
            shape = CardRounded2xl,
            color = SurfaceWhite,
            border = BorderStroke(1.dp, BorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = TrStrings.Share.WATERMARK_SHOW,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Switch(
                        checked = showWatermark,
                        onCheckedChange = onShowWatermarkChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SurfaceWhite,
                            checkedTrackColor = PrimaryBlue
                        ),
                        modifier = Modifier.testTag("switch_canvas_watermark")
                    )
                }

                if (showWatermark) {
                    Text(
                        text = TrStrings.Share.WATERMARK_LABEL,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WatermarkPosition.entries.forEach { pos ->
                            val isSelected = watermarkPosition == pos
                            Surface(
                                onClick = { onWatermarkPositionChange(pos) },
                                shape = BadgeRoundedPill,
                                color = if (isSelected) PrimaryBlue else SurfaceMuted,
                                modifier = Modifier.testTag("wm_pos_${pos.name}")
                            ) {
                                Text(
                                    text = pos.labelTr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) SurfaceWhite else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${TrStrings.Share.OPACITY_LABEL}: %${(watermarkOpacity * 100).roundToInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.width(130.dp)
                        )
                        Slider(
                            value = watermarkOpacity,
                            onValueChange = onWatermarkOpacityChange,
                            valueRange = 0.2f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryBlue,
                                activeTrackColor = PrimaryBlue
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = SurfaceMuted)

                // Bilgi Alanlarını Aç / Kapat (Başlık, Fiyat, SKU)
                Text(
                    text = TrStrings.Share.OVERLAY_FIELDS_LABEL,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OverlayToggleChip(
                        label = TrStrings.Share.FIELD_TITLE,
                        checked = showTitle,
                        onToggle = { onShowTitleChange(!showTitle) },
                        modifier = Modifier.weight(1f)
                    )
                    OverlayToggleChip(
                        label = TrStrings.Share.FIELD_PRICE,
                        checked = showPrice,
                        onToggle = { onShowPriceChange(!showPrice) },
                        modifier = Modifier.weight(1f)
                    )
                    OverlayToggleChip(
                        label = TrStrings.Share.FIELD_SKU,
                        checked = showSku,
                        onToggle = { onShowSkuChange(!showSku) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Ek Not Alanı
                KYInput(
                    value = extraNote,
                    onValueChange = onExtraNoteChange,
                    label = TrStrings.Share.EXTRA_NOTE_LABEL,
                    placeholder = TrStrings.Share.EXTRA_NOTE_PLACEHOLDER,
                    tag = "input_canvas_extra_note"
                )
            }
        }

        KYButton(
            text = if (isGenerating) TrStrings.Share.IMAGE_GENERATING else TrStrings.Share.BTN_SHARE_IMAGE,
            onClick = onShareImageClick,
            enabled = !isGenerating,
            isLoading = isGenerating,
            variant = ButtonVariant.PRIMARY,
            leadingIcon = Icons.Outlined.Share,
            modifier = Modifier.fillMaxWidth(),
            tag = "btn_share_canvas_image"
        )
    }
}

@Composable
private fun OverlayToggleChip(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onToggle,
        shape = ButtonRoundedXl,
        color = if (checked) PrimaryBlueSubtle else SurfaceMuted,
        border = BorderStroke(1.dp, if (checked) PrimaryBlue else BorderLight),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (checked) PrimaryBlue else TextSecondary
            )
        }
    }
}

/**
 * 4) BİRDEN FAZLA KATALOĞU TEK LİNKLE PAYLAŞ (Koleksiyon Linki Seçici)
 */
@Composable
private fun MultiCatalogCollectionShareContent(
    allCatalogs: List<Catalog>,
    selectedCatalogIds: Set<String>,
    collectionTitle: String,
    collectionShortUrl: String,
    onCollectionTitleChange: (String) -> Unit,
    onToggleCatalogSelection: (String) -> Unit,
    onCopyCollectionLink: () -> Unit,
    onWhatsAppCollectionShare: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KYInput(
            value = collectionTitle,
            onValueChange = onCollectionTitleChange,
            label = TrStrings.Share.COLLECTION_NAME_LABEL,
            placeholder = TrStrings.Share.COLLECTION_NAME_PLACEHOLDER,
            tag = "input_collection_share_title"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = TrStrings.Share.MULTI_SUBTITLE,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            KYBadge(
                text = "${selectedCatalogIds.size} ${TrStrings.Share.SELECTED_CATALOGS_SUFFIX}",
                variant = BadgeVariant.PRIMARY
            )
        }

        // Katalog Seçici Listesi
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allCatalogs.forEach { cat ->
                val isSelected = cat.id in selectedCatalogIds
                Surface(
                    onClick = { onToggleCatalogSelection(cat.id) },
                    shape = ButtonRoundedXl,
                    color = if (isSelected) PrimaryBlueSubtle else SurfaceWhite,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) PrimaryBlue else BorderLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("collection_select_catalog_${cat.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleCatalogSelection(cat.id) },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        AsyncImage(
                            model = cat.coverImageUrl,
                            contentDescription = cat.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(ButtonRoundedXl)
                                .background(SurfaceMuted)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cat.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${cat.productCount} ürün",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Birleştirilmiş Koleksiyon Kısa Link Kutusu
        Surface(
            shape = ButtonRoundedXl,
            color = SurfaceMuted,
            border = BorderStroke(1.dp, BorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CollectionsBookmark,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = collectionShortUrl,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KYButton(
                text = TrStrings.Share.BTN_COPY_LINK,
                onClick = onCopyCollectionLink,
                variant = ButtonVariant.SECONDARY,
                leadingIcon = Icons.Outlined.ContentCopy,
                modifier = Modifier.weight(1f),
                tag = "btn_copy_collection_link"
            )
            KYButton(
                text = TrStrings.Share.BTN_SEND_WHATSAPP,
                onClick = onWhatsAppCollectionShare,
                variant = ButtonVariant.PRIMARY,
                leadingIcon = Icons.Outlined.Send,
                modifier = Modifier.weight(1.25f),
                tag = "btn_whatsapp_collection_link"
            )
        }
    }
}
