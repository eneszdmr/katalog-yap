package com.example.ui.screens.catalogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.i18n.TrStrings
import com.example.model.AppNotification
import com.example.model.Catalog
import com.example.model.Product
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYBottomSheet
import com.example.ui.components.KYButton
import com.example.ui.components.KYInput
import com.example.ui.components.KYModal
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentAmberLight
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import com.example.services.AiPhotoProductDraft
import com.example.services.GeminiProductService
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.util.ImageCompressionUtil
import kotlinx.coroutines.launch

/**
 * Sağ Alt (+) FAB Butonuna Tıklanınca Açılan Bottom Sheet:
 * 1) "Boş katalog oluştur"
 * 2) "Fotoğraflardan katalog oluştur"
 * 3) "Excel/CSV'den içe aktar"
 * 4) "Farklı kataloglardan özel katalog oluştur"
 */
@Composable
fun CreateCatalogOptionsSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSelectCreateEmpty: () -> Unit,
    onSelectCreateFromPhotos: () -> Unit,
    onSelectImportCsv: () -> Unit,
    onSelectCreateCustomFromProducts: () -> Unit = {}
) {
    KYBottomSheet(
        isVisible = isVisible,
        title = TrStrings.Catalogs.FAB_SHEET_TITLE,
        subtitle = TrStrings.Catalogs.FAB_SHEET_SUBTITLE,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CreationOptionRow(
                icon = Icons.Outlined.NoteAdd,
                iconBgColor = PrimaryBlueSubtle,
                iconTint = PrimaryBlue,
                title = TrStrings.Catalogs.CREATE_EMPTY_TITLE,
                description = TrStrings.Catalogs.CREATE_EMPTY_DESC,
                onClick = {
                    onDismiss()
                    onSelectCreateEmpty()
                },
                tag = "sheet_opt_create_empty"
            )

            CreationOptionRow(
                icon = Icons.Outlined.CollectionsBookmark,
                iconBgColor = PrimaryBlueSubtle,
                iconTint = PrimaryBlue,
                title = TrStrings.Catalogs.CREATE_CUSTOM_FROM_PRODUCTS_TITLE,
                description = TrStrings.Catalogs.CREATE_CUSTOM_FROM_PRODUCTS_DESC,
                onClick = {
                    onDismiss()
                    onSelectCreateCustomFromProducts()
                },
                tag = "sheet_opt_create_custom_from_products"
            )

            CreationOptionRow(
                icon = Icons.Outlined.PhotoLibrary,
                iconBgColor = AccentAmberLight,
                iconTint = AccentAmber,
                title = TrStrings.Catalogs.CREATE_FROM_PHOTOS_TITLE,
                description = TrStrings.Catalogs.CREATE_FROM_PHOTOS_DESC,
                onClick = {
                    onDismiss()
                    onSelectCreateFromPhotos()
                },
                tag = "sheet_opt_create_photos"
            )

            CreationOptionRow(
                icon = Icons.Outlined.TableChart,
                iconBgColor = SuccessGreenLight,
                iconTint = SuccessGreen,
                title = TrStrings.Catalogs.IMPORT_EXCEL_CSV_TITLE,
                description = TrStrings.Catalogs.IMPORT_EXCEL_CSV_DESC,
                onClick = {
                    onDismiss()
                    onSelectImportCsv()
                },
                tag = "sheet_opt_import_csv"
            )
        }
    }
}

@Composable
private fun CreationOptionRow(
    icon: ImageVector,
    iconBgColor: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    description: String,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        onClick = onClick,
        shape = ButtonRoundedXl,
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderLight),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(ButtonRoundedXl)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/**
 * Boş Katalog Oluşturma veya Mevcut Kataloğu Düzenleme Modalı.
 */
@Composable
fun CreateOrEditCatalogModal(
    isVisible: Boolean,
    editingCatalog: Catalog?,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, tag: String) -> Unit
) {
    if (!isVisible) return

    var title by remember(editingCatalog) { mutableStateOf(editingCatalog?.title.orEmpty()) }
    var description by remember(editingCatalog) { mutableStateOf(editingCatalog?.description.orEmpty()) }
    var tag by remember(editingCatalog) {
        mutableStateOf(editingCatalog?.tags?.firstOrNull().orEmpty().ifBlank { "Yeni Sezon" })
    }
    var titleError by remember { mutableStateOf<String?>(null) }

    KYModal(
        isVisible = true,
        title = if (editingCatalog == null) {
            TrStrings.Catalogs.MODAL_CREATE_TITLE
        } else {
            TrStrings.Catalogs.MODAL_EDIT_TITLE
        },
        confirmText = TrStrings.Common.SAVE,
        cancelText = TrStrings.Common.CANCEL,
        onConfirm = {
            if (title.isBlank()) {
                titleError = "Lütfen katalog adını girin"
            } else {
                onSave(title.trim(), description.trim(), tag.trim())
            }
        },
        onDismiss = onDismiss
    ) {
        KYInput(
            value = title,
            onValueChange = {
                title = it
                if (titleError != null) titleError = null
            },
            label = TrStrings.Catalogs.INPUT_CATALOG_NAME_LABEL,
            placeholder = TrStrings.Catalogs.INPUT_CATALOG_NAME_PLACEHOLDER,
            errorText = titleError,
            tag = "modal_input_catalog_title"
        )
        Spacer(modifier = Modifier.height(12.dp))
        KYInput(
            value = description,
            onValueChange = { description = it },
            label = TrStrings.Catalogs.INPUT_CATALOG_DESC_LABEL,
            placeholder = TrStrings.Catalogs.INPUT_CATALOG_DESC_PLACEHOLDER,
            leadingIcon = Icons.Outlined.Description,
            tag = "modal_input_catalog_desc"
        )
        Spacer(modifier = Modifier.height(12.dp))
        KYInput(
            value = tag,
            onValueChange = { tag = it },
            label = TrStrings.Catalogs.INPUT_CATALOG_TAG_LABEL,
            placeholder = TrStrings.Catalogs.INPUT_CATALOG_TAG_PLACEHOLDER,
            leadingIcon = Icons.Outlined.Label,
            tag = "modal_input_catalog_tag"
        )
    }
}

/**
 * Tekli veya Çoklu Katalog İçin Etiket Ekleme Modalı:
 * - Mevcut etiketleri seçilebilir çip olarak gösterir
 * - Yeni etiket yazıp eklemeyi destekler
 * - "Uygula" ile kaydeder ve ana ekrandaki etiket filtresi bununla çalışır.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTagModal(
    isVisible: Boolean,
    existingTags: List<String> = emptyList(),
    initialSelectedTags: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onApplyTags: (List<String>) -> Unit = {},
    onConfirmTag: (String) -> Unit = {}
) {
    if (!isVisible) return

    val defaultPresets = remember {
        listOf("Yeni Sezon", "İndirimli", "Çok Satanlar", "Toptan", "Özel Koleksiyon", "Fırsat")
    }
    val allAvailableChips = remember(existingTags, initialSelectedTags) {
        mutableStateListOf<String>().apply {
            val combined = (existingTags + initialSelectedTags + defaultPresets)
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.equals(TrStrings.Catalogs.TAG_ALL, ignoreCase = true) }
                .distinct()
            addAll(combined)
        }
    }

    val selectedTags = remember(initialSelectedTags) {
        mutableStateListOf<String>().apply {
            val init = initialSelectedTags.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            if (init.isNotEmpty()) {
                addAll(init)
            } else {
                add("Yeni Sezon")
            }
        }
    }

    var newTagInput by remember { mutableStateOf("") }

    KYModal(
        isVisible = true,
        title = TrStrings.Catalogs.MODAL_TAG_TITLE,
        description = TrStrings.Catalogs.MODAL_TAG_DESC,
        confirmText = TrStrings.Catalogs.BTN_APPLY_TAGS,
        onConfirm = {
            val pendingTyped = newTagInput.trim()
            val finalTags = buildList {
                addAll(selectedTags)
                if (pendingTyped.isNotBlank() && none { it.equals(pendingTyped, ignoreCase = true) }) {
                    add(pendingTyped)
                }
            }.distinct()

            if (finalTags.isNotEmpty()) {
                onApplyTags(finalTags)
                onConfirmTag(finalTags.first())
            }
        },
        onDismiss = onDismiss
    ) {
        Text(
            text = TrStrings.Catalogs.EXISTING_TAGS_LABEL,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("modal_existing_tags_chips")
        ) {
            allAvailableChips.forEach { chipTag ->
                val isSelected = selectedTags.any { it.equals(chipTag, ignoreCase = true) }
                Surface(
                    onClick = {
                        val existingMatch = selectedTags.firstOrNull { it.equals(chipTag, ignoreCase = true) }
                        if (existingMatch != null) {
                            if (selectedTags.size > 1) {
                                selectedTags.remove(existingMatch)
                            }
                        } else {
                            selectedTags.add(chipTag)
                        }
                    },
                    shape = CircleShape,
                    color = if (isSelected) PrimaryBlue else SurfaceMuted,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) PrimaryBlue else BorderLight
                    ),
                    modifier = Modifier.testTag("tag_modal_chip_$chipTag")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = SurfaceWhite,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = chipTag,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) SurfaceWhite else TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KYInput(
                value = newTagInput,
                onValueChange = { newTagInput = it },
                label = TrStrings.Catalogs.INPUT_TAG_LABEL,
                placeholder = TrStrings.Catalogs.INPUT_TAG_PLACEHOLDER,
                leadingIcon = Icons.Outlined.Label,
                modifier = Modifier.weight(1f),
                tag = "modal_input_tag_name"
            )
            KYButton(
                text = "Ekle",
                onClick = {
                    val clean = newTagInput.trim()
                    if (clean.isNotBlank()) {
                        if (allAvailableChips.none { it.equals(clean, ignoreCase = true) }) {
                            allAvailableChips.add(clean)
                        }
                        if (selectedTags.none { it.equals(clean, ignoreCase = true) }) {
                            selectedTags.add(clean)
                        }
                        newTagInput = ""
                    }
                },
                variant = ButtonVariant.SECONDARY,
                leadingIcon = Icons.Outlined.Add,
                tag = "btn_add_custom_tag_chip"
            )
        }
    }
}

/**
 * Çoklu Seçimde Katalogları Birleştirme Modalı.
 */
@Composable
fun MergeCatalogsModal(
    isVisible: Boolean,
    selectedCount: Int,
    defaultMergedTitle: String,
    onDismiss: () -> Unit,
    onConfirmMerge: (String) -> Unit
) {
    if (!isVisible) return

    var mergedTitle by remember(defaultMergedTitle) { mutableStateOf(defaultMergedTitle) }

    KYModal(
        isVisible = true,
        title = "${TrStrings.Catalogs.MODAL_MERGE_TITLE} ($selectedCount)",
        description = TrStrings.Catalogs.MODAL_MERGE_DESC,
        confirmText = TrStrings.Catalogs.ACTION_MERGE,
        onConfirm = {
            onConfirmMerge(mergedTitle.trim())
        },
        onDismiss = onDismiss
    ) {
        KYInput(
            value = mergedTitle,
            onValueChange = { mergedTitle = it },
            label = TrStrings.Catalogs.INPUT_MERGE_NAME_LABEL,
            placeholder = TrStrings.Catalogs.INPUT_MERGE_NAME_PLACEHOLDER,
            tag = "modal_input_merge_title"
        )
    }
}

/**
* 1) "Fotoğraflardan katalog oluştur" Modalı (Gemini AI Entegrasyonlu):
* - Kullanıcı birden fazla fotoğraf seçer (veya örnek ürün fotoğrafları ekler).
* - Her fotoğraf için Gemini API ile ürün adı, açıklama, kategori ve tahmini etiketler üretilir.
* - Yükleme durumu (progress), hata yönetimi ve "Tekrar Dene" butonu bulunur.
* - Sonuçlar düzenlenebilir bir listede gösterilir; onaylanınca katalog ve ürünler oluşturulur.
*/
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateFromPhotosModal(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onCreateFromPhotos: (title: String, photoUrls: List<String>) -> Unit,
    onCreateFromAiDrafts: (title: String, drafts: List<AiPhotoProductDraft>) -> Unit = { title, drafts ->
        onCreateFromPhotos(title, drafts.map { it.imageUri })
    }
) {
    if (!isVisible) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("Yeni Sezon Fotoğraf Kataloğu") }
    val selectedUris = remember {
        mutableStateListOf(
            "https://picsum.photos/seed/katalog-ai-1/700/700",
            "https://picsum.photos/seed/katalog-ai-2/700/700",
            "https://picsum.photos/seed/katalog-ai-3/700/700"
        )
    }

    var isAnalyzing by remember { mutableStateOf(false) }
    var progressCompleted by remember { mutableIntStateOf(0) }
    var progressTotal by remember { mutableIntStateOf(0) }
    var aiErrorMessage by remember { mutableStateOf<String?>(null) }
    val generatedDrafts = remember { mutableStateListOf<AiPhotoProductDraft>() }

    val multiplePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val compressed = uris.map { uri ->
                    ImageCompressionUtil.compressUriToMax1280(context, uri) ?: uri.toString()
                }
                selectedUris.clear()
                selectedUris.addAll(compressed)
                generatedDrafts.clear()
                aiErrorMessage = null
            }
        }
    }

    fun analyzeSelectedPhotosWithGemini() {
        if (selectedUris.isEmpty() || isAnalyzing) return
        scope.launch {
            isAnalyzing = true
            aiErrorMessage = null
            progressCompleted = 0
            progressTotal = selectedUris.size

            val result = GeminiProductService.generateCatalogDraftsFromPhotos(
                context = context,
                photoUris = selectedUris.toList(),
                onProgress = { done, total ->
                    progressCompleted = done
                    progressTotal = total
                }
            )
            isAnalyzing = false

            result.onSuccess { drafts ->
                generatedDrafts.clear()
                generatedDrafts.addAll(drafts)
            }.onFailure { err ->
                aiErrorMessage = err.localizedMessage ?: "Fotoğraflar analiz edilirken bir hata oluştu."
            }
        }
    }

    KYModal(
        isVisible = true,
        title = TrStrings.Catalogs.MODAL_PHOTOS_TITLE,
        description = "Fotoğrafları seçin, Gemini AI ile ürün adı, açıklama, kategori ve etiketleri otomatik üretip düzenleyerek kataloğu oluşturun.",
        confirmText = if (generatedDrafts.isNotEmpty()) {
            "Onayla ve Kataloğu Oluştur (${generatedDrafts.size})"
        } else {
            TrStrings.Common.CREATE
        },
        onConfirm = {
            if (generatedDrafts.isNotEmpty()) {
                onCreateFromAiDrafts(title.trim().ifBlank { "Fotoğraf Kataloğu" }, generatedDrafts.toList())
            } else {
                onCreateFromPhotos(title.trim().ifBlank { "Fotoğraf Kataloğu" }, selectedUris.toList())
            }
        },
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KYInput(
                value = title,
                onValueChange = { title = it },
                label = TrStrings.Catalogs.INPUT_CATALOG_NAME_LABEL,
                placeholder = "Örn: Lookbook Çekimi",
                tag = "modal_photos_catalog_name"
            )

            // Fotoğraf Seçim Butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KYButton(
                    text = if (selectedUris.isEmpty()) {
                        TrStrings.Catalogs.PICK_PHOTOS_BTN
                    } else {
                        "Galeriden Seç (${selectedUris.size})"
                    },
                    onClick = {
                        multiplePhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    variant = ButtonVariant.SECONDARY,
                    leadingIcon = Icons.Outlined.AddPhotoAlternate,
                    modifier = Modifier.weight(1f),
                    tag = "btn_pick_multiple_photos_for_catalog"
                )

                Surface(
                    onClick = {
                        val seed = System.currentTimeMillis() % 1000
                        selectedUris.add("https://picsum.photos/seed/photo-$seed/700/700")
                        aiErrorMessage = null
                    },
                    shape = ButtonRoundedXl,
                    color = SurfaceMuted,
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.testTag("btn_add_sample_photo_for_catalog")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Örnek Ekle",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = PrimaryBlue
                        )
                    }
                }
            }

            // Seçilen Fotoğrafların Önizleme Şeridi
            if (selectedUris.isNotEmpty() && generatedDrafts.isEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    selectedUris.forEachIndexed { idx, uri ->
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(ButtonRoundedXl)
                                .background(SurfaceMuted)
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Seçilen Fotoğraf ${idx + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                            Surface(
                                onClick = {
                                    if (selectedUris.size > 1) {
                                        selectedUris.removeAt(idx)
                                    }
                                },
                                shape = CircleShape,
                                color = TextPrimary.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp)
                                    .size(18.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "×",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SurfaceWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ✨ Gemini ile Analiz Et Butonu
            KYButton(
                text = if (isAnalyzing) {
                    "Fotoğraflar Analiz Ediliyor ($progressCompleted/$progressTotal)..."
                } else if (generatedDrafts.isNotEmpty()) {
                    "✨ Fotoğrafları Gemini ile Yeniden Analiz Et"
                } else {
                    "✨ Gemini ile Ürün Bilgilerini Üret (${selectedUris.size} Fotoğraf)"
                },
                onClick = { analyzeSelectedPhotosWithGemini() },
                variant = if (generatedDrafts.isEmpty()) ButtonVariant.PRIMARY else ButtonVariant.SECONDARY,
                leadingIcon = Icons.Outlined.AutoAwesome,
                enabled = selectedUris.isNotEmpty() && !isAnalyzing,
                modifier = Modifier.fillMaxWidth(),
                tag = "btn_analyze_photos_gemini"
            )

            // Yükleme Durumu (Loading State)
            if (isAnalyzing) {
                Surface(
                    shape = ButtonRoundedXl,
                    color = PrimaryBlueSubtle,
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("photos_ai_loading_state")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = PrimaryBlue,
                            strokeWidth = 2.5.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gemini AI fotoğrafları inceliyor...",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue
                            )
                            Text(
                                text = "${progressCompleted}/${progressTotal} fotoğraf için ürün adı, açıklama, kategori ve etiketler üretiliyor.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Hata Yönetimi ve Tekrar Deneme Butonu (Error State + Retry)
            if (aiErrorMessage != null) {
                Surface(
                    shape = ButtonRoundedXl,
                    color = DangerRedLight,
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("photos_ai_error_banner")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = aiErrorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = DangerRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        KYButton(
                            text = "Tekrar Dene",
                            onClick = { analyzeSelectedPhotosWithGemini() },
                            variant = ButtonVariant.SECONDARY,
                            leadingIcon = Icons.Outlined.Refresh,
                            modifier = Modifier.fillMaxWidth(),
                            tag = "btn_retry_photos_gemini"
                        )
                    }
                }
            }

            // Düzenlenebilir Ürün Taslakları Listesi
            if (generatedDrafts.isNotEmpty()) {
                HorizontalDivider(color = BorderLight)
                Text(
                    text = "Üretilen Ürünler (${generatedDrafts.size}) — Düzenleyip Onaylayın",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 290.dp)
                        .testTag("photos_ai_editable_drafts_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(generatedDrafts, key = { it.id }) { draft ->
                        val index = generatedDrafts.indexOfFirst { it.id == draft.id }
                        if (index >= 0) {
                            EditableAiDraftCard(
                                draft = draft,
                                index = index,
                                onUpdate = { updated ->
                                    generatedDrafts[index] = updated
                                },
                                onRemove = {
                                    if (generatedDrafts.size > 1) {
                                        generatedDrafts.removeAt(index)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableAiDraftCard(
    draft: AiPhotoProductDraft,
    index: Int,
    onUpdate: (AiPhotoProductDraft) -> Unit,
    onRemove: () -> Unit
) {
    var tagsText by remember(draft.id) { mutableStateOf(draft.tags.joinToString(", ")) }
    var priceText by remember(draft.id) { mutableStateOf(draft.price.toInt().toString()) }

    Surface(
        shape = ButtonRoundedXl,
        color = SurfaceMuted.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, BorderLight),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_draft_card_$index")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = draft.imageUri,
                    contentDescription = draft.productName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(ButtonRoundedXl)
                        .background(PrimaryBlueSubtle)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fotoğraf #${index + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryBlue
                    )
                    Text(
                        text = "Kategori: ${draft.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Surface(
                    onClick = onRemove,
                    shape = CircleShape,
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Kaldır",
                        tint = DangerRed,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp)
                    )
                }
            }

            KYInput(
                value = draft.productName,
                onValueChange = { onUpdate(draft.copy(productName = it)) },
                label = "Ürün Adı",
                placeholder = "Ürün adı",
                tag = "ai_draft_name_$index"
            )

            KYInput(
                value = draft.description,
                onValueChange = { onUpdate(draft.copy(description = it)) },
                label = "Açıklama",
                placeholder = "Ürün açıklaması",
                singleLine = false,
                maxLines = 3,
                tag = "ai_draft_desc_$index"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KYInput(
                    value = draft.category,
                    onValueChange = { onUpdate(draft.copy(category = it)) },
                    label = "Kategori",
                    placeholder = "Kategori",
                    modifier = Modifier.weight(1f),
                    tag = "ai_draft_category_$index"
                )
                KYInput(
                    value = priceText,
                    onValueChange = { newVal ->
                        priceText = newVal
                        val parsed = newVal.replace(",", ".").toDoubleOrNull()
                        if (parsed != null && parsed > 0) {
                            onUpdate(draft.copy(price = parsed))
                        }
                    },
                    label = "Fiyat (₺)",
                    placeholder = "750",
                    modifier = Modifier.weight(1f),
                    tag = "ai_draft_price_$index"
                )
            }

            KYInput(
                value = tagsText,
                onValueChange = { newTags ->
                    tagsText = newTags
                    val splitTags = newTags.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }
                    onUpdate(draft.copy(tags = splitTags))
                },
                label = "Tahmini Etiketler (virgülle ayırın)",
                placeholder = "Yeni Sezon, Doğal Doku, Özel Tasarım",
                tag = "ai_draft_tags_$index"
            )
        }
    }
}

/**
 * Excel / CSV'den Katalog İçe Aktarma Modalı.
 */
@Composable
fun ImportFromCsvModal(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onImportCsv: (title: String, csvContent: String) -> Unit
) {
    if (!isVisible) return

    var title by remember { mutableStateOf("Toptan Fiyat Listesi") }
    var csvText by remember {
        mutableStateOf(
            """
            Kaşmir Karışımlı Kazak; 1250; KSM-101
            Klasik Kesim Kumaş Pantolon; 980; PNT-102
            Hakiki Deri Kemer; 490; KMR-103
            Pamuklu Oxford Gömlek; 840; GML-104
            Su Geçirmez Trençkot; 2390; TRN-105
            Yün Atkı & Bere Takımı; 620; ATK-106
            Günlük Süet Loafer; 1650; LFR-107
            Kanvas Hafta Sonu Çantası; 1190; CNT-108
            """.trimIndent()
        )
    }

    KYModal(
        isVisible = true,
        title = TrStrings.Catalogs.MODAL_CSV_TITLE,
        description = TrStrings.Catalogs.MODAL_CSV_DESC,
        confirmText = TrStrings.Common.CREATE,
        onConfirm = {
            onImportCsv(title.trim(), csvText)
        },
        onDismiss = onDismiss
    ) {
        KYInput(
            value = title,
            onValueChange = { title = it },
            label = TrStrings.Catalogs.INPUT_CATALOG_NAME_LABEL,
            placeholder = "Örn: Kış Sezonu Excel Listesi",
            tag = "modal_csv_catalog_name"
        )
        Spacer(modifier = Modifier.height(10.dp))
        KYInput(
            value = csvText,
            onValueChange = { csvText = it },
            label = TrStrings.Catalogs.INPUT_CSV_LABEL,
            singleLine = false,
            showClearButton = false,
            tag = "modal_csv_content"
        )
    }
}

/**
 * Katalog Paylaşım Alt Çekmecesi (Link Kopyala, WhatsApp, Sistem Paylaşımı).
 */
@Composable
fun ShareCatalogBottomSheet(
    catalog: Catalog?,
    storeSlug: String,
    onDismiss: () -> Unit,
    onLinkCopied: (String) -> Unit
) {
    val context = LocalContext.current
    if (catalog == null) return

    val shareUrl = "https://katalogyap.app/$storeSlug/${catalog.slug}"
    val shareMessage = "${catalog.title} dijital ürün kataloğumuza göz atın: $shareUrl"

    KYBottomSheet(
        isVisible = true,
        title = TrStrings.Catalogs.SHARE_SHEET_TITLE,
        subtitle = catalog.title,
        onDismiss = onDismiss
    ) {
        Surface(
            shape = ButtonRoundedXl,
            color = SurfaceMuted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = shareUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        KYButton(
            text = TrStrings.Catalogs.SHARE_COPY_LINK,
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Katalog Linki", shareUrl))
                onDismiss()
                onLinkCopied(shareUrl)
            },
            variant = ButtonVariant.PRIMARY,
            leadingIcon = Icons.Outlined.ContentCopy,
            modifier = Modifier.fillMaxWidth(),
            tag = "btn_copy_catalog_link"
        )

        Spacer(modifier = Modifier.height(10.dp))

        KYButton(
            text = TrStrings.Catalogs.SHARE_SYSTEM,
            onClick = {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, catalog.title)
                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                }
                context.startActivity(Intent.createChooser(sendIntent, catalog.title))
                onDismiss()
            },
            variant = ButtonVariant.SECONDARY,
            leadingIcon = Icons.Outlined.Share,
            modifier = Modifier.fillMaxWidth(),
            tag = "btn_system_share_catalog"
        )
    }
}

/**
 * Üst Çubuktaki Bildirim İkonuna Tıklanınca Açılan Bildirimler Alt Çekmecesi.
 */
@Composable
fun NotificationsBottomSheet(
    isVisible: Boolean,
    notifications: List<AppNotification>,
    onMarkAllRead: () -> Unit,
    onDismiss: () -> Unit
) {
    KYBottomSheet(
        isVisible = isVisible,
        title = TrStrings.Catalogs.NOTIFICATIONS_TITLE,
        subtitle = TrStrings.Catalogs.NOTIFICATIONS_SUBTITLE,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            notifications.forEach { notif ->
                Surface(
                    shape = ButtonRoundedXl,
                    color = if (notif.isUnread) PrimaryBlueSubtle else SurfaceWhite,
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = notif.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = notif.timeAgo,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = notif.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            if (notifications.any { it.isUnread }) {
                Spacer(modifier = Modifier.height(4.dp))
                KYButton(
                    text = TrStrings.Catalogs.NOTIFICATIONS_MARK_READ,
                    onClick = {
                        onMarkAllRead()
                        onDismiss()
                    },
                    variant = ButtonVariant.GHOST,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Katalog Kartına Tıklanınca Ürünlerini Hızlı Önizleyen Alt Çekmece.
 */
@Composable
fun CatalogProductsPreviewSheet(
    catalog: Catalog?,
    products: List<Product>,
    onShareCatalog: (Catalog) -> Unit,
    onEditCatalog: (Catalog) -> Unit,
    onDismiss: () -> Unit
) {
    if (catalog == null) return

    KYBottomSheet(
        isVisible = true,
        title = catalog.title,
        subtitle = "${products.size} ${TrStrings.Catalogs.PRODUCT_COUNT_SUFFIX} • ${catalog.viewCount} görüntülenme • ${catalog.orderCount} sipariş",
        onDismiss = onDismiss
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(products, key = { it.id }) { product ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ButtonRoundedXl)
                        .background(SurfaceMuted.copy(alpha = 0.55f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(ButtonRoundedXl)
                            .background(PrimaryBlueSubtle)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "SKU: ${product.sku}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${product.price.toInt()} ${product.currency}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = BorderLight)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KYButton(
                text = TrStrings.Catalogs.MENU_EDIT,
                onClick = {
                    onDismiss()
                    onEditCatalog(catalog)
                },
                variant = ButtonVariant.SECONDARY,
                modifier = Modifier.weight(1f)
            )
            KYButton(
                text = TrStrings.Common.SHARE,
                onClick = {
                    onDismiss()
                    onShareCatalog(catalog)
                },
                variant = ButtonVariant.PRIMARY,
                leadingIcon = Icons.Outlined.Share,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
