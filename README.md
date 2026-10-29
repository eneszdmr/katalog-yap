# 📘 Katalog Yap — Dijital Ürün Kataloğu, Sipariş ve Bayi Yönetim Platformu

**Katalog Yap**, KOBİ'lerin, toptancıların, butiklerin ve üreticilerin dakikalar içinde profesyonel dijital ürün katalogları oluşturmasını, **WhatsApp / Kısa Link / PDF Broşür / Filigranlı Görsel** olarak paylaşmasını, müşterilerden **WhatsApp entegreli sipariş** toplamasını, **stok ve bayi (reseller)** ağını yönetmesini ve **Gemini AI** destekli araçlarla satışlarını büyütmesini sağlayan modern bir mobil & PWA uyumlu katalog platformudur.

---

## 🚀 1. Kurulum (Setup)

### Ön Gereksinimler
- **JDK:** 17 veya üzeri
- **Android SDK:** `compileSdk 35`, `minSdk 26`, `targetSdk 35`
- **Gradle:** Kotlin DSL (`.gradle.kts`)
- **Firebase Projesi:** Firebase AI Logic (Gemini API) ve App Check yapılandırılmış `app/google-services.json`

### API Anahtarları ve Güvenlik Yapılandırması
- Uygulama içerisinde **hiçbir API anahtarı istemci koduna (hardcoded) gömülmemiştir**.
- **Gemini AI Özellikleri:** `Firebase.ai(backend = GenerativeBackend.googleAI())` (`gemini-2.5-flash`) üzerinden güvenli sunucu/SDK katmanıyla veya ortam değişkeninden (`BuildConfig` / `System.getenv("GEMINI_API_KEY")`) okunarak çalışır.
- Özel ortam değişkenleri tanımlamak için AI Studio **Secrets** panelini veya `.env.example` şablonunu kullanabilirsiniz:
  ```env
  GEMINI_API_KEY=your_api_key_here
  ```

### Derleme ve Çalıştırma
```bash
# Debug APK derleme
gradle assembleDebug

# Birim ve Robolectric testlerini çalıştırma
gradle :app:testDebugUnitTest
```

---

## 📂 2. Klasör Yapısı (Folder Structure)

```text
├── manifest.json                                  # PWA Web App Manifest (Ana ekrana ekleme, kısayollar, tema)
├── README.md                                      # Proje dokümantasyonu, veri modeli ve mimari rehber
└── app/
    ├── google-services.json                       # Firebase proje ve AI Logic yapılandırması
    └── src/main/
        ├── AndroidManifest.xml                    # İzinler, klavye (adjustResize) ve kısayol bildirimleri
        ├── assets/
        │   └── manifest.json                      # İstemci içi PWA manifest varlığı
        ├── res/
        │   ├── values/strings.xml                 # Türkçe uygulama metin kaynakları ve erişilebilirlik etiketleri
        │   ├── xml/shortcuts.xml                  # Android Launcher sabit uygulama kısayolları
        │   └── mipmap-*/                          # Özel uyarlanabilir (Adaptive) uygulama ikonları
        └── java/com/example/
            ├── MainActivity.kt                    # App Check, yerel veri katmanı ve Coil Offline Cache başlatıcı
            ├── auth/
            │   ├── AuthContext.kt                 # Telefon + Şifre girişi, Google Girişi ve Firebase Şifre Sıfırlama yönetimi
            │   ├── AuthAccountRepository.kt       # Firebase Firestore (/users/{userId}) senkronizasyonu ve sendPasswordResetEmail
            │   └── FirebaseGoogleAuthHelper.kt    # Jetpack Credential Manager (GetSignInWithGoogleOption) entegrasyonu
            ├── i18n/
            │   └── TrStrings.kt                   # Merkezi Türkçe metin sabitleri
            ├── model/
            │   └── CatalogModels.kt               # Tüm veri modelleri (Catalog, Product, Order, Visit, Reseller vb.)
            ├── navigation/
            │   └── AppRouter.kt                   # NavHost, Slide geçiş animasyonları, Korumalı rotalar, 404 ve ErrorBoundary
            ├── services/
            │   ├── AppCheckConfiguration.kt       # Firebase App Check başlatma
            │   ├── DataService.kt                 # Veri katmanı arayüzü ve durum akışları (StateFlow)
            │   ├── LocalStorageDataService.kt     # Kalıcı JSON + SharedPreferences + Offline önbellek motoru
            │   └── GeminiProductService.kt        # Gemini 2.5 Flash AI servisi (4 temel yapay zeka özelliği)
            ├── ui/
            │   ├── components/
            │   │   ├── KYButton.kt                # Spring basma efektli (0.96x scale) erişilebilir buton
            │   │   ├── KYInput.kt                 # XSS/Script temizleyicili (InputSanitizer) metin giriş bileşeni
            │   │   ├── KYOverlay.kt               # Spring animasyonlu ve klavye güvenli (imePadding) BottomSheet & Modal
            │   │   ├── KYFeedback.kt              # Boş durum (EmptyState), Rozet (Badge) ve Skeleton yükleyiciler
            │   │   ├── KYNavigation.kt            # Üst çubuk, Alt sekme çubuğu ve Mobil Çerçeve
            │   │   ├── SalesAssistantChatCard.kt  # Siparişler ekranı için AI Satış Asistanı sohbet kutusu
            │   │   └── ErrorBoundaryAndNotFound.kt# Global Hata Sınırı (ErrorBoundary), 404 ekranı ve Offline çubuğu
            │   ├── screens/
            │   │   ├── CatalogsDraftScreen.kt     # Kataloglar ana ekranı, etiket filtreleme, düşük stok uyarısı
            │   │   ├── OrdersDraftScreen.kt       # Siparişler sekmesi (/orders), durum filtreleri, AI Satış Asistanı
            │   │   ├── AnalyticsDraftScreen.kt    # Analitik sekmesi (/analytics), grafikler, AI haftalık özet
            │   │   ├── SettingsDraftScreen.kt     # Ayarlar sekmesi (/settings), ekip, abonelik, PWA ve bayi özeti
            │   │   ├── auth/                      # Splash, Login, VerifyOtp ve 3 adımlı Onboarding ekranları
            │   │   ├── catalogs/                  # Katalog detay, Ürün ekleme/düzenleme, AI fotoğraflardan katalog, Paylaşım Sheet'i
            │   │   ├── inventory/                 # Stok yönetimi (/inventory), hızlı +/- güncelleme, CSV içe/dışa aktarma
            │   │   ├── orders/                    # Sipariş detayı (/orders/:id), durum zaman çizelgesi, PDF fiş/fatura
            │   │   ├── publiccatalog/             # Herkese açık müşteri kataloğu (/c/:shortId), sepet ve WhatsApp sipariş
            │   │   ├── resellers/                 # Bayi (Reseller) yönetimi, özel marj/indirim ve bayi link üretimi
            │   │   └── settings/                  # İşletme profili, abonelik yükseltme, ekip davet ve hesap silme modalları
            │   └── theme/                         # Material 3 renk paleti (#2563EB, #F59E0B), tipografi ve şekiller
            └── util/
                ├── InputSanitizer.kt              # XSS, HTML tag, script ve kontrol karakteri temizleyici
                ├── PwaOfflineAndShortcutHelper.kt # "Ana ekrana ekle" kısayol sabitleyici ve 50 MB Offline görsel önbelleği
                └── ShareExportGenerator.kt        # PDF Broşür (PdfDocument) ve Filigranlı Ürün Görseli (Canvas) üretici
```

---

## 🗄️ 3. Veri Modeli (Data Models)

Uygulama verileri hem yerel çevrimdışı önbellekte (`LocalStorageDataService`) hem de bulut senkronizasyonuna uygun koleksiyon yapısında aşağıdaki 6 ana varlık (+ yardımcı analitik/bayi tabloları) üzerinde modellenmiştir:

### 3.1. `users` (Kullanıcılar & Ekip Üyeleri)
İşletme sahibini ve mağaza ekibine davet edilen kullanıcıları temsil eder (`AuthState` & `TeamMember`).
| Alan | Tip | Açıklama |
| :--- | :--- | :--- |
| `id` | `String` | Benzersiz kullanıcı kimliği (örn. `usr-owner`) |
| `name` | `String` | Ad soyad |
| `phoneOrEmail` | `String` | Doğrulanmış telefon numarası veya e-posta |
| `role` | `TeamRole` | Yetki rolü: `ADMIN` (Yönetici), `EDITOR` (Editör), `VIEWER` (Sadece görüntüleme) |
| `statusTr` | `String` | Üyelik durumu (`Aktif`, `Davet Gönderildi`) |
| `invitedAtMillis` | `Long` | Oluşturulma / davet edilme zaman damgası |

### 3.2. `businesses` (İşletme & Mağaza Ayarları)
İşletmenin kurumsal kimliğini, vergi/kargo kurallarını, katalog görünüm tercihlerini ve abonelik planını tutar (`StoreSettings`).
| Alan | Tip | Açıklama |
| :--- | :--- | :--- |
| `businessName` | `String` | İşletme / mağaza adı |
| `businessType` | `String` | Sektör / işletme türü (Butik, Toptan, Ev & Yaşam vb.) |
| `logoUri` | `String` | İşletme logosu URL veya URI'si |
| `phoneWhatsapp` | `String` | Siparişlerin yönlendirileceği WhatsApp hattı (`905...`) |
| `businessAddress` | `String` | Açık işletme adresi |
| `instagramUrl`, `websiteUrl`, `facebookUrl` | `String` | Sosyal medya ve web bağlantıları |
| `currencyCode`, `currencySymbol` | `String` | Para birimi (`TRY`/`₺`, `USD`/`$`, `EUR`/`€`) |
| `vatRate`, `showVatInPrices` | `Int`, `Boolean` | KDV oranı (%) ve fiyatlarda KDV dahil gösterim anahtarı |
| `minOrderAmount`, `shippingFee` | `Double` | Minimum sipariş tutarı ve standart kargo ücreti |
| `catalogThemeColorHex`, `catalogGridColumns` | `String`, `Int` | Katalog vitrin tema rengi ve grid sütun sayısı |
| `protectImagesFromSaving` | `Boolean` | Müşteri sayfasında sağ tık / uzun basma ve ekran görüntüsü koruması |
| `subscriptionPlan` | `SubscriptionPlan` | `FREE` (Max 2 katalog, 50 ürün), `LITE_MONTHLY`, `PREMIUM_YEARLY` |

### 3.3. `catalogues` (Kataloglar)
Ürünlerin gruplandığı ve müşterilerle paylaşılan dijital katalogları temsil eder (`Catalog`).
| Alan | Tip | Açıklama |
| :--- | :--- | :--- |
| `id` | `String` | Benzersiz katalog kimliği (`cat-1`) |
| `title` | `String` | Katalog başlığı |
| `description` | `String` | Katalog alt açıklaması |
| `category` | `String` | Ana kategori |
| `coverImageUrl` | `String` | Kapak görseli URL'si |
| `isActive` | `Boolean` | **Yayında mı?** (`false` ise müşteri sayfasında `/c/:shortId` görüntülenemez) |
| `slug` | `String` | Kısa bağlantı kodu (`/c/ilkbahar-2025`) |
| `tags` | `List<String>` | Katalog etiketleri (`Yeni Sezon`, `Toptan`, `İndirim`) |
| `viewCount`, `shareCount` | `Int` | Toplam görüntülenme ve paylaşım sayaçları |
| `updatedAtMillis` | `Long` | Son güncellenme zamanı |

### 3.4. `products` (Ürünler & Stok)
Kataloglarda yer alan ürünleri, varyantlarını ve gerçek zamanlı stok durumunu temsil eder (`Product`).
| Alan | Tip | Açıklama |
| :--- | :--- | :--- |
| `id` | `String` | Benzersiz ürün kimliği (`prd-101`) |
| `catalogId` | `String` | Bağlı olduğu katalog kimliği |
| `name`, `sku` | `String` | Ürün adı ve stok kodu (SKU) |
| `description` | `String` | Ürün açıklaması (Gemini AI ile Kurumsal/Samimi/Kısa tonda iyileştirilebilir) |
| `price`, `originalPrice` | `Double`, `Double?` | Satış fiyatı ve varsa indirimsiz liste fiyatı |
| `currency` | `String` | Para birimi sembolü |
| `imageUrl`, `galleryUrls` | `String`, `List<String>` | Vitrin görseli ve yakınlaştırılabilir galeri görselleri |
| `subCategory`, `tags` | `String`, `List<String>` | Alt kategori ve arama etiketleri |
| `variants` | `List<ProductVariantGroup>` | Varyant grupları (Beden, Renk, Materyal + fiyat farkı ve stok durumu) |
| `stockCount`, `trackStock`, `stockStatus` | `Int`, `Boolean`, `StockStatus` | Stok adedi, otomatik stok düşürme anahtarı ve durum (`IN_STOCK`, `LOW_STOCK`, `OUT_OF_STOCK`) |
| `sortOrder` | `Int` | Katalog içi sıralama indeksi |

### 3.5. `orders` (Siparişler)
Müşteri katalog sayfasından veya bayi bağlantılarından gelen siparişleri temsil eder (`Order`).
| Alan | Tip | Açıklama |
| :--- | :--- | :--- |
| `id` | `String` | Sipariş numarası (`ord-901`) |
| `catalogId`, `catalogTitle` | `String` | Siparişin verildiği katalog bilgisi |
| `customerName`, `customerPhone` | `String` | Temizlenmiş (sanitized) müşteri adı soyadı ve telefon numarası |
| `customerAddress`, `customerNote` | `String` | Teslimat adresi ve müşteri sipariş notu |
| `items` | `List<OrderItem>` | Sipariş kalemleri (`productId`, `productName`, `quantity`, `unitPrice`, `selectedVariantSummary`) |
| `totalAmount`, `currency` | `Double`, `String` | Toplam sipariş tutarı ve para birimi |
| `status` | `OrderStatus` | `NEW`, `APPROVED`, `PREPARING`, `SHIPPED`, `DELIVERED`, `CANCELLED` |
| `timeline` | `List<OrderTimelineEntry>` | Durum değişikliklerinin tarihçesini tutan zaman çizelgesi |
| `resellerId`, `resellerName` | `String` | Sipariş bir bayi özel linkinden geldiyse bayi kimliği ve adı |
| `createdAtMillis` | `Long` | Sipariş oluşturulma zamanı |

### 3.6. `visits` (Ziyaretler & Analitik Kayıtları)
Müşteri etkileşimlerini oturum bazında (`sessionId`) tekilleştirerek izleyen analitik koleksiyonlarıdır (`CatalogVisitEvent`, `ProductViewEvent`, `AddToCartEvent`, `ShareEvent`).
| Alan | Tip | Açıklama |
| :--- | :--- | :--- |
| `id` | `String` | Olay kimliği (`vis-...`, `pvw-...`, `atc-...`) |
| `sessionId` | `String` | Aynı oturumu tekrar saymamak için kullanılan oturum anahtarı |
| `catalogId`, `shortId` | `String` | Ziyaret edilen katalog ve kısa link |
| `referrer` | `TrafficReferrer` | Trafik kaynağı: `WHATSAPP`, `DIRECT_LINK`, `OTHER` |
| `timestampMillis` | `Long` | Olayın gerçekleştiği zaman damgası |

---

## ✨ 4. Öne Çıkan Özellikler (Son Hali)

1. **Tam Kapsamlı UI/UX & Erişilebilirlik:**
   - Tüm ekranlarda tutarlı Material 3 boşlukları (8.dp grid), metin taşma korumaları (`TextOverflow.Ellipsis`), `KYEmptyState` boş durum ekranları ve `KYSkeletonCard` yükleme iskeletleri.
   - Ekran okuyucular (TalkBack / `aria-label`) için `semantics { contentDescription = ... }` etiketleri.
   - Klavye açıldığında bozulmayan form ve alt çekmeceler (`windowSoftInputMode="adjustResize"` + `imePadding()`).
2. **Akıcı Animasyonlar:**
   - Sayfa geçişlerinde yatay `slideInHorizontally` / `slideOutHorizontally` + `fadeIn` / `fadeOut` animasyonları.
   - `KYBottomSheet` ve `KYModal` açılışlarında yaylanma (`Spring.DampingRatioMediumBouncy`) animasyonu.
   - `KYButton` bileşeninde dokunsal basma küçülme efekti (`0.96f` spring scale).
3. **PWA, Ana Ekrana Ekle & Offline Önbellek:**
   - `manifest.json` ve Android `shortcuts.xml` desteği.
   - Müşteri kataloğu ve Ayarlar ekranından tek dokunuşla **"Ana Ekrana Ekle"** (`ShortcutManagerCompat.requestPinShortcut`).
   - **50 MB Coil Disk Cache + Memory Cache** ile çevrimdışı görsel önbellekleme ve yerel JSON veri önbelleği.
4. **Performans & Sayfalama (Pagination):**
   - Görseller için `crossfade` destekli Lazy Loading.
   - Uzun ürün listelerinde sayfalama (`pageSize = 8` + *"Daha Fazla Ürün Göster"*).
5. **Hata Sınırı (`KYErrorBoundary`) & 404 Sayfası (`NotFound404Screen`):**
   - Beklenmeyen hataları yakalayan kurtarma ekranı ve geçersiz rotalar için özel **404 Sayfa Bulunamadı** ekranı.
6. **Güvenlik:**
   - `InputSanitizer` ile tüm kullanıcı girdilerinde XSS / `<script>` / HTML etiket temizliği.
   - Müşteri sayfasında (`/c/:shortId`) **yalnızca yayında olan (`isActive == true`) katalogların görüntülenmesi**; gizlenen kataloglarda otomatik erişim engeli ekranı.

---

## 🛣️ 5. Sonraki Adımlar (Next Steps)

- **Gerçek Ödeme Geçidi Entegrasyonu:** Abonelik yükseltme (`Lite` / `Premium`) ve müşteri sepeti için iyzico / Stripe sanal POS entegrasyonu.
- **Çoklu Cihaz Gerçek Zamanlı Firestore Senkronizasyonu:** Ekip üyeleri arasında anlık stok ve sipariş bildirimleri için FCM (Firebase Cloud Messaging) push bildirimleri.
- **Barkod / QR Kod Okuyucu:** Kamera ile ürün barkodu okutarak hızlı stok düşürme ve ürün arama.
- **Çoklu Dil Desteği (i18n):** İhracat yapan toptancılar için İngilizce, Arapça ve Almanca katalog vitrin dili seçimi.
