package com.example.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.MainActivity
import com.example.R

/**
 * PWA / "Ana Ekrana Ekle" (Pinned Shortcut) ve Çevrimdışı Önbellek (Coil Disk & Memory Cache +
 * Bağlantı Kontrolü) Yardımcı Sınıfı.
 */
object PwaOfflineAndShortcutHelper {

    /**
     * Cihazın aktif internet bağlantısı olup olmadığını kontrol eder.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return true
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    /**
     * "Ana Ekrana Ekle" (PWA Add-to-Home-Screen eşdeğeri Android Pinned Shortcut) isteği başlatır.
     * Launcher destekliyorsa sistem sabitleme diyaloğunu açar ve true döner.
     */
    fun requestAddToHomeScreen(
        context: Context,
        title: String = "Katalog Yap",
        shortId: String? = null
    ): Boolean {
        return try {
            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                return false
            }

            val shortcutId = if (shortId.isNullOrBlank()) {
                "pwa_home_katalogyap"
            } else {
                "pwa_catalog_${InputSanitizer.sanitizeSlugOrShortId(shortId)}"
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                if (!shortId.isNullOrBlank()) {
                    putExtra("extra_short_id", shortId)
                }
            }

            val cleanTitle = InputSanitizer.sanitizeForStorage(title, maxLength = 25)
                .ifBlank { "Katalog Yap" }

            val pinShortcutInfo = ShortcutInfoCompat.Builder(context, shortcutId)
                .setShortLabel(cleanTitle)
                .setLongLabel("$cleanTitle • Dijital Katalog")
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(launchIntent)
                .build()

            ShortcutManagerCompat.requestPinShortcut(context, pinShortcutInfo, null)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Görseller için Lazy Loading, Crossfade ve Çevrimdışı Disk/Bellek Önbelleğini (Coil) yapılandırır.
     */
    fun configureCachedImageLoader(context: Context) {
        try {
            val imageLoader = ImageLoader.Builder(context)
                .crossfade(true)
                .crossfade(220)
                .memoryCache {
                    MemoryCache.Builder(context)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.cacheDir.resolve("katalogyap_offline_image_cache"))
                        .maxSizeBytes(50L * 1024L * 1024L) // 50 MB offline görsel önbelleği
                        .build()
                }
                .respectCacheHeaders(false)
                .build()

            Coil.setImageLoader(imageLoader)
        } catch (_: Exception) {
            // Varsayılan yükleyici ile devam et
        }
    }
}
