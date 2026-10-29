package com.example.ui.screens.catalogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.i18n.TrStrings
import com.example.model.DealerMarginType
import com.example.model.ShareSettings
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentAmberLight
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.CardRounded2xl
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmberDark
import kotlin.math.roundToInt

/**
 * PAYLAŞIM AYARLARI (Aynı Sheet İçinde):
 * - Fiyatları göster / gizle
 * - Linke şifre koruması ekle (opsiyonel)
 * - Link son kullanma tarihi (opsiyonel)
 * - "Bayi olarak paylaş": Bayi fiyatı veya marj ekleme alanı (% veya sabit tutar)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShareSettingsSection(
    settings: ShareSettings,
    currencySymbol: String,
    onSettingsChange: (ShareSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var marginText by remember(settings.dealerMarginValue) {
        mutableStateOf(
            if (settings.dealerMarginValue % 1.0 == 0.0) {
                settings.dealerMarginValue.toInt().toString()
            } else {
                settings.dealerMarginValue.toString()
            }
        )
    }

    Surface(
        shape = CardRounded2xl,
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderLight),
        modifier = modifier
            .fillMaxWidth()
            .testTag("share_settings_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Başlık
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = TrStrings.Share.SETTINGS_HEADER,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            HorizontalDivider(color = SurfaceMuted)

            // 1) Fiyatları Göster / Gizle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = TrStrings.Share.SETTING_SHOW_PRICES,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = TrStrings.Share.SETTING_SHOW_PRICES_DESC,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Switch(
                    checked = settings.showPrices,
                    onCheckedChange = { onSettingsChange(settings.copy(showPrices = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SurfaceWhite,
                        checkedTrackColor = PrimaryBlue
                    ),
                    modifier = Modifier.testTag("switch_share_show_prices")
                )
            }

            HorizontalDivider(color = SurfaceMuted)

            // 2) "Bayi Olarak Paylaş" (Marj % veya Sabit Tutar)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = WarningAmberDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = TrStrings.Share.SETTING_DEALER_MODE,
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = TrStrings.Share.SETTING_DEALER_DESC,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = settings.isDealerShare,
                        onCheckedChange = { onSettingsChange(settings.copy(isDealerShare = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SurfaceWhite,
                            checkedTrackColor = AccentAmber
                        ),
                        modifier = Modifier.testTag("switch_share_dealer_mode")
                    )
                }

                if (settings.isDealerShare) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = ButtonRoundedXl,
                        color = AccentAmberLight.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Yüzde (%) / Sabit Tutar Seçici
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DealerMarginType.entries.forEach { type ->
                                    val isSelected = settings.dealerMarginType == type
                                    Surface(
                                        onClick = {
                                            onSettingsChange(settings.copy(dealerMarginType = type))
                                        },
                                        shape = ButtonRoundedXl,
                                        color = if (isSelected) PrimaryBlue else SurfaceWhite,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) PrimaryBlue else BorderLight
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("dealer_margin_type_${type.name}")
                                    ) {
                                        Text(
                                            text = if (type == DealerMarginType.PERCENTAGE) {
                                                TrStrings.Share.DEALER_MARGIN_PERCENT
                                            } else {
                                                "${TrStrings.Share.DEALER_MARGIN_FIXED} ($currencySymbol)"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSelected) SurfaceWhite else TextPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            // Marj Değeri Girişi + Canlı Örnek Hesaplama
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = marginText,
                                    onValueChange = { raw ->
                                        marginText = raw
                                        val parsed = raw.replace(",", ".").toDoubleOrNull() ?: 0.0
                                        onSettingsChange(settings.copy(dealerMarginValue = parsed))
                                    },
                                    label = {
                                        val unit = if (settings.dealerMarginType == DealerMarginType.PERCENTAGE) "%" else currencySymbol
                                        Text("${TrStrings.Share.DEALER_MARGIN_INPUT_LABEL} ($unit)")
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = ButtonRoundedXl,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SurfaceWhite,
                                        unfocusedContainerColor = SurfaceWhite
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_dealer_margin_value")
                                )

                                val sampleOriginal = 1000.0
                                val sampleAdjusted = settings.calculateAdjustedPrice(sampleOriginal).roundToInt()
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Örnek Fiyat",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "1000$currencySymbol → $sampleAdjusted$currencySymbol",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = WarningAmberDark
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = SurfaceMuted)

            // 3) Linke Şifre Koruması Ekle (Opsiyonel)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = TrStrings.Share.SETTING_PASSWORD,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                    Switch(
                        checked = settings.hasPassword,
                        onCheckedChange = {
                            val defaultPass = if (it && settings.password.isBlank()) "2025" else settings.password
                            onSettingsChange(settings.copy(hasPassword = it, password = defaultPass))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SurfaceWhite,
                            checkedTrackColor = PrimaryBlue
                        ),
                        modifier = Modifier.testTag("switch_share_password")
                    )
                }

                if (settings.hasPassword) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = settings.password,
                        onValueChange = { onSettingsChange(settings.copy(password = it)) },
                        placeholder = { Text(TrStrings.Share.SETTING_PASSWORD_PLACEHOLDER) },
                        singleLine = true,
                        shape = ButtonRoundedXl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_share_password")
                    )
                }
            }

            HorizontalDivider(color = SurfaceMuted)

            // 4) Link Son Kullanma Tarihi (Opsiyonel)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = TrStrings.Share.SETTING_EXPIRY,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                    Switch(
                        checked = settings.hasExpiryDate,
                        onCheckedChange = {
                            val defaultExp = if (it && settings.expiryDateText.isBlank()) {
                                TrStrings.Share.EXPIRY_7D
                            } else {
                                settings.expiryDateText
                            }
                            onSettingsChange(settings.copy(hasExpiryDate = it, expiryDateText = defaultExp))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SurfaceWhite,
                            checkedTrackColor = PrimaryBlue
                        ),
                        modifier = Modifier.testTag("switch_share_expiry")
                    )
                }

                if (settings.hasExpiryDate) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            TrStrings.Share.EXPIRY_24H,
                            TrStrings.Share.EXPIRY_7D,
                            TrStrings.Share.EXPIRY_30D,
                            "31.12.2026"
                        ).forEach { preset ->
                            val isSelected = settings.expiryDateText == preset
                            Surface(
                                onClick = {
                                    onSettingsChange(settings.copy(expiryDateText = preset))
                                },
                                shape = BadgeRoundedPill,
                                color = if (isSelected) PrimaryBlue else PrimaryBlueSubtle,
                                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) SurfaceWhite else PrimaryBlue,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = settings.expiryDateText,
                        onValueChange = { onSettingsChange(settings.copy(expiryDateText = it)) },
                        placeholder = { Text(TrStrings.Share.SETTING_EXPIRY_PLACEHOLDER) },
                        singleLine = true,
                        shape = ButtonRoundedXl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_share_expiry_date")
                    )
                }
            }

            HorizontalDivider(color = SurfaceMuted)

            // 5) Görsel Kaydetmeyi ve Ekran Görüntüsünü Zorlaştır (Opsiyonel Ayar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = TrStrings.Share.SETTING_IMAGE_PROTECTION,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = TrStrings.Share.SETTING_IMAGE_PROTECTION_DESC,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Switch(
                    checked = settings.disableImageSaving,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(disableImageSaving = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SurfaceWhite,
                        checkedTrackColor = PrimaryBlue
                    ),
                    modifier = Modifier.testTag("switch_share_image_protection")
                )
            }
        }
    }
}
