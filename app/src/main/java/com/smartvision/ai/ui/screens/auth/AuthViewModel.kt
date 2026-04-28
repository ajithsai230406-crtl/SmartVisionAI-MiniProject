package com.smartvision.ai.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val isLoading:  Boolean = false,
    val error:      String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Already logged in?
        if (auth.currentUser != null) {
            _uiState.update { it.copy(isLoggedIn = true) }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                _uiState.update { it.copy(isLoggedIn = true, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Sign-in failed") }
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                auth.createUserWithEmailAndPassword(email.trim(), password).await()
                _uiState.update { it.copy(isLoggedIn = true, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Sign-up failed") }
            }
        }
    }

    /**
     * Called after Google Sign-In activity returns an ID token.
     * Wire the Google Sign-In launcher in LoginScreen, pass idToken here.
     */
    fun signInWithGoogleToken(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                _uiState.update { it.copy(isLoggedIn = true, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Google sign-in failed") }
            }
        }
    }

    /** Placeholder — actual Google Sign-In launcher lives in LoginScreen composable */
    fun signInWithGoogle() {
        // Trigger the Google Sign-In intent via Activity Result API in LoginScreen
    }

    fun signOut() {
        auth.signOut()
        _uiState.update { it.copy(isLoggedIn = false) }
    }
}
