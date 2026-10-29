package com.example.ui.screens.catalogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.i18n.TrStrings
import com.example.model.ProductVariantGroup
import com.example.model.ProductVariantOption
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYButton
import com.example.ui.components.KYCard
import com.example.ui.components.KYSkeletonBox
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentAmberLight
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.CardRounded2xl
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmberDark

/**
 * 1) ÇOKLU GÖRSEL YÜKLEME (Max 10, Sürükle/Sırala, İlki Kapak, Kamera & Galeri)
 *    ve "✨ Yapay zeka ile doldur" (Gemini API) Tetikleyici Kartı
 */
@Composable
fun ProductImagesUploaderSection(
    images: List<String>,
    isCompressing: Boolean,
    isAiLoading: Boolean,
    onPickGallery: () -> Unit,
    onOpenCamera: () -> Unit,
    onAddDemoImage: () -> Unit,
    onMoveImage: (fromIndex: Int, toIndex: Int) -> Unit,
    onMakeCover: (index: Int) -> Unit,
    onRemoveImage: (index: Int) -> Unit,
    onAiFillClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    KYCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = TrStrings.ProductForm.SECTION_IMAGES,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            KYBadge(
                text = "${images.size} / 10",
                variant = if (images.size >= 10) BadgeVariant.ACCENT else BadgeVariant.PRIMARY
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = TrStrings.ProductForm.IMAGES_SUBTITLE,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Kamera, Galeri ve Hızlı Örnek Görsel Butonları
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = onOpenCamera,
                enabled = images.size < 10 && !isCompressing,
                shape = ButtonRoundedXl,
                color = SurfaceMuted,
                border = BorderStroke(1.dp, BorderLight),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_upload_camera")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = TrStrings.ProductForm.BTN_ADD_CAMERA,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = TrStrings.ProductForm.BTN_ADD_CAMERA,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
            }

            Surface(
                onClick = onPickGallery,
                enabled = images.size < 10 && !isCompressing,
                shape = ButtonRoundedXl,
                color = PrimaryBlueSubtle,
                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.35f)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_upload_gallery")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = TrStrings.ProductForm.BTN_ADD_GALLERY,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = TrStrings.ProductForm.BTN_ADD_GALLERY,
                        style = MaterialTheme.typography.labelLarge,
                        color = PrimaryBlue
                    )
                }
            }

            Surface(
                onClick = onAddDemoImage,
                enabled = images.size < 10 && !isCompressing,
                shape = ButtonRoundedXl,
                color = SurfaceWhite,
                border = BorderStroke(1.dp, BorderLight),
                modifier = Modifier
                    .weight(1.1f)
                    .testTag("btn_upload_demo_photo")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = TrStrings.ProductForm.BTN_ADD_DEMO_PHOTO,
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = TrStrings.ProductForm.BTN_ADD_DEMO_PHOTO,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (isCompressing) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = PrimaryBlue
                )
                Text(
                    text = TrStrings.ProductForm.IMAGE_COMPRESSING,
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryBlue
                )
            }
        }

        // Görsel Listesi (Yatay Kaydırmalı & Sıralanabilir, İlki Kapak)
        if (images.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_form_images_row")
            ) {
                itemsIndexed(images, key = { idx, url -> "$idx-$url" }) { index, imageUrl ->
                    val isCover = index == 0
                    Box(
                        modifier = Modifier
                            .width(126.dp)
                            .height(148.dp)
                            .clip(ButtonRoundedXl)
                            .border(
                                width = if (isCover) 2.dp else 1.dp,
                                color = if (isCover) PrimaryBlue else BorderLight,
                                shape = ButtonRoundedXl
                            )
                            .background(SurfaceMuted)
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Ürün Görseli ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Üst: "Kapak" Rozeti veya "Kapak Yap" Butonu + Sil (X) Butonu
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCover) {
                                Surface(
                                    shape = BadgeRoundedPill,
                                    color = PrimaryBlue
                                ) {
                                    Text(
                                        text = TrStrings.ProductForm.COVER_BADGE,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SurfaceWhite,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    onClick = { onMakeCover(index) },
                                    shape = BadgeRoundedPill,
                                    color = Color.Black.copy(alpha = 0.62f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Star,
                                            contentDescription = "Kapak Yap",
                                            tint = AccentAmber,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "Kapak Yap",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SurfaceWhite
                                        )
                                    }
                                }
                            }

                            Surface(
                                onClick = { onRemoveImage(index) },
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.62f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Görseli Kaldır",
                                        tint = SurfaceWhite,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Alt: Sürükle/Sırala Ok Kontrolleri (< Sıra >)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 4.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (index > 0) onMoveImage(index, index - 1) },
                                enabled = index > 0,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                    contentDescription = "Sola Taşı",
                                    tint = if (index > 0) SurfaceWhite else SurfaceWhite.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = "#${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SurfaceWhite
                            )

                            IconButton(
                                onClick = { if (index < images.lastIndex) onMoveImage(index, index + 1) },
                                enabled = index < images.lastIndex,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = "Sağa Taşı",
                                    tint = if (index < images.lastIndex) SurfaceWhite else SurfaceWhite.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // GEMINI ENTEGRASYONU: Görsel yüklenince "✨ Yapay zeka ile doldur" butonu çıksın
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                onClick = { if (!isAiLoading) onAiFillClick() },
                shape = CardRounded2xl,
                color = AccentAmberLight,
                border = BorderStroke(1.5.dp, AccentAmber),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_ai_fill_product")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAiLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.2.dp,
                            color = WarningAmberDark
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = TrStrings.ProductForm.AI_LOADING_TITLE,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = WarningAmberDark
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = WarningAmberDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = TrStrings.ProductForm.BTN_AI_FILL,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = WarningAmberDark
                        )
                    }
                }
            }
        }
    }
}

/**
 * Gemini API yüklenirken form alanları üzerinde gösterilen Skeleton Loader kartı.
 */
@Composable
fun AiFormSkeletonCard(modifier: Modifier = Modifier) {
    KYCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_loading_skeleton_card")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = TrStrings.ProductForm.AI_LOADING_TITLE,
                    style = MaterialTheme.typography.titleSmall,
                    color = PrimaryBlue
                )
                Text(
                    text = TrStrings.ProductForm.AI_LOADING_SUBTITLE,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        KYSkeletonBox(modifier = Modifier.fillMaxWidth(0.55f), height = 20.dp)
        Spacer(modifier = Modifier.height(8.dp))
        KYSkeletonBox(modifier = Modifier.fillMaxWidth(), height = 46.dp)
        Spacer(modifier = Modifier.height(12.dp))
        KYSkeletonBox(modifier = Modifier.fillMaxWidth(0.4f), height = 20.dp)
        Spacer(modifier = Modifier.height(8.dp))
        KYSkeletonBox(modifier = Modifier.fillMaxWidth(), height = 76.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KYSkeletonBox(modifier = Modifier.width(100.dp), height = 32.dp)
            KYSkeletonBox(modifier = Modifier.width(140.dp), height = 32.dp)
        }
    }
}

/**
 * 2) VARYANTLAR BÖLÜMÜ (Beden, Renk vb. özellik grupları + her varyant için ek fiyat ve stok)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductVariantsEditorSection(
    variantGroups: List<ProductVariantGroup>,
    currencySymbol: String,
    onUpdateGroups: (List<ProductVariantGroup>) -> Unit,
    modifier: Modifier = Modifier
) {
    var newGroupName by remember { mutableStateOf("") }
    var showNewGroupInput by remember { mutableStateOf(false) }

    KYCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = TrStrings.ProductForm.SECTION_VARIANTS,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = TrStrings.ProductForm.VARIANTS_SUBTITLE,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Hızlı Grup Ekleme Çipleri (Beden, Renk, Özel Grup)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = {
                    if (variantGroups.none { it.name.equals("Beden", ignoreCase = true) }) {
                        val sizeOpts = listOf(
                            ProductVariantOption("S", 0.0, 10),
                            ProductVariantOption("M", 0.0, 15),
                            ProductVariantOption("L", 0.0, 12),
                            ProductVariantOption("XL", 50.0, 8)
                        )
                        onUpdateGroups(
                            variantGroups + ProductVariantGroup(
                                name = "Beden",
                                options = sizeOpts.map { it.label },
                                optionDetails = sizeOpts
                            )
                        )
                    }
                },
                shape = BadgeRoundedPill,
                color = PrimaryBlueSubtle,
                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)),
                modifier = Modifier.testTag("btn_preset_size_group")
            ) {
                Text(
                    text = TrStrings.ProductForm.PRESET_SIZE,
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryBlue,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }

            Surface(
                onClick = {
                    if (variantGroups.none { it.name.equals("Renk", ignoreCase = true) }) {
                        val colorOpts = listOf(
                            ProductVariantOption("Siyah", 0.0, 15),
                            ProductVariantOption("Ekru", 0.0, 12),
                            ProductVariantOption("Zeytin Yeşili", 0.0, 8)
                        )
                        onUpdateGroups(
                            variantGroups + ProductVariantGroup(
                                name = "Renk",
                                options = colorOpts.map { it.label },
                                optionDetails = colorOpts
                            )
                        )
                    }
                },
                shape = BadgeRoundedPill,
                color = PrimaryBlueSubtle,
                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)),
                modifier = Modifier.testTag("btn_preset_color_group")
            ) {
                Text(
                    text = TrStrings.ProductForm.PRESET_COLOR,
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryBlue,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }

            Surface(
                onClick = { showNewGroupInput = !showNewGroupInput },
                shape = BadgeRoundedPill,
                color = SurfaceMuted,
                border = BorderStroke(1.dp, BorderLight),
                modifier = Modifier.testTag("btn_add_custom_variant_group")
            ) {
                Text(
                    text = "+ ${TrStrings.ProductForm.BTN_ADD_VARIANT_GROUP}",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }

        if (showNewGroupInput) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    placeholder = { Text(TrStrings.ProductForm.LABEL_GROUP_NAME) },
                    singleLine = true,
                    shape = ButtonRoundedXl,
                    modifier = Modifier.weight(1f)
                )
                KYButton(
                    text = "Ekle",
                    onClick = {
                        val clean = newGroupName.trim()
                        if (clean.isNotBlank()) {
                            onUpdateGroups(
                                variantGroups + ProductVariantGroup(
                                    name = clean,
                                    options = emptyList(),
                                    optionDetails = emptyList()
                                )
                            )
                            newGroupName = ""
                            showNewGroupInput = false
                        }
                    },
                    variant = ButtonVariant.PRIMARY
                )
            }
        }

        // Mevcut Varyant Grupları ve Seçenekleri (Ek Fiyat + Stok)
        if (variantGroups.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                variantGroups.forEachIndexed { groupIdx, group ->
                    VariantGroupCard(
                        group = group,
                        currencySymbol = currencySymbol,
                        onRemoveGroup = {
                            onUpdateGroups(variantGroups.filterIndexed { i, _ -> i != groupIdx })
                        },
                        onUpdateGroup = { updatedGroup ->
                            onUpdateGroups(
                                variantGroups.mapIndexed { i, existing ->
                                    if (i == groupIdx) updatedGroup else existing
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VariantGroupCard(
    group: ProductVariantGroup,
    currencySymbol: String,
    onRemoveGroup: () -> Unit,
    onUpdateGroup: (ProductVariantGroup) -> Unit
) {
    var optionLabelInput by remember { mutableStateOf("") }
    var extraPriceInput by remember { mutableStateOf("0") }
    var stockInput by remember { mutableStateOf("10") }

    val details = if (group.optionDetails.isNotEmpty()) {
        group.optionDetails
    } else {
        group.options.map { ProductVariantOption(label = it, extraPrice = 0.0, stock = 10) }
    }

    Surface(
        shape = ButtonRoundedXl,
        color = SurfaceMuted.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryBlue
                )
                IconButton(
                    onClick = onRemoveGroup,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Grubu Sil",
                        tint = DangerRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    details.forEachIndexed { optIdx, opt ->
                        Surface(
                            shape = ButtonRoundedXl,
                            color = SurfaceWhite,
                            border = BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = opt.label,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = buildString {
                                            append("Ek Fiyat: ")
                                            if (opt.extraPrice > 0) {
                                                append("+${opt.extraPrice.toInt()} $currencySymbol")
                                            } else {
                                                append("0 $currencySymbol")
                                            }
                                            append("  •  Stok: ${opt.stock} adet")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val updatedDetails = details.filterIndexed { i, _ -> i != optIdx }
                                        onUpdateGroup(
                                            group.copy(
                                                options = updatedDetails.map { it.label },
                                                optionDetails = updatedDetails
                                            )
                                        )
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Seçeneği Sil",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderLight)
            Spacer(modifier = Modifier.height(10.dp))

            // Yeni Varyant Seçeneği + Ek Fiyat + Stok Girişi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = optionLabelInput,
                    onValueChange = { optionLabelInput = it },
                    label = { Text("Seçenek") },
                    placeholder = { Text("Örn: XL") },
                    singleLine = true,
                    shape = ButtonRoundedXl,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite
                    ),
                    modifier = Modifier.weight(1.3f)
                )

                OutlinedTextField(
                    value = extraPriceInput,
                    onValueChange = { extraPriceInput = it },
                    label = { Text("+Fiyat") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = ButtonRoundedXl,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite
                    ),
                    modifier = Modifier.weight(0.95f)
                )

                OutlinedTextField(
                    value = stockInput,
                    onValueChange = { stockInput = it },
                    label = { Text("Stok") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = ButtonRoundedXl,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite
                    ),
                    modifier = Modifier.weight(0.85f)
                )

                Surface(
                    onClick = {
                        val cleanLabel = optionLabelInput.trim()
                        if (cleanLabel.isNotBlank()) {
                            val extra = extraPriceInput.replace(",", ".").toDoubleOrNull() ?: 0.0
                            val stk = stockInput.toIntOrNull()?.coerceAtLeast(0) ?: 10
                            val newOpt = ProductVariantOption(
                                label = cleanLabel,
                                extraPrice = extra,
                                stock = stk
                            )
                            val updatedDetails = details + newOpt
                            onUpdateGroup(
                                group.copy(
                                    options = updatedDetails.map { it.label },
                                    optionDetails = updatedDetails
                                )
                            )
                            optionLabelInput = ""
                            extraPriceInput = "0"
                        }
                    },
                    shape = ButtonRoundedXl,
                    color = PrimaryBlue,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = TrStrings.ProductForm.BTN_ADD_OPTION,
                            tint = SurfaceWhite
                        )
                    }
                }
            }
        }
    }
}
