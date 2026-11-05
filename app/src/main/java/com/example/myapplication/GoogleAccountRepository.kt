package com.example.myapplication

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** Identity obtained only after Firebase has verified the Google credential. No tokens are stored here. */
data class VerifiedGoogleIdentity(val uid: String, val email: String, val displayName: String) {
    init { require(uid.isNotBlank() && email.isNotBlank()) }
}

sealed class GoogleAccountResult {
    data class SignedIn(val userId: Int) : GoogleAccountResult()
    object PasswordRequired : GoogleAccountResult()
}

class LocalLinkPasswordException : Exception()
class GoogleAccountConflictException : Exception()

class GoogleAccountRepository(private val database: AppDatabase) {
    suspend fun signIn(identity: VerifiedGoogleIdentity, localPassword: String? = null): GoogleAccountResult =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val dao = database.userDao()
                val known = dao.getUserByGoogleUid(identity.uid)
                if (known != null) return@withTransaction GoogleAccountResult.SignedIn(known.id)

                val email = identity.email.trim().lowercase(Locale.ROOT)
                val candidates = dao.getUsersByEmail(email)
                if (candidates.isNotEmpty()) {
                    // A verified email alone must not silently link a local password account.
                    if (candidates.any { it.googleUid != null }) throw GoogleAccountConflictException()
                    if (localPassword == null) return@withTransaction GoogleAccountResult.PasswordRequired
                    val matches = candidates.filter { PasswordVerifier.matches(localPassword, it.password) }
                    if (matches.size != 1) throw LocalLinkPasswordException()
                    val account = matches.single()
                    dao.updateUser(account.copy(googleUid = identity.uid))
                    return@withTransaction GoogleAccountResult.SignedIn(account.id)
                }

                val id = dao.signup(User(
                    fullName = identity.displayName.ifBlank { email.substringBefore('@') },
                    email = email,
                    password = "", // There is no local password for a Google-only account.
                    googleUid = identity.uid
                ))
                GoogleAccountResult.SignedIn(id.toInt())
            }
        }
}
