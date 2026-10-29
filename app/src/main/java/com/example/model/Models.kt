package com.example.model

/**
 * Katalog Yap veri modelleri.
 * İleride Firebase Firestore koleksiyonlarına birebir eşlenecek şekilde tasarlanmıştır.
 */
data class AuthUser(
    val id: String,
    val phoneCountryCode: String = "+90",
    val phoneNumber: String = "",
    val fullPhone: String = "",
    val email: String = "",
    val displayName: String = "",
    val passwordHash: String = "",
    val firebaseUid: String = "",
    val provider: String = "phone_password", // "phone_password", "google", "apple"
    val isOnboardingCompleted: Boolean = false,
    val loggedInAt: Long = System.currentTimeMillis()
)

data class PhonePasswordCredential(
    val fullPhone: String,
    val phoneCountryCode: String,
    val phoneNumber: String,
    val passwordHash: String,
    val recoveryEmail: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

enum class BusinessType(val labelTr: String) {
    CLOTHING("Giyim"),
    ELECTRONICS("Elektronik"),
    HOME_LIVING("Ev & Yaşam"),
    FOOD("Gıda"),
    COSMETICS("Kozmetik"),
    OTHER("Diğer")
}

enum class CurrencyOption(val code: String, val symbol: String, val labelTr: String) {
    TRY("TRY", "₺", "Türk Lirası (TRY - ₺)"),
    USD("USD", "$", "Amerikan Doları (USD - $)"),
    EUR("EUR", "€", "Euro (EUR - €)")
}

enum class CatalogLanguage(val code: String, val labelTr: String) {
    TR("tr", "Türkçe (TR)"),
    EN("en", "İngilizce (EN)"),
    DE("de", "Almanca (DE)"),
    AR("ar", "Arapça (AR)")
}

data class Catalog(
    val id: String,
    val title: String,
    val description: String,
    val slug: String,
    val coverImageUrl: String = "",
    val tags: List<String> = emptyList(),
    val isActive: Boolean = true,
    val productCount: Int = 0,
    val viewCount: Int = 0,
    val orderCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class StockStatus(val labelTr: String) {
    IN_STOCK("Stokta"),
    LOW_STOCK("Az kaldı"),
    OUT_OF_STOCK("Tükendi")
}

data class ProductVariantOption(
    val label: String,
    val extraPrice: Double = 0.0,
    val stock: Int = 10
)

data class ProductVariantGroup(
    val name: String,          // Örn: "Beden" veya "Renk"
    val options: List<String>, // Örn: ["S", "M", "L", "XL"]
    val optionDetails: List<ProductVariantOption> = options.map { ProductVariantOption(label = it) }
)

data class Product(
    val id: String,
    val catalogId: String,
    val name: String,
    val description: String,
    val price: Double,
    val originalPrice: Double? = null, // İndirimliyse üstü çizili eski fiyat
    val currency: String = "₺",
    val imageUrl: String = "",
    val galleryUrls: List<String> = emptyList(),
    val category: String = "Giyim",
    val subCategory: String = "Genel",
    val tags: List<String> = emptyList(),
    val trackStock: Boolean = true,
    val stockStatus: StockStatus = StockStatus.IN_STOCK,
    val stockCount: Int = 25,
    val inStock: Boolean = true,
    val sku: String = "",
    val variants: List<ProductVariantGroup> = emptyList(),
    val secretNote: String = "", // Müşteriye görünmeyen işletme içi gizli not
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class OrderStatus(val labelTr: String) {
    NEW("Yeni"),
    APPROVED("Onaylandı"),
    PREPARING("Hazırlanıyor"),
    SHIPPED("Kargoda"),
    DELIVERED("Teslim edildi"),
    CANCELLED("İptal")
}

data class OrderTimelineEntry(
    val id: String,
    val status: OrderStatus,
    val title: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class OrderItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val selectedVariantSummary: String = "",
    val imageUrl: String = ""
)

data class Order(
    val id: String,
    val orderNumber: String = id.uppercase(),
    val catalogId: String,
    val catalogTitle: String = "",
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String = "",
    val customerNote: String = "",
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val currency: String = "₺",
    val status: OrderStatus = OrderStatus.NEW,
    val timeline: List<OrderTimelineEntry> = emptyList(),
    val stockDeducted: Boolean = false,
    val resellerId: String = "",
    val resellerName: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val isResellerOrder: Boolean
        get() = resellerId.isNotBlank() || resellerName.isNotBlank()
}

data class CustomerCartItem(
    val key: String, // productId + selectedVariants
    val product: Product,
    val selectedVariants: Map<String, String> = emptyMap(),
    val unitPrice: Double,
    val quantity: Int = 1
) {
    val variantSummary: String
        get() = selectedVariants.entries.joinToString(" • ") { "${it.key}: ${it.value}" }
}

data class CatalogAnalyticsEvent(
    val id: String,
    val sessionId: String,
    val eventType: String, // "visit", "productView", "addToCart"
    val catalogId: String,
    val shortId: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 1,
    val referrer: String = "whatsapp", // "whatsapp", "direct", "other"
    val createdAt: Long = System.currentTimeMillis()
)

enum class AnalyticsDateRange(val labelTr: String, val days: Int) {
    TODAY("Bugün", 1),
    DAYS_7("7 gün", 7),
    DAYS_30("30 gün", 30),
    CUSTOM("Özel", 14)
}

data class AnalyticsSummary(
    val totalViews: Int = 0,
    val uniqueVisitors: Int = 0,
    val totalOrders: Int = 0,
    val totalRevenue: Double = 0.0,
    val conversionRatePercent: Double = 0.0
)

enum class InventoryFilter(val labelTr: String) {
    ALL("Tümü"),
    IN_STOCK("Stokta"),
    LOW_STOCK("Az kalan"),
    OUT_OF_STOCK("Tükendi")
}

enum class TeamRole(val labelTr: String, val descriptionTr: String) {
    ADMIN("Yönetici", "Tüm katalog, sipariş, stok, ekip ve abonelik yetkileri"),
    EDITOR("Editör", "Katalog, ürün ve stok düzenleyebilir; abonelik ve ekip yönetemez"),
    VIEWER("Sadece görüntüleme", "Katalogları, siparişleri ve analitiği yalnızca görüntüleyebilir")
}

data class TeamMember(
    val id: String,
    val name: String,
    val emailOrPhone: String,
    val role: TeamRole,
    val isOwner: Boolean = false,
    val statusText: String = "Aktif",
    val invitedAt: Long = System.currentTimeMillis()
)

enum class SubscriptionPlan(
    val titleTr: String,
    val periodLabelTr: String,
    val priceLabelTr: String,
    val maxCatalogs: Int?, // null = Sınırsız
    val maxProducts: Int?, // null = Sınırsız
    val badgeTr: String
) {
    FREE(
        titleTr = "Ücretsiz",
        periodLabelTr = "Başlangıç",
        priceLabelTr = "0 ₺ / ay",
        maxCatalogs = 2,
        maxProducts = 50,
        badgeTr = "Başlangıç"
    ),
    LITE_MONTHLY(
        titleTr = "Lite (Aylık)",
        periodLabelTr = "Aylık Plan",
        priceLabelTr = "299 ₺ / ay",
        maxCatalogs = 10,
        maxProducts = 500,
        badgeTr = "Popüler"
    ),
    PREMIUM_YEARLY(
        titleTr = "Premium (Yıllık)",
        periodLabelTr = "Yıllık Avantajlı",
        priceLabelTr = "2.490 ₺ / yıl",
        maxCatalogs = null,
        maxProducts = null,
        badgeTr = "En Avantajlı"
    );

    fun isCatalogLimitExceeded(currentCatalogCount: Int): Boolean {
        val limit = maxCatalogs ?: return false
        return currentCatalogCount >= limit
    }

    fun isProductLimitExceeded(currentProductCount: Int): Boolean {
        val limit = maxProducts ?: return false
        return currentProductCount >= limit
    }
}

fun defaultStoreTeamMembers(): List<TeamMember> = listOf(
    TeamMember(
        id = "team-owner",
        name = "Selin Kaya (Siz)",
        emailOrPhone = "selin@veratasarim.com",
        role = TeamRole.ADMIN,
        isOwner = true,
        statusText = "İşletme Sahibi"
    ),
    TeamMember(
        id = "team-editor-1",
        name = "Mert Demir",
        emailOrPhone = "mert@veratasarim.com",
        role = TeamRole.EDITOR,
        isOwner = false,
        statusText = "Aktif"
    ),
    TeamMember(
        id = "team-viewer-1",
        name = "Zeynep Arslan",
        emailOrPhone = "zeynep@veratasarim.com",
        role = TeamRole.VIEWER,
        isOwner = false,
        statusText = "Davet Gönderildi"
    )
)

data class StoreSettings(
    // 1) İşletme Profili
    val businessName: String = "Vera Tasarım Butik",
    val businessType: String = BusinessType.CLOTHING.labelTr,
    val logoUri: String = "https://picsum.photos/seed/vera-logo/300/300",
    val phoneWhatsapp: String = "+90 532 410 20 30",
    val city: String = "İstanbul",
    val businessAddress: String = "Bağdat Cad. Suadiye Mah. No: 142/B Kadıköy / İstanbul",
    val instagramUrl: String = "instagram.com/veratasarimbutik",
    val websiteUrl: String = "https://veratasarim.com",
    val facebookUrl: String = "facebook.com/veratasarimbutik",
    // 2) Mağaza Ayarları
    val currencyCode: String = CurrencyOption.TRY.code,
    val currencySymbol: String = CurrencyOption.TRY.symbol,
    val catalogLanguage: String = CatalogLanguage.TR.labelTr,
    val customSlug: String = "vera-tasarim-butik",
    val vatRatePercent: Int = 20,
    val showVatInPrices: Boolean = true,
    val minOrderAmount: Double = 500.0,
    val shippingFee: Double = 49.0,
    val orderConfirmationTemplate: String = "Merhaba {musteri_adi}, {siparis_no} numaralı {toplam_tutar} tutarındaki siparişiniz başarıyla alınmıştır. Hazırlandığında kargo bilgisi paylaşılacaktır.",
    // 3) Katalog Görünümü
    val catalogThemeColorHex: String = "#2563EB",
    val catalogGridColumns: Int = 2,
    val catalogShowPrices: Boolean = true,
    val protectImagesFromSaving: Boolean = true,
    // 4) Bildirimler
    val notificationsEnabled: Boolean = true,
    val notifyNewOrder: Boolean = true,
    val notifyLowStock: Boolean = true,
    val notifyWeeklySummary: Boolean = true,
    // 5) Stok, Ekip & Abonelik
    val lowStockThreshold: Int = 5,
    val subscriptionPlan: SubscriptionPlan = SubscriptionPlan.FREE,
    val teamMembers: List<TeamMember> = defaultStoreTeamMembers()
)

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val timeAgo: String,
    val isUnread: Boolean = true
)

enum class ShareOptionType(val labelTr: String) {
    LINK("Link olarak paylaş"),
    PDF_BROCHURE("Broşür (PDF) olarak paylaş"),
    IMAGE_CARD("Görsel olarak paylaş"),
    MULTI_CATALOG("Birden fazla kataloğu tek linkle paylaş")
}

enum class WatermarkPosition(val labelTr: String) {
    TOP_LEFT("Sol Üst"),
    TOP_RIGHT("Sağ Üst"),
    BOTTOM_LEFT("Sol Alt"),
    BOTTOM_RIGHT("Sağ Alt"),
    CENTER("Orta")
}

enum class DealerMarginType(val labelTr: String) {
    PERCENTAGE("Yüzde (%)"),
    FIXED_AMOUNT("Sabit Tutar")
}

enum class ResellerPriceMode(val labelTr: String) {
    RESELLER_ADD_MARGIN_PERCENT("Bayi Marjı Ekle (%)"),
    RESELLER_ADD_MARGIN_FIXED("Bayi Sabit Marj Ekle (₺)"),
    DISCOUNT_FOR_RESELLER("Bayi İndirimli Fiyatı (%)")
}

data class Reseller(
    val id: String,
    val name: String,
    val phone: String,
    val shortCode: String, // Örn: "bayi-anadolu" -> /c/bayi-anadolu
    val visibleCatalogIds: List<String> = emptyList(),
    val priceMode: ResellerPriceMode = ResellerPriceMode.RESELLER_ADD_MARGIN_PERCENT,
    val marginValue: Double = 15.0,
    val note: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun calculateResellerPrice(basePrice: Double): Double {
        if (marginValue == 0.0) return basePrice
        return when (priceMode) {
            ResellerPriceMode.RESELLER_ADD_MARGIN_PERCENT ->
                basePrice * (1.0 + marginValue / 100.0)
            ResellerPriceMode.RESELLER_ADD_MARGIN_FIXED ->
                (basePrice + marginValue).coerceAtLeast(1.0)
            ResellerPriceMode.DISCOUNT_FOR_RESELLER ->
                (basePrice * (1.0 - marginValue / 100.0)).coerceAtLeast(1.0)
        }
    }

    fun formatPriceRuleSummary(currencySymbol: String = "₺"): String {
        val cleanVal = if (marginValue % 1.0 == 0.0) marginValue.toInt().toString() else marginValue.toString()
        return when (priceMode) {
            ResellerPriceMode.RESELLER_ADD_MARGIN_PERCENT -> "+%$cleanVal Bayi Marjı"
            ResellerPriceMode.RESELLER_ADD_MARGIN_FIXED -> "+$cleanVal $currencySymbol Bayi Marjı"
            ResellerPriceMode.DISCOUNT_FOR_RESELLER -> "%$cleanVal Bayi İndirimi"
        }
    }
}

data class ShareSettings(
    val showPrices: Boolean = true,
    val hasPassword: Boolean = false,
    val password: String = "",
    val hasExpiryDate: Boolean = false,
    val expiryDateText: String = "",
    val isDealerShare: Boolean = false,
    val dealerMarginType: DealerMarginType = DealerMarginType.PERCENTAGE,
    val dealerMarginValue: Double = 15.0,
    val disableImageSaving: Boolean = true
) {
    fun calculateAdjustedPrice(basePrice: Double): Double {
        if (!isDealerShare || dealerMarginValue == 0.0) return basePrice
        return when (dealerMarginType) {
            DealerMarginType.PERCENTAGE -> basePrice * (1.0 + dealerMarginValue / 100.0)
            DealerMarginType.FIXED_AMOUNT -> (basePrice + dealerMarginValue).coerceAtLeast(0.0)
        }
    }
}

data class ShareEvent(
    val id: String,
    val targetType: String, // "catalog", "product", "collection"
    val targetTitle: String,
    val targetId: String,
    val channel: String, // "link_copy", "whatsapp", "pdf_download", "pdf_share", "image_share", "collection_link", "collection_whatsapp"
    val shortLink: String,
    val showPrices: Boolean = true,
    val isPasswordProtected: Boolean = false,
    val expiryDate: String? = null,
    val isDealerShare: Boolean = false,
    val dealerMarginType: String? = null,
    val dealerMarginValue: Double = 0.0,
    val catalogIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)


