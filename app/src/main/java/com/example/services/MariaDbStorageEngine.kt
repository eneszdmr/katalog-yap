package com.example.services

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.model.AuthUser
import com.example.model.Catalog
import com.example.model.CatalogAnalyticsEvent
import com.example.model.Order
import com.example.model.OrderItem
import com.example.model.OrderStatus
import com.example.model.OrderTimelineEntry
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
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
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

    @Volatile
    var activeBusinessId: String = "default_store"

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
                            logo_uri LONGTEXT DEFAULT NULL,
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
                            cover_image_url LONGTEXT DEFAULT NULL,
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
                            image_url LONGTEXT DEFAULT NULL,
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

                    // 12. catalog_live_sessions (constantinopolishotel.store canlı ziyaretçi sayacı)
                    stmt.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS catalog_live_sessions (
                            catalog_id VARCHAR(128) NOT NULL,
                            session_id VARCHAR(128) NOT NULL,
                            last_seen_at BIGINT NOT NULL,
                            PRIMARY KEY (catalog_id, session_id),
                            KEY idx_catalog_live_seen (catalog_id, last_seen_at)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """.trimIndent()
                    )

                    // 13. Askıda kalmış (Sleep > 90sn) eski oturumları temizle ve görsel sütunlarının LONGTEXT olduğundan emin ol
                    runCatching {
                        val zombieIds = mutableListOf<Long>()
                        stmt.executeQuery("SHOW FULL PROCESSLIST").use { rs ->
                            while (rs.next()) {
                                val cmd = rs.getString("Command").orEmpty()
                                val timeSec = rs.getInt("Time")
                                val pid = rs.getLong("Id")
                                if (cmd.equals("Sleep", ignoreCase = true) && timeSec > 90) {
                                    zombieIds.add(pid)
                                }
                            }
                        }
                        zombieIds.forEach { pid ->
                            runCatching { stmt.execute("KILL $pid") }
                        }
                    }
                    runCatching {
                        stmt.executeUpdate("ALTER TABLE catalogues MODIFY COLUMN cover_image_url LONGTEXT DEFAULT NULL")
                    }
                    runCatching {
                        stmt.executeUpdate("ALTER TABLE products MODIFY COLUMN image_url LONGTEXT DEFAULT NULL")
                    }
                    runCatching {
                        stmt.executeUpdate("ALTER TABLE businesses MODIFY COLUMN logo_uri LONGTEXT DEFAULT NULL")
                    }
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

    /**
     * Tek bir kataloğu oluşturulduğu veya güncellendiği anda hızla MariaDB `catalogues` tablosuna yazar.
     */
    fun upsertSingleCatalogAsync(cat: Catalog, businessId: String = activeBusinessId) {
        if (!isConfigured()) return
        val resolvedBizId = businessId.ifBlank { "default_store" }
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                val webCoverUrl = toWebCompatibleImageUrl(cat.coverImageUrl)
                val tagsJson = JSONArray(cat.tags).toString()
                val itemJson = JSONObject().apply {
                    put("id", cat.id)
                    put("businessId", resolvedBizId)
                    put("title", cat.title)
                    put("description", cat.description)
                    put("slug", cat.slug)
                    put("tags", JSONArray(cat.tags))
                    put("isActive", cat.isActive)
                    put("productCount", cat.productCount)
                    put("viewCount", cat.viewCount)
                    put("orderCount", cat.orderCount)
                    put("createdAt", cat.createdAt)
                }.toString()

                val sql = """
                    REPLACE INTO catalogues (
                        id, business_id, title, description, slug, cover_image_url, tags_json,
                        is_active, product_count, view_count, order_count, created_at, payload_json
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                DatabaseManager.withConnection { conn ->
                    conn.prepareStatement(sql).use { ps ->
                        ps.setString(1, cat.id)
                        ps.setString(2, resolvedBizId)
                        ps.setString(3, cat.title)
                        ps.setString(4, cat.description)
                        ps.setString(5, cat.slug)
                        ps.setString(6, webCoverUrl)
                        ps.setString(7, tagsJson)
                        ps.setInt(8, if (cat.isActive) 1 else 0)
                        ps.setInt(9, cat.productCount)
                        ps.setInt(10, cat.viewCount)
                        ps.setInt(11, cat.orderCount)
                        ps.setLong(12, cat.createdAt)
                        ps.setString(13, itemJson)
                        ps.executeUpdate()
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB tekil katalog yazma hatası (${cat.id}): ${err.message}", err)
            }
        }
    }

    /**
     * Tek bir ürünü (sıkıştırılmış görseliyle birlikte) eklendiği veya düzenlendiği anda hızla MariaDB `products` tablosuna yazar.
     */
    fun upsertSingleProductAsync(prod: Product, businessId: String = activeBusinessId) {
        if (!isConfigured()) return
        val resolvedBizId = businessId.ifBlank { "default_store" }
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                val webImageUrl = toWebCompatibleImageUrl(prod.imageUrl)
                val webGallery = prod.galleryUrls.take(4).map { toWebCompatibleImageUrl(it) }
                val galleryJson = JSONArray(webGallery).toString()
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
                    put("businessId", resolvedBizId)
                    put("name", prod.name)
                    put("sku", prod.sku)
                    put("price", prod.price)
                    put("currency", prod.currency)
                    put("stockCount", prod.stockCount)
                    put("stockStatus", prod.stockStatus.name)
                }.toString()

                val sql = """
                    REPLACE INTO products (
                        id, catalog_id, business_id, name, sku, description, price, original_price,
                        currency, image_url, gallery_urls_json, category, sub_category, tags_json,
                        track_stock, stock_status, stock_count, in_stock, variants_json,
                        secret_note, sort_order, created_at, payload_json
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                DatabaseManager.withConnection { conn ->
                    conn.prepareStatement(sql).use { ps ->
                        ps.setString(1, prod.id)
                        ps.setString(2, prod.catalogId)
                        ps.setString(3, resolvedBizId)
                        ps.setString(4, prod.name)
                        ps.setString(5, prod.sku)
                        ps.setString(6, prod.description)
                        ps.setDouble(7, prod.price)
                        if (prod.originalPrice != null) {
                            ps.setDouble(8, prod.originalPrice)
                        } else {
                            ps.setNull(8, java.sql.Types.DOUBLE)
                        }
                        ps.setString(9, prod.currency)
                        ps.setString(10, webImageUrl)
                        ps.setString(11, galleryJson)
                        ps.setString(12, prod.category)
                        ps.setString(13, prod.subCategory)
                        ps.setString(14, tagsJson)
                        ps.setInt(15, if (prod.trackStock) 1 else 0)
                        ps.setString(16, prod.stockStatus.name)
                        ps.setInt(17, prod.stockCount)
                        ps.setInt(18, if (prod.inStock) 1 else 0)
                        ps.setString(19, variantsJson)
                        ps.setString(20, prod.secretNote)
                        ps.setInt(21, prod.sortOrder)
                        ps.setLong(22, prod.createdAt)
                        ps.setString(23, itemJson)
                        ps.executeUpdate()
                    }
                    // Kataloğun ürün sayısını da anında güncelle
                    conn.prepareStatement(
                        "UPDATE catalogues SET product_count = (SELECT COUNT(*) FROM products WHERE catalog_id = ?), is_active = 1 WHERE id = ?"
                    ).use { psCount ->
                        psCount.setString(1, prod.catalogId)
                        psCount.setString(2, prod.catalogId)
                        psCount.executeUpdate()
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB tekil ürün yazma hatası (${prod.id}): ${err.message}", err)
            }
        }
    }

    /**
     * Paylaşım ekranı açıldığında veya paylaş butonuna tıklandığında kataloğu, mağaza bilgilerini
     * ve kataloğa ait tüm ürünleri (sıkıştırılmış Base64 görselleriyle) TEK BİR bağlantı üzerinden
     * senkron olarak MariaDB'ye yazar. Böylece `constantinopolishotel.store/c/<slug>` anında açılır.
     */
    suspend fun publishCatalogWithProductsNow(
        catalog: Catalog,
        products: List<Product>,
        settings: StoreSettings,
        businessId: String = activeBusinessId
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        if (!ensureSchema()) return@withContext false
        val resolvedBizId = businessId.ifBlank { "default_store" }

        runCatching {
            val webCoverUrl = toWebCompatibleImageUrl(
                catalog.coverImageUrl.ifBlank { products.firstOrNull()?.imageUrl.orEmpty() }
            )
            val webLogoUrl = toWebCompatibleImageUrl(settings.logoUri)
            val preparedProducts = products.map { prod ->
                val webImg = toWebCompatibleImageUrl(prod.imageUrl)
                val webGal = if (prod.galleryUrls.isNotEmpty()) {
                    prod.galleryUrls.take(3).map { toWebCompatibleImageUrl(it) }
                } else if (webImg.isNotBlank()) {
                    listOf(webImg)
                } else {
                    emptyList()
                }
                Triple(prod, webImg, webGal)
            }

            DatabaseManager.withConnection { conn ->
                // 1) businesses (resolvedBizId) kaydını garanti altına al (index.php mağaza başlığı, logo ve WhatsApp numarası için)
                val now = System.currentTimeMillis()
                val bizSql = """
                    REPLACE INTO businesses (
                        id, business_name, business_type, logo_uri,
                        phone_whatsapp, city, business_address, instagram_url,
                        website_url, facebook_url, currency_code, currency_symbol,
                        catalog_language, custom_slug, vat_rate_percent, show_vat_in_prices,
                        min_order_amount, shipping_fee, order_confirmation_template,
                        catalog_theme_color_hex, catalog_grid_columns, catalog_show_prices,
                        protect_images_from_saving, low_stock_threshold, subscription_plan,
                        team_members_json, updated_at, payload_json
                    ) VALUES (
                        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '[]', ?, '{}'
                    )
                """.trimIndent()
                conn.prepareStatement(bizSql).use { ps ->
                    ps.setString(1, resolvedBizId)
                    ps.setString(2, settings.businessName.ifBlank { "Mağaza" })
                    ps.setString(3, settings.businessType)
                    ps.setString(4, webLogoUrl)
                    ps.setString(5, settings.phoneWhatsapp)
                    ps.setString(6, settings.city)
                    ps.setString(7, settings.businessAddress)
                    ps.setString(8, settings.instagramUrl)
                    ps.setString(9, settings.websiteUrl)
                    ps.setString(10, settings.facebookUrl)
                    ps.setString(11, settings.currencyCode.ifBlank { "TRY" })
                    ps.setString(12, settings.currencySymbol.ifBlank { "₺" })
                    ps.setString(13, settings.catalogLanguage.ifBlank { "tr" })
                    ps.setString(14, settings.customSlug)
                    ps.setInt(15, settings.vatRatePercent)
                    ps.setInt(16, if (settings.showVatInPrices) 1 else 0)
                    ps.setDouble(17, settings.minOrderAmount)
                    ps.setDouble(18, settings.shippingFee)
                    ps.setString(19, settings.orderConfirmationTemplate)
                    ps.setString(20, settings.catalogThemeColorHex.ifBlank { "#10B981" })
                    ps.setInt(21, settings.catalogGridColumns.coerceIn(1, 4))
                    ps.setInt(22, if (settings.catalogShowPrices) 1 else 0)
                    ps.setInt(23, if (settings.protectImagesFromSaving) 1 else 0)
                    ps.setInt(24, settings.lowStockThreshold)
                    ps.setString(25, settings.subscriptionPlan.name)
                    ps.setLong(26, now)
                    ps.executeUpdate()
                }

                // 2) Katalog kaydını `is_active = 1` ve `business_id = resolvedBizId` olarak yaz
                val tagsJson = JSONArray(catalog.tags).toString()
                val catJson = JSONObject().apply {
                    put("id", catalog.id)
                    put("businessId", resolvedBizId)
                    put("title", catalog.title)
                    put("description", catalog.description)
                    put("slug", catalog.slug)
                    put("tags", JSONArray(catalog.tags))
                    put("isActive", true)
                    put("productCount", preparedProducts.size)
                    put("viewCount", catalog.viewCount)
                    put("orderCount", catalog.orderCount)
                    put("createdAt", catalog.createdAt)
                }.toString()

                val catSql = """
                    REPLACE INTO catalogues (
                        id, business_id, title, description, slug, cover_image_url, tags_json,
                        is_active, product_count, view_count, order_count, created_at, payload_json
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?)
                """.trimIndent()
                conn.prepareStatement(catSql).use { ps ->
                    ps.setString(1, catalog.id)
                    ps.setString(2, resolvedBizId)
                    ps.setString(3, catalog.title)
                    ps.setString(4, catalog.description)
                    ps.setString(5, catalog.slug)
                    ps.setString(6, webCoverUrl)
                    ps.setString(7, tagsJson)
                    ps.setInt(8, preparedProducts.size)
                    ps.setInt(9, catalog.viewCount)
                    ps.setInt(10, catalog.orderCount)
                    ps.setLong(11, catalog.createdAt)
                    ps.setString(12, catJson)
                    ps.executeUpdate()
                }

                // 3) Katalogdaki tüm ürünleri yaz
                val prodSql = """
                    REPLACE INTO products (
                        id, catalog_id, business_id, name, sku, description, price, original_price,
                        currency, image_url, gallery_urls_json, category, sub_category, tags_json,
                        track_stock, stock_status, stock_count, in_stock, variants_json,
                        secret_note, sort_order, created_at, payload_json
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                conn.prepareStatement(prodSql).use { ps ->
                    preparedProducts.forEach { (prod, webImageUrl, webGallery) ->
                        val galleryJson = JSONArray(webGallery).toString()
                        val prodTagsJson = JSONArray(prod.tags).toString()
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
                            put("businessId", resolvedBizId)
                            put("name", prod.name)
                            put("sku", prod.sku)
                            put("price", prod.price)
                            put("currency", prod.currency)
                            put("stockCount", prod.stockCount)
                            put("stockStatus", prod.stockStatus.name)
                        }.toString()

                        ps.setString(1, prod.id)
                        ps.setString(2, catalog.id)
                        ps.setString(3, resolvedBizId)
                        ps.setString(4, prod.name)
                        ps.setString(5, prod.sku)
                        ps.setString(6, prod.description)
                        ps.setDouble(7, prod.price)
                        if (prod.originalPrice != null) {
                            ps.setDouble(8, prod.originalPrice)
                        } else {
                            ps.setNull(8, java.sql.Types.DOUBLE)
                        }
                        ps.setString(9, prod.currency)
                        ps.setString(10, webImageUrl)
                        ps.setString(11, galleryJson)
                        ps.setString(12, prod.category)
                        ps.setString(13, prod.subCategory)
                        ps.setString(14, prodTagsJson)
                        ps.setInt(15, if (prod.trackStock) 1 else 0)
                        ps.setString(16, prod.stockStatus.name)
                        ps.setInt(17, prod.stockCount)
                        ps.setInt(18, if (prod.inStock) 1 else 0)
                        ps.setString(19, variantsJson)
                        ps.setString(20, prod.secretNote)
                        ps.setInt(21, prod.sortOrder)
                        ps.setLong(22, prod.createdAt)
                        ps.setString(23, itemJson)
                        ps.executeUpdate()
                    }
                }
            }
            true
        }.onFailure { err ->
            Log.e(TAG, "publishCatalogWithProductsNow hatası (${catalog.id} / ${catalog.slug}): ${err.message}", err)
        }.getOrDefault(false)
    }

    /**
     * Kullanıcı açıkça bir kataloğu sildiğinde yalnızca o katalog ID'lerini ve ürünlerini MariaDB'den siler.
     */
    fun deleteCatalogsByIdsAsync(catalogIds: Set<String>) {
        if (!isConfigured() || catalogIds.isEmpty()) return
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                DatabaseManager.withConnection { conn ->
                    val placeholders = catalogIds.joinToString(",") { "?" }
                    conn.prepareStatement("DELETE FROM products WHERE catalog_id IN ($placeholders)").use { ps ->
                        catalogIds.forEachIndexed { idx, id -> ps.setString(idx + 1, id) }
                        ps.executeUpdate()
                    }
                    conn.prepareStatement("DELETE FROM catalogues WHERE id IN ($placeholders)").use { ps ->
                        catalogIds.forEachIndexed { idx, id -> ps.setString(idx + 1, id) }
                        ps.executeUpdate()
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB katalog silme hatası: ${err.message}", err)
            }
        }
    }

    /**
     * Kullanıcı açıkça ürün sildiğinde yalnızca o ürün ID'lerini MariaDB'den siler.
     */
    fun deleteProductsByIdsAsync(productIds: Set<String>) {
        if (!isConfigured() || productIds.isEmpty()) return
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                DatabaseManager.withConnection { conn ->
                    val placeholders = productIds.joinToString(",") { "?" }
                    conn.prepareStatement("DELETE FROM products WHERE id IN ($placeholders)").use { ps ->
                        productIds.forEachIndexed { idx, id -> ps.setString(idx + 1, id) }
                        ps.executeUpdate()
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB ürün silme hatası: ${err.message}", err)
            }
        }
    }

    /**
     * MariaDB `catalogues` tablosundaki belirli bir mağazaya (`business_id`) ait katalogları okur.
     */
    suspend fun fetchAllRelationalCatalogs(businessId: String = activeBusinessId): List<Catalog> = withContext(Dispatchers.IO) {
        if (!ensureSchema()) return@withContext emptyList()
        val resolvedBizId = businessId.ifBlank { "default_store" }
        runCatching {
            DatabaseManager.executeQuery(
                sql = """
                    SELECT id, title, description, slug, cover_image_url, tags_json,
                           is_active, product_count, view_count, order_count, created_at
                    FROM catalogues
                    WHERE business_id = ?
                    ORDER BY created_at DESC
                """.trimIndent(),
                resolvedBizId
            ) { rs ->
                val tagsArr = runCatching { JSONArray(rs.getString("tags_json").orEmpty()) }.getOrNull()
                val tags = buildList {
                    if (tagsArr != null) {
                        for (i in 0 until tagsArr.length()) {
                            tagsArr.optString(i)?.takeIf { it.isNotBlank() }?.let { add(it) }
                        }
                    }
                }
                val rawCover = rs.getString("cover_image_url").orEmpty()
                val localCover = com.example.util.ImageCompressionUtil.ensureRenderableLocalUri(appContext, rawCover, "cat_cover")
                Catalog(
                    id = rs.getString("id").orEmpty(),
                    title = rs.getString("title").orEmpty(),
                    description = rs.getString("description").orEmpty(),
                    slug = rs.getString("slug").orEmpty(),
                    coverImageUrl = localCover,
                    tags = tags,
                    isActive = rs.getInt("is_active") == 1,
                    productCount = rs.getInt("product_count"),
                    viewCount = rs.getInt("view_count"),
                    orderCount = rs.getInt("order_count"),
                    createdAt = rs.getLong("created_at")
                )
            }.filter { it.id.isNotBlank() }
        }.onFailure { err ->
            Log.w(TAG, "MariaDB ilişkisel catalogues okuma hatası: ${err.message}")
        }.getOrDefault(emptyList())
    }

    /**
     * MariaDB `products` tablosundaki belirli bir mağazaya (`business_id`) ait ürünleri okur.
     */
    suspend fun fetchAllRelationalProducts(businessId: String = activeBusinessId): List<Product> = withContext(Dispatchers.IO) {
        if (!ensureSchema()) return@withContext emptyList()
        val resolvedBizId = businessId.ifBlank { "default_store" }
        runCatching {
            DatabaseManager.executeQuery(
                sql = """
                    SELECT id, catalog_id, name, sku, description, price, original_price,
                           currency, image_url, gallery_urls_json, category, sub_category,
                           tags_json, track_stock, stock_status, stock_count, in_stock,
                           secret_note, sort_order, created_at
                    FROM products
                    WHERE business_id = ?
                    ORDER BY sort_order ASC, created_at DESC
                """.trimIndent(),
                resolvedBizId
            ) { rs ->
                val price = rs.getDouble("price")
                val orig = rs.getDouble("original_price").takeIf { !rs.wasNull() && it > price }
                val rawImg = rs.getString("image_url").orEmpty()
                val img = com.example.util.ImageCompressionUtil.ensureRenderableLocalUri(appContext, rawImg, "prod_img")
                val galArr = runCatching { JSONArray(rs.getString("gallery_urls_json").orEmpty()) }.getOrNull()
                val gallery = buildList {
                    if (galArr != null) {
                        for (i in 0 until galArr.length()) {
                            galArr.optString(i)?.takeIf { it.isNotBlank() }?.let { gUrl ->
                                add(com.example.util.ImageCompressionUtil.ensureRenderableLocalUri(appContext, gUrl, "prod_gal"))
                            }
                        }
                    }
                    if (isEmpty() && img.isNotBlank()) add(img)
                }
                val tagsArr = runCatching { JSONArray(rs.getString("tags_json").orEmpty()) }.getOrNull()
                val tags = buildList {
                    if (tagsArr != null) {
                        for (i in 0 until tagsArr.length()) {
                            tagsArr.optString(i)?.takeIf { it.isNotBlank() }?.let { add(it) }
                        }
                    }
                }
                val statusStr = rs.getString("stock_status").orEmpty()
                val status = runCatching { com.example.model.StockStatus.valueOf(statusStr) }
                    .getOrDefault(com.example.model.StockStatus.IN_STOCK)

                Product(
                    id = rs.getString("id").orEmpty(),
                    catalogId = rs.getString("catalog_id").orEmpty(),
                    name = rs.getString("name").orEmpty(),
                    sku = rs.getString("sku").orEmpty(),
                    description = rs.getString("description").orEmpty(),
                    price = price,
                    originalPrice = orig,
                    currency = rs.getString("currency")?.ifBlank { "₺" } ?: "₺",
                    imageUrl = img,
                    galleryUrls = gallery,
                    category = rs.getString("category").orEmpty(),
                    subCategory = rs.getString("sub_category")?.ifBlank { "Genel" } ?: "Genel",
                    tags = tags,
                    trackStock = rs.getInt("track_stock") == 1,
                    stockStatus = status,
                    stockCount = rs.getInt("stock_count"),
                    inStock = rs.getInt("in_stock") == 1,
                    secretNote = rs.getString("secret_note").orEmpty(),
                    sortOrder = rs.getInt("sort_order"),
                    createdAt = rs.getLong("created_at")
                )
            }.filter { it.id.isNotBlank() && it.catalogId.isNotBlank() }
        }.onFailure { err ->
            Log.w(TAG, "MariaDB ilişkisel products okuma hatası: ${err.message}")
        }.getOrDefault(emptyList())
    }

    /**
     * Telefon numarası veya hesap anahtarı ile giriş yapıldığında, MariaDB `businesses`, `users`
     * ve `phone_credentials` tablolarında o numaraya kayıtlı mağaza ve kullanıcı bilgilerini çeker.
     */
    suspend fun fetchBusinessSettingsByIdOrPhone(
        businessId: String,
        normalizedPhone: String
    ): StoreSettings? = withContext(Dispatchers.IO) {
        if (!isConfigured() || !ensureSchema()) return@withContext null
        runCatching {
            val phoneLike = if (normalizedPhone.length >= 10) "%${normalizedPhone.takeLast(10)}%" else "%$normalizedPhone%"
            DatabaseManager.withConnection { conn ->
                conn.prepareStatement(
                    """
                    SELECT id, business_name, business_type, logo_uri, logo_blob,
                           phone_whatsapp, city, business_address, instagram_url,
                           website_url, facebook_url, currency_code, currency_symbol,
                           catalog_language, custom_slug, vat_rate_percent, show_vat_in_prices,
                           min_order_amount, shipping_fee, order_confirmation_template,
                           catalog_theme_color_hex, catalog_grid_columns, catalog_show_prices,
                           protect_images_from_saving, low_stock_threshold
                    FROM businesses
                    WHERE id = ? OR (phone_whatsapp IS NOT NULL AND phone_whatsapp != '' AND phone_whatsapp LIKE ?)
                    ORDER BY CASE WHEN id = ? THEN 0 ELSE 1 END, updated_at DESC
                    LIMIT 1
                    """.trimIndent()
                ).use { ps ->
                    ps.setString(1, businessId)
                    ps.setString(2, phoneLike)
                    ps.setString(3, businessId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            val bName = rs.getString("business_name").orEmpty().trim()
                            if (bName.isBlank() || bName.equals("Vera Tasarım Butik", ignoreCase = true) && rs.getString("id") == "default_store" && businessId != "default_store") {
                                return@use null
                            }
                            val rawLogoUri = rs.getString("logo_uri").orEmpty()
                            val logoBlob = rs.getBytes("logo_blob")
                            val effectiveLogoInput = when {
                                rawLogoUri.startsWith("data:image/", ignoreCase = true) -> rawLogoUri
                                logoBlob != null && logoBlob.isNotEmpty() -> {
                                    "data:image/jpeg;base64," + Base64.encodeToString(logoBlob, Base64.NO_WRAP)
                                }
                                else -> rawLogoUri
                            }
                            val persistedLogo = com.example.util.ImageCompressionUtil.persistStoreLogo(
                                context = appContext,
                                rawLogoUri = effectiveLogoInput,
                                accountKey = businessId
                            )
                            StoreSettings(
                                businessName = bName,
                                businessType = rs.getString("business_type").orEmpty().ifBlank { "Giyim & Butik" },
                                logoUri = persistedLogo,
                                phoneWhatsapp = rs.getString("phone_whatsapp").orEmpty(),
                                city = rs.getString("city").orEmpty(),
                                businessAddress = rs.getString("business_address").orEmpty(),
                                instagramUrl = rs.getString("instagram_url").orEmpty(),
                                websiteUrl = rs.getString("website_url").orEmpty().ifBlank { "https://constantinopolishotel.store" },
                                facebookUrl = rs.getString("facebook_url").orEmpty(),
                                currencyCode = rs.getString("currency_code").orEmpty().ifBlank { "TRY" },
                                currencySymbol = rs.getString("currency_symbol").orEmpty().ifBlank { "₺" },
                                catalogLanguage = rs.getString("catalog_language").orEmpty().ifBlank { "Türkçe (TR)" },
                                customSlug = rs.getString("custom_slug").orEmpty(),
                                vatRatePercent = rs.getInt("vat_rate_percent").takeIf { !rs.wasNull() } ?: 20,
                                showVatInPrices = rs.getInt("show_vat_in_prices") == 1,
                                minOrderAmount = rs.getDouble("min_order_amount"),
                                shippingFee = rs.getDouble("shipping_fee"),
                                orderConfirmationTemplate = rs.getString("order_confirmation_template").orEmpty().ifBlank {
                                    "Merhaba {musteri_adi}, {siparis_no} numaralı {toplam_tutar} tutarındaki siparişiniz başarıyla alınmıştır."
                                },
                                catalogThemeColorHex = rs.getString("catalog_theme_color_hex").orEmpty().ifBlank { "#2563EB" },
                                catalogGridColumns = rs.getInt("catalog_grid_columns").coerceIn(1, 3),
                                catalogShowPrices = rs.getInt("catalog_show_prices") == 1,
                                protectImagesFromSaving = rs.getInt("protect_images_from_saving") == 1,
                                lowStockThreshold = rs.getInt("low_stock_threshold").coerceAtLeast(1)
                            )
                        } else null
                    }
                }
            }
        }.onFailure { err ->
            Log.w(TAG, "MariaDB fetchBusinessSettingsByIdOrPhone hatası: ${err.message}")
        }.getOrNull()
    }

    /**
     * MariaDB `phone_credentials` tablosundan telefon numarasına ait şifre bilgisini çeker.
     */
    suspend fun fetchPhoneCredentialFromDb(normalizedPhone: String): PhonePasswordCredential? = withContext(Dispatchers.IO) {
        if (!isConfigured() || !ensureSchema() || normalizedPhone.isBlank()) return@withContext null
        runCatching {
            val phoneLike = "%${normalizedPhone.takeLast(10)}%"
            DatabaseManager.withConnection { conn ->
                conn.prepareStatement(
                    """
                    SELECT full_phone, phone_country_code, phone_number, password_hash, recovery_email, updated_at
                    FROM phone_credentials
                    WHERE full_phone = ? OR phone_number = ? OR full_phone LIKE ?
                    ORDER BY updated_at DESC
                    LIMIT 1
                    """.trimIndent()
                ).use { ps ->
                    ps.setString(1, normalizedPhone)
                    ps.setString(2, normalizedPhone)
                    ps.setString(3, phoneLike)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            PhonePasswordCredential(
                                fullPhone = rs.getString("full_phone").orEmpty(),
                                phoneCountryCode = rs.getString("phone_country_code").orEmpty().ifBlank { "+90" },
                                phoneNumber = rs.getString("phone_number").orEmpty(),
                                passwordHash = rs.getString("password_hash").orEmpty(),
                                recoveryEmail = rs.getString("recovery_email").orEmpty(),
                                updatedAt = rs.getLong("updated_at")
                            )
                        } else null
                    }
                }
            }
        }.getOrNull()
    }

    fun syncCatalogsAsync(catalogs: List<Catalog>, rawJson: String, businessId: String = activeBusinessId) {
        if (!isConfigured()) return
        val resolvedBizId = businessId.ifBlank { "default_store" }
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                DatabaseManager.withConnection { conn ->
                    upsertKvSnapshot(conn, "${SNAPSHOT_CATALOGS}_$resolvedBizId", rawJson)

                    val sql = """
                        REPLACE INTO catalogues (
                            id, business_id, title, description, slug, cover_image_url, tags_json,
                            is_active, product_count, view_count, order_count, created_at, payload_json
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                    conn.prepareStatement(sql).use { ps ->
                        catalogs.forEach { cat ->
                            val tagsJson = JSONArray(cat.tags).toString()
                            val itemJson = JSONObject().apply {
                                put("id", cat.id)
                                put("businessId", resolvedBizId)
                                put("title", cat.title)
                                put("description", cat.description)
                                put("slug", cat.slug)
                                put("tags", JSONArray(cat.tags))
                                put("isActive", cat.isActive)
                                put("productCount", cat.productCount)
                                put("viewCount", cat.viewCount)
                                put("orderCount", cat.orderCount)
                                put("createdAt", cat.createdAt)
                            }.toString()

                            val webCoverUrl = toWebCompatibleImageUrl(cat.coverImageUrl)
                            ps.setString(1, cat.id)
                            ps.setString(2, resolvedBizId)
                            ps.setString(3, cat.title)
                            ps.setString(4, cat.description)
                            ps.setString(5, cat.slug)
                            ps.setString(6, webCoverUrl)
                            ps.setString(7, tagsJson)
                            ps.setInt(8, if (cat.isActive) 1 else 0)
                            ps.setInt(9, cat.productCount)
                            ps.setInt(10, cat.viewCount)
                            ps.setInt(11, cat.orderCount)
                            ps.setLong(12, cat.createdAt)
                            ps.setString(13, itemJson)
                            ps.executeUpdate()
                        }
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB catalogues senkronizasyon hatası: ${err.message}", err)
            }
        }
    }

    fun syncProductsAsync(products: List<Product>, rawJson: String, businessId: String = activeBusinessId) {
        if (!isConfigured()) return
        val resolvedBizId = businessId.ifBlank { "default_store" }
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                DatabaseManager.withConnection { conn ->
                    upsertKvSnapshot(conn, "${SNAPSHOT_PRODUCTS}_$resolvedBizId", rawJson)

                    val sql = """
                        REPLACE INTO products (
                            id, catalog_id, business_id, name, sku, description, price, original_price,
                            currency, image_url, gallery_urls_json, category, sub_category, tags_json,
                            track_stock, stock_status, stock_count, in_stock, variants_json,
                            secret_note, sort_order, created_at, payload_json
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                    conn.prepareStatement(sql).use { ps ->
                        products.forEach { prod ->
                            val webImageUrl = toWebCompatibleImageUrl(prod.imageUrl)
                            val webGallery = prod.galleryUrls.take(3).map { toWebCompatibleImageUrl(it) }
                            val galleryJson = JSONArray(webGallery).toString()
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
                                put("businessId", resolvedBizId)
                                put("name", prod.name)
                                put("sku", prod.sku)
                                put("price", prod.price)
                                put("currency", prod.currency)
                                put("stockCount", prod.stockCount)
                                put("stockStatus", prod.stockStatus.name)
                            }.toString()

                            ps.setString(1, prod.id)
                            ps.setString(2, prod.catalogId)
                            ps.setString(3, resolvedBizId)
                            ps.setString(4, prod.name)
                            ps.setString(5, prod.sku)
                            ps.setString(6, prod.description)
                            ps.setDouble(7, prod.price)
                            if (prod.originalPrice != null) {
                                ps.setDouble(8, prod.originalPrice)
                            } else {
                                ps.setNull(8, java.sql.Types.DOUBLE)
                            }
                            ps.setString(9, prod.currency)
                            ps.setString(10, webImageUrl)
                            ps.setString(11, galleryJson)
                            ps.setString(12, prod.category)
                            ps.setString(13, prod.subCategory)
                            ps.setString(14, tagsJson)
                            ps.setInt(15, if (prod.trackStock) 1 else 0)
                            ps.setString(16, prod.stockStatus.name)
                            ps.setInt(17, prod.stockCount)
                            ps.setInt(18, if (prod.inStock) 1 else 0)
                            ps.setString(19, variantsJson)
                            ps.setString(20, prod.secretNote)
                            ps.setInt(21, prod.sortOrder)
                            ps.setLong(22, prod.createdAt)
                            ps.setString(23, itemJson)
                            ps.executeUpdate()
                        }
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB products/images senkronizasyon hatası: ${err.message}", err)
            }
        }
    }

    fun syncOrdersAsync(orders: List<Order>, rawJson: String, businessId: String = activeBusinessId) {
        if (!isConfigured()) return
        val resolvedBizId = businessId.ifBlank { "default_store" }
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                DatabaseManager.withConnection { conn ->
                    upsertKvSnapshot(conn, "${SNAPSHOT_ORDERS}_$resolvedBizId", rawJson)

                    val sql = """
                        REPLACE INTO orders (
                            id, order_number, catalog_id, catalog_title, business_id,
                            customer_name, customer_phone, customer_address, customer_note,
                            items_json, total_amount, currency, status, timeline_json,
                            stock_deducted, reseller_id, reseller_name, created_at, payload_json
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                                put("businessId", resolvedBizId)
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
                            ps.setString(5, resolvedBizId)
                            ps.setString(6, order.customerName)
                            ps.setString(7, order.customerPhone)
                            ps.setString(8, order.customerAddress)
                            ps.setString(9, order.customerNote)
                            ps.setString(10, itemsArr.toString())
                            ps.setDouble(11, order.totalAmount)
                            ps.setString(12, order.currency)
                            ps.setString(13, order.status.name)
                            ps.setString(14, timelineArr.toString())
                            ps.setInt(15, if (order.stockDeducted) 1 else 0)
                            ps.setString(16, order.resellerId)
                            ps.setString(17, order.resellerName)
                            ps.setLong(18, order.createdAt)
                            ps.setString(19, payload)
                            ps.executeUpdate()
                        }
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB orders senkronizasyon hatası: ${err.message}", err)
            }
        }
    }

    fun syncResellersAsync(resellers: List<Reseller>, rawJson: String, businessId: String = activeBusinessId) {
        if (!isConfigured()) return
        val resolvedBizId = businessId.ifBlank { "default_store" }
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                DatabaseManager.withConnection { conn ->
                    upsertKvSnapshot(conn, "${SNAPSHOT_RESELLERS}_$resolvedBizId", rawJson)

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
                            ps.executeUpdate()
                        }
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB resellers senkronizasyon hatası: ${err.message}", err)
            }
        }
    }

    fun syncSettingsAsync(
        settings: StoreSettings,
        rawJson: String,
        businessId: String = activeBusinessId,
        ownerUserId: String = ""
    ) {
        if (!isConfigured()) return
        val resolvedBizId = businessId.ifBlank { "default_store" }
        scope.launch {
            if (!ensureSchema()) return@launch
            runCatching {
                DatabaseManager.withConnection { conn ->
                    upsertKvSnapshot(conn, "${SNAPSHOT_SETTINGS}_$resolvedBizId", rawJson)
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
                    val webLogoUri = toWebCompatibleImageUrl(settings.logoUri)

                    val sql = """
                        REPLACE INTO businesses (
                            id, owner_user_id, business_name, business_type, logo_uri, logo_blob,
                            phone_whatsapp, city, business_address, instagram_url,
                            website_url, facebook_url, currency_code, currency_symbol,
                            catalog_language, custom_slug, vat_rate_percent, show_vat_in_prices,
                            min_order_amount, shipping_fee, order_confirmation_template,
                            catalog_theme_color_hex, catalog_grid_columns, catalog_show_prices,
                            protect_images_from_saving, low_stock_threshold, subscription_plan,
                            team_members_json, updated_at, payload_json
                        ) VALUES (
                            ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                        )
                    """.trimIndent()
                    conn.prepareStatement(sql).use { ps ->
                        ps.setString(1, resolvedBizId)
                        ps.setString(2, ownerUserId.ifBlank { resolvedBizId })
                        ps.setString(3, settings.businessName)
                        ps.setString(4, settings.businessType)
                        ps.setString(5, webLogoUri)
                        if (logoBytes != null) {
                            ps.setBytes(6, logoBytes)
                        } else {
                            ps.setNull(6, java.sql.Types.BLOB)
                        }
                        ps.setString(7, settings.phoneWhatsapp)
                        ps.setString(8, settings.city)
                        ps.setString(9, settings.businessAddress)
                        ps.setString(10, settings.instagramUrl)
                        ps.setString(11, settings.websiteUrl)
                        ps.setString(12, settings.facebookUrl)
                        ps.setString(13, settings.currencyCode)
                        ps.setString(14, settings.currencySymbol)
                        ps.setString(15, settings.catalogLanguage)
                        ps.setString(16, settings.customSlug)
                        ps.setInt(17, settings.vatRatePercent)
                        ps.setInt(18, if (settings.showVatInPrices) 1 else 0)
                        ps.setDouble(19, settings.minOrderAmount)
                        ps.setDouble(20, settings.shippingFee)
                        ps.setString(21, settings.orderConfirmationTemplate)
                        ps.setString(22, settings.catalogThemeColorHex)
                        ps.setInt(23, settings.catalogGridColumns)
                        ps.setInt(24, if (settings.catalogShowPrices) 1 else 0)
                        ps.setInt(25, if (settings.protectImagesFromSaving) 1 else 0)
                        ps.setInt(26, settings.lowStockThreshold)
                        ps.setString(27, settings.subscriptionPlan.name)
                        ps.setString(28, teamJson)
                        ps.setLong(29, now)
                        ps.setString(30, rawJson)
                        ps.executeUpdate()
                    }
                    if (settings.logoUri.isNotBlank()) {
                        upsertImageRecord(
                            conn = conn,
                            ownerType = "store_logo",
                            ownerId = resolvedBizId,
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
                DatabaseManager.withConnection { conn ->
                    if (user == null || rawJson.isNullOrBlank()) {
                        conn.prepareStatement("DELETE FROM kv_snapshots WHERE store_key = ?").use { ps ->
                            ps.setString(1, SNAPSHOT_AUTH_USER)
                            ps.executeUpdate()
                        }
                        return@withConnection
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
                DatabaseManager.withConnection { conn ->
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
                            ps.executeUpdate()
                        }
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
                DatabaseManager.withConnection { conn ->
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
                            ps.executeUpdate()
                        }
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
                DatabaseManager.withConnection { conn ->
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
                            ps.executeUpdate()
                        }
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
                        stmt.executeUpdate("DELETE FROM catalog_live_sessions")
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "MariaDB tablo temizleme hatası: ${err.message}", err)
            }
        }
    }

    /**
     * `constantinopolishotel.store/api.php` tarafından doğrudan `orders` tablosuna yazılan
     * web katalog siparişlerini ve mevcut ilişkisel siparişleri okur.
     */
    suspend fun fetchRelationalOrders(
        businessId: String = activeBusinessId,
        ownedCatalogIds: Set<String> = emptySet()
    ): List<Order> = withContext(Dispatchers.IO) {
        if (!ensureSchema()) return@withContext emptyList()
        val resolvedBizId = businessId.ifBlank { "default_store" }
        runCatching {
            val allOrders = DatabaseManager.executeQuery(
                sql = """
                    SELECT id, order_number, catalog_id, catalog_title, business_id, customer_name,
                           customer_phone, customer_address, customer_note, items_json,
                           total_amount, currency, status, timeline_json, stock_deducted,
                           reseller_id, reseller_name, created_at
                    FROM orders
                    ORDER BY created_at DESC
                    LIMIT 500
                """.trimIndent()
            ) { rs ->
                val orderId = rs.getString("id").orEmpty()
                val orderNumber = rs.getString("order_number").orEmpty().ifBlank { orderId.uppercase() }
                val catalogId = rs.getString("catalog_id").orEmpty()
                val catalogTitle = rs.getString("catalog_title").orEmpty()
                val orderBizId = rs.getString("business_id").orEmpty()
                val customerName = rs.getString("customer_name").orEmpty()
                val customerPhone = rs.getString("customer_phone").orEmpty()
                val customerAddress = rs.getString("customer_address").orEmpty()
                val customerNote = rs.getString("customer_note").orEmpty()
                val itemsRaw = rs.getString("items_json").orEmpty()
                val totalAmount = rs.getDouble("total_amount")
                val currency = rs.getString("currency").orEmpty().ifBlank { "₺" }
                val statusStr = rs.getString("status").orEmpty().ifBlank { "NEW" }
                val timelineRaw = rs.getString("timeline_json").orEmpty()
                val stockDeducted = rs.getInt("stock_deducted") == 1
                val resellerId = rs.getString("reseller_id").orEmpty()
                val resellerName = rs.getString("reseller_name").orEmpty()
                val createdAt = rs.getLong("created_at")

                val items = parseRelationalOrderItems(itemsRaw)
                val status = runCatching { OrderStatus.valueOf(statusStr.uppercase()) }
                    .getOrDefault(OrderStatus.NEW)
                val timeline = parseRelationalOrderTimeline(timelineRaw, status, createdAt)

                orderBizId to Order(
                    id = orderId,
                    orderNumber = orderNumber,
                    catalogId = catalogId,
                    catalogTitle = catalogTitle,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    customerAddress = customerAddress,
                    customerNote = customerNote,
                    items = items,
                    totalAmount = totalAmount,
                    currency = currency,
                    status = status,
                    timeline = timeline,
                    stockDeducted = stockDeducted,
                    resellerId = resellerId,
                    resellerName = resellerName,
                    createdAt = createdAt
                )
            }
            allOrders.filter { (orderBizId, order) ->
                orderBizId == resolvedBizId || (ownedCatalogIds.isNotEmpty() && order.catalogId in ownedCatalogIds)
            }.map { it.second }
        }.onFailure { err ->
            Log.w(TAG, "MariaDB ilişkisel orders okuma hatası: ${err.message}")
        }.getOrDefault(emptyList())
    }

    /**
     * `constantinopolishotel.store/api.php` tarafından doğrudan `visits` tablosuna yazılan
     * (`catalog_open`, `product_view`, `add_to_cart`) web ziyaretçi hareketlerini okur.
     */
    suspend fun fetchRelationalVisits(): List<CatalogAnalyticsEvent> = withContext(Dispatchers.IO) {
        if (!ensureSchema()) return@withContext emptyList()
        runCatching {
            DatabaseManager.executeQuery(
                sql = """
                    SELECT id, session_id, event_type, catalog_id, short_id,
                           product_id, product_name, quantity, referrer, created_at
                    FROM visits
                    ORDER BY created_at DESC
                    LIMIT 800
                """.trimIndent()
            ) { rs ->
                val rawType = rs.getString("event_type").orEmpty()
                val normalizedType = when (rawType) {
                    "catalog_open", "visit" -> "visit"
                    "product_view", "productView" -> "productView"
                    "add_to_cart", "addToCart" -> "addToCart"
                    else -> "visit"
                }
                CatalogAnalyticsEvent(
                    id = rs.getString("id").orEmpty(),
                    sessionId = rs.getString("session_id").orEmpty(),
                    eventType = normalizedType,
                    catalogId = rs.getString("catalog_id").orEmpty(),
                    shortId = rs.getString("short_id").orEmpty(),
                    productId = rs.getString("product_id").orEmpty(),
                    productName = rs.getString("product_name").orEmpty(),
                    quantity = rs.getInt("quantity").coerceAtLeast(1),
                    referrer = rs.getString("referrer").orEmpty().ifBlank { "web" },
                    createdAt = rs.getLong("created_at")
                )
            }
        }.onFailure { err ->
            Log.w(TAG, "MariaDB ilişkisel visits okuma hatası: ${err.message}")
        }.getOrDefault(emptyList())
    }

    /**
     * `catalogues` tablosundaki güncel `view_count` ve `order_count` sayaçlarını döner.
     */
    suspend fun fetchRelationalCatalogCounters(): Map<String, Pair<Int, Int>> = withContext(Dispatchers.IO) {
        if (!ensureSchema()) return@withContext emptyMap()
        runCatching {
            val rows = DatabaseManager.executeQuery(
                sql = "SELECT id, view_count, order_count FROM catalogues"
            ) { rs ->
                val id = rs.getString("id").orEmpty()
                val views = rs.getInt("view_count")
                val orders = rs.getInt("order_count")
                id to (views to orders)
            }
            rows.filter { it.first.isNotBlank() }.toMap()
        }.getOrDefault(emptyMap())
    }

    /**
     * `catalog_live_sessions` tablosunu (son 90 saniye içinde aktif olan oturumlar) sorgulayarak
     * katalog başına canlı ziyaretçi sayısını (`catalogId -> activeViewers`) döner.
     * JDBC henüz yapılandırılmamışsa `constantinopolishotel.store/api.php` üzerinden HTTP ile sorgular.
     */
    suspend fun fetchLiveViewerCounts(catalogs: List<Catalog>): Map<String, Int> = withContext(Dispatchers.IO) {
        val cutoffMs = System.currentTimeMillis() - LIVE_WINDOW_MS
        if (isConfigured() && ensureSchema()) {
            val dbCounts = runCatching {
                val rows = DatabaseManager.executeQuery(
                    sql = """
                        SELECT catalog_id, COUNT(*) AS cnt
                        FROM catalog_live_sessions
                        WHERE last_seen_at >= ?
                        GROUP BY catalog_id
                    """.trimIndent(),
                    cutoffMs
                ) { rs ->
                    val catId = rs.getString("catalog_id").orEmpty()
                    val cnt = rs.getInt("cnt")
                    catId to cnt
                }
                rows.filter { it.first.isNotBlank() }.toMap()
            }.getOrNull()
            if (dbCounts != null) return@withContext dbCounts
        }

        // HTTP Fallback: constantinopolishotel.store/api.php?action=catalog&key=<slug>
        buildMap {
            val baseUrl = DatabaseManager.getWebBaseUrl()
            catalogs.filter { it.isActive }.take(8).forEach { cat ->
                val key = cat.slug.ifBlank { cat.id }
                val count = fetchActiveViewersViaWebApi(baseUrl, key)
                if (count != null && count > 0) {
                    put(cat.id, count)
                }
            }
        }
    }

    /**
     * Katalog incelenirken `catalog_live_sessions` tablosuna canlı oturum sinyali (heartbeat) yazar
     * ve eski oturumları (10 dakikadan eski) temizler.
     */
    fun recordLiveSessionHeartbeatAsync(
        catalogId: String,
        catalogSlug: String,
        sessionId: String,
        event: String = "heartbeat",
        productId: String = "",
        quantity: Int = 1
    ) {
        val cleanSessionId = sessionId.replace(Regex("[^a-zA-Z0-9_-]"), "").take(120)
            .let { if (it.length < 16) it.padEnd(16, '0') else it }
        val nowMs = System.currentTimeMillis()

        scope.launch {
            if (isConfigured() && ensureSchema()) {
                runCatching {
                    DatabaseManager.withConnection { conn ->
                        conn.prepareStatement(
                            """
                            INSERT INTO catalog_live_sessions (catalog_id, session_id, last_seen_at)
                            VALUES (?, ?, ?)
                            ON DUPLICATE KEY UPDATE last_seen_at = VALUES(last_seen_at)
                            """.trimIndent()
                        ).use { ps ->
                            ps.setString(1, catalogId)
                            ps.setString(2, cleanSessionId)
                            ps.setLong(3, nowMs)
                            ps.executeUpdate()
                        }

                        conn.prepareStatement(
                            "DELETE FROM catalog_live_sessions WHERE last_seen_at < ?"
                        ).use { ps ->
                            ps.setLong(1, nowMs - 600_000L)
                            ps.executeUpdate()
                        }
                    }
                }
            } else {
                // JDBC kapalıysa doğrudan constantinopolishotel.store/api.php?action=activity uç noktasına bildir
                postActivityToWebApi(
                    baseUrl = DatabaseManager.getWebBaseUrl(),
                    catalogKey = catalogSlug.ifBlank { catalogId },
                    sessionId = cleanSessionId,
                    event = event,
                    productId = productId,
                    quantity = quantity
                )
            }
        }
    }

    private fun fetchActiveViewersViaWebApi(baseUrl: String, catalogKey: String): Int? {
        return runCatching {
            val encodedKey = URLEncoder.encode(catalogKey, "UTF-8")
            val url = URL("$baseUrl/api.php?action=catalog&key=$encodedKey")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    json.optInt("active_viewers", 0)
                } else {
                    null
                }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun postActivityToWebApi(
        baseUrl: String,
        catalogKey: String,
        sessionId: String,
        event: String,
        productId: String,
        quantity: Int
    ) {
        runCatching {
            val url = URL("$baseUrl/api.php?action=activity")
            val payload = JSONObject().apply {
                put("key", catalogKey)
                put("session_id", sessionId)
                put("event", event)
                if (productId.isNotBlank()) put("product_id", productId)
                put("quantity", quantity.coerceAtLeast(1))
            }.toString()
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 5_000
                readTimeout = 5_000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            try {
                conn.outputStream.use { out ->
                    out.write(payload.toByteArray(Charsets.UTF_8))
                }
                conn.responseCode
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun parseRelationalOrderItems(itemsRaw: String): List<OrderItem> {
        if (itemsRaw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(itemsRaw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    // Hem Android camelCase hem de api.php snake_case anahtarlarını destekle
                    val prodId = obj.optString("productId").ifBlank { obj.optString("product_id") }
                    val prodName = obj.optString("productName").ifBlank { obj.optString("name", "Ürün") }
                    val qty = obj.optInt("quantity", 1).coerceAtLeast(1)
                    val unitPrice = if (obj.has("unitPrice")) {
                        obj.optDouble("unitPrice", 0.0)
                    } else {
                        obj.optDouble("unit_price", 0.0)
                    }
                    val variantSummary = obj.optString("selectedVariantSummary", "")
                    val imageUrl = obj.optString("imageUrl").ifBlank { obj.optString("image_url", "") }
                    add(
                        OrderItem(
                            productId = prodId,
                            productName = prodName,
                            quantity = qty,
                            unitPrice = unitPrice,
                            selectedVariantSummary = variantSummary,
                            imageUrl = imageUrl
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun parseRelationalOrderTimeline(
        timelineRaw: String,
        fallbackStatus: OrderStatus,
        createdAt: Long
    ): List<OrderTimelineEntry> {
        if (timelineRaw.isBlank()) {
            return listOf(
                OrderTimelineEntry(
                    id = "tl-$createdAt",
                    status = fallbackStatus,
                    title = "Sipariş alındı",
                    note = "Web Katalog (constantinopolishotel.store)",
                    timestamp = createdAt
                )
            )
        }
        return runCatching {
            val arr = JSONArray(timelineRaw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val stStr = obj.optString("status", fallbackStatus.name)
                    val st = runCatching { OrderStatus.valueOf(stStr.uppercase()) }.getOrDefault(fallbackStatus)
                    val ts = if (obj.has("timestamp")) obj.optLong("timestamp", createdAt) else obj.optLong("at", createdAt)
                    val source = obj.optString("source", "")
                    val title = obj.optString("title").ifBlank {
                        if (source == "web_catalog") "Web Katalogdan Sipariş Alındı" else st.labelTr
                    }
                    val note = obj.optString("note").ifBlank {
                        if (source == "web_catalog") "constantinopolishotel.store üzerinden iletildi" else ""
                    }
                    add(
                        OrderTimelineEntry(
                            id = obj.optString("id").ifBlank { "tl-$ts-$i" },
                            status = st,
                            title = title,
                            note = note,
                            timestamp = ts
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Yerel `content://` veya `file://` görsellerini `constantinopolishotel.store` web tarayıcısında
     * ve veritabanında doğrudan görüntülenebilmesi için sıkıştırılmış `data:image/jpeg;base64,...`
     * formatına dönüştürür.
     */
    private fun toWebCompatibleImageUrl(uriString: String): String {
        val trimmed = uriString.trim()
        if (trimmed.isBlank()) return ""
        return com.example.util.ImageCompressionUtil.toWebAndDbCompatibleUrl(appContext, trimmed)
    }

    /**
     * Paylaşılan bir katalog yerelde bulunamadığında MariaDB veya `constantinopolishotel.store/api.php`
     * üzerinden kataloğu ve ürünlerini çeker.
     */
    suspend fun fetchCatalogAndProductsBySlugOrId(
        slugOrShortId: String
    ): Pair<Catalog, List<Product>>? = withContext(Dispatchers.IO) {
        val clean = slugOrShortId.trim().removePrefix("/c/").removePrefix("c/").trim('/')
        if (clean.isEmpty()) return@withContext null

        // 1) Önce hızlı HTTP API (constantinopolishotel.store/api.php?slug=...) üzerinden dene
        val fromWebApi = runCatching {
            val baseUrl = DatabaseManager.getWebBaseUrl().trimEnd('/')
            val url = java.net.URL("$baseUrl/api.php?slug=${java.net.URLEncoder.encode(clean, "UTF-8")}")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body)
                if (root.optBoolean("ok", false)) {
                    val catObj = root.optJSONObject("catalog")
                    val prodsArr = root.optJSONArray("products")
                    if (catObj != null) {
                        val catId = catObj.optString("id").ifBlank { clean }
                        val tagsArr = runCatching { JSONArray(catObj.optString("tags_json", "[]")) }.getOrNull()
                        val tags = buildList {
                            if (tagsArr != null) {
                                for (i in 0 until tagsArr.length()) {
                                    tagsArr.optString(i)?.takeIf { it.isNotBlank() }?.let { add(it) }
                                }
                            }
                        }
                        val catalog = Catalog(
                            id = catId,
                            title = catObj.optString("title", "Katalog"),
                            description = catObj.optString("description", ""),
                            slug = catObj.optString("slug", clean).ifBlank { clean },
                            coverImageUrl = catObj.optString("cover_image_url", ""),
                            tags = tags,
                            isActive = true,
                            productCount = prodsArr?.length() ?: catObj.optInt("product_count", 0),
                            viewCount = catObj.optInt("view_count", 0),
                            orderCount = catObj.optInt("order_count", 0)
                        )
                        val products = buildList {
                            if (prodsArr != null) {
                                for (i in 0 until prodsArr.length()) {
                                    val p = prodsArr.optJSONObject(i) ?: continue
                                    val pid = p.optString("id").ifBlank { "prod-$i" }
                                    val img = p.optString("image_url", "")
                                    add(
                                        Product(
                                            id = pid,
                                            catalogId = catId,
                                            name = p.optString("name", "Ürün"),
                                            description = p.optString("description", ""),
                                            price = p.optDouble("price", 0.0),
                                            originalPrice = p.optDouble("original_price", 0.0).takeIf { it > p.optDouble("price", 0.0) },
                                            currency = p.optString("currency", "₺").ifBlank { "₺" },
                                            imageUrl = img,
                                            galleryUrls = if (img.isNotBlank()) listOf(img) else emptyList(),
                                            category = p.optString("category", ""),
                                            subCategory = p.optString("sub_category", "Genel").ifBlank { "Genel" },
                                            sku = p.optString("sku", ""),
                                            inStock = p.optInt("in_stock", 1) == 1,
                                            stockCount = p.optInt("stock_count", 25),
                                            sortOrder = p.optInt("sort_order", i)
                                        )
                                    )
                                }
                            }
                        }
                        catalog to products
                    } else null
                } else null
            } else null
        }.getOrNull()

        if (fromWebApi != null) return@withContext fromWebApi

        // 2) Doğrudan MariaDB catalogues & products tablolarından dene
        if (!isConfigured()) return@withContext null
        return@withContext runCatching {
            DatabaseManager.withConnection { conn ->
                val foundCatalog = conn.prepareStatement(
                    "SELECT id, title, description, slug, cover_image_url, tags_json, is_active, product_count, view_count, order_count, created_at FROM catalogues WHERE slug = ? OR id = ? LIMIT 1"
                ).use { ps ->
                    ps.setString(1, clean)
                    ps.setString(2, clean)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            val tagsArr = runCatching { JSONArray(rs.getString("tags_json").orEmpty()) }.getOrNull()
                            val tags = buildList {
                                if (tagsArr != null) {
                                    for (i in 0 until tagsArr.length()) {
                                        tagsArr.optString(i)?.takeIf { it.isNotBlank() }?.let { add(it) }
                                    }
                                }
                            }
                            Catalog(
                                id = rs.getString("id").orEmpty(),
                                title = rs.getString("title").orEmpty(),
                                description = rs.getString("description").orEmpty(),
                                slug = rs.getString("slug").orEmpty(),
                                coverImageUrl = rs.getString("cover_image_url").orEmpty(),
                                tags = tags,
                                isActive = rs.getInt("is_active") == 1,
                                productCount = rs.getInt("product_count"),
                                viewCount = rs.getInt("view_count"),
                                orderCount = rs.getInt("order_count"),
                                createdAt = rs.getLong("created_at")
                            )
                        } else null
                    }
                } ?: return@withConnection null

                val products = conn.prepareStatement(
                    "SELECT id, catalog_id, name, sku, description, price, original_price, currency, image_url, category, sub_category, stock_status, stock_count, in_stock, sort_order FROM products WHERE catalog_id = ? ORDER BY sort_order ASC"
                ).use { ps ->
                    ps.setString(1, foundCatalog.id)
                    ps.executeQuery().use { rs ->
                        buildList {
                            while (rs.next()) {
                                val price = rs.getDouble("price")
                                val orig = rs.getDouble("original_price").takeIf { !rs.wasNull() && it > price }
                                val img = rs.getString("image_url").orEmpty()
                                val statusStr = rs.getString("stock_status").orEmpty()
                                val status = runCatching { com.example.model.StockStatus.valueOf(statusStr) }
                                    .getOrDefault(com.example.model.StockStatus.IN_STOCK)
                                add(
                                    Product(
                                        id = rs.getString("id").orEmpty(),
                                        catalogId = rs.getString("catalog_id").orEmpty(),
                                        name = rs.getString("name").orEmpty(),
                                        sku = rs.getString("sku").orEmpty(),
                                        description = rs.getString("description").orEmpty(),
                                        price = price,
                                        originalPrice = orig,
                                        currency = rs.getString("currency")?.ifBlank { "₺" } ?: "₺",
                                        imageUrl = img,
                                        galleryUrls = if (img.isNotBlank()) listOf(img) else emptyList(),
                                        category = rs.getString("category").orEmpty(),
                                        subCategory = rs.getString("sub_category")?.ifBlank { "Genel" } ?: "Genel",
                                        stockStatus = status,
                                        stockCount = rs.getInt("stock_count"),
                                        inStock = rs.getInt("in_stock") == 1,
                                        sortOrder = rs.getInt("sort_order")
                                    )
                                )
                            }
                        }
                    }
                }
                foundCatalog.copy(isActive = true, productCount = products.size) to products
            }
        }.getOrNull()
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
            val compatUrl = com.example.util.ImageCompressionUtil.toWebAndDbCompatibleUrl(appContext, uriString)
            if (compatUrl.startsWith("data:image/") && compatUrl.contains(";base64,")) {
                val base64Part = compatUrl.substringAfter(";base64,")
                return@runCatching Base64.decode(base64Part, Base64.DEFAULT)
            }
            when {
                uriString.startsWith("file://") -> {
                    val path = Uri.parse(uriString).path ?: uriString.removePrefix("file://")
                    val f = File(path)
                    if (f.exists()) f.readBytes() else null
                }
                uriString.startsWith("content://") -> {
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
        private const val LIVE_WINDOW_MS = 90_000L
    }
}
