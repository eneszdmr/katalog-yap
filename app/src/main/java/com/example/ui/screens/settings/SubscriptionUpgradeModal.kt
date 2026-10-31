package com.example.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.SubscriptionPlan
import com.example.ui.components.BadgeVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYCard
import com.example.ui.components.KYModal
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * 6) PAKET VE KULLANIM DURUMU (/settings):
 * Şu an uygulama tamamen ücretsizdir ve tüm özellikler (Sınırsız Katalog, Sınırsız Ürün,
 * PDF Broşür/Fatura, Yapay Zeka Asistanı, Bayi Yönetimi ve Bulut Senkronizasyonu) sınırsız olarak açıktır.
 */
@Composable
fun SubscriptionSectionCard(
    currentPlan: SubscriptionPlan,
    catalogCount: Int,
    productCount: Int,
    onSelectPlanForUpgrade: (SubscriptionPlan) -> Unit = {},
    onOpenUpgradeModal: (String?, SubscriptionPlan) -> Unit = { _, _ -> },
    onSwitchToFreePlan: () -> Unit = {}
) {
    KYCard(modifier = Modifier.testTag("settings_section_subscription")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(ButtonRoundedXl)
                        .background(SuccessGreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Verified,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Paket & Kullanım Durumu",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Tüm özellikler ücretsiz ve sınırsız olarak kullanıma açıktır",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            KYBadge(
                text = "Tamamen Ücretsiz",
                variant = BadgeVariant.SUCCESS
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = BorderLight)
        Spacer(modifier = Modifier.height(14.dp))

        // Aktif Katalog ve Ürün Sayacı (Sınırsız)
        Surface(
            shape = ButtonRoundedXl,
            color = SurfaceMuted,
            border = BorderStroke(1.dp, BorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Oluşturulan Kataloglar",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "$catalogCount Katalog (Sınırsız)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = SuccessGreen
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Toplam Ürün Sayısı",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "$productCount Ürün (Sınırsız)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = ButtonRoundedXl,
            color = PrimaryBlueSubtle.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val freeFeatures = listOf(
                    "Sınırsız katalog ve sınırsız ürün ekleme",
                    "Yüksek kaliteli sıkıştırılmış görsel & anında bulut paylaşımı",
                    "Sipariş fişi / fatura ve katalog PDF broşür indirme",
                    "Gemini AI katalog oluşturucu, açıklama yazarı ve satış asistanı",
                    "Özel bayi fiyatlandırma ve canlı ziyaretçi takibi"
                )
                freeFeatures.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * İleride abonelik yapısı tekrar açıldığında kullanılabilecek uyumluluk modalı.
 * Şu anda tüm limitler kaldırıldığı için tetiklenmez; açılırsa tüm özelliklerin ücretsiz olduğunu bildirir.
 */
@Composable
fun SubscriptionUpgradeModal(
    isVisible: Boolean,
    limitReasonText: String? = null,
    currentPlan: SubscriptionPlan,
    initialSelectedPlan: SubscriptionPlan = SubscriptionPlan.FREE,
    catalogCount: Int,
    productCount: Int,
    onDismiss: () -> Unit,
    onConfirmUpgrade: (SubscriptionPlan) -> Unit
) {
    if (!isVisible) return

    KYModal(
        isVisible = true,
        title = "Tüm Özellikler Tamamen Ücretsiz",
        description = "Uygulamadaki tüm katalog, ürün, PDF ve yapay zeka özellikleri şu anda ücretsiz ve sınırsız olarak kullanıma açıktır.",
        confirmText = "Tamam",
        cancelText = "Kapat",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    ) {
        Surface(
            shape = ButtonRoundedXl,
            color = SuccessGreenLight,
            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("modal_subscription_upgrade")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WorkspacePremium,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Herhangi bir ücret veya limit olmadan sınırsız katalog ve ürün ekleyebilirsiniz.",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = SuccessGreen
                )
            }
        }
    }
}
