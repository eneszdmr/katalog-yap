package com.example.ui.screens.publiccatalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.i18n.TrStrings
import com.example.model.Product
import com.example.model.ProductVariantOption
import com.example.model.StockStatus
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYButton
import com.example.ui.components.KYCard
import com.example.ui.theme.BackgroundSlate
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

/**
 * MÜŞTERİ TAM EKRAN ÜRÜN DETAY MODALI:
 * - Yakınlaştırılabilir (pinch-to-zoom & çift dokunma) görsel galerisi
 * - Açıklama, varyant seçimi (ek fiyat hesaplamalı), adet seçici ve "Sepete ekle"
 * - Görsel kaydetme koruması aktifken uzun basmayı engeller
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PublicProductZoomModal(
    product: Product?,
    protectImages: Boolean,
    onImageSaveBlocked: () -> Unit,
    onDismiss: () -> Unit,
    onAddToCart: (product: Product, selectedVariants: Map<String, String>, unitPrice: Double, quantity: Int) -> Unit
) {
    if (product == null) return

    val isOutOfStock = product.stockStatus == StockStatus.OUT_OF_STOCK ||
        !product.inStock ||
        (product.trackStock && product.stockCount <= 0)

    val galleryUrls = remember(product) {
        if (product.galleryUrls.isNotEmpty()) {
            product.galleryUrls
        } else {
            listOf(
                product.imageUrl,
                "https://picsum.photos/seed/${product.id}-z1/800/800",
                "https://picsum.photos/seed/${product.id}-z2/800/800"
            )
        }
    }

    val pagerState = rememberPagerState(pageCount = { galleryUrls.size })
    var quantity by remember(product.id) { mutableIntStateOf(1) }

    // Varsayılan varyant seçimleri
    val selectedVariants = remember(product.id) {
        mutableStateMapOf<String, String>().apply {
            product.variants.forEach { group ->
                val firstOpt = group.optionDetails.firstOrNull()?.label ?: group.options.firstOrNull()
                if (!firstOpt.isNullOrBlank()) {
                    put(group.name, firstOpt)
                }
            }
        }
    }

    // Seçili varyantların ek fiyat toplamı
    val variantExtraPrice = remember(product, selectedVariants.toMap()) {
        product.variants.sumOf { group ->
            val chosen = selectedVariants[group.name]
            val detail = group.optionDetails.find { it.label == chosen }
            detail?.extraPrice ?: 0.0
        }
    }

    val effectiveUnitPrice = product.price + variantExtraPrice

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("public_product_detail_modal"),
            containerColor = BackgroundSlate,
            contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            bottomBar = {
                Surface(
                    color = SurfaceWhite,
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        HorizontalDivider(color = BorderLight)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Adet Seçici (- 1 +)
                            Surface(
                                shape = ButtonRoundedXl,
                                color = SurfaceMuted,
                                border = BorderStroke(1.dp, BorderLight)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    IconButton(
                                        onClick = { if (quantity > 1) quantity-- },
                                        enabled = !isOutOfStock && quantity > 1,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .testTag("modal_qty_decrease")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Remove,
                                            contentDescription = "Azalt",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = quantity.toString(),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    )
                                    IconButton(
                                        onClick = { if (quantity < 99) quantity++ },
                                        enabled = !isOutOfStock,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .testTag("modal_qty_increase")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "Artır",
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Sepete Ekle Butonu
                            val totalLinePrice = (effectiveUnitPrice * quantity).roundToInt()
                            KYButton(
                                text = if (isOutOfStock) {
                                    TrStrings.PublicCatalog.BADGE_OUT_OF_STOCK
                                } else {
                                    "${TrStrings.PublicCatalog.BTN_ADD_TO_CART} • $totalLinePrice ${product.currency}"
                                },
                                onClick = {
                                    if (!isOutOfStock) {
                                        onAddToCart(
                                            product,
                                            selectedVariants.toMap(),
                                            effectiveUnitPrice,
                                            quantity
                                        )
                                        onDismiss()
                                    }
                                },
                                enabled = !isOutOfStock,
                                variant = ButtonVariant.PRIMARY,
                                leadingIcon = if (isOutOfStock) null else Icons.Outlined.ShoppingBag,
                                modifier = Modifier.weight(1f),
                                tag = "modal_btn_add_to_cart"
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1) YAKINLAŞTIRILABİLİR GÖRSEL GALERİSİ
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.02f)
                        .background(Color(0xFF0F172A))
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        var scale by remember(page) { mutableFloatStateOf(1f) }
                        var offset by remember(page) { androidx.compose.runtime.mutableStateOf(Offset.Zero) }

                        val grayscaleFilter = remember(isOutOfStock) {
                            if (isOutOfStock) {
                                ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                            } else null
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(page, protectImages) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (scale > 1.05f) {
                                                scale = 1f
                                                offset = Offset.Zero
                                            } else {
                                                scale = 2.4f
                                            }
                                        },
                                        onLongPress = {
                                            if (protectImages) {
                                                onImageSaveBlocked()
                                            }
                                        }
                                    )
                                }
                                .pointerInput(page) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val newScale = (scale * zoom).coerceIn(1f, 3.2f)
                                        scale = newScale
                                        offset = if (newScale > 1f) {
                                            Offset(
                                                x = (offset.x + pan.x).coerceIn(-400f, 400f),
                                                y = (offset.y + pan.y).coerceIn(-400f, 400f)
                                            )
                                        } else {
                                            Offset.Zero
                                        }
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = galleryUrls[page],
                                contentDescription = product.name,
                                contentScale = ContentScale.Crop,
                                colorFilter = grayscaleFilter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                            )
                        }
                    }

                    // Üst Bar: Kapat (X) ve Sayaç
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onDismiss,
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("btn_close_product_modal")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = TrStrings.Common.CLOSE,
                                    tint = SurfaceWhite
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (protectImages) {
                                Surface(
                                    shape = BadgeRoundedPill,
                                    color = Color.Black.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Lock,
                                            contentDescription = null,
                                            tint = SurfaceWhite,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Korumalı",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SurfaceWhite
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = BadgeRoundedPill,
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "${pagerState.currentPage + 1} / ${galleryUrls.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SurfaceWhite,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Alt Yakınlaştırma İpucu & Nokta Göstergesi
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = BadgeRoundedPill,
                            color = Color.Black.copy(alpha = 0.55f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ZoomIn,
                                    contentDescription = null,
                                    tint = SurfaceWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = TrStrings.PublicCatalog.ZOOM_HINT,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SurfaceWhite
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(galleryUrls.size) { idx ->
                                val active = pagerState.currentPage == idx
                                Box(
                                    modifier = Modifier
                                        .size(if (active) 9.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (active) SurfaceWhite else SurfaceWhite.copy(alpha = 0.45f)
                                        )
                                )
                            }
                        }
                    }
                }

                // 2) ÜRÜN BİLGİLERİ, VARYANT SEÇİMİ VE AÇIKLAMA
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    KYCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KYBadge(
                                text = product.subCategory,
                                variant = BadgeVariant.PRIMARY
                            )
                            if (isOutOfStock) {
                                KYBadge(
                                    text = TrStrings.PublicCatalog.BADGE_OUT_OF_STOCK,
                                    variant = BadgeVariant.DANGER
                                )
                            } else {
                                Text(
                                    text = "SKU: ${product.sku}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${effectiveUnitPrice.roundToInt()} ${product.currency}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isOutOfStock) TextSecondary else PrimaryBlue
                            )
                            val orig = product.originalPrice
                            if (orig != null && orig > product.price) {
                                Text(
                                    text = "${orig.roundToInt()} ${product.currency}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        textDecoration = TextDecoration.LineThrough
                                    ),
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    // Varyant Seçimi (Beden, Renk vb.)
                    if (product.variants.isNotEmpty()) {
                        KYCard {
                            Text(
                                text = TrStrings.PublicCatalog.VARIANT_SELECT_PROMPT,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                product.variants.forEach { group ->
                                    val details = if (group.optionDetails.isNotEmpty()) {
                                        group.optionDetails
                                    } else {
                                        group.options.map { ProductVariantOption(label = it) }
                                    }
                                    val activeOption = selectedVariants[group.name]
                                        ?: details.firstOrNull()?.label.orEmpty()

                                    Column {
                                        Text(
                                            text = "${group.name}: $activeOption",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            details.forEach { opt ->
                                                val isSelected = activeOption == opt.label
                                                val labelText = buildString {
                                                    append(opt.label)
                                                    if (opt.extraPrice > 0) {
                                                        append(" (+${opt.extraPrice.roundToInt()}${product.currency})")
                                                    }
                                                }
                                                Surface(
                                                    onClick = { selectedVariants[group.name] = opt.label },
                                                    shape = ButtonRoundedXl,
                                                    color = if (isSelected) PrimaryBlueSubtle else SurfaceWhite,
                                                    border = BorderStroke(
                                                        width = if (isSelected) 1.5.dp else 1.dp,
                                                        color = if (isSelected) PrimaryBlue else BorderLight
                                                    )
                                                ) {
                                                    Text(
                                                        text = labelText,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = if (isSelected) PrimaryBlue else TextPrimary,
                                                        modifier = Modifier.padding(
                                                            horizontal = 14.dp,
                                                            vertical = 8.dp
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Ürün Açıklaması
                    KYCard {
                        Text(
                            text = TrStrings.ProductDetail.DESCRIPTION_LABEL,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
