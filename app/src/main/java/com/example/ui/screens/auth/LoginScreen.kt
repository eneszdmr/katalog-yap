package com.example.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.auth.FirebaseGoogleAuthHelper
import com.example.auth.LocalAuthContext
import com.example.i18n.TrStrings
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.KYBadge
import com.example.ui.components.KYBottomSheet
import com.example.ui.components.KYButton
import com.example.ui.components.KYCard
import com.example.ui.components.KYInput
import com.example.ui.components.KYModal
import com.example.ui.components.KYToastData
import com.example.ui.components.KYToastType
import com.example.ui.theme.BackgroundSlate
import com.example.ui.theme.BorderLight
import com.example.ui.theme.ButtonRoundedXl
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueSubtle
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class CountryCodeOption(
    val code: String,
    val countryName: String,
    val flagEmoji: String
)

private val countryCodeOptions = listOf(
    CountryCodeOption("+90", "Türkiye", "🇹🇷"),
    CountryCodeOption("+994", "Azerbaycan", "🇦🇿"),
    CountryCodeOption("+49", "Almanya", "🇩🇪"),
    CountryCodeOption("+44", "Birleşik Krallık", "🇬🇧"),
    CountryCodeOption("+31", "Hollanda", "🇳🇱"),
    CountryCodeOption("+1", "Amerika Birleşik Devletleri", "🇺🇸")
)

/**
 * GİRİŞ EKRANI (/login):
 * - Telefon numarası ve şifre belirleme / giriş yapma (OTP gerekmeden doğrudan giriş)
 * - Google ile Giriş Yap (Jetpack Credential Manager + Firebase Auth)
 * - Firebase bağlantılı Şifre Sıfırlama (Firebase Auth e-posta sıfırlama + yeni şifre belirleme)
 */
@Composable
fun LoginScreen(
    onNavigateToVerify: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
    onShowToast: (KYToastData) -> Unit,
    onOpenPublicCatalog: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val authContext = LocalAuthContext.current
    val context = LocalContext.current

    var selectedCountry by remember {
        mutableStateOf(
            countryCodeOptions.find { it.code == authContext.pendingCountryCode }
                ?: countryCodeOptions.first()
        )
    }
    var phoneNumber by remember { mutableStateOf(authContext.pendingPhoneNumber) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSetPasswordMode by remember { mutableStateOf(false) }
    var recoveryEmail by remember { mutableStateOf("") }

    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    // Firebase Şifre Sıfırlama BottomSheet durumu
    var showPasswordResetSheet by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetPhone by remember { mutableStateOf("") }
    var resetNewPassword by remember { mutableStateOf("") }
    var resetPasswordVisible by remember { mutableStateOf(false) }
    var isResetSubmitting by remember { mutableStateOf(false) }

    var showCountryPicker by remember { mutableStateOf(false) }
    var activeLegalModal by remember { mutableStateOf<String?>(null) }

    // Kayıtlı Google hesabı varsa sessiz otomatik kontrol
    LaunchedEffect(Unit) {
        if (!authContext.isAuthenticated) {
            FirebaseGoogleAuthHelper.attemptAutoSignIn(context)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundSlate)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .testTag("screen_login"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Logo ve Marka Başlığı
        KatalogYapBrandLogo(sizeDp = 68)
        Spacer(modifier = Modifier.height(12.dp))
        KYBadge(
            text = TrStrings.APP_NAME,
            variant = BadgeVariant.PRIMARY
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = TrStrings.Auth.LOGIN_HEADLINE,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Telefon numaranız ve şifrenizle hemen giriş yapın veya Google hesabınızla bağlanın.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Giriş & Şifre Belirleme Kartı
        KYCard {
            // Mod Seçici Sekmeler: Giriş Yap / Şifre Belirle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ButtonRoundedXl)
                    .background(BackgroundSlate)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabs = listOf(
                    false to "Giriş Yap",
                    true to "Şifre Belirle / Kayıt"
                )
                tabs.forEach { (modeValue, label) ->
                    val selected = isSetPasswordMode == modeValue
                    Surface(
                        onClick = {
                            isSetPasswordMode = modeValue
                            phoneError = null
                            passwordError = null
                        },
                        shape = ButtonRoundedXl,
                        color = if (selected) SurfaceWhite else BackgroundSlate,
                        shadowElevation = if (selected) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag(if (modeValue) "tab_set_password" else "tab_login_password")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) PrimaryBlue else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Telefon Numarası Başlığı ve Alanı
            Text(
                text = TrStrings.Auth.PHONE_LABEL,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = { showCountryPicker = true },
                    shape = ButtonRoundedXl,
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 56.dp)
                        .testTag("country_code_selector")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedCountry.flagEmoji} ${selectedCountry.code}",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = TrStrings.Auth.COUNTRY_CODE_TITLE,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                KYInput(
                    value = phoneNumber,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == ' ' }.take(15)
                        phoneNumber = filtered
                        if (phoneError != null) phoneError = null
                    },
                    placeholder = TrStrings.Auth.PHONE_PLACEHOLDER,
                    errorText = phoneError,
                    leadingIcon = Icons.Outlined.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f),
                    tag = "input_phone_number"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Şifre Alanı Başlığı + Şifremi Unuttum
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSetPasswordMode) "Yeni Şifre Belirle" else "Şifreniz",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Şifremi Unuttum (Firebase)",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(ButtonRoundedXl)
                        .clickable {
                            resetPhone = phoneNumber
                            resetEmail = authContext.getSavedRecoveryEmailForPhone(
                                selectedCountry.code,
                                phoneNumber
                            ).ifBlank { recoveryEmail }
                            showPasswordResetSheet = true
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("btn_forgot_password_firebase")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it.take(64)
                    if (passwordError != null) passwordError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .testTag("input_password"),
                singleLine = true,
                isError = !passwordError.isNullOrBlank(),
                shape = ButtonRoundedXl,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                placeholder = {
                    Text(
                        text = if (isSetPasswordMode) {
                            "En az 6 karakterli şifre belirleyin"
                        } else {
                            "Şifrenizi girin (ilk girişte otomatik tanımlanır)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = if (!passwordError.isNullOrBlank()) DangerRed else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.testTag("btn_toggle_password_visibility")
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = if (passwordVisible) "Şifreyi gizle" else "Şifreyi göster",
                            tint = TextSecondary
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderLight,
                    errorBorderColor = DangerRed,
                    cursorColor = PrimaryBlue
                )
            )

            if (!passwordError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = passwordError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = DangerRed
                )
            }

            // Şifre belirleme modunda isteğe bağlı Firebase kurtarma e-postası
            if (isSetPasswordMode) {
                Spacer(modifier = Modifier.height(12.dp))
                KYInput(
                    value = recoveryEmail,
                    onValueChange = { recoveryEmail = it.trim() },
                    label = "Kurtarma E-postası (Firebase Şifre Sıfırlama İçin - Opsiyonel)",
                    placeholder = "ornek@isletme.com",
                    leadingIcon = Icons.Outlined.Email,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    tag = "input_recovery_email"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ana Giriş / Şifre Kaydet Butonu
            KYButton(
                text = if (isSetPasswordMode) {
                    "Şifreyi Kaydet ve Giriş Yap"
                } else {
                    "Telefon ve Şifre ile Giriş Yap"
                },
                onClick = {
                    val digitsOnly = phoneNumber.filter { it.isDigit() }
                    if (digitsOnly.length < 10) {
                        phoneError = TrStrings.Auth.ERROR_INVALID_PHONE
                        return@KYButton
                    }
                    if (password.length < 6) {
                        passwordError = "Şifreniz en az 6 karakter olmalıdır."
                        return@KYButton
                    }

                    authContext.loginOrSetPhonePassword(
                        countryCode = selectedCountry.code,
                        phoneNumber = phoneNumber,
                        passwordPlain = password,
                        recoveryEmail = recoveryEmail,
                        isRegisterOrSetPasswordMode = isSetPasswordMode,
                        onSuccess = { needsOnboarding, isNewPasswordSet ->
                            onShowToast(
                                KYToastData(
                                    message = if (isNewPasswordSet) {
                                        "Şifreniz tanımlandı ve giriş yapıldı"
                                    } else {
                                        "Başarıyla giriş yapıldı"
                                    },
                                    type = KYToastType.SUCCESS
                                )
                            )
                            if (needsOnboarding) onNavigateToOnboarding() else onNavigateToHome()
                        },
                        onError = { errMsg ->
                            passwordError = errMsg
                            onShowToast(
                                KYToastData(
                                    message = errMsg,
                                    type = KYToastType.ERROR
                                )
                            )
                        }
                    )
                },
                variant = ButtonVariant.PRIMARY,
                trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                modifier = Modifier.fillMaxWidth(),
                tag = "btn_login_continue"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Ayırıcı
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = BorderLight)
                Text(
                    text = TrStrings.Auth.OR_DIVIDER,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = BorderLight)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google ile Giriş Yap (Gerçek Credential Manager + Firebase Auth)
            KYButton(
                text = if (isGoogleLoading) {
                    "Google Hesabı Bağlanıyor..."
                } else {
                    "Google ile Giriş Yap"
                },
                onClick = {
                    if (isGoogleLoading) return@KYButton
                    isGoogleLoading = true
                    authContext.loginWithGoogleFirebase(
                        activityContext = context,
                        onSuccess = { needsOnboarding, displayName ->
                            isGoogleLoading = false
                            onShowToast(
                                KYToastData(
                                    message = "$displayName olarak Google ile giriş yapıldı",
                                    type = KYToastType.SUCCESS
                                )
                            )
                            if (needsOnboarding) onNavigateToOnboarding() else onNavigateToHome()
                        },
                        onError = { errMsg ->
                            isGoogleLoading = false
                            onShowToast(
                                KYToastData(
                                    message = errMsg,
                                    type = KYToastType.ERROR
                                )
                            )
                        }
                    )
                },
                variant = ButtonVariant.SECONDARY,
                leadingIcon = Icons.Outlined.Public,
                modifier = Modifier.fillMaxWidth(),
                tag = "btn_login_google"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Şifremi Unuttum / Firebase Şifre Sıfırlama Hızlı Erişim Butonu
            KYButton(
                text = "Şifremi Unuttum / Firebase ile Şifre Sıfırla",
                onClick = {
                    resetPhone = phoneNumber
                    resetEmail = authContext.getSavedRecoveryEmailForPhone(
                        selectedCountry.code,
                        phoneNumber
                    ).ifBlank { recoveryEmail }
                    showPasswordResetSheet = true
                },
                variant = ButtonVariant.GHOST,
                leadingIcon = Icons.Outlined.LockReset,
                modifier = Modifier.fillMaxWidth(),
                tag = "btn_open_firebase_reset_sheet"
            )


        }

        Spacer(modifier = Modifier.height(20.dp))

        // Kullanım Koşulları ve Gizlilik Politikası Linkleri
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val annotatedNotice = buildAnnotatedString {
                append(TrStrings.Auth.LEGAL_PREFIX)
                withStyle(
                    SpanStyle(
                        color = PrimaryBlue,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(TrStrings.Auth.TERMS_LINK)
                }
                append(TrStrings.Auth.LEGAL_AND)
                withStyle(
                    SpanStyle(
                        color = PrimaryBlue,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(TrStrings.Auth.PRIVACY_LINK)
                }
                append(TrStrings.Auth.LEGAL_SUFFIX)
            }

            Text(
                text = annotatedNotice,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = TrStrings.Auth.TERMS_LINK,
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryBlue,
                    modifier = Modifier
                        .clip(ButtonRoundedXl)
                        .clickable { activeLegalModal = "terms" }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("link_terms")
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = TrStrings.Auth.PRIVACY_LINK,
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryBlue,
                    modifier = Modifier
                        .clip(ButtonRoundedXl)
                        .clickable { activeLegalModal = "privacy" }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("link_privacy")
                )
            }
        }
    }

    // Firebase Şifre Sıfırlama BottomSheet
    KYBottomSheet(
        isVisible = showPasswordResetSheet,
        title = "Firebase ile Şifre Sıfırlama",
        onDismiss = { showPasswordResetSheet = false }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Firebase Auth üzerinden kurtarma e-postanıza sıfırlama bağlantısı gönderebilir ve telefon numaranız için yeni şifrenizi hemen tanımlayabilirsiniz.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            KYInput(
                value = resetPhone,
                onValueChange = { input ->
                    resetPhone = input.filter { it.isDigit() || it == ' ' }.take(15)
                },
                label = "Telefon Numaranız (${selectedCountry.code})",
                placeholder = TrStrings.Auth.PHONE_PLACEHOLDER,
                leadingIcon = Icons.Outlined.Phone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                tag = "input_reset_phone"
            )

            KYInput(
                value = resetEmail,
                onValueChange = { resetEmail = it.trim() },
                label = "Firebase Kurtarma E-postası",
                placeholder = "ornek@isletme.com",
                leadingIcon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                tag = "input_reset_email"
            )

            Text(
                text = "Yeni Şifre Belirle (İsteğe Bağlı)",
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )

            OutlinedTextField(
                value = resetNewPassword,
                onValueChange = { resetNewPassword = it.take(64) },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .testTag("input_reset_new_password"),
                singleLine = true,
                shape = ButtonRoundedXl,
                visualTransformation = if (resetPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                placeholder = {
                    Text(
                        text = "Yeni şifreniz (en az 6 karakter)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { resetPasswordVisible = !resetPasswordVisible }) {
                        Icon(
                            imageVector = if (resetPasswordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = if (resetPasswordVisible) "Şifreyi gizle" else "Şifreyi göster",
                            tint = TextSecondary
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderLight,
                    cursorColor = PrimaryBlue
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            KYButton(
                text = if (isResetSubmitting) {
                    "Firebase ile İşleniyor..."
                } else {
                    "Firebase ile Şifreyi Sıfırla"
                },
                onClick = {
                    if (isResetSubmitting) return@KYButton
                    isResetSubmitting = true
                    authContext.resetPasswordWithFirebase(
                        countryCode = selectedCountry.code,
                        phoneNumber = resetPhone,
                        recoveryEmail = resetEmail,
                        newPasswordPlain = resetNewPassword.takeIf { it.isNotBlank() }
                    ) { success, message ->
                        isResetSubmitting = false
                        onShowToast(
                            KYToastData(
                                message = message,
                                type = if (success) KYToastType.SUCCESS else KYToastType.ERROR
                            )
                        )
                        if (success) {
                            if (resetPhone.isNotBlank()) phoneNumber = resetPhone
                            if (resetNewPassword.isNotBlank()) password = resetNewPassword
                            resetNewPassword = ""
                            showPasswordResetSheet = false
                        }
                    }
                },
                variant = ButtonVariant.PRIMARY,
                leadingIcon = Icons.Outlined.LockReset,
                modifier = Modifier.fillMaxWidth(),
                tag = "btn_submit_firebase_password_reset"
            )
        }
    }

    // Ülke Kodu Seçici Alt Çekmecesi
    KYBottomSheet(
        isVisible = showCountryPicker,
        title = TrStrings.Auth.COUNTRY_CODE_TITLE,
        onDismiss = { showCountryPicker = false }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            countryCodeOptions.forEach { option ->
                val isSelected = option.code == selectedCountry.code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ButtonRoundedXl)
                        .background(if (isSelected) PrimaryBlueSubtle else SurfaceWhite)
                        .clickable {
                            selectedCountry = option
                            showCountryPicker = false
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${option.flagEmoji}  ${option.countryName} (${option.code})",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) PrimaryBlue else TextPrimary
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

    // Kullanım Koşulları & Gizlilik Politikası Modalı
    KYModal(
        isVisible = activeLegalModal != null,
        title = if (activeLegalModal == "terms") {
            TrStrings.Auth.TERMS_MODAL_TITLE
        } else {
            TrStrings.Auth.PRIVACY_MODAL_TITLE
        },
        description = if (activeLegalModal == "terms") {
            TrStrings.Auth.TERMS_MODAL_BODY
        } else {
            TrStrings.Auth.PRIVACY_MODAL_BODY
        },
        confirmText = TrStrings.Common.CLOSE,
        cancelText = TrStrings.Common.CANCEL,
        onConfirm = { activeLegalModal = null },
        onDismiss = { activeLegalModal = null }
    )
}
