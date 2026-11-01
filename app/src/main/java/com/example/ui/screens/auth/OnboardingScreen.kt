package com.example.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.auth.LocalAuthContext
import com.example.model.BusinessType
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYButton
import com.example.ui.components.KYCard
import com.example.ui.components.KYInput
import com.example.ui.components.KYToastData
import com.example.ui.components.KYToastType
import com.example.ui.theme.BackgroundSlate
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryBlueDark
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Hızlı Tek Adımlı Mağaza Başlangıç Ekranı:
 * Kullanıcının uygulamaya girdikten sonra vakit kaybetmeden doğrudan katalog oluşturmaya
 * odaklanabilmesi için yalnızca "İşletme / Mağaza Adı" bilgisini ister.
 * Diğer tüm ayarlar (logo, şehir, iletişim, para birimi vb.) istendiğinde Ayarlar'dan düzenlenebilir.
 */
@Composable
fun OnboardingScreen(
    onOnboardingFinished: () -> Unit,
    onShowToast: (KYToastData) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val authContext = LocalAuthContext.current

    var businessName by rememberSaveable { mutableStateOf("") }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by rememberSaveable { mutableStateOf(false) }

    fun submitQuickOnboarding() {
        if (businessName.isBlank()) {
            nameError = "Lütfen işletme veya mağaza adınızı girin."
            return
        }
        nameError = null
        isSaving = true
        authContext.completeOnboarding(
            businessName = businessName.trim(),
            businessType = BusinessType.OTHER.labelTr,
            logoUri = "",
            whatsappNumber = authContext.formattedPendingPhone,
            city = "",
            currencyCode = "TRY",
            currencySymbol = "₺",
            catalogLanguage = "Türkçe (TR)",
            onComplete = {
                isSaving = false
                onShowToast(
                    KYToastData(
                        message = "${businessName.trim()} mağazanız hazır! Hemen katalog oluşturabilirsiniz.",
                        type = KYToastType.SUCCESS
                    )
                )
                onOnboardingFinished()
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundSlate)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("onboarding_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Üst Kurumsal Karşılama Başlığı (Antrasit & Kurumsal Yeşil)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1E293B),
                                    Color(0xFF0F766E)
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Storefront,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hoş Geldiniz",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = PrimaryBlueLight
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Mağazanızı Hemen Oluşturun",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sadece işletme adınızı girerek saniyeler içinde ilk dijital kataloğunuzu hazırlamaya başlayın.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Tek Adımlı İşletme Adı Kartı
            KYCard {
                Text(
                    text = "İşletme Bilgisi",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Katalog kapaklarınızda ve paylaşılan sipariş bağlantılarında müşterilerinize görünecek mağaza adınızı yazın.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                KYInput(
                    value = businessName,
                    onValueChange = {
                        businessName = it
                        if (nameError != null) nameError = null
                    },
                    label = "İşletme / Mağaza Adı *",
                    placeholder = "Örn: Vera Butik, Anadolu Tekstil, Özkan Mobilya",
                    leadingIcon = Icons.Outlined.Storefront,
                    errorText = nameError,
                    tag = "input_business_name"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Hızlı Bilgi Satırları
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryBlueSubtle,
                    border = BorderStroke(1.dp, PrimaryBlueLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickOnboardingFeatureRow(
                            icon = Icons.Outlined.MenuBook,
                            text = "Fotoğraflarınızdan saniyeler içinde katalog oluşturun"
                        )
                        QuickOnboardingFeatureRow(
                            icon = Icons.Outlined.Share,
                            text = "Tek tıkla WhatsApp veya özel web linki ile paylaşın"
                        )
                        QuickOnboardingFeatureRow(
                            icon = Icons.Outlined.CheckCircle,
                            text = "Logo ve diğer detayları dilediğiniz zaman Ayarlar'dan ekleyin"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                KYButton(
                    text = "Katalog Oluşturmaya Başla",
                    onClick = { submitQuickOnboarding() },
                    variant = ButtonVariant.PRIMARY,
                    leadingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                    isLoading = isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    tag = "btn_onboarding_next"
                )
            }

            // Alt bilgi
            Surface(
                shape = CircleShape,
                color = SurfaceMuted,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Kurumsal • Hızlı • Ücretsiz Dijital Katalog",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickOnboardingFeatureRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryBlueDark,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = TextPrimary
        )
    }
}
