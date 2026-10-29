-- ============================================================================
-- Katalog Yap - MariaDB Veritabanı Şeması (UTF8MB4 / InnoDB)
-- Tablolar: users, businesses, catalogues, products, orders, visits
-- Yardımcı Tablolar: images, resellers, phone_credentials, share_events, kv_snapshots
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. USERS (Kullanıcılar: Telefon + Şifre ve Google Oturum Açma)
CREATE TABLE IF NOT EXISTS `users` (
    `id` VARCHAR(128) NOT NULL,
    `full_phone` VARCHAR(64) DEFAULT NULL,
    `phone_country_code` VARCHAR(16) DEFAULT '+90',
    `phone_number` VARCHAR(64) DEFAULT NULL,
    `email` VARCHAR(255) DEFAULT NULL,
    `display_name` VARCHAR(255) DEFAULT NULL,
    `password_hash` VARCHAR(255) DEFAULT NULL,
    `provider` VARCHAR(64) NOT NULL DEFAULT 'phone_password',
    `firebase_uid` VARCHAR(128) DEFAULT NULL,
    `is_onboarding_completed` TINYINT(1) NOT NULL DEFAULT 0,
    `logged_in_at` BIGINT NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `payload_json` LONGTEXT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_users_full_phone` (`full_phone`),
    KEY `idx_users_email` (`email`),
    KEY `idx_users_firebase_uid` (`firebase_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. BUSINESSES (İşletme Profili, Mağaza Ayarları, Abonelik ve Ekip)
CREATE TABLE IF NOT EXISTS `businesses` (
    `id` VARCHAR(64) NOT NULL DEFAULT 'default_store',
    `owner_user_id` VARCHAR(128) DEFAULT NULL,
    `business_name` VARCHAR(255) NOT NULL,
    `business_type` VARCHAR(128) DEFAULT NULL,
    `logo_uri` TEXT DEFAULT NULL,
    `logo_blob` LONGBLOB DEFAULT NULL,
    `phone_whatsapp` VARCHAR(64) DEFAULT NULL,
    `city` VARCHAR(128) DEFAULT NULL,
    `business_address` TEXT DEFAULT NULL,
    `instagram_url` VARCHAR(255) DEFAULT NULL,
    `website_url` VARCHAR(255) DEFAULT NULL,
    `facebook_url` VARCHAR(255) DEFAULT NULL,
    `currency_code` VARCHAR(16) NOT NULL DEFAULT 'TRY',
    `currency_symbol` VARCHAR(16) NOT NULL DEFAULT '₺',
    `catalog_language` VARCHAR(16) NOT NULL DEFAULT 'tr',
    `custom_slug` VARCHAR(128) DEFAULT NULL,
    `vat_rate_percent` INT NOT NULL DEFAULT 20,
    `show_vat_in_prices` TINYINT(1) NOT NULL DEFAULT 1,
    `min_order_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `shipping_fee` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    `order_confirmation_template` TEXT DEFAULT NULL,
    `catalog_theme_color_hex` VARCHAR(32) DEFAULT '#10B981',
    `catalog_grid_columns` INT NOT NULL DEFAULT 2,
    `catalog_show_prices` TINYINT(1) NOT NULL DEFAULT 1,
    `protect_images_from_saving` TINYINT(1) NOT NULL DEFAULT 0,
    `low_stock_threshold` INT NOT NULL DEFAULT 5,
    `subscription_plan` VARCHAR(64) NOT NULL DEFAULT 'FREE',
    `team_members_json` LONGTEXT DEFAULT NULL,
    `updated_at` BIGINT NOT NULL,
    `payload_json` LONGTEXT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_businesses_slug` (`custom_slug`),
    KEY `idx_businesses_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. CATALOGUES (Kataloglar)
CREATE TABLE IF NOT EXISTS `catalogues` (
    `id` VARCHAR(128) NOT NULL,
    `business_id` VARCHAR(64) NOT NULL DEFAULT 'default_store',
    `title` VARCHAR(255) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `slug` VARCHAR(255) NOT NULL,
    `cover_image_url` TEXT DEFAULT NULL,
    `tags_json` TEXT DEFAULT NULL,
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `product_count` INT NOT NULL DEFAULT 0,
    `view_count` INT NOT NULL DEFAULT 0,
    `order_count` INT NOT NULL DEFAULT 0,
    `created_at` BIGINT NOT NULL,
    `payload_json` LONGTEXT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_catalogues_business` (`business_id`),
    KEY `idx_catalogues_slug` (`slug`),
    KEY `idx_catalogues_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. PRODUCTS (Ürünler, Varyantlar, Stok ve Görsel Galerisi)
CREATE TABLE IF NOT EXISTS `products` (
    `id` VARCHAR(128) NOT NULL,
    `catalog_id` VARCHAR(128) NOT NULL,
    `business_id` VARCHAR(64) NOT NULL DEFAULT 'default_store',
    `name` VARCHAR(255) NOT NULL,
    `sku` VARCHAR(128) DEFAULT NULL,
    `description` TEXT DEFAULT NULL,
    `price` DOUBLE NOT NULL DEFAULT 0,
    `original_price` DOUBLE DEFAULT NULL,
    `currency` VARCHAR(16) NOT NULL DEFAULT '₺',
    `image_url` TEXT DEFAULT NULL,
    `gallery_urls_json` LONGTEXT DEFAULT NULL,
    `category` VARCHAR(128) DEFAULT NULL,
    `sub_category` VARCHAR(128) DEFAULT NULL,
    `tags_json` TEXT DEFAULT NULL,
    `track_stock` TINYINT(1) NOT NULL DEFAULT 1,
    `stock_status` VARCHAR(64) NOT NULL DEFAULT 'IN_STOCK',
    `stock_count` INT NOT NULL DEFAULT 0,
    `in_stock` TINYINT(1) NOT NULL DEFAULT 1,
    `variants_json` LONGTEXT DEFAULT NULL,
    `secret_note` TEXT DEFAULT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `created_at` BIGINT NOT NULL,
    `payload_json` LONGTEXT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_products_catalog` (`catalog_id`),
    KEY `idx_products_sku` (`sku`),
    KEY `idx_products_stock_status` (`stock_status`),
    KEY `idx_products_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. ORDERS (Müşteri Siparişleri, Sipariş Kalemleri ve Zaman Çizelgesi)
CREATE TABLE IF NOT EXISTS `orders` (
    `id` VARCHAR(128) NOT NULL,
    `order_number` VARCHAR(64) NOT NULL,
    `catalog_id` VARCHAR(128) NOT NULL,
    `catalog_title` VARCHAR(255) DEFAULT NULL,
    `business_id` VARCHAR(64) NOT NULL DEFAULT 'default_store',
    `customer_name` VARCHAR(255) NOT NULL,
    `customer_phone` VARCHAR(64) NOT NULL,
    `customer_address` TEXT DEFAULT NULL,
    `customer_note` TEXT DEFAULT NULL,
    `items_json` LONGTEXT NOT NULL,
    `total_amount` DOUBLE NOT NULL DEFAULT 0,
    `currency` VARCHAR(16) NOT NULL DEFAULT '₺',
    `status` VARCHAR(64) NOT NULL DEFAULT 'NEW',
    `timeline_json` LONGTEXT DEFAULT NULL,
    `stock_deducted` TINYINT(1) NOT NULL DEFAULT 1,
    `reseller_id` VARCHAR(128) DEFAULT NULL,
    `reseller_name` VARCHAR(255) DEFAULT NULL,
    `created_at` BIGINT NOT NULL,
    `payload_json` LONGTEXT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_orders_order_number` (`order_number`),
    KEY `idx_orders_catalog` (`catalog_id`),
    KEY `idx_orders_status` (`status`),
    KEY `idx_orders_reseller` (`reseller_id`),
    KEY `idx_orders_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. VISITS (Katalog Ziyaretleri, Ürün Görüntülemeleri ve Sepete Ekleme Analitiği)
CREATE TABLE IF NOT EXISTS `visits` (
    `id` VARCHAR(128) NOT NULL,
    `session_id` VARCHAR(128) NOT NULL,
    `event_type` VARCHAR(64) NOT NULL DEFAULT 'visit',
    `catalog_id` VARCHAR(128) DEFAULT NULL,
    `short_id` VARCHAR(128) DEFAULT NULL,
    `product_id` VARCHAR(128) DEFAULT NULL,
    `product_name` VARCHAR(255) DEFAULT NULL,
    `quantity` INT NOT NULL DEFAULT 1,
    `referrer` VARCHAR(64) NOT NULL DEFAULT 'whatsapp',
    `created_at` BIGINT NOT NULL,
    `payload_json` LONGTEXT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_visits_session` (`session_id`),
    KEY `idx_visits_event_type` (`event_type`),
    KEY `idx_visits_catalog` (`catalog_id`),
    KEY `idx_visits_product` (`product_id`),
    KEY `idx_visits_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. IMAGES (Ürün, Katalog Kapağı ve İşletme Logosu Binary LONGBLOB + Base64 Kayıtları)
CREATE TABLE IF NOT EXISTS `images` (
    `image_key` VARCHAR(128) NOT NULL,
    `owner_type` VARCHAR(64) NOT NULL,
    `owner_id` VARCHAR(128) NOT NULL,
    `source_uri` TEXT NOT NULL,
    `mime_type` VARCHAR(64) NOT NULL DEFAULT 'image/jpeg',
    `image_base64` LONGTEXT DEFAULT NULL,
    `image_blob` LONGBLOB DEFAULT NULL,
    `updated_at` BIGINT NOT NULL,
    PRIMARY KEY (`image_key`),
    KEY `idx_images_owner` (`owner_type`, `owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. RESELLERS (Bayiler ve Bayiye Özel Fiyat/Marj Tanımları)
CREATE TABLE IF NOT EXISTS `resellers` (
    `id` VARCHAR(128) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `phone` VARCHAR(64) NOT NULL,
    `short_code` VARCHAR(64) NOT NULL,
    `visible_catalog_ids_json` TEXT DEFAULT NULL,
    `price_mode` VARCHAR(64) NOT NULL,
    `margin_value` DOUBLE NOT NULL DEFAULT 0,
    `note` TEXT DEFAULT NULL,
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` BIGINT NOT NULL,
    `payload_json` LONGTEXT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_resellers_short_code` (`short_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. PHONE_CREDENTIALS (Telefon + Şifre Kimlik Doğrulama Kayıtları)
CREATE TABLE IF NOT EXISTS `phone_credentials` (
    `full_phone` VARCHAR(64) NOT NULL,
    `phone_country_code` VARCHAR(16) NOT NULL,
    `phone_number` VARCHAR(64) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `recovery_email` VARCHAR(255) DEFAULT NULL,
    `updated_at` BIGINT NOT NULL,
    `payload_json` LONGTEXT NOT NULL,
    PRIMARY KEY (`full_phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. SHARE_EVENTS (Katalog Paylaşım Bağlantısı Hareketleri)
CREATE TABLE IF NOT EXISTS `share_events` (
    `id` VARCHAR(128) NOT NULL,
    `target_type` VARCHAR(64) NOT NULL,
    `target_id` VARCHAR(128) DEFAULT NULL,
    `target_title` VARCHAR(255) DEFAULT NULL,
    `channel` VARCHAR(64) DEFAULT NULL,
    `short_link` TEXT DEFAULT NULL,
    `created_at` BIGINT NOT NULL,
    `payload_json` LONGTEXT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. KV_SNAPSHOTS (Uygulama Durumu Anlık Senkronizasyon Yedeği)
CREATE TABLE IF NOT EXISTS `kv_snapshots` (
    `store_key` VARCHAR(128) NOT NULL,
    `json_payload` LONGTEXT NOT NULL,
    `updated_at` BIGINT NOT NULL,
    PRIMARY KEY (`store_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
