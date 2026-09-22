package com.cinepulse.app.ui.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

class AuthManager {
    private val auth = FirebaseAuth.getInstance()

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
        Unit
    }

    suspend fun register(email: String, password: String, displayName: String): Result<Unit> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        result.user?.updateProfile(profileUpdates)?.await()
        Unit
    }

    suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
        Unit
    }

    fun logout() = auth.signOut()

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun getCurrentUid(): String? = auth.currentUser?.uid
}
