package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GoogleSetupMissingException : Exception()
class GoogleIdentityVerificationException : Exception()

object GoogleSignInCoordinator {
    fun webClientId(context: Context): String? {
        // Google Services generates this resource only after Google is enabled in Firebase.
        val resource = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resource == 0) null else context.getString(resource).takeIf { it.isNotBlank() }
    }

    suspend fun authenticate(activity: Activity): VerifiedGoogleIdentity {
        if (!PushNotifications.isConfigured()) throw GoogleSetupMissingException()
        val clientId = webClientId(activity) ?: throw GoogleSetupMissingException()
        val nonceBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val nonce = Base64.encodeToString(nonceBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val option = GetSignInWithGoogleOption.Builder(clientId).setNonce(nonce).build()
        val result = CredentialManager.create(activity).getCredential(
            activity, GetCredentialRequest.Builder().addCredentialOption(option).build()
        )
        val credential = result.credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw GoogleIdentityVerificationException()
        }
        val google = GoogleIdTokenCredential.createFrom(credential.data)
        val segments = google.idToken.split('.')
        if (segments.size != 3) throw GoogleIdentityVerificationException()
        val claims = JSONObject(String(Base64.decode(segments[1], Base64.URL_SAFE), Charsets.UTF_8))
        if (claims.optString("nonce") != nonce) throw GoogleIdentityVerificationException()

        val auth = FirebaseAuth.getInstance()
        val firebaseUser = suspendCancellableCoroutine<com.google.firebase.auth.FirebaseUser> { continuation ->
            auth.signInWithCredential(GoogleAuthProvider.getCredential(google.idToken, null))
                .addOnCompleteListener { task ->
                    if (!continuation.isActive) {
                        if (task.isSuccessful) auth.signOut()
                    } else if (task.isSuccessful && task.result.user != null) {
                        continuation.resume(task.result.user!!)
                    } else {
                        continuation.resumeWithException(task.exception ?: GoogleIdentityVerificationException())
                    }
                }
        }
        val email = firebaseUser.email
        if (!firebaseUser.isEmailVerified || email.isNullOrBlank() ||
            firebaseUser.providerData.none { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
            auth.signOut()
            throw GoogleIdentityVerificationException()
        }
        return VerifiedGoogleIdentity(firebaseUser.uid, email, firebaseUser.displayName.orEmpty())
    }
}
