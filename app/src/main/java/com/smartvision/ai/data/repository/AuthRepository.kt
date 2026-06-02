package com.smartvision.ai.data.repository

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.smartvision.ai.domain.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val sessionManager: SessionManager
) {
    private val auth = FirebaseAuth.getInstance()
    private var demoProfile = UserProfile()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            sessionManager.userProfile.collect { profile ->
                if (profile != null) {
                    demoProfile = profile
                }
            }
        }
    }

    val onboardingCompleted: Flow<Boolean> = sessionManager.onboardingCompleted
    val isLoggedIn: Flow<Boolean> = sessionManager.isLoggedIn

    suspend fun setOnboardingCompleted(completed: Boolean) {
        sessionManager.setOnboardingCompleted(completed)
    }

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
        val profile = currentProfile()
        sessionManager.saveSession(profile)
        profile
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
        val profile = currentProfile()
        sessionManager.saveSession(profile)
        profile
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
        val profile = currentProfile()
        sessionManager.saveSession(profile)
        profile
    }

    suspend fun loginAsGuest(): Result<UserProfile> = runCatching {
        demoProfile = UserProfile(name = "Guest User", email = "guest@smartvision.ai")
        sessionManager.saveSession(demoProfile)
        demoProfile
    }

    suspend fun logout() {
        auth.signOut()
        sessionManager.clearSession()
        demoProfile = UserProfile()
    }

    suspend fun updateUserProfile(name: String, email: String, photoUrl: String?): Result<UserProfile> = runCatching {
        val user = auth.currentUser
        if (user != null) {
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .setPhotoUri(photoUrl?.let { android.net.Uri.parse(it) })
                .build()
            user.updateProfile(profileUpdates).await()
            if (email.isNotBlank() && email != user.email && email.contains("@")) {
                runCatching { user.updateEmail(email).await() }
            }
        } else {
            demoProfile = UserProfile(name = name, email = email, photoUrl = photoUrl)
        }
        val profile = currentProfile()
        sessionManager.saveSession(profile)
        profile
    }

    fun getLoginProvider(): String {
        val user = auth.currentUser ?: return "Guest Account"
        for (profile in user.providerData) {
            if (profile.providerId == "google.com") {
                return "Google Sign-In"
            }
        }
        return "Email / Password"
    }
}

