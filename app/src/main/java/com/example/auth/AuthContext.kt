package com.example.auth

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.example.model.AuthUser
import com.example.model.PhonePasswordCredential
import com.example.model.StoreSettings
import com.example.services.DataService
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Kimlik doğrulama ve işletme kurulum durumunu merkezi olarak yöneten AuthContext.
 * - Telefon Numarası + Şifre ile doğrudan giriş ve şifre belirleme
 * - Jetpack Credential Manager ile gerçek Google Girişi (Firebase Auth + Firestore senkronizasyonu)
 * - Firebase bağlantılı şifre sıfırlama (`sendPasswordResetEmail` + Firestore `/users/{userId}`)
 */
class AuthContextState(
    val currentUser: AuthUser?,
    val storeSettings: StoreSettings,
    val pendingCountryCode: String,
    val pendingPhoneNumber: String,
    private val appContext: Context,
    private val dataService: DataService,
    private val scope: CoroutineScope,
    private val onUpdatePendingPhone: (String, String) -> Unit
) {
    val isAuthenticated: Boolean
        get() = currentUser != null

    val isOnboardingCompleted: Boolean
        get() = currentUser?.isOnboardingCompleted == true

    val formattedPendingPhone: String
        get() = "$pendingCountryCode $pendingPhoneNumber".trim()

    fun hasPasswordForPhone(countryCode: String, phoneNumber: String): Boolean {
        val fullPhone = "$countryCode ${phoneNumber.trim()}"
        return dataService.getPhoneCredential(fullPhone) != null
    }

    fun getSavedRecoveryEmailForPhone(countryCode: String, phoneNumber: String): String {
        val fullPhone = "$countryCode ${phoneNumber.trim()}"
        return dataService.getPhoneCredential(fullPhone)?.recoveryEmail.orEmpty()
    }

    /**
     * Telefon numarası ve şifre ile giriş yapar veya yeni şifre belirler (OTP gerekmeden).
     */
    fun loginOrSetPhonePassword(
        countryCode: String,
        phoneNumber: String,
        passwordPlain: String,
        recoveryEmail: String = "",
        isRegisterOrSetPasswordMode: Boolean = false,
        onSuccess: (needsOnboarding: Boolean, isNewPasswordSet: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanPhone = phoneNumber.trim()
        val digits = cleanPhone.filter { it.isDigit() }
        if (digits.length < 10) {
            onError("Lütfen geçerli bir 10 haneli telefon numarası girin.")
            return
        }
        if (passwordPlain.length < 6) {
            onError("Şifreniz en az 6 karakter olmalıdır.")
            return
        }

        val fullPhone = "$countryCode $cleanPhone"
        val incomingHash = AuthAccountRepository.sha256Hex(passwordPlain)

        onUpdatePendingPhone(countryCode, cleanPhone)

        scope.launch {
            val existingCred = dataService.getPhoneCredential(fullPhone)
            if (!isRegisterOrSetPasswordMode && existingCred != null && existingCred.passwordHash != incomingHash) {
                onError("Girdiğiniz şifre bu telefon numarası için hatalı. Şifrenizi unuttuysanız sıfırlayabilirsiniz.")
                return@launch
            }

            val isSettingPasswordNow = isRegisterOrSetPasswordMode || existingCred == null
            val resolvedEmail = recoveryEmail.trim().ifBlank { existingCred?.recoveryEmail.orEmpty() }
            val firebaseUid = runCatching { Firebase.auth.currentUser?.uid.orEmpty() }.getOrDefault("")

            dataService.savePhoneCredential(
                PhonePasswordCredential(
                    fullPhone = fullPhone,
                    phoneCountryCode = countryCode,
                    phoneNumber = cleanPhone,
                    passwordHash = incomingHash,
                    recoveryEmail = resolvedEmail,
                    updatedAt = System.currentTimeMillis()
                )
            )

            // Bu telefon numarasına özel mağaza ve katalog oturumunu yükle veya yeni mağaza olarak başlat!
            val (activatedUser, needsOnboarding) = dataService.activateUserAccountSession(
                countryCode = countryCode,
                phoneNumber = cleanPhone,
                passwordHash = incomingHash,
                recoveryEmail = resolvedEmail,
                firebaseUid = firebaseUid,
                provider = "phone_password",
                displayNameHint = ""
            )

            // Firebase oturumu açıksa Firestore /users/{userId} kaydını senkronize et
            runCatching {
                val activeDisplayName = dataService.settings.value.businessName.ifBlank { activatedUser.displayName }
                AuthAccountRepository(appContext).upsertUserAccountIfSignedIn(
                    phone = fullPhone,
                    email = resolvedEmail,
                    displayName = activeDisplayName,
                    provider = "phone_password",
                    passwordHash = incomingHash
                )
            }

            onSuccess(needsOnboarding, isSettingPasswordNow)
        }
    }

    /**
     * Jetpack Credential Manager (`GetSignInWithGoogleOption`) + Firebase Auth ile gerçek Google girişi.
     */
    fun loginWithGoogleFirebase(
        activityContext: Context,
        onSuccess: (needsOnboarding: Boolean, displayName: String) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            val firebaseUser = FirebaseGoogleAuthHelper.signInWithGoogleInteractive(
                context = activityContext,
                onError = onError
            ) ?: return@launch

            val email = firebaseUser.email.orEmpty()
            val displayName = firebaseUser.displayName?.takeIf { it.isNotBlank() }
                ?: email.substringBefore("@").ifBlank { "Google Kullanıcısı" }
            val phone = firebaseUser.phoneNumber?.takeIf { it.isNotBlank() }
                ?: "+90 532 000 00 00"

            val (_, needsOnboarding) = dataService.activateUserAccountSession(
                countryCode = "+90",
                phoneNumber = phone.removePrefix("+90").trim(),
                passwordHash = "",
                recoveryEmail = email,
                firebaseUid = firebaseUser.uid,
                provider = "google",
                displayNameHint = displayName
            )

            runCatching {
                AuthAccountRepository(appContext).upsertUserAccountIfSignedIn(
                    phone = phone,
                    email = email,
                    displayName = dataService.settings.value.businessName.ifBlank { displayName },
                    provider = "google",
                    passwordHash = ""
                )
            }

            onSuccess(needsOnboarding, dataService.settings.value.businessName.ifBlank { displayName })
        }
    }

    /**
     * Firebase üzerinden şifre sıfırlama e-postası gönderir ve/veya telefon hesabının şifresini
     * yenileyerek Firestore `/users/{userId}` koleksiyonuna kaydeder.
     */
    fun resetPasswordWithFirebase(
        countryCode: String,
        phoneNumber: String,
        recoveryEmail: String,
        newPasswordPlain: String?,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanPhone = phoneNumber.trim()
        val digits = cleanPhone.filter { it.isDigit() }
        val fullPhone = if (digits.length >= 10) "$countryCode $cleanPhone" else ""

        if (recoveryEmail.isBlank() && newPasswordPlain.isNullOrBlank()) {
            onResult(false, "Lütfen Firebase şifre sıfırlama e-postası veya yeni şifrenizi girin.")
            return
        }
        if (!newPasswordPlain.isNullOrBlank() && newPasswordPlain.length < 6) {
            onResult(false, "Yeni şifreniz en az 6 karakter olmalıdır.")
            return
        }
        if (!newPasswordPlain.isNullOrBlank() && digits.length < 10) {
            onResult(false, "Yeni şifre tanımlamak için geçerli bir telefon numarası girin.")
            return
        }

        scope.launch {
            if (!newPasswordPlain.isNullOrBlank() && fullPhone.isNotBlank()) {
                val hash = AuthAccountRepository.sha256Hex(newPasswordPlain)
                val existing = dataService.getPhoneCredential(fullPhone)
                dataService.savePhoneCredential(
                    PhonePasswordCredential(
                        fullPhone = fullPhone,
                        phoneCountryCode = countryCode,
                        phoneNumber = cleanPhone,
                        passwordHash = hash,
                        recoveryEmail = recoveryEmail.trim().ifBlank { existing?.recoveryEmail.orEmpty() },
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            val repoResult = runCatching {
                AuthAccountRepository(appContext).sendFirebasePasswordReset(
                    email = recoveryEmail.trim(),
                    phone = fullPhone,
                    newPasswordPlain = newPasswordPlain
                )
            }.getOrElse { e ->
                if (!newPasswordPlain.isNullOrBlank()) {
                    Result.success("Telefon numaranız için yeni şifreniz kaydedildi.")
                } else {
                    Result.failure(e)
                }
            }

            repoResult.fold(
                onSuccess = { msg -> onResult(true, msg) },
                onFailure = { err ->
                    onResult(false, err.localizedMessage ?: "Şifre sıfırlama işlemi tamamlanamadı.")
                }
            )
        }
    }

    /**
     * Telefon numarası ile OTP adımına geçişi başlatır (geriye dönük uyumluluk).
     */
    fun startPhoneVerification(countryCode: String, phoneNumber: String) {
        onUpdatePendingPhone(countryCode, phoneNumber.trim())
    }

    /**
     * 6 haneli OTP kodunu doğrular.
     */
    fun verifyOtpCode(code: String, onSuccess: (needsOnboarding: Boolean) -> Unit): Boolean {
        if (code.trim() != DEMO_OTP_CODE) {
            return false
        }
        val cleanPhone = pendingPhoneNumber.ifBlank { "555 123 45 67" }

        scope.launch {
            val (_, needsOnboarding) = dataService.activateUserAccountSession(
                countryCode = pendingCountryCode,
                phoneNumber = cleanPhone,
                passwordHash = "",
                recoveryEmail = "",
                firebaseUid = "",
                provider = "phone_password",
                displayNameHint = ""
            )
            onSuccess(needsOnboarding)
        }
        return true
    }

    /**
     * Apple veya Google demo giriş yedek akışı.
     */
    fun loginWithSocialDemo(provider: String, onSuccess: (needsOnboarding: Boolean) -> Unit) {
        scope.launch {
            val (_, needsOnboarding) = dataService.activateUserAccountSession(
                countryCode = "+90",
                phoneNumber = "532 000 00 00",
                passwordHash = "",
                recoveryEmail = "",
                firebaseUid = "",
                provider = provider,
                displayNameHint = ""
            )
            onSuccess(needsOnboarding)
        }
    }

    /**
     * 3 adımlı işletme kurulumunu (Onboarding) tamamlar ve ana sayfaya geçişe izin verir.
     */
    fun completeOnboarding(
        businessName: String,
        businessType: String,
        logoUri: String,
        whatsappNumber: String,
        city: String,
        currencyCode: String,
        currencySymbol: String,
        catalogLanguage: String,
        onComplete: () -> Unit
    ) {
        val slug = businessName
            .lowercase()
            .replace("ş", "s")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace("ı", "i")
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "magazam" }

        scope.launch {
            val currentSettings = dataService.settings.value
            val updatedSettings = currentSettings.copy(
                businessName = businessName.trim(),
                businessType = businessType,
                logoUri = logoUri.trim(),
                phoneWhatsapp = whatsappNumber.trim(),
                city = city.trim(),
                currencyCode = currencyCode,
                currencySymbol = currencySymbol,
                catalogLanguage = catalogLanguage,
                customSlug = slug
            )

            dataService.updateSettings(updatedSettings)

            val baseUser = dataService.authUser.value ?: currentUser ?: AuthUser(
                id = UUID.randomUUID().toString(),
                phoneCountryCode = pendingCountryCode,
                phoneNumber = pendingPhoneNumber,
                fullPhone = whatsappNumber.trim(),
                provider = "phone_password"
            )
            val updatedUser = baseUser.copy(
                displayName = businessName.trim(),
                isOnboardingCompleted = true
            )
            dataService.saveAuthUser(updatedUser)

            runCatching {
                AuthAccountRepository(appContext).upsertUserAccountIfSignedIn(
                    phone = updatedUser.fullPhone,
                    email = updatedUser.email,
                    displayName = businessName.trim(),
                    provider = if (updatedUser.provider == "google") "google" else "phone_password",
                    passwordHash = updatedUser.passwordHash
                )
            }
            onComplete()
        }
    }

    /**
     * Kullanıcı oturumunu ve Firebase / Credential Manager durumunu kapatır.
     */
    fun logout(onLoggedOut: () -> Unit = {}) {
        scope.launch {
            FirebaseGoogleAuthHelper.signOutFirebaseAndCredentials(appContext)
            dataService.clearAuthSession()
            onLoggedOut()
        }
    }

    companion object {
        const val DEMO_OTP_CODE = "123456"
    }
}

val LocalAuthContext = staticCompositionLocalOf<AuthContextState> {
    error("AuthContextState sağlayıcısı (AuthProvider) bulunamadı!")
}

@Composable
fun AuthProvider(
    dataService: DataService,
    content: @Composable (AuthContextState) -> Unit
) {
    val appContext = LocalContext.current.applicationContext
    val currentUser by dataService.authUser.collectAsState()
    val storeSettings by dataService.settings.collectAsState()
    val scope = rememberCoroutineScope()

    var pendingCountryCode by remember { mutableStateOf("+90") }
    var pendingPhoneNumber by remember { mutableStateOf("") }

    val authState = remember(currentUser, storeSettings, pendingCountryCode, pendingPhoneNumber, appContext) {
        AuthContextState(
            currentUser = currentUser,
            storeSettings = storeSettings,
            pendingCountryCode = pendingCountryCode,
            pendingPhoneNumber = pendingPhoneNumber,
            appContext = appContext,
            dataService = dataService,
            scope = scope,
            onUpdatePendingPhone = { code, phone ->
                pendingCountryCode = code
                pendingPhoneNumber = phone
            }
        )
    }

    CompositionLocalProvider(LocalAuthContext provides authState) {
        content(authState)
    }
}
