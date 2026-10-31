package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Order
import com.example.model.OrderItem
import com.example.services.LocalStorageDataService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context and verify single sample catalog and multi-user store isolation`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Katalog Yap", appName)

        val dataService = LocalStorageDataService.getInstance(context)
        assertNotNull(dataService)
        assertEquals(1, dataService.catalogs.value.size)
        assertEquals("Test Deneme Kataloğu", dataService.catalogs.value.first().title)
        assertEquals(4, dataService.products.value.size)

        val rand1 = (1000000..4999999).random()
        val rand2 = (5000000..9999999).random()
        val phone1 = "532$rand1"
        val phone2 = "544$rand2"

        // 1. Yeni telefon numarası ile giriş -> yeni kullanıcı / yeni mağaza (needsOnboarding = true)
        val (_, needsOnboardingPhone1) = dataService.activateUserAccountSession(
            countryCode = "+90",
            phoneNumber = phone1,
            passwordHash = "hash1",
            recoveryEmail = "test1@ornek.com",
            provider = "phone_password"
        )
        assertTrue(needsOnboardingPhone1)
        assertEquals("", dataService.settings.value.businessName)
        assertEquals(1, dataService.catalogs.value.size)
        assertEquals("Test Deneme Kataloğu", dataService.catalogs.value.first().title)

        // Mağaza bilgilerini ve logoyu kaydet
        dataService.updateSettings(
            dataService.settings.value.copy(
                businessName = "Anadolu Seramik Atölyesi",
                logoUri = "https://picsum.photos/seed/anadolu-logo/300/300",
                city = "İzmir"
            )
        )
        assertEquals("Anadolu Seramik Atölyesi", dataService.settings.value.businessName)

        // Çıkış yap ve tamamen farklı bir telefon numarası ile giriş yap -> yeni mağaza gelmeli!
        dataService.clearAuthSession()
        val (_, needsOnboardingPhone2) = dataService.activateUserAccountSession(
            countryCode = "+90",
            phoneNumber = phone2,
            passwordHash = "hash2",
            recoveryEmail = "test2@ornek.com",
            provider = "phone_password"
        )
        assertTrue(needsOnboardingPhone2)
        assertEquals("", dataService.settings.value.businessName)
        assertEquals(1, dataService.catalogs.value.size)

        // Tekrar 1. telefon numarası ile giriş yap -> kayıtlı mağaza adı ve logosu eksiksiz geri gelmeli!
        dataService.clearAuthSession()
        val (_, needsOnboardingPhone1Again) = dataService.activateUserAccountSession(
            countryCode = "+90",
            phoneNumber = phone1,
            passwordHash = "hash1",
            recoveryEmail = "test1@ornek.com",
            provider = "phone_password"
        )
        assertFalse(needsOnboardingPhone1Again)
        assertEquals("Anadolu Seramik Atölyesi", dataService.settings.value.businessName)
        assertEquals("https://picsum.photos/seed/anadolu-logo/300/300", dataService.settings.value.logoUri)

        // Müşteri siparişi oluşturulduğunda mağaza sahibine bildirim tetiklenmeli
        val testCatalog = dataService.catalogs.value.first()
        val testProduct = dataService.products.value.first()
        dataService.createCustomerOrder(
            Order(
                id = "order-test-9901",
                orderNumber = "#KY-9901",
                catalogId = testCatalog.id,
                catalogTitle = testCatalog.title,
                customerName = "Zeynep Kaya",
                customerPhone = "+90 555 444 33 22",
                customerAddress = "Kadıköy / İstanbul",
                items = listOf(
                    OrderItem(
                        productId = testProduct.id,
                        productName = testProduct.name,
                        quantity = 1,
                        unitPrice = testProduct.price
                    )
                ),
                totalAmount = testProduct.price
            )
        )
        assertNotNull(dataService.latestOrderAlert.value)
        assertEquals("Zeynep Kaya", dataService.latestOrderAlert.value?.customerName)
    }
}
