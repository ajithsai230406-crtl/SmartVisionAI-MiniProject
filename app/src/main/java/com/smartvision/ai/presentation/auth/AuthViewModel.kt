package com.smartvision.ai.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.smartvision.ai.data.repository.AuthRepository
import com.smartvision.ai.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    data class Loading(val message: String) : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val isLoggedIn: Flow<Boolean> = authRepository.isLoggedIn
    val onboardingCompleted: Flow<Boolean> = authRepository.onboardingCompleted

    val profile: StateFlow<UserProfile> = authRepository.isLoggedIn
        .map { authRepository.currentProfile() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authRepository.currentProfile()
        )

    fun clearAuthState() {
        _authState.value = AuthState.Idle
    }

    private fun getFriendlyErrorMessage(throwable: Throwable): String {
        android.util.Log.e("AuthViewModel", "Authentication exception caught", throwable)
        val msg = throwable.localizedMessage ?: throwable.message ?: "Unknown error"
        return when (throwable) {
            is com.google.firebase.auth.FirebaseAuthException -> {
                android.util.Log.e("AuthViewModel", "FirebaseAuthException: errorCode=${throwable.errorCode}", throwable)
                if (throwable.errorCode.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) || msg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true)) {
                    "Firebase Auth Error: Please enable the Anonymous/Google provider in your Firebase Console (Build > Authentication > Sign-in method)."
                } else {
                    "Firebase Auth Error (${throwable.errorCode}): $msg"
                }
            }
            is com.google.firebase.FirebaseNetworkException -> {
                android.util.Log.e("AuthViewModel", "FirebaseNetworkException occurred", throwable)
                "Firebase Network Error: $msg"
            }
            is com.google.firebase.FirebaseApiNotAvailableException -> {
                android.util.Log.e("AuthViewModel", "FirebaseApiNotAvailableException occurred", throwable)
                "Firebase API Not Available: $msg"
            }
            else -> {
                android.util.Log.e("AuthViewModel", "General Auth Exception: ${throwable.javaClass.simpleName}", throwable)
                if (msg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true)) {
                    "Firebase Auth Error: Please enable the Anonymous/Google provider in your Firebase Console (Build > Authentication > Sign-in method)."
                } else {
                    "Authentication failed (${throwable.javaClass.simpleName}): $msg"
                }
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) = viewModelScope.launch {
        _authState.value = AuthState.Loading("Signing in...")
        authRepository.emailLogin(email, password)
            .onSuccess {
                _authState.value = AuthState.Success
                onSuccess()
            }
            .onFailure {
                val errorMsg = getFriendlyErrorMessage(it)
                _authState.value = AuthState.Error(errorMsg)
                _message.value = errorMsg
            }
    }

    fun signup(name: String, email: String, password: String, onSuccess: () -> Unit) = viewModelScope.launch {
        _authState.value = AuthState.Loading("Creating Account...")
        authRepository.signup(name, email, password)
            .onSuccess {
                _authState.value = AuthState.Success
                onSuccess()
            }
            .onFailure {
                val errorMsg = getFriendlyErrorMessage(it)
                _authState.value = AuthState.Error(errorMsg)
                _message.value = errorMsg
            }
    }

    fun connectGoogle(account: GoogleSignInAccount?, onSuccess: () -> Unit = {}) = viewModelScope.launch {
        _authState.value = AuthState.Loading("Signing in with Google...")
        authRepository.connectGoogle(account)
            .onSuccess {
                _authState.value = AuthState.Success
                onSuccess()
            }
            .onFailure {
                val errorMsg = getFriendlyErrorMessage(it)
                _authState.value = AuthState.Error(errorMsg)
                _message.value = errorMsg
            }
    }

    fun loginAsGuest(onSuccess: () -> Unit) = viewModelScope.launch {
        _authState.value = AuthState.Loading("Signing in...")
        authRepository.loginAsGuest()
            .onSuccess {
                _authState.value = AuthState.Success
                onSuccess()
            }
            .onFailure {
                val errorMsg = getFriendlyErrorMessage(it)
                _authState.value = AuthState.Error(errorMsg)
                _message.value = errorMsg
            }
    }

    fun logout(onSuccess: () -> Unit) = viewModelScope.launch {
        authRepository.logout()
        onSuccess()
    }

    fun setErrorMessage(msg: String?) {
        _message.value = msg
    }

    fun setOnboardingCompleted(completed: Boolean) = viewModelScope.launch {
        authRepository.setOnboardingCompleted(completed)
    }

    fun getLoginProvider(): String = authRepository.getLoginProvider()

    fun isGuestUser(): Boolean = authRepository.isGuest()

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit) = viewModelScope.launch {
        authRepository.sendPasswordResetEmail(email)
            .onSuccess {
                _message.value = "Password reset email sent."
                onSuccess()
            }
            .onFailure { _message.value = it.message }
    }

    fun linkGoogle(account: GoogleSignInAccount, onSuccess: () -> Unit) = viewModelScope.launch {
        authRepository.linkAnonymousWithGoogle(account)
            .onSuccess {
                _message.value = "Account successfully upgraded with Google!"
                onSuccess()
            }
            .onFailure { _message.value = it.message }
    }

    fun linkEmail(email: String, password: String, onSuccess: () -> Unit) = viewModelScope.launch {
        authRepository.linkAnonymousWithEmail(email, password)
            .onSuccess {
                _message.value = "Account linked! Verification email sent."
                onSuccess()
            }
            .onFailure { _message.value = it.message }
    }

    fun updateUserProfile(name: String, email: String, photoUrl: String?, onSuccess: () -> Unit) = viewModelScope.launch {
        authRepository.updateUserProfile(name, email, photoUrl)
            .onSuccess { onSuccess() }
            .onFailure { _message.value = it.message }
    }

    fun getStartDestination(): String {
        return when {
            authRepository.isSessionActiveAndVerified() -> com.smartvision.ai.compose.navigation.Routes.HOME
            else -> com.smartvision.ai.compose.navigation.Routes.ONBOARDING
        }
    }
}

