package com.example.ui.screens.publiccatalog

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.i18n.TrStrings
import com.example.model.CustomerCartItem
import com.example.model.Order
import com.example.model.OrderItem
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.StockStatus
import com.example.services.LocalStorageDataService
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYButton
import com.example.ui.components.KYEmptyState
import com.example.ui.components.KYInput
import com.example.ui.components.KYToastData
import com.example.ui.components.KYToastType
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentAmberLight
import com.example.ui.theme.BackgroundSlate
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.CardRounded2xl
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import androidx.compose.material.icons.outlined.AddToHomeScreen
import com.example.ui.components.NotFound404Screen
import com.example.util.InputSanitizer
import com.example.util.PwaOfflineAndShortcutHelper
import com.example.util.ShareExportGenerator
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

enum class PublicPriceFilter(val labelTr: String) {
    ALL(TrStrings.PublicCatalog.PRICE_FILTER_ALL),
    UNDER_500(TrStrings.PublicCatalog.PRICE_FILTER_UNDER_500),
    BETWEEN_500_1000(TrStrings.PublicCatalog.PRICE_FILTER_500_1000),
    OVER_1000(TrStrings.PublicCatalog.PRICE_FILTER_OVER_1000)
}

/**
 * MÜŞTERİNİN GÖRDÜĞÜ HERKESE AÇIK KATALOG SAYFASI (/c/:shortId):
 * - Giriş gerekmez, alt sekme çubuğu yoktur.
 * - ÜST: İşletme logosu, adı, katalog başlığı, "WhatsApp ile iletişim" butonu.
 * - İÇERİK: 2 sütun ürün grid'i, kategori çipleri, arama, fiyat filtresi, "Sepete ekle" butonu.
 *   Tükenmiş ürünler gri ve "Tükendi" rozetli.
 * - ÜRÜN DETAY: Tam ekran modal (yakınlaştırılabilir görsel galerisi, varyant ve adet seçimi).
 * - SEPET & SİPARİŞ FORMU: Sağ altta yüzen sepet butonu, sipariş kaydı (Durum: Yeni), wa.me yönlendirmesi ve başarı ekranı.
 * - ANALİTİK: sessionId ile "visit", "productView" ve "addToCart" kayıtları.
 * - GÖRSEL KORUMASI: Sağ tık/uzun basma ile görsel kaydetmeyi ve ekran görüntüsü almayı (FLAG_SECURE) devre dışı bırakma.
 */
@Composable
fun PublicCatalogScreen(
    shortId: String,
    onBackClick: (() -> Unit)? = null,
    onShowToast: (KYToastData) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataService = remember { LocalStorageDataService.getInstance(context) }
    val scope = rememberCoroutineScope()

    val allCatalogs by dataService.catalogs.collectAsState()
    val allProducts by dataService.products.collectAsState()
    val resellers by dataService.resellers.collectAsState()
    val store by dataService.settings.collectAsState()

    // Aynı oturumu tekrar saymamak için benzersiz sessionId
    val sessionId = rememberSaveable { "sess-${UUID.randomUUID().toString().take(10)}" }

    val cleanShortId = remember(shortId) {
        InputSanitizer.sanitizeSlugOrShortId(
            shortId.removePrefix("/c/").removePrefix("c/").trim()
        )
    }

    // shortId bir bayiye (reseller) aitse aktif bayiyi bul
    val activeReseller = remember(resellers, cleanShortId) {
        resellers.find { reseller ->
            reseller.shortCode.equals(cleanShortId, ignoreCase = true) ||
                reseller.id.equals(cleanShortId, ignoreCase = true)
        }
    }

    // GÜVENLİK: Müşteri sayfasında YALNIZCA yayında olan (isActive == true) kataloglar görünsün!
    val resellerVisibleCatalogs = remember(allCatalogs, activeReseller) {
        if (activeReseller == null) {
            emptyList()
        } else if (activeReseller.visibleCatalogIds.isEmpty()) {
            allCatalogs.filter { it.isActive }
        } else {
            allCatalogs.filter { it.isActive && it.id in activeReseller.visibleCatalogIds }
        }
    }

    var selectedResellerCatalogId by remember(activeReseller) {
        mutableStateOf("ALL")
    }

    // shortId ile eşleşen kataloğu bul (yayında olup olmadığını kontrol etmek için önce ham eşleşmeyi bul)
    val matchedRawCatalog = remember(allCatalogs, cleanShortId) {
        if (cleanShortId.isBlank() || cleanShortId.equals("demo", ignoreCase = true)) {
            allCatalogs.firstOrNull { it.isActive } ?: allCatalogs.firstOrNull()
        } else {
            allCatalogs.find { cat ->
                val generatedCode = ShareExportGenerator.generateShortPath(cat.id, "c").removePrefix("/c/")
                generatedCode.equals(cleanShortId, ignoreCase = true) ||
                    cat.id.equals(cleanShortId, ignoreCase = true) ||
                    cat.slug.equals(cleanShortId, ignoreCase = true)
            }
        }
    }

    // GÜVENLİK KONTROLÜ 1: Katalog mevcut ancak yayında değilse (isActive == false) müşteri erişimini engelle
    if (activeReseller == null && matchedRawCatalog != null && !matchedRawCatalog.isActive) {
        NotFound404Screen(
            requestedPath = "/c/$cleanShortId",
            title = "Bu Katalog Yayında Değil",
            description = "'${matchedRawCatalog.title}' kataloğu mağaza sahibi tarafından yayından kaldırılmış veya gizlenmiştir. Müşteri sayfasında yalnızca yayında olan kataloglar görüntülenebilir.",
            isUnpublishedCatalog = true,
            onBackClick = onBackClick,
            onGoHomeClick = { onBackClick?.invoke() }
        )
        return
    }

    // GÜVENLİK & 404 KONTROLÜ 2: Eşleşen katalog veya aktif bayi kataloğu bulunamadıysa 404 göster
    if (activeReseller == null && matchedRawCatalog == null) {
        NotFound404Screen(
            requestedPath = "/c/$cleanShortId",
            title = "Katalog Bulunamadı (404)",
            description = "Aradığınız bağlantı (/c/$cleanShortId) ile eşleşen yayında bir katalog bulunamadı.",
            isUnpublishedCatalog = false,
            onBackClick = onBackClick,
            onGoHomeClick = { onBackClick?.invoke() }
        )
        return
    }

    if (activeReseller != null && resellerVisibleCatalogs.isEmpty()) {
        NotFound404Screen(
            requestedPath = "/c/$cleanShortId",
            title = "Yayında Bayi Kataloğu Yok",
            description = "${activeReseller.name} bayisi için şu anda yayında olan aktif bir katalog bulunmuyor.",
            isUnpublishedCatalog = true,
            onBackClick = onBackClick,
            onGoHomeClick = { onBackClick?.invoke() }
        )
        return
    }

    val catalog = remember(matchedRawCatalog, activeReseller, resellerVisibleCatalogs, selectedResellerCatalogId) {
        if (activeReseller != null) {
            if (selectedResellerCatalogId != "ALL") {
                resellerVisibleCatalogs.find { it.id == selectedResellerCatalogId }
                    ?: resellerVisibleCatalogs.firstOrNull()
            } else {
                resellerVisibleCatalogs.firstOrNull()
            }
        } else {
            matchedRawCatalog?.takeIf { it.isActive }
        }
    }

    // Bayi özel linkindeyse bayiye görünür katalogların ürünlerini al ve bayi fiyat/marj kuralını uygula
    val catalogProducts = remember(allProducts, catalog, activeReseller, resellerVisibleCatalogs, selectedResellerCatalogId) {
        val baseList = if (activeReseller != null) {
            val allowedCatIds = if (selectedResellerCatalogId == "ALL") {
                resellerVisibleCatalogs.map { it.id }.toSet()
            } else {
                setOf(selectedResellerCatalogId)
            }
            allProducts
                .filter { it.catalogId in allowedCatIds }
                .sortedBy { it.sortOrder }
        } else if (catalog == null) {
            emptyList()
        } else {
            allProducts.filter { it.catalogId == catalog.id }.sortedBy { it.sortOrder }
        }

        if (activeReseller != null) {
            baseList.map { prod ->
                prod.copy(
                    price = activeReseller.calculateResellerPrice(prod.price),
                    originalPrice = prod.originalPrice?.let { activeReseller.calculateResellerPrice(it) }
                )
            }
        } else {
            baseList
        }
    }

    // Görsel kaydetme ve ekran görüntüsü alma koruması (opsiyonel ayar)
    var protectImages by remember(store.protectImagesFromSaving) {
        mutableStateOf(store.protectImagesFromSaving)
    }

    // Ekran görüntüsü almayı zorlaştırmak için opsiyonel FLAG_SECURE
    DisposableEffect(protectImages) {
        val window = (context as? Activity)?.window
        if (protectImages) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // ANALİTİK: Sayfa açıldığında sessionId ile "visit" kaydı oluştur
    LaunchedEffect(sessionId, catalog?.id) {
        val targetCat = catalog ?: return@LaunchedEffect
        dataService.recordCatalogVisit(
            sessionId = sessionId,
            catalogId = targetCat.id,
            shortId = shortId
        )
    }

    // Filtre & Arama State'leri
    var selectedCategory by remember { mutableStateOf(TrStrings.PublicCatalog.CATEGORY_ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPriceFilter by remember { mutableStateOf(PublicPriceFilter.ALL) }

    // Tam Ekran Ürün Detay Modalı State'i
    var activeDetailProduct by remember { mutableStateOf<Product?>(null) }

    // Sepet ve Sipariş Akışı State'leri
    val cartItems = remember { mutableStateListOf<CustomerCartItem>() }
    var showCartSheet by remember { mutableStateOf(false) }
    var showOrderFormSheet by remember { mutableStateOf(false) }
    var completedOrder by remember { mutableStateOf<Order?>(null) }
    var completedWhatsAppMessage by remember { mutableStateOf("") }

    BackHandler(enabled = onBackClick != null) {
        onBackClick?.invoke()
    }

    val categories by remember(catalogProducts) {
        derivedStateOf {
            val subs = catalogProducts.map { it.subCategory }.filter { it.isNotBlank() }.distinct()
            listOf(TrStrings.PublicCatalog.CATEGORY_ALL) + subs
        }
    }

    val filteredProducts by remember(
        catalogProducts,
        selectedCategory,
        searchQuery,
        selectedPriceFilter
    ) {
        derivedStateOf {
            catalogProducts.filter { prod ->
                val matchesCat = selectedCategory == TrStrings.PublicCatalog.CATEGORY_ALL ||
                    prod.subCategory.equals(selectedCategory, ignoreCase = true)
                val matchesSearch = searchQuery.isBlank() ||
                    prod.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    prod.sku.contains(searchQuery.trim(), ignoreCase = true)
                val matchesPrice = when (selectedPriceFilter) {
                    PublicPriceFilter.ALL -> true
                    PublicPriceFilter.UNDER_500 -> prod.price <= 500.0
                    PublicPriceFilter.BETWEEN_500_1000 -> prod.price > 500.0 && prod.price <= 1000.0
                    PublicPriceFilter.OVER_1000 -> prod.price > 1000.0
                }
                matchesCat && matchesSearch && matchesPrice
            }
        }
    }

    // PERFORMANS: Uzun ürün listeleri için sayfalama (Pagination / Sanal Liste Penceresi)
    val pageSize = 8
    var currentPage by remember(selectedCategory, searchQuery, selectedPriceFilter, selectedResellerCatalogId) {
        mutableStateOf(1)
    }
    val paginatedProducts by remember(filteredProducts, currentPage) {
        derivedStateOf {
            filteredProducts.take(currentPage * pageSize)
        }
    }
    val hasMoreProducts = filteredProducts.size > paginatedProducts.size

    fun addItemToCart(
        product: Product,
        selectedVariants: Map<String, String>,
        unitPrice: Double,
        quantity: Int
    ) {
        val key = "${product.id}_${selectedVariants.entries.sortedBy { it.key }.joinToString("|") { "${it.key}:${it.value}" }}"
        val existingIdx = cartItems.indexOfFirst { it.key == key }
        if (existingIdx >= 0) {
            val existing = cartItems[existingIdx]
            cartItems[existingIdx] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            cartItems.add(
                CustomerCartItem(
                    key = key,
                    product = product,
                    selectedVariants = selectedVariants,
                    unitPrice = unitPrice,
                    quantity = quantity
                )
            )
        }

        // ANALİTİK: Sepete eklemelerde "addToCart" kaydı oluştur
        scope.launch {
            dataService.recordAddToCart(
                sessionId = sessionId,
                catalogId = catalog?.id.orEmpty(),
                shortId = shortId,
                productId = product.id,
                productName = product.name,
                quantity = quantity
            )
        }

        onShowToast(
            KYToastData(
                message = "${product.name} ($quantity adet) ${TrStrings.PublicCatalog.TOAST_ADDED_TO_CART}",
                type = KYToastType.SUCCESS
            )
        )
    }

    fun notifyImageSaveBlocked() {
        onShowToast(
            KYToastData(
                message = TrStrings.PublicCatalog.TOAST_IMAGE_SAVE_BLOCKED,
                type = KYToastType.INFO
            )
        )
    }

    val totalCartItemsCount = cartItems.sumOf { it.quantity }
    val totalCartPrice = cartItems.sumOf { it.unitPrice * it.quantity }
    val currencySymbol = catalogProducts.firstOrNull()?.currency ?: store.currencySymbol.ifBlank { "₺" }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundSlate)
            .testTag("screen_public_catalog")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1) ÜST: İşletme Logosu, Adı, Katalog Başlığı ve "WhatsApp ile iletişim" Butonu
            item {
                PublicStoreHeader(
                    businessName = store.businessName,
                    logoUri = store.logoUri,
                    catalogTitle = if (activeReseller != null) {
                        "${activeReseller.name} • Özel Bayi Kataloğu"
                    } else {
                        catalog?.title ?: store.businessName
                    },
                    catalogDescription = if (activeReseller != null) {
                        "${activeReseller.formatPriceRuleSummary(currencySymbol)} uygulandı • ${resellerVisibleCatalogs.size} katalog erişime açık"
                    } else {
                        catalog?.description.orEmpty()
                    },
                    protectImages = protectImages,
                    onToggleProtectImages = { protectImages = !protectImages },
                    onWhatsAppContactClick = {
                        CustomerWhatsAppOrderHelper.openWhatsAppToSeller(
                            context = context,
                            sellerWhatsappPhone = store.phoneWhatsapp,
                            message = "Merhaba ${store.businessName}, '${catalog?.title ?: "Katalog"}' kataloğunuz hakkında bilgi almak istiyorum."
                        )
                    },
                    onAddToHomeScreenClick = {
                        val pinned = PwaOfflineAndShortcutHelper.requestAddToHomeScreen(
                            context = context,
                            title = catalog?.title ?: store.businessName,
                            shortId = cleanShortId
                        )
                        onShowToast(
                            KYToastData(
                                message = if (pinned) {
                                    "'${catalog?.title ?: store.businessName}' ana ekrana ekleniyor..."
                                } else {
                                    "Katalog kısayolu kaydedildi: /c/$cleanShortId"
                                },
                                type = KYToastType.SUCCESS
                            )
                        )
                    },
                    onBackClick = onBackClick
                )
            }

            // 1.5) BAYİ ÖZEL PORTAL BANNERI VE GÖRÜNÜR KATALOG SEÇİCİ
            if (activeReseller != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = ButtonRoundedXl,
                            color = AccentAmberLight,
                            border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("public_reseller_banner")
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
                                        imageVector = Icons.Outlined.Storefront,
                                        contentDescription = null,
                                        tint = AccentAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Bayi: ${activeReseller.name}",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${activeReseller.formatPriceRuleSummary(currencySymbol)} otomatik uygulandı",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                KYBadge(
                                    text = "Bayi Fiyatlı",
                                    variant = BadgeVariant.ACCENT
                                )
                            }
                        }

                        if (resellerVisibleCatalogs.size > 1) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("public_reseller_catalog_chips")
                            ) {
                                item {
                                    val isAll = selectedResellerCatalogId == "ALL"
                                    Surface(
                                        onClick = { selectedResellerCatalogId = "ALL" },
                                        shape = BadgeRoundedPill,
                                        color = if (isAll) PrimaryBlue else SurfaceWhite,
                                        border = BorderStroke(1.dp, if (isAll) PrimaryBlue else BorderLight)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.MenuBook,
                                                contentDescription = null,
                                                tint = if (isAll) SurfaceWhite else PrimaryBlue,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Tüm Görünür Kataloglar (${resellerVisibleCatalogs.size})",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = if (isAll) SurfaceWhite else TextPrimary
                                            )
                                        }
                                    }
                                }
                                items(resellerVisibleCatalogs, key = { it.id }) { visCat ->
                                    val isSelected = selectedResellerCatalogId == visCat.id
                                    Surface(
                                        onClick = { selectedResellerCatalogId = visCat.id },
                                        shape = BadgeRoundedPill,
                                        color = if (isSelected) PrimaryBlue else SurfaceWhite,
                                        border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderLight)
                                    ) {
                                        Text(
                                            text = visCat.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSelected) SurfaceWhite else TextSecondary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2) ARAMA, KATEGORİ ÇİPLERİ VE FİYAT FİLTRESİ
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Arama Kutusu
                    KYInput(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = TrStrings.PublicCatalog.SEARCH_PLACEHOLDER,
                        leadingIcon = Icons.Outlined.Search,
                        tag = "public_input_search"
                    )

                    // Kategori Çipleri
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("public_category_chips")
                    ) {
                        items(categories, key = { it }) { catName ->
                            val isSelected = selectedCategory.equals(catName, ignoreCase = true)
                            Surface(
                                onClick = { selectedCategory = catName },
                                shape = BadgeRoundedPill,
                                color = if (isSelected) PrimaryBlue else SurfaceWhite,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) PrimaryBlue else BorderLight
                                )
                            ) {
                                Text(
                                    text = catName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) SurfaceWhite else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Fiyat Filtresi Çipleri
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("public_price_filter_chips")
                    ) {
                        items(PublicPriceFilter.entries, key = { it.name }) { priceFilter ->
                            val isSelected = selectedPriceFilter == priceFilter
                            Surface(
                                onClick = { selectedPriceFilter = priceFilter },
                                shape = ButtonRoundedXl,
                                color = if (isSelected) PrimaryBlueSubtle else SurfaceWhite,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) PrimaryBlue else BorderLight
                                )
                            ) {
                                Text(
                                    text = priceFilter.labelTr,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) PrimaryBlue else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3) ÜRÜN GRİD'İ (2 SÜTUN)
            if (filteredProducts.isEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        KYEmptyState(
                            icon = Icons.Outlined.Inventory2,
                            title = "Ürün bulunamadı",
                            description = "Seçtiğiniz kategori veya fiyat aralığında ürün bulunmuyor."
                        )
                    }
                }
            } else {
                val rows = paginatedProducts.chunked(2)
                itemsIndexed(
                    items = rows,
                    key = { _, row -> row.joinToString("-") { it.id } }
                ) { _, rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { product ->
                            PublicCustomerProductCard(
                                product = product,
                                protectImages = protectImages,
                                onCardClick = {
                                    activeDetailProduct = product
                                    // ANALİTİK: Ürün görüntülemelerinde "productView" kaydı oluştur
                                    scope.launch {
                                        dataService.recordProductView(
                                            sessionId = sessionId,
                                            catalogId = catalog?.id.orEmpty(),
                                            shortId = shortId,
                                            productId = product.id,
                                            productName = product.name
                                        )
                                    }
                                },
                                onLongPressBlocked = { notifyImageSaveBlocked() },
                                onAddToCartClick = {
                                    val defaultVars = buildMap {
                                        product.variants.forEach { g ->
                                            val first = g.optionDetails.firstOrNull()?.label ?: g.options.firstOrNull()
                                            if (!first.isNullOrBlank()) put(g.name, first)
                                        }
                                    }
                                    addItemToCart(
                                        product = product,
                                        selectedVariants = defaultVars,
                                        unitPrice = product.price,
                                        quantity = 1
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // SAYFALAMA (PAGINATION): Daha Fazla Ürün Göster Butonu
                if (hasMoreProducts) {
                    item(key = "public_catalog_load_more") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            KYButton(
                                text = "Daha Fazla Ürün Göster (${filteredProducts.size - paginatedProducts.size} ürün daha)",
                                onClick = { currentPage += 1 },
                                variant = ButtonVariant.SECONDARY,
                                modifier = Modifier.fillMaxWidth(),
                                tag = "btn_public_load_more_products"
                            )
                        }
                    }
                }
            }
        }

        // 4) SAĞ ALTTA YÜZEN SEPET BUTONU (Adet Rozetli)
        Surface(
            onClick = { showCartSheet = true },
            shape = CardRounded2xl,
            color = PrimaryBlue,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("fab_public_cart")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingBag,
                        contentDescription = TrStrings.PublicCatalog.CART_TITLE,
                        tint = SurfaceWhite,
                        modifier = Modifier.size(24.dp)
                    )
                    if (totalCartItemsCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = AccentAmber,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = totalCartItemsCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                Column {
                    Text(
                        text = if (totalCartItemsCount > 0) {
                            "$totalCartItemsCount Ürün Sepette"
                        } else {
                            "Sepetim (0)"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = SurfaceWhite
                    )
                    if (totalCartItemsCount > 0) {
                        Text(
                            text = "${totalCartPrice.roundToInt()} $currencySymbol",
                            style = MaterialTheme.typography.labelSmall,
                            color = SurfaceWhite.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }

    // --- TAM EKRAN ÜRÜN DETAY MODALI ---
    PublicProductZoomModal(
        product = activeDetailProduct,
        protectImages = protectImages,
        onImageSaveBlocked = { notifyImageSaveBlocked() },
        onDismiss = { activeDetailProduct = null },
        onAddToCart = { prod, selectedVars, unitPrice, qty ->
            addItemToCart(prod, selectedVars, unitPrice, qty)
        }
    )

    // --- SEPET ALT ÇEKMECESİ ---
    PublicCartBottomSheet(
        isVisible = showCartSheet,
        cartItems = cartItems.toList(),
        currencySymbol = currencySymbol,
        onUpdateQuantity = { key, newQty ->
            val idx = cartItems.indexOfFirst { it.key == key }
            if (idx >= 0) {
                if (newQty <= 0) {
                    cartItems.removeAt(idx)
                } else {
                    cartItems[idx] = cartItems[idx].copy(quantity = newQty)
                }
            }
        },
        onProceedToOrderForm = {
            showCartSheet = false
            showOrderFormSheet = true
        },
        onDismiss = { showCartSheet = false }
    )

    // --- SİPARİŞ FORMU ALT ÇEKMECESİ ---
    PublicOrderFormBottomSheet(
        isVisible = showOrderFormSheet,
        cartItems = cartItems.toList(),
        currencySymbol = currencySymbol,
        onDismiss = { showOrderFormSheet = false },
        onSubmitOrder = { fullName, phone, address, note ->
            val orderItems = cartItems.map { item ->
                OrderItem(
                    productId = item.product.id,
                    productName = item.product.name,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    selectedVariantSummary = item.variantSummary,
                    imageUrl = item.product.imageUrl
                )
            }
            val safeCustomerName = InputSanitizer.sanitizeForStorage(fullName, maxLength = 120)
            val safeCustomerPhone = InputSanitizer.sanitizePhone(phone)
            val safeCustomerAddress = InputSanitizer.sanitizeForStorage(address, maxLength = 400)
            val safeCustomerNote = InputSanitizer.sanitizeForStorage(note, maxLength = 500)

            val newOrder = Order(
                id = "ord-${UUID.randomUUID().toString().take(8)}",
                catalogId = catalog?.id.orEmpty(),
                catalogTitle = catalog?.title ?: store.businessName,
                customerName = safeCustomerName,
                customerPhone = safeCustomerPhone,
                customerAddress = safeCustomerAddress,
                customerNote = safeCustomerNote,
                items = orderItems,
                totalAmount = totalCartPrice,
                currency = currencySymbol,
                status = OrderStatus.NEW,
                resellerId = activeReseller?.id.orEmpty(),
                resellerName = activeReseller?.name.orEmpty()
            )
            val waMessage = CustomerWhatsAppOrderHelper.buildSellerOrderWhatsAppMessage(
                storeName = store.businessName,
                catalogTitle = catalog?.title ?: store.businessName,
                order = newOrder
            )

            scope.launch {
                // 1) Siparişi kaydet (durum: Yeni)
                dataService.createCustomerOrder(newOrder)
                showOrderFormSheet = false
                cartItems.clear()

                // 2) Başarı ekranını göster ve WhatsApp mesajını wa.me ile aç
                completedWhatsAppMessage = waMessage
                completedOrder = newOrder
                CustomerWhatsAppOrderHelper.openWhatsAppToSeller(
                    context = context,
                    sellerWhatsappPhone = store.phoneWhatsapp,
                    message = waMessage
                )
            }
        }
    )

    // --- SİPARİŞ BAŞARI MODALI ---
    PublicOrderSuccessModal(
        order = completedOrder,
        whatsappMessage = completedWhatsAppMessage,
        onReopenWhatsApp = {
            CustomerWhatsAppOrderHelper.openWhatsAppToSeller(
                context = context,
                sellerWhatsappPhone = store.phoneWhatsapp,
                message = completedWhatsAppMessage
            )
        },
        onCloseToCatalog = { completedOrder = null }
    )
}

@Composable
private fun PublicStoreHeader(
    businessName: String,
    logoUri: String,
    catalogTitle: String,
    catalogDescription: String,
    protectImages: Boolean,
    onToggleProtectImages: () -> Unit,
    onWhatsAppContactClick: () -> Unit,
    onAddToHomeScreenClick: () -> Unit,
    onBackClick: (() -> Unit)?
) {
    val initials = remember(businessName) {
        businessName.trim().split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "KY" }
    }

    Surface(
        color = SurfaceWhite,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("public_catalog_header")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("public_catalog_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = TrStrings.Navigation.BACK,
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // İşletme Logosu
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlueSubtle)
                            .border(1.5.dp, PrimaryBlue.copy(alpha = 0.35f), CircleShape),
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
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = businessName,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = PrimaryBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = catalogTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Görsel Kaydetme Koruması Durum Rozeti
                Surface(
                    onClick = onToggleProtectImages,
                    shape = BadgeRoundedPill,
                    color = if (protectImages) PrimaryBlueSubtle else SurfaceMuted,
                    border = BorderStroke(1.dp, if (protectImages) PrimaryBlue.copy(alpha = 0.3f) else BorderLight),
                    modifier = Modifier.testTag("btn_toggle_image_protection")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (protectImages) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                            contentDescription = null,
                            tint = if (protectImages) PrimaryBlue else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (protectImages) "Korumalı" else "Koruma Kapalı",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (protectImages) PrimaryBlue else TextSecondary
                        )
                    }
                }
            }

            if (catalogDescription.isNotBlank()) {
                Text(
                    text = catalogDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // "WhatsApp ile iletişim" ve "Ana ekrana ekle (PWA)" Butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onWhatsAppContactClick,
                    shape = ButtonRoundedXl,
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_public_whatsapp_contact")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Send,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = TrStrings.PublicCatalog.WHATSAPP_CONTACT_BTN,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = SuccessGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    onClick = onAddToHomeScreenClick,
                    shape = ButtonRoundedXl,
                    color = PrimaryBlueSubtle,
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.35f)),
                    modifier = Modifier.testTag("btn_public_add_to_home_screen")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddToHomeScreen,
                            contentDescription = "Ana ekrana ekle",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Ana Ekrana Ekle",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryBlue,
                            maxLines = 1
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderLight)
        }
    }
}

/**
 * MÜŞTERİ ÜRÜN KARTI (2 Sütun Grid):
 * - Tükenmiş ürünler gri (grayscale + alpha) ve "Tükendi" rozetli
 * - Kartta "Sepete ekle" butonu
 * - Görsel kaydetme koruması aktifken uzun basmayı yakalar ve engeller
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PublicCustomerProductCard(
    product: Product,
    protectImages: Boolean,
    onCardClick: () -> Unit,
    onLongPressBlocked: () -> Unit,
    onAddToCartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = product.stockStatus == StockStatus.OUT_OF_STOCK ||
        !product.inStock ||
        (product.trackStock && product.stockCount <= 0)

    val grayscaleFilter = remember(isOutOfStock) {
        if (isOutOfStock) {
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        } else null
    }

    Surface(
        shape = CardRounded2xl,
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderLight),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isOutOfStock) 0.72f else 1f)
            .clip(CardRounded2xl)
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = {
                    if (protectImages) {
                        onLongPressBlocked()
                    }
                }
            )
            .testTag("public_product_card_${product.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(SurfaceMuted)
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    colorFilter = grayscaleFilter,
                    modifier = Modifier.fillMaxSize()
                )

                // Tükenmişse "Tükendi" rozeti, indirimliyse "% İndirim" rozeti
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    if (isOutOfStock) {
                        KYBadge(
                            text = TrStrings.PublicCatalog.BADGE_OUT_OF_STOCK,
                            variant = BadgeVariant.NEUTRAL
                        )
                    } else {
                        val orig = product.originalPrice
                        if (orig != null && orig > product.price) {
                            val pct = (((orig - product.price) / orig) * 100).roundToInt()
                            if (pct > 0) {
                                KYBadge(
                                    text = "%$pct",
                                    variant = BadgeVariant.ACCENT
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isOutOfStock) TextSecondary else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${product.price.roundToInt()} ${product.currency}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isOutOfStock) TextSecondary else PrimaryBlue
                    )
                    val orig = product.originalPrice
                    if (orig != null && orig > product.price) {
                        Text(
                            text = "${orig.roundToInt()} ${product.currency}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                textDecoration = TextDecoration.LineThrough
                            ),
                            color = TextMuted
                        )
                    }
                }

                // "Sepete ekle" veya "Tükendi" Butonu
                Surface(
                    onClick = { if (!isOutOfStock) onAddToCartClick() },
                    enabled = !isOutOfStock,
                    shape = ButtonRoundedXl,
                    color = if (isOutOfStock) SurfaceMuted else PrimaryBlue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_add_to_cart_${product.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isOutOfStock) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = SurfaceWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (isOutOfStock) {
                                TrStrings.PublicCatalog.BADGE_OUT_OF_STOCK
                            } else {
                                TrStrings.PublicCatalog.BTN_ADD_TO_CART
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isOutOfStock) TextSecondary else SurfaceWhite
                        )
                    }
                }
            }
        }
    }
}
