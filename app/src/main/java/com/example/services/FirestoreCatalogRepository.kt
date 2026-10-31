package com.example.services

import android.content.Context
import com.example.R
import com.example.model.Catalog
import com.example.model.FirestoreCatalogDocument
import com.example.model.FirestoreProductDocument
import com.example.model.Product
import com.example.model.StoreSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow

/**
 * Firebase Firestore katalog ve ürün senkronizasyon katmanı.
 * MariaDB'den Firestore'a geçiş sürecinde tüm işlemleri `FirebaseRepository`
 * ve `FirestoreCatalogDocument` / `FirestoreProductDocument` modelleri üzerinden yürütür.
 */
class FirestoreCatalogRepository(
    db: FirebaseFirestore,
    auth: FirebaseAuth = Firebase.auth,
    appContext: Context? = null
) {
    private val firebaseRepository = FirebaseRepository(
        db = db,
        auth = auth,
        appContext = appContext
    )

    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
        appContext = context.applicationContext
    )

    suspend fun upsertCatalog(
        catalog: Catalog,
        store: StoreSettings = StoreSettings()
    ): Boolean = firebaseRepository.upsertCatalog(catalog, store)

    suspend fun saveCatalogStrict(
        catalog: Catalog,
        store: StoreSettings = StoreSettings()
    ): FirestoreCatalogDocument = firebaseRepository.saveCatalogStrict(catalog, store)

    suspend fun upsertProduct(product: Product): Boolean =
        firebaseRepository.upsertProduct(product)

    suspend fun saveProductStrict(product: Product): FirestoreProductDocument =
        firebaseRepository.saveProductStrict(product)

    suspend fun deleteCatalog(catalogId: String) =
        firebaseRepository.deleteCatalog(catalogId)

    suspend fun deleteProduct(productId: String) =
        firebaseRepository.deleteProduct(productId)

    suspend fun getPublicCatalogByIdStrict(catalogId: String): Catalog? =
        firebaseRepository.getPublicCatalogByIdStrict(catalogId)

    suspend fun fetchPublicCatalogAndProducts(
        slugOrShortId: String
    ): Pair<Catalog, List<Product>>? =
        firebaseRepository.fetchPublicCatalogAndProducts(slugOrShortId)

    fun observeUserCatalogs(): Flow<List<Catalog>> =
        firebaseRepository.observeUserCatalogs()

    fun observeCatalogProducts(catalogId: String): Flow<List<Product>> =
        firebaseRepository.observeCatalogProducts(catalogId)

    suspend fun syncAllIfSignedIn(
        catalogs: List<Catalog>,
        products: List<Product>,
        store: StoreSettings
    ) = firebaseRepository.syncAllIfSignedIn(catalogs, products, store)
}
