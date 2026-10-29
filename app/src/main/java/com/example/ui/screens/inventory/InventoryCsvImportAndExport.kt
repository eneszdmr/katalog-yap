package com.example.ui.screens.inventory

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.i18n.TrStrings
import com.example.model.Catalog
import com.example.model.Product
import com.example.model.StockStatus
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYButton
import com.example.ui.components.KYCard
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.BackgroundSlate
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.util.UUID

/**
 * Mevcut ürünleri CSV olarak dışa aktaran ve paylaşan yardımcı sınıf.
 */
object InventoryCsvExporter {

    fun buildProductsCsvContent(
        products: List<Product>,
        catalogs: List<Catalog>
    ): String {
        val catalogNames = catalogs.associate { it.id to it.title }
        val sb = StringBuilder()
        // UTF-8 BOM (Excel'de Türkçe karakterlerin doğru açılması için)
        sb.append("\uFEFF")
        sb.appendLine("Ad;Fiyat;ParaBirimi;SKU;Stok;Durum;Kategori;AltKategori;Katalog;GorselURL")

        products.forEach { product ->
            val catTitle = catalogNames[product.catalogId] ?: "Genel Katalog"
            val statusLabel = when {
                product.trackStock && product.stockCount <= 0 -> StockStatus.OUT_OF_STOCK.labelTr
                else -> product.stockStatus.labelTr
            }
            val row = listOf(
                escapeCsvField(product.name),
                product.price.toInt().toString(),
                escapeCsvField(product.currency),
                escapeCsvField(product.sku),
                product.stockCount.toString(),
                escapeCsvField(statusLabel),
                escapeCsvField(product.category),
                escapeCsvField(product.subCategory),
                escapeCsvField(catTitle),
                escapeCsvField(product.imageUrl)
            ).joinToString(";")
            sb.appendLine(row)
        }
        return sb.toString()
    }

    private fun escapeCsvField(value: String): String {
        val clean = value.replace("\n", " ").replace("\r", " ").trim()
        return if (clean.contains(";") || clean.contains("\"") || clean.contains(",")) {
            "\"${clean.replace("\"", "\"\"")}\""
        } else {
            clean
        }
    }

    fun exportProductsToCsvFileAndShare(
        context: Context,
        products: List<Product>,
        catalogs: List<Catalog>
    ): Boolean {
        return runCatching {
            val csvText = buildProductsCsvContent(products, catalogs)

            // Panoya da kopyala (kullanıcı hızlıca yapıştırabilsin)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("katalog_yap_urunler.csv", csvText))

            val sharedDir = File(context.cacheDir, "shared_files").apply {
                if (!exists()) mkdirs()
            }
            val csvFile = File(sharedDir, "katalog_yap_urunler_stok.csv")
            csvFile.writeText(csvText, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "KATALOG YAP - Ürün ve Stok Listesi (CSV)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "CSV Dosyasını İndir / Paylaş").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
    }

    fun defaultSampleCsv(): String = """
Urun_Adi;Satis_Fiyati;Urun_Kodu_SKU;Stok_Adedi;Kategori_Adi;Gorsel_Linki
Kaşmir Dokulu Triko Kazak;1290;VR-KSM-501;24;Üst Giyim;https://picsum.photos/seed/csv-kasmir-1/600/600
Doğal Mermer Kahve Tepsisi;740;EV-MRM-502;4;Sofra & Mutfak;https://picsum.photos/seed/csv-mermer-2/600/600
Manyetik Kablosuz Araç Şarjı;1150;TK-MAG-503;18;Şarj & Güç;https://picsum.photos/seed/csv-magsafe-3/600/600
Biberiye Özlü Saç Toniği;420;KZ-BBR-504;0;Saç Bakımı;https://picsum.photos/seed/csv-biberiye-4/600/600
Keten Kruvaze Yazlık Ceket;1680;VR-KTN-505;3;Dış Giyim;https://picsum.photos/seed/csv-keten-5/600/600
El Yapımı Soya Mumu Seti;560;EV-SOY-506;32;Aydınlatma & Koku;https://picsum.photos/seed/csv-soya-6/600/600
    """.trimIndent()
}

/**
 * Excel / CSV'den Toplu Ürün İçe Aktarma Tam Ekran Modalı:
 * 1) Dosya yükleme (.csv/.txt) veya örnek/metin CSV girişi
 * 2) Sütun eşleştirme (ad, fiyat, SKU, stok, kategori, görsel URL)
 * 3) Önizleme tablosu
 * 4) İçe aktar butonu
 */
@Composable
fun BulkCsvImportModal(
    isVisible: Boolean,
    catalogs: List<Catalog>,
    currencySymbol: String,
    lowStockThreshold: Int,
    onDismiss: () -> Unit,
    onConfirmImport: (catalogId: String, products: List<Product>) -> Unit
) {
    if (!isVisible) return

    val context = LocalContext.current
    var rawCsvText by remember { mutableStateOf(InventoryCsvExporter.defaultSampleCsv()) }
    var uploadedFileName by remember { mutableStateOf("ornek_urun_listesi.csv") }
    var selectedCatalogId by remember(catalogs) {
        mutableStateOf(catalogs.firstOrNull()?.id.orEmpty())
    }

    // CSV satırlarını ve başlıklarını ayrıştır
    val parsedLines = remember(rawCsvText) {
        rawCsvText
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                val delimiter = when {
                    line.contains(";") -> ";"
                    line.contains("\t") -> "\t"
                    else -> ","
                }
                line.split(delimiter).map { cell -> cell.trim().removeSurrounding("\"") }
            }
    }

    val headers = remember(parsedLines) {
        parsedLines.firstOrNull() ?: listOf("Sütun 1", "Sütun 2", "Sütun 3", "Sütun 4", "Sütun 5", "Sütun 6")
    }
    val dataRows = remember(parsedLines) {
        if (parsedLines.size > 1) parsedLines.drop(1) else emptyList()
    }

    // 6 Alan için sütun indeksleri (ad, fiyat, SKU, stok, kategori, görsel URL)
    var nameColIdx by remember { mutableIntStateOf(0) }
    var priceColIdx by remember { mutableIntStateOf(1) }
    var skuColIdx by remember { mutableIntStateOf(2) }
    var stockColIdx by remember { mutableIntStateOf(3) }
    var categoryColIdx by remember { mutableIntStateOf(4) }
    var imageUrlColIdx by remember { mutableIntStateOf(5) }

    // Başlık isimlerinden otomatik akıllı sütun eşleştirme
    LaunchedEffect(headers) {
        headers.forEachIndexed { idx, h ->
            val lower = h.lowercase()
            when {
                lower.contains("ad") || lower.contains("name") || lower.contains("ürün") || lower.contains("urun") -> {
                    if (!lower.contains("kod")) nameColIdx = idx
                }
                lower.contains("fiyat") || lower.contains("price") || lower.contains("tutar") -> priceColIdx = idx
                lower.contains("sku") || lower.contains("kod") || lower.contains("barkod") -> skuColIdx = idx
                lower.contains("stok") || lower.contains("adet") || lower.contains("stock") || lower.contains("miktar") -> stockColIdx = idx
                lower.contains("kategori") || lower.contains("category") || lower.contains("grup") -> categoryColIdx = idx
                lower.contains("görsel") || lower.contains("gorsel") || lower.contains("resim") || lower.contains("image") || lower.contains("url") || lower.contains("link") -> imageUrlColIdx = idx
            }
        }
    }

    // Dosya seçici (.csv / .txt)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    rawCsvText = content.removePrefix("\uFEFF")
                    uploadedFileName = uri.lastPathSegment?.substringAfterLast("/") ?: "yuklenen_dosya.csv"
                }
            }
        }
    }

    // Eşleştirilen sütunlara göre önizleme ürünleri
    val previewProducts = remember(
        dataRows,
        nameColIdx,
        priceColIdx,
        skuColIdx,
        stockColIdx,
        categoryColIdx,
        imageUrlColIdx,
        selectedCatalogId,
        currencySymbol,
        lowStockThreshold
    ) {
        dataRows.mapIndexedNotNull { idx, row ->
            val name = row.getOrNull(nameColIdx)?.trim().orEmpty()
            if (name.isBlank()) return@mapIndexedNotNull null

            val rawPrice = row.getOrNull(priceColIdx)?.trim().orEmpty()
            val price = rawPrice
                .replace("[^0-9.,]".toRegex(), "")
                .replace(",", ".")
                .toDoubleOrNull() ?: 450.0

            val sku = row.getOrNull(skuColIdx)?.trim().orEmpty().ifBlank { "CSV-${100 + idx}" }
            val stock = row.getOrNull(stockColIdx)
                ?.replace("[^0-9]".toRegex(), "")
                ?.toIntOrNull() ?: 15
            val category = row.getOrNull(categoryColIdx)?.trim().orEmpty().ifBlank { "Genel" }
            val imageUrl = row.getOrNull(imageUrlColIdx)?.trim().orEmpty()
                .ifBlank { "https://picsum.photos/seed/csv-prev-$idx/600/600" }

            val status = when {
                stock <= 0 -> StockStatus.OUT_OF_STOCK
                stock <= lowStockThreshold -> StockStatus.LOW_STOCK
                else -> StockStatus.IN_STOCK
            }

            Product(
                id = "prod-csv-${UUID.randomUUID().toString().take(8)}",
                catalogId = selectedCatalogId,
                name = name,
                description = "$category kategorisinde toplu CSV içe aktarma ile eklendi.",
                price = price,
                currency = currencySymbol,
                imageUrl = imageUrl,
                category = category,
                subCategory = category,
                trackStock = true,
                stockCount = stock,
                stockStatus = status,
                inStock = stock > 0,
                sku = sku
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            containerColor = BackgroundSlate,
            topBar = {
                Surface(
                    color = SurfaceWhite,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = TrStrings.Inventory.IMPORT_MODAL_TITLE,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = TrStrings.Inventory.IMPORT_MODAL_SUBTITLE,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("btn_close_csv_import_modal")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = TrStrings.Common.CLOSE,
                                tint = TextPrimary
                            )
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = SurfaceWhite,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KYButton(
                            text = TrStrings.Common.CANCEL,
                            onClick = onDismiss,
                            variant = ButtonVariant.SECONDARY,
                            modifier = Modifier.weight(0.9f),
                            tag = "btn_cancel_bulk_csv"
                        )
                        KYButton(
                            text = "${TrStrings.Inventory.BTN_CONFIRM_IMPORT} (${previewProducts.size})",
                            onClick = {
                                if (previewProducts.isNotEmpty()) {
                                    onConfirmImport(selectedCatalogId, previewProducts)
                                }
                            },
                            enabled = previewProducts.isNotEmpty(),
                            variant = ButtonVariant.PRIMARY,
                            modifier = Modifier.weight(1.4f),
                            tag = "btn_confirm_bulk_csv_import"
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1) DOSYA YÜKLEME VE HEDEF KATALOG SEÇİMİ
                KYCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = uploadedFileName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${headers.size} sütun • ${dataRows.size} veri satırı algılandı",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        KYBadge(
                            text = "UTF-8 CSV",
                            variant = BadgeVariant.PRIMARY
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KYButton(
                            text = TrStrings.Inventory.BTN_PICK_CSV_FILE,
                            onClick = { filePickerLauncher.launch("*/*") },
                            variant = ButtonVariant.PRIMARY,
                            leadingIcon = Icons.Outlined.FileUpload,
                            modifier = Modifier.weight(1f),
                            tag = "btn_pick_csv_file"
                        )
                        KYButton(
                            text = TrStrings.Inventory.BTN_LOAD_SAMPLE_CSV,
                            onClick = {
                                rawCsvText = InventoryCsvExporter.defaultSampleCsv()
                                uploadedFileName = "ornek_urun_listesi.csv"
                            },
                            variant = ButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f),
                            tag = "btn_load_sample_csv"
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Düzenlenebilir CSV Veri Alanı
                    OutlinedTextField(
                        value = rawCsvText,
                        onValueChange = { rawCsvText = it },
                        label = { Text("CSV İçeriği (Başlık + Satırlar)") },
                        minLines = 3,
                        maxLines = 5,
                        shape = ButtonRoundedXl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_raw_csv_content")
                    )

                    if (catalogs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = TrStrings.Inventory.TARGET_CATALOG_LABEL,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            catalogs.forEach { cat ->
                                val isSel = cat.id == selectedCatalogId
                                Surface(
                                    onClick = { selectedCatalogId = cat.id },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) PrimaryBlue else SurfaceMuted,
                                    border = BorderStroke(1.dp, if (isSel) PrimaryBlue else BorderLight)
                                ) {
                                    Text(
                                        text = cat.title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSel) SurfaceWhite else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2) SÜTUN EŞLEŞTİRME (COLUMN MAPPING: Ad, Fiyat, SKU, Stok, Kategori, Görsel URL)
                KYCard(modifier = Modifier.testTag("csv_column_mapping_card")) {
                    Text(
                        text = TrStrings.Inventory.COLUMN_MAPPING_TITLE,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = TrStrings.Inventory.COLUMN_MAPPING_DESC,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ColumnMappingSelectorRow(
                            fieldLabel = TrStrings.Inventory.MAP_FIELD_NAME,
                            headers = headers,
                            selectedIndex = nameColIdx,
                            onSelectIndex = { nameColIdx = it },
                            sampleValue = dataRows.firstOrNull()?.getOrNull(nameColIdx).orEmpty()
                        )
                        ColumnMappingSelectorRow(
                            fieldLabel = TrStrings.Inventory.MAP_FIELD_PRICE,
                            headers = headers,
                            selectedIndex = priceColIdx,
                            onSelectIndex = { priceColIdx = it },
                            sampleValue = dataRows.firstOrNull()?.getOrNull(priceColIdx).orEmpty()
                        )
                        ColumnMappingSelectorRow(
                            fieldLabel = TrStrings.Inventory.MAP_FIELD_SKU,
                            headers = headers,
                            selectedIndex = skuColIdx,
                            onSelectIndex = { skuColIdx = it },
                            sampleValue = dataRows.firstOrNull()?.getOrNull(skuColIdx).orEmpty()
                        )
                        ColumnMappingSelectorRow(
                            fieldLabel = TrStrings.Inventory.MAP_FIELD_STOCK,
                            headers = headers,
                            selectedIndex = stockColIdx,
                            onSelectIndex = { stockColIdx = it },
                            sampleValue = dataRows.firstOrNull()?.getOrNull(stockColIdx).orEmpty()
                        )
                        ColumnMappingSelectorRow(
                            fieldLabel = TrStrings.Inventory.MAP_FIELD_CATEGORY,
                            headers = headers,
                            selectedIndex = categoryColIdx,
                            onSelectIndex = { categoryColIdx = it },
                            sampleValue = dataRows.firstOrNull()?.getOrNull(categoryColIdx).orEmpty()
                        )
                        ColumnMappingSelectorRow(
                            fieldLabel = TrStrings.Inventory.MAP_FIELD_IMAGE_URL,
                            headers = headers,
                            selectedIndex = imageUrlColIdx,
                            onSelectIndex = { imageUrlColIdx = it },
                            sampleValue = dataRows.firstOrNull()?.getOrNull(imageUrlColIdx).orEmpty()
                        )
                    }
                }

                // 3) ÖNİZLEME TABLOSU (PREVIEW)
                KYCard(modifier = Modifier.testTag("csv_preview_table_card")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TableChart,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = TrStrings.Inventory.PREVIEW_TABLE_TITLE,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                        KYBadge(
                            text = "${previewProducts.size} ürün hazır",
                            variant = BadgeVariant.SUCCESS
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Önizleme Başlık Satırı
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceMuted)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ürün / Kategori",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary,
                            modifier = Modifier.weight(1.8f)
                        )
                        Text(
                            text = "SKU",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary,
                            modifier = Modifier.weight(0.9f)
                        )
                        Text(
                            text = "Fiyat",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary,
                            modifier = Modifier.weight(0.75f)
                        )
                        Text(
                            text = "Stok",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary,
                            modifier = Modifier.weight(0.75f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    previewProducts.forEachIndexed { idx, prod ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1.8f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AsyncImage(
                                    model = prod.imageUrl,
                                    contentDescription = prod.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SurfaceMuted)
                                )
                                Column {
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = prod.category,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Text(
                                text = prod.sku,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.9f)
                            )

                            Text(
                                text = "${prod.price.toInt()} ${prod.currency}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue,
                                modifier = Modifier.weight(0.75f)
                            )

                            val badgeVariant = when (prod.stockStatus) {
                                StockStatus.IN_STOCK -> BadgeVariant.SUCCESS
                                StockStatus.LOW_STOCK -> BadgeVariant.WARNING
                                StockStatus.OUT_OF_STOCK -> BadgeVariant.DANGER
                            }
                            Box(modifier = Modifier.weight(0.75f)) {
                                KYBadge(
                                    text = "${prod.stockCount} (${prod.stockStatus.labelTr})",
                                    variant = badgeVariant
                                )
                            }
                        }
                        if (idx < previewProducts.lastIndex) {
                            HorizontalDivider(color = BorderLight)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnMappingSelectorRow(
    fieldLabel: String,
    headers: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    sampleValue: String
) {
    var expanded by remember { mutableStateOf(false) }
    val safeIdx = selectedIndex.coerceIn(0, (headers.size - 1).coerceAtLeast(0))
    val currentHeader = headers.getOrElse(safeIdx) { "Sütun ${safeIdx + 1}" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BackgroundSlate)
            .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = fieldLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
            }
            if (sampleValue.isNotBlank()) {
                Text(
                    text = "Örnek: $sampleValue",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(8.dp),
                color = PrimaryBlueSubtle,
                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.35f))
            ) {
                Text(
                    text = "$currentHeader ▾",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryBlue,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                headers.forEachIndexed { idx, header ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${idx + 1}. $header",
                                fontWeight = if (idx == safeIdx) FontWeight.Bold else FontWeight.Normal,
                                color = if (idx == safeIdx) PrimaryBlue else TextPrimary
                            )
                        },
                        onClick = {
                            onSelectIndex(idx)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
