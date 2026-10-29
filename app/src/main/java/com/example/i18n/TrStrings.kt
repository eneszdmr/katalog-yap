package com.example.i18n

/**
 * Tüm uygulama arayüz metinlerini barındıran merkezi Türkçe dil dosyası (i18n/tr).
 */
object TrStrings {
    const val APP_NAME = "KATALOG YAP"
    const val APP_TAGLINE = "Mobil Dijital Katalog & Satış Platformu"

    object Navigation {
        const val TAB_CATALOGS = "Kataloglar"
        const val TAB_ORDERS = "Siparişler"
        const val TAB_ANALYTICS = "Analitik"
        const val TAB_SETTINGS = "Ayarlar"
        const val BACK = "Geri"
        const val DESIGN_SYSTEM = "Tasarım Sistemi"
    }

    object Common {
        const val SAVE = "Kaydet"
        const val CANCEL = "Vazgeç"
        const val CONFIRM = "Onayla"
        const val CLOSE = "Kapat"
        const val CREATE = "Oluştur"
        const val EDIT = "Düzenle"
        const val DELETE = "Sil"
        const val SHARE = "Paylaş"
        const val NEXT = "Devam Et"
        const val PREVIOUS = "Geri"
        const val SEARCH_PLACEHOLDER = "Ara..."
        const val LOADING = "Yükleniyor..."
        const val ACTIVE = "Yayında"
        const val HIDDEN = "Gizli"
        const val DRAFT = "Taslak"
        const val NEW_BADGE = "Yeni"
        const val PRO_BADGE = "SaaS İskeleti"
        const val EMPTY_LIST_ACTION = "İlk Kaydı Oluştur"
        const val PREVIEW_COMPONENTS = "Bileşenleri Test Et"
        const val STORAGE_MODE_LOCAL = "Veri Katmanı: LocalStorage (DataService)"
        const val FIREBASE_READY = "Firebase Geçişine Hazır"
    }

    object Auth {
        // 1) SPLASH
        const val SPLASH_SUBTITLE = "İşletmeni dijitale taşı, saniyeler içinde satışa başla"
        const val SPLASH_LOADING = "Oturum kontrol ediliyor..."

        // 2) LOGIN (/login)
        const val LOGIN_HEADLINE = "Kataloğunu telefonunda oluştur, müşterinle paylaş"
        const val LOGIN_SUBTITLE = "Hızlıca giriş yapın veya yeni işletme hesabınızı oluşturun"
        const val PHONE_LABEL = "Telefon Numarası"
        const val PHONE_PLACEHOLDER = "5XX XXX XX XX"
        const val COUNTRY_CODE_TITLE = "Ülke Kodu Seçin"
        const val CONTINUE_BUTTON = "Devam Et"
        const val OR_DIVIDER = "veya"
        const val CONTINUE_WITH_APPLE = "Apple ile devam et"
        const val CONTINUE_WITH_GOOGLE = "Google ile devam et"
        const val DEMO_BADGE = "Demo"
        const val LEGAL_PREFIX = "Devam ederek "
        const val TERMS_LINK = "Kullanım Koşulları"
        const val LEGAL_AND = " ve "
        const val PRIVACY_LINK = "Gizlilik Politikası"
        const val LEGAL_SUFFIX = "'nı kabul etmiş olursunuz."
        const val ERROR_INVALID_PHONE = "Lütfen geçerli bir telefon numarası girin (en az 10 hane)"
        const val TERMS_MODAL_TITLE = "Kullanım Koşulları"
        const val TERMS_MODAL_BODY = "KATALOG YAP hizmeti üzerinden oluşturduğunuz dijital ürün katalogları, fiyatlandırma bilgileri ve müşteri sipariş verileri işletmenizin sorumluluğundadır. Platformu yasal ticaret kurallarına uygun şekilde kullanmayı kabul edersiniz."
        const val PRIVACY_MODAL_TITLE = "Gizlilik Politikası"
        const val PRIVACY_MODAL_BODY = "Telefon numaranız, mağaza bilgileriniz ve katalog verileriniz güvenli veri katmanında (DataService) saklanır ve üçüncü taraflarla reklam amacıyla paylaşılmaz."

        // 3) OTP VERIFY (/verify)
        const val OTP_TITLE = "Doğrulama Kodunu Girin"
        const val OTP_SUBTITLE_PREFIX = "Telefonunuza ("
        const val OTP_SUBTITLE_SUFFIX = ") gönderilen 6 haneli SMS kodunu girin."
        const val OTP_DEMO_HINT_TITLE = "Demo Doğrulama İpucu"
        const val OTP_DEMO_HINT_DESC = "Test için her zaman 123456 kodunu kullanabilirsiniz."
        const val OTP_AUTO_FILL = "123456 Doldur"
        const val OTP_VERIFY_BUTTON = "Doğrula ve Devam Et"
        const val OTP_RESEND_COUNTDOWN = "Kodu tekrar gönder"
        const val OTP_RESEND_ACTIVE = "Kodu tekrar gönder"
        const val OTP_RESENT_TOAST = "Yeni doğrulama kodu gönderildi (Demo: 123456)"
        const val OTP_ERROR_INVALID = "Hatalı doğrulama kodu. Demo için 123456 girin."
        const val OTP_CHANGE_NUMBER = "Numarayı Değiştir"

        // 4) ONBOARDING (/onboarding)
        const val ONBOARDING_TOP_TITLE = "İşletme Kurulumu"
        const val STEP_1_TITLE = "İşletme Bilgileri"
        const val STEP_1_SUBTITLE = "Müşterilerinizin katalog başlığında göreceği marka kimliği"
        const val BUSINESS_NAME_LABEL = "İşletme / Marka Adı"
        const val BUSINESS_NAME_PLACEHOLDER = "Örn: Vera Butik, Kuzey Elektronik"
        const val BUSINESS_TYPE_LABEL = "İşletme Türü"
        const val ERROR_BUSINESS_NAME = "Lütfen işletme veya marka adınızı girin"

        const val STEP_2_TITLE = "Logo & İletişim"
        const val STEP_2_SUBTITLE = "Siparişlerin iletileceği WhatsApp hattı ve konum bilgisi"
        const val LOGO_UPLOAD_LABEL = "İşletme Logosu"
        const val LOGO_PICK_GALLERY = "Galeriden Seç"
        const val LOGO_REMOVE = "Kaldır"
        const val LOGO_HELPER = "Dairesel mağaza profil görseli"
        const val WHATSAPP_LABEL = "WhatsApp Sipariş Numarası"
        const val WHATSAPP_PLACEHOLDER = "+90 555 000 00 00"
        const val CITY_LABEL = "Şehir"
        const val CITY_PLACEHOLDER = "Örn: İstanbul, Ankara, İzmir"
        const val ERROR_WHATSAPP = "Lütfen geçerli bir WhatsApp numarası girin"
        const val ERROR_CITY = "Lütfen bulunduğunuz şehri girin"

        const val STEP_3_TITLE = "Bölgesel Tercihler"
        const val STEP_3_SUBTITLE = "Kataloglarınızda kullanılacak varsayılan para birimi ve dil"
        const val CURRENCY_LABEL = "Para Birimi"
        const val LANGUAGE_LABEL = "Katalog Dili"
        const val FINISH_ONBOARDING_BUTTON = "Kataloğunu Oluştur"
        const val ONBOARDING_COMPLETED_TOAST = "İşletme kurulumu tamamlandı! Hoş geldiniz."

        const val LOGOUT_BUTTON = "Oturumu Kapat (Çıkış Yap)"
        const val LOGOUT_TOAST = "Oturum kapatıldı"
    }

    object Catalogs {
        const val TITLE = "Kataloglar"
        const val SUBTITLE = "Ürün kataloglarınızı oluşturun ve müşterilerinizle paylaşın"
        const val SEARCH_PLACEHOLDER = "Katalog adına göre ara..."
        const val TAG_ALL = "Tümü"
        const val DEFAULT_TAG_NEW_SEASON = "Yeni Sezon"
        const val DEFAULT_TAG_DISCOUNT = "İndirimli"
        const val DEFAULT_TAG_BESTSELLER = "Çok Satanlar"
        const val DEFAULT_TAG_WHOLESALE = "Toptan"

        const val PRODUCT_COUNT_SUFFIX = "ürün"
        const val SHARE_CONTENT_DESC = "Kataloğu Paylaş"
        const val MORE_MENU_CONTENT_DESC = "Katalog İşlemleri"

        // Kart ⋯ Menüsü
        const val MENU_EDIT = "Düzenle"
        const val MENU_DUPLICATE = "Kopyala"
        const val MENU_ADD_TAG = "Etiket ekle"
        const val MENU_HIDE = "Gizle"
        const val MENU_PUBLISH = "Yayınla"
        const val MENU_DELETE = "Sil"

        // Çoklu Seçim Modu (Uzun Basma)
        const val MULTI_SELECT_COUNT_SUFFIX = "seçildi"
        const val ACTION_ADD_TAG = "Etiket ekle"
        const val ACTION_MERGE = "Birleştir"
        const val ACTION_DELETE = "Sil"
        const val MULTI_SELECT_HINT = "Kartlara uzun basarak çoklu seçim yapabilirsiniz"

        // Boş Durum
        const val EMPTY_TITLE = "Henüz hiç kataloğunuz yok"
        const val EMPTY_DESCRIPTION = "Ürünlerinizi saniyeler içinde dijital kataloğa dönüştürün, özel link ile WhatsApp ve sosyal medyada paylaşın."
        const val EMPTY_SEARCH_TITLE = "Aramanızla eşleşen katalog bulunamadı"
        const val EMPTY_SEARCH_DESCRIPTION = "Farklı bir anahtar kelime deneyin veya etiket filtresini temizleyin."
        const val FIRST_CATALOG_BUTTON = "İlk kataloğunu oluştur"
        const val LOAD_SAMPLE_CATALOGS = "Örnek Katalogları Yükle"

        // Sağ Alt (+) FAB Bottom Sheet Seçenekleri
        const val FAB_SHEET_TITLE = "Yeni Katalog Oluştur"
        const val FAB_SHEET_SUBTITLE = "Kataloğunuzu nasıl oluşturmak istersiniz?"
        const val CREATE_EMPTY_TITLE = "Boş katalog oluştur"
        const val CREATE_EMPTY_DESC = "Sıfırdan başlık, kapak ve ürünlerinizi tek tek ekleyin"
        const val CREATE_FROM_PHOTOS_TITLE = "Fotoğraflardan katalog oluştur"
        const val CREATE_FROM_PHOTOS_DESC = "Galeriden toplu ürün fotoğrafı seçerek otomatik katalog kurun"
        const val IMPORT_EXCEL_CSV_TITLE = "Excel/CSV'den içe aktar"
        const val IMPORT_EXCEL_CSV_DESC = "Ürün adları, stok kodları ve fiyat listesini tek tıkla aktarın"

        // Modallar & Bildirimler
        const val NOTIFICATIONS_TITLE = "Bildirimler"
        const val NOTIFICATIONS_SUBTITLE = "Son ziyaretçi ve sipariş hareketleri"
        const val NOTIFICATIONS_MARK_READ = "Tümünü Okundu İşaretle"

        const val MODAL_CREATE_TITLE = "Boş Katalog Oluştur"
        const val MODAL_EDIT_TITLE = "Kataloğu Düzenle"
        const val INPUT_CATALOG_NAME_LABEL = "Katalog Adı"
        const val INPUT_CATALOG_NAME_PLACEHOLDER = "Örn: 2025 İlkbahar / Yaz Koleksiyonu"
        const val INPUT_CATALOG_DESC_LABEL = "Açıklama"
        const val INPUT_CATALOG_DESC_PLACEHOLDER = "Katalog hakkında kısa bilgi"
        const val INPUT_CATALOG_TAG_LABEL = "Etiket (İsteğe Bağlı)"
        const val INPUT_CATALOG_TAG_PLACEHOLDER = "Örn: Yeni Sezon"

        const val MODAL_TAG_TITLE = "Etiket ekle"
        const val MODAL_TAG_DESC = "Mevcut etiket çiplerini seçin veya yeni etiket yazıp 'Uygula' ile kaydedin."
        const val INPUT_TAG_LABEL = "Yeni Etiket Yaz"
        const val INPUT_TAG_PLACEHOLDER = "Örn: Kampanya, Kış Sezonu"
        const val EXISTING_TAGS_LABEL = "Mevcut Etiketler (Çip Seçin)"
        const val BTN_ADD_NEW_TAG_CHIP = "+ Ekle"
        const val BTN_APPLY_TAGS = "Uygula"

        const val CREATE_CUSTOM_FROM_PRODUCTS_TITLE = "Farklı kataloglardan özel katalog oluştur"
        const val CREATE_CUSTOM_FROM_PRODUCTS_DESC = "Farklı kataloglardaki ürünleri seçerek tek bir yeni katalogda veya mevcut katalogda toplayın"

        const val MODAL_MERGE_TITLE = "Katalogları Birleştir"
        const val MODAL_MERGE_DESC = "Seçilen kataloglardaki tüm ürünler ve metrikler yeni bir katalog altında birleştirilecek."
        const val INPUT_MERGE_NAME_LABEL = "Birleştirilmiş Katalog Adı"
        const val INPUT_MERGE_NAME_PLACEHOLDER = "Örn: Birleşik Sezon Kataloğu"

        const val MODAL_DELETE_TITLE = "Katalog Silinsin mi?"
        const val MODAL_DELETE_DESC = "Seçili katalog ve içindeki ürün bağlantıları kalıcı olarak kaldırılacak."

        const val MODAL_PHOTOS_TITLE = "Fotoğraflardan Katalog Oluştur"
        const val MODAL_PHOTOS_DESC = "Katalog adını girin ve galerinizden ürün fotoğraflarını seçin (veya hızlı demo fotoğraflarıyla oluşturun)."
        const val PICK_PHOTOS_BTN = "Galeriden Fotoğraf Seç"

        const val MODAL_CSV_TITLE = "Excel / CSV'den İçe Aktar"
        const val MODAL_CSV_DESC = "Her satıra 'Ürün Adı; Fiyat; Stok Kodu' formatında ürünlerinizi yapıştırın veya hazır şablonu kullanın."
        const val INPUT_CSV_LABEL = "CSV / Tablo İçeriği"

        const val SHARE_SHEET_TITLE = "Kataloğu Müşterinle Paylaş"
        const val SHARE_COPY_LINK = "Katalog Linkini Kopyala"
        const val SHARE_WHATSAPP = "WhatsApp ile Gönder"
        const val SHARE_SYSTEM = "Diğer Uygulamalarla Paylaş"

        // Toast Mesajları
        const val TOAST_CREATED = "Yeni katalog başarıyla oluşturuldu"
        const val TOAST_UPDATED = "Katalog bilgileri güncellendi"
        const val TOAST_DUPLICATED = "Kataloğun kopyası oluşturuldu"
        const val TOAST_TAG_ADDED = "Katalog etiketleri uygulandı"
        const val TOAST_VISIBILITY_CHANGED = "Katalog yayın durumu güncellendi"
        const val TOAST_DELETED = "Seçili katalog(lar) silindi"
        const val TOAST_MERGED = "Seçili kataloglar başarıyla birleştirildi"
        const val TOAST_MERGE_MIN_ERROR = "Birleştirmek için en az 2 katalog seçmelisiniz"
        const val TOAST_LINK_COPIED = "Katalog paylaşım linki panoya kopyalandı!"
    }

    object CatalogDetail {
        const val METRIC_VISITORS = "Ziyaretçi"
        const val METRIC_ORDERS = "Sipariş"
        const val METRIC_PRODUCTS = "Ürün Sayısı"

        const val BTN_SHARE = "Paylaş"
        const val BTN_MANAGE = "Yönet"
        const val BTN_MANAGE_DONE = "Yönetimi Bitir"
        const val MANAGE_BANNER_HINT = "Yönet Modu: Ürünleri sürükleyerek veya ok butonlarıyla yeniden sıralayabilirsiniz."

        // Görünüm Değiştirici
        const val VIEW_GRID = "Grid"
        const val VIEW_LIST = "Liste"
        const val VIEW_LARGE = "Büyük Kart"

        // Arama & Sıralama
        const val SEARCH_PRODUCTS_PLACEHOLDER = "Ürün adı veya SKU ara..."
        const val SORT_NEWEST = "Yeni"
        const val SORT_PRICE_ASC = "Fiyat artan"
        const val SORT_PRICE_DESC = "Fiyat azalan"
        const val SORT_NAME_AZ = "A-Z"
        const val SORT_CUSTOM = "Özel Sıra"

        // Çoklu Seçim Aksiyonları (Ürüne Uzun Basınca)
        const val ACTION_CREATE_NEW_CATALOG = "Yeni katalog oluştur"
        const val ACTION_ADD_TO_CATALOG_SHEET = "Kataloğa ekle"
        const val ACTION_ADD_TO_NEW_CATALOG = "Yeni katalog oluştur"
        const val ACTION_MOVE_TO_CATALOG = "Başka kataloğa taşı"
        const val ACTION_DELETE_PRODUCTS = "Sil"
        const val ACTION_UPDATE_STOCK = "Stok güncelle"

        // Sağ Alt (+) FAB
        const val FAB_ADD_PRODUCT = "Ürün ekle"

        // Modallar & Sheet'ler
        const val MODAL_ADD_PRODUCT_TITLE = "Yeni Ürün Ekle"
        const val MODAL_EDIT_PRODUCT_TITLE = "Ürünü Düzenle"
        const val INPUT_PROD_NAME = "Ürün Adı"
        const val INPUT_PROD_PRICE = "Satış Fiyatı (₺)"
        const val INPUT_PROD_ORIGINAL_PRICE = "İndirimsiz Eski Fiyat (İsteğe Bağlı)"
        const val INPUT_PROD_SUBCATEGORY = "Alt Kategori"
        const val INPUT_PROD_SKU = "Stok Kodu (SKU)"
        const val INPUT_PROD_DESC = "Ürün Açıklaması"
        const val INPUT_PROD_STOCK_STATUS = "Stok Durumu"

        const val MODAL_NEW_CAT_FROM_PRODS_TITLE = "Yeni katalog oluştur"
        const val MODAL_NEW_CAT_FROM_PRODS_DESC = "Seçilen ürünler yeni oluşturulacak kataloğa kopyalanacak. Lütfen katalog adını girin."

        const val SHEET_ADD_TO_CATALOG_TITLE = "Kataloğa ekle"
        const val SHEET_ADD_TO_CATALOG_SUBTITLE = "Seçili ürünleri mevcut bir kataloğa kopyalayın veya yeni bir katalog oluşturun"
        const val MODE_CREATE_NEW_CATALOG = "Yeni katalog oluştur"
        const val MODE_SELECT_EXISTING_CATALOG = "Mevcut katalog seç"
        const val BTN_COPY_TO_SELECTED_CATALOG = "Seçili Kataloğa Kopyala"
        const val BTN_CREATE_AND_COPY_CATALOG = "Yeni Katalog Oluştur ve Kopyala"
        const val CROSS_CATALOG_PICKER_TITLE = "Farklı Kataloglardan Ürün Seç"
        const val CROSS_CATALOG_PICKER_SUBTITLE = "Birden fazla katalogdan dilediğiniz ürünleri seçerek tek bir özel katalog oluşturun"

        const val MODAL_MOVE_PRODS_TITLE = "Başka Kataloğa Taşı"
        const val MODAL_MOVE_PRODS_DESC = "Seçilen ürünlerin taşınacağı hedef kataloğu seçin."

        const val MODAL_STOCK_UPDATE_TITLE = "Stok Durumunu Güncelle"
        const val MODAL_STOCK_UPDATE_DESC = "Seçilen ürünler için yeni stok rozetini seçin."

        const val TOAST_PRODUCT_SAVED = "Ürün başarıyla kaydedildi"
        const val TOAST_PRODUCTS_DELETED = "Seçili ürünler silindi"
        const val TOAST_STOCK_UPDATED = "Stok durumu güncellendi"
        const val TOAST_MOVED_TO_CATALOG = "Ürünler seçilen kataloğa taşındı"
        const val TOAST_COPIED_TO_NEW_CATALOG = "Seçili ürünler yeni kataloğa kopyalandı"
        const val TOAST_COPIED_TO_EXISTING_CATALOG = "Seçili ürünler mevcut kataloğa kopyalandı"
        const val TOAST_ORDER_SAVED = "Ürün sıralaması güncellendi"
    }

    object ProductDetail {
        const val TOP_TITLE = "Ürün Detayı"
        const val SKU_LABEL = "Stok Kodu (SKU)"
        const val CATEGORY_LABEL = "Kategori"
        const val STOCK_LABEL = "Stok Durumu"
        const val DESCRIPTION_LABEL = "Ürün Açıklaması"
        const val VARIANTS_LABEL = "Seçenekler & Varyantlar"
        const val SECRET_NOTE_LABEL = "Gizli İşletme Notu (Müşteriye Görünmez)"
        const val BTN_EDIT = "Düzenle"
        const val BTN_SHARE = "Paylaş"
        const val SHARE_PRODUCT_TOAST = "Ürün paylaşım linki panoya kopyalandı!"
    }

    object ProductForm {
        const val TITLE_NEW = "Yeni Ürün Ekle"
        const val TITLE_EDIT = "Ürünü Düzenle"

        // Görsel Yükleme
        const val SECTION_IMAGES = "Ürün Görselleri"
        const val IMAGES_SUBTITLE = "En fazla 10 görsel (Max 1280px otomatik sıkıştırılır). İlk görsel kapaktır, oklarla veya sürükleyerek sıralayın."
        const val COVER_BADGE = "Kapak"
        const val BTN_ADD_CAMERA = "Kamera"
        const val BTN_ADD_GALLERY = "Galeri"
        const val BTN_ADD_DEMO_PHOTO = "Örnek Fotoğraf"
        const val MAX_IMAGES_WARNING = "En fazla 10 görsel yükleyebilirsiniz"
        const val IMAGE_COMPRESSING = "Görsel sıkıştırılıyor (max 1280px)..."

        // Gemini AI Entegrasyonu
        const val BTN_AI_FILL = "✨ Yapay zeka ile doldur"
        const val AI_LOADING_TITLE = "Gemini görseli analiz ediyor..."
        const val AI_LOADING_SUBTITLE = "Ürün adı, Türkçe satış açıklaması, kategori ve etiketler oluşturuluyor"
        const val AI_SUCCESS_TOAST = "✨ Ürün bilgileri yapay zeka ile dolduruldu!"
        const val AI_ERROR_TOAST = "Yapay zeka analizi sırasında hata oluştu: "

        // Temel Bilgiler
        const val SECTION_BASIC = "Temel Bilgiler"
        const val LABEL_NAME = "Ürün Adı *"
        const val PLACEHOLDER_NAME = "Örn: Keten Oversize Gömlek"
        const val LABEL_DESCRIPTION = "Ürün Açıklaması"
        const val PLACEHOLDER_DESCRIPTION = "Ürünün kumaş, malzeme, ölçü ve kullanım detaylarını yazın..."

        // Fiyatlandırma
        const val SECTION_PRICING = "Fiyatlandırma"
        const val LABEL_PRICE = "Satış Fiyatı *"
        const val PLACEHOLDER_PRICE = "Örn: 890"
        const val LABEL_DISCOUNT_PRICE = "İndirimli Fiyat (Opsiyonel)"
        const val PLACEHOLDER_DISCOUNT_PRICE = "Örn: 690"
        const val LABEL_CURRENCY = "Para Birimi"

        // Kategori, SKU ve Etiketler
        const val SECTION_CATEGORY_SKU = "Kategori & Ürün Kodu"
        const val LABEL_SKU = "SKU / Ürün Kodu"
        const val PLACEHOLDER_SKU = "Örn: VR-SS25-101"
        const val LABEL_CATEGORY = "Ana Kategori"
        const val LABEL_SUBCATEGORY = "Alt Kategori"
        const val BTN_NEW_CATEGORY = "+ Yeni Ekle"
        const val PLACEHOLDER_NEW_CATEGORY = "Yeni kategori adı yazın..."
        const val PLACEHOLDER_NEW_SUBCATEGORY = "Yeni alt kategori adı yazın..."
        const val LABEL_TAGS = "Ürün Etiketleri"
        const val PLACEHOLDER_TAG_INPUT = "Etiket yazıp + butonuna basın"

        // Stok Yönetimi
        const val SECTION_STOCK = "Stok Yönetimi"
        const val LABEL_TRACK_STOCK = "Stok Takibi Yap"
        const val DESC_TRACK_STOCK = "Sipariş geldikçe kalan ürün adedini takip et"
        const val LABEL_STOCK_COUNT = "Stok Adedi"
        const val PLACEHOLDER_STOCK_COUNT = "Örn: 25"
        const val LABEL_OUT_OF_STOCK = "Tükendi Olarak İşaretle"
        const val DESC_OUT_OF_STOCK = "Ürün katalogda 'Tükendi' rozetiyle gösterilir"

        // Varyantlar
        const val SECTION_VARIANTS = "Varyantlar (Beden, Renk vb.)"
        const val VARIANTS_SUBTITLE = "Her varyant seçeneği için ek fiyat ve stok adedi tanımlayabilirsiniz"
        const val BTN_ADD_VARIANT_GROUP = "Özellik Grubu Ekle"
        const val PRESET_SIZE = "+ Beden Grubu"
        const val PRESET_COLOR = "+ Renk Grubu"
        const val LABEL_GROUP_NAME = "Grup Adı (Örn: Beden, Renk, Boyut)"
        const val LABEL_OPTION_NAME = "Seçenek (Örn: M, Kırmızı)"
        const val LABEL_EXTRA_PRICE = "Ek Fiyat"
        const val LABEL_OPTION_STOCK = "Stok"
        const val BTN_ADD_OPTION = "Seçenek Ekle"

        // Gizli Not
        const val SECTION_SECRET_NOTE = "Gizli Not"
        const val SECRET_NOTE_SUBTITLE = "Bu not müşteriye görünmez; tedarikçi, maliyet veya raf bilgisi için kullanabilirsiniz."
        const val PLACEHOLDER_SECRET_NOTE = "Örn: Toptancı Ahmet Bey, raf no B-12, alış maliyeti 380₺..."

        // Doğrulama & Kaydetme
        const val ERROR_NAME_REQUIRED = "Lütfen ürün adını girin"
        const val ERROR_PRICE_REQUIRED = "Lütfen geçerli bir satış fiyatı girin"
        const val BTN_SAVE_PRODUCT = "Ürünü Kaydet"
        const val TOAST_SAVED_SUCCESS = "Ürün başarıyla kaydedildi"
    }

    object Orders {
        const val TITLE = "Siparişler"
        const val SUBTITLE = "Gelen müşteri siparişlerini ve teslimat durumlarını yönetin"
        const val SEARCH_PLACEHOLDER = "Müşteri adı veya sipariş no ara..."

        // Durum Filtreleri
        const val FILTER_ALL = "Tümü"
        const val FILTER_NEW = "Yeni"
        const val FILTER_APPROVED = "Onaylandı"
        const val FILTER_PREPARING = "Hazırlanıyor"
        const val FILTER_SHIPPED = "Kargoda"
        const val FILTER_DELIVERED = "Teslim edildi"
        const val FILTER_CANCELLED = "İptal"

        // Sipariş Kartı
        const val ITEM_COUNT_SUFFIX = "ürün"
        const val ORDER_NO_PREFIX = "Sipariş No:"
        const val CARD_DETAIL_HINT = "Detay"

        // Boş Durum Ekranı
        const val EMPTY_TITLE = "Henüz hiç siparişiniz yok"
        const val EMPTY_DESCRIPTION = "Katalog linklerinizi müşterilerinizle paylaştığınızda gelen siparişler anında burada listelenir."
        const val EMPTY_FILTER_TITLE = "Filtreyle eşleşen sipariş bulunamadı"
        const val EMPTY_FILTER_DESCRIPTION = "Farklı bir sipariş durumu seçin veya arama kutusunu temizleyin."
        const val BTN_LOAD_SAMPLE_ORDERS = "Örnek Siparişleri Yükle"
        const val BTN_CREATE_DEMO_ORDER = "Yeni Test Siparişi Oluştur (Stok Düşür)"
        const val BTN_CLEAR_ORDERS = "Tüm Siparişleri Temizle (Boş Durum)"
        const val BTN_RESET_FILTERS = "Filtreleri Temizle"

        // Sipariş Detayı (/orders/:id)
        const val DETAIL_TITLE = "Sipariş Detayı"
        const val SECTION_STATUS_CHANGE = "Sipariş Durumu Yönetimi"
        const val STATUS_DROPDOWN_LABEL = "Durumu Değiştir"
        const val STATUS_AUTO_STOCK_NOTE = "Stok takibi açık olan ürünlerin stoğu sipariş oluşturulduğunda otomatik düşürüldü."
        const val SECTION_PRODUCTS = "Sipariş Edilen Ürünler"
        const val SECTION_CUSTOMER = "Müşteri & Teslimat Bilgileri"
        const val LABEL_CUSTOMER_NAME = "Müşteri Adı Soyadı"
        const val LABEL_CUSTOMER_PHONE = "Telefon Numarası"
        const val LABEL_CUSTOMER_ADDRESS = "Teslimat Adresi"
        const val LABEL_CUSTOMER_NOTE = "Müşteri Sipariş Notu"
        const val LABEL_NO_NOTE = "Sipariş notu belirtilmedi"
        const val LABEL_TOTAL_AMOUNT = "Genel Toplam"
        const val SECTION_TIMELINE = "Sipariş Zaman Çizelgesi"
        const val TIMELINE_SUBTITLE = "Durum değişiklikleri otomatik olarak tarih ve saat ile kaydedilir"

        // Aksiyonlar
        const val SECTION_ACTIONS = "Hızlı Sipariş Aksiyonları"
        const val BTN_WHATSAPP_CUSTOMER = "Müşteriye WhatsApp yaz"
        const val BTN_DOWNLOAD_INVOICE_PDF = "Faturayı/Fişi PDF indir"
        const val BTN_CANCEL_ORDER = "Sipariş iptal et"
        const val BTN_ALREADY_CANCELLED = "Sipariş İptal Edildi"

        // İptal Onay Modalı
        const val MODAL_CANCEL_TITLE = "Sipariş İptal Edilsin mi?"
        const val MODAL_CANCEL_DESC = "Bu siparişin durumu 'İptal' olarak güncellenecek, zaman çizelgesine işlenecek ve düşülen ürün stokları geri eklenecektir."
        const val MODAL_CANCEL_CONFIRM = "Evet, Siparişi İptal Et"

        // Toast Mesajları
        const val TOAST_STATUS_UPDATED = "Sipariş durumu güncellendi ve zaman çizelgesine eklendi"
        const val TOAST_ORDER_CANCELLED = "Sipariş iptal edildi ve stoklar geri yüklendi"
        const val TOAST_INVOICE_DOWNLOADED = "Sipariş faturası/fişi PDF olarak indirildi"
        const val TOAST_NEW_DEMO_ORDER = "Yeni sipariş oluşturuldu ve ürün stokları otomatik düşürüldü!"
        const val TOAST_SAMPLE_ORDERS_LOADED = "Örnek siparişler yüklendi"
    }

    object Analytics {
        const val TITLE = "Analitik"
        const val SUBTITLE = "Katalog ziyaretçilerini, ürün ilgisini ve satış dönüşümlerini izleyin"
        const val SEED_DEMO_DATA = "Örnek Veri Üret"
        const val TOAST_SEEDED = "Örnek ziyaret, ürün inceleme ve sepet verileri güncellendi!"

        // Tarih Aralığı Seçici
        const val RANGE_TODAY = "Bugün"
        const val RANGE_7_DAYS = "7 gün"
        const val RANGE_30_DAYS = "30 gün"
        const val RANGE_CUSTOM = "Özel"
        const val CUSTOM_RANGE_TITLE = "Özel Tarih Aralığı Seçimi"
        const val CUSTOM_START_DAYS_AGO = "Başlangıç (gün önce)"
        const val CUSTOM_END_DAYS_AGO = "Bitiş (gün önce)"
        const val CUSTOM_ACTIVE_SUMMARY_PREFIX = "Son"
        const val CUSTOM_ACTIVE_SUMMARY_SUFFIX = "günlük özel aralık gösteriliyor"

        // Üst Metrik Kartları (5 Kart)
        const val METRIC_TOTAL_VISITS = "Toplam ziyaretçi"
        const val METRIC_UNIQUE_VISITORS = "Benzersiz ziyaretçi"
        const val METRIC_ORDER_COUNT = "Sipariş sayısı"
        const val METRIC_REVENUE = "Ciro"
        const val METRIC_CONVERSION_RATE = "Dönüşüm oranı"

        // Grafik 1: Günlük Ziyaretçi Çizgi Grafiği
        const val CHART_DAILY_VISITORS_TITLE = "Günlük Ziyaretçi Çizgi Grafiği"
        const val CHART_DAILY_VISITORS_SUBTITLE = "Seçilen tarih aralığındaki toplam ve benzersiz ziyaretçi eğilimi"
        const val LEGEND_TOTAL_VISITS = "Toplam Ziyaret"
        const val LEGEND_UNIQUE_VISITORS = "Benzersiz Ziyaretçi"
        const val CHART_TAP_HINT = "Günlük detayı görmek için grafik noktalarına dokunun"

        // Grafik 2: Katalog Bazında Ziyaretçi Çubuk Grafiği
        const val CHART_CATALOG_BAR_TITLE = "Katalog Bazında Ziyaretçi Çubuk Grafiği"
        const val CHART_CATALOG_BAR_SUBTITLE = "Katalogların aldığı ziyaretçi ve sipariş dağılımı"

        // En Çok Görüntülenen & En Çok Sepete Eklenen Ürünler
        const val TOP_VIEWED_TITLE = "En çok görüntülenen ürünler"
        const val TOP_VIEWED_SUBTITLE = "Müşterilerin en fazla incelediği ilk 10 ürün"
        const val TOP_CART_TITLE = "En çok sepete eklenen ürünler"
        const val TOP_CART_SUBTITLE = "Sepete eklenme adedine göre en popüler ürünler"
        const val UNIT_VIEWS = "görüntülenme"
        const val UNIT_CART_ADDS = "sepete ekleme"
        const val UNIT_ORDERS = "sipariş"

        // Müşteri İlgisi Bölümü (Tablo)
        const val CUSTOMER_INTEREST_TITLE = "Müşteri ilgisi"
        const val CUSTOMER_INTEREST_SUBTITLE = "Ürün başına görüntülenme, sepete ekleme ve sipariş sayıları"
        const val TABLE_COL_PRODUCT = "Ürün"
        const val TABLE_COL_VIEWS = "Görüntülenme"
        const val TABLE_COL_CART = "Sepete Ekleme"
        const val TABLE_COL_ORDERS = "Sipariş"
        const val BTN_SHOW_ALL_PRODUCTS = "Tüm Ürünleri Göster"
        const val BTN_SHOW_LESS_PRODUCTS = "Daha Az Göster"

        // Kaynak Dağılımı (Referrer)
        const val REFERRER_TITLE = "Kaynak dağılımı"
        const val REFERRER_SUBTITLE = "Ziyaretçilerin kataloğunuza ulaştığı trafik kaynakları (referrer)"
        const val REFERRER_WHATSAPP = "WhatsApp"
        const val REFERRER_DIRECT = "Doğrudan link"
        const val REFERRER_OTHER = "Diğer"

        // Eski uyumluluk sabitleri
        const val EMPTY_TITLE = "Henüz analitik verisi bulunmuyor"
        const val EMPTY_DESCRIPTION = "Kataloglarınız ziyaret edildiğinde ve ürünler incelendiğinde grafikler burada oluşacaktır."
        const val METRIC_VIEWS = "Görüntülenme"
        const val METRIC_VISITORS = "Tekil Ziyaretçi"
        const val METRIC_ORDERS = "Sipariş Dönüşümü"
        const val DRAFT_NOTICE = "Canlı Analitik Raporu"
    }

    object Settings {
        const val TITLE = "Ayarlar"
        const val SUBTITLE = "Mağaza profili, bayiler, stok yönetimi ve veri katmanı tercihleri"
        const val EMPTY_TITLE = "Mağaza ve uygulama ayarları"
        const val EMPTY_DESCRIPTION = "İşletme bilgilerinizi, stok eşiklerinizi ve CSV içe/dışa aktarma işlemlerinizi buradan yönetebilirsiniz."
        const val RESET_DEMO_DATA = "Yerel Önbelleği Sıfırla"
        const val DRAFT_NOTICE = "Mağaza Ayarları"

        // Ayarlar -> Bayiler Bölümü
        const val RESELLERS_SECTION_TITLE = "Bayiler"
        const val RESELLERS_SECTION_SUBTITLE = "Bayi ekleyin, görünür katalogları seçin, bayiye özel fiyat/marjlı link üretin ve bayi bazında satış özetini inceleyin"
        const val BTN_OPEN_RESELLERS = "Bayi Yönetimi & Satış Özeti (/resellers)"
        const val BTN_ADD_NEW_RESELLER = "+ Yeni Bayi Ekle"

        // Ayarlar -> Stok Bölümü
        const val STOCK_SECTION_TITLE = "Stok"
        const val STOCK_SECTION_SUBTITLE = "Tüm ürünlerin stok takibini, az kalan eşik değerini ve toplu CSV işlemlerini yönetin"
        const val LOW_STOCK_THRESHOLD_LABEL = "Az kalan stok eşiği"
        const val LOW_STOCK_THRESHOLD_HELPER = "Stok adedi bu değer ve altına düştüğünde 'Az kaldı' olarak işaretlenir ve uyarı gösterilir"
        const val BTN_OPEN_INVENTORY = "Stok Yönetimi Ekranını Aç (/inventory)"
        const val BTN_BULK_CSV_IMPORT = "Excel / CSV'den Toplu Ürün İçe Aktar"
        const val BTN_EXPORT_CSV = "Mevcut Ürünleri CSV Dışa Aktar"
    }

    object Resellers {
        const val TITLE = "Bayiler & Satış Özeti"
        const val SUBTITLE = "Bayilerinizi, görünür katalogları, özel fiyat linklerini ve bayi satış performansını yönetin"
        const val BADGE_RESELLER = "Bayi"
        const val ORDER_BADGE_RESELLER = "Bayi"
        const val EMPTY_TITLE = "Henüz kayıtlı bayi bulunmuyor"
        const val EMPTY_DESC = "Bayi ekleyerek her bayiye hangi katalogların görünür olacağını seçebilir ve özel fiyatlı katalog linki üretebilirsiniz."
        const val BTN_ADD_RESELLER = "Bayi Ekle"
        const val MODAL_ADD_TITLE = "Yeni Bayi Ekle"
        const val MODAL_EDIT_TITLE = "Bayi Bilgilerini Düzenle"
        const val INPUT_NAME_LABEL = "Bayi Adı / Firma *"
        const val INPUT_NAME_PLACEHOLDER = "Örn: Anadolu Moda & Tekstil Bayi"
        const val INPUT_PHONE_LABEL = "Bayi Telefon Numarası *"
        const val INPUT_PHONE_PLACEHOLDER = "Örn: +90 532 610 44 12"
        const val VISIBLE_CATALOGS_LABEL = "Görünür Kataloglar"
        const val LABEL_VISIBLE_CATALOGS = "Görünür Kataloglar"
        const val VISIBLE_CATALOGS_HELPER = "Bu bayinin özel linkinde hangi katalogların görüntüleneceğini seçin"
        const val DESC_VISIBLE_CATALOGS = "Bu bayinin özel linkinde hangi katalogların görüntüleneceğini seçin"
        const val PRICE_RULE_LABEL = "Bayi Fiyat / Marj Ayarı"
        const val LABEL_PRICE_RULE = "Bayi Fiyat / Marj Ayarı"
        const val MARGIN_VALUE_LABEL = "Marj / İndirim Değeri"
        const val SHORT_LINK_LABEL = "Bayiye Özel Katalog Linki"
        const val SPECIAL_LINK_TITLE = "Bayiye Özel Katalog Linki"
        const val BTN_COPY_RESELLER_LINK = "Bayi Linkini Kopyala"
        const val BTN_WHATSAPP_RESELLER_LINK = "WhatsApp'ta Gönder"
        const val BTN_PREVIEW_RESELLER_CATALOG = "Bayi Görünümünü Aç"
        const val BTN_OPEN_RESELLER_VIEW = "Bayi Görünümünü Aç"
        const val SALES_SUMMARY_TITLE = "Bayi Bazında Satış Özeti"
        const val SALES_SUMMARY_SUBTITLE = "Bayi özel linkleri üzerinden gelen siparişlerin ve ciroların dağılımı"
        const val COL_RESELLER = "Bayi"
        const val COL_ORDERS = "Sipariş"
        const val COL_ITEMS = "Ürün Adedi"
        const val COL_REVENUE = "Toplam Satış"
        const val TOAST_RESELLER_SAVED = "Bayi bilgileri ve görünür kataloglar kaydedildi"
        const val TOAST_RESELLER_DELETED = "Bayi kaydı silindi"
        const val TOAST_RESELLER_LINK_COPIED = "Bayiye özel katalog linki panoya kopyalandı!"
    }

    object Inventory {
        const val TITLE = "Stok Yönetimi"
        const val SUBTITLE = "Tüm ürünlerin stok adetlerini, kritik eşikleri ve CSV aktarımlarını yönetin"
        const val SEARCH_PLACEHOLDER = "Ürün adı veya SKU kodu ara..."

        // Filtreler
        const val FILTER_ALL = "Tümü"
        const val FILTER_IN_STOCK = "Stokta"
        const val FILTER_LOW_STOCK = "Az kalan"
        const val FILTER_OUT_OF_STOCK = "Tükendi"
        const val THRESHOLD_CHIP_PREFIX = "Az kalan eşiği:"

        // Tablo Başlıkları
        const val COL_PRODUCT = "Görsel / Ürün & SKU"
        const val COL_STOCK_COUNT = "Stok Adedi"
        const val COL_STATUS = "Durum"

        // Toplu İşlemler (Bulk Actions)
        const val SELECT_ALL = "Tümünü Seç"
        const val CLEAR_SELECTION = "Seçimi Temizle"
        const val SELECTED_SUFFIX = "ürün seçildi"
        const val BTN_BULK_ASSIGN_STOCK = "Stok Ata"
        const val BTN_BULK_MARK_OUT_OF_STOCK = "Tükendi Olarak İşaretle"
        const val MODAL_BULK_STOCK_TITLE = "Seçilen Ürünlere Stok Ata"
        const val MODAL_BULK_STOCK_DESC = "Seçilen tüm ürünlerin stok adedi aşağıdaki değer olarak güncellenecektir."
        const val INPUT_BULK_STOCK_LABEL = "Yeni Stok Adedi"
        const val TOAST_BULK_STOCK_UPDATED = "Seçilen ürünlerin stok adetleri güncellendi"
        const val TOAST_BULK_OUT_OF_STOCK = "Seçilen ürünler 'Tükendi' olarak işaretlendi"
        const val TOAST_SINGLE_STOCK_UPDATED = "Stok adedi güncellendi"

        // Ana Ekran Stok Az Kaldı Uyarı Bildirimi
        const val HOME_LOW_STOCK_ALERT_TITLE = "Stok Uyarısı: Kritik Seviyedeki Ürünler"
        const val HOME_LOW_STOCK_MANAGE_BTN = "Stokları Yönet"

        // Excel / CSV Toplu Ürün İçe Aktarma & Dışa Aktarma
        const val BTN_IMPORT_CSV = "İçe Aktar (CSV)"
        const val BTN_EXPORT_CSV = "CSV İndir / Paylaş"
        const val IMPORT_MODAL_TITLE = "Excel / CSV'den Toplu Ürün İçe Aktarma"
        const val IMPORT_MODAL_SUBTITLE = "CSV dosyanızı yükleyin, sütunları eşleştirin ve önizleyerek içe aktarın"
        const val BTN_PICK_CSV_FILE = "Dosya Yükle (.CSV)"
        const val BTN_LOAD_SAMPLE_CSV = "Örnek CSV Yükle"
        const val TARGET_CATALOG_LABEL = "Hedef Katalog Seçin"
        const val COLUMN_MAPPING_TITLE = "Sütun Eşleştirme"
        const val COLUMN_MAPPING_DESC = "Dosyanızdaki sütun başlıklarını ürün alanlarıyla eşleştirin"
        const val MAP_FIELD_NAME = "Ürün Adı *"
        const val MAP_FIELD_PRICE = "Fiyat *"
        const val MAP_FIELD_SKU = "SKU / Ürün Kodu"
        const val MAP_FIELD_STOCK = "Stok Adedi"
        const val MAP_FIELD_CATEGORY = "Kategori"
        const val MAP_FIELD_IMAGE_URL = "Görsel URL"
        const val PREVIEW_TABLE_TITLE = "İçe Aktarılacak Ürün Önizlemesi"
        const val BTN_CONFIRM_IMPORT = "Ürünleri İçe Aktar"
        const val TOAST_CSV_IMPORT_SUCCESS = "ürün başarıyla içe aktarıldı!"
        const val TOAST_CSV_EXPORT_SUCCESS = "Tüm ürünler CSV dosyası olarak dışa aktarıldı!"
    }

    object Share {
        const val SHEET_TITLE_CATALOG = "Kataloğu Paylaş"
        const val SHEET_TITLE_PRODUCT = "Ürünü Paylaş"
        const val SHEET_SUBTITLE = "Paylaşım biçimini ve gelişmiş bayi/fiyat ayarlarını seçin"

        // 4 Ana Seçenek Sekmesi
        const val OPT_LINK = "Link olarak paylaş"
        const val OPT_PDF = "Broşür (PDF) olarak paylaş"
        const val OPT_IMAGE = "Görsel olarak paylaş"
        const val OPT_MULTI_CATALOG = "Birden fazla kataloğu tek linkle paylaş"

        const val TAB_LINK = "Kısa Link"
        const val TAB_PDF = "Broşür (PDF)"
        const val TAB_IMAGE = "Görsel Kart"
        const val TAB_COLLECTION = "Çoklu Katalog"

        // 1) Link Olarak Paylaş
        const val SHORT_LINK_BADGE = "Benzersiz Kısa Link"
        const val PREVIEW_CARD_LABEL = "Link Önizleme Kartı"
        const val BTN_COPY_LINK = "Kopyala"
        const val BTN_SEND_WHATSAPP = "WhatsApp'ta gönder"
        const val PRICE_HIDDEN_BADGE = "Fiyatlar Gizli"
        const val DEALER_BADGE = "Bayi Fiyatlı"
        const val PASSWORD_PROTECTED_BADGE = "Şifre Korumalı"

        // 2) Broşür (PDF) Olarak Paylaş
        const val PDF_TITLE = "Katalog PDF Broşürü"
        const val PDF_SUBTITLE = "Kapakta işletme logosu ve katalog adı, her ürün için görsel + ad + fiyat yer alır."
        const val PDF_SINGLE_PAGE = "Tek Sayfa Özet"
        const val PDF_MULTI_PAGE = "Çok Sayfalı Broşür"
        const val BTN_DOWNLOAD_PDF = "İndir"
        const val BTN_SHARE_PDF = "Paylaş"
        const val PDF_GENERATING = "PDF broşür hazırlanıyor..."
        const val TOAST_PDF_DOWNLOADED = "PDF broşür cihazınıza kaydedildi!"
        const val TOAST_PDF_SHARED = "PDF broşür paylaşım menüsü açıldı"

        // 3) Görsel Olarak Paylaş (Canvas)
        const val IMAGE_TITLE = "Ürün Görsel Kartı (Canvas)"
        const val IMAGE_SUBTITLE = "Ürün görseli üzerine başlık, fiyat, SKU ve logo filigranı işlenir."
        const val WATERMARK_LABEL = "Logo Filigranı & Konumu"
        const val WATERMARK_SHOW = "Logo Filigranı Göster"
        const val OPACITY_LABEL = "Filigran Şeffaflığı"
        const val OVERLAY_FIELDS_LABEL = "Görsel Üzerindeki Bilgi Alanları"
        const val FIELD_TITLE = "Başlık"
        const val FIELD_PRICE = "Fiyat"
        const val FIELD_SKU = "SKU Kodu"
        const val EXTRA_NOTE_LABEL = "Görsel Üzeri Ek Not (İsteğe Bağlı)"
        const val EXTRA_NOTE_PLACEHOLDER = "Örn: Yeni Sezon • Toptan & Perakende • Kargo Bedava"
        const val BTN_SHARE_IMAGE = "Görseli Oluştur ve Paylaş"
        const val IMAGE_GENERATING = "Görsel Canvas ile işleniyor..."
        const val TOAST_IMAGE_SHARED = "Ürün görsel kartı paylaşıma hazır!"

        // 4) Birden Fazla Kataloğu Tek Linkle Paylaş
        const val MULTI_TITLE = "Koleksiyon Linki Oluştur"
        const val MULTI_SUBTITLE = "Birden fazla kataloğu seçerek müşterinize tek bir koleksiyon linki gönderin."
        const val COLLECTION_NAME_LABEL = "Koleksiyon Başlığı"
        const val COLLECTION_NAME_PLACEHOLDER = "Örn: 2025 Tüm Sezon Koleksiyonu"
        const val SELECTED_CATALOGS_SUFFIX = "katalog seçildi"
        const val BTN_COPY_COLLECTION_LINK = "Koleksiyon Linkini Kopyala"
        const val ERROR_MIN_CATALOGS = "Lütfen en az 1 katalog seçin"

        // Paylaşım Ayarları (Aynı Sheet'te)
        const val SETTINGS_HEADER = "Paylaşım Ayarları"
        const val SETTING_SHOW_PRICES = "Fiyatları Göster"
        const val SETTING_SHOW_PRICES_DESC = "Kapatıldığında müşteriler fiyat yerine 'Fiyat Sorunuz' görür"
        const val SETTING_PASSWORD = "Linke Şifre Koruması Ekle"
        const val SETTING_PASSWORD_PLACEHOLDER = "Katalog giriş şifresi belirleyin (örn: 2025)"
        const val SETTING_EXPIRY = "Link Son Kullanma Tarihi"
        const val SETTING_EXPIRY_PLACEHOLDER = "Örn: 30.11.2026"
        const val EXPIRY_24H = "24 Saat"
        const val EXPIRY_7D = "7 Gün"
        const val EXPIRY_30D = "30 Gün"
        const val SETTING_DEALER_MODE = "Bayi Olarak Paylaş"
        const val SETTING_DEALER_DESC = "Mevcut fiyatların üstüne % veya sabit tutar marjı ekle"
        const val DEALER_MARGIN_PERCENT = "Yüzde (%)"
        const val DEALER_MARGIN_FIXED = "Sabit Tutar"
        const val DEALER_MARGIN_INPUT_LABEL = "Eklenecek Marj / Fark Değeri"
        const val SETTING_IMAGE_PROTECTION = "Görsel Kaydetme / Ekran Görüntüsü Koruması"
        const val SETTING_IMAGE_PROTECTION_DESC = "Müşteri sayfasında uzun basarak görsel kaydetmeyi ve ekran görüntüsünü engeller"
        const val BTN_OPEN_PUBLIC_VIEW = "Müşteri Görünümünü Önizle (/c/...)"
    }

    object PublicCatalog {
        const val WHATSAPP_CONTACT_BTN = "WhatsApp ile iletişim"
        const val SEARCH_PLACEHOLDER = "Katalogda ürün veya kod ara..."
        const val CATEGORY_ALL = "Tümü"

        // Fiyat Filtresi Seçenekleri
        const val PRICE_FILTER_ALL = "Tüm Fiyatlar"
        const val PRICE_FILTER_UNDER_500 = "0 - 500 ₺"
        const val PRICE_FILTER_500_1000 = "500 - 1.000 ₺"
        const val PRICE_FILTER_OVER_1000 = "1.000 ₺+"

        // Ürün Kartı
        const val BTN_ADD_TO_CART = "Sepete ekle"
        const val BADGE_OUT_OF_STOCK = "Tükendi"
        const val TOAST_ADDED_TO_CART = "sepete eklendi"

        // Görsel Koruması
        const val IMAGE_PROTECTION_ACTIVE_BADGE = "Görsel Koruması Aktif"
        const val IMAGE_PROTECTION_OFF_BADGE = "Görsel Koruması Kapalı"
        const val TOAST_IMAGE_SAVE_BLOCKED = "🔒 Görsel koruması aktif: Bu katalogda sağ tık / uzun basma ile görsel kaydetme devre dışıdır."

        // Tam Ekran Ürün Detay Modalı
        const val ZOOM_HINT = "Çift dokunarak veya iki parmakla yakınlaştırabilirsiniz"
        const val QUANTITY_LABEL = "Adet Seçimi"
        const val VARIANT_SELECT_PROMPT = "Seçenekler"

        // Sepet & Sipariş Formu
        const val CART_TITLE = "Alışveriş Sepetim"
        const val CART_EMPTY_TITLE = "Sepetiniz henüz boş"
        const val CART_EMPTY_DESC = "Katalogdan beğendiğiniz ürünleri sepete ekleyerek hızlıca sipariş oluşturabilirsiniz."
        const val TOTAL_AMOUNT_LABEL = "Toplam Tutar"
        const val BTN_PROCEED_TO_ORDER = "Siparişi Gönder"

        const val ORDER_FORM_TITLE = "Sipariş Bilgileri"
        const val ORDER_FORM_SUBTITLE = "Siparişinizin hazırlanması için iletişim ve teslimat bilgilerinizi girin"
        const val INPUT_FULL_NAME = "Ad Soyad *"
        const val PLACEHOLDER_FULL_NAME = "Örn: Ayşe Yılmaz"
        const val INPUT_PHONE = "Telefon Numarası *"
        const val PLACEHOLDER_PHONE = "Örn: 0532 123 45 67"
        const val INPUT_ADDRESS = "Teslimat Adresi *"
        const val PLACEHOLDER_ADDRESS = "Mahalle, sokak, bina no, ilçe / il..."
        const val INPUT_NOTE = "Sipariş Notu (İsteğe Bağlı)"
        const val PLACEHOLDER_NOTE = "Örn: Hediye paketi olsun, öğleden sonra teslim..."
        const val ERROR_NAME_REQUIRED = "Lütfen adınızı ve soyadınızı girin"
        const val ERROR_PHONE_REQUIRED = "Lütfen telefon numaranızı girin"
        const val ERROR_ADDRESS_REQUIRED = "Lütfen teslimat adresinizi girin"
        const val BTN_SUBMIT_ORDER = "Siparişi Gönder & WhatsApp'ta Aç"

        // Sipariş Başarı Ekranı
        const val SUCCESS_TITLE = "Siparişiniz Alındı!"
        const val SUCCESS_DESC = "Siparişiniz satıcıya 'Yeni' durumuyla kaydedildi ve WhatsApp sipariş mesajınız hazırlandı."
        const val BTN_REOPEN_WHATSAPP = "WhatsApp Mesajını Tekrar Aç"
        const val BTN_BACK_TO_CATALOG = "Kataloğa Dön"
    }

    object DesignShowcase {
        const val TITLE = "Tasarım Sistemi & Ortak Bileşenler"
        const val SUBTITLE = "Renk paleti, Inter tipografi ve ortak UI bileşenleri vitrini"
        const val SECTION_BUTTONS = "Butonlar (Primary / Secondary / Ghost)"
        const val SECTION_INPUTS = "Metin Alanları (Input)"
        const val SECTION_BADGES = "Rozetler (Badge)"
        const val SECTION_CARDS = "Kartlar (Card - rounded-2xl)"
        const val SECTION_FEEDBACK = "Skeleton Loader & EmptyState"
        const val SECTION_OVERLAYS = "BottomSheet, Modal ve Toast"
        const val BTN_PRIMARY = "Birincil Buton"
        const val BTN_SECONDARY = "İkincil Buton"
        const val BTN_GHOST = "Hayalet (Ghost) Buton"
        const val INPUT_LABEL = "Mağaza Adı"
        const val INPUT_PLACEHOLDER = "Örn: Kuzey Tasarım Atölyesi"
        const val INPUT_HELPER = "Müşterilerinizin katalog başlığında göreceği isim"
        const val OPEN_BOTTOM_SHEET = "BottomSheet Aç"
        const val OPEN_MODAL = "Modal Aç"
        const val SHOW_TOAST = "Toast Göster"
        const val SHEET_TITLE = "Örnek Alt Çekmece (BottomSheet)"
        const val SHEET_DESC = "Bu bileşen hızlı filtreleme, paylaşım seçenekleri ve ürün eylemleri için tasarlandı."
        const val MODAL_TITLE = "Onay Diyaloğu (Modal)"
        const val MODAL_DESC = "Kritik işlemler ve hızlı bildirimler için kullanılan yuvarlatılmış (rounded-2xl) modal bileşeni."
        const val TOAST_SAMPLE_MESSAGE = "İşlem başarıyla yerel veri katmanına (DataService) kaydedildi!"
    }
}
