package com.example.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.SubscriptionPlan
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYButton
import com.example.ui.components.KYCard
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentAmberDark
import com.example.ui.theme.AccentAmberLight
import com.example.ui.theme.BackgroundSlate
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.CardRounded2xl
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

/**
 * AYARLAR > ABONELİK KARTI VE PLAN KARŞILAŞTIRMA TABLOSU:
 * - Ücretsiz, Lite (aylık), Premium (yıllık) planlarını karşılaştıran tablo
 * - Plana göre limitler (Ücretsizde max 2 katalog ve 50 ürün)
 * - Mevcut katalog ve ürün kullanım çubukları
 * - Limit aşılınca yükseltme uyarısı ve Demo Ödeme ile Yükseltme ekranı tetikleyicisi
 */
@Composable
fun SubscriptionSectionCard(
    currentPlan: SubscriptionPlan,
    catalogCount: Int,
    productCount: Int,
    onOpenUpgradeModal: (reason: String?, preselectedPlan: SubscriptionPlan) -> Unit,
    onSwitchToFreePlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCatalogLimitExceeded = currentPlan.isCatalogLimitExceeded(catalogCount)
    val isProductLimitExceeded = currentPlan.isProductLimitExceeded(productCount)
    val isAnyLimitExceeded = isCatalogLimitExceeded || isProductLimitExceeded

    KYCard(modifier = modifier.testTag("settings_subscription_section_card")) {
        // Başlık Satırı
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryBlueSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RocketLaunch,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Abonelik ve Plan Limitleri",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Ücretsiz, Lite (aylık) ve Premium (yıllık) paket karşılaştırması",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            KYBadge(
                text = currentPlan.titleTr,
                variant = when (currentPlan) {
                    SubscriptionPlan.FREE -> BadgeVariant.NEUTRAL
                    SubscriptionPlan.LITE_MONTHLY -> BadgeVariant.ACCENT
                    SubscriptionPlan.PREMIUM_YEARLY -> BadgeVariant.SUCCESS
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mevcut Kullanım & Limit Durumu Kutusu
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BackgroundSlate,
            border = BorderStroke(1.dp, BorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Mevcut Plan Kullanımınız (${currentPlan.titleTr} • ${currentPlan.priceLabelTr})",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Katalog Limiti Çubuğu
                val maxCat = currentPlan.maxCatalogs
                val catProgress = if (maxCat != null && maxCat > 0) {
                    (catalogCount.toFloat() / maxCat.toFloat()).coerceIn(0f, 1f)
                } else 0.25f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Katalog Limiti",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = if (maxCat != null) "$catalogCount / $maxCat katalog" else "$catalogCount / Sınırsız",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isCatalogLimitExceeded) DangerRed else TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { catProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(BadgeRoundedPill),
                    color = if (isCatalogLimitExceeded) DangerRed else PrimaryBlue,
                    trackColor = BorderLight
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Ürün Limiti Çubuğu
                val maxProd = currentPlan.maxProducts
                val prodProgress = if (maxProd != null && maxProd > 0) {
                    (productCount.toFloat() / maxProd.toFloat()).coerceIn(0f, 1f)
                } else 0.2f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Toplam Ürün Limiti",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = if (maxProd != null) "$productCount / $maxProd ürün" else "$productCount / Sınırsız",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isProductLimitExceeded) DangerRed else TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { prodProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(BadgeRoundedPill),
                    color = if (isProductLimitExceeded) DangerRed else SuccessGreen,
                    trackColor = BorderLight
                )
            }
        }

        // Limit Aşıldıysa Uyarı Kutusu
        if (isAnyLimitExceeded) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DangerRedLight.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subscription_limit_exceeded_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Plan Limitiniz Doldu!",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = DangerRed
                        )
                        Text(
                            text = "Ücretsiz planda en fazla 2 katalog ve 50 ürün hakkınız bulunur ($catalogCount katalog, $productCount ürün mevcut). Yeni ekleme yapmak için planınızı yükseltin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                    KYButton(
                        text = "Yükselt",
                        onClick = {
                            onOpenUpgradeModal(
                                "Ücretsiz plan limitine ($catalogCount/2 katalog, $productCount/50 ürün) ulaştınız.",
                                SubscriptionPlan.LITE_MONTHLY
                            )
                        },
                        variant = ButtonVariant.PRIMARY,
                        tag = "btn_banner_upgrade_plan"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // PLAN KARŞILAŞTIRMA TABLOSU (Ücretsiz vs Lite Aylık vs Premium Yıllık)
        Text(
            text = "Plan Karşılaştırma Tablosu",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SurfaceWhite,
            border = BorderStroke(1.dp, BorderLight),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("subscription_comparison_table")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Tablo Başlık Satırı
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceMuted)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Özellik",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary,
                        modifier = Modifier.weight(1.25f)
                    )
                    Text(
                        text = "Ücretsiz\n0 ₺",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (currentPlan == SubscriptionPlan.FREE) PrimaryBlue else TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.9f)
                    )
                    Text(
                        text = "Lite (Aylık)\n299 ₺/ay",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (currentPlan == SubscriptionPlan.LITE_MONTHLY) PrimaryBlue else AccentAmberDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Premium\n2.490 ₺/yıl",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (currentPlan == SubscriptionPlan.PREMIUM_YEARLY) PrimaryBlue else SuccessGreen,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }

                val rows = listOf(
                    listOf("Maks. Katalog", "2 Katalog", "10 Katalog", "Sınırsız"),
                    listOf("Maks. Ürün", "50 Ürün", "500 Ürün", "Sınırsız"),
                    listOf("PDF & Görsel Paylaşım", "Filigranlı", "Filigransız", "Özel Logolu"),
                    listOf("Bayi Özel Link & Marj", "1 Bayi", "10 Bayi", "Sınırsız"),
                    listOf("AI Ürün Doldurma", "Günlük 5", "Aylık 200", "Sınırsız"),
                    listOf("Ekip Üyesi Daveti", "1 Kullanıcı", "3 Kullanıcı", "10 Kullanıcı")
                )

                rows.forEachIndexed { idx, row ->
                    HorizontalDivider(color = BorderLight)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (idx % 2 == 1) BackgroundSlate.copy(alpha = 0.5f) else SurfaceWhite)
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = row[0],
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary,
                            modifier = Modifier.weight(1.25f)
                        )
                        Text(
                            text = row[1],
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.9f)
                        )
                        Text(
                            text = row[2],
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = row[3],
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SuccessGreen,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Plan Seçim / Yükseltme Butonları
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KYButton(
                text = if (currentPlan == SubscriptionPlan.LITE_MONTHLY) "Lite Aktif • Yükselt" else "Lite'a Geç (299 ₺/ay)",
                onClick = {
                    onOpenUpgradeModal(null, SubscriptionPlan.LITE_MONTHLY)
                },
                variant = ButtonVariant.SECONDARY,
                modifier = Modifier.weight(1f),
                tag = "settings_btn_upgrade_lite"
            )
            KYButton(
                text = if (currentPlan == SubscriptionPlan.PREMIUM_YEARLY) "Premium Aktif" else "Premium'a Yükselt",
                onClick = {
                    onOpenUpgradeModal(null, SubscriptionPlan.PREMIUM_YEARLY)
                },
                variant = ButtonVariant.PRIMARY,
                leadingIcon = Icons.Outlined.AutoAwesome,
                modifier = Modifier.weight(1.1f),
                tag = "settings_btn_upgrade_premium"
            )
        }

        if (currentPlan != SubscriptionPlan.FREE) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                onClick = onSwitchToFreePlan,
                shape = ButtonRoundedXl,
                color = SurfaceMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_btn_switch_free_plan")
            ) {
                Text(
                    text = "Ücretsiz Plana Dön (Limit testini görmek için: Maks. 2 katalog & 50 ürün)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                )
            }
        }
    }
}

/**
 * LİMİT AŞILINCA VEYA AYARLARDAN AÇILAN YÜKSELTME & DEMO ÖDEME EKRANI (Modal):
 * - Ücretsiz planda max 2 katalog veya 50 ürün limiti aşıldığında otomatik gösterilir.
 * - Lite (Aylık) ve Premium (Yıllık) planları seçilebilir.
 * - Demo ödeme kartı ile tek tıkla plan yükseltme tamamlanır.
 */
@Composable
fun SubscriptionUpgradeModal(
    isVisible: Boolean,
    limitReasonText: String? = null,
    currentPlan: SubscriptionPlan,
    initialSelectedPlan: SubscriptionPlan = SubscriptionPlan.LITE_MONTHLY,
    catalogCount: Int,
    productCount: Int,
    onDismiss: () -> Unit,
    onConfirmUpgrade: (SubscriptionPlan) -> Unit
) {
    if (!isVisible) return

    var selectedPlan by remember(initialSelectedPlan) {
        mutableStateOf(
            if (initialSelectedPlan == SubscriptionPlan.FREE) SubscriptionPlan.LITE_MONTHLY else initialSelectedPlan
        )
    }
    var cardHolderName by remember { mutableStateOf("SELİN KAYA") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = CardRounded2xl,
            color = SurfaceWhite,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag("modal_subscription_upgrade")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // Üst Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlueSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RocketLaunch,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Planınızı Yükseltin",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Daha fazla katalog, ürün ve bayi limiti açın",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_upgrade_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Kapat",
                            tint = TextSecondary
                        )
                    }
                }

                // Limit Aşım Uyarısı (Varsa)
                if (!limitReasonText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentAmberLight,
                        border = BorderStroke(1.dp, AccentAmber),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upgrade_modal_limit_reason_box")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = AccentAmberDark,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Limit Aşıldı (${currentPlan.titleTr} Plan)",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = AccentAmberDark
                                )
                                Text(
                                    text = limitReasonText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Plan Seçim Kartları (Lite Aylık & Premium Yıllık)
                listOf(SubscriptionPlan.LITE_MONTHLY, SubscriptionPlan.PREMIUM_YEARLY).forEach { plan ->
                    val isSelected = selectedPlan == plan
                    Surface(
                        onClick = { selectedPlan = plan },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) PrimaryBlueSubtle else SurfaceWhite,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) PrimaryBlue else BorderLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .testTag("upgrade_plan_option_${plan.name}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = plan.titleTr,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Surface(
                                        shape = BadgeRoundedPill,
                                        color = if (plan == SubscriptionPlan.PREMIUM_YEARLY) SuccessGreenLight else AccentAmberLight
                                    ) {
                                        Text(
                                            text = plan.badgeTr,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (plan == SubscriptionPlan.PREMIUM_YEARLY) SuccessGreen else AccentAmberDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = plan.priceLabelTr,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = PrimaryBlue
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val perks = if (plan == SubscriptionPlan.LITE_MONTHLY) {
                                listOf(
                                    "10 Kataloğa ve 500 ürüne kadar oluşturma",
                                    "Filigransız PDF broşür ve görsel paylaşımı",
                                    "10 Bayi ve bayi özel fiyat linki"
                                )
                            } else {
                                listOf(
                                    "Sınırsız katalog, sınırsız ürün ve sınırsız bayi",
                                    "Sınırsız ✨ Yapay Zeka ile ürün doldurma",
                                    "10 Ekip üyesi daveti ve öncelikli WhatsApp destek"
                                )
                            }

                            perks.forEach { perk ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = perk,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // DEMO ÖDEME KUTUSU
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BackgroundSlate,
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.CreditCard,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Demo Ödeme Bilgileri",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }
                            Surface(
                                shape = BadgeRoundedPill,
                                color = PrimaryBlueSubtle
                            ) {
                                Text(
                                    text = "Demo Modu",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PrimaryBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Kart: **** **** **** 4242 • SKT: 12/28 • CVV: *** ($cardHolderName)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gerçek kartınızdan tahsilat yapılmaz; abonelik anında aktifleşir.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Aksiyon Butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KYButton(
                        text = "Vazgeç",
                        onClick = onDismiss,
                        variant = ButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f),
                        tag = "btn_cancel_upgrade_modal"
                    )
                    KYButton(
                        text = "Demo Ödeme ile Yükselt (${selectedPlan.priceLabelTr})",
                        onClick = { onConfirmUpgrade(selectedPlan) },
                        variant = ButtonVariant.PRIMARY,
                        leadingIcon = Icons.Outlined.Verified,
                        modifier = Modifier.weight(1.6f),
                        tag = "btn_confirm_upgrade_modal"
                    )
                }
            }
        }
    }
}
