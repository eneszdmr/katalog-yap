package com.example.model

import android.content.Context
import com.example.util.ImageCompressionUtil
import com.example.util.ShareExportGenerator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firestore `/catalogs/{catalogId}` döküman şemasına birebir eşlenen veri modeli (DTO).
 *
 * `firestore.rules` içerisindeki `isValidCatalog(data)` kurallarıyla tam uyumludur:
 * - catalogId, ownerId, title, description, slug, shortCode, coverImageUrl,
 *   tags, isActive, productCount, viewCount, orderCount,
 *   businessName, businessPhone, currencySymbol, createdAt, updatedAt
 */
data class FirestoreCatalogDocument(
    @DocumentId
    val documentId: String = "",
    val catalogId: String = "",
    val ownerId: String = "",
    val title: String = "",
    val description: String = "",
    val slug: String = "",
    val shortCode: String = "",
    val coverImageUrl: String = "",
    val tags: List<String> = emptyList(),
    val isActive: Boolean = true,
    val productCount: Int = 0,
    val viewCount: Int = 0,
    val orderCount: Int = 0,
    val businessName: String = "",
    val businessPhone: String = "",
    val currencySymbol: String = "₺",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    /**
     * Firestore dökümanını uygulama içi `Catalog` domain modeline dönüştürür.
     */
    fun toDomainCatalog(): Catalog {
        val resolvedId = catalogId.ifBlank { documentId }.ifBlank { "cat-1" }
        return Catalog(
            id = resolvedId,
            title = title.ifBlank { "Katalog" },
            description = description,
            slug = slug.ifBlank { resolvedId },
            coverImageUrl = coverImageUrl,
            tags = tags,
            isActive = isActive,
            productCount = productCount.coerceAtLeast(0),
            viewCount = viewCount.coerceAtLeast(0),
            orderCount = orderCount.coerceAtLeast(0),
            createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
        )
    }

    /**
     * `firestore.rules` doğrulamasından geçecek tam anahtar kümesine sahip Map üretir.
     */
    fun toFirestorePayloadMap(createdAtValue: Any = createdAt ?: FieldValue.serverTimestamp()): Map<String, Any> {
        val resolvedCatalogId = sanitizeFirestoreDocId(catalogId.ifBlank { documentId })
        val resolvedSlug = slug.trim().ifBlank { resolvedCatalogId }.take(200)
        val resolvedShortCode = shortCode.trim()
            .ifBlank {
                ShareExportGenerator.generateShortPath(resolvedCatalogId, "c")
                    .removePrefix("/c/")
                    .take(64)
                    .ifBlank { resolvedCatalogId.take(16) }
            }
            .take(64)

        return mapOf(
            "catalogId" to resolvedCatalogId,
            "ownerId" to ownerId.trim().take(128),
            "title" to title.trim().ifBlank { "Katalog" }.take(200),
            "description" to description.trim().take(2000),
            "slug" to resolvedSlug,
            "shortCode" to resolvedShortCode,
            "coverImageUrl" to coverImageUrl.take(340_000),
            "tags" to tags.map { it.trim().take(40) }.filter { it.isNotEmpty() }.take(25),
            "isActive" to isActive,
            "productCount" to productCount.coerceAtLeast(0),
            "viewCount" to viewCount.coerceAtLeast(0),
            "orderCount" to orderCount.coerceAtLeast(0),
            "businessName" to businessName.trim().take(200),
            "businessPhone" to businessPhone.trim().take(40),
            "currencySymbol" to currencySymbol.trim().ifBlank { "₺" }.take(16),
            "createdAt" to createdAtValue,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

/**
 * Firestore `/products/{productId}` döküman şemasına birebir eşlenen veri modeli (DTO).
 *
 * `firestore.rules` içerisindeki `isValidProduct(data)` kurallarıyla tam uyumludur:
 * - productId, catalogId, ownerId, name, description, price, originalPrice,
 *   currency, imageUrl, category, subCategory, sku, stockStatus, stockCount,
 *   inStock, sortOrder, createdAt, updatedAt
 */
data class FirestoreProductDocument(
    @DocumentId
    val documentId: String = "",
    val productId: String = "",
    val catalogId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val originalPrice: Double = 0.0,
    val currency: String = "₺",
    val imageUrl: String = "",
    val category: String = "Genel",
    val subCategory: String = "Genel",
    val sku: String = "",
    val stockStatus: String = StockStatus.IN_STOCK.name,
    val stockCount: Int = 25,
    val inStock: Boolean = true,
    val sortOrder: Int = 0,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    /**
     * Firestore dökümanını uygulama içi `Product` domain modeline dönüştürür.
     */
    fun toDomainProduct(): Product {
        val resolvedId = productId.ifBlank { documentId }.ifBlank { "prod-1" }
        val resolvedStatus = runCatching { StockStatus.valueOf(stockStatus) }
            .getOrDefault(StockStatus.IN_STOCK)
        val cleanImage = imageUrl.trim()
        return Product(
            id = resolvedId,
            catalogId = catalogId,
            name = name.ifBlank { "Ürün" },
            description = description,
            price = price.coerceAtLeast(0.0),
            originalPrice = originalPrice.takeIf { it > price },
            currency = currency.ifBlank { "₺" },
            imageUrl = cleanImage,
            galleryUrls = if (cleanImage.isNotBlank()) listOf(cleanImage) else emptyList(),
            category = category.ifBlank { "Genel" },
            subCategory = subCategory.ifBlank { "Genel" },
            stockStatus = resolvedStatus,
            stockCount = stockCount.coerceAtLeast(0),
            inStock = inStock,
            sku = sku,
            sortOrder = sortOrder.coerceAtLeast(0),
            createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
        )
    }

    /**
     * `firestore.rules` doğrulamasından geçecek tam anahtar kümesine sahip Map üretir.
     */
    fun toFirestorePayloadMap(createdAtValue: Any = createdAt ?: FieldValue.serverTimestamp()): Map<String, Any> {
        val resolvedProductId = sanitizeFirestoreDocId(productId.ifBlank { documentId })
        val resolvedCatalogId = sanitizeFirestoreDocId(catalogId)
        val validStatus = runCatching { StockStatus.valueOf(stockStatus).name }
            .getOrDefault(StockStatus.IN_STOCK.name)

        return mapOf(
            "productId" to resolvedProductId,
            "catalogId" to resolvedCatalogId,
            "ownerId" to ownerId.trim().take(128),
            "name" to name.trim().ifBlank { "Ürün" }.take(200),
            "description" to description.trim().take(3000),
            "price" to price.coerceAtLeast(0.0),
            "originalPrice" to originalPrice.coerceAtLeast(0.0),
            "currency" to currency.trim().ifBlank { "₺" }.take(16),
            "imageUrl" to imageUrl.take(340_000),
            "category" to category.trim().take(120),
            "subCategory" to subCategory.trim().take(120),
            "sku" to sku.trim().take(80),
            "stockStatus" to validStatus,
            "stockCount" to stockCount.coerceAtLeast(0),
            "inStock" to inStock,
            "sortOrder" to sortOrder.coerceAtLeast(0),
            "createdAt" to createdAtValue,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

/**
 * Firestore döküman ID'lerini güvenli formata (`[a-zA-Z0-9_-]`, max 128 karakter) dönüştürür.
 */
fun sanitizeFirestoreDocId(rawId: String): String {
    return rawId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "-").take(128).ifBlank { "item-1" }
}

/**
 * Domain `Catalog` nesnesini `FirestoreCatalogDocument` modeline dönüştürür.
 */
fun Catalog.toFirestoreDocument(
    ownerId: String,
    store: StoreSettings = StoreSettings(),
    context: Context? = null,
    createdAtTimestamp: Timestamp? = null,
    updatedAtTimestamp: Timestamp? = null
): FirestoreCatalogDocument {
    val docId = sanitizeFirestoreDocId(id)
    val shortCode = ShareExportGenerator.generateShortPath(id, "c")
        .removePrefix("/c/")
        .take(64)
        .ifBlank { docId.take(16) }
    val normalizedSlug = slug.trim().ifBlank { docId }.take(200)
    val optimizedCover = if (context != null) {
        ImageCompressionUtil.toWebAndDbCompatibleUrl(context, coverImageUrl).take(340_000)
    } else {
        coverImageUrl.trim().take(340_000)
    }

    return FirestoreCatalogDocument(
        documentId = docId,
        catalogId = docId,
        ownerId = ownerId.trim().take(128),
        title = title.trim().ifBlank { "Katalog" }.take(200),
        description = description.trim().take(2000),
        slug = normalizedSlug,
        shortCode = shortCode,
        coverImageUrl = optimizedCover,
        tags = tags.map { it.trim().take(40) }.filter { it.isNotEmpty() }.take(25),
        isActive = isActive,
        productCount = productCount.coerceAtLeast(0),
        viewCount = viewCount.coerceAtLeast(0),
        orderCount = orderCount.coerceAtLeast(0),
        businessName = store.businessName.trim().take(200),
        businessPhone = store.phoneWhatsapp.trim().take(40),
        currencySymbol = store.currencySymbol.trim().ifBlank { "₺" }.take(16),
        createdAt = createdAtTimestamp,
        updatedAt = updatedAtTimestamp
    )
}

/**
 * Domain `Product` nesnesini `FirestoreProductDocument` modeline dönüştürür.
 */
fun Product.toFirestoreDocument(
    ownerId: String,
    context: Context? = null,
    createdAtTimestamp: Timestamp? = null,
    updatedAtTimestamp: Timestamp? = null
): FirestoreProductDocument {
    val docId = sanitizeFirestoreDocId(id)
    val catId = sanitizeFirestoreDocId(catalogId)
    val optimizedImage = if (context != null) {
        ImageCompressionUtil.toWebAndDbCompatibleUrl(context, imageUrl).take(340_000)
    } else {
        imageUrl.trim().take(340_000)
    }

    return FirestoreProductDocument(
        documentId = docId,
        productId = docId,
        catalogId = catId,
        ownerId = ownerId.trim().take(128),
        name = name.trim().ifBlank { "Ürün" }.take(200),
        description = description.trim().take(3000),
        price = price.coerceAtLeast(0.0),
        originalPrice = (originalPrice ?: 0.0).coerceAtLeast(0.0),
        currency = currency.trim().ifBlank { "₺" }.take(16),
        imageUrl = optimizedImage,
        category = category.trim().take(120),
        subCategory = subCategory.trim().ifBlank { "Genel" }.take(120),
        sku = sku.trim().take(80),
        stockStatus = stockStatus.name,
        stockCount = stockCount.coerceAtLeast(0),
        inStock = inStock,
        sortOrder = sortOrder.coerceAtLeast(0),
        createdAt = createdAtTimestamp,
        updatedAt = updatedAtTimestamp
    )
}

/**
 * Firestore `DocumentSnapshot` nesnesini güvenli biçimde `FirestoreCatalogDocument` modeline dönüştürür.
 */
fun DocumentSnapshot.toFirestoreCatalogDocument(): FirestoreCatalogDocument? {
    if (!exists()) return null
    val resolvedCatalogId = getString("catalogId")?.takeIf { it.isNotBlank() } ?: id
    val titleVal = getString("title") ?: return null
    val tagsList = (get("tags") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

    return FirestoreCatalogDocument(
        documentId = id,
        catalogId = resolvedCatalogId,
        ownerId = getString("ownerId").orEmpty(),
        title = titleVal,
        description = getString("description").orEmpty(),
        slug = getString("slug")?.ifBlank { resolvedCatalogId } ?: resolvedCatalogId,
        shortCode = getString("shortCode").orEmpty(),
        coverImageUrl = getString("coverImageUrl").orEmpty(),
        tags = tagsList,
        isActive = getBoolean("isActive") ?: true,
        productCount = (getLong("productCount") ?: 0L).toInt(),
        viewCount = (getLong("viewCount") ?: 0L).toInt(),
        orderCount = (getLong("orderCount") ?: 0L).toInt(),
        businessName = getString("businessName").orEmpty(),
        businessPhone = getString("businessPhone").orEmpty(),
        currencySymbol = getString("currencySymbol")?.ifBlank { "₺" } ?: "₺",
        createdAt = getTimestamp("createdAt"),
        updatedAt = getTimestamp("updatedAt")
    )
}

/**
 * Firestore `DocumentSnapshot` nesnesini güvenli biçimde `FirestoreProductDocument` modeline dönüştürür.
 */
fun DocumentSnapshot.toFirestoreProductDocument(): FirestoreProductDocument? {
    if (!exists()) return null
    val resolvedProductId = getString("productId")?.takeIf { it.isNotBlank() } ?: id
    val resolvedCatalogId = getString("catalogId") ?: return null
    val nameVal = getString("name") ?: return null

    return FirestoreProductDocument(
        documentId = id,
        productId = resolvedProductId,
        catalogId = resolvedCatalogId,
        ownerId = getString("ownerId").orEmpty(),
        name = nameVal,
        description = getString("description").orEmpty(),
        price = getDouble("price") ?: 0.0,
        originalPrice = getDouble("originalPrice") ?: 0.0,
        currency = getString("currency")?.ifBlank { "₺" } ?: "₺",
        imageUrl = getString("imageUrl").orEmpty(),
        category = getString("category").orEmpty(),
        subCategory = getString("subCategory")?.ifBlank { "Genel" } ?: "Genel",
        sku = getString("sku").orEmpty(),
        stockStatus = getString("stockStatus") ?: StockStatus.IN_STOCK.name,
        stockCount = (getLong("stockCount") ?: 25L).toInt(),
        inStock = getBoolean("inStock") ?: true,
        sortOrder = (getLong("sortOrder") ?: 0L).toInt(),
        createdAt = getTimestamp("createdAt"),
        updatedAt = getTimestamp("updatedAt")
    )
}
