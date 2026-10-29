package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val KatalogYapLightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = AccentAmber,
    onSecondary = TextPrimary,
    secondaryContainer = AccentAmberLight,
    onSecondaryContainer = WarningAmberDark,
    tertiary = SuccessGreen,
    onTertiary = SurfaceWhite,
    background = BackgroundSlate,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    outlineVariant = BorderStrong,
    error = DangerRed,
    onError = SurfaceWhite,
    errorContainer = DangerRedLight,
    onErrorContainer = DangerRedDark
)

@Composable
fun KatalogYapTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KatalogYapLightColorScheme,
        typography = KatalogYapTypography,
        shapes = KatalogYapShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    KatalogYapTheme(content = content)
}
