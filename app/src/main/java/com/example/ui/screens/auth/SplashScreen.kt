package com.example.ui.screens.auth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.auth.LocalAuthContext
import com.example.i18n.TrStrings
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueDark
import com.example.ui.theme.SurfaceWhite
import kotlinx.coroutines.delay

/**
 * 1) SPLASH EKRANI:
 * Logo + uygulama adı ("KATALOG YAP"), 1.5 sn sonra giriş durumuna göre yönlendirir.
 */
@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authContext = LocalAuthContext.current
    var startAnimation by remember { mutableStateOf(false) }

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.85f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "splash_alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1500L)
        when {
            !authContext.isAuthenticated -> onNavigateToLogin()
            !authContext.isOnboardingCompleted -> onNavigateToOnboarding()
            else -> onNavigateToHome()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(PrimaryBlue, PrimaryBlueDark)
                )
            )
            .padding(24.dp)
            .testTag("screen_splash"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .scale(scaleAnim)
                .alpha(alphaAnim),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            KatalogYapBrandLogo(sizeDp = 92)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = TrStrings.APP_NAME,
                style = MaterialTheme.typography.displayLarge,
                color = SurfaceWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = TrStrings.Auth.SPLASH_SUBTITLE,
                style = MaterialTheme.typography.bodyMedium,
                color = SurfaceWhite.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = AccentAmber,
                strokeWidth = 2.5.dp
            )
        }
    }
}

/**
 * Katalog Yap Ortak Marka Logosu (Katalog Kitabı + Kehribar Etiket Rozeti).
 */
@Composable
fun KatalogYapBrandLogo(
    sizeDp: Int = 72,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape((sizeDp * 0.28f).dp),
            color = SurfaceWhite,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = TrStrings.APP_NAME,
                    tint = PrimaryBlue,
                    modifier = Modifier.size((sizeDp * 0.52f).dp)
                )
            }
        }

        Surface(
            modifier = Modifier
                .size((sizeDp * 0.36f).dp)
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp),
            shape = CircleShape,
            color = AccentAmber,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.LocalOffer,
                    contentDescription = null,
                    tint = SurfaceWhite,
                    modifier = Modifier.size((sizeDp * 0.2f).dp)
                )
            }
        }
    }
}
