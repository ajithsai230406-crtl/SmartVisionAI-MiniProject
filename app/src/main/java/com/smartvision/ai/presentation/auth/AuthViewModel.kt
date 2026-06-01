package com.smartvision.ai.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.smartvision.ai.data.repository.AuthRepository
import com.smartvision.ai.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _profile = MutableStateFlow(authRepository.currentProfile())
    val profile: StateFlow<UserProfile> = _profile

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun login(email: String, password: String, onSuccess: () -> Unit) = viewModelScope.launch {
        authRepository.emailLogin(email, password)
            .onSuccess {
                _profile.value = it
                onSuccess()
            }
            .onFailure { _message.value = it.message }
    }

    fun signup(name: String, email: String, password: String, onSuccess: () -> Unit) = viewModelScope.launch {
        authRepository.signup(name, email, password)
            .onSuccess {
                _profile.value = it
                onSuccess()
            }
            .onFailure { _message.value = it.message }
    }

    fun connectGoogle(account: GoogleSignInAccount?, onSuccess: () -> Unit = {}) = viewModelScope.launch {
        authRepository.connectGoogle(account)
            .onSuccess {
                _profile.value = it
                onSuccess()
            }
            .onFailure { _message.value = it.message }
    }
}
