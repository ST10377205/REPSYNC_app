package com.example.myapplication

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import com.google.firebase.auth.FirebaseAuth

object AuthSession {
    fun signOutFirebase() { if (PushNotifications.isConfigured()) FirebaseAuth.getInstance().signOut() }

    fun beginLocal(context: Context, userId: Int) {
        WorkoutSyncScheduler.cancelCurrent(context)
        signOutFirebase()
        context.getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE).edit()
            .putInt("current_user_id", userId).putString("auth_method", "password").remove("firebase_uid").apply()
    }

    fun beginGoogle(context: Context, userId: Int, uid: String) {
        check(FirebaseAuth.getInstance().currentUser?.uid == uid)
        WorkoutSyncScheduler.cancelCurrent(context)
        context.getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE).edit()
            .putInt("current_user_id", userId).putString("auth_method", "google").putString("firebase_uid", uid).apply()
        WorkoutSyncScheduler.enqueue(context, userId, uid)
    }

    fun clearLocalSession(context: Context) {
        WorkoutSyncScheduler.cancelCurrent(context)
        signOutFirebase()
        context.getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE).edit()
            .remove("current_user_id").remove("auth_method").remove("firebase_uid").apply()
    }

    suspend fun clearGoogleSession(context: Context) {
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (_: ClearCredentialException) {
            // Firebase and the local session are already signed out even if a provider cannot clear its hint.
        }
    }
}
