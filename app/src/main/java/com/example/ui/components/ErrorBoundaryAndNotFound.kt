package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExploreOff
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LockPerson
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentAmberDark
import com.example.ui.theme.AccentAmberLight
import com.example.ui.theme.BackgroundSlate
import com.example.ui.theme.BadgeRoundedPill
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Ekranların veya asenkron işlemlerin kritik bir hatayı ErrorBoundary'ye bildirmesi için CompositionLocal.
 */
class ErrorBoundaryController(
    private val onReportError: (Throwable) -> Unit,
    private val onClearError: () -> Unit
) {
    fun reportError(error: Throwable) = onReportError(error)
    fun clearError() = onClearError()
}

val LocalErrorBoundary = staticCompositionLocalOf {
    ErrorBoundaryController(onReportError = {}, onClearError = {})
}

/**
 * 5) HATA SINIRLARI (ErrorBoundary):
 * Uygulama genelinde beklenmeyen çalışma zamanı hatalarını yakalayıp
 * uygulamanın çökmesini önleyen ve kullanıcıya "Yeniden Yükle / Ana Ekrana Dön"
 * seçenekleri sunan sarmalayıcı bileşen.
 */
@Composable
fun KYErrorBoundary(
    onResetToHome: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var caughtError by remember { mutableStateOf<Throwable?>(null) }

    val controller = remember {
        ErrorBoundaryController(
            onReportError = { throwable -> caughtError = throwable },
            onClearError = { caughtError = null }
        )
    }

    // Beklenmeyen Coroutine / Thread hatalarını yakalamak için güvenli işleyici
    DisposableEffect(Unit) {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Ölümcül olmayan UI/State hatalarında kurtarma ekranını göster, aksi halde varsayılana ilet
            if (throwable is IllegalStateException || throwable is IllegalArgumentException) {
                caughtError = throwable
            } else {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
        onDispose {
            Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        }
    }

    CompositionLocalProvider(LocalErrorBoundary provides controller) {
        val error = caughtError
        if (error != null) {
            ErrorFallbackScreen(
                errorMessage = error.localizedMessage ?: "Beklenmeyen bir uygulama hatası oluştu.",
                onRetry = { caughtError = null },
                onGoHome = {
                    caughtError = null
                    onResetToHome()
                }
            )
        } else {
            content()
        }
    }
}

@Composable
fun ErrorFallbackScreen(
    errorMessage: String,
    onRetry: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundSlate)
            .statusBarsPadding()
            .padding(24.dp)
            .testTag("error_boundary_fallback_screen")
            .semantics { contentDescription = "Beklenmeyen Hata Ekranı" },
        contentAlignment = Alignment.Center
    ) {
        KYCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(DangerRedLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = "Hata İkonu",
                        tint = DangerRed,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = BadgeRoundedPill,
                    color = DangerRedLight,
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Hata Sınırı (Error Boundary)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DangerRed,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Bir Şeyler Ters Gitti",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                KYButton(
                    text = "Tekrar Dene",
                    onClick = onRetry,
                    variant = ButtonVariant.PRIMARY,
                    leadingIcon = Icons.Outlined.Refresh,
                    modifier = Modifier.fillMaxWidth(),
                    tag = "btn_error_boundary_retry"
                )

                Spacer(modifier = Modifier.height(10.dp))

                KYButton(
                    text = "Ana Ekrana Dön",
                    onClick = onGoHome,
                    variant = ButtonVariant.SECONDARY,
                    leadingIcon = Icons.Outlined.Home,
                    modifier = Modifier.fillMaxWidth(),
                    tag = "btn_error_boundary_home"
                )
            }
        }
    }
}

/**
 * 5) 404 SAYFA BULUNAMADI / YAYINDA OLMAYAN KATALOG EKRANI:
 * Geçersiz bir rota (/404), silinmiş bir kayıt veya yayında olmayan (isActive = false)
 * bir müşteri kataloğu ziyaret edildiğinde gösterilen 404 ekranı.
 */
@Composable
fun NotFound404Screen(
    requestedPath: String = "",
    title: String = "404 — Sayfa veya Katalog Bulunamadı",
    description: String = "Aradığınız sayfa taşınmış, silinmiş veya bu katalog şu anda yayında değil.",
    isUnpublishedCatalog: Boolean = false,
    onGoHomeClick: () -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundSlate)
            .statusBarsPadding()
            .padding(24.dp)
            .testTag("screen_404_not_found")
            .semantics { contentDescription = "404 Sayfa Bulunamadı Ekranı" },
        contentAlignment = Alignment.Center
    ) {
        KYCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(if (isUnpublishedCatalog) AccentAmberLight else PrimaryBlueSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUnpublishedCatalog) Icons.Outlined.LockPerson else Icons.Outlined.ExploreOff,
                        contentDescription = null,
                        tint = if (isUnpublishedCatalog) AccentAmberDark else PrimaryBlue,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = BadgeRoundedPill,
                    color = if (isUnpublishedCatalog) AccentAmberLight else PrimaryBlueSubtle,
                    border = BorderStroke(
                        1.dp,
                        if (isUnpublishedCatalog) AccentAmberDark.copy(alpha = 0.3f) else PrimaryBlue.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (isUnpublishedCatalog) "YAYINDA DEĞİL • GİZLİ KATALOG" else "HATA KODU: 404",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isUnpublishedCatalog) AccentAmberDark else PrimaryBlue,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                if (requestedPath.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = ButtonRoundedXl,
                        color = BackgroundSlate,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Text(
                            text = "İstenen Adres: $requestedPath",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                KYButton(
                    text = "Ana Ekrana Dön",
                    onClick = onGoHomeClick,
                    variant = ButtonVariant.PRIMARY,
                    leadingIcon = Icons.Outlined.Home,
                    modifier = Modifier.fillMaxWidth(),
                    tag = "btn_404_go_home"
                )

                if (onBackClick != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    KYButton(
                        text = "Önceki Sayfaya Dön",
                        onClick = onBackClick,
                        variant = ButtonVariant.SECONDARY,
                        leadingIcon = Icons.AutoMirrored.Outlined.ArrowBack,
                        modifier = Modifier.fillMaxWidth(),
                        tag = "btn_404_go_back"
                    )
                }
            }
        }
    }
}

/**
 * 3) ÇEVRİMDIŞI (OFFLINE) ÖNBELLEK BİLGİ ÇUBUĞU:
 * İnternet bağlantısı olmadığında yerel önbellekten (Offline Cache) çalışıldığını gösterir.
 */
@Composable
fun OfflineCacheStatusBanner(
    isOffline: Boolean,
    cachedCatalogCount: Int,
    cachedProductCount: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOffline,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = AccentAmberLight,
            border = BorderStroke(1.dp, AccentAmberDark.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("offline_cache_banner")
                .semantics {
                    contentDescription = "Çevrimdışı mod aktif. Veriler yerel önbellekten gösteriliyor."
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = null,
                    tint = AccentAmberDark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Çevrimdışı Önbellek Modu: $cachedCatalogCount katalog ve $cachedProductCount ürün cihaz önbelleğinden sunuluyor.",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = AccentAmberDark,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
