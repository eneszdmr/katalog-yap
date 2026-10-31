package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.navigation.AppRouter
import com.example.services.LocalStorageDataService
import com.example.services.configureAppCheck
import com.example.ui.theme.KatalogYapTheme
import com.example.util.PwaOfflineAndShortcutHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or Firebase calls
        runCatching {
            configureAppCheck(this, intent)
        }.onFailure { err ->
            android.util.Log.e("MainActivity", "App Check initialization bypassed: ${err.message}")
        }

        // STEP 2: Initialize local data layer & offline image cache (Lazy Loading + 50MB Disk Cache)
        runCatching {
            LocalStorageDataService.getInstance(applicationContext)
            PwaOfflineAndShortcutHelper.configureCachedImageLoader(applicationContext)
        }.onFailure { err ->
            android.util.Log.e("MainActivity", "Data service initialization warning: ${err.message}")
        }

        enableEdgeToEdge()
        setContent {
            KatalogYapTheme {
                AppRouter()
            }
        }
    }
}
