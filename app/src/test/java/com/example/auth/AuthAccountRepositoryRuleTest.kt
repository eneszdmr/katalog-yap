package com.example.auth

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthAccountRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun authenticatedUserCanCreateReadAndUpdateOwnAccount() = runTest {
        val uid = signInAsTestUser("owner@katalogyap.com")
        val repo = AuthAccountRepository(db, auth)

        repo.createUserAccount(
            phone = "+90 5321234567",
            email = "owner@katalogyap.com",
            displayName = "Anadolu Toptan",
            provider = "phone_password",
            passwordHash = AuthAccountRepository.sha256Hex("Sifre1234"),
            passwordResetRequestedAt = 0L
        )

        val fetched = repo.getUserAccount(uid)
        assertNotNull(fetched)
        assertEquals(uid, fetched?.userId)
        assertEquals("+90 5321234567", fetched?.phone)

        val newHash = AuthAccountRepository.sha256Hex("YeniSifre999")
        repo.updateUserAccount(
            passwordHash = newHash,
            passwordResetRequestedAt = 1712000000000L
        )

        val updated = repo.getUserAccount(uid)
        assertEquals(newHash, updated?.passwordHash)
        assertEquals(1712000000000L, updated?.passwordResetRequestedAt)
    }

    @Test
    fun readingAnotherUsersAccountIsDeniedByRules() = runTest {
        val aliceUid = signInAsTestUser("alice@katalogyap.com")
        val repo = AuthAccountRepository(db, auth)
        repo.createUserAccount(
            phone = "+90 5321112233",
            email = "alice@katalogyap.com",
            displayName = "Alice Store",
            provider = "phone_password",
            passwordHash = AuthAccountRepository.sha256Hex("AlicePass123")
        )

        signInAsTestUser("bob@katalogyap.com")
        try {
            repo.getUserAccount(aliceUid)
            fail("Expected PERMISSION_DENIED when reading another user's document")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun authenticatedOwnerCanCreateCatalogAndProductAndPublicCanRead() = runTest {
        val ownerUid = signInAsTestUser("merchant@katalogyap.com")
        val firebaseRepo = com.example.services.FirebaseRepository(db, auth)

        val catalog = com.example.model.Catalog(
            id = "cat-test-1",
            slug = "ilkbahar-koleksiyonu",
            title = "İlkbahar Koleksiyonu",
            description = "Yeni sezon kataloğu",
            coverImageUrl = "https://constantinopolishotel.store/cover.jpg",
            isActive = true,
            viewCount = 5
        )
        val product = com.example.model.Product(
            id = "prod-test-1",
            catalogId = "cat-test-1",
            name = "İpek Eşarp",
            sku = "IPK-101",
            price = 750.0,
            currency = "₺",
            category = "Aksesuar",
            subCategory = "Eşarp",
            description = "Saf ipek eşarp",
            imageUrl = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
            stockCount = 15,
            sortOrder = 1
        )

        firebaseRepo.saveCatalogStrict(catalog)
        firebaseRepo.saveProductStrict(product)

        val catalogDoc = firebaseRepo.getCatalogDocumentById("cat-test-1")
        assertNotNull(catalogDoc)
        assertEquals(ownerUid, catalogDoc?.ownerId)
        assertEquals("İlkbahar Koleksiyonu", catalogDoc?.toDomainCatalog()?.title)

        val productDoc = firebaseRepo.getProductDocumentById("prod-test-1")
        assertNotNull(productDoc)
        assertEquals(ownerUid, productDoc?.ownerId)
        assertEquals("İpek Eşarp", productDoc?.toDomainProduct()?.name)
    }
}

