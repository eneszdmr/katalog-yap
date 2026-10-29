package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.services.LocalStorageDataService
import org.junit.Assert.assertEquals
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
    fun `read string from context and verify DataService seeded catalogs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Katalog Yap", appName)

        val dataService = LocalStorageDataService.getInstance(context)
        assertNotNull(dataService)
        assertEquals(4, dataService.catalogs.value.size)
        assertTrue(dataService.products.value.size >= 32)
    }
}
