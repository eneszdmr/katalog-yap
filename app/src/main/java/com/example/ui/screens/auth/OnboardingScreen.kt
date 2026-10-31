package com.example.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.auth.LocalAuthContext
import com.example.i18n.TrStrings
import com.example.model.BusinessType
import com.example.model.CatalogLanguage
import com.example.model.CurrencyOption
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYButton
import com.example.ui.components.KYCard
import com.example.ui.components.KYInput
import com.example.ui.components.KYToastData
import com.example.ui.components.KYToastType
import com.example.ui.components.KYTopBar
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.BackgroundSlate
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ImageCompressionUtil

private val quickCities = listOf("İstanbul", "Ankara", "İzmir", "Bursa", "Antalya", "Gaziantep")

/**
 * 4) İŞLETME KURULUMU (/onboarding) - 3 Adımlı Stepper:
 * - Adım 1: İşletme/marka adı, işletme türü (Giyim, Elektronik, Ev & Yaşam, Gıda, Kozmetik, Diğer)
 * - Adım 2: Logo yükleme (dairesel önizleme), WhatsApp numarası, şehir
 * - Adım 3: Para birimi (TRY, USD, EUR), katalog dili, "Kataloğunu Oluştur" butonu
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingFinished: () -> Unit,
    onShowToast: (KYToastData) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authContext = LocalAuthContext.current
    val existingSettings = authContext.storeSettings

    var currentStep by remember { mutableIntStateOf(1) }

    // Adım 1 State
    var businessName by remember { mutableStateOf(existingSettings.businessName) }
    var selectedBusinessType by remember {
        mutableStateOf(
            BusinessType.entries.find { it.labelTr == existingSettings.businessType }
                ?: BusinessType.CLOTHING
        )
    }
    var businessNameError by remember { mutableStateOf<String?>(null) }

    // Adım 2 State
    var logoUri by remember { mutableStateOf(existingSettings.logoUri) }
    var whatsappNumber by remember {
        mutableStateOf(
            existingSettings.phoneWhatsapp.ifBlank {
                authContext.formattedPendingPhone.ifBlank { "+90 555 000 00 00" }
            }
        )
    }
    var city by remember { mutableStateOf(existingSettings.city) }
    var whatsappError by remember { mutableStateOf<String?>(null) }
    var cityError by remember { mutableStateOf<String?>(null) }

    // Adım 3 State
    var selectedCurrency by remember {
        mutableStateOf(
            CurrencyOption.entries.find { it.code == existingSettings.currencyCode }
                ?: CurrencyOption.TRY
        )
    }
    var selectedLanguage by remember {
        mutableStateOf(
            CatalogLanguage.entries.find { it.labelTr == existingSettings.catalogLanguage }
                ?: CatalogLanguage.TR
        )
    }

    // Android Zero-Permission Photo Picker (Logoyu kalıcı dosya olarak kaydeder)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val phoneDigits = whatsappNumber.filter { it.isDigit() }.takeLast(10)
            val accountKey = if (phoneDigits.length >= 10) "store_$phoneDigits" else "store_default"
            logoUri = ImageCompressionUtil.persistStoreLogo(context, uri.toString(), accountKey)
        }
    }

    BackHandler(enabled = currentStep > 1) {
        currentStep -= 1
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundSlate)
            .testTag("screen_onboarding")
    ) {
        KYTopBar(
            title = TrStrings.Auth.ONBOARDING_TOP_TITLE,
            subtitle = "Adım $currentStep / 3",
            onBackClick = if (currentStep > 1) {
                { currentStep -= 1 }
            } else null
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 3 Adımlı Stepper Göstergesi
            OnboardingStepperHeader(currentStep = currentStep)

            when (currentStep) {
                1 -> {
                    KYCard {
                        Text(
                            text = TrStrings.Auth.STEP_1_TITLE,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = TrStrings.Auth.STEP_1_SUBTITLE,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        KYInput(
                            value = businessName,
                            onValueChange = {
                                businessName = it
                                if (businessNameError != null) businessNameError = null
                            },
                            label = TrStrings.Auth.BUSINESS_NAME_LABEL,
                            placeholder = TrStrings.Auth.BUSINESS_NAME_PLACEHOLDER,
                            errorText = businessNameError,
                            leadingIcon = Icons.Outlined.Storefront,
                            tag = "input_business_name"
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = TrStrings.Auth.BUSINESS_TYPE_LABEL,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            BusinessType.entries.chunked(2).forEach { rowTypes ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowTypes.forEach { type ->
                                        val isSelected = selectedBusinessType == type
                                        Surface(
                                            onClick = { selectedBusinessType = type },
                                            shape = ButtonRoundedXl,
                                            color = if (isSelected) PrimaryBlueSubtle else SurfaceWhite,
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) PrimaryBlue else BorderLight
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("business_type_${type.name}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = type.labelTr,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (isSelected) PrimaryBlue else TextPrimary
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.CheckCircle,
                                                        contentDescription = null,
                                                        tint = PrimaryBlue,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        KYButton(
                            text = TrStrings.Common.NEXT,
                            onClick = {
                                if (businessName.isBlank()) {
                                    businessNameError = TrStrings.Auth.ERROR_BUSINESS_NAME
                                } else {
                                    currentStep = 2
                                }
                            },
                            trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                            modifier = Modifier.fillMaxWidth(),
                            tag = "btn_onboarding_step1_next"
                        )
                    }
                }

                2 -> {
                    KYCard {
                        Text(
                            text = TrStrings.Auth.STEP_2_TITLE,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = TrStrings.Auth.STEP_2_SUBTITLE,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Dairesel Logo Önizleme ve Yükleme Alanı
                        Text(
                            text = TrStrings.Auth.LOGO_UPLOAD_LABEL,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val initials = businessName
                                .trim()
                                .split(" ")
                                .filter { it.isNotBlank() }
                                .take(2)
                                .joinToString("") { it.first().uppercase() }
                                .ifBlank { "KY" }

                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (logoUri.isNotBlank()) SurfaceMuted else PrimaryBlueSubtle
                                    )
                                    .border(2.dp, PrimaryBlue, CircleShape)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("onboarding_logo_circle"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (logoUri.isNotBlank()) {
                                    AsyncImage(
                                        model = logoUri,
                                        contentDescription = "Mağaza Logosu",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = PrimaryBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (logoUri.isNotBlank()) {
                                        "Logo seçildi ($initials)"
                                    } else {
                                        TrStrings.Auth.LOGO_HELPER
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    KYButton(
                                        text = TrStrings.Auth.LOGO_PICK_GALLERY,
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        variant = ButtonVariant.SECONDARY,
                                        leadingIcon = Icons.Outlined.AddPhotoAlternate,
                                        tag = "btn_pick_logo"
                                    )
                                    if (logoUri.isNotBlank()) {
                                        KYButton(
                                            text = TrStrings.Auth.LOGO_REMOVE,
                                            onClick = { logoUri = "" },
                                            variant = ButtonVariant.GHOST
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // WhatsApp Numarası
                        KYInput(
                            value = whatsappNumber,
                            onValueChange = {
                                whatsappNumber = it
                                if (whatsappError != null) whatsappError = null
                            },
                            label = TrStrings.Auth.WHATSAPP_LABEL,
                            placeholder = TrStrings.Auth.WHATSAPP_PLACEHOLDER,
                            errorText = whatsappError,
                            leadingIcon = Icons.Outlined.Phone,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            tag = "input_whatsapp_number"
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Şehir
                        KYInput(
                            value = city,
                            onValueChange = {
                                city = it
                                if (cityError != null) cityError = null
                            },
                            label = TrStrings.Auth.CITY_LABEL,
                            placeholder = TrStrings.Auth.CITY_PLACEHOLDER,
                            errorText = cityError,
                            leadingIcon = Icons.Outlined.LocationCity,
                            tag = "input_city"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Hızlı Şehir Seçimi Chip'leri
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickCities.forEach { sampleCity ->
                                val isSelected = city.equals(sampleCity, ignoreCase = true)
                                KYBadge(
                                    text = sampleCity,
                                    variant = if (isSelected) BadgeVariant.PRIMARY else BadgeVariant.NEUTRAL,
                                    modifier = Modifier.clickable {
                                        city = sampleCity
                                        cityError = null
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KYButton(
                                text = TrStrings.Common.PREVIOUS,
                                onClick = { currentStep = 1 },
                                variant = ButtonVariant.SECONDARY,
                                modifier = Modifier.weight(1f),
                                tag = "btn_onboarding_step2_back"
                            )
                            KYButton(
                                text = TrStrings.Common.NEXT,
                                onClick = {
                                    val digits = whatsappNumber.filter { it.isDigit() }
                                    if (digits.length < 10) {
                                        whatsappError = TrStrings.Auth.ERROR_WHATSAPP
                                    } else if (city.isBlank()) {
                                        cityError = TrStrings.Auth.ERROR_CITY
                                    } else {
                                        currentStep = 3
                                    }
                                },
                                variant = ButtonVariant.PRIMARY,
                                trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                                modifier = Modifier.weight(1.4f),
                                tag = "btn_onboarding_step2_next"
                            )
                        }
                    }
                }

                3 -> {
                    KYCard {
                        Text(
                            text = TrStrings.Auth.STEP_3_TITLE,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = TrStrings.Auth.STEP_3_SUBTITLE,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Para Birimi Seçimi (TRY, USD, EUR)
                        Text(
                            text = TrStrings.Auth.CURRENCY_LABEL,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CurrencyOption.entries.forEach { currency ->
                                val isSelected = selectedCurrency == currency
                                Surface(
                                    onClick = { selectedCurrency = currency },
                                    shape = ButtonRoundedXl,
                                    color = if (isSelected) PrimaryBlueSubtle else SurfaceWhite,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) PrimaryBlue else BorderLight
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("currency_option_${currency.code}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = currency.labelTr,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isSelected) PrimaryBlue else TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Katalog Dili Seçimi
                        Text(
                            text = TrStrings.Auth.LANGUAGE_LABEL,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CatalogLanguage.entries.forEach { lang ->
                                val isSelected = selectedLanguage == lang
                                Surface(
                                    onClick = { selectedLanguage = lang },
                                    shape = ButtonRoundedXl,
                                    color = if (isSelected) PrimaryBlueSubtle else SurfaceWhite,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) PrimaryBlue else BorderLight
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("language_option_${lang.code}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = lang.labelTr,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isSelected) PrimaryBlue else TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KYButton(
                                text = TrStrings.Common.PREVIOUS,
                                onClick = { currentStep = 2 },
                                variant = ButtonVariant.SECONDARY,
                                modifier = Modifier.weight(1f),
                                tag = "btn_onboarding_step3_back"
                            )
                            KYButton(
                                text = TrStrings.Auth.FINISH_ONBOARDING_BUTTON,
                                onClick = {
                                    authContext.completeOnboarding(
                                        businessName = businessName,
                                        businessType = selectedBusinessType.labelTr,
                                        logoUri = logoUri,
                                        whatsappNumber = whatsappNumber,
                                        city = city,
                                        currencyCode = selectedCurrency.code,
                                        currencySymbol = selectedCurrency.symbol,
                                        catalogLanguage = selectedLanguage.labelTr,
                                        onComplete = {
                                            onShowToast(
                                                KYToastData(
                                                    message = TrStrings.Auth.ONBOARDING_COMPLETED_TOAST,
                                                    type = KYToastType.SUCCESS
                                                )
                                            )
                                            onOnboardingFinished()
                                        }
                                    )
                                },
                                variant = ButtonVariant.PRIMARY,
                                leadingIcon = Icons.Outlined.RocketLaunch,
                                modifier = Modifier.weight(1.6f),
                                tag = "btn_finish_onboarding"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepperHeader(
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        1 to "Marka",
        2 to "İletişim",
        3 to "Bölge"
    )

    KYCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, (stepNumber, stepTitle) ->
                val isCompleted = stepNumber < currentStep
                val isCurrent = stepNumber == currentStep

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCompleted -> SuccessGreen
                                    isCurrent -> PrimaryBlue
                                    else -> SurfaceMuted
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = SurfaceWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = stepNumber.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isCurrent) SurfaceWhite else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = stepTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = when {
                            isCurrent -> PrimaryBlue
                            isCompleted -> TextPrimary
                            else -> TextSecondary
                        }
                    )
                }

                if (index < steps.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .height(2.dp)
                            .background(
                                if (stepNumber < currentStep) SuccessGreen else BorderLight
                            )
                    )
                }
            }
        }
    }
}
