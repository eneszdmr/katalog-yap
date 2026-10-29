package com.example.services

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.model.AuthUser
import com.example.model.Catalog
import com.example.model.CatalogAnalyticsEvent
import com.example.model.Order
import com.example.model.PhonePasswordCredential
import com.example.model.Product
import com.example.model.Reseller
import com.example.model.ShareEvent
import com.example.model.StoreSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.sql.Connection

/**
 * MariaDB / MySQL (JDBC) veritabanı senkronizasyon motoru.
 * Tüm bağlantılar ve sorgular `DatabaseManager` singleton bağlantı havuzu (connection pool)
 * üzerinden güvenli ve asenkron (`Dispatchers.IO`) olarak yürütülür.
 *
 * Ana Tablolar: `users`, `businesses`, `catalogues`, `products`, `orders`, `visits`
 * Yardımcı Tablolar: `images`, `resellers`, `phone_credentials`, `share_events`, `kv_snapshots`
 */
class MariaDbStorageEngine(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var schemaInitialized: Boolean = false

    init {
        DatabaseManager.initialize(appContext)
    }

    fun isConfigured(): Boolean = DatabaseManager.isConfigured()

    suspend fun ensureSchema(): Boolean = withContext(Dispatchers.IO) {
        if (!DatabaseManager.isConfigured()) return@withContext false
        if (schemaInitialized) return@withContext true

        runCatching {
            DatabaseManager.withConnection { conn ->
                conn.createStatement().use { stmt ->
                    // 1. users
                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS users (
                            id VARCHAR(128) NOT NULL PRIMARY KEY,
                            full_phone VARCHAR(64) DEFAULT NULL,
                            phone_country_code VARCHAR(16) DEFAULT '+90',
                            phone_number VARCHAR(64) DEFAULT NULL,
                            email VARCHAR(255) DEFAULT NULL,
                            display_name VARCHAR(255) DEFAULT NULL,
                            password_hash VARCHAR(255) DEFAULT NULL,
                            provider VARCHAR(64) NOT NULL DEFAULT 'phone_password',
                            firebase_uid VARCHAR(128) DEFAULT NULL,
                            is_onboarding_completed TINYINT(1) NOT NULL DEFAULT 0,
                            logged_in_at BIGINT NOT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            payload_json LONGTEXT NOT NULL,
                            INDEX idx_users_full_phone (full_phone),
                            INDEX idx_users_email (email),
                            INDEX idx_users_firebase_uid (firebase_uid)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """.trimIndent()
                    )

                    // 2. businesses
                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS businesses (
                            id VARCHAR(64) NOT NULL PRIMARY KEY,
                            owner_user_id VARCHAR(128) DEFAULT NULL,
                            business_name VARCHAR(255) NOT NULL,
                            business_type VARCHAR(128) DEFAULT NULL,
                            logo_uri TEXT DEFAULT NULL,
                            logo_blob LONGBLOB DEFAULT NULL,
                            phone_whatsapp VARCHAR(64) DEFAULT NULL,
                            city VARCHAR(128) DEFAULT NULL,
                            business_address TEXT DEFAULT NULL,
                            instagram_url VARCHAR(255) DEFAULT NULL,
                            website_url VARCHAR(255) DEFAULT NULL,
                            facebook_url VARCHAR(255) DEFAULT NULL,
                            currency_code VARCHAR(16) NOT NULL DEFAULT 'TRY',
                            currency_symbol VARCHAR(16) NOT NULL DEFAULT '₺',
                            catalog_language VARCHAR(16) NOT NULL DEFAULT 'tr',
                            custom_slug VARCHAR(128) DEFAULT NULL,
                            vat_rate_percent INT NOT NULL DEFAULT 20,
                            show_vat_in_prices TINYINT(1) NOT NULL DEFAULT 1,
                            min_order_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                            shipping_fee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                            order_confirmation_template TEXT DEFAULT NULL,
                            catalog_theme_color_hex VARCHAR(32) DEFAULT '#10B981',
                            catalog_grid_columns INT NOT NULL DEFAULT 2,
                            catalog_show_prices TINYINT(1) NOT NULL DEFAULT 1,
                            protect_images_from_saving TINYINT(1) NOT NULL DEFAULT 0,
                            low_stock_threshold INT NOT NULL DEFAULT 5,
                            subscription_plan VARCHAR(64) NOT NULL DEFAULT 'FREE',
                            team_members_json LONGTEXT DEFAULT NULL,
                            updated_at BIGINT NOT NULL,
                            payload_json LONGTEXT NOT NULL,
                            INDEX idx_businesses_slug (custom_slug),
                            INDEX idx_businesses_owner (owner_user_id)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """.trimIndent()
                    )

                    // 3. catalogues
                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS catalogues (
                            id VARCHAR(128) NOT NULL PRIMARY KEY,
                            business_id VARCHAR(64) NOT NULL DEFAULT 'default_store',
                            title VARCHAR(255) NOT NULL,
                            description TEXT DEFAULT NULL,
                            slug VARCHAR(255) NOT NULL,
                            cover_image_url TEXT DEFAULT NULL,
                            tags_json TEXT DEFAULT NULL,
                            is_active TINYINT(1) NOT NULL DEFAULT 1,
                            product_count INT NOT NULL DEFAULT 0,
                            view_count INT NOT NULL DEFAULT 0,
                            order_count INT NOT NULL DEFAULT 0,
                            created_at BIGINT NOT NULL,
                            payload_json LONGTEXT NOT NULL,
                            INDEX idx_catalogues_business (business_id),
                            INDEX idx_catalogues_slug (slug),
                            INDEX idx_catalogues_active (is_active)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """.trimIndent()
                    )

                    // 4. products
                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS products (
                            id VARCHAR(128) NOT NULL PRIMARY KEY,
                            catalog_id VARCHAR(128) NOT NULL,
                            business_id VARCHAR(64) NOT NULL DEFAULT 'default_store',
                            name VARCHAR(255) NOT NULL,
                            sku VARCHAR(128) DEFAULT NULL,
                            description TEXT DEFAULT NULL,
                            price DOUBLE NOT NULL DEFAULT 0,
                            original_price DOUBLE DEFAULT NULL,
                            currency VARCHAR(16) NOT NULL DEFAULT '₺',
                            image_url TEXT DEFAULT NULL,
                            gallery_urls_json LONGTEXT DEFAULT NULL,
                            category VARCHAR(128) DEFAULT NULL,
                            sub_category VARCHAR(128) DEFAULT NULL,
                            tags_json TEXT DEFAULT NULL,
                            track_stock TINYINT(1) NOT NULL DEFAULT 1,
                            stock_status VARCHAR(64) NOT NULL DEFAULT 'IN_STOCK',
                            stock_count INT NOT NULL DEFAULT 0,
                            in_stock TINYINT(1) NOT NULL DEFAULT 1,
                            variants_json LONGTEXT DEFAULT NULL,
                            secret_note TEXT DEFAULT NULL,
                            sort_order INT NOT NULL DEFAULT 0,
                            created_at BIGINT NOT NULL,
                            payload_json LONGTEXT NOT NULL,
                            INDEX idx_products_catalog (catalog_id),
                            INDEX idx_products_sku (sku),
                            INDEX idx_products_stock_status (stock_status),
                            INDEX idx_products_category (category)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """.trimIndent()
                    )

                    // 5. orders
                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS orders (
                            id VARCHAR(128) NOT NULL PRIMARY KEY,
                            order_number VARCHAR(64) NOT NULL,
                            catalog_id VARCHAR(128) NOT NULL,
                            catalog_title VARCHAR(255) DEFAULT NULL,
                            business_id VARCHAR(64) NOT NULL DEFAULT 'default_store',
                            customer_name VARCHAR(255) NOT NULL,
                            customer_phone VARCHAR(64) NOT NULL,
                            customer_address TEXT DEFAULT NULL,
                            customer_note TEXT DEFAULT NULL,
                            items_json LONGTEXT NOT NULL,
                            total_amount DOUBLE NOT NULL DEFAULT 0,
                            currency VARCHAR(16) NOT NULL DEFAULT '₺',
                            status VARCHAR(64) NOT NULL DEFAULT 'NEW',
                            timeline_json LONGTEXT DEFAULT NULL,
                            stock_deducted TINYINT(1) NOT NULL DEFAULT 1,
                            reseller_id VARCHAR(128) DEFAULT NULL,
                            reseller_name VARCHAR(255) DEFAULT NULL,
                            created_at BIGINT NOT NULL,
                            payload_json LONGTEXT NOT NULL,
                            INDEX idx_orders_order_number (order_number),
                            INDEX idx_orders_catalog (catalog_id),
                            INDEX idx_orders_status (status),
                            INDEX idx_orders_reseller (reseller_id),
                            INDEX idx_orders_created_at (created_at)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """.trimIndent()
                    )

                    // 6. visits
                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS visits (
                            id VARCHAR(128) NOT NULL PRIMARY KEY,
                            session_id VARCHAR(128) NOT NULL,
                            event_type VARCHAR(64) NOT NULL DEFAULT 'visit',
                            catalog_id VARCHAR(128) DEFAULT NULL,
                            short_id VARCHAR(128) DEFAULT NULL,
                            product_id VARCHAR(128) DEFAULT NULL,
                            product_name VARCHAR(255) DEFAULT NULL,
                            quantity INT NOT NULL DEFAULT 1,
                            referrer VARCHAR(64) NOT NULL DEFAULT 'whatsapp',
                            created_at BIGINT NOT NULL,
                            payload_json LONGTEXT NOT NULL,
                            INDEX idx_visits_session (session_id),
                            INDEX idx_visits_event_type (event_type),
                            INDEX idx_visits_catalog (catalog_id),
                            INDEX idx_visits_product (product_id),
                            INDEX idx_visits_created_at (created_at)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """.trimIndent()
                    )

                    // 7. images
                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS images (
                            image_key VARCHAR(128) NOT NULL PRIMARY KEY,
                            owner_type VARCHAR(64) NOT NULL,
                            owner_id VARCHAR(128) NOT NULL,
                            source_uri TEXT NOT NULL,
                            mime_type VARCHAR(64) NOT NULL DEFAULT 'image/jpeg',
                            image_base64 LONGTEXT DEFAULT NULL,
                            image_blob LONGBLOB DEFAULT NULL,
                            updated_at BIGINT NOT NULL,
                            INDEX idx_images_owner (owner_type, owner_id)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """.trimIndent()
                    )

                    // 8. resellers
                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS resellers (
                            id VARCHAR(128) NOT NULL PRIMARY KEY,
                            name VARCHAR(255) NOT NULL,
                            phone VARCHAR(64) NOT NULL,
                            short_code VARCHAR(64) NOT NULL,
                            visible_catalog_ids_json TEXT DEFAULT NULL,
                            price_mode VARCHAR(64) NOT NULL,
                            margin_value DOUBLE NOT NULL DEFAULT 0,
                            note TEXT DEFAULT NULL,
                            is_active TINYINT(1) NOT NULL DEFAULT 1,
                            created_at BIGINT NOT NULL,
                            payload_json LONGTEXT NOT NULL,
                            INDEX idx_resellers_short_code (short_code)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """.trimIndent()
                    )

                    // 9. phone_credentials
                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS phone_credentials (
                            full_phone VARCHAR(64) NOT NULL PRIMARY KEY,
                            phone_country_code VARCHAR(16) NOT NULL,
                            phone_number VARCHAR(64) NOT NULL,
                            password_hash VARCHAR(255) NOT NULL,
                            recovery_email VARCHAR(255) DEFAULT NULL,
                            updated_at BIGINT NOT NULL,
                            payload_json LONGTEXT NOT NULL
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """.trimIndent()
                    )

                    // 10. share_events
                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS share_events (
                            id VARCHAR(128) NOT NULL PRIMARY KEY,
                            target_type VARCHAR(64) NOT NULL,
                            target_id VARCHAR(128) DEFAULT NULL,
                            target_title VARCHAR(255) DEFAULT NULL,
                            channel VARCHAR(64) DEFAULT NULL,
                            short_link TEXT DEFAULT NULL,
                            created_at BIGINT NOT NULL,
                            payload_json LONGTEXT NOT NULL
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """.trimIndent()
                    )

                    // 11. kv_snapshots
                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS kv_snapshots (
                            store_key VARCHAR(128) NOT NULL PRIMARY KEY,
                            json_payload LONGTEXT NOT NULL,
                            updated_at BIGINT NOT NULL
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """.trimIndent()
                    )
                }
            }
            schemaInitialized = true
            true
        }.onFailure { err ->
            Log.e(TAG, "MariaDB şema oluşturma hatası: ${err.message}", err)
        }.getOrDefault(false)
    }

    /**
     * Başlangıçta MariaDB'deki tüm snapshot kayıtlarını `DatabaseManager` bağlantı havuzu üzerinden çeker.
     */
    suspend fun fetchAllSnapshots(): Map<String, String> = withContext(Dispatchers.IO) {
        if (!ensureSchema()) return@withContext emptyMap()
        runCatching {
            val rows = DatabaseManager.executeQuery(
                sql = "SELECT store_key, json_payload FROM kv_snapshots"
            ) { rs ->
                val key = rs.getString("store_key").orEmpty()
                val json = rs.getString("json_payload").orEmpty()
                key to json
            }
            rows.filter { (k, v) -> k.isNotBlank() && v.isNotBlank() }.toMap()
        }.onFailure { err ->
            Log.e(TAG, "MariaDB snapshot okuma hatası: ${err.message}", err)
        }.getOrDefault(emptyMap())
    }

    /**
     * MariaDB'de saklanan yerel görselleri (`images`) cihaz önbelleğine geri yükler.
     */
    suspend fun restoreBinaryImagesToLocalCache(): Map<String, String> = withContext(Dispatchers.IO) {
        if (!ensureSchema()) return@withContext emptyMap()
        runCatching {
            val restoredDir = File(appContext.filesDir, "mariadb_images").apply { mkdirs() }
            DatabaseManager.withConnection { conn ->
                conn.prepareStatement(
                    "SELECT image_key, source_uri, image_blob, image_base64 FROM images WHERE image_blob IS NOT NULL OR image_base64 IS NOT NULL"
                ).use { ps ->
                    ps.executeQuery().use { rs ->
                        buildMap {
                            while (rs.next()) {
                                val imageKey = rs.getString("image_key") ?: continue
                                val sourceUri = rs.getString("source_uri") ?: continue
                                val blobBytes = rs.getBytes("image_blob")
                                    ?: rs.getString("image_base64")?.let { b64 ->
                                        runCatching { Base64.decode(b64, Base64.DEFAULT) }.getOrNull()
                                    }
                                if (blobBytes != null && blobBytes.isNotEmpty()) {
                                    val targetFile = File(restoredDir, "$imageKey.jpg")
                                    if (!targetFile.exists() || targetFile.length() == 0L) {
                                        targetFile.writeBytes(blobBytes)
                                    }
                                    put(sourceUri, Uri.fromFile(targetFile).toString())
                                }
                            }
                        }
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun syncCatalogsAsync(catalogs: List<Catalog>, rawJson: String) {
        if (!isConfigured()) return
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                DatabaseManager.withTransaction { conn ->
                    upsertKvSnapshot(conn, SNAPSHOT_CATALOGS, rawJson)
                    val keepIds = catalogs.map { it.id }.toSet()
                    deleteMissingIds(conn, "catalogues", "id", keepIds)

                    val sql = """
                        REPLACE INTO catalogues (
                            id, business_id, title, description, slug, cover_image_url, tags_json,
                            is_active, product_count, view_count, order_count, created_at, payload_json
                        ) VALUES (?, 'default_store', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                    conn.prepareStatement(sql).use { ps ->
                        catalogs.forEach { cat ->
                            val tagsJson = JSONArray(cat.tags).toString()
                            val itemJson = JSONObject().apply {
                                put("id", cat.id)
                                put("title", cat.title)
                                put("description", cat.description)
                                put("slug", cat.slug)
                                put("coverImageUrl", cat.coverImageUrl)
                                put("tags", JSONArray(cat.tags))
                                put("isActive", cat.isActive)
                                put("productCount", cat.productCount)
                                put("viewCount", cat.viewCount)
                                put("orderCount", cat.orderCount)
                                put("createdAt", cat.createdAt)
                            }.toString()

                            ps.setString(1, cat.id)
                            ps.setString(2, cat.title)
                            ps.setString(3, cat.description)
                            ps.setString(4, cat.slug)
                            ps.setString(5, cat.coverImageUrl)
                            ps.setString(6, tagsJson)
                            ps.setInt(7, if (cat.isActive) 1 else 0)
                            ps.setInt(8, cat.productCount)
                            ps.setInt(9, cat.viewCount)
                            ps.setInt(10, cat.orderCount)
                            ps.setLong(11, cat.createdAt)
                            ps.setString(12, itemJson)
                            ps.addBatch()

                            if (cat.coverImageUrl.isNotBlank()) {
                                upsertImageRecord(
                                    conn = conn,
                                    ownerType = "catalog",
                                    ownerId = cat.id,
                                    sourceUri = cat.coverImageUrl
                                )
                            }
                        }
                        ps.executeBatch()
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB catalogues senkronizasyon hatası: ${err.message}", err)
            }
        }
    }

    fun syncProductsAsync(products: List<Product>, rawJson: String) {
        if (!isConfigured()) return
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                DatabaseManager.withTransaction { conn ->
                    upsertKvSnapshot(conn, SNAPSHOT_PRODUCTS, rawJson)
                    val keepIds = products.map { it.id }.toSet()
                    deleteMissingIds(conn, "products", "id", keepIds)

                    val sql = """
                        REPLACE INTO products (
                            id, catalog_id, business_id, name, sku, description, price, original_price,
                            currency, image_url, gallery_urls_json, category, sub_category, tags_json,
                            track_stock, stock_status, stock_count, in_stock, variants_json,
                            secret_note, sort_order, created_at, payload_json
                        ) VALUES (?, ?, 'default_store', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                    conn.prepareStatement(sql).use { ps ->
                        products.forEach { prod ->
                            val galleryJson = JSONArray(prod.galleryUrls).toString()
                            val tagsJson = JSONArray(prod.tags).toString()
                            val variantsJson = JSONArray().apply {
                                prod.variants.forEach { vg ->
                                    put(
                                        JSONObject().apply {
                                            put("name", vg.name)
                                            put("options", JSONArray(vg.options))
                                        }
                                    )
                                }
                            }.toString()
                            val itemJson = JSONObject().apply {
                                put("id", prod.id)
                                put("catalogId", prod.catalogId)
                                put("name", prod.name)
                                put("sku", prod.sku)
                                put("price", prod.price)
                                put("currency", prod.currency)
                                put("imageUrl", prod.imageUrl)
                                put("stockCount", prod.stockCount)
                                put("stockStatus", prod.stockStatus.name)
                            }.toString()

                            ps.setString(1, prod.id)
                            ps.setString(2, prod.catalogId)
                            ps.setString(3, prod.name)
                            ps.setString(4, prod.sku)
                            ps.setString(5, prod.description)
                            ps.setDouble(6, prod.price)
                            if (prod.originalPrice != null) {
                                ps.setDouble(7, prod.originalPrice)
                            } else {
                                ps.setNull(7, java.sql.Types.DOUBLE)
                            }
                            ps.setString(8, prod.currency)
                            ps.setString(9, prod.imageUrl)
                            ps.setString(10, galleryJson)
                            ps.setString(11, prod.category)
                            ps.setString(12, prod.subCategory)
                            ps.setString(13, tagsJson)
                            ps.setInt(14, if (prod.trackStock) 1 else 0)
                            ps.setString(15, prod.stockStatus.name)
                            ps.setInt(16, prod.stockCount)
                            ps.setInt(17, if (prod.inStock) 1 else 0)
                            ps.setString(18, variantsJson)
                            ps.setString(19, prod.secretNote)
                            ps.setInt(20, prod.sortOrder)
                            ps.setLong(21, prod.createdAt)
                            ps.setString(22, itemJson)
                            ps.addBatch()

                            val allImageUris = (listOf(prod.imageUrl) + prod.galleryUrls)
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .distinct()
                            allImageUris.forEach { imgUri ->
                                upsertImageRecord(
                                    conn = conn,
                                    ownerType = "product",
                                    ownerId = prod.id,
                                    sourceUri = imgUri
                                )
                            }
                        }
                        ps.executeBatch()
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB products/images senkronizasyon hatası: ${err.message}", err)
            }
        }
    }

    fun syncOrdersAsync(orders: List<Order>, rawJson: String) {
        if (!isConfigured()) return
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                DatabaseManager.withTransaction { conn ->
                    upsertKvSnapshot(conn, SNAPSHOT_ORDERS, rawJson)
                    val keepIds = orders.map { it.id }.toSet()
                    deleteMissingIds(conn, "orders", "id", keepIds)

                    val sql = """
                        REPLACE INTO orders (
                            id, order_number, catalog_id, catalog_title, business_id,
                            customer_name, customer_phone, customer_address, customer_note,
                            items_json, total_amount, currency, status, timeline_json,
                            stock_deducted, reseller_id, reseller_name, created_at, payload_json
                        ) VALUES (?, ?, ?, ?, 'default_store', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                    conn.prepareStatement(sql).use { ps ->
                        orders.forEach { order ->
                            val itemsArr = JSONArray().apply {
                                order.items.forEach { item ->
                                    put(
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
                            }
                            val timelineArr = JSONArray().apply {
                                order.timeline.forEach { entry ->
                                    put(
                                        JSONObject().apply {
                                            put("id", entry.id)
                                            put("status", entry.status.name)
                                            put("title", entry.title)
                                            put("note", entry.note)
                                            put("timestamp", entry.timestamp)
                                        }
                                    )
                                }
                            }
                            val payload = JSONObject().apply {
                                put("id", order.id)
                                put("orderNumber", order.orderNumber)
                                put("catalogId", order.catalogId)
                                put("customerName", order.customerName)
                                put("customerPhone", order.customerPhone)
                                put("totalAmount", order.totalAmount)
                                put("status", order.status.name)
                                put("createdAt", order.createdAt)
                            }.toString()

                            ps.setString(1, order.id)
                            ps.setString(2, order.orderNumber)
                            ps.setString(3, order.catalogId)
                            ps.setString(4, order.catalogTitle)
                            ps.setString(5, order.customerName)
                            ps.setString(6, order.customerPhone)
                            ps.setString(7, order.customerAddress)
                            ps.setString(8, order.customerNote)
                            ps.setString(9, itemsArr.toString())
                            ps.setDouble(10, order.totalAmount)
                            ps.setString(11, order.currency)
                            ps.setString(12, order.status.name)
                            ps.setString(13, timelineArr.toString())
                            ps.setInt(14, if (order.stockDeducted) 1 else 0)
                            ps.setString(15, order.resellerId)
                            ps.setString(16, order.resellerName)
                            ps.setLong(17, order.createdAt)
                            ps.setString(18, payload)
                            ps.addBatch()
                        }
                        ps.executeBatch()
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB orders senkronizasyon hatası: ${err.message}", err)
            }
        }
    }

    fun syncResellersAsync(resellers: List<Reseller>, rawJson: String) {
        if (!isConfigured()) return
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                DatabaseManager.withTransaction { conn ->
                    upsertKvSnapshot(conn, SNAPSHOT_RESELLERS, rawJson)
                    val keepIds = resellers.map { it.id }.toSet()
                    deleteMissingIds(conn, "resellers", "id", keepIds)

                    val sql = """
                        REPLACE INTO resellers (
                            id, name, phone, short_code, visible_catalog_ids_json,
                            price_mode, margin_value, note, is_active, created_at, payload_json
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                    conn.prepareStatement(sql).use { ps ->
                        resellers.forEach { reseller ->
                            val visibleJson = JSONArray(reseller.visibleCatalogIds).toString()
                            val payload = JSONObject().apply {
                                put("id", reseller.id)
                                put("name", reseller.name)
                                put("phone", reseller.phone)
                                put("shortCode", reseller.shortCode)
                                put("priceMode", reseller.priceMode.name)
                                put("marginValue", reseller.marginValue)
                            }.toString()

                            ps.setString(1, reseller.id)
                            ps.setString(2, reseller.name)
                            ps.setString(3, reseller.phone)
                            ps.setString(4, reseller.shortCode)
                            ps.setString(5, visibleJson)
                            ps.setString(6, reseller.priceMode.name)
                            ps.setDouble(7, reseller.marginValue)
                            ps.setString(8, reseller.note)
                            ps.setInt(9, if (reseller.isActive) 1 else 0)
                            ps.setLong(10, reseller.createdAt)
                            ps.setString(11, payload)
                            ps.addBatch()
                        }
                        ps.executeBatch()
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB resellers senkronizasyon hatası: ${err.message}", err)
            }
        }
    }

    fun syncSettingsAsync(settings: StoreSettings, rawJson: String) {
        if (!isConfigured()) return
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                DatabaseManager.withTransaction { conn ->
                    upsertKvSnapshot(conn, SNAPSHOT_SETTINGS, rawJson)
                    val now = System.currentTimeMillis()
                    val teamJson = JSONArray().apply {
                        settings.teamMembers.forEach { m ->
                            put(
                                JSONObject().apply {
                                    put("id", m.id)
                                    put("name", m.name)
                                    put("emailOrPhone", m.emailOrPhone)
                                    put("role", m.role.name)
                                    put("isOwner", m.isOwner)
                                }
                            )
                        }
                    }.toString()
                    val logoBytes = if (settings.logoUri.isNotBlank()) {
                        readLocalImageBytesIfAvailable(settings.logoUri)
                    } else {
                        null
                    }

                    val sql = """
                        REPLACE INTO businesses (
                            id, business_name, business_type, logo_uri, logo_blob,
                            phone_whatsapp, city, business_address, instagram_url,
                            website_url, facebook_url, currency_code, currency_symbol,
                            catalog_language, custom_slug, vat_rate_percent, show_vat_in_prices,
                            min_order_amount, shipping_fee, order_confirmation_template,
                            catalog_theme_color_hex, catalog_grid_columns, catalog_show_prices,
                            protect_images_from_saving, low_stock_threshold, subscription_plan,
                            team_members_json, updated_at, payload_json
                        ) VALUES (
                            'default_store', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                        )
                    """.trimIndent()
                    conn.prepareStatement(sql).use { ps ->
                        ps.setString(1, settings.businessName)
                        ps.setString(2, settings.businessType)
                        ps.setString(3, settings.logoUri)
                        if (logoBytes != null) {
                            ps.setBytes(4, logoBytes)
                        } else {
                            ps.setNull(4, java.sql.Types.BLOB)
                        }
                        ps.setString(5, settings.phoneWhatsapp)
                        ps.setString(6, settings.city)
                        ps.setString(7, settings.businessAddress)
                        ps.setString(8, settings.instagramUrl)
                        ps.setString(9, settings.websiteUrl)
                        ps.setString(10, settings.facebookUrl)
                        ps.setString(11, settings.currencyCode)
                        ps.setString(12, settings.currencySymbol)
                        ps.setString(13, settings.catalogLanguage)
                        ps.setString(14, settings.customSlug)
                        ps.setInt(15, settings.vatRatePercent)
                        ps.setInt(16, if (settings.showVatInPrices) 1 else 0)
                        ps.setDouble(17, settings.minOrderAmount)
                        ps.setDouble(18, settings.shippingFee)
                        ps.setString(19, settings.orderConfirmationTemplate)
                        ps.setString(20, settings.catalogThemeColorHex)
                        ps.setInt(21, settings.catalogGridColumns)
                        ps.setInt(22, if (settings.catalogShowPrices) 1 else 0)
                        ps.setInt(23, if (settings.protectImagesFromSaving) 1 else 0)
                        ps.setInt(24, settings.lowStockThreshold)
                        ps.setString(25, settings.subscriptionPlan.name)
                        ps.setString(26, teamJson)
                        ps.setLong(27, now)
                        ps.setString(28, rawJson)
                        ps.executeUpdate()
                    }
                    if (settings.logoUri.isNotBlank()) {
                        upsertImageRecord(
                            conn = conn,
                            ownerType = "store_logo",
                            ownerId = "default_store",
                            sourceUri = settings.logoUri
                        )
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB businesses senkronizasyon hatası: ${err.message}", err)
            }
        }
    }

    fun syncAuthUserAsync(user: AuthUser?, rawJson: String?) {
        if (!isConfigured()) return
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                DatabaseManager.withTransaction { conn ->
                    if (user == null || rawJson.isNullOrBlank()) {
                        conn.prepareStatement("DELETE FROM kv_snapshots WHERE store_key = ?").use { ps ->
                            ps.setString(1, SNAPSHOT_AUTH_USER)
                            ps.executeUpdate()
                        }
                        return@withTransaction
                    }
                    upsertKvSnapshot(conn, SNAPSHOT_AUTH_USER, rawJson)
                    val sql = """
                        REPLACE INTO users (
                            id, full_phone, phone_country_code, phone_number,
                            email, display_name, password_hash, provider,
                            firebase_uid, is_onboarding_completed, logged_in_at, payload_json
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                    conn.prepareStatement(sql).use { ps ->
                        ps.setString(1, user.id)
                        ps.setString(2, user.fullPhone)
                        ps.setString(3, user.phoneCountryCode)
                        ps.setString(4, user.phoneNumber)
                        ps.setString(5, user.email)
                        ps.setString(6, user.displayName)
                        ps.setString(7, user.passwordHash)
                        ps.setString(8, user.provider)
                        ps.setString(9, user.firebaseUid)
                        ps.setInt(10, if (user.isOnboardingCompleted) 1 else 0)
                        ps.setLong(11, user.loggedInAt)
                        ps.setString(12, rawJson)
                        ps.executeUpdate()
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB users senkronizasyon hatası: ${err.message}", err)
            }
        }
    }

    fun syncPhoneCredentialsAsync(credentials: List<PhonePasswordCredential>, rawJson: String) {
        if (!isConfigured()) return
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                DatabaseManager.withTransaction { conn ->
                    upsertKvSnapshot(conn, SNAPSHOT_PHONE_CREDENTIALS, rawJson)
                    val sql = """
                        REPLACE INTO phone_credentials (
                            full_phone, phone_country_code, phone_number,
                            password_hash, recovery_email, updated_at, payload_json
                        ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                    conn.prepareStatement(sql).use { ps ->
                        credentials.forEach { cred ->
                            val itemJson = JSONObject().apply {
                                put("fullPhone", cred.fullPhone)
                                put("phoneCountryCode", cred.phoneCountryCode)
                                put("phoneNumber", cred.phoneNumber)
                                put("passwordHash", cred.passwordHash)
                                put("recoveryEmail", cred.recoveryEmail)
                                put("updatedAt", cred.updatedAt)
                            }.toString()

                            ps.setString(1, cred.fullPhone)
                            ps.setString(2, cred.phoneCountryCode)
                            ps.setString(3, cred.phoneNumber)
                            ps.setString(4, cred.passwordHash)
                            ps.setString(5, cred.recoveryEmail)
                            ps.setLong(6, cred.updatedAt)
                            ps.setString(7, itemJson)
                            ps.addBatch()
                        }
                        ps.executeBatch()
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB phone_credentials senkronizasyon hatası: ${err.message}", err)
            }
        }
    }

    fun syncShareEventsAsync(events: List<ShareEvent>, rawJson: String) {
        if (!isConfigured()) return
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                DatabaseManager.withTransaction { conn ->
                    upsertKvSnapshot(conn, SNAPSHOT_SHARE_EVENTS, rawJson)
                    val sql = """
                        REPLACE INTO share_events (
                            id, target_type, target_id, target_title,
                            channel, short_link, created_at, payload_json
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                    conn.prepareStatement(sql).use { ps ->
                        events.take(200).forEach { ev ->
                            val itemJson = JSONObject().apply {
                                put("id", ev.id)
                                put("targetType", ev.targetType)
                                put("targetId", ev.targetId)
                                put("targetTitle", ev.targetTitle)
                                put("channel", ev.channel)
                                put("shortLink", ev.shortLink)
                                put("createdAt", ev.createdAt)
                            }.toString()

                            ps.setString(1, ev.id)
                            ps.setString(2, ev.targetType)
                            ps.setString(3, ev.targetId)
                            ps.setString(4, ev.targetTitle)
                            ps.setString(5, ev.channel)
                            ps.setString(6, ev.shortLink)
                            ps.setLong(7, ev.createdAt)
                            ps.setString(8, itemJson)
                            ps.addBatch()
                        }
                        ps.executeBatch()
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB share_events senkronizasyon hatası: ${err.message}", err)
            }
        }
    }

    fun syncAnalyticsEventsAsync(events: List<CatalogAnalyticsEvent>, rawJson: String) {
        if (!isConfigured()) return
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                DatabaseManager.withTransaction { conn ->
                    upsertKvSnapshot(conn, SNAPSHOT_ANALYTICS_EVENTS, rawJson)
                    val sql = """
                        REPLACE INTO visits (
                            id, session_id, event_type, catalog_id, short_id,
                            product_id, product_name, quantity, referrer, created_at, payload_json
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                    conn.prepareStatement(sql).use { ps ->
                        events.take(500).forEach { ev ->
                            val itemJson = JSONObject().apply {
                                put("id", ev.id)
                                put("sessionId", ev.sessionId)
                                put("eventType", ev.eventType)
                                put("catalogId", ev.catalogId)
                                put("shortId", ev.shortId)
                                put("productId", ev.productId)
                                put("productName", ev.productName)
                                put("quantity", ev.quantity)
                                put("referrer", ev.referrer)
                                put("createdAt", ev.createdAt)
                            }.toString()

                            ps.setString(1, ev.id)
                            ps.setString(2, ev.sessionId)
                            ps.setString(3, ev.eventType)
                            ps.setString(4, ev.catalogId)
                            ps.setString(5, ev.shortId)
                            ps.setString(6, ev.productId)
                            ps.setString(7, ev.productName)
                            ps.setInt(8, ev.quantity)
                            ps.setString(9, ev.referrer)
                            ps.setLong(10, ev.createdAt)
                            ps.setString(11, itemJson)
                            ps.addBatch()
                        }
                        ps.executeBatch()
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB visits senkronizasyon hatası: ${err.message}", err)
            }
        }
    }

    fun clearAllRemoteTablesAsync() {
        if (!isConfigured()) return
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                DatabaseManager.withTransaction { conn ->
                    conn.createStatement().use { stmt ->
                        stmt.executeUpdate("DELETE FROM kv_snapshots")
                        stmt.executeUpdate("DELETE FROM catalogues")
                        stmt.executeUpdate("DELETE FROM products")
                        stmt.executeUpdate("DELETE FROM images")
                        stmt.executeUpdate("DELETE FROM orders")
                        stmt.executeUpdate("DELETE FROM visits")
                        stmt.executeUpdate("DELETE FROM resellers")
                        stmt.executeUpdate("DELETE FROM users")
                        stmt.executeUpdate("DELETE FROM businesses")
                        stmt.executeUpdate("DELETE FROM share_events")
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB tablo temizleme hatası: ${err.message}", err)
            }
        }
    }

    private fun upsertKvSnapshot(conn: Connection, key: String, jsonPayload: String) {
        val sql = """
            REPLACE INTO kv_snapshots (store_key, json_payload, updated_at)
            VALUES (?, ?, ?)
        """.trimIndent()
        conn.prepareStatement(sql).use { ps ->
            ps.setString(1, key)
            ps.setString(2, jsonPayload)
            ps.setLong(3, System.currentTimeMillis())
            ps.executeUpdate()
        }
    }

    private fun deleteMissingIds(conn: Connection, tableName: String, idColumn: String, keepIds: Set<String>) {
        if (keepIds.isEmpty()) {
            conn.createStatement().use { it.executeUpdate("DELETE FROM $tableName") }
            return
        }
        val placeholders = keepIds.joinToString(",") { "?" }
        val sql = "DELETE FROM $tableName WHERE $idColumn NOT IN ($placeholders)"
        conn.prepareStatement(sql).use { ps ->
            keepIds.forEachIndexed { index, id ->
                ps.setString(index + 1, id)
            }
            ps.executeUpdate()
        }
    }

    private fun upsertImageRecord(
        conn: Connection,
        ownerType: String,
        ownerId: String,
        sourceUri: String
    ) {
        runCatching {
            val imageKey = sha256Short("$ownerType:$ownerId:$sourceUri")
            val binaryBytes = readLocalImageBytesIfAvailable(sourceUri)
            val base64Str = binaryBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
            val mimeType = when {
                sourceUri.endsWith(".png", ignoreCase = true) -> "image/png"
                sourceUri.endsWith(".webp", ignoreCase = true) -> "image/webp"
                else -> "image/jpeg"
            }

            val sql = """
                REPLACE INTO images (
                    image_key, owner_type, owner_id, source_uri,
                    mime_type, image_base64, image_blob, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, imageKey)
                ps.setString(2, ownerType)
                ps.setString(3, ownerId)
                ps.setString(4, sourceUri)
                ps.setString(5, mimeType)
                if (base64Str != null) {
                    ps.setString(6, base64Str)
                } else {
                    ps.setNull(6, java.sql.Types.LONGVARCHAR)
                }
                if (binaryBytes != null) {
                    ps.setBytes(7, binaryBytes)
                } else {
                    ps.setNull(7, java.sql.Types.BLOB)
                }
                ps.setLong(8, System.currentTimeMillis())
                ps.executeUpdate()
            }
        }.onFailure { err ->
            Log.w(TAG, "Görsel MariaDB'ye kaydedilemedi ($sourceUri): ${err.message}")
        }
    }

    private fun readLocalImageBytesIfAvailable(uriString: String): ByteArray? {
        return runCatching {
            when {
                uriString.startsWith("content://") || uriString.startsWith("file://") -> {
                    val uri = Uri.parse(uriString)
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes()
                    }
                }
                uriString.startsWith("data:image/") && uriString.contains(";base64,") -> {
                    val base64Part = uriString.substringAfter(";base64,")
                    Base64.decode(base64Part, Base64.DEFAULT)
                }
                else -> null
            }
        }.getOrNull()
    }

    private fun sha256Short(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(40)
    }

    companion object {
        private const val TAG = "MariaDbStorageEngine"

        const val SNAPSHOT_AUTH_USER = "ky_auth_user_v1"
        const val SNAPSHOT_PHONE_CREDENTIALS = "ky_phone_credentials_v1"
        const val SNAPSHOT_CATALOGS = "ky_catalogs_v3"
        const val SNAPSHOT_PRODUCTS = "ky_products_v3"
        const val SNAPSHOT_ORDERS = "ky_orders_v4"
        const val SNAPSHOT_RESELLERS = "ky_resellers_v1"
        const val SNAPSHOT_SETTINGS = "ky_settings_v1"
        const val SNAPSHOT_SHARE_EVENTS = "ky_share_events_v1"
        const val SNAPSHOT_ANALYTICS_EVENTS = "ky_analytics_events_v2"
    }
}
