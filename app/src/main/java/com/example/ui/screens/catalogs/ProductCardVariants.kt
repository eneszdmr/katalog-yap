package com.example.ui.screens.catalogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.i18n.TrStrings
import com.example.model.Product
import com.example.model.StockStatus
import com.example.ui.components.BadgeVariant
import com.example.ui.components.KYBadge
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.CardRounded2xl
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class ProductViewMode(val labelTr: String) {
    GRID_2_COL(TrStrings.CatalogDetail.VIEW_GRID),
    LIST(TrStrings.CatalogDetail.VIEW_LIST),
    LARGE_CARD(TrStrings.CatalogDetail.VIEW_LARGE)
}

enum class ProductSortOption(val labelTr: String) {
    CUSTOM(TrStrings.CatalogDetail.SORT_CUSTOM),
    NEWEST(TrStrings.CatalogDetail.SORT_NEWEST),
    PRICE_ASC(TrStrings.CatalogDetail.SORT_PRICE_ASC),
    PRICE_DESC(TrStrings.CatalogDetail.SORT_PRICE_DESC),
    NAME_AZ(TrStrings.CatalogDetail.SORT_NAME_AZ)
}

@Composable
fun ProductStockBadge(
    status: StockStatus,
    modifier: Modifier = Modifier
) {
    val variant = when (status) {
        StockStatus.IN_STOCK -> BadgeVariant.SUCCESS
        StockStatus.LOW_STOCK -> BadgeVariant.WARNING
        StockStatus.OUT_OF_STOCK -> BadgeVariant.DANGER
    }
    KYBadge(
        text = status.labelTr,
        variant = variant,
        modifier = modifier
    )
}

@Composable
fun ProductPriceDisplay(
    price: Double,
    originalPrice: Double?,
    currency: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "${price.toInt()} $currency",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = PrimaryBlue
        )
        if (originalPrice != null && originalPrice > price) {
            Text(
                text = "${originalPrice.toInt()} $currency",
                style = MaterialTheme.typography.bodySmall.copy(
                    textDecoration = TextDecoration.LineThrough
                ),
                color = TextMuted
            )
        }
    }
}

/**
 * 1) 2 Sütunlu Grid Görünümü İçin Ürün Kartı.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductGridCard(
    product: Product,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isManageMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragAccumulated by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardRounded2xl)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(
                if (isManageMode) {
                    Modifier.pointerInput(product.id, canMoveUp, canMoveDown) {
                        detectVerticalDragGestures(
                            onDragStart = { dragAccumulated = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulated += dragAmount
                                if (dragAccumulated < -70f && canMoveUp) {
                                    onMoveUp()
                                    dragAccumulated = 0f
                                } else if (dragAccumulated > 70f && canMoveDown) {
                                    onMoveDown()
                                    dragAccumulated = 0f
                                }
                            }
                        )
                    }
                } else Modifier
            )
            .testTag("product_grid_card_${product.id}"),
        shape = CardRounded2xl,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryBlueSubtle else SurfaceWhite
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) PrimaryBlue else BorderLight
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(SurfaceMuted)
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Sol Üst: Çoklu Seçim Kutusu veya Stok Rozeti
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    if (isSelectionMode) {
                        SelectionCheckBadge(isSelected = isSelected)
                    } else {
                        ProductStockBadge(status = product.stockStatus)
                    }
                }

                // Sağ Üst: Yönet Modunda Sürükle / Sırala Tutamaçları
                if (isManageMode) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(ButtonRoundedXl)
                            .background(SurfaceWhite.copy(alpha = 0.92f))
                            .padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = canMoveUp,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowUp,
                                contentDescription = "Yukarı Taşı",
                                tint = if (canMoveUp) PrimaryBlue else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.DragIndicator,
                            contentDescription = "Sürükle",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        IconButton(
                            onClick = onMoveDown,
                            enabled = canMoveDown,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Aşağı Taşı",
                                tint = if (canMoveDown) PrimaryBlue else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = product.subCategory,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                ProductPriceDisplay(
                    price = product.price,
                    originalPrice = product.originalPrice,
                    currency = product.currency
                )
            }
        }
    }
}

/**
 * 2) Liste Görünümü İçin Ürün Satır Kartı (Sürükle-bırak destekli).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductListRowCard(
    product: Product,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isManageMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragAccumulated by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardRounded2xl)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(
                if (isManageMode) {
                    Modifier.pointerInput(product.id, canMoveUp, canMoveDown) {
                        detectVerticalDragGestures(
                            onDragStart = { dragAccumulated = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulated += dragAmount
                                if (dragAccumulated < -65f && canMoveUp) {
                                    onMoveUp()
                                    dragAccumulated = 0f
                                } else if (dragAccumulated > 65f && canMoveDown) {
                                    onMoveDown()
                                    dragAccumulated = 0f
                                }
                            }
                        )
                    }
                } else Modifier
            )
            .testTag("product_list_card_${product.id}"),
        shape = CardRounded2xl,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryBlueSubtle else SurfaceWhite
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) PrimaryBlue else BorderLight
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                SelectionCheckBadge(isSelected = isSelected)
                Spacer(modifier = Modifier.width(10.dp))
            }

            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(ButtonRoundedXl)
                    .background(SurfaceMuted)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.subCategory,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    ProductStockBadge(status = product.stockStatus)
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                ProductPriceDisplay(
                    price = product.price,
                    originalPrice = product.originalPrice,
                    currency = product.currency
                )
            }

            if (isManageMode) {
                Spacer(modifier = Modifier.width(6.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowUp,
                            contentDescription = "Yukarı Taşı",
                            tint = if (canMoveUp) PrimaryBlue else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.DragIndicator,
                        contentDescription = "Sürükle-Bırak",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Aşağı Taşı",
                            tint = if (canMoveDown) PrimaryBlue else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3) Büyük Kart Görünümü İçin Tam Genişlik Ürün Kartı.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductLargeCard(
    product: Product,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isManageMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardRounded2xl)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("product_large_card_${product.id}"),
        shape = CardRounded2xl,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryBlueSubtle else SurfaceWhite
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) PrimaryBlue else BorderLight
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.45f)
                    .background(SurfaceMuted)
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    if (isSelectionMode) {
                        SelectionCheckBadge(isSelected = isSelected)
                    } else {
                        ProductStockBadge(status = product.stockStatus)
                    }
                }

                if (isManageMode) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(ButtonRoundedXl)
                            .background(SurfaceWhite.copy(alpha = 0.92f))
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = canMoveUp,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowUp,
                                contentDescription = "Yukarı Taşı",
                                tint = if (canMoveUp) PrimaryBlue else TextMuted
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.DragIndicator,
                            contentDescription = "Sürükle",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        IconButton(
                            onClick = onMoveDown,
                            enabled = canMoveDown,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Aşağı Taşı",
                                tint = if (canMoveDown) PrimaryBlue else TextMuted
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KYBadge(
                        text = product.subCategory,
                        variant = BadgeVariant.NEUTRAL
                    )
                    Text(
                        text = "SKU: ${product.sku}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                if (product.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                ProductPriceDisplay(
                    price = product.price,
                    originalPrice = product.originalPrice,
                    currency = product.currency
                )
            }
        }
    }
}

@Composable
private fun SelectionCheckBadge(isSelected: Boolean) {
    Surface(
        shape = CircleShape,
        color = SurfaceWhite.copy(alpha = 0.94f),
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
}
