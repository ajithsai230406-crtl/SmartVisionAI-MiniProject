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

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    val isLoggedIn: Flow<Boolean> = authRepository.isLoggedIn
    val onboardingCompleted: Flow<Boolean> = authRepository.onboardingCompleted

    val profile: StateFlow<UserProfile> = authRepository.isLoggedIn
        .map { authRepository.currentProfile() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authRepository.currentProfile()
        )

    fun login(email: String, password: String, onSuccess: () -> Unit) = viewModelScope.launch {
        authRepository.emailLogin(email, password)
            .onSuccess {
                onSuccess()
            }
            .onFailure { _message.value = it.message }
    }

    fun signup(name: String, email: String, password: String, onSuccess: () -> Unit) = viewModelScope.launch {
        authRepository.signup(name, email, password)
            .onSuccess {
                onSuccess()
            }
            .onFailure { _message.value = it.message }
    }

    fun connectGoogle(account: GoogleSignInAccount?, onSuccess: () -> Unit = {}) = viewModelScope.launch {
        authRepository.connectGoogle(account)
            .onSuccess {
                onSuccess()
            }
            .onFailure { _message.value = it.message }
    }

    fun loginAsGuest(onSuccess: () -> Unit) = viewModelScope.launch {
        authRepository.loginAsGuest()
            .onSuccess { onSuccess() }
            .onFailure { _message.value = it.message }
    }

    fun logout(onSuccess: () -> Unit) = viewModelScope.launch {
        authRepository.logout()
        onSuccess()
    }

    fun setOnboardingCompleted(completed: Boolean) = viewModelScope.launch {
        authRepository.setOnboardingCompleted(completed)
    }

    fun getLoginProvider(): String = authRepository.getLoginProvider()

    fun updateUserProfile(name: String, email: String, photoUrl: String?, onSuccess: () -> Unit) = viewModelScope.launch {
        authRepository.updateUserProfile(name, email, photoUrl)
            .onSuccess { onSuccess() }
            .onFailure { _message.value = it.message }
    }
}

