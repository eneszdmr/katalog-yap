package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

/**
 * Helper for real Google Sign-In via Jetpack Credential Manager and Firebase Auth.
 * Follows the official 2-step flow:
 * 1) Silent auto-sign-in via `GetGoogleIdOption` (`setFilterByAuthorizedAccounts(true)`).
 * 2) Explicit button click via `GetSignInWithGoogleOption` in its own `GetCredentialRequest`.
 */
object FirebaseGoogleAuthHelper {

    suspend fun attemptAutoSignIn(context: Context): FirebaseUser? {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) return currentUser

        val credentialManager = CredentialManager.create(context)
        val clientId = runCatching { context.getString(R.string.default_web_client_id) }.getOrDefault("")
        if (clientId.isBlank()) return null

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(clientId)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                Firebase.auth.signInWithCredential(firebaseCredential).await().user
            } else {
                null
            }
        } catch (e: NoCredentialException) {
            null
        } catch (e: GetCredentialException) {
            Log.w("Auth", "Auto sign-in failed", e)
            null
        } catch (e: Exception) {
            Log.w("Auth", "Firebase signInWithCredential failed during auto sign-in", e)
            null
        }
    }

    suspend fun signInWithGoogleInteractive(
        context: Context,
        onError: (String) -> Unit
    ): FirebaseUser? {
        val credentialManager = CredentialManager.create(context)
        val clientId = runCatching { context.getString(R.string.default_web_client_id) }.getOrDefault("")
        if (clientId.isBlank()) {
            onError("Google Web Client ID yapılandırması bulunamadı.")
            return null
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                Firebase.auth.signInWithCredential(firebaseCredential).await().user
            } else {
                onError("Beklenmeyen kimlik bilgisi türü alındı.")
                null
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "User cancelled Google Sign-In", e)
            null
        } catch (e: GetCredentialException) {
            Log.e("Auth", "Credential Manager failed", e)
            onError("Google hesabı seçilemedi. Cihazda oturum açılmış bir Google hesabı olduğundan emin olun.")
            null
        } catch (e: Exception) {
            Log.e("Auth", "Firebase signInWithCredential failed", e)
            onError("Firebase Google girişi başarısız: ${e.localizedMessage ?: "Bilinmeyen hata"}")
            null
        }
    }

    suspend fun signOutFirebaseAndCredentials(context: Context) {
        runCatching { Firebase.auth.signOut() }
        runCatching {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }
}
