package com.example.ui.screens.catalogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.i18n.TrStrings
import com.example.model.Catalog
import com.example.ui.components.BadgeVariant
import com.example.ui.components.KYBadge
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CardRounded2xl
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueDark
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * 2 Sütunlu Grid İçindeki Katalog Kartı Bileşeni:
 * - Kapak görseli (kataloğun kapak görseli veya ilk ürün görseli)
 * - Sağ üstte "Paylaş" ikon butonu ve "⋯" menüsü (Düzenle, Kopyala, Etiket ekle, Gizle/Yayınla, Sil)
 * - Katalog adı, ürün sayısı
 * - Küçük metrikler: ziyaretçi sayısı (göz ikonu), sipariş sayısı (çanta ikonu)
 * - Uzun basınca çoklu seçim moduna geçiş
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CatalogCardItem(
    catalog: Catalog,
    coverUrl: String,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onShareClick: () -> Unit,
    onEditClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onAddTagClick: () -> Unit,
    onToggleVisibilityClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardRounded2xl)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("catalog_card_${catalog.id}"),
        shape = CardRounded2xl,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryBlueSubtle else SurfaceWhite
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) PrimaryBlue else BorderLight
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Kapak Görseli Alanı
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.12f)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryBlue.copy(alpha = 0.16f), PrimaryBlueDark.copy(alpha = 0.28f))
                        )
                    )
            ) {
                // Arka planda ikon yedek katmanı (görsel yüklenirken veya hata olursa görünür)
                Icon(
                    imageVector = Icons.Outlined.MenuBook,
                    contentDescription = null,
                    tint = PrimaryBlue.copy(alpha = 0.35f),
                    modifier = Modifier
                        .size(42.dp)
                        .align(Alignment.Center)
                )

                if (coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = catalog.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Hafif üst karartma gradyanı (butonların okunaklılığı için)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.38f), Color.Transparent)
                            )
                        )
                )

                // Sol Üst: Çoklu Seçim İkonu veya Yayın Durumu Rozeti
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    if (isSelectionMode) {
                        Surface(
                            shape = CircleShape,
                            color = SurfaceWhite.copy(alpha = 0.92f),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSelected) {
                                        Icons.Filled.CheckCircle
                                    } else {
                                        Icons.Outlined.RadioButtonUnchecked
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) PrimaryBlue else TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    } else if (!catalog.isActive) {
                        KYBadge(
                            text = TrStrings.Common.HIDDEN,
                            variant = BadgeVariant.NEUTRAL
                        )
                    } else if (catalog.tags.isNotEmpty()) {
                        KYBadge(
                            text = catalog.tags.first(),
                            variant = if (catalog.tags.first().equals("İndirimli", ignoreCase = true)) {
                                BadgeVariant.ACCENT
                            } else {
                                BadgeVariant.PRIMARY
                            }
                        )
                    }
                }

                // Sağ Üst: "Paylaş" İkon Butonu ve "⋯" Menüsü
                if (!isSelectionMode) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SurfaceWhite.copy(alpha = 0.92f),
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(32.dp)
                        ) {
                            IconButton(
                                onClick = onShareClick,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("btn_share_${catalog.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = TrStrings.Catalogs.SHARE_CONTENT_DESC,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Box {
                            Surface(
                                shape = CircleShape,
                                color = SurfaceWhite.copy(alpha = 0.92f),
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(32.dp)
                            ) {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("btn_more_${catalog.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = TrStrings.Catalogs.MORE_MENU_CONTENT_DESC,
                                        tint = TextPrimary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(SurfaceWhite)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(TrStrings.Catalogs.MENU_EDIT) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = null,
                                            tint = TextPrimary
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onEditClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(TrStrings.Catalogs.MENU_DUPLICATE) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = null,
                                            tint = TextPrimary
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onDuplicateClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(TrStrings.Catalogs.MENU_ADD_TAG) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Label,
                                            contentDescription = null,
                                            tint = TextPrimary
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onAddTagClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (catalog.isActive) {
                                                TrStrings.Catalogs.MENU_HIDE
                                            } else {
                                                TrStrings.Catalogs.MENU_PUBLISH
                                            }
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (catalog.isActive) {
                                                Icons.Outlined.VisibilityOff
                                            } else {
                                                Icons.Outlined.Visibility
                                            },
                                            contentDescription = null,
                                            tint = TextPrimary
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onToggleVisibilityClick()
                                    }
                                )
                                HorizontalDivider(color = BorderLight)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = TrStrings.Catalogs.MENU_DELETE,
                                            color = DangerRed
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = null,
                                            tint = DangerRed
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onDeleteClick()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Kart Alt Bilgileri: Katalog Adı, Ürün Sayısı ve Metrikler
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = catalog.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${catalog.productCount} ${TrStrings.Catalogs.PRODUCT_COUNT_SUFFIX}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    if (!catalog.isActive) {
                        Text(
                            text = TrStrings.Common.HIDDEN,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    } else if (catalog.tags.size > 1) {
                        Text(
                            text = "+${catalog.tags.size - 1} etiket",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = PrimaryBlue
                        )
                    }
                }

                if (catalog.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        catalog.tags.take(2).forEach { tagText ->
                            Surface(
                                shape = CircleShape,
                                color = PrimaryBlueSubtle,
                                border = BorderStroke(0.5.dp, PrimaryBlue.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = tagText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = SurfaceMuted)
                Spacer(modifier = Modifier.height(8.dp))

                // Küçük Metrikler: Ziyaretçi Sayısı (Göz İkonu) & Sipariş Sayısı (Çanta İkonu)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = TrStrings.Analytics.METRIC_VIEWS,
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatCompactMetric(catalog.viewCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingBag,
                            contentDescription = TrStrings.Navigation.TAB_ORDERS,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatCompactMetric(catalog.orderCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

private fun formatCompactMetric(value: Int): String {
    return if (value >= 1000) {
        val k = value / 1000.0
        String.format(java.util.Locale.US, "%.1fB", k).replace(".0B", "B")
    } else {
        value.toString()
    }
}
