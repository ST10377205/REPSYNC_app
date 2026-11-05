package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private class GoogleLinkCancelledException : Exception()

class LoginActivity : BaseActivity() {
    private var googleAttemptActive = false
    private var sessionEstablished = false

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openNotificationForSignedInUser()
    }

    private fun openNotificationForSignedInUser(): Boolean {
        if (!NotificationNavigation.isNotificationIntent(intent) ||
            getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE).getInt("current_user_id", -1) == -1) return false
        openHome()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!intent.getBooleanExtra("show_login", false) &&
            getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE).getInt("current_user_id", -1) != -1) {
            openHome()
            return
        }
        setContentView(R.layout.activity_login)
        applySystemBarInsets(R.id.loginMain)

        findViewById<MaterialButton>(R.id.btnLogin).setOnClickListener { signInWithPassword() }
        findViewById<MaterialButton>(R.id.btnGoogleSignIn).setOnClickListener { signInWithGoogle() }
        findViewById<TextView>(R.id.tvSignUpRedirect).setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun signInWithPassword() {
        val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
        val password = findViewById<EditText>(R.id.etPassword).text.toString().trim()
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.ui_please_enter_all_fields, Toast.LENGTH_SHORT).show()
            return
        }
        if (!ValidationUtils.isValidEmail(email)) {
            findViewById<EditText>(R.id.etEmail).error = getString(R.string.validation_email)
            return
        }
        setBusy(true)
        lifecycleScope.launch {
            try {
                val candidates = AppDatabase.getDatabase(this@LoginActivity).userDao().getUsersByEmail(email)
                val account = withContext(Dispatchers.IO) {
                    candidates.firstOrNull { PasswordVerifier.matches(password, it.password) }
                }
                if (account == null) {
                    Toast.makeText(this@LoginActivity, R.string.ui_invalid_email_or_password, Toast.LENGTH_SHORT).show()
                } else {
                    AuthSession.beginLocal(this@LoginActivity, account.id)
                    openHome()
                }
            } finally {
                if (!isFinishing && !isDestroyed) setBusy(false)
            }
        }
    }

    private fun signInWithGoogle() {
        if (googleAttemptActive) return
        showStatus(null)
        googleAttemptActive = true
        setBusy(true)
        lifecycleScope.launch {
            try {
                val identity = GoogleSignInCoordinator.authenticate(this@LoginActivity)
                val repository = GoogleAccountRepository(AppDatabase.getDatabase(this@LoginActivity))
                var result = repository.signIn(identity)
                var passwordError = false
                while (result is GoogleAccountResult.PasswordRequired) {
                    val password = requestLocalPassword(identity.email, passwordError)
                    try {
                        result = repository.signIn(identity, password)
                    } catch (_: LocalLinkPasswordException) {
                        passwordError = true
                    }
                }
                val account = result as GoogleAccountResult.SignedIn
                AuthSession.beginGoogle(this@LoginActivity, account.userId, identity.uid)
                sessionEstablished = true
                openHome()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: GetCredentialCancellationException) {
                // The account picker may be dismissed without changing the local session.
            } catch (_: GoogleLinkCancelledException) {
                // Linking requires the existing account password and explicit confirmation.
            } catch (_: GoogleSetupMissingException) {
                showStatus(R.string.google_not_configured)
            } catch (_: NoCredentialException) {
                showStatus(R.string.google_no_account)
            } catch (_: FirebaseNetworkException) {
                showStatus(R.string.google_network_error)
            } catch (_: GoogleAccountConflictException) {
                showStatus(R.string.google_link_conflict)
            } catch (_: Exception) {
                showStatus(R.string.google_sign_in_failed)
            } finally {
                googleAttemptActive = false
                if (!sessionEstablished) AuthSession.signOutFirebase()
                if (!isFinishing && !isDestroyed) setBusy(false)
            }
        }
    }

    private suspend fun requestLocalPassword(email: String, incorrect: Boolean): String =
        suspendCancellableCoroutine { continuation ->
            val input = EditText(this).apply {
                hint = getString(R.string.ui_password)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                if (incorrect) error = getString(R.string.google_link_password_error)
            }
            val padding = (24 * resources.displayMetrics.density).toInt()
            val container = android.widget.FrameLayout(this).apply {
                setPadding(padding, 0, padding, 0)
                addView(input)
            }
            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle(R.string.google_link_title)
                .setMessage(getString(R.string.google_link_message, email))
                .setView(container)
                .setPositiveButton(R.string.google_link_button) { _, _ ->
                    if (continuation.isActive) continuation.resume(input.text.toString().trim())
                }
                .setNegativeButton(R.string.ui_cancel) { _, _ ->
                    if (continuation.isActive) continuation.resumeWithException(GoogleLinkCancelledException())
                }
                .setOnCancelListener {
                    if (continuation.isActive) continuation.resumeWithException(GoogleLinkCancelledException())
                }
                .show()
            continuation.invokeOnCancellation { runOnUiThread { dialog.dismiss() } }
        }

    private fun setBusy(busy: Boolean) {
        for (id in intArrayOf(R.id.btnLogin, R.id.btnGoogleSignIn, R.id.etEmail, R.id.etPassword,
            R.id.tvSignUpRedirect, R.id.tvForgotPassword)) findViewById<View>(id).isEnabled = !busy
        findViewById<MaterialButton>(R.id.btnGoogleSignIn).setText(
            if (busy && googleAttemptActive) R.string.google_signing_in else R.string.google_sign_in
        )
    }

    private fun showStatus(message: Int?) {
        findViewById<TextView>(R.id.tvGoogleSignInStatus).apply {
            visibility = if (message == null) View.GONE else View.VISIBLE
            if (message != null) setText(message)
        }
    }

    private fun openHome() {
        startActivity(NotificationNavigation.destinationIntent(this, intent))
        finish()
    }

    override fun onDestroy() {
        if (googleAttemptActive && !sessionEstablished) AuthSession.signOutFirebase()
        super.onDestroy()
    }
}
