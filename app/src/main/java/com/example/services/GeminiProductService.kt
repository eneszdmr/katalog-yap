package com.example.services

import android.content.Context
import android.util.Log
import com.example.util.ImageCompressionUtil
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AiProductSuggestion(
    val productName: String,
    val shortDescription: String,
    val category: String,
    val subCategory: String,
    val tags: List<String>
)

/**
 * "Fotoğraflardan katalog oluştur" akışında her fotoğraf için üretilen düzenlenebilir ürün taslağı.
 */
data class AiPhotoProductDraft(
    val id: String = UUID.randomUUID().toString(),
    val imageUri: String,
    val productName: String,
    val description: String,
    val category: String,
    val subCategory: String = "Yeni Gelenler",
    val tags: List<String>,
    val price: Double = 650.0
)

/**
 * Ürün düzenleme ekranındaki "Açıklamayı iyileştir" ton seçenekleri.
 */
enum class DescriptionTone(val labelTr: String, val promptInstruction: String) {
    CORPORATE(
        labelTr = "Kurumsal",
        promptInstruction = "Kurumsal, profesyonel, güven veren ve kalite odaklı bir dil kullan (2-3 cümle)."
    ),
    FRIENDLY(
        labelTr = "Samimi",
        promptInstruction = "Samimi, sıcak, müşteriyle sohbet eder gibi içten ve heyecan uyandıran bir dil kullan (2-3 cümle)."
    ),
    SHORT(
        labelTr = "Kısa",
        promptInstruction = "Çok kısa, vurucu, net ve doğrudan satın almaya teşvik eden tek veya en fazla iki kısa cümle kullan."
    )
}

/**
 * Firebase AI Logic (Gemini API) servis katmanı:
 * 1) Görselden ürün bilgisi doldurma (generateProductDetailsFromImage)
 * 2) Fotoğraflardan katalog oluşturma (generateCatalogDraftsFromPhotos)
 * 3) Açıklama yazarı / iyileştirme (improveProductDescription - Kurumsal, Samimi, Kısa)
 * 4) Siparişler ekranı satış asistanı (generateSalesAssistantReply)
 * 5) Analitik ekranı "Bu hafta ne oldu?" özeti (generateWeeklyAnalyticsSummary)
 */
object GeminiProductService {

    private const val TAG = "GeminiProductService"

    private val productJsonSchema = Schema.obj(
        mapOf(
            "productName" to Schema.string("Ürünün dikkat çekici, net Türkçe satış adı"),
            "shortDescription" to Schema.string("Türkçe, 2-3 cümlelik ikna edici kısa satış açıklaması"),
            "category" to Schema.string("Ana kategori önerisi (örn: Giyim, Elektronik, Ev & Yaşam, Gıda, Kozmetik, Aksesuar)"),
            "subCategory" to Schema.string("Alt kategori önerisi (örn: Üst Giyim, Elbise, Dekoratif Obje, Ses & Kulaklık)"),
            "tags" to Schema.array(Schema.string("Ürünle ilgili Türkçe etiket")),
            "suggestedPrice" to Schema.double("TL cinsinden tahmini satış fiyatı (örn: 750)")
        ),
        optionalProperties = listOf("suggestedPrice")
    )

    private val analyticsInsightsSchema = Schema.obj(
        mapOf(
            "insights" to Schema.array(
                Schema.string("Türkçe, somut metrik içeren içgörü ve aksiyon önerisi maddesi")
            )
        )
    )

    private val productVisionModel by lazy {
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = productJsonSchema
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    private val textModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    private val analyticsJsonModel by lazy {
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = analyticsInsightsSchema
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    /**
     * Tekil ürün görselinden ürün adı, açıklama, kategori ve etiket üretir.
     */
    suspend fun generateProductDetailsFromImage(
        context: Context,
        imageSource: String
    ): Result<AiProductSuggestion> {
        return try {
            val bitmap = ImageCompressionUtil.loadBitmapForGemini(context, imageSource)
                ?: return Result.failure(IllegalArgumentException("Görsel okunamadı, lütfen başka bir fotoğraf deneyin."))

            val response = withTimeout(60_000L) {
                productVisionModel.generateContent(
                    content {
                        image(bitmap)
                        text(
                            """
                            Bu ürün görselini incele ve e-ticaret kataloğu için Türkçe ürün bilgilerini oluştur:
                            - productName: Ürünün Türkçe, profesyonel ve çekici satış adı.
                            - shortDescription: Ürünün öne çıkan özelliklerini ve kullanım değerini anlatan 2-3 cümlelik Türkçe kısa satış açıklaması.
                            - category: Ana kategori (Giyim, Elektronik, Ev & Yaşam, Gıda, Kozmetik veya Diğer).
                            - subCategory: Alt kategori (örn: Üst Giyim, Alt Giyim, Elbise, Aksesuar, Dekoratif Obje, Sofra & Mutfak, Ses & Kulaklık vb.).
                            - tags: 3 ile 5 adet Türkçe etiket (örn: Yeni Sezon, Doğal Doku, El Yapımı, Çok Satanlar).
                            """.trimIndent()
                        )
                    }
                )
            }

            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Yapay zeka yanıtı boş döndü."))

            val jsonObj = JSONObject(rawJson)
            val productName = jsonObj.optString("productName", "").trim()
            val shortDescription = jsonObj.optString("shortDescription", "").trim()
            val category = jsonObj.optString("category", "Giyim").trim()
            val subCategory = jsonObj.optString("subCategory", "Genel").trim()
            val tagsArr = jsonObj.optJSONArray("tags") ?: JSONArray()
            val tags = buildList {
                for (i in 0 until tagsArr.length()) {
                    val item = tagsArr.optString(i, "").trim()
                    if (item.isNotBlank()) add(item)
                }
            }

            if (productName.isBlank()) {
                return Result.failure(IllegalStateException("Ürün adı oluşturulamadı."))
            }

            Result.success(
                AiProductSuggestion(
                    productName = productName,
                    shortDescription = shortDescription,
                    category = category.ifBlank { "Giyim" },
                    subCategory = subCategory.ifBlank { "Genel" },
                    tags = tags
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini isteği zaman aşımına uğradı", e)
            Result.failure(Exception("Yapay zeka yanıtı zaman aşımına uğradı, lütfen tekrar deneyin.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini ürün analizi hatası", e)
            Result.failure(Exception("Yapay zeka analizi sırasında hata oluştu: ${e.localizedMessage ?: "Bilinmeyen hata"}", e))
        }
    }

    /**
     * 1) "Fotoğraflardan katalog oluştur":
     * Seçilen birden fazla fotoğrafın her biri için Gemini API ile ürün adı, açıklama,
     * kategori ve tahmini etiketler üretir; düzenlenebilir taslak listesi döner.
     */
    suspend fun generateCatalogDraftsFromPhotos(
        context: Context,
        photoUris: List<String>,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<List<AiPhotoProductDraft>> {
        if (photoUris.isEmpty()) {
            return Result.failure(IllegalArgumentException("Lütfen en az bir fotoğraf seçin."))
        }
        return try {
            val drafts = mutableListOf<AiPhotoProductDraft>()
            val total = photoUris.size

            for ((index, uri) in photoUris.withIndex()) {
                onProgress(index, total)
                val bitmap = ImageCompressionUtil.loadBitmapForGemini(context, uri)
                    ?: return Result.failure(
                        IllegalArgumentException("${index + 1}. fotoğraf okunamadı. Lütfen geçerli bir görsel seçin.")
                    )

                val response = withTimeout(60_000L) {
                    productVisionModel.generateContent(
                        content {
                            image(bitmap)
                            text(
                                """
                                Bu fotoğrafı bir e-ticaret kataloğu ürünü olarak analiz et (${index + 1}. ürün).
                                Lütfen Türkçe olarak şu alanları üret:
                                - productName: Ürünün net, çekici Türkçe adı.
                                - shortDescription: Ürünün kumaş/materyal, tasarım ve kullanım avantajlarını anlatan 2-3 cümlelik Türkçe açıklama.
                                - category: Ana kategori (örn: Giyim, Aksesuar, Ev & Yaşam, Elektronik, Kozmetik).
                                - subCategory: Alt kategori (örn: Üst Giyim, Dış Giyim, Çanta, Dekorasyon).
                                - tags: 3-4 adet Türkçe tahmini etiket (örn: Yeni Sezon, Özel Tasarım, Doğal Doku).
                                - suggestedPrice: Türkiye piyasasına uygun tahmini TL satış fiyatı (sayı olarak, örn: 890).
                                """.trimIndent()
                            )
                        }
                    )
                }

                val rawJson = response.text
                    ?: return Result.failure(IllegalStateException("${index + 1}. fotoğraf için yapay zeka yanıtı boş döndü."))

                val jsonObj = JSONObject(rawJson)
                val name = jsonObj.optString("productName", "").trim().ifBlank { "Koleksiyon Ürünü #${index + 1}" }
                val desc = jsonObj.optString("shortDescription", "").trim().ifBlank {
                    "$name özel sezon koleksiyonu ürünü."
                }
                val category = jsonObj.optString("category", "Giyim").trim().ifBlank { "Giyim" }
                val subCategory = jsonObj.optString("subCategory", "Yeni Gelenler").trim().ifBlank { "Yeni Gelenler" }
                val price = jsonObj.optDouble("suggestedPrice", (550 + index * 120).toDouble())
                    .takeIf { !it.isNaN() && it > 0.0 } ?: (550 + index * 120).toDouble()

                val tagsArr = jsonObj.optJSONArray("tags") ?: JSONArray()
                val parsedTags = buildList {
                    for (i in 0 until tagsArr.length()) {
                        val t = tagsArr.optString(i, "").trim().removePrefix("#")
                        if (t.isNotBlank()) add(t)
                    }
                }.ifEmpty { listOf("Yeni Sezon", category) }

                drafts.add(
                    AiPhotoProductDraft(
                        imageUri = uri,
                        productName = name,
                        description = desc,
                        category = category,
                        subCategory = subCategory,
                        tags = parsedTags,
                        price = price
                    )
                )
                onProgress(index + 1, total)
            }

            Result.success(drafts)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Fotoğraflardan katalog analizi zaman aşımına uğradı", e)
            Result.failure(Exception("Fotoğraf analizi zaman aşımına uğradı. Lütfen tekrar deneyin.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Fotoğraflardan katalog analizi hatası", e)
            Result.failure(Exception("Fotoğraflar analiz edilirken hata oluştu: ${e.localizedMessage ?: "Bağlantı hatası"}", e))
        }
    }

    /**
     * 2) Açıklama yazarı ("Açıklamayı iyileştir"):
     * Ürün düzenleme ekranında seçilen tona göre (Kurumsal, Samimi, Kısa) Türkçe ürün açıklamasını iyileştirir.
     */
    suspend fun improveProductDescription(
        productName: String,
        currentDescription: String,
        category: String,
        tone: DescriptionTone
    ): Result<String> {
        val cleanName = productName.trim().ifBlank { "Koleksiyon Ürünü" }
        return try {
            val response = withTimeout(60_000L) {
                textModel.generateContent(
                    """
                    Sen deneyimli bir e-ticaret metin yazarısın.
                    Aşağıdaki ürün için Türkçe satış açıklamasını "${tone.labelTr}" tonunda yeniden yaz ve iyileştir.
                    
                    Kurallar:
                    - Ton talimatı: ${tone.promptInstruction}
                    - Sadece iyileştirilmiş Türkçe açıklama metnini döndür (başlık, tırnak işareti veya ek açıklama ekleme).
                    
                    Ürün Adı: $cleanName
                    Kategori: ${category.ifBlank { "Genel" }}
                    Mevcut Açıklama: ${currentDescription.ifBlank { "Belirtilmedi, ürün adına ve kategorisine göre oluştur." }}
                    """.trimIndent()
                )
            }

            val improved = response.text?.trim()?.removeSurrounding("\"")
                ?: return Result.failure(IllegalStateException("Açıklama üretilemedi, yanıt boş döndü."))

            if (improved.isBlank()) {
                return Result.failure(IllegalStateException("Açıklama metni boş döndü."))
            }

            Result.success(improved)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Açıklama iyileştirme zaman aşımına uğradı", e)
            Result.failure(Exception("İstek zaman aşımına uğradı. Lütfen tekrar deneyin.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Açıklama iyileştirme hatası", e)
            Result.failure(Exception("Açıklama iyileştirilemedi: ${e.localizedMessage ?: "Bilinmeyen hata"}", e))
        }
    }

    /**
     * 3) Satış Asistanı:
     * Siparişler ekranında müşteri mesajına veya sipariş notuna profesyonel, nazik ve satış odaklı
     * Türkçe cevap önerisi üretir.
     */
    suspend fun generateSalesAssistantReply(
        customerMessage: String,
        orderContextSummary: String
    ): Result<String> {
        val cleanMsg = customerMessage.trim()
        if (cleanMsg.isBlank()) {
            return Result.failure(IllegalArgumentException("Lütfen yanıtlanacak müşteri mesajını girin veya seçin."))
        }
        return try {
            val response = withTimeout(60_000L) {
                textModel.generateContent(
                    """
                    Sen bir butik/mağaza sahibinin WhatsApp ve sipariş mesajlarını yanıtlayan "Satış Asistanı"sın.
                    Müşterinin mesajına Türkçe, nazik, güven veren, çözüm odaklı ve doğrudan WhatsApp'tan gönderilebilecek hazır bir yanıt önerisi yaz.
                    
                    Sipariş / Mağaza Bağlamı:
                    $orderContextSummary
                    
                    Müşteri Mesajı / Sorusu:
                    "$cleanMsg"
                    
                    Kurallar:
                    - Sadece müşteriye gönderilecek Türkçe cevabı yaz (2-4 cümle).
                    - Samimi ama profesyonel bir üslup kullan.
                    """.trimIndent()
                )
            }

            val reply = response.text?.trim()?.removeSurrounding("\"")
                ?: return Result.failure(IllegalStateException("Cevap önerisi oluşturulamadı."))

            if (reply.isBlank()) {
                return Result.failure(IllegalStateException("Cevap önerisi boş döndü."))
            }

            Result.success(reply)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Satış asistanı zaman aşımına uğradı", e)
            Result.failure(Exception("Satış asistanı yanıtı zaman aşımına uğradı. Lütfen tekrar deneyin.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Satış asistanı hatası", e)
            Result.failure(Exception("Cevap önerisi üretilemedi: ${e.localizedMessage ?: "Bağlantı hatası"}", e))
        }
    }

    /**
     * 4) Analitik Özeti ("Bu hafta ne oldu?"):
     * Mağaza analitik metriklerini Gemini'ye gönderip 3-4 maddelik Türkçe içgörü ve aksiyon önerisi üretir.
     */
    suspend fun generateWeeklyAnalyticsSummary(
        metricsSummary: String
    ): Result<List<String>> {
        return try {
            val response = withTimeout(60_000L) {
                analyticsJsonModel.generateContent(
                    """
                    Sen bir e-ticaret ve dijital katalog büyüme danışmanısın.
                    Aşağıdaki mağaza analitik verilerini incele ve mağaza sahibine "Bu hafta ne oldu?" sorusunu yanıtlayan
                    tam olarak 4 maddelik Türkçe içgörü ve somut satış artırma önerisi üret.
                    
                    Mağaza Metrikleri:
                    $metricsSummary
                    
                    Kurallar:
                    - Her madde net, anlaşılır Türkçe olsun ve gerçek metrikleri (ziyaretçi, dönüşüm, en çok ilgi gören ürün, WhatsApp trafiği vb.) yorumlasın.
                    - En az 3, en fazla 4 madde döndür.
                    """.trimIndent()
                )
            }

            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Analitik özeti yanıtı boş döndü."))

            val jsonObj = JSONObject(rawJson)
            val arr = jsonObj.optJSONArray("insights") ?: JSONArray()
            val insights = buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.optString(i, "").trim().removePrefix("-").removePrefix("•").trim()
                    if (item.isNotBlank()) add(item)
                }
            }

            if (insights.isEmpty()) {
                return Result.failure(IllegalStateException("Analitik içgörü maddeleri üretilemedi."))
            }

            Result.success(insights.take(4))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Analitik özeti zaman aşımına uğradı", e)
            Result.failure(Exception("Analitik özeti zaman aşımına uğradı. Lütfen tekrar deneyin.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Analitik özeti hatası", e)
            Result.failure(Exception("Analitik özeti alınamadı: ${e.localizedMessage ?: "Bilinmeyen hata"}", e))
        }
    }
}
