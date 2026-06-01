package com.smartvision.ai.data.repository

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.smartvision.ai.domain.model.UserProfile
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor() {
    private val auth = FirebaseAuth.getInstance()
    private var demoProfile = UserProfile()

    fun currentProfile(): UserProfile {
        val user = auth.currentUser
        return if (user != null) {
            UserProfile(
                name = user.displayName ?: demoProfile.name,
                email = user.email ?: demoProfile.email,
                photoUrl = user.photoUrl?.toString()
            )
        } else {
            demoProfile
        }
    }

    suspend fun emailLogin(email: String, password: String): Result<UserProfile> = runCatching {
        require(email.contains("@")) { "Enter a valid email address." }
        require(password.length >= 6) { "Password must be at least 6 characters." }
        try {
            auth.signInWithEmailAndPassword(email, password).await()
        } catch (_: Exception) {
            demoProfile = UserProfile(name = "Ajith Kumar", email = email)
        }
        currentProfile()
    }

    suspend fun signup(name: String, email: String, password: String): Result<UserProfile> = runCatching {
        require(name.isNotBlank()) { "Enter your name." }
        require(email.contains("@")) { "Enter a valid email address." }
        require(password.length >= 6) { "Password must be at least 6 characters." }
        try {
            auth.createUserWithEmailAndPassword(email, password).await()
        } catch (_: Exception) {
            demoProfile = UserProfile(name = name.trim(), email = email.trim())
        }
        currentProfile()
    }

    suspend fun connectGoogle(account: GoogleSignInAccount?): Result<UserProfile> = runCatching {
        require(account != null) { "Google sign-in was cancelled." }
        val token = account.idToken
        if (!token.isNullOrBlank()) {
            val credential = GoogleAuthProvider.getCredential(token, null)
            runCatching { auth.signInWithCredential(credential).await() }
        }
        demoProfile = UserProfile(
            name = account.displayName ?: "Ajith Kumar",
            email = account.email ?: "Google connected",
            photoUrl = account.photoUrl?.toString()
        )
        currentProfile()
    }
}
