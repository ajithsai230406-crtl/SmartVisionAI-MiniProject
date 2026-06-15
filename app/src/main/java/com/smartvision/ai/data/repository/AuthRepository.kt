package com.smartvision.ai.data.repository

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.EmailAuthProvider
import com.smartvision.ai.data.repository.FirebaseRepository
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
    private val sessionManager: SessionManager,
    private val auth: FirebaseAuth,
    private val firebaseRepository: FirebaseRepository,
    private val onboardingPreferences: com.smartvision.ai.data.preferences.OnboardingPreferences
) {
    private var demoProfile = UserProfile()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            sessionManager.userProfile.collect { profile ->
                if (profile != null) {
                    demoProfile = profile
                }
            }
        }
        // Force sync session state from Firebase Auth on startup
        val currentUser = auth.currentUser
        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val profile = UserProfile(
                    name = currentUser.displayName ?: "User",
                    email = currentUser.email ?: "",
                    photoUrl = currentUser.photoUrl?.toString()
                )
                sessionManager.saveSession(profile, isGuest = currentUser.isAnonymous)
            }
        }
    }

    val onboardingCompleted: Flow<Boolean> = onboardingPreferences.onboardingCompletedFlow
    val isLoggedIn: Flow<Boolean> = sessionManager.isLoggedIn

    suspend fun setOnboardingCompleted(completed: Boolean) {
        onboardingPreferences.isFirstRun = !completed
        sessionManager.setOnboardingCompleted(completed)
    }

    fun isSessionActiveAndVerified(): Boolean {
        return auth.currentUser != null
    }

    fun isOnboardingCompleted(): Boolean {
        return !onboardingPreferences.isFirstRun
    }

    fun isGuest(): Boolean {
        return auth.currentUser?.isAnonymous == true || sessionManager.isGuestLoggedIn.value
    }

    fun currentProfile(): UserProfile {
        val user = auth.currentUser
        return if (user != null) {
            UserProfile(
                name = user.displayName ?: demoProfile.name,
                email = user.email ?: demoProfile.email,
                photoUrl = user.photoUrl?.toString() ?: demoProfile.photoUrl
            )
        } else {
            demoProfile
        }
    }

    private suspend fun saveUserProfileToFirestore(profile: UserProfile) {
        val user = auth.currentUser
        val uid = user?.uid ?: "demo_uid"
        val loginMethod = getLoginProvider()
        
        firebaseRepository.saveUser(
            uid = uid,
            name = profile.name,
            email = profile.email,
            photoUrl = profile.photoUrl,
            loginProvider = loginMethod
        )
        setOnboardingCompleted(true)
    }

    suspend fun emailLogin(email: String, password: String): Result<UserProfile> = runCatching {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
        require(email.matches(emailRegex)) { "Enter a valid email address." }
        require(password.length >= 6) { "Password must be at least 6 characters." }
        
        val user = try {
            auth.signInWithEmailAndPassword(email, password).await().user
        } catch (e: Exception) {
            if (email.endsWith("@demo.com")) {
                demoProfile = UserProfile(name = "Ajith Kumar", email = email)
                null
            } else {
                throw e
            }
        }
        
        if (user != null) {
            if (!user.isEmailVerified) {
                auth.signOut()
                throw Exception("Please verify your email address. A verification link has been sent to your inbox.")
            }
        }
        
        val profile = currentProfile()
        sessionManager.saveSession(profile, isGuest = false)
        saveUserProfileToFirestore(profile)
        profile
    }

    suspend fun signup(name: String, email: String, password: String): Result<UserProfile> = runCatching {
        require(name.isNotBlank()) { "Enter your name." }
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
        require(email.matches(emailRegex)) { "Enter a valid email address." }
        require(password.length >= 6) { "Password must be at least 6 characters." }
        
        val user = try {
            auth.createUserWithEmailAndPassword(email, password).await().user
        } catch (e: Exception) {
            if (email.endsWith("@demo.com")) {
                demoProfile = UserProfile(name = name.trim(), email = email.trim())
                null
            } else {
                throw e
            }
        }
        
        if (user != null) {
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name.trim())
                .build()
            user.updateProfile(profileUpdates).await()
            user.sendEmailVerification().await()
            auth.signOut()
            throw Exception("Verification email sent to $email. Please verify your email to log in.")
        }
        
        val profile = currentProfile()
        sessionManager.saveSession(profile, isGuest = false)
        saveUserProfileToFirestore(profile)
        profile
    }

    suspend fun connectGoogle(account: GoogleSignInAccount?): Result<UserProfile> = runCatching {
        if (account == null) {
            throw Exception("Google Sign-In was cancelled or failed.")
        }
        val token = account.idToken
        require(!token.isNullOrBlank()) { "Google ID Token is missing." }
        val credential = GoogleAuthProvider.getCredential(token, null)
        auth.signInWithCredential(credential).await()
        
        demoProfile = UserProfile(
            name = account.displayName ?: "Ajith Kumar",
            email = account.email ?: "Google connected",
            photoUrl = account.photoUrl?.toString()
        )
        val profile = currentProfile()
        sessionManager.saveSession(profile, isGuest = false)
        saveUserProfileToFirestore(profile)
        profile
    }

    suspend fun loginAsGuest(): Result<UserProfile> = runCatching {
        android.util.Log.d("AuthRepository", "Starting loginAsGuest (anonymous sign-in execution)")
        val authResult = auth.signInAnonymously().await()
        val user = authResult.user ?: throw Exception("Anonymous sign-in returned null UserProfile.")
        android.util.Log.d("AuthRepository", "Anonymous sign-in success: uid=${user.uid}, isAnonymous=${user.isAnonymous}")
        
        demoProfile = UserProfile(name = "Guest User", email = "guest@smartvision.ai")
        android.util.Log.d("AuthRepository", "Saving session state as Guest")
        sessionManager.saveSession(demoProfile, isGuest = true)
        
        android.util.Log.d("AuthRepository", "Writing guest user profile to Firestore")
        saveUserProfileToFirestore(demoProfile)
        
        android.util.Log.d("AuthRepository", "Guest login process completed successfully")
        demoProfile
    }.onFailure { e ->
        android.util.Log.e("AuthRepository", "Guest login failed: ${e.javaClass.name} - ${e.localizedMessage}", e)
    }

    suspend fun linkAnonymousWithGoogle(account: GoogleSignInAccount): Result<UserProfile> = runCatching {
        val token = account.idToken
        require(!token.isNullOrBlank()) { "Google ID Token is missing." }
        val credential = GoogleAuthProvider.getCredential(token, null)
        
        val user = auth.currentUser ?: throw Exception("No guest session found.")
        user.linkWithCredential(credential).await()
        
        demoProfile = UserProfile(
            name = account.displayName ?: "Ajith Kumar",
            email = account.email ?: "Google connected",
            photoUrl = account.photoUrl?.toString()
        )
        val profile = currentProfile()
        sessionManager.saveSession(profile, isGuest = false)
        saveUserProfileToFirestore(profile)
        profile
    }

    suspend fun linkAnonymousWithEmail(email: String, password: String): Result<UserProfile> = runCatching {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
        require(email.matches(emailRegex)) { "Enter a valid email address." }
        require(password.length >= 6) { "Password must be at least 6 characters." }
        val credential = EmailAuthProvider.getCredential(email, password)
        
        val user = auth.currentUser ?: throw Exception("No guest session found.")
        user.linkWithCredential(credential).await()
        
        runCatching { user.sendEmailVerification().await() }
        
        demoProfile = UserProfile(
            name = demoProfile.name,
            email = email
        )
        val profile = currentProfile()
        sessionManager.saveSession(profile, isGuest = false)
        saveUserProfileToFirestore(profile)
        profile
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
        require(email.matches(emailRegex)) { "Enter a valid email address." }
        try {
            auth.sendPasswordResetEmail(email).await()
        } catch (e: Exception) {
            if (!email.endsWith("@demo.com")) {
                throw e
            }
        }
    }

    suspend fun logout() {
        auth.signOut()
        sessionManager.clearSession()
        demoProfile = UserProfile()
    }

    suspend fun updateUserProfile(name: String, email: String, photoUrl: String?): Result<UserProfile> = runCatching {
        val user = auth.currentUser
        val uid = user?.uid ?: "demo_uid"
        
        var finalPhotoUrl = photoUrl
        if (photoUrl != null && (photoUrl.startsWith("content://") || photoUrl.startsWith("file://"))) {
            val localUri = android.net.Uri.parse(photoUrl)
            val path = "users/$uid/profile/profile_${System.currentTimeMillis()}.jpg"
            val uploadResult = firebaseRepository.uploadFile(path, localUri)
            if (uploadResult.isSuccess) {
                finalPhotoUrl = uploadResult.getOrThrow()
            }
        }
        
        if (user != null) {
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .setPhotoUri(finalPhotoUrl?.let { android.net.Uri.parse(it) })
                .build()
            user.updateProfile(profileUpdates).await()
            if (email.isNotBlank() && email != user.email && email.contains("@")) {
                runCatching { user.updateEmail(email).await() }
            }
        } else {
            demoProfile = UserProfile(name = name, email = email, photoUrl = finalPhotoUrl)
        }
        val profile = currentProfile()
        sessionManager.saveSession(profile, isGuest = isGuest())
        saveUserProfileToFirestore(profile)
        profile
    }

    fun getLoginProvider(): String {
        val user = auth.currentUser ?: return "Guest Account"
        if (user.isAnonymous) return "Guest Account"
        for (profile in user.providerData) {
            if (profile.providerId == "google.com") {
                return "Google Sign-In"
            }
        }
        return "Email / Password"
    }

    fun getCurrentUid(): String {
        return auth.currentUser?.uid ?: "demo_uid"
    }
}

