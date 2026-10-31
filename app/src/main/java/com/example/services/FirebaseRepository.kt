package com.example.services

import android.content.Context
import android.util.Log
import com.example.R
import com.example.auth.OperationType
import com.example.model.Catalog
import com.example.model.FirestoreCatalogDocument
import com.example.model.FirestoreProductDocument
import com.example.model.Product
import com.example.model.StoreSettings
import com.example.model.sanitizeFirestoreDocId
import com.example.model.toFirestoreCatalogDocument
import com.example.model.toFirestoreDocument
import com.example.model.toFirestoreProductDocument
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * MariaDB'den Firebase Firestore'a geçişi yöneten ana `FirebaseRepository` sınıfı.
 *
 * - Her zaman `R.string.firestore_database_id` ile tanımlı olan isimlendirilmiş Firestore
 *   veritabanını (`ai-studio-android-katalogy-2462d413-3378-4213-8f18-2ff139fd35eb`) kullanır.
 * - `Catalog` ve `Product` domain modellerini `FirestoreCatalogDocument` ve
 *   `FirestoreProductDocument` DTO sınıfları aracılığıyla `/catalogs/{catalogId}` ve
 *   `/products/{productId}` koleksiyonlarına eşler.
 * - Gerçek zamanlı `Flow` dinleyicileri (`callbackFlow` + `addSnapshotListener`) ve
 *   MariaDB -> Firestore toplu taşıma (`migrateMariaDbCatalogsAndProducts`) metodlarını içerir.
 */
class FirebaseRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val appContext: Context? = null
) {
    constructor(context: Context) : this(
        db = context.applicationContext.let { appCtx ->
            if (FirebaseApp.getApps(appCtx).isEmpty()) {
                FirebaseApp.initializeApp(appCtx)
            }
            FirebaseFirestore.getInstance(
                appCtx.getString(R.string.firestore_database_id)
            )
        },
        auth = Firebase.auth,
        appContext = context.applicationContext
    )

    companion object {
        const val COLLECTION_CATALOGS = "catalogs"
        const val COLLECTION_PRODUCTS = "products"
        private const val TAG = "FirebaseRepository"
    }

    /**
     * Yazma işlemlerinde oturum açmış kullanıcı UID'sini zorunlu kılar (Fail-Fast).
     */
    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Bu işlem için kullanıcının Firebase oturumu açmış olması gerekir.")
    }

    /**
     * Firestore güvenlik kuralları ve ağ hatalarını yapılandırılmış JSON formatında loglar ve yeniden fırlatır.
     */
    private fun handleFirestoreError(
        exception: Exception,
        operationType: OperationType,
        path: String?
    ) {
        val currentUser = auth.currentUser
        val providerInfoArray = JSONArray().apply {
            currentUser?.providerData?.forEach { profile ->
                put(
                    JSONObject().apply {
                        put("providerId", profile.providerId)
                        put("displayName", profile.displayName)
                        put("email", profile.email)
                        put("phoneNumber", profile.phoneNumber)
                        put("photoUrl", profile.photoUrl?.toString())
                    }
                )
            }
        }
        val authInfo = JSONObject().apply {
            put("userId", currentUser?.uid)
            put("email", currentUser?.email)
            put("emailVerified", currentUser?.isEmailVerified)
            put("isAnonymous", currentUser?.isAnonymous)
            put("tenantId", currentUser?.tenantId)
            put("providerInfo", providerInfoArray)
        }
        val errInfo = JSONObject().apply {
            put("error", exception.message ?: "Unknown Firestore error")
            put("operationType", operationType.value)
            put("path", path)
            put("authInfo", authInfo)
        }
        Log.e("FirestoreError", errInfo.toString(), exception)
        throw exception
    }

    /**
     * Katalog nesnesini `FirestoreCatalogDocument` modeline çevirir ve `/catalogs/{catalogId}` dökümanına kaydeder.
     * Mevcut döküman varsa `createdAt` ve `ownerId` alanlarını korur (`firestore.rules` uyumu).
     */
    suspend fun upsertCatalog(
        catalog: Catalog,
        store: StoreSettings = StoreSettings()
    ): Boolean = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext false
        val docId = sanitizeFirestoreDocId(catalog.id)
        val docPath = "$COLLECTION_CATALOGS/$docId"

        return@withContext try {
            val existingSnap = db.document(docPath).get().await()
            val preservedOwnerId = if (existingSnap.exists()) {
                existingSnap.getString("ownerId")?.takeIf { it.isNotBlank() } ?: uid
            } else {
                uid
            }
            val createdAtValue: Any = if (existingSnap.exists()) {
                existingSnap.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
            } else {
                FieldValue.serverTimestamp()
            }

            val docModel: FirestoreCatalogDocument = catalog.toFirestoreDocument(
                ownerId = preservedOwnerId,
                store = store,
                context = appContext
            )
            val payload = docModel.toFirestorePayloadMap(createdAtValue = createdAtValue)
            db.document(docPath).set(payload).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "upsertCatalog skipped/failed for $docPath: ${e.message}")
            false
        }
    }

    /**
     * Güvenlik kuralları testleri ve zorunlu yazma senaryoları için hata fırlatan katalog kayıt metodu.
     */
    suspend fun saveCatalogStrict(
        catalog: Catalog,
        store: StoreSettings = StoreSettings()
    ): FirestoreCatalogDocument {
        val uid = requireUserId()
        val docId = sanitizeFirestoreDocId(catalog.id)
        val docPath = "$COLLECTION_CATALOGS/$docId"
        val docModel = catalog.toFirestoreDocument(
            ownerId = uid,
            store = store,
            context = appContext
        )
        val payload = docModel.toFirestorePayloadMap(createdAtValue = FieldValue.serverTimestamp())
        try {
            db.document(docPath).set(payload).await()
            return docModel
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            throw e
        }
    }

    /**
     * Ürün nesnesini `FirestoreProductDocument` modeline çevirir ve `/products/{productId}` dökümanına kaydeder.
     * Mevcut döküman varsa `createdAt` ve `ownerId` alanlarını korur (`firestore.rules` uyumu).
     */
    suspend fun upsertProduct(product: Product): Boolean = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext false
        val docId = sanitizeFirestoreDocId(product.id)
        val docPath = "$COLLECTION_PRODUCTS/$docId"

        return@withContext try {
            val existingSnap = db.document(docPath).get().await()
            val preservedOwnerId = if (existingSnap.exists()) {
                existingSnap.getString("ownerId")?.takeIf { it.isNotBlank() } ?: uid
            } else {
                uid
            }
            val createdAtValue: Any = if (existingSnap.exists()) {
                existingSnap.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
            } else {
                FieldValue.serverTimestamp()
            }

            val docModel: FirestoreProductDocument = product.toFirestoreDocument(
                ownerId = preservedOwnerId,
                context = appContext
            )
            val payload = docModel.toFirestorePayloadMap(createdAtValue = createdAtValue)
            db.document(docPath).set(payload).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "upsertProduct skipped/failed for $docPath: ${e.message}")
            false
        }
    }

    /**
     * Güvenlik kuralları testleri ve zorunlu yazma senaryoları için hata fırlatan ürün kayıt metodu.
     */
    suspend fun saveProductStrict(product: Product): FirestoreProductDocument {
        val uid = requireUserId()
        val docId = sanitizeFirestoreDocId(product.id)
        val docPath = "$COLLECTION_PRODUCTS/$docId"
        val docModel = product.toFirestoreDocument(
            ownerId = uid,
            context = appContext
        )
        val payload = docModel.toFirestorePayloadMap(createdAtValue = FieldValue.serverTimestamp())
        try {
            db.document(docPath).set(payload).await()
            return docModel
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            throw e
        }
    }

    /**
     * Katalog dökümanını (`FirestoreCatalogDocument`) ID üzerinden getirir.
     */
    suspend fun getCatalogDocumentById(catalogId: String): FirestoreCatalogDocument? {
        val docId = sanitizeFirestoreDocId(catalogId)
        val docPath = "$COLLECTION_CATALOGS/$docId"
        return try {
            val snap = db.document(docPath).get().await()
            snap.toFirestoreCatalogDocument()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            null
        }
    }

    /**
     * Ürün dökümanını (`FirestoreProductDocument`) ID üzerinden getirir.
     */
    suspend fun getProductDocumentById(productId: String): FirestoreProductDocument? {
        val docId = sanitizeFirestoreDocId(productId)
        val docPath = "$COLLECTION_PRODUCTS/$docId"
        return try {
            val snap = db.document(docPath).get().await()
            snap.toFirestoreProductDocument()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            null
        }
    }

    /**
     * Katalog domain modelini ID üzerinden getirir (güvenlik kuralı hatalarını `handleFirestoreError` ile raporlar).
     */
    suspend fun getPublicCatalogByIdStrict(catalogId: String): Catalog? {
        return getCatalogDocumentById(catalogId)?.toDomainCatalog()
    }

    /**
     * Katalog dökümanını Firestore'dan siler.
     */
    suspend fun deleteCatalog(catalogId: String) = withContext(Dispatchers.IO) {
        if (auth.currentUser == null) return@withContext
        val docId = sanitizeFirestoreDocId(catalogId)
        runCatching {
            db.document("$COLLECTION_CATALOGS/$docId").delete().await()
        }
    }

    /**
     * Ürün dökümanını Firestore'dan siler.
     */
    suspend fun deleteProduct(productId: String) = withContext(Dispatchers.IO) {
        if (auth.currentUser == null) return@withContext
        val docId = sanitizeFirestoreDocId(productId)
        runCatching {
            db.document("$COLLECTION_PRODUCTS/$docId").delete().await()
        }
    }

    /**
     * Oturum açmış kullanıcının kataloglarını gerçek zamanlı `Flow<List<Catalog>>` olarak dinler.
     */
    fun observeUserCatalogs(): Flow<List<Catalog>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = db.collection(COLLECTION_CATALOGS)
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    try {
                        handleFirestoreError(error, OperationType.LIST, COLLECTION_CATALOGS)
                    } catch (t: Throwable) {
                        close(t)
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents
                        .mapNotNull { it.toFirestoreCatalogDocument()?.toDomainCatalog() }
                    trySend(items)
                }
            }
        awaitClose { registration.remove() }
    }

    /**
     * Belirli bir kataloğa ait ürünleri gerçek zamanlı `Flow<List<Product>>` olarak dinler.
     */
    fun observeCatalogProducts(catalogId: String): Flow<List<Product>> = callbackFlow {
        val sanitizedCatId = sanitizeFirestoreDocId(catalogId)
        val registration = db.collection(COLLECTION_PRODUCTS)
            .whereEqualTo("catalogId", sanitizedCatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    try {
                        handleFirestoreError(error, OperationType.LIST, COLLECTION_PRODUCTS)
                    } catch (t: Throwable) {
                        close(t)
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents
                        .mapNotNull { it.toFirestoreProductDocument()?.toDomainProduct() }
                        .sortedBy { it.sortOrder }
                    trySend(items)
                }
            }
        awaitClose { registration.remove() }
    }

    /**
     * Müşteri paylaşım bağlantısı (`/c/{slugOrShortId}`) açıldığında Firestore'dan yayındaki
     * kataloğu ve ürünlerini getirir.
     */
    suspend fun fetchPublicCatalogAndProducts(
        slugOrShortId: String
    ): Pair<Catalog, List<Product>>? = withContext(Dispatchers.IO) {
        val clean = slugOrShortId.trim().removePrefix("/c/").removePrefix("c/").trim('/')
        if (clean.isEmpty()) return@withContext null

        return@withContext try {
            // 1) Doğrudan döküman ID'si ile dene
            val directSnap = runCatching {
                db.collection(COLLECTION_CATALOGS).document(sanitizeFirestoreDocId(clean)).get().await()
            }.getOrNull()

            val matchedDoc: DocumentSnapshot? = if (directSnap != null && directSnap.exists()) {
                directSnap
            } else {
                // 2) Slug eşleşmesi (isActive == true filtreli)
                val bySlug = runCatching {
                    db.collection(COLLECTION_CATALOGS)
                        .whereEqualTo("isActive", true)
                        .whereEqualTo("slug", clean)
                        .limit(1)
                        .get()
                        .await()
                        .documents
                        .firstOrNull()
                }.getOrNull()

                bySlug ?: runCatching {
                    // 3) shortCode eşleşmesi (isActive == true filtreli)
                    db.collection(COLLECTION_CATALOGS)
                        .whereEqualTo("isActive", true)
                        .whereEqualTo("shortCode", clean)
                        .limit(1)
                        .get()
                        .await()
                        .documents
                        .firstOrNull()
                }.getOrNull()
            }

            val catalog = matchedDoc?.toFirestoreCatalogDocument()?.toDomainCatalog()
                ?: return@withContext null

            val productsSnap = runCatching {
                db.collection(COLLECTION_PRODUCTS)
                    .whereEqualTo("catalogId", catalog.id)
                    .get()
                    .await()
            }.getOrNull()

            val products = productsSnap?.documents
                ?.mapNotNull { it.toFirestoreProductDocument()?.toDomainProduct() }
                ?.sortedBy { it.sortOrder }
                .orEmpty()

            catalog.copy(productCount = products.size.coerceAtLeast(catalog.productCount)) to products
        } catch (e: Exception) {
            Log.w(TAG, "fetchPublicCatalogAndProducts failed for $clean: ${e.message}")
            null
        }
    }

    /**
     * MariaDB veya yerel depodan gelen tüm katalog ve ürünleri Firestore'a taşır / senkronize eder.
     */
    suspend fun migrateMariaDbCatalogsAndProducts(
        catalogs: List<Catalog>,
        products: List<Product>,
        store: StoreSettings = StoreSettings()
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        if (auth.currentUser == null) return@withContext 0 to 0
        var migratedCatalogs = 0
        var migratedProducts = 0
        catalogs.forEach { cat ->
            if (upsertCatalog(cat, store)) {
                migratedCatalogs++
            }
        }
        products.forEach { prod ->
            if (upsertProduct(prod)) {
                migratedProducts++
            }
        }
        migratedCatalogs to migratedProducts
    }

    /**
     * Kullanıcı oturum açmışsa katalog ve ürünleri arka planda Firestore ile eşitler.
     */
    suspend fun syncAllIfSignedIn(
        catalogs: List<Catalog>,
        products: List<Product>,
        store: StoreSettings
    ) {
        migrateMariaDbCatalogsAndProducts(catalogs, products, store)
    }
}
