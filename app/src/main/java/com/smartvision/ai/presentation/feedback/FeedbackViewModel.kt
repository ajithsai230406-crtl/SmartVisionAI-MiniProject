package com.smartvision.ai.presentation.feedback

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.repository.AuthRepository
import com.smartvision.ai.data.repository.FirebaseRepository
import com.smartvision.ai.domain.model.FeedbackEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FeedbackSubmitState {
    object Idle : FeedbackSubmitState()
    object Submitting : FeedbackSubmitState()
    object Success : FeedbackSubmitState()
    data class Error(val message: String) : FeedbackSubmitState()
}

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseRepository: FirebaseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val userProfile = authRepository.currentProfile()

    // Form inputs
    val category = MutableStateFlow("General Feedback")
    val subject = MutableStateFlow("")
    val message = MutableStateFlow("")
    val rating = MutableStateFlow(5)
    val attachmentUri = MutableStateFlow<Uri?>(null)
    val attachmentName = MutableStateFlow("")

    private val _submitState = MutableStateFlow<FeedbackSubmitState>(FeedbackSubmitState.Idle)
    val submitState: StateFlow<FeedbackSubmitState> = _submitState.asStateFlow()

    private val _userHistory = MutableStateFlow<List<FeedbackEntry>>(emptyList())
    val userHistory: StateFlow<List<FeedbackEntry>> = _userHistory.asStateFlow()

    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

    private val _allFeedback = MutableStateFlow<List<FeedbackEntry>>(emptyList())
    val allFeedback: StateFlow<List<FeedbackEntry>> = _allFeedback.asStateFlow()

    private val _isLoadingAdmin = MutableStateFlow(false)
    val isLoadingAdmin: StateFlow<Boolean> = _isLoadingAdmin.asStateFlow()

    fun setAttachment(uri: Uri?, name: String) {
        attachmentUri.value = uri
        attachmentName.value = name
    }

    fun clearForm() {
        category.value = "General Feedback"
        subject.value = ""
        message.value = ""
        rating.value = 5
        attachmentUri.value = null
        attachmentName.value = ""
        _submitState.value = FeedbackSubmitState.Idle
    }

    fun submitFeedback() = viewModelScope.launch {
        if (subject.value.isBlank()) {
            _submitState.value = FeedbackSubmitState.Error("Subject cannot be empty")
            return@launch
        }
        if (message.value.isBlank()) {
            _submitState.value = FeedbackSubmitState.Error("Message cannot be empty")
            return@launch
        }

        _submitState.value = FeedbackSubmitState.Submitting

        val feedbackId = java.util.UUID.randomUUID().toString()
        var uploadedUrl: String? = null

        attachmentUri.value?.let { uri ->
            val filename = attachmentName.value.ifBlank { "attachment_${System.currentTimeMillis()}" }
            val path = "FeedbackAttachments/${feedbackId}_${filename}"
            val uploadResult = firebaseRepository.uploadFile(path, uri)
            uploadResult.onSuccess { url ->
                uploadedUrl = url
            }.onFailure { err ->
                _submitState.value = FeedbackSubmitState.Error("Attachment upload failed: ${err.localizedMessage}")
                return@launch
            }
        }

        val devInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
        val appVer = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

        val feedbackEntry = FeedbackEntry(
            feedbackId = feedbackId,
            userName = userProfile.name,
            email = userProfile.email,
            category = category.value,
            subject = subject.value,
            message = message.value,
            rating = rating.value,
            imageUrl = uploadedUrl,
            deviceInfo = devInfo,
            appVersion = appVer,
            timestamp = System.currentTimeMillis(),
            status = "Pending"
        )

        firebaseRepository.saveFeedback(feedbackEntry)
            .onSuccess {
                _submitState.value = FeedbackSubmitState.Success
                loadUserHistory()
            }
            .onFailure { err ->
                _submitState.value = FeedbackSubmitState.Error("Failed to save feedback: ${err.localizedMessage}")
            }
    }

    fun loadUserHistory() = viewModelScope.launch {
        _isLoadingHistory.value = true
        firebaseRepository.getUserFeedbackHistory(userProfile.email)
            .onSuccess { list ->
                _userHistory.value = list
            }
        _isLoadingHistory.value = false
    }

    fun updatePendingFeedback(feedback: FeedbackEntry) = viewModelScope.launch {
        if (feedback.status != "Pending") return@launch
        firebaseRepository.updateFeedback(feedback)
            .onSuccess {
                loadUserHistory()
            }
    }

    fun deleteUserFeedback(feedbackId: String) = viewModelScope.launch {
        firebaseRepository.deleteFeedback(feedbackId)
            .onSuccess {
                loadUserHistory()
            }
    }

    fun loadAllFeedback() = viewModelScope.launch {
        _isLoadingAdmin.value = true
        firebaseRepository.getAllFeedback()
            .onSuccess { list ->
                _allFeedback.value = list
            }
        _isLoadingAdmin.value = false
    }

    fun updateFeedbackStatus(feedbackId: String, status: String) = viewModelScope.launch {
        firebaseRepository.updateFeedbackStatus(feedbackId, status)
            .onSuccess {
                loadAllFeedback()
            }
    }

    fun deleteFeedbackEntry(feedbackId: String) = viewModelScope.launch {
        firebaseRepository.deleteFeedback(feedbackId)
            .onSuccess {
                loadAllFeedback()
            }
    }
}
