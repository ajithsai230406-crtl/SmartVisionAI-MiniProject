package com.smartvision.ai.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AuthState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSignUp: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    app: Application
) : AndroidViewModel(app) {
    private val _s = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _s.asStateFlow()

    init {
        if (auth.currentUser != null) {
            _s.update { it.copy(isLoggedIn = true) }
        }
    }

    fun toggleMode() {
        _s.update { it.copy(isSignUp = !it.isSignUp, error = null) }
    }

    fun signIn(email: String, password: String) {
        if (!isValidEmail(email)) {
            _s.update { it.copy(error = "Enter a valid email address") }
            return
        }
        if (password.length < 6) {
            _s.update { it.copy(error = "Password must be at least 6 characters") }
            return
        }
        viewModelScope.launch {
            _s.update { it.copy(isLoading = true, error = null) }
            try {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                _s.update { it.copy(isLoggedIn = true) }
            } catch (e: Exception) {
                _s.update { it.copy(error = friendlyError(e)) }
            } finally {
                _s.update { it.copy(isLoading = false) }
            }
        }
    }

    fun signUp(email: String, password: String, name: String) {
        if (!isValidEmail(email)) {
            _s.update { it.copy(error = "Enter a valid email address") }
            return
        }
        if (password.length < 6) {
            _s.update { it.copy(error = "Password must be at least 6 characters") }
            return
        }
        if (name.isBlank()) {
            _s.update { it.copy(error = "Please enter your name") }
            return
        }
        viewModelScope.launch {
            _s.update { it.copy(isLoading = true, error = null) }
            try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                result.user?.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()
                )?.await()
                _s.update { it.copy(isLoggedIn = true) }
            } catch (e: Exception) {
                _s.update { it.copy(error = friendlyError(e)) }
            } finally {
                _s.update { it.copy(isLoading = false) }
            }
        }
    }

    fun signInWithGoogleToken(idToken: String) {
        viewModelScope.launch {
            _s.update { it.copy(isLoading = true, error = null) }
            try {
                val cred = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(cred).await()
                _s.update { it.copy(isLoggedIn = true) }
            } catch (e: Exception) {
                _s.update { it.copy(error = friendlyError(e)) }
            } finally {
                _s.update { it.copy(isLoading = false) }
            }
        }
    }

    fun skipLogin() {
        _s.update { it.copy(isLoggedIn = true) }
    }

    fun signOut() {
        auth.signOut()
        _s.update { it.copy(isLoggedIn = false) }
    }

    private fun isValidEmail(email: String) =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    private fun friendlyError(e: Exception) = when {
        e.message?.contains("password") == true -> "Incorrect password. Please try again."
        e.message?.contains("no user") == true -> "No account found with this email."
        e.message?.contains("already in use") == true -> "This email is already registered."
        e.message?.contains("network") == true -> "Network error. Check your connection."
        else -> e.message ?: "Authentication failed"
    }
}
