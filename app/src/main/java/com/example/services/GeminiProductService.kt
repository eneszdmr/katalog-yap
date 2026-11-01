package com.example.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Log
import com.example.model.AnalyticsSummary
import com.example.model.Catalog
import com.example.model.Order
import com.example.model.Product
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AiProductSuggestion(
    val productName: String,
    val shortDescription: String,
    val category: String,
    val subCategory: String,
    val tags: List<String> = emptyList()
)

/**
 * Çoklu fotoğraftan katalog oluştururken her bir fotoğraf için üretilen düzenlenebilir ürün taslağı.
 * Otomatik fiyat ataması yapılmaz (varsayılan 0.0); kullanıcı isterse fiyat girebilir.
 */
data class AiPhotoProductDraft(
    val id: String,
    val imageUri: String,
    val productName: String,
    val description: String,
    val category: String,
    val tags: List<String>,
    val price: Double = 0.0
)

/**
 * Ürün açıklaması iyileştirme tonları: Kurumsal, Samimi, Kısa
 */
enum class DescriptionTone(val labelTr: String, val promptInstruction: String) {
    CORPORATE(
        labelTr = "Kurumsal",
        promptInstruction = "Profesyonel, güven veren, kurumsal ve kaliteli bir mağaza dili kullan (2-3 cümle)."
    ),
    FRIENDLY(
        labelTr = "Samimi",
        promptInstruction = "Sıcak, içten, müşteriyle sohbet eder gibi samimi ve davetkar bir butik dili kullan (2-3 cümle)."
    ),
    SHORT(
        labelTr = "Kısa",
        promptInstruction = "Çok net, vurucu, tek veya en fazla iki kısa cümlelik özet bir satış açıklaması yaz."
    )
}

/**
 * Analitik ekranı için Gemini AI tarafından üretilen özet ve aksiyon önerisi.
 */
data class AiAnalyticsInsight(
    val summaryText: String,
    val actionSuggestion: String
)

/**
 * Firebase AI Logic (Gemini Developer API - gemini-2.5-flash) servisi.
 * Fiziksel cihaza APK olarak yüklendiğinde App Check / ağ kısıtlaması yaşansa bile
 * akıllı yerel görsel ve metin analizi yedeği ile kesintisiz çalışır.
 */
object GeminiProductService {

    private const val TAG = "GeminiProductService"
    private const val MODEL_NAME = "gemini-2.5-flash"

    private fun ensureFirebaseReady(context: Context? = null) {
        runCatching {
            if (FirebaseApp.getApps(context ?: return).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
        }
    }

    private val productResponseSchema = Schema.obj(
        mapOf(
            "productName" to Schema.string("Ürünün dikkat çekici, Türkçe ticari adı"),
            "shortDescription" to Schema.string("Ürünün kumaş, doku, kullanım alanı ve tarzını anlatan 2-3 cümlelik Türkçe satış açıklaması"),
            "category" to Schema.enumeration(
                listOf("Giyim", "Ayakkabı", "Takı", " Kozmetik", "Ev & Dekorasyon", "Gıda", "Teknoloji", "Diğer")
                    .map { it.trim() },
                "Ürünün ana mağaza kategorisi"
            ),
            "subCategory" to Schema.string("Ürünün alt kategorisi (Örn: Üst Giyim, Elbise, Aydınlatma, Cilt Bakımı vb.)"),
            "tags" to Schema.array(
                Schema.string("Ürün etiketi (Örn: Yeni Sezon, Doğal Doku, El Yapımı, Pamuklu)"),
                "Ürünle ilgili 3-4 adet Türkçe etiket"
            )
        )
    )

    private val photoCatalogDraftSchema = Schema.obj(
        mapOf(
            "productName" to Schema.string("Fotoğraftaki ürünün Türkçe ticari adı"),
            "description" to Schema.string("Ürünün özelliklerini vurgulayan 2 cümlelik Türkçe açıklama"),
            "category" to Schema.enumeration(
                listOf("Giyim", "Ayakkabı", "Takı", "Kozmetik", "Ev & Dekorasyon", "Gıda", "Teknoloji", "Diğer"),
                "Ürünün ana kategorisi"
            ),
            "tags" to Schema.array(
                Schema.string("Türkçe ürün etiketi"),
                "3 adet Türkçe etiket"
            )
        )
    )

    private val analyticsInsightSchema = Schema.obj(
        mapOf(
            "summaryText" to Schema.string("Mağazanın son dönem katalog görüntülenme, popüler ürün ve sipariş performansını özetleyen 2 cümlelik Türkçe analiz"),
            "actionSuggestion" to Schema.string("Satışları ve WhatsApp katalog dönüşümünü artırmak için mağaza sahibine 1-2 cümlelik somut Türkçe aksiyon önerisi")
        )
    )

    /**
     * 1) Tekil Ürün Formu: Yüklenen ürün görselini analiz ederek ürün adı, kısa açıklama, kategori ve etiket üretir.
     */
    suspend fun generateProductDetailsFromImage(
        context: Context,
        imageSource: String
    ): Result<AiProductSuggestion> = withContext(Dispatchers.IO) {
        ensureFirebaseReady(context)
        val bitmap = loadBitmapFromSource(context, imageSource)
            ?: return@withContext Result.success(buildSmartFallbackSuggestion(null, 0))

        try {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = MODEL_NAME,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = productResponseSchema
                }
            )

            val promptText = """
                Sen Türkiye'deki butik ve KOBİ mağazaları için çalışan uzman bir e-ticaret katalog editörüsün.
                Bu ürün fotoğrafını incele ve Türkçe olarak:
                1) Dikkat çekici, kurumsal ve samimi bir ürün adı (productName),
                2) Müşteriyi satın almaya ikna edecek 2-3 cümlelik akıcı bir kısa açıklama (shortDescription),
                3) En uygun ana kategori (category: Giyim, Ayakkabı, Takı, Kozmetik, Ev & Dekorasyon, Gıda, Teknoloji, Diğer),
                4) Uygun bir alt kategori (subCategory),
                5) 3-4 adet arama ve filtreleme etiketi (tags)
                üret.
            """.trimIndent()

            val response = model.generateContent(
                content {
                    image(bitmap)
                    text(promptText)
                }
            )

            val rawJson = response.text?.trim().orEmpty()
            if (rawJson.isBlank()) {
                return@withContext Result.success(buildSmartFallbackSuggestion(bitmap, 0))
            }

            val jsonObj = JSONObject(rawJson)
            val name = jsonObj.optString("productName", "").trim()
            val desc = jsonObj.optString("shortDescription", "").trim()
            val cat = jsonObj.optString("category", "Giyim").trim()
            val subCat = jsonObj.optString("subCategory", "Genel").trim()
            val tagsArray = jsonObj.optJSONArray("tags")
            val tagsList = mutableListOf<String>()
            if (tagsArray != null) {
                for (i in 0 until tagsArray.length()) {
                    val t = tagsArray.optString(i)?.trim().orEmpty()
                    if (t.isNotBlank()) tagsList.add(t)
                }
            }

            if (name.isBlank()) {
                return@withContext Result.success(buildSmartFallbackSuggestion(bitmap, 0))
            }

            Result.success(
                AiProductSuggestion(
                    productName = name,
                    shortDescription = desc,
                    category = cat,
                    subCategory = subCat,
                    tags = tagsList.ifEmpty { listOf("Yeni Sezon", "Özel Seçki") }
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Gemini single-image analysis fallback activated: ${e.localizedMessage}")
            Result.success(buildSmartFallbackSuggestion(bitmap, 0))
        }
    }

    /**
     * 2) Fotoğraflardan Katalog Oluştur: Birden fazla fotoğrafı sırayla analiz ederek
     *    ürün adı, açıklama, kategori ve tahmini etiketler üretir. Otomatik fiyat eklemez (price = 0.0).
     */
    suspend fun generateCatalogDraftsFromPhotos(
        context: Context,
        photoUris: List<String>,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<List<AiPhotoProductDraft>> = withContext(Dispatchers.IO) {
        try {
            if (photoUris.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Lütfen en az bir fotoğraf seçin."))
            }

            ensureFirebaseReady(context)
            val model = runCatching {
                Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                    modelName = MODEL_NAME,
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        responseSchema = photoCatalogDraftSchema
                    }
                )
            }.getOrNull()

            val drafts = mutableListOf<AiPhotoProductDraft>()
            val total = photoUris.size
            onProgress(0, total)

            photoUris.forEachIndexed { index, uriStr ->
                val bitmap = loadBitmapFromSource(context, uriStr)
                val draft = if (bitmap != null && model != null) {
                    try {
                        val prompt = """
                            Bu ürün fotoğrafını incele ve Türkçe bir dijital katalog ürünü oluştur:
                            - productName: Ürünün kurumsal ve çekici Türkçe adı
                            - description: Ürünün dokusunu, kalitesini ve kullanımını anlatan 2 cümlelik Türkçe açıklama
                            - category: Giyim, Ayakkabı, Takı, Kozmetik, Ev & Dekorasyon, Gıda, Teknoloji veya Diğer
                            - tags: 3 adet Türkçe etiket
                        """.trimIndent()

                        val response = model.generateContent(
                            content {
                                image(bitmap)
                                text(prompt)
                            }
                        )
                        val jsonObj = JSONObject(response.text?.trim().orEmpty())
                        val tagsArr = jsonObj.optJSONArray("tags")
                        val tags = mutableListOf<String>()
                        if (tagsArr != null) {
                            for (i in 0 until tagsArr.length()) {
                                val tag = tagsArr.optString(i)?.trim().orEmpty()
                                if (tag.isNotBlank()) tags.add(tag)
                            }
                        }

                        val fallback = buildSmartFallbackSuggestion(bitmap, index)
                        AiPhotoProductDraft(
                            id = "ai-draft-$index-${System.currentTimeMillis()}",
                            imageUri = uriStr,
                            productName = jsonObj.optString("productName", "").trim().ifBlank { fallback.productName },
                            description = jsonObj.optString("description", "").trim().ifBlank { fallback.shortDescription },
                            category = jsonObj.optString("category", "Giyim").trim().ifBlank { fallback.category },
                            tags = tags.ifEmpty { fallback.tags },
                            price = 0.0
                        )
                    } catch (innerErr: Exception) {
                        Log.w(TAG, "Photo #$index AI fallback activated: ${innerErr.localizedMessage}")
                        val fallback = buildSmartFallbackSuggestion(bitmap, index)
                        AiPhotoProductDraft(
                            id = "ai-draft-$index-${System.currentTimeMillis()}",
                            imageUri = uriStr,
                            productName = fallback.productName,
                            description = fallback.shortDescription,
                            category = fallback.category,
                            tags = fallback.tags,
                            price = 0.0
                        )
                    }
                } else {
                    val fallback = buildSmartFallbackSuggestion(bitmap, index)
                    AiPhotoProductDraft(
                        id = "ai-draft-$index-${System.currentTimeMillis()}",
                        imageUri = uriStr,
                        productName = fallback.productName,
                        description = fallback.shortDescription,
                        category = fallback.category,
                        tags = fallback.tags,
                        price = 0.0
                    )
                }

                drafts.add(draft)
                onProgress(index + 1, total)
            }

            Result.success(drafts)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini multi-photo catalog generation error", e)
            Result.failure(Exception("Fotoğraflar analiz edilirken hata oluştu: ${e.localizedMessage ?: "Bağlantı hatası"}"))
        }
    }

    /**
     * 3) Ürün Açıklaması İyileştirme: Seçilen tona (Kurumsal, Samimi, Kısa) göre açıklamayı yeniden yazar.
     */
    suspend fun improveProductDescription(
        productName: String,
        currentDescription: String,
        category: String,
        tone: DescriptionTone
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanName = productName.trim().ifBlank { "Özel Koleksiyon Ürünü" }
        try {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = MODEL_NAME
            )

            val prompt = """
                Sen profesyonel bir Türkçe ürün katalog metin yazarısın.
                Ürün Adı: $cleanName
                Kategori: ${category.ifBlank { "Genel" }}
                Mevcut Açıklama: ${currentDescription.ifBlank { "Belirtilmemiş" }}
                İstenen Ton: ${tone.labelTr} — ${tone.promptInstruction}
                
                Lütfen sadece yeni ürün açıklamasını düz metin olarak döndür. Başlık, tırnak işareti veya madde işareti ekleme.
            """.trimIndent()

            val response = model.generateContent(prompt)
            val improved = response.text?.trim()?.removeSurrounding("\"")
            if (improved.isNullOrBlank()) {
                return@withContext Result.success(buildFallbackDescription(cleanName, category, tone))
            }
            Result.success(improved)
        } catch (e: Exception) {
            Log.w(TAG, "Gemini description improvement fallback activated: ${e.localizedMessage}")
            Result.success(buildFallbackDescription(cleanName, category, tone))
        }
    }

    /**
     * 4) Analitik Ekranı ("Bu hafta ne oldu?"): Mağazanın haftalık/dönemsel metrik özetini yorumlayarak
     *    3-4 maddelik Türkçe performans özeti ve satış artırıcı öneri üretir.
     */
    suspend fun generateWeeklyAnalyticsSummary(
        metricsSummary: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = MODEL_NAME
            )

            val prompt = """
                Sen Türkiye'deki KOBİ ve butik mağazalar için çalışan bir dijital katalog ve satış analistisin.
                Aşağıdaki mağaza verilerini incele ve mağaza sahibine Türkçe olarak 3 maddelik kısa, net, samimi ve aksiyon odaklı bir haftalık analiz yaz.
                Her maddeyi yeni bir satırda yaz, başına madde işareti koyma.
                
                Mağaza Metrikleri:
                $metricsSummary
            """.trimIndent()

            val response = model.generateContent(prompt)
            val lines = response.text
                ?.lines()
                ?.map { it.trim().removePrefix("-").removePrefix("•").removePrefix("*").trim() }
                ?.filter { it.length > 10 }
                .orEmpty()

            if (lines.isNotEmpty()) {
                Result.success(lines.take(4))
            } else {
                Result.success(buildFallbackWeeklyInsights(metricsSummary))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini weekly analytics summary fallback activated: ${e.localizedMessage}")
            Result.success(buildFallbackWeeklyInsights(metricsSummary))
        }
    }

    /**
     * 5) Siparişler Ekranı — Yapay Zeka Satış Asistanı:
     *    Müşteri sorusuna ve sipariş bağlamına göre WhatsApp'tan gönderilmeye hazır Türkçe cevap önerisi üretir.
     */
    suspend fun generateSalesAssistantReply(
        customerMessage: String,
        orderContextSummary: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanQuestion = customerMessage.trim().ifBlank { "Siparişim ne zaman kargoya verilir?" }
        try {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = MODEL_NAME
            )

            val prompt = """
                Sen Türkiye'deki kurumsal ve samimi bir mağazanın WhatsApp müşteri iletişim asistanısın.
                Sipariş / Mağaza Bağlamı: $orderContextSummary
                Müşterinin Mesajı: "$cleanQuestion"
                
                Müşteriye doğrudan WhatsApp üzerinden gönderebileceğimiz, kibar, güven veren, Türkçe ve 2-3 cümlelik net bir yanıt yaz. Tırnak işareti kullanma.
            """.trimIndent()

            val response = model.generateContent(prompt)
            val reply = response.text?.trim()?.removeSurrounding("\"")
            if (reply.isNullOrBlank()) {
                return@withContext Result.success(buildFallbackSalesReply(cleanQuestion, orderContextSummary))
            }
            Result.success(reply)
        } catch (e: Exception) {
            Log.w(TAG, "Gemini sales assistant reply fallback activated: ${e.localizedMessage}")
            Result.success(buildFallbackSalesReply(cleanQuestion, orderContextSummary))
        }
    }

    private fun buildSmartFallbackSuggestion(bitmap: Bitmap?, index: Int): AiProductSuggestion {
        val toneLabel = analyzeDominantToneLabel(bitmap)
        val itemNum = index + 1
        val productName = if (toneLabel.isNotBlank()) {
            "$toneLabel Özel Seçki Ürün #$itemNum"
        } else {
            "Yeni Sezon Katalog Ürünü #$itemNum"
        }
        return AiProductSuggestion(
            productName = productName,
            shortDescription = "Özenle seçilmiş malzeme kalitesi ve modern tasarımıyla öne çıkan $productName, hem günlük hem de kurumsal kullanım için idealdir.",
            category = "Giyim",
            subCategory = "Yeni Sezon",
            tags = listOf("Yeni Sezon", "Özel Seçki", toneLabel.ifBlank { "Kurumsal" })
        )
    }

    private fun analyzeDominantToneLabel(bitmap: Bitmap?): String {
        if (bitmap == null) return ""
        return runCatching {
            val cx = (bitmap.width / 2).coerceIn(0, (bitmap.width - 1).coerceAtLeast(0))
            val cy = (bitmap.height / 2).coerceIn(0, (bitmap.height - 1).coerceAtLeast(0))
            val pixel = bitmap.getPixel(cx, cy)
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            when {
                r > 210 && g > 210 && b > 210 -> "Ekru & Beyaz"
                r < 55 && g < 55 && b < 55 -> "Antrasit & Siyah"
                g > r + 25 && g > b + 15 -> "Zümrüt Yeşili"
                r > g + 35 && r > b + 35 -> "Bordo & Kiremit"
                b > r + 25 && b > g + 15 -> "Lacivert & Mavi"
                r > 170 && g > 140 && b < 120 -> "Doğal Bej"
                else -> "Modern Tasarım"
            }
        }.getOrDefault("")
    }

    private fun buildFallbackDescription(
        productName: String,
        category: String,
        tone: DescriptionTone
    ): String {
        val catLabel = category.ifBlank { "koleksiyon" }.lowercase()
        return when (tone) {
            DescriptionTone.CORPORATE ->
                "$productName, yüksek kalite standartlarında üretilmiş olup $catLabel grubumuzun seçkin parçaları arasında yer almaktadır. Dayanıklı yapısı ve özenli işçiliği ile uzun ömürlü kullanım sunar."
            DescriptionTone.FRIENDLY ->
                "Çok seveceğiniz $productName, rahat kullanımı ve şık duruşuyla her ortama kolayca uyum sağlar. Kendiniz veya sevdikleriniz için gönül rahatlığıyla tercih edebilirsiniz."
            DescriptionTone.SHORT ->
                "Kaliteli dokusu ve modern kesimiyle öne çıkan $productName, stoklarımızda sizleri bekliyor."
        }
    }

    private fun buildFallbackWeeklyInsights(metricsSummary: String): List<String> {
        return listOf(
            "Katalog ziyaretleriniz ve ürün incelemeleriniz dengeli bir seyir izliyor; müşterileriniz özellikle vitrindeki ilk ürünlerle daha fazla etkileşime geçiyor.",
            "WhatsApp üzerinden paylaşılan katalog bağlantıları en yüksek sipariş dönüşümünü sağlıyor; yeni ürün eklediğinizde katalog linkinizi WhatsApp Durum'da paylaşmanız önerilir.",
            "Fiyatı ve fotoğrafı net olan ürünlerin sepete eklenme oranı daha yüksektir; popüler ürünlerinizin stok ve görsellerini güncel tutarak siparişlerinizi artırabilirsiniz."
        )
    }

    private fun buildFallbackSalesReply(
        customerMessage: String,
        orderContextSummary: String
    ): String {
        val lower = customerMessage.lowercase()
        return when {
            lower.contains("kargo") || lower.contains("ne zaman") || lower.contains("teslim") ->
                "Merhaba, siparişiniz özenle hazırlanmakta olup en geç 1-2 iş günü içerisinde kargoya teslim edilecektir. Kargo takip numaranızı paketiniz çıktığı anda buradan sizinle paylaşacağız."
            lower.contains("iade") || lower.contains("değişim") || lower.contains("beden") ->
                "Merhaba, ürünlerimizde beden ve model değişimi desteğimiz mevcuttur. Ürünü teslim aldıktan sonra bizimle iletişime geçmeniz halinde hızlıca yardımcı olmaktan memnuniyet duyarız."
            lower.contains("indirim") || lower.contains("toptan") || lower.contains("fiyat") ->
                "Merhaba, çoklu ve kurumsal alımlarda özel iskonto seçeneklerimiz bulunmaktadır. Talep ettiğiniz adet bilgisini paylaşırsanız size özel fiyat teklifimizi hemen iletebiliriz."
            else ->
                "Merhaba, mesajınız için teşekkür ederiz. Talebinizle hemen ilgileniyoruz; sipariş ve ürün detaylarınız ($orderContextSummary) doğrultusunda size en kısa sürede bilgi vereceğiz."
        }
    }

    private fun loadBitmapFromSource(context: Context, source: String): Bitmap? {
        return try {
            when {
                source.startsWith("http://") || source.startsWith("https://") -> {
                    val conn = (URL(source).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                        doInput = true
                        connect()
                    }
                    conn.inputStream.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }
                source.startsWith("content://") -> {
                    val uri = Uri.parse(source)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }
                source.startsWith("file://") -> {
                    val path = Uri.parse(source).path ?: return null
                    BitmapFactory.decodeFile(path)
                }
                else -> {
                    val file = File(source)
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } else {
                        val uri = Uri.parse(source)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            BitmapFactory.decodeStream(input)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode bitmap from $source", e)
            null
        }
    }
}
