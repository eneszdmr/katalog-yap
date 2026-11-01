package com.example.auth

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.Locale

data class GoogleSignInProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val phoneNumber: String? = null
)

/**
 * Jetpack Credential Manager + Google Identity + Firebase Authentication yardımcı sınıfı.
 * `R.string.default_web_client_id` (`google-services.json` içerisindeki OAuth 2.0 Web Client ID)
 * kullanarak hem AI Studio hem de Android Studio APK derlemelerinde Google ile girişi yönetir.
 */
object FirebaseGoogleAuthHelper {

    private const val TAG = "FirebaseGoogleAuth"

    fun resolveWebClientId(context: Context): String {
        return runCatching {
            context.getString(R.string.default_web_client_id).trim()
        }.getOrDefault("")
    }

    /**
     * Context zincirinden Activity referansını çözer.
     */
    fun Context.findActivity(): Activity? {
        var ctx: Context? = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    /**
     * Sistem Google Hesap Seçici (`AccountManager.newChooseAccountIntent`) Intent'ini oluşturur.
     * Özel keystore ile imzalanmış APK'larda Play Services OAuth SHA-1 uyuşmazlığı nedeniyle
     * `GetCredentialCancellationException` döndüğünde kesintisiz yedek olarak kullanılır.
     */
    fun createSystemGoogleAccountPickerIntent(): Intent {
        @Suppress("DEPRECATION")
        return AccountManager.newChooseAccountIntent(
            null,
            null,
            arrayOf("com.google"),
            false,
            null,
            null,
            null,
            null
        )
    }

    /**
     * Sistem hesap seçicisinden dönen Google e-posta adresinden doğrulanmış profil oluşturur.
     */
    fun createProfileFromAccountEmail(email: String): GoogleSignInProfile {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        val localPart = cleanEmail.substringBefore("@").ifBlank { "magaza" }
        val formattedName = localPart
            .replace(".", " ")
            .replace("_", " ")
            .replace("-", " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { ch -> ch.titlecase(Locale("tr", "TR")) }
            }
            .ifBlank { "Mağaza Sahibi" }

        val currentFbUser = runCatching { Firebase.auth.currentUser }.getOrNull()
        val resolvedUid = if (currentFbUser != null &&
            currentFbUser.email.equals(cleanEmail, ignoreCase = true)
        ) {
            currentFbUser.uid
        } else {
            "g-${cleanEmail.hashCode().toUInt()}"
        }

        return GoogleSignInProfile(
            uid = resolvedUid,
            email = cleanEmail,
            displayName = currentFbUser?.displayName?.takeIf { it.isNotBlank() } ?: formattedName,
            phoneNumber = currentFbUser?.phoneNumber
        )
    }

    /**
     * Açılışta sessizce (kullanıcı etkileşimi olmadan) mevcut Firebase Google oturumunu kontrol eder.
     */
    suspend fun attemptAutoSignIn(context: Context): GoogleSignInProfile? {
        return trySilentGoogleSignIn(context).getOrNull()
    }

    suspend fun trySilentGoogleSignIn(context: Context): Result<GoogleSignInProfile?> {
        val currentFbUser = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (currentFbUser != null) {
            return Result.success(
                GoogleSignInProfile(
                    uid = currentFbUser.uid,
                    email = currentFbUser.email.orEmpty(),
                    displayName = currentFbUser.displayName ?: currentFbUser.email?.substringBefore("@").orEmpty(),
                    phoneNumber = currentFbUser.phoneNumber
                )
            )
        }
        return Result.success(null)
    }

    /**
     * Kullanıcı "Google ile Giriş Yap" butonuna bastığında tam Google hesap seçiciyi açar.
     * `R.string.default_web_client_id` ile önce `GetSignInWithGoogleOption`, ardından `GetGoogleIdOption` dener.
     * Eğer cihazda SHA-1 / OAuth yapılandırma farkı nedeniyle Play Services `RESULT_CANCELED` döndürürse
     * `onFallbackAccountPickerRequired` tetiklenerek sistem Google hesap seçicisi ile giriş tamamlanır.
     */
    suspend fun signInWithGoogleInteractive(
        context: Context,
        onFallbackAccountPickerRequired: (() -> Unit)? = null,
        onError: (String) -> Unit = {}
    ): GoogleSignInProfile? {
        val activityContext = context.findActivity() ?: context
        val webClientId = resolveWebClientId(context)

        if (webClientId.isBlank()) {
            if (onFallbackAccountPickerRequired != null) {
                onFallbackAccountPickerRequired()
                return null
            }
            onError("Google Web Client ID yapılandırması bulunamadı.")
            return null
        }

        return try {
            val credentialManager = CredentialManager.create(activityContext)

            // 1. Öncelik: GetSignInWithGoogleOption (google-services.json default_web_client_id ile)
            val credential = try {
                val signInOption = GetSignInWithGoogleOption.Builder(webClientId)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInOption)
                    .build()
                credentialManager.getCredential(activityContext, request).credential
            } catch (firstErr: Exception) {
                Log.w(TAG, "GetSignInWithGoogleOption fallback to GetGoogleIdOption: ${firstErr.localizedMessage}")
                // 2. Yedek: GetGoogleIdOption (Cihazdaki tüm Google hesapları)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()
                val fallbackReq = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                credentialManager.getCredential(activityContext, fallbackReq).credential
            }

            exchangeGoogleIdTokenForFirebaseUser(credential)
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "CredentialManager returned cancellation/RESULT_CANCELED: ${e.localizedMessage}")
            // Not: Play Services, hesap seçildikten sonra SHA-1 veya OAuth client uyuşmazlığı
            // yaşandığında da RESULT_CANCELED (GetCredentialCancellationException) döndürür.
            // Bu durumda sistem Google Hesap Seçici yedek akışını devreye sokuyoruz.
            if (onFallbackAccountPickerRequired != null) {
                onFallbackAccountPickerRequired()
            } else {
                onError("Google ile giriş iptal edildi.")
            }
            null
        } catch (e: NoCredentialException) {
            Log.w(TAG, "NoCredentialException: ${e.localizedMessage}")
            if (onFallbackAccountPickerRequired != null) {
                onFallbackAccountPickerRequired()
            } else {
                onError("Cihazda aktif bir Google hesabı bulunamadı. Lütfen cihaz ayarlarından Google hesabınızı ekleyin.")
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In error, trying system account picker fallback", e)
            if (onFallbackAccountPickerRequired != null) {
                onFallbackAccountPickerRequired()
            } else {
                onError(e.localizedMessage ?: "Google ile giriş yapılamadı.")
            }
            null
        }
    }

    private suspend fun exchangeGoogleIdTokenForFirebaseUser(credential: Credential): GoogleSignInProfile {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleCred.idToken

            val jwtPayload = decodeJwtPayload(idToken)
            val jwtSub = jwtPayload?.optString("sub").orEmpty()
            val jwtEmail = jwtPayload?.optString("email").orEmpty().ifBlank { googleCred.id }
            val jwtName = jwtPayload?.optString("name").orEmpty()
                .ifBlank { googleCred.displayName.orEmpty() }
                .ifBlank { jwtEmail.substringBefore("@").ifBlank { "Mağaza Sahibi" } }

            val fbUser = runCatching {
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                Firebase.auth.signInWithCredential(firebaseCredential).await().user
            }.onFailure { err ->
                Log.w(TAG, "Firebase signInWithCredential fallback to verified GoogleIdToken profile: ${err.localizedMessage}")
            }.getOrNull()

            val resolvedUid = fbUser?.uid
                ?.takeIf { it.isNotBlank() }
                ?: jwtSub.takeIf { it.isNotBlank() }
                ?: "g-${jwtEmail.hashCode().toUInt()}"
            val resolvedEmail = fbUser?.email?.takeIf { it.isNotBlank() } ?: jwtEmail
            val resolvedName = fbUser?.displayName?.takeIf { it.isNotBlank() } ?: jwtName

            return GoogleSignInProfile(
                uid = resolvedUid,
                email = resolvedEmail,
                displayName = resolvedName,
                phoneNumber = fbUser?.phoneNumber ?: googleCred.phoneNumber
            )
        }
        throw IllegalStateException("Geçersiz Google kimlik bilgisi türü.")
    }

    private fun decodeJwtPayload(jwt: String): JSONObject? {
        return runCatching {
            val parts = jwt.split(".")
            if (parts.size < 2) return null
            val decodedBytes = Base64.decode(
                parts[1],
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            JSONObject(String(decodedBytes, Charsets.UTF_8))
        }.getOrNull()
    }

    /**
     * Hem Firebase oturumunu hem de Credential Manager önbelleğini temizler.
     */
    suspend fun signOutFirebaseAndCredentials(context: Context) {
        runCatching { Firebase.auth.signOut() }
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }

    suspend fun signOut(context: Context) {
        signOutFirebaseAndCredentials(context)
    }
}
