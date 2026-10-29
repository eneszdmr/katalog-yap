package com.example.ui.screens.catalogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.auth.LocalAuthContext
import com.example.i18n.TrStrings
import com.example.services.LocalStorageDataService
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYButton
import com.example.ui.components.KYCard
import com.example.ui.components.KYEmptyState
import com.example.ui.components.KYToastData
import com.example.ui.components.KYToastType
import com.example.ui.components.KYTopBar
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
import kotlinx.coroutines.launch

/**
 * ÜRÜN DETAY EKRANI (/products/:id):
 * - Görsel galerisi (kaydırılabilir HorizontalPager, sayfa göstergeli)
 * - Ürün adı, fiyat (indirimliyse eski fiyat üstü çizili), açıklama, SKU, kategori, stok, varyantlar
 * - Alt sabit çubukta "Düzenle" ve "Paylaş" butonları.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onBackClick: () -> Unit,
    onEditProductClick: (String) -> Unit,
    onShowToast: (KYToastData) -> Unit,
    onOpenPublicCatalog: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    val dataService = remember { LocalStorageDataService.getInstance(context) }
    val scope = rememberCoroutineScope()
    val authContext = LocalAuthContext.current
    val store = authContext.storeSettings

    val products by dataService.products.collectAsState()
    val product = remember(products, productId) {
        products.find { it.id == productId }
    }

    var showEditModal by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    val selectedVariants = remember(productId) { mutableStateMapOf<String, String>() }

    if (product == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundSlate)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            KYEmptyState(
                icon = Icons.Outlined.Inventory2,
                title = "Ürün bulunamadı",
                description = "Bu ürün silinmiş olabilir.",
                actionLabel = TrStrings.Navigation.BACK,
                onActionClick = onBackClick
            )
        }
        return
    }

    val galleryImages = remember(product) {
        if (product.galleryUrls.isNotEmpty()) {
            product.galleryUrls
        } else {
            listOf(
                product.imageUrl,
                "https://picsum.photos/seed/${product.id}-g2/600/600",
                "https://picsum.photos/seed/${product.id}-g3/600/600"
            )
        }
    }

    val pagerState = rememberPagerState(pageCount = { galleryImages.size })

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_product_detail"),
        containerColor = BackgroundSlate,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            KYTopBar(
                title = product.name,
                subtitle = "SKU: ${product.sku}",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            // Alt Sabit Çubuk: "Düzenle" ve "Paylaş"
            Surface(
                color = SurfaceWhite,
                shadowElevation = 8.dp,
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KYButton(
                            text = TrStrings.ProductDetail.BTN_EDIT,
                            onClick = { onEditProductClick(product.id) },
                            variant = ButtonVariant.SECONDARY,
                            leadingIcon = Icons.Outlined.Edit,
                            modifier = Modifier.weight(1f),
                            tag = "btn_product_detail_edit"
                        )
                        KYButton(
                            text = TrStrings.ProductDetail.BTN_SHARE,
                            onClick = { showShareSheet = true },
                            variant = ButtonVariant.PRIMARY,
                            leadingIcon = Icons.Outlined.Share,
                            modifier = Modifier.weight(1f),
                            tag = "btn_product_detail_share"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1) KAYDIRILABİLİR GÖRSEL GALERİSİ (Sayfa Göstergeli)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.08f)
                        .background(SurfaceMuted)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("product_image_pager")
                    ) { page ->
                        AsyncImage(
                            model = galleryImages[page],
                            contentDescription = "${product.name} - Görsel ${page + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Sağ Üst Sayaç Rozeti ("1 / 4")
                    Surface(
                        shape = BadgeRoundedPill,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${galleryImages.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SurfaceWhite,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Alt Orta Nokta Sayfa Göstergeleri
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .clip(BadgeRoundedPill)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(galleryImages.size) { idx ->
                            val isSelected = pagerState.currentPage == idx
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 9.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) SurfaceWhite else SurfaceWhite.copy(alpha = 0.5f)
                                    )
                            )
                        }
                    }
                }
            }

            // 2) TEMEL BİLGİLER: Ad, Fiyat (İndirimli), Stok Rozeti
            item {
                KYCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KYBadge(
                            text = product.subCategory,
                            variant = BadgeVariant.PRIMARY
                        )
                        ProductStockBadge(status = product.stockStatus)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${product.price.toInt()} ${product.currency}",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryBlue
                        )
                        val orig = product.originalPrice
                        if (orig != null && orig > product.price) {
                            Text(
                                text = "${orig.toInt()} ${product.currency}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    textDecoration = TextDecoration.LineThrough
                                ),
                                color = TextMuted
                            )
                            val discountPct = (((orig - product.price) / orig) * 100).toInt()
                            if (discountPct > 0) {
                                KYBadge(
                                    text = "%$discountPct İndirim",
                                    variant = BadgeVariant.ACCENT
                                )
                            }
                        }
                    }
                }
            }

            // 3) VARYANTLAR (Beden, Renk vb.)
            if (product.variants.isNotEmpty()) {
                item {
                    KYCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = TrStrings.ProductDetail.VARIANTS_LABEL,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            product.variants.forEach { group ->
                                val details = if (group.optionDetails.isNotEmpty()) {
                                    group.optionDetails
                                } else {
                                    group.options.map { com.example.model.ProductVariantOption(it) }
                                }
                                val activeChoice = selectedVariants[group.name]
                                    ?: details.firstOrNull()?.label.orEmpty()

                                Column {
                                    Text(
                                        text = "${group.name}: $activeChoice",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        details.forEach { detail ->
                                            val isSelected = activeChoice == detail.label
                                            val chipText = buildString {
                                                append(detail.label)
                                                if (detail.extraPrice > 0) {
                                                    append(" (+${detail.extraPrice.toInt()}${product.currency})")
                                                }
                                            }
                                            Surface(
                                                onClick = { selectedVariants[group.name] = detail.label },
                                                shape = ButtonRoundedXl,
                                                color = if (isSelected) PrimaryBlueSubtle else SurfaceWhite,
                                                border = BorderStroke(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) PrimaryBlue else BorderLight
                                                )
                                            ) {
                                                Text(
                                                    text = chipText,
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
            }

            // 4) SKU, KATEGORİ VE STOK DETAY KARTI
            item {
                KYCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ProductMetaRow(
                        icon = Icons.Outlined.QrCode,
                        label = TrStrings.ProductDetail.SKU_LABEL,
                        value = product.sku
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = SurfaceMuted)
                    Spacer(modifier = Modifier.height(10.dp))

                    ProductMetaRow(
                        icon = Icons.Outlined.Category,
                        label = TrStrings.ProductDetail.CATEGORY_LABEL,
                        value = "${product.category} / ${product.subCategory}"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = SurfaceMuted)
                    Spacer(modifier = Modifier.height(10.dp))

                    ProductMetaRow(
                        icon = Icons.Outlined.Inventory2,
                        label = TrStrings.ProductDetail.STOCK_LABEL,
                        value = "${product.stockStatus.labelTr} (${product.stockCount} adet)"
                    )
                }
            }

            // 5) AÇIKLAMA
            item {
                KYCard(modifier = Modifier.padding(horizontal = 16.dp)) {
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

    // Ürün Kapsamlı Paylaşım Çekmecesi (Link, PDF Broşür, Görsel Canvas, Çoklu Katalog + Ayarlar)
    KYShareBottomSheet(
        isVisible = showShareSheet,
        catalog = null,
        product = product,
        onDismiss = { showShareSheet = false },
        onShowToast = onShowToast,
        onOpenPublicCatalog = onOpenPublicCatalog
    )

    // Ürünü Düzenle Modalı
    AddOrEditProductModal(
        isVisible = showEditModal,
        catalogId = product.catalogId,
        currencySymbol = product.currency,
        editingProduct = product,
        onDismiss = { showEditModal = false },
        onSaveProduct = { updated ->
            scope.launch {
                dataService.saveProduct(updated)
                showEditModal = false
                onShowToast(
                    KYToastData(
                        message = TrStrings.CatalogDetail.TOAST_PRODUCT_SAVED,
                        type = KYToastType.SUCCESS
                    )
                )
            }
        }
    )
}

@Composable
private fun ProductMetaRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )
    }
}
