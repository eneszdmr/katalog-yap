package com.example.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.AuthProvider
import com.example.model.InventoryFilter
import com.example.services.LocalStorageDataService
import com.example.ui.components.BottomTabDestination
import com.example.ui.components.KYBottomTabBar
import com.example.ui.components.KYErrorBoundary
import com.example.ui.components.KYToastData
import com.example.ui.components.KYToastHost
import com.example.ui.components.MobileFrameContainer
import com.example.ui.components.NotFound404Screen
import com.example.ui.components.OfflineCacheStatusBanner
import com.example.ui.screens.AnalyticsDraftScreen
import com.example.ui.screens.CatalogsDraftScreen
import com.example.ui.screens.DesignSystemScreen
import com.example.ui.screens.OrdersDraftScreen
import com.example.ui.screens.SettingsDraftScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.OnboardingScreen
import com.example.ui.screens.auth.SplashScreen
import com.example.ui.screens.auth.VerifyOtpScreen
import com.example.ui.screens.catalogs.CatalogDetailScreen
import com.example.ui.screens.catalogs.ProductDetailScreen
import com.example.ui.screens.catalogs.ProductFormScreen
import com.example.ui.screens.inventory.InventoryScreen
import com.example.ui.screens.orders.OrderDetailScreen
import com.example.ui.screens.publiccatalog.PublicCatalogScreen
import com.example.ui.screens.resellers.ResellersScreen
import com.example.ui.theme.BackgroundSlate
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.OrderNotificationHelper
import com.example.util.PwaOfflineAndShortcutHelper
import kotlinx.coroutines.delay

object AppRoutes {
    // Kimlik Doğrulama & Kurulum Rotaları
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val VERIFY = "verify"
    const val ONBOARDING = "onboarding"

    // Herkese Açık Müşteri Katalog Rotası (/c/:shortId - Giriş gerekmez, alt sekme çubuğu yoktur)
    const val PUBLIC_CATALOG = "c/{shortId}"

    // 404 Sayfa Bulunamadı Rotası (/404)
    const val NOT_FOUND = "not_found?path={path}"

    // Korumalı Ana Uygulama Rotaları
    const val CATALOGS = "catalogs"
    const val CATALOG_DETAIL = "catalogues/{catalogId}"
    const val PRODUCT_DETAIL = "products/{productId}"
    const val PRODUCT_NEW = "products/new?catalogId={catalogId}"
    const val PRODUCT_EDIT = "products/{productId}/edit"
    const val ORDERS = "orders"
    const val ORDER_DETAIL = "orders/{orderId}"
    const val ANALYTICS = "analytics"
    const val SETTINGS = "settings"
    const val RESELLERS = "resellers"
    const val INVENTORY = "inventory?filter={filter}&import={import}"
    const val DESIGN_SYSTEM = "design_system"

    fun publicCatalogRoute(shortId: String) = "c/$shortId"
    fun notFoundRoute(path: String = "/bilinmeyen-sayfa") = "not_found?path=$path"
    fun catalogDetailRoute(catalogId: String) = "catalogues/$catalogId"
    fun productDetailRoute(productId: String) = "products/$productId"
    fun productNewRoute(catalogId: String = "") = "products/new?catalogId=$catalogId"
    fun productEditRoute(productId: String) = "products/$productId/edit"
    fun orderDetailRoute(orderId: String) = "orders/$orderId"
    fun inventoryRoute(
        filter: InventoryFilter = InventoryFilter.ALL,
        openImport: Boolean = false
    ) = "inventory?filter=${filter.name}&import=$openImport"

    val protectedRoutes = setOf(
        CATALOGS,
        CATALOG_DETAIL,
        PRODUCT_DETAIL,
        PRODUCT_NEW,
        PRODUCT_EDIT,
        ORDERS,
        ORDER_DETAIL,
        ANALYTICS,
        SETTINGS,
        RESELLERS,
        INVENTORY,
        DESIGN_SYSTEM
    )
}

/**
 * Uygulama Ana Router Yapısı:
 * - KYErrorBoundary (Hata Sınırı)
 * - AuthProvider + Korumalı Rotalar + 404 Sayfası
 * - Hafif Slide + Fade Sayfa Geçiş Animasyonları
 * - Offline Önbellek Durum Çubuğu (PWA Offline Cache)
 */
@Composable
fun AppRouter() {
    val context = LocalContext.current
    val dataService = remember { LocalStorageDataService.getInstance(context) }
    val catalogs by dataService.catalogs.collectAsState()
    val products by dataService.products.collectAsState()
    val latestOrderAlert by dataService.latestOrderAlert.collectAsState()

    var isOfflineBannerVisible by remember {
        mutableStateOf(!PwaOfflineAndShortcutHelper.isNetworkAvailable(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        OrderNotificationHelper.ensureChannel(context)
    }

    AuthProvider(dataService = dataService) { authState ->
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        var activeToast by remember { mutableStateOf<KYToastData?>(null) }

        LaunchedEffect(authState.isAuthenticated, authState.isOnboardingCompleted) {
            if (authState.isAuthenticated && authState.isOnboardingCompleted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        runCatching {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
            }
        }

        LaunchedEffect(latestOrderAlert?.id) {
            if (latestOrderAlert != null) {
                delay(8000L)
                dataService.dismissLatestOrderAlert()
            }
        }

        LaunchedEffect(activeToast) {
            if (activeToast != null) {
                delay(3000L)
                activeToast = null
            }
        }

        // Korumalı Sayfa Denetimi (Route Guard):
        // Giriş yapmamış kullanıcı korumalı sayfalara veya onboarding'e giremesin.
        // Not: PUBLIC_CATALOG ("c/{shortId}") ve NOT_FOUND protectedRoutes içinde DEĞİLDİR.
        LaunchedEffect(currentRoute, authState.isAuthenticated, authState.isOnboardingCompleted) {
            if (currentRoute == null ||
                currentRoute == AppRoutes.SPLASH ||
                currentRoute == AppRoutes.PUBLIC_CATALOG ||
                currentRoute == AppRoutes.NOT_FOUND
            ) {
                return@LaunchedEffect
            }

            if (!authState.isAuthenticated &&
                (currentRoute in AppRoutes.protectedRoutes || currentRoute == AppRoutes.ONBOARDING)
            ) {
                navController.navigate(AppRoutes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            } else if (authState.isAuthenticated &&
                !authState.isOnboardingCompleted &&
                currentRoute in AppRoutes.protectedRoutes
            ) {
                navController.navigate(AppRoutes.ONBOARDING) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        val showBottomBar = authState.isAuthenticated &&
            authState.isOnboardingCompleted &&
            currentRoute in listOf(
                AppRoutes.CATALOGS,
                AppRoutes.ORDERS,
                AppRoutes.ANALYTICS,
                AppRoutes.SETTINGS
            )

        KYErrorBoundary(
            onResetToHome = {
                navController.navigate(
                    if (authState.isAuthenticated) AppRoutes.CATALOGS else AppRoutes.LOGIN
                ) {
                    popUpTo(0) { inclusive = true }
                }
            }
        ) {
            MobileFrameContainer {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                ) {
                    Scaffold(
                        containerColor = BackgroundSlate,
                        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                        bottomBar = {
                            if (showBottomBar) {
                                KYBottomTabBar(
                                    currentRoute = currentRoute,
                                    onTabSelected = { tab: BottomTabDestination ->
                                        if (currentRoute != tab.route) {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            OfflineCacheStatusBanner(
                                isOffline = isOfflineBannerVisible,
                                cachedCatalogCount = catalogs.size,
                                cachedProductCount = products.size
                            )

                            AnimatedVisibility(visible = latestOrderAlert != null && authState.isAuthenticated) {
                                val alertOrder = latestOrderAlert
                                if (alertOrder != null) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = SurfaceWhite,
                                        border = BorderStroke(1.5.dp, SuccessGreen),
                                        shadowElevation = 6.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .clickable {
                                                dataService.dismissLatestOrderAlert()
                                                navController.navigate(AppRoutes.orderDetailRoute(alertOrder.id))
                                            }
                                            .testTag("new_order_notification_banner")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(SuccessGreenLight),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.NotificationsActive,
                                                        contentDescription = "Yeni Sipariş Bildirimi",
                                                        tint = SuccessGreen,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Yeni Sipariş Geldi! (${alertOrder.orderNumber})",
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = TextPrimary
                                                    )
                                                    Text(
                                                        text = "${alertOrder.customerName} • ${alertOrder.totalAmount.toInt()} ${alertOrder.currency} • Görüntülemek için dokunun",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = { dataService.dismissLatestOrderAlert() },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = "Bildirimi Kapat",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            NavHost(
                                navController = navController,
                                startDestination = AppRoutes.SPLASH,
                                enterTransition = {
                                    slideInHorizontally(
                                        initialOffsetX = { fullWidth -> (fullWidth * 0.14f).toInt() },
                                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(220))
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        targetOffsetX = { fullWidth -> -(fullWidth * 0.08f).toInt() },
                                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                                    ) + fadeOut(animationSpec = tween(180))
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        initialOffsetX = { fullWidth -> -(fullWidth * 0.14f).toInt() },
                                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(220))
                                },
                                popExitTransition = {
                                    slideOutHorizontally(
                                        targetOffsetX = { fullWidth -> (fullWidth * 0.14f).toInt() },
                                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                                    ) + fadeOut(animationSpec = tween(180))
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f)
                            ) {
                                // 1) SPLASH
                                composable(AppRoutes.SPLASH) {
                                    SplashScreen(
                                        onNavigateToLogin = {
                                            navController.navigate(AppRoutes.LOGIN) {
                                                popUpTo(AppRoutes.SPLASH) { inclusive = true }
                                            }
                                        },
                                        onNavigateToOnboarding = {
                                            navController.navigate(AppRoutes.ONBOARDING) {
                                                popUpTo(AppRoutes.SPLASH) { inclusive = true }
                                            }
                                        },
                                        onNavigateToHome = {
                                            navController.navigate(AppRoutes.CATALOGS) {
                                                popUpTo(AppRoutes.SPLASH) { inclusive = true }
                                            }
                                        }
                                    )
                                }

                                // 2) LOGIN (/login)
                                composable(AppRoutes.LOGIN) {
                                    LoginScreen(
                                        onNavigateToVerify = {
                                            navController.navigate(AppRoutes.VERIFY)
                                        },
                                        onNavigateToOnboarding = {
                                            navController.navigate(AppRoutes.ONBOARDING) {
                                                popUpTo(AppRoutes.LOGIN) { inclusive = true }
                                            }
                                        },
                                        onNavigateToHome = {
                                            navController.navigate(AppRoutes.CATALOGS) {
                                                popUpTo(AppRoutes.LOGIN) { inclusive = true }
                                            }
                                        },
                                        onShowToast = { toast -> activeToast = toast },
                                        onOpenPublicCatalog = { shortId ->
                                            navController.navigate(AppRoutes.publicCatalogRoute(shortId))
                                        }
                                    )
                                }

                                // 3) OTP VERIFY (/verify)
                                composable(AppRoutes.VERIFY) {
                                    VerifyOtpScreen(
                                        onBackToLogin = { navController.popBackStack() },
                                        onNavigateToOnboarding = {
                                            navController.navigate(AppRoutes.ONBOARDING) {
                                                popUpTo(AppRoutes.LOGIN) { inclusive = true }
                                            }
                                        },
                                        onNavigateToHome = {
                                            navController.navigate(AppRoutes.CATALOGS) {
                                                popUpTo(AppRoutes.LOGIN) { inclusive = true }
                                            }
                                        },
                                        onShowToast = { toast -> activeToast = toast }
                                    )
                                }

                                // 4) ONBOARDING (/onboarding)
                                composable(AppRoutes.ONBOARDING) {
                                    OnboardingScreen(
                                        onOnboardingFinished = {
                                            navController.navigate(AppRoutes.CATALOGS) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        },
                                        onShowToast = { toast -> activeToast = toast }
                                    )
                                }

                                // HERKESE AÇIK MÜŞTERİ KATALOG SAYFASI (/c/:shortId)
                                // Giriş gerekmez, alt sekme çubuğu (BottomTabBar) gösterilmez.
                                composable(
                                    route = AppRoutes.PUBLIC_CATALOG,
                                    arguments = listOf(
                                        navArgument("shortId") { type = NavType.StringType }
                                    )
                                ) { backStackEntry ->
                                    val shortId = backStackEntry.arguments?.getString("shortId").orEmpty()
                                    PublicCatalogScreen(
                                        shortId = shortId,
                                        onBackClick = if (navController.previousBackStackEntry != null) {
                                            { navController.popBackStack() }
                                        } else {
                                            {
                                                navController.navigate(
                                                    if (authState.isAuthenticated) AppRoutes.CATALOGS else AppRoutes.LOGIN
                                                )
                                            }
                                        },
                                        onShowToast = { toast -> activeToast = toast }
                                    )
                                }

                                // 404 SAYFA BULUNAMADI EKRANI
                                composable(
                                    route = AppRoutes.NOT_FOUND,
                                    arguments = listOf(
                                        navArgument("path") {
                                            type = NavType.StringType
                                            defaultValue = "/404"
                                        }
                                    )
                                ) { backStackEntry ->
                                    val requestedPath = backStackEntry.arguments?.getString("path").orEmpty()
                                    NotFound404Screen(
                                        requestedPath = requestedPath,
                                        onBackClick = if (navController.previousBackStackEntry != null) {
                                            { navController.popBackStack() }
                                        } else null,
                                        onGoHomeClick = {
                                            navController.navigate(
                                                if (authState.isAuthenticated) AppRoutes.CATALOGS else AppRoutes.LOGIN
                                            ) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    )
                                }

                                // KORUMALI ANA SAYFALAR
                                composable(AppRoutes.CATALOGS) {
                                    if (authState.isAuthenticated && authState.isOnboardingCompleted) {
                                        CatalogsDraftScreen(
                                            onCatalogClick = { catalogId ->
                                                navController.navigate(AppRoutes.catalogDetailRoute(catalogId))
                                            },
                                            onOpenDesignSystem = {
                                                navController.navigate(AppRoutes.DESIGN_SYSTEM)
                                            },
                                            onShowToast = { toast -> activeToast = toast },
                                            onOpenPublicCatalog = { shortId ->
                                                navController.navigate(AppRoutes.publicCatalogRoute(shortId))
                                            },
                                            onOpenInventory = { filter ->
                                                navController.navigate(AppRoutes.inventoryRoute(filter = filter))
                                            }
                                        )
                                    }
                                }

                                // KATALOG DETAY (/catalogues/:id)
                                composable(
                                    route = AppRoutes.CATALOG_DETAIL,
                                    arguments = listOf(
                                        navArgument("catalogId") { type = NavType.StringType }
                                    )
                                ) { backStackEntry ->
                                    val catalogId = backStackEntry.arguments?.getString("catalogId").orEmpty()
                                    if (authState.isAuthenticated && authState.isOnboardingCompleted) {
                                        CatalogDetailScreen(
                                            catalogId = catalogId,
                                            onBackClick = { navController.popBackStack() },
                                            onProductClick = { productId ->
                                                navController.navigate(AppRoutes.productDetailRoute(productId))
                                            },
                                            onAddProductClick = {
                                                navController.navigate(AppRoutes.productNewRoute(catalogId))
                                            },
                                            onShowToast = { toast -> activeToast = toast },
                                            onOpenPublicCatalog = { shortId ->
                                                navController.navigate(AppRoutes.publicCatalogRoute(shortId))
                                            }
                                        )
                                    }
                                }

                                // YENİ ÜRÜN EKLE (/products/new)
                                composable(
                                    route = AppRoutes.PRODUCT_NEW,
                                    arguments = listOf(
                                        navArgument("catalogId") {
                                            type = NavType.StringType
                                            defaultValue = ""
                                        }
                                    )
                                ) { backStackEntry ->
                                    val catalogId = backStackEntry.arguments?.getString("catalogId").orEmpty()
                                    if (authState.isAuthenticated && authState.isOnboardingCompleted) {
                                        ProductFormScreen(
                                            productId = null,
                                            initialCatalogId = catalogId,
                                            onBackClick = { navController.popBackStack() },
                                            onSavedSuccess = { _ ->
                                                navController.popBackStack()
                                            },
                                            onShowToast = { toast -> activeToast = toast }
                                        )
                                    }
                                }

                                // ÜRÜN DÜZENLE (/products/:id/edit)
                                composable(
                                    route = AppRoutes.PRODUCT_EDIT,
                                    arguments = listOf(
                                        navArgument("productId") { type = NavType.StringType }
                                    )
                                ) { backStackEntry ->
                                    val productId = backStackEntry.arguments?.getString("productId").orEmpty()
                                    if (authState.isAuthenticated && authState.isOnboardingCompleted) {
                                        ProductFormScreen(
                                            productId = productId,
                                            initialCatalogId = null,
                                            onBackClick = { navController.popBackStack() },
                                            onSavedSuccess = { _ ->
                                                navController.popBackStack()
                                            },
                                            onShowToast = { toast -> activeToast = toast }
                                        )
                                    }
                                }

                                // ÜRÜN DETAY (/products/:id)
                                composable(
                                    route = AppRoutes.PRODUCT_DETAIL,
                                    arguments = listOf(
                                        navArgument("productId") { type = NavType.StringType }
                                    )
                                ) { backStackEntry ->
                                    val productId = backStackEntry.arguments?.getString("productId").orEmpty()
                                    if (authState.isAuthenticated && authState.isOnboardingCompleted) {
                                        ProductDetailScreen(
                                            productId = productId,
                                            onBackClick = { navController.popBackStack() },
                                            onEditProductClick = { editId ->
                                                navController.navigate(AppRoutes.productEditRoute(editId))
                                            },
                                            onShowToast = { toast -> activeToast = toast },
                                            onOpenPublicCatalog = { shortId ->
                                                navController.navigate(AppRoutes.publicCatalogRoute(shortId))
                                            }
                                        )
                                    }
                                }

                                // SİPARİŞLER SEKMESİ (/orders)
                                composable(AppRoutes.ORDERS) {
                                    if (authState.isAuthenticated && authState.isOnboardingCompleted) {
                                        OrdersDraftScreen(
                                            onOrderClick = { orderId ->
                                                navController.navigate(AppRoutes.orderDetailRoute(orderId))
                                            },
                                            onOpenDesignSystem = {
                                                navController.navigate(AppRoutes.DESIGN_SYSTEM)
                                            },
                                            onShowToast = { toast -> activeToast = toast }
                                        )
                                    }
                                }

                                // SİPARİŞ DETAYI (/orders/:id)
                                composable(
                                    route = AppRoutes.ORDER_DETAIL,
                                    arguments = listOf(
                                        navArgument("orderId") { type = NavType.StringType }
                                    )
                                ) { backStackEntry ->
                                    val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
                                    if (authState.isAuthenticated && authState.isOnboardingCompleted) {
                                        OrderDetailScreen(
                                            orderId = orderId,
                                            onBackClick = { navController.popBackStack() },
                                            onShowToast = { toast -> activeToast = toast }
                                        )
                                    }
                                }

                                composable(AppRoutes.ANALYTICS) {
                                    if (authState.isAuthenticated && authState.isOnboardingCompleted) {
                                        AnalyticsDraftScreen(
                                            onOpenDesignSystem = {
                                                navController.navigate(AppRoutes.DESIGN_SYSTEM)
                                            },
                                            onProductClick = { productId ->
                                                navController.navigate(AppRoutes.productDetailRoute(productId))
                                            },
                                            onShowToast = { toast -> activeToast = toast }
                                        )
                                    }
                                }

                                composable(AppRoutes.SETTINGS) {
                                    if (authState.isAuthenticated && authState.isOnboardingCompleted) {
                                        SettingsDraftScreen(
                                            onOpenDesignSystem = {
                                                navController.navigate(AppRoutes.DESIGN_SYSTEM)
                                            },
                                            onOpenInventory = { filter, openImport ->
                                                navController.navigate(
                                                    AppRoutes.inventoryRoute(
                                                        filter = filter,
                                                        openImport = openImport
                                                    )
                                                )
                                            },
                                            onOpenResellers = {
                                                navController.navigate(AppRoutes.RESELLERS)
                                            },
                                            onOpenPublicCatalog = { shortId ->
                                                navController.navigate(AppRoutes.publicCatalogRoute(shortId))
                                            },
                                            onOpenNotFound404 = {
                                                navController.navigate(AppRoutes.notFoundRoute("/c/bulunamayan-katalog"))
                                            },
                                            isOfflinePreviewActive = isOfflineBannerVisible,
                                            onToggleOfflinePreview = {
                                                isOfflineBannerVisible = !isOfflineBannerVisible
                                            },
                                            onLoggedOut = {
                                                navController.navigate(AppRoutes.LOGIN) {
                                                    popUpTo(0) { inclusive = true }
                                                }
                                            },
                                            onShowToast = { toast -> activeToast = toast }
                                        )
                                    }
                                }

                                // BAYİLER (RESELLERS) YÖNETİMİ EKRANI (/resellers)
                                composable(AppRoutes.RESELLERS) {
                                    if (authState.isAuthenticated && authState.isOnboardingCompleted) {
                                        ResellersScreen(
                                            onBackClick = { navController.popBackStack() },
                                            onOpenPublicCatalog = { shortId ->
                                                navController.navigate(AppRoutes.publicCatalogRoute(shortId))
                                            },
                                            onShowToast = { toast -> activeToast = toast }
                                        )
                                    }
                                }

                                // STOK YÖNETİMİ EKRANI (/inventory)
                                composable(
                                    route = AppRoutes.INVENTORY,
                                    arguments = listOf(
                                        navArgument("filter") {
                                            type = NavType.StringType
                                            defaultValue = InventoryFilter.ALL.name
                                        },
                                        navArgument("import") {
                                            type = NavType.BoolType
                                            defaultValue = false
                                        }
                                    )
                                ) { backStackEntry ->
                                    val filterArg = backStackEntry.arguments?.getString("filter").orEmpty()
                                    val openImport = backStackEntry.arguments?.getBoolean("import") ?: false
                                    val initialFilter = runCatching { InventoryFilter.valueOf(filterArg) }
                                        .getOrDefault(InventoryFilter.ALL)

                                    if (authState.isAuthenticated && authState.isOnboardingCompleted) {
                                        InventoryScreen(
                                            initialFilter = initialFilter,
                                            openImportOnLaunch = openImport,
                                            onBackClick = { navController.popBackStack() },
                                            onProductClick = { productId ->
                                                navController.navigate(AppRoutes.productDetailRoute(productId))
                                            },
                                            onShowToast = { toast -> activeToast = toast }
                                        )
                                    }
                                }

                                composable(AppRoutes.DESIGN_SYSTEM) {
                                    if (authState.isAuthenticated && authState.isOnboardingCompleted) {
                                        DesignSystemScreen(
                                            onBackClick = { navController.popBackStack() },
                                            onShowToast = { toast -> activeToast = toast }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    KYToastHost(
                        toastData = activeToast,
                        onDismiss = { activeToast = null },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                    )
                }
            }
        }
    }
}
