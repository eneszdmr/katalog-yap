package com.example.ui.screens

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.AddToHomeScreen
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storefront
import com.example.util.PwaOfflineAndShortcutHelper
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.auth.LocalAuthContext
import com.example.i18n.TrStrings
import com.example.model.CurrencyOption
import com.example.model.InventoryFilter
import com.example.model.OrderStatus
import com.example.model.StockStatus
import com.example.model.SubscriptionPlan
import com.example.model.TeamRole
import com.example.services.LocalStorageDataService
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYButton
import com.example.ui.components.KYCard
import com.example.ui.components.KYInput
import com.example.ui.components.KYToastData
import com.example.ui.components.KYToastType
import com.example.ui.components.KYTopBar
import com.example.ui.screens.inventory.BulkCsvImportModal
import com.example.ui.screens.inventory.InventoryCsvExporter
import com.example.ui.screens.resellers.AddOrEditResellerModal
import com.example.ui.screens.settings.DeleteAccountConfirmModal
import com.example.ui.screens.settings.EditBusinessProfileModal
import com.example.ui.screens.settings.HelpAndSupportModal
import com.example.ui.screens.settings.InviteTeamMemberModal
import com.example.ui.screens.settings.PrivacyPolicyModal
import com.example.ui.screens.settings.SubscriptionSectionCard
import com.example.ui.screens.settings.SubscriptionUpgradeModal
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentAmberDark
import com.example.ui.theme.AccentAmberLight
import com.example.ui.theme.BackgroundSlate
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmberDark
import com.example.ui.theme.WarningAmberLight
import kotlinx.coroutines.launch

private data class CatalogThemeColorPreset(
    val nameTr: String,
    val hex: String,
    val color: Color
)

private val catalogThemeColorPresets = listOf(
    CatalogThemeColorPreset("Kraliyet Mavisi", "#2563EB", Color(0xFF2563EB)),
    CatalogThemeColorPreset("Zümrüt Yeşili", "#10B981", Color(0xFF10B981)),
    CatalogThemeColorPreset("Kehribar Turuncu", "#D97706", Color(0xFFD97706)),
    CatalogThemeColorPreset("Asil Mor", "#7C3AED", Color(0xFF7C3AED)),
    CatalogThemeColorPreset("Mercan Gül", "#E11D48", Color(0xFFE11D48)),
    CatalogThemeColorPreset("Antrasit Siyah", "#0F172A", Color(0xFF0F172A))
)

/**
 * AYARLAR SEKMESİ (/settings):
 * 1. İşletme Profili: Logo, ad, telefon, adres, sosyal medya linkleri, düzenleme.
 * 2. Mağaza Ayarları: Para birimi, KDV oranı ve fiyatlarda KDV gösterme anahtarı,
 *    minimum sipariş tutarı, kargo ücreti, sipariş onay mesajı şablonu.
 * 3. Katalog Görünümü: Tema rengi seçici, grid sütun sayısı, fiyat gösterimi, ekran görüntüsü koruması.
 * 4. Bildirimler: Yeni sipariş, düşük stok, haftalık özet anahtarları.
 * 5. Ekip: Kullanıcı davet etme (Rol: Yönetici, Editör, Sadece görüntüleme) + rol yönetimi.
 * 6. Abonelik: Ücretsiz, Lite (aylık), Premium (yıllık) planlarını karşılaştıran tablo;
 *    plana göre limitler (Ücretsizde max 2 katalog ve 50 ürün), limit aşılınca yükseltme ekranı (demo ödeme).
 * 7. Bayiler ve Stok Yönetimi bölümleri.
 * 8. Yardım ve Destek, Gizlilik Politikası, Çıkış Yap, Hesabı Sil (onay modalı ile).
 */
@Composable
fun SettingsDraftScreen(
    onOpenDesignSystem: () -> Unit,
    onOpenInventory: (initialFilter: InventoryFilter, openImportModal: Boolean) -> Unit = { _, _ -> },
    onOpenResellers: (openAddOnLaunch: Boolean) -> Unit = {},
    onOpenPublicCatalog: (String) -> Unit = {},
    onOpenNotFound404: () -> Unit = {},
    isOfflinePreviewActive: Boolean = false,
    onToggleOfflinePreview: () -> Unit = {},
    onLoggedOut: () -> Unit,
    onShowToast: (KYToastData) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataService = remember { LocalStorageDataService.getInstance(context) }
    val scope = rememberCoroutineScope()

    val authContext = LocalAuthContext.current
    val store by dataService.settings.collectAsState()
    val products by dataService.products.collectAsState()
    val catalogs by dataService.catalogs.collectAsState()
    val resellers by dataService.resellers.collectAsState()
    val orders by dataService.orders.collectAsState()

    val threshold = store.lowStockThreshold.coerceAtLeast(1)
    val currency = store.currencySymbol.ifBlank { "₺" }

    // Modallar
    var showEditProfileModal by remember { mutableStateOf(false) }
    var showInviteTeamModal by remember { mutableStateOf(false) }
    var showUpgradeModal by remember { mutableStateOf(false) }
    var upgradeLimitReason by remember { mutableStateOf<String?>(null) }
    var upgradePreselectedPlan by remember { mutableStateOf(SubscriptionPlan.LITE_MONTHLY) }
    var showBulkCsvImportModal by remember { mutableStateOf(false) }
    var showAddResellerModal by remember { mutableStateOf(false) }
    var showHelpModal by remember { mutableStateOf(false) }
    var showPrivacyModal by remember { mutableStateOf(false) }
    var showDeleteAccountModal by remember { mutableStateOf(false) }

    // Mağaza Ayarları Düzenlenebilir State'leri
    var minOrderInput by remember(store.minOrderAmount) {
        mutableStateOf(store.minOrderAmount.toInt().toString())
    }
    var shippingFeeInput by remember(store.shippingFee) {
        mutableStateOf(store.shippingFee.toInt().toString())
    }
    var confirmationTemplateInput by remember(store.orderConfirmationTemplate) {
        mutableStateOf(store.orderConfirmationTemplate)
    }

    val resellerOrders = remember(orders) {
        orders.filter { it.status != OrderStatus.CANCELLED && it.isResellerOrder }
    }
    val totalResellerRevenue = remember(resellerOrders) {
        resellerOrders.sumOf { it.totalAmount }
    }

    val inStockCount = remember(products, threshold) {
        products.count { it.inStock && it.stockCount > threshold && it.stockStatus != StockStatus.OUT_OF_STOCK }
    }
    val lowStockCount = remember(products, threshold) {
        products.count { it.inStock && it.stockCount in 1..threshold && it.stockStatus != StockStatus.OUT_OF_STOCK }
    }
    val outOfStockCount = remember(products, threshold) {
        products.count { !it.inStock || it.stockCount <= 0 || it.stockStatus == StockStatus.OUT_OF_STOCK }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundSlate)
            .testTag("screen_settings_draft")
    ) {
        KYTopBar(
            title = TrStrings.Settings.TITLE,
            subtitle = "İşletme profili, mağaza, katalog görünümü, ekip ve abonelik",
            rightAction = {
                KYButton(
                    text = TrStrings.Navigation.DESIGN_SYSTEM,
                    onClick = onOpenDesignSystem,
                    variant = ButtonVariant.GHOST,
                    leadingIcon = Icons.Outlined.Palette,
                    tag = "settings_btn_design_system"
                )
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_scroll_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =========================================================================
            // 1) İŞLETME PROFİLİ: Logo, ad, telefon, adres, sosyal medya linkleri, düzenleme
            // =========================================================================
            item(key = "section_business_profile") {
                KYCard(modifier = Modifier.testTag("settings_business_profile_card")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            AsyncImage(
                                model = store.logoUri.ifBlank { "https://picsum.photos/seed/vera-logo/300/300" },
                                contentDescription = "İşletme Logosu",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.5.dp, PrimaryBlue.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .background(SurfaceMuted)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = store.businessName.ifBlank { "Mağazam" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${store.businessType} • ${store.city.ifBlank { "Türkiye" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        KYBadge(
                            text = TrStrings.Common.FIREBASE_READY,
                            variant = BadgeVariant.SUCCESS,
                            icon = Icons.Outlined.CloudSync
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderLight)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Telefon ve Adres Bilgileri
                    ProfileDetailRow(
                        icon = Icons.Outlined.Phone,
                        label = "Telefon / WhatsApp",
                        value = store.phoneWhatsapp.ifBlank { "Belirtilmedi" }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ProfileDetailRow(
                        icon = Icons.Outlined.LocationOn,
                        label = "Adres",
                        value = store.businessAddress.ifBlank { "Adres eklenmedi" }
                    )

                    // Sosyal Medya Linkleri
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Sosyal Medya ve Web Linkleri",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (store.instagramUrl.isNotBlank()) {
                            SocialChip(label = "Instagram", value = store.instagramUrl, modifier = Modifier.weight(1f))
                        }
                        if (store.websiteUrl.isNotBlank()) {
                            SocialChip(label = "Web Sitesi", value = store.websiteUrl, modifier = Modifier.weight(1f))
                        }
                        if (store.facebookUrl.isNotBlank()) {
                            SocialChip(label = "Facebook", value = store.facebookUrl, modifier = Modifier.weight(1f))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    KYButton(
                        text = "İşletme Profilini Düzenle",
                        onClick = { showEditProfileModal = true },
                        variant = ButtonVariant.PRIMARY,
                        leadingIcon = Icons.Outlined.Edit,
                        modifier = Modifier.fillMaxWidth(),
                        tag = "btn_edit_business_profile"
                    )
                }
            }

            // =========================================================================
            // 2) MAĞAZA AYARLARI: Para birimi, KDV oranı & KDV gösterme, min sipariş, kargo, onay mesajı
            // =========================================================================
            item(key = "section_store_settings") {
                KYCard(modifier = Modifier.testTag("settings_store_config_card")) {
                    SectionHeaderRow(
                        icon = Icons.Outlined.Storefront,
                        iconTint = PrimaryBlue,
                        iconBg = PrimaryBlueSubtle,
                        title = "Mağaza Ayarları",
                        subtitle = "Para birimi, KDV, minimum sipariş, kargo ücreti ve sipariş onay şablonu"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Para Birimi Seçici
                    Text(
                        text = "Para Birimi",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyOption.entries.forEach { option ->
                            val isSelected = store.currencyCode == option.code
                            Surface(
                                onClick = {
                                    scope.launch {
                                        dataService.updateSettings(
                                            store.copy(
                                                currencyCode = option.code,
                                                currencySymbol = option.symbol
                                            )
                                        )
                                        onShowToast(
                                            KYToastData(
                                                message = "Para birimi ${option.code} (${option.symbol}) olarak güncellendi",
                                                type = KYToastType.SUCCESS
                                            )
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryBlue else BackgroundSlate,
                                border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderLight),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("currency_option_${option.code}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${option.code} (${option.symbol})",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) SurfaceWhite else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // KDV Oranı Seçici (%1, %10, %20) ve Fiyatlarda KDV Gösterme Anahtarı
                    Text(
                        text = "KDV Oranı (%)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 1, 10, 20).forEach { rate ->
                            val isSelected = store.vatRatePercent == rate
                            Surface(
                                onClick = {
                                    scope.launch {
                                        dataService.updateSettings(store.copy(vatRatePercent = rate))
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) PrimaryBlueSubtle else BackgroundSlate,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) PrimaryBlue else BorderLight
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("vat_rate_chip_$rate")
                            ) {
                                Text(
                                    text = "%$rate",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) PrimaryBlue else TextPrimary,
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsSwitchRow(
                        title = "Fiyatlarda KDV Göster",
                        subtitle = "Müşteri katalog sayfasında ürün fiyatlarının yanında '+ %${store.vatRatePercent} KDV Dahil' ibaresi gösterilir",
                        checked = store.showVatInPrices,
                        onCheckedChange = { checked ->
                            scope.launch {
                                dataService.updateSettings(store.copy(showVatInPrices = checked))
                            }
                        },
                        tag = "switch_show_vat_in_prices"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Minimum Sipariş Tutarı ve Kargo Ücreti
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KYInput(
                            value = minOrderInput,
                            onValueChange = { minOrderInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = "Min. Sipariş ($currency)",
                            placeholder = "500",
                            leadingIcon = Icons.Outlined.ReceiptLong,
                            modifier = Modifier.weight(1f),
                            tag = "input_min_order_amount"
                        )
                        KYInput(
                            value = shippingFeeInput,
                            onValueChange = { shippingFeeInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = "Kargo Ücreti ($currency)",
                            placeholder = "49",
                            leadingIcon = Icons.Outlined.LocalShipping,
                            modifier = Modifier.weight(1f),
                            tag = "input_shipping_fee"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sipariş Onay Mesajı Şablonu
                    KYInput(
                        value = confirmationTemplateInput,
                        onValueChange = { confirmationTemplateInput = it },
                        label = "Sipariş Onay Mesajı Şablonu (WhatsApp)",
                        placeholder = "Merhaba {musteri_adi}, {siparis_no} numaralı siparişiniz alınmıştır...",
                        helperText = "Kullanılabilir değişkenler: {musteri_adi}, {siparis_no}, {toplam_tutar}",
                        singleLine = false,
                        minLines = 3,
                        maxLines = 5,
                        tag = "input_order_confirmation_template"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hızlı Değişken Ekleme Çipleri
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("{musteri_adi}", "{siparis_no}", "{toplam_tutar}")) { token ->
                            Surface(
                                onClick = {
                                    if (!confirmationTemplateInput.contains(token)) {
                                        confirmationTemplateInput = "$confirmationTemplateInput $token"
                                    }
                                },
                                shape = BadgeRoundedPill,
                                color = PrimaryBlueSubtle,
                                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "+ $token",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PrimaryBlue,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    KYButton(
                        text = "Mağaza Ayarlarını Kaydet",
                        onClick = {
                            val parsedMinOrder = minOrderInput.toDoubleOrNull() ?: store.minOrderAmount
                            val parsedShipping = shippingFeeInput.toDoubleOrNull() ?: store.shippingFee
                            scope.launch {
                                dataService.updateSettings(
                                    store.copy(
                                        minOrderAmount = parsedMinOrder,
                                        shippingFee = parsedShipping,
                                        orderConfirmationTemplate = confirmationTemplateInput.trim().ifBlank {
                                            store.orderConfirmationTemplate
                                        }
                                    )
                                )
                                onShowToast(
                                    KYToastData(
                                        message = "Mağaza ayarları ve sipariş onay şablonu kaydedildi!",
                                        type = KYToastType.SUCCESS
                                    )
                                )
                            }
                        },
                        variant = ButtonVariant.PRIMARY,
                        leadingIcon = Icons.Outlined.Save,
                        modifier = Modifier.fillMaxWidth(),
                        tag = "btn_save_store_settings"
                    )
                }
            }

            // =========================================================================
            // 3) KATALOG GÖRÜNÜMÜ: Tema rengi seçici, grid sütun sayısı, fiyat gösterimi, ekran görüntüsü koruması
            // =========================================================================
            item(key = "section_catalog_appearance") {
                KYCard(modifier = Modifier.testTag("settings_catalog_appearance_card")) {
                    SectionHeaderRow(
                        icon = Icons.Outlined.Palette,
                        iconTint = AccentAmberDark,
                        iconBg = AccentAmberLight,
                        title = "Katalog Görünümü",
                        subtitle = "Müşteri katalog sayfası tema rengi, sütun düzeni, fiyat ve görsel koruması"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tema Rengi Seçici
                    Text(
                        text = "Tema Rengi Seçici",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.testTag("catalog_theme_color_picker_row")
                    ) {
                        items(catalogThemeColorPresets) { preset ->
                            val isSelected = store.catalogThemeColorHex.equals(preset.hex, ignoreCase = true)
                            Surface(
                                onClick = {
                                    scope.launch {
                                        dataService.updateSettings(store.copy(catalogThemeColorHex = preset.hex))
                                        onShowToast(
                                            KYToastData(
                                                message = "Katalog tema rengi '${preset.nameTr}' olarak seçildi",
                                                type = KYToastType.SUCCESS
                                            )
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) preset.color.copy(alpha = 0.12f) else BackgroundSlate,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) preset.color else BorderLight
                                ),
                                modifier = Modifier.testTag("theme_color_${preset.hex.removePrefix("#")}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(preset.color),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = SurfaceWhite,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = preset.nameTr,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Grid Sütun Sayısı Seçici (1, 2, 3 Sütun)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.GridView,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Grid Sütun Sayısı",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 3).forEach { cols ->
                                val isSelected = store.catalogGridColumns == cols
                                Surface(
                                    onClick = {
                                        scope.launch {
                                            dataService.updateSettings(store.copy(catalogGridColumns = cols))
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) PrimaryBlue else BackgroundSlate,
                                    border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderLight),
                                    modifier = Modifier.testTag("grid_columns_option_$cols")
                                ) {
                                    Text(
                                        text = "$cols Sütun",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) SurfaceWhite else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderLight)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Fiyat Gösterimi Anahtarı
                    SettingsSwitchRow(
                        title = "Kataloglarda Fiyat Gösterimi",
                        subtitle = "Kapatıldığında herkese açık müşteri kataloğunda ürün fiyatları gizlenir, yalnızca teklif/sipariş toplanır",
                        checked = store.catalogShowPrices,
                        onCheckedChange = { checked ->
                            scope.launch {
                                dataService.updateSettings(store.copy(catalogShowPrices = checked))
                            }
                        },
                        tag = "switch_catalog_show_prices"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Ekran Görüntüsü ve Görsel Kaydetme Koruması Anahtarı
                    SettingsSwitchRow(
                        title = "Ekran Görüntüsü ve Görsel Kaydetme Koruması",
                        subtitle = "Müşteri katalog sayfasında sağ tık / uzun basma ile ürün görsellerinin indirilmesini engeller",
                        checked = store.protectImagesFromSaving,
                        onCheckedChange = { checked ->
                            scope.launch {
                                dataService.updateSettings(store.copy(protectImagesFromSaving = checked))
                            }
                        },
                        tag = "switch_protect_images_from_saving"
                    )
                }
            }

            // =========================================================================
            // 4) BİLDİRİMLER: Yeni sipariş, düşük stok, haftalık özet anahtarları
            // =========================================================================
            item(key = "section_notifications") {
                KYCard(modifier = Modifier.testTag("settings_notifications_card")) {
                    SectionHeaderRow(
                        icon = Icons.Outlined.NotificationsActive,
                        iconTint = SuccessGreen,
                        iconBg = SuccessGreenLight,
                        title = "Bildirimler",
                        subtitle = "Yeni sipariş, düşük stok uyarısı ve haftalık performans özeti tercihleri"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingsSwitchRow(
                        title = "Yeni Sipariş Bildirimi",
                        subtitle = "Müşteri veya bayi katalog linkinden yeni sipariş oluşturulduğunda anında bildirim gönder",
                        checked = store.notifyNewOrder,
                        onCheckedChange = { checked ->
                            scope.launch {
                                dataService.updateSettings(store.copy(notifyNewOrder = checked))
                            }
                        },
                        tag = "switch_notify_new_order"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsSwitchRow(
                        title = "Düşük Stok Uyarısı",
                        subtitle = "Ürün stoğu belirlenen kritik eşiğin ($threshold adet) altına düştüğünde uyar",
                        checked = store.notifyLowStock,
                        onCheckedChange = { checked ->
                            scope.launch {
                                dataService.updateSettings(store.copy(notifyLowStock = checked))
                            }
                        },
                        tag = "switch_notify_low_stock"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsSwitchRow(
                        title = "Haftalık Analitik ve Satış Özeti",
                        subtitle = "Her Pazartesi katalog ziyaretçi, sepete ekleme ve ciro özetini raporla",
                        checked = store.notifyWeeklySummary,
                        onCheckedChange = { checked ->
                            scope.launch {
                                dataService.updateSettings(store.copy(notifyWeeklySummary = checked))
                            }
                        },
                        tag = "switch_notify_weekly_summary"
                    )
                }
            }

            // =========================================================================
            // 5) EKİP: Kullanıcı davet etme (Yönetici, Editör, Sadece görüntüleme)
            // =========================================================================
            item(key = "section_team_management") {
                KYCard(modifier = Modifier.testTag("settings_team_section_card")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SectionHeaderRow(
                                icon = Icons.Outlined.Badge,
                                iconTint = PrimaryBlue,
                                iconBg = PrimaryBlueSubtle,
                                title = "Ekip Yönetimi",
                                subtitle = "Yönetici, Editör veya Sadece görüntüleme yetkisiyle kullanıcı davet edin"
                            )
                        }
                        KYBadge(
                            text = "${store.teamMembers.size} Üye",
                            variant = BadgeVariant.NEUTRAL
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    store.teamMembers.forEach { member ->
                        var showRoleMenu by remember { mutableStateOf(false) }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BackgroundSlate,
                            border = BorderStroke(1.dp, BorderLight),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("team_member_row_${member.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = member.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                        Surface(
                                            shape = BadgeRoundedPill,
                                            color = if (member.isOwner) PrimaryBlueSubtle else SurfaceMuted
                                        ) {
                                            Text(
                                                text = member.statusText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (member.isOwner) PrimaryBlue else TextSecondary,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = member.emailOrPhone,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box {
                                        Surface(
                                            onClick = { if (!member.isOwner) showRoleMenu = true },
                                            shape = BadgeRoundedPill,
                                            color = when (member.role) {
                                                TeamRole.ADMIN -> PrimaryBlueSubtle
                                                TeamRole.EDITOR -> AccentAmberLight
                                                TeamRole.VIEWER -> SurfaceWhite
                                            },
                                            border = BorderStroke(1.dp, BorderLight),
                                            modifier = Modifier.testTag("team_member_role_chip_${member.id}")
                                        ) {
                                            Text(
                                                text = member.role.labelTr,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = when (member.role) {
                                                    TeamRole.ADMIN -> PrimaryBlue
                                                    TeamRole.EDITOR -> AccentAmberDark
                                                    TeamRole.VIEWER -> TextPrimary
                                                },
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showRoleMenu,
                                            onDismissRequest = { showRoleMenu = false },
                                            modifier = Modifier.background(SurfaceWhite)
                                        ) {
                                            TeamRole.entries.forEach { roleOption ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = roleOption.labelTr,
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                fontWeight = if (roleOption == member.role) FontWeight.Bold else FontWeight.Normal
                                                            )
                                                        )
                                                    },
                                                    onClick = {
                                                        showRoleMenu = false
                                                        scope.launch {
                                                            dataService.updateTeamMemberRole(member.id, roleOption)
                                                            onShowToast(
                                                                KYToastData(
                                                                    message = "${member.name} rolü '${roleOption.labelTr}' olarak güncellendi",
                                                                    type = KYToastType.SUCCESS
                                                                )
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    if (!member.isOwner) {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    dataService.removeTeamMember(member.id)
                                                    onShowToast(
                                                        KYToastData(
                                                            message = "${member.name} ekipten çıkarıldı",
                                                            type = KYToastType.INFO
                                                        )
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("btn_remove_team_member_${member.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = "Üyeyi Çıkar",
                                                tint = DangerRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    KYButton(
                        text = "Kullanıcı Davet Et (Yönetici / Editör / Görüntüleme)",
                        onClick = { showInviteTeamModal = true },
                        variant = ButtonVariant.SECONDARY,
                        leadingIcon = Icons.Outlined.PersonAdd,
                        modifier = Modifier.fillMaxWidth(),
                        tag = "btn_invite_team_member"
                    )
                }
            }

            // =========================================================================
            // 6) ABONELİK: Ücretsiz, Lite (aylık), Premium (yıllık) karşılaştırma tablosu & Limit yükseltme
            // =========================================================================
            item(key = "section_subscription") {
                SubscriptionSectionCard(
                    currentPlan = store.subscriptionPlan,
                    catalogCount = catalogs.size,
                    productCount = products.size,
                    onOpenUpgradeModal = { reason, plan ->
                        upgradeLimitReason = reason
                        upgradePreselectedPlan = plan
                        showUpgradeModal = true
                    },
                    onSwitchToFreePlan = {
                        scope.launch {
                            dataService.upgradeSubscriptionPlan(SubscriptionPlan.FREE)
                            onShowToast(
                                KYToastData(
                                    message = "Plan 'Ücretsiz' (Maks. 2 katalog & 50 ürün) olarak değiştirildi",
                                    type = KYToastType.INFO
                                )
                            )
                        }
                    }
                )
            }

            // =========================================================================
            // 7) AYARLAR > BAYİLER BÖLÜMÜ
            // =========================================================================
            item(key = "section_resellers") {
                KYCard(modifier = Modifier.testTag("settings_resellers_section_card")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SectionHeaderRow(
                                icon = Icons.Outlined.Groups,
                                iconTint = AccentAmber,
                                iconBg = AccentAmberLight,
                                title = TrStrings.Settings.RESELLERS_SECTION_TITLE,
                                subtitle = TrStrings.Settings.RESELLERS_SECTION_SUBTITLE
                            )
                        }
                        KYBadge(
                            text = "${resellers.size} Bayi",
                            variant = BadgeVariant.ACCENT
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StockMiniSummaryBox(
                            label = "Aktif Bayi",
                            count = resellers.size,
                            bgColor = PrimaryBlueSubtle,
                            textColor = PrimaryBlue,
                            onClick = { onOpenResellers(false) },
                            modifier = Modifier.weight(1f)
                        )
                        StockMiniSummaryBox(
                            label = "Bayi Siparişi",
                            count = resellerOrders.size,
                            bgColor = WarningAmberLight.copy(alpha = 0.7f),
                            textColor = WarningAmberDark,
                            onClick = { onOpenResellers(false) },
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            onClick = { onOpenResellers(false) },
                            shape = RoundedCornerShape(12.dp),
                            color = SuccessGreenLight.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.25f)),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${totalResellerRevenue.toInt()} $currency",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = SuccessGreen
                                )
                                Text(
                                    text = "Bayi Cirosu",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SuccessGreen
                                )
                            }
                        }
                    }

                    if (resellers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = BorderLight)
                        Spacer(modifier = Modifier.height(10.dp))

                        resellers.take(3).forEach { reseller ->
                            val primaryCatSlug = catalogs
                                .firstOrNull { it.id in reseller.visibleCatalogIds && it.isActive }
                                ?.let { it.slug.ifBlank { it.id } }
                                ?: catalogs.firstOrNull { it.isActive }?.let { it.slug.ifBlank { it.id } }
                                ?: reseller.shortCode
                            val resellerLink = "https://constantinopolishotel.store/?c=$primaryCatSlug&bayi=${reseller.shortCode}"
                            val resellerOrderCount = resellerOrders.count {
                                it.resellerId == reseller.id || it.resellerName.equals(reseller.name, ignoreCase = true)
                            }
                            val resellerRev = resellerOrders
                                .filter { it.resellerId == reseller.id || it.resellerName.equals(reseller.name, ignoreCase = true) }
                                .sumOf { it.totalAmount }

                            Surface(
                                shape = ButtonRoundedXl,
                                color = BackgroundSlate,
                                border = BorderStroke(1.dp, BorderLight),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("settings_reseller_row_${reseller.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reseller.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${reseller.formatPriceRuleSummary(currency)} • ${reseller.visibleCatalogIds.size} katalog • $resellerOrderCount sipariş (${resellerRev.toInt()} $currency)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                val clipboard =
                                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                clipboard?.setPrimaryClip(ClipData.newPlainText("Bayi Linki", resellerLink))
                                                onShowToast(
                                                    KYToastData(
                                                        message = "${TrStrings.Resellers.TOAST_RESELLER_LINK_COPIED}: $resellerLink",
                                                        type = KYToastType.SUCCESS
                                                    )
                                                )
                                            },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .testTag("settings_copy_reseller_${reseller.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ContentCopy,
                                                contentDescription = "Linki Kopyala",
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onOpenPublicCatalog(reseller.shortCode) },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .testTag("settings_preview_reseller_${reseller.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.OpenInNew,
                                                contentDescription = "Bayi Kataloğunu Aç",
                                                tint = AccentAmber,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KYButton(
                            text = TrStrings.Settings.BTN_OPEN_RESELLERS,
                            onClick = { onOpenResellers(false) },
                            variant = ButtonVariant.PRIMARY,
                            leadingIcon = Icons.Outlined.Groups,
                            modifier = Modifier.weight(1.3f),
                            tag = "settings_btn_open_resellers"
                        )
                        KYButton(
                            text = TrStrings.Resellers.BTN_ADD_RESELLER,
                            onClick = { showAddResellerModal = true },
                            variant = ButtonVariant.SECONDARY,
                            leadingIcon = Icons.Filled.Add,
                            modifier = Modifier.weight(1f),
                            tag = "settings_btn_add_reseller"
                        )
                    }
                }
            }

            // =========================================================================
            // 8) AYARLAR > STOK BÖLÜMÜ
            // =========================================================================
            item(key = "section_stock_management") {
                KYCard(modifier = Modifier.testTag("settings_stock_section_card")) {
                    SectionHeaderRow(
                        icon = Icons.Outlined.Inventory2,
                        iconTint = PrimaryBlue,
                        iconBg = PrimaryBlueSubtle,
                        title = TrStrings.Settings.STOCK_SECTION_TITLE,
                        subtitle = TrStrings.Settings.STOCK_SECTION_SUBTITLE
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StockMiniSummaryBox(
                            label = TrStrings.Inventory.FILTER_IN_STOCK,
                            count = inStockCount,
                            bgColor = SuccessGreenLight.copy(alpha = 0.55f),
                            textColor = SuccessGreen,
                            onClick = { onOpenInventory(InventoryFilter.IN_STOCK, false) },
                            modifier = Modifier.weight(1f)
                        )
                        StockMiniSummaryBox(
                            label = TrStrings.Inventory.FILTER_LOW_STOCK,
                            count = lowStockCount,
                            bgColor = WarningAmberLight.copy(alpha = 0.7f),
                            textColor = WarningAmberDark,
                            onClick = { onOpenInventory(InventoryFilter.LOW_STOCK, false) },
                            modifier = Modifier.weight(1f)
                        )
                        StockMiniSummaryBox(
                            label = TrStrings.Inventory.FILTER_OUT_OF_STOCK,
                            count = outOfStockCount,
                            bgColor = DangerRedLight.copy(alpha = 0.65f),
                            textColor = DangerRed,
                            onClick = { onOpenInventory(InventoryFilter.OUT_OF_STOCK, false) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = BorderLight)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BackgroundSlate)
                            .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = TrStrings.Settings.LOW_STOCK_THRESHOLD_LABEL,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = TrStrings.Settings.LOW_STOCK_THRESHOLD_HELPER,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (threshold > 1) {
                                        scope.launch {
                                            dataService.updateLowStockThreshold(threshold - 1)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceWhite)
                                    .border(1.dp, BorderLight, RoundedCornerShape(8.dp))
                                    .testTag("settings_btn_threshold_minus")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Remove,
                                    contentDescription = "Azalt",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "$threshold",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = PrimaryBlue
                            )

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        dataService.updateLowStockThreshold(threshold + 1)
                                    }
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrimaryBlueSubtle)
                                    .border(1.dp, PrimaryBlue.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .testTag("settings_btn_threshold_plus")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Artır",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    KYButton(
                        text = TrStrings.Settings.BTN_OPEN_INVENTORY,
                        onClick = { onOpenInventory(InventoryFilter.ALL, false) },
                        variant = ButtonVariant.PRIMARY,
                        leadingIcon = Icons.Outlined.Inventory2,
                        modifier = Modifier.fillMaxWidth(),
                        tag = "settings_btn_open_inventory"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KYButton(
                            text = "CSV İçe Aktar",
                            onClick = { showBulkCsvImportModal = true },
                            variant = ButtonVariant.SECONDARY,
                            leadingIcon = Icons.Outlined.FileUpload,
                            modifier = Modifier.weight(1f),
                            tag = "settings_btn_csv_import"
                        )
                        KYButton(
                            text = "CSV Dışa Aktar",
                            onClick = {
                                InventoryCsvExporter.exportProductsToCsvFileAndShare(
                                    context = context,
                                    products = products,
                                    catalogs = catalogs
                                )
                                onShowToast(
                                    KYToastData(
                                        message = TrStrings.Inventory.TOAST_CSV_EXPORT_SUCCESS,
                                        type = KYToastType.SUCCESS
                                    )
                                )
                            },
                            variant = ButtonVariant.SECONDARY,
                            leadingIcon = Icons.Outlined.FileDownload,
                            modifier = Modifier.weight(1f),
                            tag = "settings_btn_csv_export"
                        )
                    }
                }
            }

            // =========================================================================
            // 9) DESTEK, GİZLİLİK POLİTİKASI, OTURUM VE HESABI SİL
            // =========================================================================
            item(key = "section_support_privacy_account") {
                KYCard(modifier = Modifier.testTag("settings_support_and_account_card")) {
                    Text(
                        text = "Destek, Yasal ve Hesap İşlemleri",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Ana Ekrana Ekle (PWA Pinned Shortcut)
                    SettingsActionRow(
                        icon = Icons.Outlined.AddToHomeScreen,
                        title = "Ana Ekrana Ekle (PWA Kısayolu)",
                        subtitle = "Katalog Yap uygulamasını telefon ana ekranınıza tek dokunuşla sabitleyin",
                        onClick = {
                            val pinned = PwaOfflineAndShortcutHelper.requestAddToHomeScreen(
                                context = context,
                                title = store.businessName.ifBlank { "Katalog Yap" }
                            )
                            onShowToast(
                                KYToastData(
                                    message = if (pinned) {
                                        "Ana ekrana ekleme penceresi açıldı!"
                                    } else {
                                        "Ana ekran kısayolu oluşturuldu: ${store.businessName.ifBlank { "Katalog Yap" }}"
                                    },
                                    type = KYToastType.SUCCESS
                                )
                            )
                        },
                        tag = "btn_settings_add_to_home_screen"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Offline Önbellek Durumu & Simülasyon Anahtarı
                    SettingsSwitchRow(
                        title = "Çevrimdışı (Offline) Önbellek Görünümü",
                        subtitle = "${catalogs.size} katalog, ${products.size} ürün ve 50 MB görsel yerel önbellekte hazır",
                        checked = isOfflinePreviewActive,
                        onCheckedChange = { onToggleOfflinePreview() },
                        tag = "switch_settings_offline_cache_preview"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 404 Sayfası Önizleme
                    SettingsActionRow(
                        icon = Icons.Outlined.CloudOff,
                        title = "404 Sayfa Bulunamadı Ekranı",
                        subtitle = "Geçersiz veya yayında olmayan katalog linklerinde gösterilen güvenlik sayfası",
                        onClick = onOpenNotFound404,
                        tag = "btn_settings_open_404"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Yardım ve Destek
                    SettingsActionRow(
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        title = "Yardım ve Destek",
                        subtitle = "Sıkça sorulan sorular ve WhatsApp canlı destek hattı",
                        onClick = { showHelpModal = true },
                        tag = "btn_open_help_support"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Gizlilik Politikası
                    SettingsActionRow(
                        icon = Icons.Outlined.PrivacyTip,
                        title = "Gizlilik Politikası ve KVKK",
                        subtitle = "Veri güvenliği, görsel koruma ve kullanıcı sözleşmesi",
                        onClick = { showPrivacyModal = true },
                        tag = "btn_open_privacy_policy"
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderLight)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Demo Verileri Sıfırla
                    KYButton(
                        text = TrStrings.Settings.RESET_DEMO_DATA,
                        onClick = {
                            scope.launch {
                                dataService.resetStorage()
                                onShowToast(
                                    KYToastData(
                                        message = "Örnek katalog, stok ve analitik verileri yenilendi!",
                                        type = KYToastType.SUCCESS
                                    )
                                )
                            }
                        },
                        variant = ButtonVariant.SECONDARY,
                        leadingIcon = Icons.Outlined.Refresh,
                        modifier = Modifier.fillMaxWidth(),
                        tag = "settings_btn_reset_storage"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Çıkış Yap
                    KYButton(
                        text = TrStrings.Auth.LOGOUT_BUTTON,
                        onClick = {
                            authContext.logout {
                                onShowToast(
                                    KYToastData(
                                        message = TrStrings.Auth.LOGOUT_TOAST,
                                        type = KYToastType.INFO
                                    )
                                )
                                onLoggedOut()
                            }
                        },
                        variant = ButtonVariant.SECONDARY,
                        leadingIcon = Icons.AutoMirrored.Outlined.Logout,
                        modifier = Modifier.fillMaxWidth(),
                        tag = "btn_logout"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hesabı Sil (Onay Modalı İle)
                    Surface(
                        onClick = { showDeleteAccountModal = true },
                        shape = ButtonRoundedXl,
                        color = DangerRedLight.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_delete_account_trigger")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteForever,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hesabı Sil",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = DangerRed
                            )
                        }
                    }
                }
            }
        }
    }

    // MODALLAR
    EditBusinessProfileModal(
        isVisible = showEditProfileModal,
        settings = store,
        onDismiss = { showEditProfileModal = false },
        onSave = { updated ->
            scope.launch {
                dataService.updateSettings(updated)
                showEditProfileModal = false
                onShowToast(
                    KYToastData(
                        message = "İşletme profili başarıyla güncellendi!",
                        type = KYToastType.SUCCESS
                    )
                )
            }
        }
    )

    InviteTeamMemberModal(
        isVisible = showInviteTeamModal,
        onDismiss = { showInviteTeamModal = false },
        onInvite = { name, contact, role ->
            scope.launch {
                dataService.inviteTeamMember(name, contact, role)
                showInviteTeamModal = false
                onShowToast(
                    KYToastData(
                        message = "$name (${role.labelTr}) ekibe davet edildi!",
                        type = KYToastType.SUCCESS
                    )
                )
            }
        }
    )

    SubscriptionUpgradeModal(
        isVisible = showUpgradeModal,
        limitReasonText = upgradeLimitReason,
        currentPlan = store.subscriptionPlan,
        initialSelectedPlan = upgradePreselectedPlan,
        catalogCount = catalogs.size,
        productCount = products.size,
        onDismiss = { showUpgradeModal = false },
        onConfirmUpgrade = { newPlan ->
            scope.launch {
                dataService.upgradeSubscriptionPlan(newPlan)
                showUpgradeModal = false
                onShowToast(
                    KYToastData(
                        message = "Tebrikler! Demo ödeme onaylandı ve '${newPlan.titleTr}' planına geçildi.",
                        type = KYToastType.SUCCESS
                    )
                )
            }
        }
    )

    HelpAndSupportModal(
        isVisible = showHelpModal,
        onDismiss = { showHelpModal = false }
    )

    PrivacyPolicyModal(
        isVisible = showPrivacyModal,
        onDismiss = { showPrivacyModal = false }
    )

    DeleteAccountConfirmModal(
        isVisible = showDeleteAccountModal,
        businessName = store.businessName,
        onDismiss = { showDeleteAccountModal = false },
        onConfirmDelete = {
            showDeleteAccountModal = false
            scope.launch {
                dataService.deleteAccount()
                authContext.logout {
                    onShowToast(
                        KYToastData(
                            message = "Hesabınız ve tüm mağaza verileriniz kalıcı olarak silindi.",
                            type = KYToastType.INFO
                        )
                    )
                    onLoggedOut()
                }
            }
        }
    )

    BulkCsvImportModal(
        isVisible = showBulkCsvImportModal,
        catalogs = catalogs,
        currencySymbol = store.currencySymbol.ifBlank { "₺" },
        lowStockThreshold = threshold,
        onDismiss = { showBulkCsvImportModal = false },
        onConfirmImport = { targetCatalogId, importedProducts ->
            scope.launch {
                val count = dataService.bulkImportProducts(targetCatalogId, importedProducts)
                showBulkCsvImportModal = false
                onShowToast(
                    KYToastData(
                        message = "$count ${TrStrings.Inventory.TOAST_CSV_IMPORT_SUCCESS}",
                        type = KYToastType.SUCCESS
                    )
                )
            }
        }
    )

    AddOrEditResellerModal(
        isVisible = showAddResellerModal,
        editingReseller = null,
        allCatalogs = catalogs,
        currency = currency,
        onDismiss = { showAddResellerModal = false },
        onSave = { newReseller ->
            scope.launch {
                dataService.saveReseller(newReseller)
                showAddResellerModal = false
                onShowToast(
                    KYToastData(
                        message = TrStrings.Resellers.TOAST_RESELLER_SAVED,
                        type = KYToastType.SUCCESS
                    )
                )
            }
        }
    )
}

@Composable
private fun SectionHeaderRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SocialChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = BackgroundSlate,
        border = BorderStroke(1.dp, BorderLight),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = PrimaryBlue
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BackgroundSlate,
        border = BorderStroke(1.dp, BorderLight),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SurfaceWhite,
                    checkedTrackColor = PrimaryBlue
                ),
                modifier = Modifier.testTag(tag)
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = BackgroundSlate,
        border = BorderStroke(1.dp, BorderLight),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun StockMiniSummaryBox(
    label: String,
    count: Int,
    bgColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = textColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
        }
    }
}
