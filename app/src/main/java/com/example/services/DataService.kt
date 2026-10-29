package com.example.services

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AnalyticsSummary
import com.example.model.AppNotification
import com.example.model.AuthUser
import com.example.model.BusinessType
import com.example.model.Catalog
import com.example.model.CatalogAnalyticsEvent
import com.example.model.CatalogLanguage
import com.example.model.CurrencyOption
import com.example.model.Order
import com.example.model.OrderItem
import com.example.model.OrderStatus
import com.example.model.OrderTimelineEntry
import com.example.model.PhonePasswordCredential
import com.example.model.Product
import com.example.model.ProductVariantGroup
import com.example.model.ProductVariantOption
import com.example.model.Reseller
import com.example.model.ResellerPriceMode
import com.example.model.ShareEvent
import com.example.model.StockStatus
import com.example.model.StoreSettings
import com.example.model.SubscriptionPlan
import com.example.model.TeamMember
import com.example.model.TeamRole
import com.example.model.defaultStoreTeamMembers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Tüm veri erişimini tek bir katmanda toplayan DataService arayüzü.
 * MariaDB veritabanı (`MariaDbStorageEngine`) yapılandırıldığında tüm verileri ve resimleri
 * MariaDB üzerinde saklar; çevrimdışı önbellek olarak yerel depolama ile senkronize çalışır.
 */
interface DataService {
    val authUser: StateFlow<AuthUser?>
    val catalogs: StateFlow<List<Catalog>>
    val products: StateFlow<List<Product>>
    val orders: StateFlow<List<Order>>
    val analytics: StateFlow<AnalyticsSummary>
    val settings: StateFlow<StoreSettings>
    val notifications: StateFlow<List<AppNotification>>
    val shareEvents: StateFlow<List<ShareEvent>>
    val catalogAnalyticsEvents: StateFlow<List<CatalogAnalyticsEvent>>
    val resellers: StateFlow<List<Reseller>>

    suspend fun saveAuthUser(user: AuthUser?)
    suspend fun clearAuthSession()
    fun getPhoneCredential(fullPhone: String): PhonePasswordCredential?
    suspend fun savePhoneCredential(credential: PhonePasswordCredential)
    suspend fun saveCatalog(catalog: Catalog)
    suspend fun deleteCatalog(catalogId: String)
    suspend fun deleteCatalogs(catalogIds: Set<String>)
    suspend fun duplicateCatalog(catalogId: String): Catalog?
    suspend fun toggleCatalogVisibility(catalogId: String)
    suspend fun addTagToCatalogs(catalogIds: Set<String>, tag: String)
    suspend fun applyTagsToCatalogs(catalogIds: Set<String>, tags: List<String>)
    suspend fun mergeCatalogs(catalogIds: Set<String>, mergedTitle: String): Catalog?
    suspend fun createCatalogFromPhotos(
        title: String,
        photoUrls: List<String>,
        tag: String? = null
    ): Catalog
    suspend fun createCatalogFromAiPhotoDrafts(
        title: String,
        drafts: List<AiPhotoProductDraft>,
        tag: String? = null
    ): Catalog
    suspend fun importCatalogFromCsv(
        title: String,
        csvContent: String,
        tag: String? = null
    ): Catalog
    suspend fun seedSampleCatalogs()

    // Ürün & Stok İşlemleri (Katalog Detay, Ürün Detay & Stok Yönetimi /inventory)
    suspend fun saveProduct(product: Product)
    suspend fun deleteProducts(productIds: Set<String>)
    suspend fun updateProductsStockStatus(productIds: Set<String>, status: StockStatus)
    suspend fun updateProductStockCount(productId: String, newStockCount: Int)
    suspend fun bulkAssignProductStock(productIds: Set<String>, stockCount: Int)
    suspend fun markProductsOutOfStock(productIds: Set<String>)
    suspend fun updateLowStockThreshold(threshold: Int)
    suspend fun bulkImportProducts(catalogId: String, importedProducts: List<Product>): Int
    suspend fun moveProductsToCatalog(productIds: Set<String>, targetCatalogId: String)
    suspend fun copyProductsToCatalog(productIds: Set<String>, targetCatalogId: String): Int
    suspend fun copyProductsToNewCatalog(productIds: Set<String>, newCatalogTitle: String): Catalog?
    suspend fun reorderCatalogProducts(catalogId: String, orderedProductIds: List<String>)

    // Bayi (Reseller) İşlemleri
    suspend fun saveReseller(reseller: Reseller)
    suspend fun deleteReseller(resellerId: String)

    // Ekip & Abonelik & Hesap İşlemleri
    suspend fun inviteTeamMember(name: String, emailOrPhone: String, role: TeamRole)
    suspend fun updateTeamMemberRole(memberId: String, role: TeamRole)
    suspend fun removeTeamMember(memberId: String)
    suspend fun upgradeSubscriptionPlan(plan: SubscriptionPlan)
    suspend fun deleteAccount()

    suspend fun createCustomerOrder(order: Order)
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus)
    suspend fun seedSampleOrders()
    suspend fun clearAllOrders()
    suspend fun updateSettings(newSettings: StoreSettings)
    suspend fun markAllNotificationsRead()
    suspend fun recordShareEvent(event: ShareEvent)
    suspend fun recordCatalogVisit(
        sessionId: String,
        catalogId: String,
        shortId: String,
        referrer: String = "whatsapp"
    )
    suspend fun recordProductView(
        sessionId: String,
        catalogId: String,
        shortId: String,
        productId: String,
        productName: String
    )
    suspend fun recordAddToCart(
        sessionId: String,
        catalogId: String,
        shortId: String,
        productId: String,
        productName: String,
        quantity: Int
    )
    suspend fun seedSampleAnalyticsEvents()
    suspend fun resetStorage()
}

class LocalStorageDataService(context: Context) : DataService {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mariaDbEngine = MariaDbStorageEngine(context.applicationContext)
    private val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _authUser = MutableStateFlow<AuthUser?>(null)
    override val authUser: StateFlow<AuthUser?> = _authUser.asStateFlow()

    private val _catalogs = MutableStateFlow<List<Catalog>>(emptyList())
    override val catalogs: StateFlow<List<Catalog>> = _catalogs.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    override val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    override val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _analytics = MutableStateFlow(AnalyticsSummary())
    override val analytics: StateFlow<AnalyticsSummary> = _analytics.asStateFlow()

    private val _settings = MutableStateFlow(StoreSettings())
    override val settings: StateFlow<StoreSettings> = _settings.asStateFlow()

    private val _notifications = MutableStateFlow(defaultNotifications())
    override val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _shareEvents = MutableStateFlow<List<ShareEvent>>(emptyList())
    override val shareEvents: StateFlow<List<ShareEvent>> = _shareEvents.asStateFlow()

    private val _catalogAnalyticsEvents = MutableStateFlow<List<CatalogAnalyticsEvent>>(emptyList())
    override val catalogAnalyticsEvents: StateFlow<List<CatalogAnalyticsEvent>> =
        _catalogAnalyticsEvents.asStateFlow()

    private val _resellers = MutableStateFlow<List<Reseller>>(emptyList())
    override val resellers: StateFlow<List<Reseller>> = _resellers.asStateFlow()

    init {
        loadFromLocalStorage()
        syncWithMariaDbOnStartup()
    }

    private fun syncWithMariaDbOnStartup() {
        if (!mariaDbEngine.isConfigured()) return
        dbScope.launch {
            val remoteSnapshots = mariaDbEngine.fetchAllSnapshots()
            val restoredImages = mariaDbEngine.restoreBinaryImagesToLocalCache()

            if (remoteSnapshots.isNotEmpty()) {
                val editor = prefs.edit()
                remoteSnapshots.forEach { (key, json) ->
                    editor.putString(key, json)
                }
                editor.putBoolean(KEY_INITIAL_SEEDED, true)
                editor.putBoolean(KEY_RESELLERS_SEEDED, true)
                editor.putBoolean(KEY_ORDERS_SEEDED, true)
                editor.putBoolean(KEY_ANALYTICS_SEEDED, true)
                editor.apply()

                _authUser.value = readAuthUser()
                val loadedSettings = readSettings()
                _settings.value = if (restoredImages.containsKey(loadedSettings.logoUri)) {
                    loadedSettings.copy(logoUri = restoredImages.getValue(loadedSettings.logoUri))
                } else {
                    loadedSettings
                }

                val loadedCatalogs = readCatalogs().map { cat ->
                    val mappedCover = restoredImages[cat.coverImageUrl] ?: cat.coverImageUrl
                    cat.copy(coverImageUrl = mappedCover)
                }
                val loadedProducts = readProducts().map { prod ->
                    val mappedMain = restoredImages[prod.imageUrl] ?: prod.imageUrl
                    val mappedGallery = prod.galleryUrls.map { g -> restoredImages[g] ?: g }
                    prod.copy(imageUrl = mappedMain, galleryUrls = mappedGallery)
                }
                _catalogs.value = loadedCatalogs
                _products.value = loadedProducts
                _resellers.value = readResellers()
                _orders.value = readOrders()
                _shareEvents.value = readShareEvents()
                _catalogAnalyticsEvents.value = readCatalogAnalyticsEvents()
                recalculateAnalytics()
            } else {
                // MariaDB henüz boşsa yerel verilerin ve resimlerin tamamını MariaDB'ye aktar
                writeAuthUser(_authUser.value)
                writePhoneCredentials(readPhoneCredentials().values.toList())
                writeSettings(_settings.value)
                writeCatalogs(_catalogs.value)
                writeProducts(_products.value)
                writeResellers(_resellers.value)
                writeOrders(_orders.value)
                writeShareEvents(_shareEvents.value)
                writeCatalogAnalyticsEvents(_catalogAnalyticsEvents.value)
            }
        }
    }

    private fun loadFromLocalStorage() {
        _authUser.value = readAuthUser()
        _settings.value = readSettings()

        val hasSeeded = prefs.getBoolean(KEY_INITIAL_SEEDED, false)
        val loadedCatalogs = readCatalogs()
        val loadedProducts = readProducts()

        if (!hasSeeded && loadedCatalogs.isEmpty()) {
            val (sampleCatalogs, sampleProducts) = buildSampleCatalogsAndProducts()
            _catalogs.value = sampleCatalogs
            _products.value = sampleProducts
            writeCatalogs(sampleCatalogs)
            writeProducts(sampleProducts)
            prefs.edit().putBoolean(KEY_INITIAL_SEEDED, true).apply()
        } else {
            _catalogs.value = loadedCatalogs
            _products.value = loadedProducts
        }

        val loadedResellers = readResellers()
        val hasResellersSeeded = prefs.getBoolean(KEY_RESELLERS_SEEDED, false)
        if (!hasResellersSeeded && loadedResellers.isEmpty()) {
            val sampleResellers = buildSampleResellers(_catalogs.value)
            _resellers.value = sampleResellers
            writeResellers(sampleResellers)
            prefs.edit().putBoolean(KEY_RESELLERS_SEEDED, true).apply()
        } else {
            _resellers.value = loadedResellers
        }

        val loadedOrders = readOrders()
        val hasOrdersSeeded = prefs.getBoolean(KEY_ORDERS_SEEDED, false)
        if (!hasOrdersSeeded && loadedOrders.isEmpty()) {
            val sampleOrders = buildSampleOrders(_products.value, _catalogs.value)
            _orders.value = sampleOrders
            writeOrders(sampleOrders)
            prefs.edit().putBoolean(KEY_ORDERS_SEEDED, true).apply()
        } else {
            _orders.value = loadedOrders
        }

        _shareEvents.value = readShareEvents()

        val loadedAnalyticsEvents = readCatalogAnalyticsEvents()
        val hasAnalyticsSeeded = prefs.getBoolean(KEY_ANALYTICS_SEEDED, false)
        if (!hasAnalyticsSeeded && loadedAnalyticsEvents.isEmpty()) {
            val sampleEvents = buildSampleAnalyticsEvents(_catalogs.value, _products.value)
            _catalogAnalyticsEvents.value = sampleEvents
            writeCatalogAnalyticsEvents(sampleEvents)
            prefs.edit().putBoolean(KEY_ANALYTICS_SEEDED, true).apply()
        } else {
            _catalogAnalyticsEvents.value = loadedAnalyticsEvents
        }

        recalculateAnalytics()
    }

    override suspend fun saveAuthUser(user: AuthUser?) {
        _authUser.value = user
        writeAuthUser(user)
    }

    override suspend fun clearAuthSession() {
        _authUser.value = null
        prefs.edit().remove(KEY_AUTH_USER).apply()
        mariaDbEngine.syncAuthUserAsync(null, null)
    }

    override fun getPhoneCredential(fullPhone: String): PhonePasswordCredential? {
        val normalized = normalizePhoneKey(fullPhone)
        return readPhoneCredentials()[normalized]
    }

    override suspend fun savePhoneCredential(credential: PhonePasswordCredential) {
        val normalized = normalizePhoneKey(credential.fullPhone)
        val map = readPhoneCredentials().toMutableMap()
        map[normalized] = credential.copy(fullPhone = normalized)
        writePhoneCredentials(map.values.toList())
    }

    override suspend fun saveCatalog(catalog: Catalog) {
        val current = _catalogs.value.toMutableList()
        val index = current.indexOfFirst { it.id == catalog.id }
        val resolvedCover = catalog.coverImageUrl.ifBlank {
            _products.value.firstOrNull { it.catalogId == catalog.id }?.imageUrl
                ?: "https://picsum.photos/seed/${catalog.slug.ifBlank { catalog.id }}/600/600"
        }
        val productCount = _products.value.count { it.catalogId == catalog.id }
        val normalized = catalog.copy(
            coverImageUrl = resolvedCover,
            productCount = if (index >= 0) productCount else catalog.productCount
        )

        if (index >= 0) {
            current[index] = normalized
        } else {
            current.add(0, normalized)
        }
        _catalogs.value = current
        writeCatalogs(current)
        recalculateAnalytics()
    }

    override suspend fun deleteCatalog(catalogId: String) {
        deleteCatalogs(setOf(catalogId))
    }

    override suspend fun deleteCatalogs(catalogIds: Set<String>) {
        if (catalogIds.isEmpty()) return
        val updatedCatalogs = _catalogs.value.filterNot { it.id in catalogIds }
        val updatedProducts = _products.value.filterNot { it.catalogId in catalogIds }
        _catalogs.value = updatedCatalogs
        _products.value = updatedProducts
        writeCatalogs(updatedCatalogs)
        writeProducts(updatedProducts)
        recalculateAnalytics()
    }

    override suspend fun duplicateCatalog(catalogId: String): Catalog? {
        val source = _catalogs.value.find { it.id == catalogId } ?: return null
        val newCatalogId = UUID.randomUUID().toString()
        val sourceProducts = _products.value.filter { it.catalogId == catalogId }

        val copiedProducts = sourceProducts.map { prod ->
            prod.copy(
                id = UUID.randomUUID().toString(),
                catalogId = newCatalogId
            )
        }

        val duplicated = source.copy(
            id = newCatalogId,
            title = "${source.title} (Kopya)",
            slug = "${source.slug}-kopya-${(100..999).random()}",
            productCount = copiedProducts.size,
            viewCount = 0,
            orderCount = 0,
            createdAt = System.currentTimeMillis()
        )

        val updatedProducts = copiedProducts + _products.value
        val updatedCatalogs = listOf(duplicated) + _catalogs.value

        _products.value = updatedProducts
        _catalogs.value = updatedCatalogs
        writeProducts(updatedProducts)
        writeCatalogs(updatedCatalogs)
        recalculateAnalytics()
        return duplicated
    }

    override suspend fun toggleCatalogVisibility(catalogId: String) {
        val updated = _catalogs.value.map { cat ->
            if (cat.id == catalogId) cat.copy(isActive = !cat.isActive) else cat
        }
        _catalogs.value = updated
        writeCatalogs(updated)
    }

    override suspend fun addTagToCatalogs(catalogIds: Set<String>, tag: String) {
        val cleanTag = tag.trim()
        if (cleanTag.isBlank() || catalogIds.isEmpty()) return

        val updated = _catalogs.value.map { cat ->
            if (cat.id in catalogIds) {
                val mergedTags = (cat.tags + cleanTag).distinct()
                cat.copy(tags = mergedTags)
            } else {
                cat
            }
        }
        _catalogs.value = updated
        writeCatalogs(updated)
    }

    override suspend fun applyTagsToCatalogs(catalogIds: Set<String>, tags: List<String>) {
        if (catalogIds.isEmpty()) return
        val cleanTags = tags.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (cleanTags.isEmpty()) return

        val isSingleCatalog = catalogIds.size == 1
        val updated = _catalogs.value.map { cat ->
            if (cat.id in catalogIds) {
                val finalTags = if (isSingleCatalog) {
                    cleanTags
                } else {
                    (cat.tags + cleanTags).distinct()
                }
                cat.copy(tags = finalTags)
            } else {
                cat
            }
        }
        _catalogs.value = updated
        writeCatalogs(updated)
    }

    override suspend fun mergeCatalogs(catalogIds: Set<String>, mergedTitle: String): Catalog? {
        val selectedCatalogs = _catalogs.value.filter { it.id in catalogIds }
        if (selectedCatalogs.size < 2) return null

        val newCatalogId = UUID.randomUUID().toString()
        val sourceProducts = _products.value.filter { it.catalogId in catalogIds }

        val clonedProducts = sourceProducts.mapIndexed { idx, product ->
            product.copy(
                id = UUID.randomUUID().toString(),
                catalogId = newCatalogId,
                sortOrder = idx
            )
        }

        val combinedTags = selectedCatalogs.flatMap { it.tags }.distinct()
        val totalViews = selectedCatalogs.sumOf { it.viewCount }
        val totalOrders = selectedCatalogs.sumOf { it.orderCount }
        val firstCover = selectedCatalogs.firstOrNull { it.coverImageUrl.isNotBlank() }?.coverImageUrl
            ?: clonedProducts.firstOrNull()?.imageUrl
            ?: "https://picsum.photos/seed/merged-catalog/600/600"

        val finalTitle = mergedTitle.trim().ifBlank {
            "${selectedCatalogs.first().title} + ${selectedCatalogs.size - 1} Katalog"
        }

        val mergedCatalog = Catalog(
            id = newCatalogId,
            title = finalTitle,
            description = "${selectedCatalogs.size} kataloğun birleştirilmesiyle oluşturuldu.",
            slug = slugify(finalTitle),
            coverImageUrl = firstCover,
            tags = if (combinedTags.isEmpty()) listOf("Yeni Sezon") else combinedTags,
            isActive = true,
            productCount = clonedProducts.size,
            viewCount = totalViews,
            orderCount = totalOrders,
            createdAt = System.currentTimeMillis()
        )

        val remainingCatalogs = _catalogs.value.filterNot { it.id in catalogIds }
        val remainingProducts = _products.value.filterNot { it.catalogId in catalogIds }

        val updatedCatalogs = listOf(mergedCatalog) + remainingCatalogs
        val updatedProducts = clonedProducts + remainingProducts

        _catalogs.value = updatedCatalogs
        _products.value = updatedProducts
        writeCatalogs(updatedCatalogs)
        writeProducts(updatedProducts)
        recalculateAnalytics()
        return mergedCatalog
    }

    override suspend fun createCatalogFromPhotos(
        title: String,
        photoUrls: List<String>,
        tag: String?
    ): Catalog {
        val newCatalogId = UUID.randomUUID().toString()
        val cleanTitle = title.trim().ifBlank { "Fotoğraf Kataloğu" }
        val currencySymbol = _settings.value.currencySymbol.ifBlank { "₺" }

        val effectiveUrls = if (photoUrls.isNotEmpty()) {
            photoUrls
        } else {
            (1..8).map { idx ->
                "https://picsum.photos/seed/photo-cat-${newCatalogId.take(4)}-$idx/600/600"
            }
        }

        val createdProducts = effectiveUrls.mapIndexed { idx, url ->
            Product(
                id = UUID.randomUUID().toString(),
                catalogId = newCatalogId,
                name = "$cleanTitle Ürün #${idx + 1}",
                description = "Fotoğraflardan otomatik oluşturulan ürün kartı",
                price = (250 + idx * 75).toDouble(),
                currency = currencySymbol,
                imageUrl = url,
                galleryUrls = listOf(
                    url,
                    "https://picsum.photos/seed/photo-gal-${idx}-a/600/600",
                    "https://picsum.photos/seed/photo-gal-${idx}-b/600/600"
                ),
                subCategory = if (idx % 2 == 0) "Yeni Gelenler" else "Özel Seçki",
                stockStatus = StockStatus.IN_STOCK,
                stockCount = 20,
                inStock = true,
                sku = "FOTO-${100 + idx}",
                variants = defaultClothingVariants(),
                sortOrder = idx
            )
        }

        val newCatalog = Catalog(
            id = newCatalogId,
            title = cleanTitle,
            description = "${createdProducts.size} ürün fotoğrafından otomatik oluşturuldu.",
            slug = slugify(cleanTitle),
            coverImageUrl = createdProducts.first().imageUrl,
            tags = listOfNotNull(tag?.trim()?.takeIf { it.isNotBlank() }, "Yeni Sezon").distinct(),
            isActive = true,
            productCount = createdProducts.size,
            viewCount = 1,
            orderCount = 0,
            createdAt = System.currentTimeMillis()
        )

        val updatedProducts = createdProducts + _products.value
        val updatedCatalogs = listOf(newCatalog) + _catalogs.value

        _products.value = updatedProducts
        _catalogs.value = updatedCatalogs
        writeProducts(updatedProducts)
        writeCatalogs(updatedCatalogs)
        recalculateAnalytics()
        return newCatalog
    }

    override suspend fun createCatalogFromAiPhotoDrafts(
        title: String,
        drafts: List<AiPhotoProductDraft>,
        tag: String?
    ): Catalog {
        if (drafts.isEmpty()) {
            return createCatalogFromPhotos(title, emptyList(), tag)
        }
        val newCatalogId = UUID.randomUUID().toString()
        val cleanTitle = title.trim().ifBlank { "AI Fotoğraf Kataloğu" }
        val currencySymbol = _settings.value.currencySymbol.ifBlank { "₺" }

        val createdProducts = drafts.mapIndexed { idx, draft ->
            Product(
                id = UUID.randomUUID().toString(),
                catalogId = newCatalogId,
                name = draft.productName.trim().ifBlank { "$cleanTitle Ürün #${idx + 1}" },
                description = draft.description.trim().ifBlank { "Yapay zeka ile oluşturulan koleksiyon ürünü." },
                price = draft.price.coerceAtLeast(1.0),
                currency = currencySymbol,
                imageUrl = draft.imageUri,
                galleryUrls = listOf(draft.imageUri),
                category = draft.category.trim().ifBlank { "Giyim" },
                subCategory = draft.subCategory.trim().ifBlank { "Yeni Gelenler" },
                tags = draft.tags.map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }.distinct(),
                stockStatus = StockStatus.IN_STOCK,
                stockCount = 20,
                inStock = true,
                sku = "AI-${100 + idx}",
                variants = defaultClothingVariants(),
                sortOrder = idx
            )
        }

        val allDraftTags = drafts.flatMap { it.tags }.map { it.trim() }.filter { it.isNotBlank() }.take(3)
        val newCatalog = Catalog(
            id = newCatalogId,
            title = cleanTitle,
            description = "Gemini AI ile ${createdProducts.size} fotoğraftan otomatik analiz edilerek oluşturuldu.",
            slug = slugify(cleanTitle),
            coverImageUrl = createdProducts.first().imageUrl,
            tags = (listOfNotNull(tag?.trim()?.takeIf { it.isNotBlank() }, "Yeni Sezon") + allDraftTags).distinct(),
            isActive = true,
            productCount = createdProducts.size,
            viewCount = 1,
            orderCount = 0,
            createdAt = System.currentTimeMillis()
        )

        val updatedProducts = createdProducts + _products.value
        val updatedCatalogs = listOf(newCatalog) + _catalogs.value

        _products.value = updatedProducts
        _catalogs.value = updatedCatalogs
        writeProducts(updatedProducts)
        writeCatalogs(updatedCatalogs)
        recalculateAnalytics()
        return newCatalog
    }

    override suspend fun importCatalogFromCsv(
        title: String,
        csvContent: String,
        tag: String?
    ): Catalog {
        val newCatalogId = UUID.randomUUID().toString()
        val cleanTitle = title.trim().ifBlank { "Excel / CSV Kataloğu" }
        val currencySymbol = _settings.value.currencySymbol.ifBlank { "₺" }

        val lines = csvContent
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val createdProducts = lines.mapIndexed { idx, line ->
            val parts = line.split(";", ",").map { it.trim() }
            val name = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "İçe Aktarılan Ürün #${idx + 1}"
            val price = parts.getOrNull(1)
                ?.replace("[^0-9.]".toRegex(), "")
                ?.toDoubleOrNull()
                ?: (390.0 + idx * 60.0)
            val sku = parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "CSV-${200 + idx}"
            val mainImg = "https://picsum.photos/seed/csv-${slugify(name)}-$idx/600/600"

            Product(
                id = UUID.randomUUID().toString(),
                catalogId = newCatalogId,
                name = name,
                description = "Excel/CSV tablosundan içe aktarıldı",
                price = price,
                currency = currencySymbol,
                imageUrl = mainImg,
                galleryUrls = listOf(
                    mainImg,
                    "https://picsum.photos/seed/csv-gal-$idx-1/600/600",
                    "https://picsum.photos/seed/csv-gal-$idx-2/600/600"
                ),
                subCategory = if (idx % 2 == 0) "Kış Sezonu" else "Aksesuar",
                stockStatus = StockStatus.IN_STOCK,
                stockCount = 30,
                inStock = true,
                sku = sku,
                variants = defaultClothingVariants(),
                sortOrder = idx
            )
        }

        val newCatalog = Catalog(
            id = newCatalogId,
            title = cleanTitle,
            description = "Excel/CSV dosyasından ${createdProducts.size} ürün ile aktarıldı.",
            slug = slugify(cleanTitle),
            coverImageUrl = createdProducts.firstOrNull()?.imageUrl.orEmpty(),
            tags = listOfNotNull(tag?.trim()?.takeIf { it.isNotBlank() }, "Toptan").distinct(),
            isActive = true,
            productCount = createdProducts.size,
            viewCount = 1,
            orderCount = 0,
            createdAt = System.currentTimeMillis()
        )

        val updatedProducts = createdProducts + _products.value
        val updatedCatalogs = listOf(newCatalog) + _catalogs.value

        _products.value = updatedProducts
        _catalogs.value = updatedCatalogs
        writeProducts(updatedProducts)
        writeCatalogs(updatedCatalogs)
        recalculateAnalytics()
        return newCatalog
    }

    override suspend fun seedSampleCatalogs() {
        val (sampleCatalogs, sampleProducts) = buildSampleCatalogsAndProducts()
        _catalogs.value = sampleCatalogs
        _products.value = sampleProducts
        writeCatalogs(sampleCatalogs)
        writeProducts(sampleProducts)
        prefs.edit().putBoolean(KEY_INITIAL_SEEDED, true).apply()
        recalculateAnalytics()
    }

    override suspend fun saveProduct(product: Product) {
        val current = _products.value.toMutableList()
        val index = current.indexOfFirst { it.id == product.id }
        val normalizedGallery = if (product.galleryUrls.isEmpty() && product.imageUrl.isNotBlank()) {
            listOf(
                product.imageUrl,
                "https://picsum.photos/seed/${product.id}-angle2/600/600",
                "https://picsum.photos/seed/${product.id}-angle3/600/600"
            )
        } else {
            product.galleryUrls
        }
        val threshold = _settings.value.lowStockThreshold.coerceAtLeast(1)
        val computedStatus = if (product.trackStock) {
            when {
                product.stockCount <= 0 -> StockStatus.OUT_OF_STOCK
                product.stockCount <= threshold -> StockStatus.LOW_STOCK
                else -> StockStatus.IN_STOCK
            }
        } else {
            product.stockStatus
        }
        val normalized = product.copy(
            stockStatus = computedStatus,
            inStock = computedStatus != StockStatus.OUT_OF_STOCK && (!product.trackStock || product.stockCount > 0),
            galleryUrls = normalizedGallery
        )

        if (index >= 0) {
            current[index] = normalized
        } else {
            current.add(0, normalized)
        }
        _products.value = current
        writeProducts(current)
        syncCatalogProductCounts()
    }

    override suspend fun deleteProducts(productIds: Set<String>) {
        if (productIds.isEmpty()) return
        val updated = _products.value.filterNot { it.id in productIds }
        _products.value = updated
        writeProducts(updated)
        syncCatalogProductCounts()
    }

    override suspend fun updateProductsStockStatus(productIds: Set<String>, status: StockStatus) {
        if (productIds.isEmpty()) return
        val threshold = _settings.value.lowStockThreshold.coerceAtLeast(1)
        val updated = _products.value.map { prod ->
            if (prod.id in productIds) {
                val updatedCount = when (status) {
                    StockStatus.IN_STOCK -> if (prod.stockCount <= threshold) threshold + 15 else prod.stockCount
                    StockStatus.LOW_STOCK -> threshold.coerceAtMost(3).coerceAtLeast(1)
                    StockStatus.OUT_OF_STOCK -> 0
                }
                prod.copy(
                    stockStatus = status,
                    stockCount = updatedCount,
                    inStock = status != StockStatus.OUT_OF_STOCK
                )
            } else {
                prod
            }
        }
        _products.value = updated
        writeProducts(updated)
    }

    override suspend fun updateProductStockCount(productId: String, newStockCount: Int) {
        val cleanCount = newStockCount.coerceAtLeast(0)
        val threshold = _settings.value.lowStockThreshold.coerceAtLeast(1)
        var targetProductName: String? = null
        var newStatusForAlert: StockStatus? = null

        val updated = _products.value.map { prod ->
            if (prod.id == productId) {
                val status = when {
                    cleanCount <= 0 -> StockStatus.OUT_OF_STOCK
                    cleanCount <= threshold -> StockStatus.LOW_STOCK
                    else -> StockStatus.IN_STOCK
                }
                if (status != prod.stockStatus && (status == StockStatus.LOW_STOCK || status == StockStatus.OUT_OF_STOCK)) {
                    targetProductName = prod.name
                    newStatusForAlert = status
                }
                prod.copy(
                    trackStock = true,
                    stockCount = cleanCount,
                    stockStatus = status,
                    inStock = cleanCount > 0
                )
            } else {
                prod
            }
        }
        _products.value = updated
        writeProducts(updated)

        if (targetProductName != null && newStatusForAlert != null) {
            val alertNotif = AppNotification(
                id = "notif-stk-${UUID.randomUUID().toString().take(6)}",
                title = if (newStatusForAlert == StockStatus.OUT_OF_STOCK) {
                    "Stok Tükendi: $targetProductName"
                } else {
                    "Kritik Stok Uyarısı: $targetProductName"
                },
                message = if (newStatusForAlert == StockStatus.OUT_OF_STOCK) {
                    "$targetProductName stoğu 0 oldu ve müşteri sayfasında 'Tükendi' olarak işaretlendi."
                } else {
                    "$targetProductName stoğu $cleanCount adede düştü (Eşik: $threshold)."
                },
                timeAgo = "Az önce",
                isUnread = true
            )
            _notifications.value = listOf(alertNotif) + _notifications.value
        }
    }

    override suspend fun bulkAssignProductStock(productIds: Set<String>, stockCount: Int) {
        if (productIds.isEmpty()) return
        val cleanCount = stockCount.coerceAtLeast(0)
        val threshold = _settings.value.lowStockThreshold.coerceAtLeast(1)
        val status = when {
            cleanCount <= 0 -> StockStatus.OUT_OF_STOCK
            cleanCount <= threshold -> StockStatus.LOW_STOCK
            else -> StockStatus.IN_STOCK
        }
        val updated = _products.value.map { prod ->
            if (prod.id in productIds) {
                prod.copy(
                    trackStock = true,
                    stockCount = cleanCount,
                    stockStatus = status,
                    inStock = cleanCount > 0
                )
            } else {
                prod
            }
        }
        _products.value = updated
        writeProducts(updated)
    }

    override suspend fun markProductsOutOfStock(productIds: Set<String>) {
        if (productIds.isEmpty()) return
        val updated = _products.value.map { prod ->
            if (prod.id in productIds) {
                prod.copy(
                    trackStock = true,
                    stockCount = 0,
                    stockStatus = StockStatus.OUT_OF_STOCK,
                    inStock = false
                )
            } else {
                prod
            }
        }
        _products.value = updated
        writeProducts(updated)
    }

    override suspend fun updateLowStockThreshold(threshold: Int) {
        val cleanThreshold = threshold.coerceIn(1, 500)
        val newSettings = _settings.value.copy(lowStockThreshold = cleanThreshold)
        _settings.value = newSettings
        writeSettings(newSettings)

        // Tüm ürünlerin stok durumunu yeni eşiğe göre yeniden hesapla
        val updatedProducts = _products.value.map { prod ->
            if (!prod.trackStock) {
                prod
            } else {
                val status = when {
                    prod.stockCount <= 0 -> StockStatus.OUT_OF_STOCK
                    prod.stockCount <= cleanThreshold -> StockStatus.LOW_STOCK
                    else -> StockStatus.IN_STOCK
                }
                prod.copy(
                    stockStatus = status,
                    inStock = prod.stockCount > 0
                )
            }
        }
        _products.value = updatedProducts
        writeProducts(updatedProducts)
    }

    override suspend fun bulkImportProducts(
        catalogId: String,
        importedProducts: List<Product>
    ): Int {
        if (importedProducts.isEmpty()) return 0
        val targetCatId = catalogId.ifBlank {
            _catalogs.value.firstOrNull()?.id ?: "cat-ilkbahar-yaz"
        }
        val threshold = _settings.value.lowStockThreshold.coerceAtLeast(1)
        val currencySymbol = _settings.value.currencySymbol.ifBlank { "₺" }

        val normalizedImported = importedProducts.mapIndexed { idx, prod ->
            val cleanStock = prod.stockCount.coerceAtLeast(0)
            val status = when {
                cleanStock <= 0 -> StockStatus.OUT_OF_STOCK
                cleanStock <= threshold -> StockStatus.LOW_STOCK
                else -> StockStatus.IN_STOCK
            }
            val mainImg = prod.imageUrl.ifBlank {
                "https://picsum.photos/seed/csv-imp-${slugify(prod.name)}-$idx/600/600"
            }
            prod.copy(
                id = prod.id.ifBlank { UUID.randomUUID().toString() },
                catalogId = targetCatId,
                currency = prod.currency.ifBlank { currencySymbol },
                imageUrl = mainImg,
                galleryUrls = if (prod.galleryUrls.isEmpty()) {
                    listOf(
                        mainImg,
                        "https://picsum.photos/seed/csv-imp-gal1-$idx/600/600",
                        "https://picsum.photos/seed/csv-imp-gal2-$idx/600/600"
                    )
                } else prod.galleryUrls,
                trackStock = true,
                stockCount = cleanStock,
                stockStatus = status,
                inStock = cleanStock > 0,
                sortOrder = idx
            )
        }

        val updatedProducts = normalizedImported + _products.value
        _products.value = updatedProducts
        writeProducts(updatedProducts)
        syncCatalogProductCounts()
        return normalizedImported.size
    }

    override suspend fun moveProductsToCatalog(productIds: Set<String>, targetCatalogId: String) {
        if (productIds.isEmpty()) return
        val updated = _products.value.map { prod ->
            if (prod.id in productIds) {
                prod.copy(catalogId = targetCatalogId)
            } else {
                prod
            }
        }
        _products.value = updated
        writeProducts(updated)
        syncCatalogProductCounts()
    }

    override suspend fun copyProductsToCatalog(productIds: Set<String>, targetCatalogId: String): Int {
        val selected = _products.value.filter { it.id in productIds }
        if (selected.isEmpty() || targetCatalogId.isBlank()) return 0

        val currentTargetProducts = _products.value.filter { it.catalogId == targetCatalogId }
        val baseSortOrder = currentTargetProducts.size

        val copiedProducts = selected.mapIndexed { idx, prod ->
            prod.copy(
                id = UUID.randomUUID().toString(),
                catalogId = targetCatalogId,
                sortOrder = baseSortOrder + idx
            )
        }

        val updatedProducts = copiedProducts + _products.value
        _products.value = updatedProducts
        writeProducts(updatedProducts)
        syncCatalogProductCounts()
        return copiedProducts.size
    }

    override suspend fun copyProductsToNewCatalog(
        productIds: Set<String>,
        newCatalogTitle: String
    ): Catalog? {
        val selected = _products.value.filter { it.id in productIds }
        if (selected.isEmpty()) return null

        val newCatalogId = UUID.randomUUID().toString()
        val cleanTitle = newCatalogTitle.trim().ifBlank { "Seçili Ürünler Kataloğu" }

        val copiedProducts = selected.mapIndexed { idx, prod ->
            prod.copy(
                id = UUID.randomUUID().toString(),
                catalogId = newCatalogId,
                sortOrder = idx
            )
        }

        val newCatalog = Catalog(
            id = newCatalogId,
            title = cleanTitle,
            description = "${copiedProducts.size} seçili üründen oluşturulan özel katalog.",
            slug = slugify(cleanTitle),
            coverImageUrl = copiedProducts.first().imageUrl,
            tags = listOf("Yeni Sezon"),
            isActive = true,
            productCount = copiedProducts.size,
            viewCount = 0,
            orderCount = 0,
            createdAt = System.currentTimeMillis()
        )

        val updatedProducts = copiedProducts + _products.value
        val updatedCatalogs = listOf(newCatalog) + _catalogs.value
        _products.value = updatedProducts
        _catalogs.value = updatedCatalogs
        writeProducts(updatedProducts)
        writeCatalogs(updatedCatalogs)
        recalculateAnalytics()
        return newCatalog
    }

    override suspend fun reorderCatalogProducts(
        catalogId: String,
        orderedProductIds: List<String>
    ) {
        val orderMap = orderedProductIds.withIndex().associate { it.value to it.index }
        val updated = _products.value.map { prod ->
            if (prod.catalogId == catalogId && orderMap.containsKey(prod.id)) {
                prod.copy(sortOrder = orderMap.getValue(prod.id))
            } else {
                prod
            }
        }
        _products.value = updated
        writeProducts(updated)
    }

    override suspend fun saveReseller(reseller: Reseller) {
        val current = _resellers.value.toMutableList()
        val index = current.indexOfFirst { it.id == reseller.id }
        val cleanShortCode = reseller.shortCode.trim().ifBlank {
            "bayi-${slugify(reseller.name).take(12)}-${(10..99).random()}"
        }
        val normalized = reseller.copy(shortCode = cleanShortCode)
        if (index >= 0) {
            current[index] = normalized
        } else {
            current.add(0, normalized)
        }
        _resellers.value = current
        writeResellers(current)
    }

    override suspend fun deleteReseller(resellerId: String) {
        val updated = _resellers.value.filterNot { it.id == resellerId }
        _resellers.value = updated
        writeResellers(updated)
    }

    private fun syncCatalogProductCounts() {
        val allProducts = _products.value
        val updatedCatalogs = _catalogs.value.map { cat ->
            val catProducts = allProducts.filter { it.catalogId == cat.id }
            val cover = cat.coverImageUrl.ifBlank { catProducts.firstOrNull()?.imageUrl.orEmpty() }
            cat.copy(
                productCount = catProducts.size,
                coverImageUrl = cover
            )
        }
        _catalogs.value = updatedCatalogs
        writeCatalogs(updatedCatalogs)
        recalculateAnalytics()
    }

    override suspend fun createCustomerOrder(order: Order) {
        val now = order.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis()
        val catalogTitle = order.catalogTitle.ifBlank {
            _catalogs.value.find { it.id == order.catalogId }?.title ?: "Dijital Katalog"
        }
        val normalizedOrderNumber = order.orderNumber.ifBlank {
            if (order.id.startsWith("SIP-", ignoreCase = true)) {
                "#${order.id.uppercase()}"
            } else {
                "#SIP-${order.id.takeLast(4).uppercase()}"
            }
        }

        // YENİ SİPARİŞ GELDİĞİNDE STOĞU OTOMATİK DÜŞÜR (stok takibi açıksa)
        val deductedNames = deductOrRestoreStockForOrderItems(order.items, deduct = true)

        val initialTimeline = if (order.timeline.isNotEmpty()) {
            order.timeline
        } else {
            val stockNote = if (deductedNames.isNotEmpty()) {
                "Stoktan otomatik düşüldü: ${deductedNames.joinToString(", ")}"
            } else {
                "Müşteri katalog sayfasından oluşturuldu"
            }
            listOf(
                OrderTimelineEntry(
                    id = "tl-${UUID.randomUUID().toString().take(8)}",
                    status = order.status,
                    title = "Sipariş oluşturuldu (${order.status.labelTr})",
                    note = stockNote,
                    timestamp = now
                )
            )
        }

        val normalizedOrder = order.copy(
            orderNumber = normalizedOrderNumber,
            catalogTitle = catalogTitle,
            timeline = initialTimeline,
            stockDeducted = true,
            createdAt = now
        )

        val updatedOrders = listOf(normalizedOrder) + _orders.value
        _orders.value = updatedOrders
        writeOrders(updatedOrders)

        // Katalog orderCount metriğini artır
        val updatedCatalogs = _catalogs.value.map { cat ->
            if (cat.id == normalizedOrder.catalogId) {
                cat.copy(orderCount = cat.orderCount + 1)
            } else {
                cat
            }
        }
        _catalogs.value = updatedCatalogs
        writeCatalogs(updatedCatalogs)

        // Bildirim ekle
        val newNotif = AppNotification(
            id = "notif-${UUID.randomUUID().toString().take(8)}",
            title = "Yeni Sipariş Geldi! (${normalizedOrder.orderNumber})",
            message = "${normalizedOrder.customerName} tarafından ${normalizedOrder.totalAmount.toInt()} ${normalizedOrder.currency} tutarında yeni sipariş oluşturuldu.",
            timeAgo = "Az önce",
            isUnread = true
        )
        _notifications.value = listOf(newNotif) + _notifications.value
        recalculateAnalytics()
    }

    /**
     * Sipariş kalemlerindeki ürünlerin stok takibi (trackStock) açıksa stoğu otomatik düşürür veya iade eder.
     */
    private fun deductOrRestoreStockForOrderItems(
        items: List<OrderItem>,
        deduct: Boolean
    ): List<String> {
        if (items.isEmpty()) return emptyList()
        val affectedProductSummaries = mutableListOf<String>()
        var anyChanged = false

        val updatedProducts = _products.value.map { product ->
            val matchingItems = items.filter { it.productId == product.id }
            if (matchingItems.isEmpty() || !product.trackStock) {
                product
            } else {
                val totalQtyDelta = matchingItems.sumOf { it.quantity }
                val newStockCount = if (deduct) {
                    (product.stockCount - totalQtyDelta).coerceAtLeast(0)
                } else {
                    product.stockCount + totalQtyDelta
                }

                val newStockStatus = when {
                    newStockCount <= 0 -> StockStatus.OUT_OF_STOCK
                    newStockCount <= 5 -> StockStatus.LOW_STOCK
                    else -> StockStatus.IN_STOCK
                }

                // Varyant stoklarını da güncelle (eşleşen varyant etiketi varsa)
                val updatedVariants = product.variants.map { group ->
                    val updatedDetails = group.optionDetails.map { optDetail ->
                        val variantQty = matchingItems
                            .filter { item ->
                                item.selectedVariantSummary.contains("${group.name}: ${optDetail.label}", ignoreCase = true) ||
                                    item.selectedVariantSummary.split("•").any { part ->
                                        part.substringAfter(":").trim().equals(optDetail.label, ignoreCase = true)
                                    }
                            }
                            .sumOf { it.quantity }

                        if (variantQty > 0) {
                            val newOptStock = if (deduct) {
                                (optDetail.stock - variantQty).coerceAtLeast(0)
                            } else {
                                optDetail.stock + variantQty
                            }
                            optDetail.copy(stock = newOptStock)
                        } else {
                            optDetail
                        }
                    }
                    group.copy(optionDetails = updatedDetails)
                }

                anyChanged = true
                affectedProductSummaries.add("${product.name} (${product.stockCount} → $newStockCount adet)")
                product.copy(
                    stockCount = newStockCount,
                    stockStatus = newStockStatus,
                    inStock = newStockStatus != StockStatus.OUT_OF_STOCK,
                    variants = updatedVariants
                )
            }
        }

        if (anyChanged) {
            _products.value = updatedProducts
            writeProducts(updatedProducts)
        }
        return affectedProductSummaries
    }

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus) {
        val now = System.currentTimeMillis()
        val updated = _orders.value.map { order ->
            if (order.id != orderId || order.status == status) {
                order
            } else {
                var newStockDeducted = order.stockDeducted
                var stockActionNote = ""

                if (status == OrderStatus.CANCELLED && order.stockDeducted) {
                    val restored = deductOrRestoreStockForOrderItems(order.items, deduct = false)
                    newStockDeducted = false
                    if (restored.isNotEmpty()) {
                        stockActionNote = " • Stok iade edildi: ${restored.joinToString(", ")}"
                    }
                } else if (order.status == OrderStatus.CANCELLED && status != OrderStatus.CANCELLED && !order.stockDeducted) {
                    val deducted = deductOrRestoreStockForOrderItems(order.items, deduct = true)
                    newStockDeducted = true
                    if (deducted.isNotEmpty()) {
                        stockActionNote = " • Stok tekrar düşüldü: ${deducted.joinToString(", ")}"
                    }
                }

                val statusNote = when (status) {
                    OrderStatus.NEW -> "Sipariş yeni durumuna alındı"
                    OrderStatus.APPROVED -> "Sipariş satıcı tarafından onaylandı"
                    OrderStatus.PREPARING -> "Ürünler paketleniyor ve sevkiyata hazırlanıyor"
                    OrderStatus.SHIPPED -> "Sipariş kargoya teslim edildi"
                    OrderStatus.DELIVERED -> "Sipariş müşteriye başarıyla teslim edildi"
                    OrderStatus.CANCELLED -> "Sipariş iptal edildi"
                } + stockActionNote

                val newEntry = OrderTimelineEntry(
                    id = "tl-${UUID.randomUUID().toString().take(8)}",
                    status = status,
                    title = "Durum '${status.labelTr}' olarak güncellendi",
                    note = statusNote,
                    timestamp = now
                )

                order.copy(
                    status = status,
                    stockDeducted = newStockDeducted,
                    timeline = order.timeline + newEntry
                )
            }
        }
        _orders.value = updated
        writeOrders(updated)
        recalculateAnalytics()
    }

    override suspend fun seedSampleOrders() {
        val sampleOrders = buildSampleOrders(_products.value, _catalogs.value)
        _orders.value = sampleOrders
        writeOrders(sampleOrders)
        prefs.edit().putBoolean(KEY_ORDERS_SEEDED, true).apply()
        recalculateAnalytics()
    }

    override suspend fun clearAllOrders() {
        _orders.value = emptyList()
        writeOrders(emptyList())
        prefs.edit().putBoolean(KEY_ORDERS_SEEDED, true).apply()
        recalculateAnalytics()
    }

    override suspend fun updateSettings(newSettings: StoreSettings) {
        _settings.value = newSettings
        writeSettings(newSettings)
    }

    override suspend fun markAllNotificationsRead() {
        _notifications.value = _notifications.value.map { it.copy(isUnread = false) }
    }

    override suspend fun recordShareEvent(event: ShareEvent) {
        val updated = listOf(event) + _shareEvents.value
        _shareEvents.value = updated
        writeShareEvents(updated)
    }

    override suspend fun recordCatalogVisit(
        sessionId: String,
        catalogId: String,
        shortId: String,
        referrer: String
    ) {
        // Aynı oturumu (sessionId + catalogId) tekrar saymamak için kontrol et
        val alreadyVisitedInSession = _catalogAnalyticsEvents.value.any {
            it.eventType == "visit" && it.sessionId == sessionId && it.catalogId == catalogId
        }
        if (alreadyVisitedInSession) return

        val newEvent = CatalogAnalyticsEvent(
            id = "evt-${UUID.randomUUID().toString().take(8)}",
            sessionId = sessionId,
            eventType = "visit",
            catalogId = catalogId,
            shortId = shortId,
            referrer = referrer
        )
        val updatedEvents = listOf(newEvent) + _catalogAnalyticsEvents.value
        _catalogAnalyticsEvents.value = updatedEvents
        writeCatalogAnalyticsEvents(updatedEvents)

        // Katalog görüntülenme sayısını 1 artır
        val updatedCatalogs = _catalogs.value.map { cat ->
            if (cat.id == catalogId) cat.copy(viewCount = cat.viewCount + 1) else cat
        }
        _catalogs.value = updatedCatalogs
        writeCatalogs(updatedCatalogs)
        recalculateAnalytics()
    }

    override suspend fun recordProductView(
        sessionId: String,
        catalogId: String,
        shortId: String,
        productId: String,
        productName: String
    ) {
        // Aynı oturumda aynı ürün görüntülemesini tekrar saymamak için sessionId kontrolü
        val alreadyViewedInSession = _catalogAnalyticsEvents.value.any {
            it.eventType == "productView" && it.sessionId == sessionId && it.productId == productId
        }
        if (alreadyViewedInSession) return

        val newEvent = CatalogAnalyticsEvent(
            id = "evt-${UUID.randomUUID().toString().take(8)}",
            sessionId = sessionId,
            eventType = "productView",
            catalogId = catalogId,
            shortId = shortId,
            productId = productId,
            productName = productName
        )
        val updatedEvents = listOf(newEvent) + _catalogAnalyticsEvents.value
        _catalogAnalyticsEvents.value = updatedEvents
        writeCatalogAnalyticsEvents(updatedEvents)
    }

    override suspend fun recordAddToCart(
        sessionId: String,
        catalogId: String,
        shortId: String,
        productId: String,
        productName: String,
        quantity: Int
    ) {
        val newEvent = CatalogAnalyticsEvent(
            id = "evt-${UUID.randomUUID().toString().take(8)}",
            sessionId = sessionId,
            eventType = "addToCart",
            catalogId = catalogId,
            shortId = shortId,
            productId = productId,
            productName = productName,
            quantity = quantity
        )
        val updatedEvents = listOf(newEvent) + _catalogAnalyticsEvents.value
        _catalogAnalyticsEvents.value = updatedEvents
        writeCatalogAnalyticsEvents(updatedEvents)
    }

    override suspend fun seedSampleAnalyticsEvents() {
        val sampleEvents = buildSampleAnalyticsEvents(_catalogs.value, _products.value)
        _catalogAnalyticsEvents.value = sampleEvents
        writeCatalogAnalyticsEvents(sampleEvents)
        prefs.edit().putBoolean(KEY_ANALYTICS_SEEDED, true).apply()
        recalculateAnalytics()
    }

    override suspend fun resetStorage() {
        prefs.edit().clear().apply()
        _authUser.value = null
        val (sampleCatalogs, sampleProducts) = buildSampleCatalogsAndProducts()
        val sampleResellers = buildSampleResellers(sampleCatalogs)
        val sampleOrders = buildSampleOrders(sampleProducts, sampleCatalogs)
        val sampleEvents = buildSampleAnalyticsEvents(sampleCatalogs, sampleProducts)
        _catalogs.value = sampleCatalogs
        _products.value = sampleProducts
        _resellers.value = sampleResellers
        _orders.value = sampleOrders
        _shareEvents.value = emptyList()
        _catalogAnalyticsEvents.value = sampleEvents
        _settings.value = StoreSettings()
        writeCatalogs(sampleCatalogs)
        writeProducts(sampleProducts)
        writeResellers(sampleResellers)
        writeOrders(sampleOrders)
        writeCatalogAnalyticsEvents(sampleEvents)
        prefs.edit()
            .putBoolean(KEY_INITIAL_SEEDED, true)
            .putBoolean(KEY_RESELLERS_SEEDED, true)
            .putBoolean(KEY_ORDERS_SEEDED, true)
            .putBoolean(KEY_ANALYTICS_SEEDED, true)
            .apply()
        recalculateAnalytics()
    }

    private fun recalculateAnalytics() {
        val visitEvents = _catalogAnalyticsEvents.value.filter { it.eventType == "visit" }
        val totalViews = if (visitEvents.isNotEmpty()) {
            visitEvents.size
        } else {
            _catalogs.value.sumOf { it.viewCount }
        }
        val uniqueVisitors = if (visitEvents.isNotEmpty()) {
            visitEvents.map { it.sessionId }.distinct().size
        } else {
            (totalViews * 0.72).toInt()
        }
        val validOrders = _orders.value.filter { it.status != OrderStatus.CANCELLED }
        val totalOrders = validOrders.size
        val revenue = validOrders.sumOf { it.totalAmount }
        val conversion = if (uniqueVisitors > 0) {
            ((totalOrders.toDouble() / uniqueVisitors.toDouble()) * 100.0)
        } else {
            0.0
        }
        _analytics.value = AnalyticsSummary(
            totalViews = totalViews,
            uniqueVisitors = uniqueVisitors,
            totalOrders = totalOrders,
            totalRevenue = revenue,
            conversionRatePercent = conversion
        )
    }

    private fun readAuthUser(): AuthUser? {
        val raw = prefs.getString(KEY_AUTH_USER, null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            AuthUser(
                id = obj.optString("id"),
                phoneCountryCode = obj.optString("phoneCountryCode", "+90"),
                phoneNumber = obj.optString("phoneNumber", ""),
                fullPhone = obj.optString("fullPhone", ""),
                email = obj.optString("email", ""),
                displayName = obj.optString("displayName", ""),
                passwordHash = obj.optString("passwordHash", ""),
                firebaseUid = obj.optString("firebaseUid", ""),
                provider = obj.optString("provider", "phone_password"),
                isOnboardingCompleted = obj.optBoolean("isOnboardingCompleted", false),
                loggedInAt = obj.optLong("loggedInAt", System.currentTimeMillis())
            )
        }.getOrNull()
    }

    private fun writeAuthUser(user: AuthUser?) {
        if (user == null) {
            prefs.edit().remove(KEY_AUTH_USER).apply()
            mariaDbEngine.syncAuthUserAsync(null, null)
            return
        }
        val obj = JSONObject().apply {
            put("id", user.id)
            put("phoneCountryCode", user.phoneCountryCode)
            put("phoneNumber", user.phoneNumber)
            put("fullPhone", user.fullPhone)
            put("email", user.email)
            put("displayName", user.displayName)
            put("passwordHash", user.passwordHash)
            put("firebaseUid", user.firebaseUid)
            put("provider", user.provider)
            put("isOnboardingCompleted", user.isOnboardingCompleted)
            put("loggedInAt", user.loggedInAt)
        }
        val json = obj.toString()
        prefs.edit().putString(KEY_AUTH_USER, json).apply()
        mariaDbEngine.syncAuthUserAsync(user, json)
    }

    private fun normalizePhoneKey(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    private fun readPhoneCredentials(): Map<String, PhonePasswordCredential> {
        val raw = prefs.getString(KEY_PHONE_CREDENTIALS, null) ?: return emptyMap()
        return runCatching {
            val arr = JSONArray(raw)
            buildMap {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val cred = PhonePasswordCredential(
                        fullPhone = obj.optString("fullPhone"),
                        phoneCountryCode = obj.optString("phoneCountryCode", "+90"),
                        phoneNumber = obj.optString("phoneNumber"),
                        passwordHash = obj.optString("passwordHash"),
                        recoveryEmail = obj.optString("recoveryEmail", ""),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                    if (cred.fullPhone.isNotBlank() && cred.passwordHash.isNotBlank()) {
                        put(normalizePhoneKey(cred.fullPhone), cred)
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun writePhoneCredentials(list: List<PhonePasswordCredential>) {
        val arr = JSONArray()
        list.forEach { cred ->
            arr.put(
                JSONObject().apply {
                    put("fullPhone", cred.fullPhone)
                    put("phoneCountryCode", cred.phoneCountryCode)
                    put("phoneNumber", cred.phoneNumber)
                    put("passwordHash", cred.passwordHash)
                    put("recoveryEmail", cred.recoveryEmail)
                    put("updatedAt", cred.updatedAt)
                }
            )
        }
        val json = arr.toString()
        prefs.edit().putString(KEY_PHONE_CREDENTIALS, json).apply()
        mariaDbEngine.syncPhoneCredentialsAsync(list, json)
    }

    private fun readCatalogs(): List<Catalog> {
        val raw = prefs.getString(KEY_CATALOGS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val tagsArr = obj.optJSONArray("tags") ?: JSONArray()
                    val tags = buildList {
                        for (t in 0 until tagsArr.length()) {
                            val tagStr = tagsArr.optString(t)
                            if (tagStr.isNotBlank()) add(tagStr)
                        }
                    }
                    add(
                        Catalog(
                            id = obj.optString("id"),
                            title = obj.optString("title"),
                            description = obj.optString("description"),
                            slug = obj.optString("slug"),
                            coverImageUrl = obj.optString("coverImageUrl", ""),
                            tags = tags,
                            isActive = obj.optBoolean("isActive", true),
                            productCount = obj.optInt("productCount", 0),
                            viewCount = obj.optInt("viewCount", 0),
                            orderCount = obj.optInt("orderCount", 0),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeCatalogs(list: List<Catalog>) {
        val array = JSONArray()
        list.forEach { item ->
            val tagsArr = JSONArray()
            item.tags.forEach { tagsArr.put(it) }
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("description", item.description)
                    put("slug", item.slug)
                    put("coverImageUrl", item.coverImageUrl)
                    put("tags", tagsArr)
                    put("isActive", item.isActive)
                    put("productCount", item.productCount)
                    put("viewCount", item.viewCount)
                    put("orderCount", item.orderCount)
                    put("createdAt", item.createdAt)
                }
            )
        }
        val json = array.toString()
        prefs.edit().putString(KEY_CATALOGS, json).apply()
        mariaDbEngine.syncCatalogsAsync(list, json)
    }

    private fun readProducts(): List<Product> {
        val raw = prefs.getString(KEY_PRODUCTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val galleryArr = obj.optJSONArray("galleryUrls") ?: JSONArray()
                    val gallery = buildList {
                        for (g in 0 until galleryArr.length()) {
                            val url = galleryArr.optString(g)
                            if (url.isNotBlank()) add(url)
                        }
                    }
                    val tagsArr = obj.optJSONArray("tags") ?: JSONArray()
                    val tags = buildList {
                        for (t in 0 until tagsArr.length()) {
                            val tagStr = tagsArr.optString(t)
                            if (tagStr.isNotBlank()) add(tagStr)
                        }
                    }
                    val variantsArr = obj.optJSONArray("variants") ?: JSONArray()
                    val variants = buildList {
                        for (v in 0 until variantsArr.length()) {
                            val vObj = variantsArr.getJSONObject(v)
                            val optsArr = vObj.optJSONArray("options") ?: JSONArray()
                            val opts = buildList {
                                for (o in 0 until optsArr.length()) {
                                    val optStr = optsArr.optString(o)
                                    if (optStr.isNotBlank()) add(optStr)
                                }
                            }
                            val detailsArr = vObj.optJSONArray("optionDetails")
                            val details = if (detailsArr != null && detailsArr.length() > 0) {
                                buildList {
                                    for (d in 0 until detailsArr.length()) {
                                        val dObj = detailsArr.getJSONObject(d)
                                        val lbl = dObj.optString("label", "")
                                        if (lbl.isNotBlank()) {
                                            add(
                                                ProductVariantOption(
                                                    label = lbl,
                                                    extraPrice = dObj.optDouble("extraPrice", 0.0),
                                                    stock = dObj.optInt("stock", 10)
                                                )
                                            )
                                        }
                                    }
                                }
                            } else {
                                opts.map { ProductVariantOption(label = it) }
                            }
                            val effectiveOpts = if (opts.isNotEmpty()) opts else details.map { it.label }
                            add(
                                ProductVariantGroup(
                                    name = vObj.optString("name", "Seçenek"),
                                    options = effectiveOpts,
                                    optionDetails = details
                                )
                            )
                        }
                    }
                    val statusStr = obj.optString("stockStatus", StockStatus.IN_STOCK.name)
                    val stockStatus = runCatching { StockStatus.valueOf(statusStr) }
                        .getOrDefault(StockStatus.IN_STOCK)
                    val origPriceVal = obj.optDouble("originalPrice", -1.0)

                    add(
                        Product(
                            id = obj.optString("id"),
                            catalogId = obj.optString("catalogId"),
                            name = obj.optString("name"),
                            description = obj.optString("description"),
                            price = obj.optDouble("price", 0.0),
                            originalPrice = if (origPriceVal > 0) origPriceVal else null,
                            currency = obj.optString("currency", "₺"),
                            imageUrl = obj.optString("imageUrl", ""),
                            galleryUrls = gallery,
                            category = obj.optString("category", "Giyim"),
                            subCategory = obj.optString("subCategory", "Genel"),
                            tags = tags,
                            trackStock = obj.optBoolean("trackStock", true),
                            stockStatus = stockStatus,
                            stockCount = obj.optInt("stockCount", 20),
                            inStock = obj.optBoolean("inStock", true),
                            sku = obj.optString("sku", ""),
                            variants = variants,
                            secretNote = obj.optString("secretNote", ""),
                            sortOrder = obj.optInt("sortOrder", i),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeProducts(list: List<Product>) {
        val array = JSONArray()
        list.forEach { item ->
            val galleryArr = JSONArray()
            item.galleryUrls.forEach { galleryArr.put(it) }

            val tagsArr = JSONArray()
            item.tags.forEach { tagsArr.put(it) }

            val variantsArr = JSONArray()
            item.variants.forEach { group ->
                val optsArr = JSONArray()
                group.options.forEach { optsArr.put(it) }
                val detailsArr = JSONArray()
                group.optionDetails.forEach { detail ->
                    detailsArr.put(
                        JSONObject().apply {
                            put("label", detail.label)
                            put("extraPrice", detail.extraPrice)
                            put("stock", detail.stock)
                        }
                    )
                }
                variantsArr.put(
                    JSONObject().apply {
                        put("name", group.name)
                        put("options", optsArr)
                        put("optionDetails", detailsArr)
                    }
                )
            }

            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("catalogId", item.catalogId)
                    put("name", item.name)
                    put("description", item.description)
                    put("price", item.price)
                    if (item.originalPrice != null) {
                        put("originalPrice", item.originalPrice)
                    }
                    put("currency", item.currency)
                    put("imageUrl", item.imageUrl)
                    put("galleryUrls", galleryArr)
                    put("category", item.category)
                    put("subCategory", item.subCategory)
                    put("tags", tagsArr)
                    put("trackStock", item.trackStock)
                    put("stockStatus", item.stockStatus.name)
                    put("stockCount", item.stockCount)
                    put("inStock", item.inStock)
                    put("sku", item.sku)
                    put("variants", variantsArr)
                    put("secretNote", item.secretNote)
                    put("sortOrder", item.sortOrder)
                    put("createdAt", item.createdAt)
                }
            )
        }
        val json = array.toString()
        prefs.edit().putString(KEY_PRODUCTS, json).apply()
        mariaDbEngine.syncProductsAsync(list, json)
    }

    private fun readOrders(): List<Order> {
        val raw = prefs.getString(KEY_ORDERS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val itemsArr = obj.optJSONArray("items") ?: JSONArray()
                    val orderItems = buildList {
                        for (j in 0 until itemsArr.length()) {
                            val itemObj = itemsArr.getJSONObject(j)
                            add(
                                OrderItem(
                                    productId = itemObj.optString("productId"),
                                    productName = itemObj.optString("productName"),
                                    quantity = itemObj.optInt("quantity", 1),
                                    unitPrice = itemObj.optDouble("unitPrice", 0.0),
                                    selectedVariantSummary = itemObj.optString("selectedVariantSummary", ""),
                                    imageUrl = itemObj.optString("imageUrl", "")
                                )
                            )
                        }
                    }
                    val statusName = obj.optString("status", OrderStatus.NEW.name)
                    val status = when (statusName) {
                        "PENDING" -> OrderStatus.NEW
                        "COMPLETED" -> OrderStatus.DELIVERED
                        else -> runCatching { OrderStatus.valueOf(statusName) }.getOrDefault(OrderStatus.NEW)
                    }
                    val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                    val timelineArr = obj.optJSONArray("timeline")
                    val timeline = if (timelineArr != null && timelineArr.length() > 0) {
                        buildList {
                            for (t in 0 until timelineArr.length()) {
                                val tObj = timelineArr.getJSONObject(t)
                                val tStatusStr = tObj.optString("status", status.name)
                                val tStatus = runCatching { OrderStatus.valueOf(tStatusStr) }
                                    .getOrDefault(status)
                                add(
                                    OrderTimelineEntry(
                                        id = tObj.optString("id", "tl-$t"),
                                        status = tStatus,
                                        title = tObj.optString("title", "Durum: ${tStatus.labelTr}"),
                                        note = tObj.optString("note", ""),
                                        timestamp = tObj.optLong("timestamp", createdAt)
                                    )
                                )
                            }
                        }
                    } else {
                        listOf(
                            OrderTimelineEntry(
                                id = "tl-init-$i",
                                status = OrderStatus.NEW,
                                title = "Sipariş oluşturuldu (Yeni)",
                                note = "Müşteri katalog bağlantısı üzerinden iletildi",
                                timestamp = createdAt
                            )
                        )
                    }

                    val idStr = obj.optString("id")
                    val orderNo = obj.optString("orderNumber").ifBlank {
                        if (idStr.startsWith("SIP-", ignoreCase = true)) "#${idStr.uppercase()}"
                        else "#SIP-${idStr.takeLast(4).uppercase()}"
                    }

                    add(
                        Order(
                            id = idStr,
                            orderNumber = orderNo,
                            catalogId = obj.optString("catalogId"),
                            catalogTitle = obj.optString("catalogTitle", "2025 İlkbahar / Yaz Koleksiyonu"),
                            customerName = obj.optString("customerName"),
                            customerPhone = obj.optString("customerPhone"),
                            customerAddress = obj.optString("customerAddress", ""),
                            customerNote = obj.optString("customerNote", ""),
                            items = orderItems,
                            totalAmount = obj.optDouble("totalAmount", 0.0),
                            currency = obj.optString("currency", "₺"),
                            status = status,
                            timeline = timeline,
                            stockDeducted = obj.optBoolean("stockDeducted", true),
                            resellerId = obj.optString("resellerId", ""),
                            resellerName = obj.optString("resellerName", ""),
                            createdAt = createdAt
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeOrders(list: List<Order>) {
        val array = JSONArray()
        list.forEach { order ->
            val itemsArr = JSONArray()
            order.items.forEach { item ->
                itemsArr.put(
                    JSONObject().apply {
                        put("productId", item.productId)
                        put("productName", item.productName)
                        put("quantity", item.quantity)
                        put("unitPrice", item.unitPrice)
                        put("selectedVariantSummary", item.selectedVariantSummary)
                        put("imageUrl", item.imageUrl)
                    }
                )
            }
            val timelineArr = JSONArray()
            order.timeline.forEach { entry ->
                timelineArr.put(
                    JSONObject().apply {
                        put("id", entry.id)
                        put("status", entry.status.name)
                        put("title", entry.title)
                        put("note", entry.note)
                        put("timestamp", entry.timestamp)
                    }
                )
            }
            array.put(
                JSONObject().apply {
                    put("id", order.id)
                    put("orderNumber", order.orderNumber)
                    put("catalogId", order.catalogId)
                    put("catalogTitle", order.catalogTitle)
                    put("customerName", order.customerName)
                    put("customerPhone", order.customerPhone)
                    put("customerAddress", order.customerAddress)
                    put("customerNote", order.customerNote)
                    put("items", itemsArr)
                    put("totalAmount", order.totalAmount)
                    put("currency", order.currency)
                    put("status", order.status.name)
                    put("timeline", timelineArr)
                    put("stockDeducted", order.stockDeducted)
                    put("resellerId", order.resellerId)
                    put("resellerName", order.resellerName)
                    put("createdAt", order.createdAt)
                }
            )
        }
        val json = array.toString()
        prefs.edit().putString(KEY_ORDERS, json).apply()
        mariaDbEngine.syncOrdersAsync(list, json)
    }

    private fun readResellers(): List<Reseller> {
        val raw = prefs.getString(KEY_RESELLERS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val catArr = obj.optJSONArray("visibleCatalogIds") ?: JSONArray()
                    val catIds = buildList {
                        for (j in 0 until catArr.length()) {
                            val cid = catArr.optString(j)
                            if (cid.isNotBlank()) add(cid)
                        }
                    }
                    val modeStr = obj.optString("priceMode", ResellerPriceMode.RESELLER_ADD_MARGIN_PERCENT.name)
                    val priceMode = runCatching { ResellerPriceMode.valueOf(modeStr) }
                        .getOrDefault(ResellerPriceMode.RESELLER_ADD_MARGIN_PERCENT)
                    add(
                        Reseller(
                            id = obj.optString("id"),
                            name = obj.optString("name"),
                            phone = obj.optString("phone"),
                            shortCode = obj.optString("shortCode"),
                            visibleCatalogIds = catIds,
                            priceMode = priceMode,
                            marginValue = obj.optDouble("marginValue", 20.0),
                            note = obj.optString("note", ""),
                            isActive = obj.optBoolean("isActive", true),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeResellers(list: List<Reseller>) {
        val array = JSONArray()
        list.forEach { reseller ->
            val catArr = JSONArray()
            reseller.visibleCatalogIds.forEach { catArr.put(it) }
            array.put(
                JSONObject().apply {
                    put("id", reseller.id)
                    put("name", reseller.name)
                    put("phone", reseller.phone)
                    put("shortCode", reseller.shortCode)
                    put("visibleCatalogIds", catArr)
                    put("priceMode", reseller.priceMode.name)
                    put("marginValue", reseller.marginValue)
                    put("note", reseller.note)
                    put("isActive", reseller.isActive)
                    put("createdAt", reseller.createdAt)
                }
            )
        }
        val json = array.toString()
        prefs.edit().putString(KEY_RESELLERS, json).apply()
        mariaDbEngine.syncResellersAsync(list, json)
    }

    private fun readSettings(): StoreSettings {
        val defaults = StoreSettings()
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return defaults
        return runCatching {
            val obj = JSONObject(raw)
            val planStr = obj.optString("subscriptionPlan", SubscriptionPlan.FREE.name)
            val plan = runCatching { SubscriptionPlan.valueOf(planStr) }
                .getOrDefault(SubscriptionPlan.FREE)

            val teamArr = obj.optJSONArray("teamMembers")
            val parsedTeam = if (teamArr != null && teamArr.length() > 0) {
                buildList {
                    for (i in 0 until teamArr.length()) {
                        val mObj = teamArr.getJSONObject(i)
                        val roleStr = mObj.optString("role", TeamRole.VIEWER.name)
                        val role = runCatching { TeamRole.valueOf(roleStr) }
                            .getOrDefault(TeamRole.VIEWER)
                        add(
                            TeamMember(
                                id = mObj.optString("id"),
                                name = mObj.optString("name"),
                                emailOrPhone = mObj.optString("emailOrPhone"),
                                role = role,
                                isOwner = mObj.optBoolean("isOwner", false),
                                statusText = mObj.optString("statusText", "Aktif"),
                                invitedAt = mObj.optLong("invitedAt", System.currentTimeMillis())
                            )
                        )
                    }
                }
            } else {
                defaultStoreTeamMembers()
            }

            StoreSettings(
                businessName = obj.optString("businessName", defaults.businessName),
                businessType = obj.optString("businessType", defaults.businessType),
                logoUri = obj.optString("logoUri", defaults.logoUri),
                phoneWhatsapp = obj.optString("phoneWhatsapp", defaults.phoneWhatsapp),
                city = obj.optString("city", defaults.city),
                businessAddress = obj.optString("businessAddress", defaults.businessAddress),
                instagramUrl = obj.optString("instagramUrl", defaults.instagramUrl),
                websiteUrl = obj.optString("websiteUrl", defaults.websiteUrl),
                facebookUrl = obj.optString("facebookUrl", defaults.facebookUrl),
                currencyCode = obj.optString("currencyCode", defaults.currencyCode),
                currencySymbol = obj.optString("currencySymbol", defaults.currencySymbol),
                catalogLanguage = obj.optString("catalogLanguage", defaults.catalogLanguage),
                customSlug = obj.optString("customSlug", defaults.customSlug),
                vatRatePercent = obj.optInt("vatRatePercent", defaults.vatRatePercent),
                showVatInPrices = obj.optBoolean("showVatInPrices", defaults.showVatInPrices),
                minOrderAmount = obj.optDouble("minOrderAmount", defaults.minOrderAmount),
                shippingFee = obj.optDouble("shippingFee", defaults.shippingFee),
                orderConfirmationTemplate = obj.optString("orderConfirmationTemplate", defaults.orderConfirmationTemplate),
                catalogThemeColorHex = obj.optString("catalogThemeColorHex", defaults.catalogThemeColorHex),
                catalogGridColumns = obj.optInt("catalogGridColumns", defaults.catalogGridColumns).coerceIn(1, 3),
                catalogShowPrices = obj.optBoolean("catalogShowPrices", defaults.catalogShowPrices),
                protectImagesFromSaving = obj.optBoolean("protectImagesFromSaving", defaults.protectImagesFromSaving),
                notificationsEnabled = obj.optBoolean("notificationsEnabled", defaults.notificationsEnabled),
                notifyNewOrder = obj.optBoolean("notifyNewOrder", defaults.notifyNewOrder),
                notifyLowStock = obj.optBoolean("notifyLowStock", defaults.notifyLowStock),
                notifyWeeklySummary = obj.optBoolean("notifyWeeklySummary", defaults.notifyWeeklySummary),
                lowStockThreshold = obj.optInt("lowStockThreshold", defaults.lowStockThreshold).coerceAtLeast(1),
                subscriptionPlan = plan,
                teamMembers = parsedTeam
            )
        }.getOrDefault(defaults)
    }

    private fun writeSettings(settings: StoreSettings) {
        val teamArr = JSONArray()
        settings.teamMembers.forEach { member ->
            teamArr.put(
                JSONObject().apply {
                    put("id", member.id)
                    put("name", member.name)
                    put("emailOrPhone", member.emailOrPhone)
                    put("role", member.role.name)
                    put("isOwner", member.isOwner)
                    put("statusText", member.statusText)
                    put("invitedAt", member.invitedAt)
                }
            )
        }
        val obj = JSONObject().apply {
            put("businessName", settings.businessName)
            put("businessType", settings.businessType)
            put("logoUri", settings.logoUri)
            put("phoneWhatsapp", settings.phoneWhatsapp)
            put("city", settings.city)
            put("businessAddress", settings.businessAddress)
            put("instagramUrl", settings.instagramUrl)
            put("websiteUrl", settings.websiteUrl)
            put("facebookUrl", settings.facebookUrl)
            put("currencyCode", settings.currencyCode)
            put("currencySymbol", settings.currencySymbol)
            put("catalogLanguage", settings.catalogLanguage)
            put("customSlug", settings.customSlug)
            put("vatRatePercent", settings.vatRatePercent)
            put("showVatInPrices", settings.showVatInPrices)
            put("minOrderAmount", settings.minOrderAmount)
            put("shippingFee", settings.shippingFee)
            put("orderConfirmationTemplate", settings.orderConfirmationTemplate)
            put("catalogThemeColorHex", settings.catalogThemeColorHex)
            put("catalogGridColumns", settings.catalogGridColumns)
            put("catalogShowPrices", settings.catalogShowPrices)
            put("protectImagesFromSaving", settings.protectImagesFromSaving)
            put("notificationsEnabled", settings.notificationsEnabled)
            put("notifyNewOrder", settings.notifyNewOrder)
            put("notifyLowStock", settings.notifyLowStock)
            put("notifyWeeklySummary", settings.notifyWeeklySummary)
            put("lowStockThreshold", settings.lowStockThreshold)
            put("subscriptionPlan", settings.subscriptionPlan.name)
            put("teamMembers", teamArr)
        }
        val json = obj.toString()
        prefs.edit().putString(KEY_SETTINGS, json).apply()
        mariaDbEngine.syncSettingsAsync(settings, json)
    }

    override suspend fun inviteTeamMember(name: String, emailOrPhone: String, role: TeamRole) {
        val cleanName = name.trim().ifBlank { "Yeni Ekip Üyesi" }
        val cleanContact = emailOrPhone.trim()
        if (cleanContact.isBlank()) return
        val newMember = TeamMember(
            id = "team-${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            emailOrPhone = cleanContact,
            role = role,
            isOwner = false,
            statusText = "Davet Gönderildi",
            invitedAt = System.currentTimeMillis()
        )
        val currentSettings = _settings.value
        val updatedSettings = currentSettings.copy(
            teamMembers = currentSettings.teamMembers + newMember
        )
        updateSettings(updatedSettings)
    }

    override suspend fun updateTeamMemberRole(memberId: String, role: TeamRole) {
        val currentSettings = _settings.value
        val updatedTeam = currentSettings.teamMembers.map { member ->
            if (member.id == memberId && !member.isOwner) {
                member.copy(role = role)
            } else {
                member
            }
        }
        updateSettings(currentSettings.copy(teamMembers = updatedTeam))
    }

    override suspend fun removeTeamMember(memberId: String) {
        val currentSettings = _settings.value
        val updatedTeam = currentSettings.teamMembers.filterNot { it.id == memberId && !it.isOwner }
        updateSettings(currentSettings.copy(teamMembers = updatedTeam))
    }

    override suspend fun upgradeSubscriptionPlan(plan: SubscriptionPlan) {
        val currentSettings = _settings.value
        updateSettings(currentSettings.copy(subscriptionPlan = plan))
    }

    override suspend fun deleteAccount() {
        prefs.edit().clear().apply()
        mariaDbEngine.clearAllRemoteTablesAsync()
        _authUser.value = null
        _catalogs.value = emptyList()
        _products.value = emptyList()
        _resellers.value = emptyList()
        _orders.value = emptyList()
        _shareEvents.value = emptyList()
        _catalogAnalyticsEvents.value = emptyList()
        _settings.value = StoreSettings()
    }

    private fun defaultClothingVariants(): List<ProductVariantGroup> = listOf(
        ProductVariantGroup("Beden", listOf("S", "M", "L", "XL")),
        ProductVariantGroup("Renk", listOf("Ekru", "Antrasit", "Zeytin Yeşili"))
    )

    private fun defaultHomeVariants(): List<ProductVariantGroup> = listOf(
        ProductVariantGroup("Boyut", listOf("Standart", "Büyük Boy")),
        ProductVariantGroup("Ton", listOf("Doğal Bej", "Toprak", "Mat Beyaz"))
    )

    private fun defaultTechVariants(): List<ProductVariantGroup> = listOf(
        ProductVariantGroup("Renk", listOf("Uzay Grisi", "Gümüş", "Gece Siyahı")),
        ProductVariantGroup("Garanti", listOf("2 Yıl Türkiye Garantili"))
    )

    private fun defaultCareVariants(): List<ProductVariantGroup> = listOf(
        ProductVariantGroup("Hacim", listOf("50 ml", "100 ml")),
        ProductVariantGroup("Cilt Tipi", listOf("Tüm Ciltler", "Hassas Cilt"))
    )

    /**
     * 4 Örnek Katalog ve her birinde 8-12 ürün (toplam 38 ürün) üretir.
     * İndirimli eski fiyatlar, stok rozetleri (Stokta / Az kaldı / Tükendi),
     * alt kategoriler, varyantlar ve çoklu görsel galerileri dahildir.
     */
    private fun buildSampleCatalogsAndProducts(): Pair<List<Catalog>, List<Product>> {
        val now = System.currentTimeMillis()

        // Katalog 1: 10 Ürün
        val cat1Id = "cat-ilkbahar-yaz"
        data class SeedSpec(
            val name: String,
            val price: Double,
            val originalPrice: Double?,
            val subCat: String,
            val stock: StockStatus,
            val count: Int
        )

        val cat1Specs = listOf(
            SeedSpec("Keten Oversize Gömlek", 890.0, 1150.0, "Üst Giyim", StockStatus.IN_STOCK, 34),
            SeedSpec("Yüksek Bel Palazzo Pantolon", 1150.0, null, "Alt Giyim", StockStatus.IN_STOCK, 18),
            SeedSpec("Müslin Plaj Elbisesi", 1290.0, 1590.0, "Elbise", StockStatus.LOW_STOCK, 4),
            SeedSpec("Kruvaze Yaka Blazer Ceket", 1850.0, null, "Dış Giyim", StockStatus.IN_STOCK, 12),
            SeedSpec("Organik Pamuk Basic Tişört", 450.0, 550.0, "Üst Giyim", StockStatus.IN_STOCK, 50),
            SeedSpec("Pileli Midi Etek", 980.0, null, "Alt Giyim", StockStatus.LOW_STOCK, 3),
            SeedSpec("Hasır Dokuma Omuz Çantası", 760.0, 950.0, "Aksesuar", StockStatus.OUT_OF_STOCK, 0),
            SeedSpec("İnce Triko Hırka", 820.0, null, "Üst Giyim", StockStatus.IN_STOCK, 22),
            SeedSpec("Poplin Kuşaklı Elbise", 1420.0, 1750.0, "Elbise", StockStatus.IN_STOCK, 15),
            SeedSpec("Deri Görünümlü Sandalet", 1100.0, null, "Aksesuar", StockStatus.LOW_STOCK, 5)
        )
        val cat1Products = cat1Specs.mapIndexed { idx, spec ->
            val mainImg = "https://picsum.photos/seed/katalog-fashion-${idx + 11}/600/600"
            Product(
                id = "prod-cat1-$idx",
                catalogId = cat1Id,
                name = spec.name,
                description = "2025 Yeni Sezon %100 doğal dokulu tasarım koleksiyonu. Nefes alan kumaş yapısı ve modern kesimiyle gün boyu konfor sunar.",
                price = spec.price,
                originalPrice = spec.originalPrice,
                currency = "₺",
                imageUrl = mainImg,
                galleryUrls = listOf(
                    mainImg,
                    "https://picsum.photos/seed/katalog-fashion-${idx + 11}-detail1/600/600",
                    "https://picsum.photos/seed/katalog-fashion-${idx + 11}-detail2/600/600",
                    "https://picsum.photos/seed/katalog-fashion-${idx + 11}-detail3/600/600"
                ),
                subCategory = spec.subCat,
                stockStatus = spec.stock,
                stockCount = spec.count,
                inStock = spec.stock != StockStatus.OUT_OF_STOCK,
                sku = "VR-SS25-${101 + idx}",
                variants = defaultClothingVariants(),
                sortOrder = idx,
                createdAt = now - idx * 3_600_000L
            )
        }

        // Katalog 2: 9 Ürün
        val cat2Id = "cat-ev-dekorasyon"
        val cat2Specs = listOf(
            SeedSpec("El Yapımı Seramik Vazo Seti", 640.0, 820.0, "Dekoratif Obje", StockStatus.IN_STOCK, 19),
            SeedSpec("Bambu Sunum Tabağı 3'lü", 490.0, null, "Sofra & Mutfak", StockStatus.IN_STOCK, 28),
            SeedSpec("Soyut Dokulu Kırlent Kılıfı", 320.0, 420.0, "Ev Tekstili", StockStatus.LOW_STOCK, 4),
            SeedSpec("Soya Mum & Oda Kokusu Seti", 550.0, null, "Aydınlatma & Koku", StockStatus.IN_STOCK, 35),
            SeedSpec("Makrome Duvar Süsü", 420.0, null, "Dekoratif Obje", StockStatus.IN_STOCK, 11),
            SeedSpec("Minimalist Masa Lambası", 1350.0, 1690.0, "Aydınlatma & Koku", StockStatus.LOW_STOCK, 2),
            SeedSpec("Pamuklu Koltuk Şalı", 690.0, null, "Ev Tekstili", StockStatus.IN_STOCK, 24),
            SeedSpec("Mermer Desenli Tepsi", 780.0, 920.0, "Sofra & Mutfak", StockStatus.OUT_OF_STOCK, 0),
            SeedSpec("Cam Karaf ve Bardak Takımı", 860.0, null, "Sofra & Mutfak", StockStatus.IN_STOCK, 16)
        )
        val cat2Products = cat2Specs.mapIndexed { idx, spec ->
            val mainImg = "https://picsum.photos/seed/katalog-decor-${idx + 31}/600/600"
            Product(
                id = "prod-cat2-$idx",
                catalogId = cat2Id,
                name = spec.name,
                description = "İskandinav stil doğal ev ve yaşam aksesuarları. El işçiliği detaylarla yaşam alanlarınıza sıcaklık katar.",
                price = spec.price,
                originalPrice = spec.originalPrice,
                currency = "₺",
                imageUrl = mainImg,
                galleryUrls = listOf(
                    mainImg,
                    "https://picsum.photos/seed/katalog-decor-${idx + 31}-b/600/600",
                    "https://picsum.photos/seed/katalog-decor-${idx + 31}-c/600/600"
                ),
                subCategory = spec.subCat,
                stockStatus = spec.stock,
                stockCount = spec.count,
                inStock = spec.stock != StockStatus.OUT_OF_STOCK,
                sku = "EV-DK-${201 + idx}",
                variants = defaultHomeVariants(),
                sortOrder = idx,
                createdAt = now - idx * 3_600_000L
            )
        }

        // Katalog 3: 11 Ürün
        val cat3Id = "cat-akilli-aksesuar"
        val cat3Specs = listOf(
            SeedSpec("Kablosuz Hızlı Şarj Standı", 950.0, 1190.0, "Şarj & Güç", StockStatus.IN_STOCK, 42),
            SeedSpec("Gürültü Engelleme Özellikli Kulaklık", 2450.0, 2990.0, "Ses & Müzik", StockStatus.IN_STOCK, 14),
            SeedSpec("Manyetik MagSafe Cüzdan", 480.0, null, "Mobil Aksesuar", StockStatus.LOW_STOCK, 5),
            SeedSpec("Alüminyum Laptop Yükseltici", 790.0, 950.0, "Masaüstü", StockStatus.IN_STOCK, 29),
            SeedSpec("Örgülü Type-C 100W Kablo", 290.0, null, "Şarj & Güç", StockStatus.IN_STOCK, 80),
            SeedSpec("Akıllı Saat Hasır Kordon", 360.0, 450.0, "Mobil Aksesuar", StockStatus.IN_STOCK, 33),
            SeedSpec("Taşınabilir 20.000 mAh Powerbank", 1590.0, null, "Şarj & Güç", StockStatus.LOW_STOCK, 3),
            SeedSpec("Bluetooth Mini Hoparlör", 1120.0, 1390.0, "Ses & Müzik", StockStatus.IN_STOCK, 19),
            SeedSpec("Mekanik Masaüstü Klavye", 2190.0, null, "Masaüstü", StockStatus.OUT_OF_STOCK, 0),
            SeedSpec("Ergonomik Dikey Mouse", 980.0, 1200.0, "Masaüstü", StockStatus.IN_STOCK, 21),
            SeedSpec("Webcam Gizlilik Kapağı 3'lü", 140.0, null, "Mobil Aksesuar", StockStatus.IN_STOCK, 65)
        )
        val cat3Products = cat3Specs.mapIndexed { idx, spec ->
            val mainImg = "https://picsum.photos/seed/katalog-tech-${idx + 51}/600/600"
            Product(
                id = "prod-cat3-$idx",
                catalogId = cat3Id,
                name = spec.name,
                description = "Yeni nesil çalışma masası ve mobil teknoloji aksesuarları. Yüksek malzeme kalitesi ve hızlı performans.",
                price = spec.price,
                originalPrice = spec.originalPrice,
                currency = "₺",
                imageUrl = mainImg,
                galleryUrls = listOf(
                    mainImg,
                    "https://picsum.photos/seed/katalog-tech-${idx + 51}-b/600/600",
                    "https://picsum.photos/seed/katalog-tech-${idx + 51}-c/600/600"
                ),
                subCategory = spec.subCat,
                stockStatus = spec.stock,
                stockCount = spec.count,
                inStock = spec.stock != StockStatus.OUT_OF_STOCK,
                sku = "TK-AK-${301 + idx}",
                variants = defaultTechVariants(),
                sortOrder = idx,
                createdAt = now - idx * 3_600_000L
            )
        }

        // Katalog 4: 8 Ürün
        val cat4Id = "cat-dogal-bakim"
        val cat4Specs = listOf(
            SeedSpec("Hyaluronik Asit Nemlendirici Serum", 590.0, 740.0, "Yüz Bakımı", StockStatus.IN_STOCK, 27),
            SeedSpec("C Vitamini Aydınlatıcı Tonik", 460.0, 580.0, "Yüz Bakımı", StockStatus.IN_STOCK, 31),
            SeedSpec("Organik Argan Saç Bakım Yağı", 520.0, null, "Saç Bakımı", StockStatus.LOW_STOCK, 4),
            SeedSpec("Kil Bazlı Arındırıcı Yüz Maskesi", 380.0, 490.0, "Yüz Bakımı", StockStatus.IN_STOCK, 18),
            SeedSpec("Gül Suyu Özlü Yüz Temizleme Jeli", 340.0, null, "Yüz Bakımı", StockStatus.IN_STOCK, 40),
            SeedSpec("SPF 50+ Mineral Güneş Kremi", 680.0, 850.0, "Güneş & Koruma", StockStatus.OUT_OF_STOCK, 0),
            SeedSpec("Shea Yağlı El & Vücut Losyonu", 310.0, null, "Vücut Bakımı", StockStatus.IN_STOCK, 22),
            SeedSpec("Yeşim Taşı Yüz Masaj Rulosu", 440.0, 560.0, "Aksesuar", StockStatus.LOW_STOCK, 3)
        )
        val cat4Products = cat4Specs.mapIndexed { idx, spec ->
            val mainImg = "https://picsum.photos/seed/katalog-care-${idx + 71}/600/600"
            Product(
                id = "prod-cat4-$idx",
                catalogId = cat4Id,
                name = spec.name,
                description = "Dermatolojik olarak test edilmiş %100 vegan ve doğal içerikli cilt bakım serisi.",
                price = spec.price,
                originalPrice = spec.originalPrice,
                currency = "₺",
                imageUrl = mainImg,
                galleryUrls = listOf(
                    mainImg,
                    "https://picsum.photos/seed/katalog-care-${idx + 71}-b/600/600",
                    "https://picsum.photos/seed/katalog-care-${idx + 71}-c/600/600"
                ),
                subCategory = spec.subCat,
                stockStatus = spec.stock,
                stockCount = spec.count,
                inStock = spec.stock != StockStatus.OUT_OF_STOCK,
                sku = "KZ-BK-${401 + idx}",
                variants = defaultCareVariants(),
                sortOrder = idx,
                createdAt = now - idx * 3_600_000L
            )
        }

        val catalogs = listOf(
            Catalog(
                id = cat1Id,
                title = "2025 İlkbahar / Yaz Koleksiyonu",
                description = "Doğal keten ve pamuklu yeni sezon kadın giyim seçkisi",
                slug = "2025-ilkbahar-yaz",
                coverImageUrl = cat1Products.first().imageUrl,
                tags = listOf("Yeni Sezon", "Çok Satanlar"),
                isActive = true,
                productCount = cat1Products.size,
                viewCount = 1284,
                orderCount = 46,
                createdAt = now - 86_400_000L * 4
            ),
            Catalog(
                id = cat2Id,
                title = "Minimalist Ev & Yaşam Seçkisi",
                description = "El yapımı seramik, ahşap sunum ve dekoratif tekstil ürünleri",
                slug = "minimalist-ev-yasam",
                coverImageUrl = cat2Products.first().imageUrl,
                tags = listOf("İndirimli", "Yeni Sezon"),
                isActive = true,
                productCount = cat2Products.size,
                viewCount = 842,
                orderCount = 29,
                createdAt = now - 86_400_000L * 3
            ),
            Catalog(
                id = cat3Id,
                title = "Akıllı Teknoloji & Masaüstü",
                description = "Toptan ve perakende mobil aksesuar & ofis ekipmanları",
                slug = "akilli-teknoloji-masaustu",
                coverImageUrl = cat3Products.first().imageUrl,
                tags = listOf("Toptan", "Çok Satanlar"),
                isActive = true,
                productCount = cat3Products.size,
                viewCount = 1690,
                orderCount = 63,
                createdAt = now - 86_400_000L * 2
            ),
            Catalog(
                id = cat4Id,
                title = "Organik Cilt & Bakım Serisi",
                description = "Sezon sonu özel indirimli doğal kozmetik ürünleri",
                slug = "organik-cilt-bakim",
                coverImageUrl = cat4Products.first().imageUrl,
                tags = listOf("İndirimli"),
                isActive = false,
                productCount = cat4Products.size,
                viewCount = 530,
                orderCount = 18,
                createdAt = now - 86_400_000L
            )
        )

        val allProducts = cat1Products + cat2Products + cat3Products + cat4Products
        return catalogs to allProducts
    }

    private fun buildSampleOrders(
        products: List<Product>,
        catalogs: List<Catalog>
    ): List<Order> {
        val now = System.currentTimeMillis()
        val cat1 = catalogs.getOrNull(0)
        val cat2 = catalogs.getOrNull(1)
        val cat3 = catalogs.getOrNull(2)

        val p1 = products.find { it.id == "prod-cat1-0" }
        val p2 = products.find { it.id == "prod-cat1-1" }
        val p3 = products.find { it.id == "prod-cat1-3" }
        val p4 = products.find { it.id == "prod-cat2-0" }
        val p5 = products.find { it.id == "prod-cat2-3" }
        val p6 = products.find { it.id == "prod-cat3-0" }
        val p7 = products.find { it.id == "prod-cat3-1" }

        return listOf(
            // 1) YENİ SİPARİŞ
            Order(
                id = "SIP-1048",
                orderNumber = "#SIP-1048",
                catalogId = cat1?.id ?: "cat-ilkbahar-yaz",
                catalogTitle = cat1?.title ?: "2025 İlkbahar / Yaz Koleksiyonu",
                customerName = "Zeynep Kaya",
                customerPhone = "+90 532 481 92 10",
                customerAddress = "Moda Mah. Bahariye Cad. No: 42 D: 8 Kadıköy / İstanbul",
                customerNote = "Hediye paketi yapılmasını rica ederim, öğleden sonra teslim alabilirim.",
                items = listOf(
                    OrderItem(
                        productId = p1?.id ?: "prod-cat1-0",
                        productName = p1?.name ?: "Keten Oversize Gömlek",
                        quantity = 2,
                        unitPrice = p1?.price ?: 890.0,
                        selectedVariantSummary = "Beden: M • Renk: Ekru",
                        imageUrl = p1?.imageUrl ?: "https://picsum.photos/seed/katalog-fashion-11/600/600"
                    ),
                    OrderItem(
                        productId = p2?.id ?: "prod-cat1-1",
                        productName = p2?.name ?: "Yüksek Bel Palazzo Pantolon",
                        quantity = 1,
                        unitPrice = p2?.price ?: 1150.0,
                        selectedVariantSummary = "Beden: S • Renk: Antrasit",
                        imageUrl = p2?.imageUrl ?: "https://picsum.photos/seed/katalog-fashion-12/600/600"
                    )
                ),
                totalAmount = 2930.0,
                currency = "₺",
                status = OrderStatus.NEW,
                timeline = listOf(
                    OrderTimelineEntry(
                        id = "tl-1048-1",
                        status = OrderStatus.NEW,
                        title = "Sipariş oluşturuldu (Yeni)",
                        note = "Müşteri katalog sayfasından oluşturdu • Stoktan otomatik düşüldü",
                        timestamp = now - 1_800_000L
                    )
                ),
                stockDeducted = true,
                createdAt = now - 1_800_000L
            ),
            // 2) ONAYLANDI
            Order(
                id = "SIP-1047",
                orderNumber = "#SIP-1047",
                catalogId = cat3?.id ?: "cat-akilli-aksesuar",
                catalogTitle = cat3?.title ?: "Akıllı Teknoloji & Masaüstü",
                customerName = "Murat Demir",
                customerPhone = "+90 544 319 65 88",
                customerAddress = "Çankaya Mah. Tunalı Hilmi Cad. No: 88/14 Çankaya / Ankara",
                customerNote = "Kurumsal fatura kesilecek, kutu içeriği kontrol edilirse sevinirim.",
                items = listOf(
                    OrderItem(
                        productId = p6?.id ?: "prod-cat3-0",
                        productName = p6?.name ?: "Kablosuz Hızlı Şarj Standı",
                        quantity = 1,
                        unitPrice = p6?.price ?: 950.0,
                        selectedVariantSummary = "Renk: Uzay Grisi • Garanti: 2 Yıl Türkiye Garantili",
                        imageUrl = p6?.imageUrl ?: "https://picsum.photos/seed/katalog-tech-51/600/600"
                    ),
                    OrderItem(
                        productId = p7?.id ?: "prod-cat3-1",
                        productName = p7?.name ?: "Gürültü Engelleme Özellikli Kulaklık",
                        quantity = 1,
                        unitPrice = p7?.price ?: 2450.0,
                        selectedVariantSummary = "Renk: Gece Siyahı",
                        imageUrl = p7?.imageUrl ?: "https://picsum.photos/seed/katalog-tech-52/600/600"
                    )
                ),
                totalAmount = 3400.0,
                currency = "₺",
                status = OrderStatus.APPROVED,
                timeline = listOf(
                    OrderTimelineEntry(
                        id = "tl-1047-1",
                        status = OrderStatus.NEW,
                        title = "Sipariş oluşturuldu (Yeni)",
                        note = "Müşteri katalog sayfasından oluşturdu",
                        timestamp = now - 14_400_000L
                    ),
                    OrderTimelineEntry(
                        id = "tl-1047-2",
                        status = OrderStatus.APPROVED,
                        title = "Durum 'Onaylandı' olarak güncellendi",
                        note = "Ödeme ve stok teyidi yapıldı, hazırlanma sırasına alındı",
                        timestamp = now - 10_800_000L
                    )
                ),
                stockDeducted = true,
                resellerId = "res-1",
                resellerName = "Anadolu Moda & Tekstil Bayi",
                createdAt = now - 14_400_000L
            ),
            // 3) HAZIRLANIYOR
            Order(
                id = "SIP-1046",
                orderNumber = "#SIP-1046",
                catalogId = cat2?.id ?: "cat-ev-dekorasyon",
                catalogTitle = cat2?.title ?: "Minimalist Ev & Yaşam Seçkisi",
                customerName = "Selin Arslan",
                customerPhone = "+90 533 712 40 55",
                customerAddress = "Alsancak Mah. Kıbrıs Şehitleri Cad. No: 119 Konak / İzmir",
                customerNote = "Seramik ürünler için ekstra balonlu patpat sarmanızı rica ederim.",
                items = listOf(
                    OrderItem(
                        productId = p4?.id ?: "prod-cat2-0",
                        productName = p4?.name ?: "El Yapımı Seramik Vazo Seti",
                        quantity = 2,
                        unitPrice = p4?.price ?: 640.0,
                        selectedVariantSummary = "Boyut: Büyük Boy • Ton: Doğal Bej",
                        imageUrl = p4?.imageUrl ?: "https://picsum.photos/seed/katalog-decor-31/600/600"
                    ),
                    OrderItem(
                        productId = p5?.id ?: "prod-cat2-3",
                        productName = p5?.name ?: "Soya Mum & Oda Kokusu Seti",
                        quantity = 1,
                        unitPrice = p5?.price ?: 550.0,
                        selectedVariantSummary = "Ton: Mat Beyaz",
                        imageUrl = p5?.imageUrl ?: "https://picsum.photos/seed/katalog-decor-34/600/600"
                    )
                ),
                totalAmount = 1830.0,
                currency = "₺",
                status = OrderStatus.PREPARING,
                timeline = listOf(
                    OrderTimelineEntry(
                        id = "tl-1046-1",
                        status = OrderStatus.NEW,
                        title = "Sipariş oluşturuldu (Yeni)",
                        note = "Stoktan otomatik düşüldü",
                        timestamp = now - 36_000_000L
                    ),
                    OrderTimelineEntry(
                        id = "tl-1046-2",
                        status = OrderStatus.APPROVED,
                        title = "Durum 'Onaylandı' olarak güncellendi",
                        note = "Sipariş satıcı tarafından onaylandı",
                        timestamp = now - 32_000_000L
                    ),
                    OrderTimelineEntry(
                        id = "tl-1046-3",
                        status = OrderStatus.PREPARING,
                        title = "Durum 'Hazırlanıyor' olarak güncellendi",
                        note = "Özel koruma ambalajı ile paketleniyor",
                        timestamp = now - 25_000_000L
                    )
                ),
                stockDeducted = true,
                createdAt = now - 36_000_000L
            ),
            // 4) KARGODA
            Order(
                id = "SIP-1045",
                orderNumber = "#SIP-1045",
                catalogId = cat1?.id ?: "cat-ilkbahar-yaz",
                catalogTitle = cat1?.title ?: "2025 İlkbahar / Yaz Koleksiyonu",
                customerName = "Burak Yıldırım",
                customerPhone = "+90 505 894 11 23",
                customerAddress = "Nilüferköy Mah. Bademli Cad. No: 15 Nilüfer / Bursa",
                customerNote = "",
                items = listOf(
                    OrderItem(
                        productId = p3?.id ?: "prod-cat1-3",
                        productName = p3?.name ?: "Kruvaze Yaka Blazer Ceket",
                        quantity = 1,
                        unitPrice = p3?.price ?: 1850.0,
                        selectedVariantSummary = "Beden: L • Renk: Zeytin Yeşili",
                        imageUrl = p3?.imageUrl ?: "https://picsum.photos/seed/katalog-fashion-14/600/600"
                    )
                ),
                totalAmount = 1850.0,
                currency = "₺",
                status = OrderStatus.SHIPPED,
                timeline = listOf(
                    OrderTimelineEntry(
                        id = "tl-1045-1",
                        status = OrderStatus.NEW,
                        title = "Sipariş oluşturuldu (Yeni)",
                        note = "Müşteri katalog sayfasından oluşturdu",
                        timestamp = now - 86_400_000L
                    ),
                    OrderTimelineEntry(
                        id = "tl-1045-2",
                        status = OrderStatus.APPROVED,
                        title = "Durum 'Onaylandı' olarak güncellendi",
                        note = "Sipariş onaylandı",
                        timestamp = now - 80_000_000L
                    ),
                    OrderTimelineEntry(
                        id = "tl-1045-3",
                        status = OrderStatus.PREPARING,
                        title = "Durum 'Hazırlanıyor' olarak güncellendi",
                        note = "Paketleme tamamlandı",
                        timestamp = now - 70_000_000L
                    ),
                    OrderTimelineEntry(
                        id = "tl-1045-4",
                        status = OrderStatus.SHIPPED,
                        title = "Durum 'Kargoda' olarak güncellendi",
                        note = "Yurtiçi Kargo ile sevk edildi (Takip: YK-904821)",
                        timestamp = now - 54_000_000L
                    )
                ),
                stockDeducted = true,
                resellerId = "res-1",
                resellerName = "Anadolu Moda & Tekstil Bayi",
                createdAt = now - 86_400_000L
            ),
            // 5) TESLİM EDİLDİ
            Order(
                id = "SIP-1044",
                orderNumber = "#SIP-1044",
                catalogId = cat1?.id ?: "cat-ilkbahar-yaz",
                catalogTitle = cat1?.title ?: "2025 İlkbahar / Yaz Koleksiyonu",
                customerName = "Elif Şahin",
                customerPhone = "+90 535 602 33 19",
                customerAddress = "Lara Mah. Güzeloba Cad. No: 64 Muratpaşa / Antalya",
                customerNote = "Kapı görevlisine bırakılabilir.",
                items = listOf(
                    OrderItem(
                        productId = p1?.id ?: "prod-cat1-0",
                        productName = p1?.name ?: "Keten Oversize Gömlek",
                        quantity = 1,
                        unitPrice = p1?.price ?: 890.0,
                        selectedVariantSummary = "Beden: S • Renk: Ekru",
                        imageUrl = p1?.imageUrl ?: "https://picsum.photos/seed/katalog-fashion-11/600/600"
                    )
                ),
                totalAmount = 890.0,
                currency = "₺",
                status = OrderStatus.DELIVERED,
                timeline = listOf(
                    OrderTimelineEntry(
                        id = "tl-1044-1",
                        status = OrderStatus.NEW,
                        title = "Sipariş oluşturuldu (Yeni)",
                        note = "Müşteri katalog sayfasından oluşturdu",
                        timestamp = now - 172_800_000L
                    ),
                    OrderTimelineEntry(
                        id = "tl-1044-2",
                        status = OrderStatus.SHIPPED,
                        title = "Durum 'Kargoda' olarak güncellendi",
                        note = "Kargoya teslim edildi",
                        timestamp = now - 130_000_000L
                    ),
                    OrderTimelineEntry(
                        id = "tl-1044-3",
                        status = OrderStatus.DELIVERED,
                        title = "Durum 'Teslim edildi' olarak güncellendi",
                        note = "Sipariş müşteriye başarıyla teslim edildi",
                        timestamp = now - 90_000_000L
                    )
                ),
                stockDeducted = true,
                createdAt = now - 172_800_000L
            ),
            // 6) İPTAL
            Order(
                id = "SIP-1043",
                orderNumber = "#SIP-1043",
                catalogId = cat2?.id ?: "cat-ev-dekorasyon",
                catalogTitle = cat2?.title ?: "Minimalist Ev & Yaşam Seçkisi",
                customerName = "Caner Özkan",
                customerPhone = "+90 542 210 77 41",
                customerAddress = "Fenerbahçe Mah. Bağdat Cad. No: 210 Kadıköy / İstanbul",
                customerNote = "Renk değişikliği için iptal talep edildi.",
                items = listOf(
                    OrderItem(
                        productId = p5?.id ?: "prod-cat2-3",
                        productName = p5?.name ?: "Soya Mum & Oda Kokusu Seti",
                        quantity = 2,
                        unitPrice = p5?.price ?: 550.0,
                        selectedVariantSummary = "Ton: Toprak",
                        imageUrl = p5?.imageUrl ?: "https://picsum.photos/seed/katalog-decor-34/600/600"
                    )
                ),
                totalAmount = 1100.0,
                currency = "₺",
                status = OrderStatus.CANCELLED,
                timeline = listOf(
                    OrderTimelineEntry(
                        id = "tl-1043-1",
                        status = OrderStatus.NEW,
                        title = "Sipariş oluşturuldu (Yeni)",
                        note = "Stoktan otomatik düşüldü",
                        timestamp = now - 240_000_000L
                    ),
                    OrderTimelineEntry(
                        id = "tl-1043-2",
                        status = OrderStatus.CANCELLED,
                        title = "Durum 'İptal' olarak güncellendi",
                        note = "Müşteri talebiyle iptal edildi • Ürün stokları geri yüklendi",
                        timestamp = now - 220_000_000L
                    )
                ),
                stockDeducted = false,
                createdAt = now - 240_000_000L
            ),
            // 7) 5 GÜN ÖNCE - TESLİM EDİLDİ
            Order(
                id = "SIP-1042",
                orderNumber = "#SIP-1042",
                catalogId = cat3?.id ?: "cat-akilli-aksesuar",
                catalogTitle = cat3?.title ?: "Akıllı Teknoloji & Masaüstü",
                customerName = "Kerem Aydın",
                customerPhone = "+90 532 908 12 45",
                customerAddress = "Levent Mah. Büyükdere Cad. No: 199 Şişli / İstanbul",
                customerNote = "Ofis resepsiyonuna teslim edilebilir.",
                items = listOf(
                    OrderItem(
                        productId = p7?.id ?: "prod-cat3-1",
                        productName = p7?.name ?: "Gürültü Engelleme Özellikli Kulaklık",
                        quantity = 1,
                        unitPrice = p7?.price ?: 2450.0,
                        selectedVariantSummary = "Renk: Gümüş",
                        imageUrl = p7?.imageUrl ?: "https://picsum.photos/seed/katalog-tech-52/600/600"
                    )
                ),
                totalAmount = 2450.0,
                currency = "₺",
                status = OrderStatus.DELIVERED,
                timeline = listOf(
                    OrderTimelineEntry(
                        id = "tl-1042-1",
                        status = OrderStatus.NEW,
                        title = "Sipariş oluşturuldu (Yeni)",
                        note = "WhatsApp katalog bağlantısından oluşturuldu",
                        timestamp = now - 5L * 86_400_000L
                    ),
                    OrderTimelineEntry(
                        id = "tl-1042-2",
                        status = OrderStatus.DELIVERED,
                        title = "Durum 'Teslim edildi' olarak güncellendi",
                        note = "Müşteriye teslim edildi",
                        timestamp = now - 4L * 86_400_000L
                    )
                ),
                stockDeducted = true,
                resellerId = "res-2",
                resellerName = "Ege Grup Mağazacılık",
                createdAt = now - 5L * 86_400_000L
            ),
            // 8) 11 GÜN ÖNCE - TESLİM EDİLDİ
            Order(
                id = "SIP-1041",
                orderNumber = "#SIP-1041",
                catalogId = cat1?.id ?: "cat-ilkbahar-yaz",
                catalogTitle = cat1?.title ?: "2025 İlkbahar / Yaz Koleksiyonu",
                customerName = "Derya Koçak",
                customerPhone = "+90 536 411 89 02",
                customerAddress = "Çayyolu Mah. Park Cad. No: 32 Çankaya / Ankara",
                customerNote = "",
                items = listOf(
                    OrderItem(
                        productId = p3?.id ?: "prod-cat1-3",
                        productName = p3?.name ?: "Kruvaze Yaka Blazer Ceket",
                        quantity = 1,
                        unitPrice = p3?.price ?: 1850.0,
                        selectedVariantSummary = "Beden: M • Renk: Ekru",
                        imageUrl = p3?.imageUrl ?: "https://picsum.photos/seed/katalog-fashion-14/600/600"
                    ),
                    OrderItem(
                        productId = p2?.id ?: "prod-cat1-1",
                        productName = p2?.name ?: "Yüksek Bel Palazzo Pantolon",
                        quantity = 1,
                        unitPrice = p2?.price ?: 1150.0,
                        selectedVariantSummary = "Beden: M • Renk: Ekru",
                        imageUrl = p2?.imageUrl ?: "https://picsum.photos/seed/katalog-fashion-12/600/600"
                    )
                ),
                totalAmount = 3000.0,
                currency = "₺",
                status = OrderStatus.DELIVERED,
                timeline = listOf(
                    OrderTimelineEntry(
                        id = "tl-1041-1",
                        status = OrderStatus.NEW,
                        title = "Sipariş oluşturuldu (Yeni)",
                        note = "Katalog sayfasından oluşturuldu",
                        timestamp = now - 11L * 86_400_000L
                    ),
                    OrderTimelineEntry(
                        id = "tl-1041-2",
                        status = OrderStatus.DELIVERED,
                        title = "Durum 'Teslim edildi' olarak güncellendi",
                        note = "Teslimat tamamlandı",
                        timestamp = now - 9L * 86_400_000L
                    )
                ),
                stockDeducted = true,
                createdAt = now - 11L * 86_400_000L
            ),
            // 9) 19 GÜN ÖNCE - TESLİM EDİLDİ
            Order(
                id = "SIP-1040",
                orderNumber = "#SIP-1040",
                catalogId = cat3?.id ?: "cat-akilli-aksesuar",
                catalogTitle = cat3?.title ?: "Akıllı Teknoloji & Masaüstü",
                customerName = "Oğuzhan Çelik",
                customerPhone = "+90 541 720 63 14",
                customerAddress = "Bostanlı Mah. Cemal Gürsel Cad. No: 78 Karşıyaka / İzmir",
                customerNote = "",
                items = listOf(
                    OrderItem(
                        productId = p6?.id ?: "prod-cat3-0",
                        productName = p6?.name ?: "Kablosuz Hızlı Şarj Standı",
                        quantity = 2,
                        unitPrice = p6?.price ?: 950.0,
                        selectedVariantSummary = "Renk: Gece Siyahı",
                        imageUrl = p6?.imageUrl ?: "https://picsum.photos/seed/katalog-tech-51/600/600"
                    )
                ),
                totalAmount = 1900.0,
                currency = "₺",
                status = OrderStatus.DELIVERED,
                timeline = listOf(
                    OrderTimelineEntry(
                        id = "tl-1040-1",
                        status = OrderStatus.NEW,
                        title = "Sipariş oluşturuldu (Yeni)",
                        note = "Katalog sayfasından oluşturuldu",
                        timestamp = now - 19L * 86_400_000L
                    ),
                    OrderTimelineEntry(
                        id = "tl-1040-2",
                        status = OrderStatus.DELIVERED,
                        title = "Durum 'Teslim edildi' olarak güncellendi",
                        note = "Teslimat tamamlandı",
                        timestamp = now - 17L * 86_400_000L
                    )
                ),
                stockDeducted = true,
                resellerId = "res-3",
                resellerName = "Kuzey Concept Toptan",
                createdAt = now - 19L * 86_400_000L
            )
        )
    }

    private fun buildSampleResellers(catalogs: List<Catalog>): List<Reseller> {
        val cat1Id = catalogs.getOrNull(0)?.id ?: "cat-ilkbahar-yaz"
        val cat2Id = catalogs.getOrNull(1)?.id ?: "cat-ev-dekorasyon"
        val cat3Id = catalogs.getOrNull(2)?.id ?: "cat-akilli-aksesuar"
        val cat4Id = catalogs.getOrNull(3)?.id ?: "cat-organik-bakim"
        val now = System.currentTimeMillis()
        return listOf(
            Reseller(
                id = "res-1",
                name = "Anadolu Moda & Tekstil Bayi",
                phone = "+90 532 410 88 20",
                shortCode = "bayi-anadolu",
                visibleCatalogIds = listOf(cat1Id, cat3Id),
                priceMode = ResellerPriceMode.RESELLER_ADD_MARGIN_PERCENT,
                marginValue = 20.0,
                note = "İç Anadolu bölge ana bayisi (%20 satış marjı ekli)",
                isActive = true,
                createdAt = now - 25L * 86_400_000L
            ),
            Reseller(
                id = "res-2",
                name = "Ege Grup Mağazacılık",
                phone = "+90 544 612 34 90",
                shortCode = "bayi-ege",
                visibleCatalogIds = listOf(cat1Id, cat2Id, cat4Id),
                priceMode = ResellerPriceMode.DISCOUNT_FOR_RESELLER,
                marginValue = 15.0,
                note = "İzmir & Muğla toptan alım bayisi (%15 bayi iskontolu)",
                isActive = true,
                createdAt = now - 18L * 86_400_000L
            ),
            Reseller(
                id = "res-3",
                name = "Kuzey Concept Toptan",
                phone = "+90 533 891 55 12",
                shortCode = "bayi-kuzey",
                visibleCatalogIds = listOf(cat2Id, cat3Id),
                priceMode = ResellerPriceMode.RESELLER_ADD_MARGIN_FIXED,
                marginValue = 150.0,
                note = "Sabit +150 ₺ bayi kar marjı ile satış yapıyor",
                isActive = true,
                createdAt = now - 10L * 86_400_000L
            )
        )
    }

    /**
     * Son 30 güne yayılmış zengin örnek analitik kayıtları (visits, productViews, addToCart) üretir.
     * - Günlük ziyaretçi çizgi grafiği (Bugün, 7 gün, 30 gün, Özel)
     * - Katalog bazında ziyaretçi çubuk grafiği
     * - En çok görüntülenen ürünler (Top 10) ve En çok sepete eklenen ürünler
     * - Müşteri ilgisi tablosu ve Kaynak dağılımı (WhatsApp, Doğrudan link, Diğer)
     * tümü bu koleksiyonlardan gerçek zamanlı hesaplanır.
     */
    private fun buildSampleAnalyticsEvents(
        catalogs: List<Catalog>,
        products: List<Product>
    ): List<CatalogAnalyticsEvent> {
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L
        val hourMs = 3_600_000L
        val events = mutableListOf<CatalogAnalyticsEvent>()

        val catIds = if (catalogs.isNotEmpty()) {
            catalogs.map { it.id to it.slug }
        } else {
            listOf(
                "cat-ilkbahar-yaz" to "2025-ilkbahar-yaz",
                "cat-ev-dekorasyon" to "minimalist-ev-yasam",
                "cat-akilli-aksesuar" to "akilli-teknoloji-masaustu",
                "cat-dogal-bakim" to "organik-cilt-bakim"
            )
        }

        // Her gün (0 = bugün .. 29 = 29 gün önce) için gerçekçi dalgalı ziyaretçi sayıları
        val dailyVisitCounts = listOf(
            24, 19, 22, 17, 26, 21, 15, // Son 7 gün (toplam 144 ziyaret)
            18, 14, 20, 23, 16, 12, 19,
            25, 21, 17, 15, 22, 18, 14,
            16, 20, 13, 19, 24, 15, 11,
            17, 20
        )

        // Katalog ağırlıkları: Katalog 3 (Teknoloji) ve Katalog 1 (Moda) daha çok ziyaret alır
        val catalogWeights = listOf(0, 0, 0, 1, 1, 2, 2, 2, 2, 3)
        // Kaynak (referrer) dağılımı: ~%60 WhatsApp, ~%27 Doğrudan link, ~%13 Diğer
        val referrerPool = listOf(
            "whatsapp", "whatsapp", "whatsapp", "whatsapp", "whatsapp", "whatsapp",
            "direct", "direct", "direct",
            "other"
        )

        var eventCounter = 1000
        for (dayOffset in 0 until 30) {
            val visitsForDay = dailyVisitCounts.getOrElse(dayOffset) { 16 }
            // Benzersiz ziyaretçi oranı ~%75 olacak şekilde oturum havuzu oluştur
            val uniqueSessionsForDay = ((visitsForDay * 0.76).toInt()).coerceAtLeast(1)

            for (v in 0 until visitsForDay) {
                val sessionIdx = v % uniqueSessionsForDay
                val sessionId = "sess-d${dayOffset}-u${sessionIdx}"
                val (catId, catSlug) = catIds[catalogWeights[(v + dayOffset) % catalogWeights.size] % catIds.size]
                val referrer = referrerPool[(v * 3 + dayOffset) % referrerPool.size]
                val timestamp = now - (dayOffset * dayMs) - ((v % 10) * hourMs) - (v * 60_000L)

                events.add(
                    CatalogAnalyticsEvent(
                        id = "evt-visit-${eventCounter++}",
                        sessionId = sessionId,
                        eventType = "visit",
                        catalogId = catId,
                        shortId = catSlug,
                        referrer = referrer,
                        createdAt = timestamp
                    )
                )
            }
        }

        // En çok görüntülenen ve sepete eklenen ürünler için 15 popüler ürün seçkisi
        data class ProductPopularitySpec(
            val productId: String,
            val fallbackName: String,
            val catalogId: String,
            val viewCount30d: Int,
            val cartAddCount30d: Int
        )

        val popularitySpecs = listOf(
            ProductPopularitySpec("prod-cat1-0", "Keten Oversize Gömlek", "cat-ilkbahar-yaz", 68, 26),
            ProductPopularitySpec("prod-cat3-1", "Gürültü Engelleme Özellikli Kulaklık", "cat-akilli-aksesuar", 61, 22),
            ProductPopularitySpec("prod-cat3-0", "Kablosuz Hızlı Şarj Standı", "cat-akilli-aksesuar", 54, 24),
            ProductPopularitySpec("prod-cat1-3", "Kruvaze Yaka Blazer Ceket", "cat-ilkbahar-yaz", 49, 17),
            ProductPopularitySpec("prod-cat2-0", "El Yapımı Seramik Vazo Seti", "cat-ev-dekorasyon", 44, 19),
            ProductPopularitySpec("prod-cat1-1", "Yüksek Bel Palazzo Pantolon", "cat-ilkbahar-yaz", 41, 15),
            ProductPopularitySpec("prod-cat4-0", "Hyaluronik Asit Nemlendirici Serum", "cat-dogal-bakim", 37, 16),
            ProductPopularitySpec("prod-cat2-3", "Soya Mum & Oda Kokusu Seti", "cat-ev-dekorasyon", 34, 14),
            ProductPopularitySpec("prod-cat1-2", "Müslin Plaj Elbisesi", "cat-ilkbahar-yaz", 31, 11),
            ProductPopularitySpec("prod-cat3-6", "Taşınabilir 20.000 mAh Powerbank", "cat-akilli-aksesuar", 28, 12),
            ProductPopularitySpec("prod-cat4-1", "C Vitamini Aydınlatıcı Tonik", "cat-dogal-bakim", 25, 9),
            ProductPopularitySpec("prod-cat2-5", "Minimalist Masa Lambası", "cat-ev-dekorasyon", 22, 8),
            ProductPopularitySpec("prod-cat3-7", "Bluetooth Mini Hoparlör", "cat-akilli-aksesuar", 19, 7),
            ProductPopularitySpec("prod-cat1-8", "Poplin Kuşaklı Elbise", "cat-ilkbahar-yaz", 16, 6),
            ProductPopularitySpec("prod-cat2-1", "Bambu Sunum Tabağı 3'lü", "cat-ev-dekorasyon", 14, 5)
        )

        popularitySpecs.forEachIndexed { specIdx, spec ->
            val matchedProduct = products.find { it.id == spec.productId }
            val productName = matchedProduct?.name ?: spec.fallbackName
            val catalogId = matchedProduct?.catalogId ?: spec.catalogId

            // Ürün görüntülemeleri (productView) - son 30 güne dengeli dağıt (yakın günlerde daha yoğun)
            for (i in 0 until spec.viewCount30d) {
                val dayOffset = when {
                    i < (spec.viewCount30d * 0.16).toInt().coerceAtLeast(2) -> 0 // Bugün
                    i < (spec.viewCount30d * 0.52).toInt() -> 1 + (i % 6)         // Son 7 gün
                    else -> 7 + ((i + specIdx) % 23)                             // 8..29 gün önce
                }
                val timestamp = now - (dayOffset * dayMs) - ((i % 8) * hourMs) - (specIdx * 120_000L)
                val referrer = referrerPool[(i + specIdx) % referrerPool.size]
                events.add(
                    CatalogAnalyticsEvent(
                        id = "evt-pview-${eventCounter++}",
                        sessionId = "sess-pv-$specIdx-$i",
                        eventType = "productView",
                        catalogId = catalogId,
                        shortId = catalogId,
                        productId = spec.productId,
                        productName = productName,
                        referrer = referrer,
                        createdAt = timestamp
                    )
                )
            }

            // Sepete eklemeler (addToCart)
            for (c in 0 until spec.cartAddCount30d) {
                val dayOffset = when {
                    c < (spec.cartAddCount30d * 0.18).toInt().coerceAtLeast(1) -> 0 // Bugün
                    c < (spec.cartAddCount30d * 0.55).toInt() -> 1 + (c % 6)        // Son 7 gün
                    else -> 7 + ((c + specIdx) % 23)                                // 8..29 gün önce
                }
                val timestamp = now - (dayOffset * dayMs) - ((c % 6) * hourMs) - (specIdx * 90_000L)
                val qty = if (c % 5 == 0) 2 else 1
                events.add(
                    CatalogAnalyticsEvent(
                        id = "evt-cart-${eventCounter++}",
                        sessionId = "sess-cart-$specIdx-$c",
                        eventType = "addToCart",
                        catalogId = catalogId,
                        shortId = catalogId,
                        productId = spec.productId,
                        productName = productName,
                        quantity = qty,
                        referrer = referrerPool[(c + specIdx) % referrerPool.size],
                        createdAt = timestamp
                    )
                )
            }
        }

        return events.sortedByDescending { it.createdAt }
    }

    private fun defaultNotifications(): List<AppNotification> = listOf(
        AppNotification(
            id = "notif-1",
            title = "Yeni Sipariş Geldi! (#SIP-1048)",
            message = "Zeynep Kaya tarafından 2.930 ₺ tutarında yeni sipariş oluşturuldu.",
            timeAgo = "5 dk önce",
            isUnread = true
        ),
        AppNotification(
            id = "notif-2",
            title = "Katalog Ziyaretçi Artışı",
            message = "Akıllı Teknoloji & Masaüstü kataloğunuz bugün 140 tekil ziyaretçi tarafından incelendi.",
            timeAgo = "1 saat önce",
            isUnread = true
        ),
        AppNotification(
            id = "notif-3",
            title = "Paylaşım Linki Aktif",
            message = "Minimalist Ev & Yaşam Seçkisi WhatsApp paylaşım linki 24 kez tıklandı.",
            timeAgo = "Dün",
            isUnread = false
        )
    )

    private fun slugify(input: String): String {
        return input
            .lowercase()
            .replace("ş", "s")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace("ı", "i")
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "katalog-${(100..999).random()}" }
    }

    private fun readShareEvents(): List<ShareEvent> {
        val raw = prefs.getString(KEY_SHARE_EVENTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val catIdsArr = obj.optJSONArray("catalogIds") ?: JSONArray()
                    val catIds = buildList {
                        for (c in 0 until catIdsArr.length()) {
                            val idStr = catIdsArr.optString(c)
                            if (idStr.isNotBlank()) add(idStr)
                        }
                    }
                    add(
                        ShareEvent(
                            id = obj.optString("id"),
                            targetType = obj.optString("targetType", "catalog"),
                            targetId = obj.optString("targetId", ""),
                            targetTitle = obj.optString("targetTitle", ""),
                            channel = obj.optString("channel", "link_copy"),
                            shortLink = obj.optString("shortLink", ""),
                            showPrices = obj.optBoolean("showPrices", true),
                            isPasswordProtected = obj.optBoolean("isPasswordProtected", false),
                            expiryDate = obj.optString("expiryDate", "").takeIf { it.isNotBlank() },
                            isDealerShare = obj.optBoolean("isDealerShare", false),
                            dealerMarginType = obj.optString("dealerMarginType", "").takeIf { it.isNotBlank() },
                            dealerMarginValue = obj.optDouble("dealerMarginValue", 0.0),
                            catalogIds = catIds,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeShareEvents(list: List<ShareEvent>) {
        val array = JSONArray()
        list.forEach { item ->
            val catIdsArr = JSONArray()
            item.catalogIds.forEach { catIdsArr.put(it) }
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("targetType", item.targetType)
                    put("targetId", item.targetId)
                    put("targetTitle", item.targetTitle)
                    put("channel", item.channel)
                    put("shortLink", item.shortLink)
                    put("showPrices", item.showPrices)
                    put("isPasswordProtected", item.isPasswordProtected)
                    if (!item.expiryDate.isNullOrBlank()) put("expiryDate", item.expiryDate)
                    put("isDealerShare", item.isDealerShare)
                    if (!item.dealerMarginType.isNullOrBlank()) put("dealerMarginType", item.dealerMarginType)
                    put("dealerMarginValue", item.dealerMarginValue)
                    put("catalogIds", catIdsArr)
                    put("createdAt", item.createdAt)
                }
            )
        }
        val json = array.toString()
        prefs.edit().putString(KEY_SHARE_EVENTS, json).apply()
        mariaDbEngine.syncShareEventsAsync(list, json)
    }

    private fun readCatalogAnalyticsEvents(): List<CatalogAnalyticsEvent> {
        val raw = prefs.getString(KEY_ANALYTICS_EVENTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        CatalogAnalyticsEvent(
                            id = obj.optString("id"),
                            sessionId = obj.optString("sessionId"),
                            eventType = obj.optString("eventType", "visit"),
                            catalogId = obj.optString("catalogId", ""),
                            shortId = obj.optString("shortId", ""),
                            productId = obj.optString("productId", ""),
                            productName = obj.optString("productName", ""),
                            quantity = obj.optInt("quantity", 1),
                            referrer = obj.optString("referrer", "whatsapp"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeCatalogAnalyticsEvents(list: List<CatalogAnalyticsEvent>) {
        val array = JSONArray()
        list.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("sessionId", item.sessionId)
                    put("eventType", item.eventType)
                    put("catalogId", item.catalogId)
                    put("shortId", item.shortId)
                    put("productId", item.productId)
                    put("productName", item.productName)
                    put("quantity", item.quantity)
                    put("referrer", item.referrer)
                    put("createdAt", item.createdAt)
                }
            )
        }
        val json = array.toString()
        prefs.edit().putString(KEY_ANALYTICS_EVENTS, json).apply()
        mariaDbEngine.syncAnalyticsEventsAsync(list, json)
    }

    companion object {
        private const val PREFS_NAME = "katalog_yap_local_storage"
        private const val KEY_INITIAL_SEEDED = "ky_initial_seeded_v3"
        private const val KEY_RESELLERS_SEEDED = "ky_resellers_seeded_v1"
        private const val KEY_ORDERS_SEEDED = "ky_orders_seeded_v4"
        private const val KEY_ANALYTICS_SEEDED = "ky_analytics_seeded_v1"
        private const val KEY_AUTH_USER = "ky_auth_user_v1"
        private const val KEY_PHONE_CREDENTIALS = "ky_phone_credentials_v1"
        private const val KEY_CATALOGS = "ky_catalogs_v3"
        private const val KEY_PRODUCTS = "ky_products_v3"
        private const val KEY_ORDERS = "ky_orders_v4"
        private const val KEY_RESELLERS = "ky_resellers_v1"
        private const val KEY_SETTINGS = "ky_settings_v1"
        private const val KEY_SHARE_EVENTS = "ky_share_events_v1"
        private const val KEY_ANALYTICS_EVENTS = "ky_analytics_events_v2"

        @Volatile
        private var instance: DataService? = null

        fun getInstance(context: Context): DataService {
            return instance ?: synchronized(this) {
                instance ?: LocalStorageDataService(context).also { instance = it }
            }
        }
    }
}
