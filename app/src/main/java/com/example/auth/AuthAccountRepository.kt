package com.example.auth

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

data class FirestoreUserAccount(
    val userId: String = "",
    val phone: String = "",
    val email: String = "",
    val displayName: String = "",
    val provider: String = "phone_password",
    val passwordHash: String = "",
    val passwordResetRequestedAt: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

/**
 * Firebase Auth & Firestore repository for user account metadata, Google Sign-In profile sync,
 * and Firebase password reset (`sendPasswordResetEmail` + Firestore `/users/{userId}` audit record).
 *
 * Always targets the provisioned named Firestore database from `R.string.firestore_database_id`.
 */
class AuthAccountRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun handleFirestoreError(
        exception: Exception,
        operationType: OperationType,
        path: String?
    ) {
        val currentUser = auth.currentUser
        val providerInfoArray = JSONArray().apply {
            currentUser?.providerData?.forEach { profile ->
                put(
                    JSONObject().apply {
                        put("providerId", profile.providerId)
                        put("displayName", profile.displayName)
                        put("email", profile.email)
                        put("phoneNumber", profile.phoneNumber)
                        put("photoUrl", profile.photoUrl?.toString())
                    }
                )
            }
        }
        val authInfo = JSONObject().apply {
            put("userId", currentUser?.uid)
            put("email", currentUser?.email)
            put("emailVerified", currentUser?.isEmailVerified)
            put("isAnonymous", currentUser?.isAnonymous)
            put("tenantId", currentUser?.tenantId)
            put("providerInfo", providerInfoArray)
        }
        val errInfo = JSONObject().apply {
            put("error", exception.message ?: "Unknown Firestore error")
            put("operationType", operationType.value)
            put("path", path)
            put("authInfo", authInfo)
        }
        Log.e("FirestoreError", errInfo.toString(), exception)
        throw exception
    }

    suspend fun createUserAccount(
        phone: String,
        email: String,
        displayName: String,
        provider: String,
        passwordHash: String,
        passwordResetRequestedAt: Long = 0L
    ) {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Firebase kullanıcısı oturum açmamış.")
        val docPath = "users/$uid"
        val payload = mapOf(
            "userId" to uid,
            "phone" to phone.take(32),
            "email" to email.take(254),
            "displayName" to displayName.take(120),
            "provider" to if (provider == "google") "google" else "phone_password",
            "passwordHash" to passwordHash.take(256),
            "passwordResetRequestedAt" to passwordResetRequestedAt.coerceAtLeast(0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            db.document(docPath).set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
        }
    }

    suspend fun updateUserAccount(
        phone: String? = null,
        email: String? = null,
        displayName: String? = null,
        provider: String? = null,
        passwordHash: String? = null,
        passwordResetRequestedAt: Long? = null
    ) {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Firebase kullanıcısı oturum açmamış.")
        val docPath = "users/$uid"
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        phone?.let { updates["phone"] = it.take(32) }
        email?.let { updates["email"] = it.take(254) }
        displayName?.let { updates["displayName"] = it.take(120) }
        provider?.let { updates["provider"] = if (it == "google") "google" else "phone_password" }
        passwordHash?.let { updates["passwordHash"] = it.take(256) }
        passwordResetRequestedAt?.let { updates["passwordResetRequestedAt"] = it.coerceAtLeast(0L) }

        try {
            db.document(docPath).update(updates).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
        }
    }

    suspend fun upsertUserAccountIfSignedIn(
        phone: String,
        email: String,
        displayName: String,
        provider: String,
        passwordHash: String,
        passwordResetRequestedAt: Long = 0L
    ): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val docPath = "users/$uid"
        return try {
            val snap = db.document(docPath).get().await()
            if (snap.exists()) {
                updateUserAccount(
                    phone = phone,
                    email = email,
                    displayName = displayName,
                    provider = provider,
                    passwordHash = passwordHash,
                    passwordResetRequestedAt = passwordResetRequestedAt
                )
            } else {
                createUserAccount(
                    phone = phone,
                    email = email,
                    displayName = displayName,
                    provider = provider,
                    passwordHash = passwordHash,
                    passwordResetRequestedAt = passwordResetRequestedAt
                )
            }
            true
        } catch (e: Exception) {
            Log.w("AuthAccountRepository", "Firestore sync skipped or failed: ${e.message}")
            false
        }
    }

    suspend fun getUserAccount(targetUserId: String? = null): FirestoreUserAccount? {
        val uid = targetUserId ?: auth.currentUser?.uid
            ?: throw IllegalStateException("Firebase kullanıcısı oturum açmamış.")
        val docPath = "users/$uid"
        return try {
            val snap = db.document(docPath).get().await()
            if (!snap.exists()) return null
            FirestoreUserAccount(
                userId = snap.getString("userId").orEmpty(),
                phone = snap.getString("phone").orEmpty(),
                email = snap.getString("email").orEmpty(),
                displayName = snap.getString("displayName").orEmpty(),
                provider = snap.getString("provider") ?: "phone_password",
                passwordHash = snap.getString("passwordHash").orEmpty(),
                passwordResetRequestedAt = snap.getLong("passwordResetRequestedAt") ?: 0L,
                createdAt = snap.getTimestamp("createdAt"),
                updatedAt = snap.getTimestamp("updatedAt")
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            null
        }
    }

    suspend fun deleteUserAccount(targetUserId: String? = null) {
        val uid = targetUserId ?: auth.currentUser?.uid
            ?: throw IllegalStateException("Firebase kullanıcısı oturum açmamış.")
        val docPath = "users/$uid"
        try {
            db.document(docPath).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
        }
    }

    fun observeUserAccount(): Flow<FirestoreUserAccount?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val docPath = "users/$uid"
        val registration = db.document(docPath).addSnapshotListener { snap, error ->
            if (error != null) {
                try {
                    handleFirestoreError(error, OperationType.GET, docPath)
                } catch (t: Throwable) {
                    close(t)
                }
                return@addSnapshotListener
            }
            if (snap == null || !snap.exists()) {
                trySend(null)
            } else {
                trySend(
                    FirestoreUserAccount(
                        userId = snap.getString("userId").orEmpty(),
                        phone = snap.getString("phone").orEmpty(),
                        email = snap.getString("email").orEmpty(),
                        displayName = snap.getString("displayName").orEmpty(),
                        provider = snap.getString("provider") ?: "phone_password",
                        passwordHash = snap.getString("passwordHash").orEmpty(),
                        passwordResetRequestedAt = snap.getLong("passwordResetRequestedAt") ?: 0L,
                        createdAt = snap.getTimestamp("createdAt"),
                        updatedAt = snap.getTimestamp("updatedAt")
                    )
                )
            }
        }
        awaitClose { registration.remove() }
    }

    /**
     * Triggers Firebase Auth password reset email (`Firebase.auth.sendPasswordResetEmail`)
     * when a valid email is provided, and syncs the password reset timestamp / new password hash
     * to Firestore `/users/{userId}` if a Firebase user session is active.
     */
    suspend fun sendFirebasePasswordReset(
        email: String,
        phone: String,
        newPasswordPlain: String?
    ): Result<String> {
        val cleanEmail = email.trim()
        var emailSent = false
        var emailError: String? = null

        if (cleanEmail.isNotEmpty() && cleanEmail.contains("@")) {
            try {
                auth.sendPasswordResetEmail(cleanEmail).await()
                emailSent = true
            } catch (e: Exception) {
                Log.w("AuthAccountRepository", "Firebase sendPasswordResetEmail warning: ${e.message}")
                emailError = e.localizedMessage
            }
        }

        val now = System.currentTimeMillis()
        val newHash = newPasswordPlain?.takeIf { it.isNotBlank() }?.let { sha256Hex(it) }
        val syncedToFirestore = if (auth.currentUser != null) {
            upsertUserAccountIfSignedIn(
                phone = phone,
                email = cleanEmail.ifBlank { auth.currentUser?.email.orEmpty() },
                displayName = auth.currentUser?.displayName.orEmpty(),
                provider = if (newHash != null) "phone_password" else "google",
                passwordHash = newHash.orEmpty(),
                passwordResetRequestedAt = now
            )
        } else {
            false
        }

        return when {
            emailSent && newHash != null -> Result.success(
                "Firebase şifre sıfırlama bağlantısı $cleanEmail adresine gönderildi ve yeni telefon şifreniz kaydedildi."
            )
            emailSent -> Result.success(
                "Firebase şifre sıfırlama bağlantısı $cleanEmail adresine başarıyla gönderildi."
            )
            newHash != null && syncedToFirestore -> Result.success(
                "Yeni şifreniz Firebase hesabınızla eşleştirilerek güncellendi."
            )
            newHash != null -> Result.success(
                "Telefon numaranız için yeni şifreniz başarıyla kaydedildi."
            )
            emailError != null -> Result.failure(
                IllegalStateException("Firebase şifre sıfırlama e-postası gönderilemedi: $emailError")
            )
            else -> Result.failure(
                IllegalArgumentException("Lütfen geçerli bir kurtarma e-postası veya yeni şifre girin.")
            )
        }
    }

    companion object {
        fun sha256Hex(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
