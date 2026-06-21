package com.smartvision.ai.student.presentation

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.repository.AuthRepository
import com.smartvision.ai.data.repository.FirebaseRepository
import com.smartvision.ai.domain.model.StudyHistoryItem
import com.smartvision.ai.ocr.data.OcrRecognitionRepository
import com.smartvision.ai.student.data.StudentAiRepository
import com.smartvision.ai.utils.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

sealed class StudentUiState {
    object Idle : StudentUiState()
    object Loading : StudentUiState()
    data class Success(val response: String) : StudentUiState()
    data class Error(val message: String) : StudentUiState()
}

@HiltViewModel
class StudentHelperViewModel @Inject constructor(
    private val aiRepo: StudentAiRepository,
    private val ocrRepo: OcrRecognitionRepository,
    private val imageUtils: ImageUtils,
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Unified Ask Question text box state
    private val _questionText = MutableStateFlow("")
    val questionText: StateFlow<String> = _questionText.asStateFlow()

    // Response and Loading states
    private val _uiState = MutableStateFlow<StudentUiState>(StudentUiState.Idle)
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    // Firestore History states
    private val _firestoreHistoryList = MutableStateFlow<List<StudyHistoryItem>>(emptyList())
    val firestoreHistoryList: StateFlow<List<StudyHistoryItem>> = _firestoreHistoryList.asStateFlow()

    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

    private val _isLastPage = MutableStateFlow(false)
    val isLastPage: StateFlow<Boolean> = _isLastPage.asStateFlow()

    private var lastVisibleTimestamp: Long? = null

    // TTS Engine
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        refreshHistory()
    }

    fun updateQuestion(q: String) {
        _questionText.value = q
    }

    fun clearAll() {
        _questionText.value = ""
        _uiState.value = StudentUiState.Idle
        stopSpeaking()
    }

    fun generateAnswer(questionOverride: String? = null) {
        val finalQuestion = (questionOverride ?: _questionText.value).trim()
        if (finalQuestion.isBlank()) {
            _uiState.value = StudentUiState.Error("Empty Question: Please type, scan, or say a question first.")
            return
        }

        _uiState.value = StudentUiState.Loading

        viewModelScope.launch {
            aiRepo.getAiAnswer(finalQuestion)
                .onSuccess { response ->
                    _uiState.value = StudentUiState.Success(response)
                    refreshHistory()
                }
                .onFailure { error ->
                    val errorMsg = error.message ?: "Failed to generate answer."
                    _uiState.value = StudentUiState.Error(errorMsg)
                }
        }
    }

    // ── Image enhancement for better OCR ────────────────────────────────────
    private fun enhanceBitmapForOcr(bitmap: Bitmap): Bitmap {
        // 1. Upscale small images for better OCR accuracy
        val minDimension = 1024
        var enhanced = if (bitmap.width < minDimension && bitmap.height < minDimension) {
            val scale = minDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else bitmap

        // 2. Apply contrast enhancement using ColorMatrix
        try {
            val result = Bitmap.createBitmap(enhanced.width, enhanced.height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(result)
            val paint = android.graphics.Paint()

            // Boost contrast by 1.3x and slight brightness increase
            val contrast = 1.3f
            val brightness = 10f
            val cm = android.graphics.ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            ))
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
            canvas.drawBitmap(enhanced, 0f, 0f, paint)
            enhanced = result
        } catch (_: Exception) {
            // Fall back to unenhanced bitmap
        }

        return enhanced
    }

    // ── OCR processing for camera captures ───────────────────────────────────
    fun processImageForOcr(bitmap: Bitmap) {
        _uiState.value = StudentUiState.Loading
        viewModelScope.launch {
            val enhanced = enhanceBitmapForOcr(bitmap)
            aiRepo.transcribeImage(enhanced)
                .onSuccess { recognizedText ->
                    val cleanText = recognizedText.trim()
                    if (cleanText.isBlank()) {
                        fallbackToLocalOcr(enhanced, "OCR Failed: No readable text detected in captured image.")
                    } else {
                        _questionText.value = cleanText
                        generateAnswer(cleanText)
                    }
                }
                .onFailure {
                    fallbackToLocalOcr(enhanced, "OCR Failed: Could not process captured image.")
                }
        }
    }

    // ── OCR processing for gallery picker URIs ───────────────────────────────
    fun processUriForOcr(uri: Uri) {
        _uiState.value = StudentUiState.Loading
        viewModelScope.launch {
            val bitmap = imageUtils.loadBitmapFromUri(context, uri)
            if (bitmap == null) {
                _uiState.value = StudentUiState.Error("OCR Failed: Could not load selected image.")
                return@launch
            }
            val enhanced = enhanceBitmapForOcr(bitmap)
            aiRepo.transcribeImage(enhanced)
                .onSuccess { recognizedText ->
                    val cleanText = recognizedText.trim()
                    if (cleanText.isBlank()) {
                        fallbackToLocalOcr(enhanced, "OCR Failed: No readable text detected in selected image.")
                    } else {
                        _questionText.value = cleanText
                        generateAnswer(cleanText)
                    }
                }
                .onFailure {
                    fallbackToLocalOcr(enhanced, "OCR Failed: Could not process gallery image.")
                }
        }
    }

    private fun fallbackToLocalOcr(bitmap: Bitmap, originalError: String) {
        viewModelScope.launch {
            ocrRepo.recognizeAutoScriptFromBitmap(bitmap)
                .onSuccess { mlText ->
                    val cleanText = mlText.text.trim()
                    if (cleanText.isBlank()) {
                        _uiState.value = StudentUiState.Error(originalError)
                    } else {
                        _questionText.value = cleanText
                        generateAnswer(cleanText)
                    }
                }
                .onFailure {
                    _uiState.value = StudentUiState.Error(originalError)
                }
        }
    }

    // ── Text-To-Speech (Read Aloud) ──────────────────────────────────────────
    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    // ── Firestore History management ───────────────────────────────────────────
    fun refreshHistory() {
        lastVisibleTimestamp = null
        _isLastPage.value = false
        _firestoreHistoryList.value = emptyList()
        loadNextPage()
    }

    fun loadNextPage() {
        if (_isLastPage.value || _isLoadingHistory.value) return
        _isLoadingHistory.value = true
        
        viewModelScope.launch {
            val uid = authRepository.getCurrentUid()
            firebaseRepository.getStudyHistoryPaginated(uid, limit = 10, lastVisibleTimestamp = lastVisibleTimestamp)
                .onSuccess { items ->
                    if (items.isEmpty()) {
                        _isLastPage.value = true
                    } else {
                        lastVisibleTimestamp = items.last().timestamp
                        _firestoreHistoryList.update { current -> (current + items).distinctBy { it.id } }
                        if (items.size < 10) {
                            _isLastPage.value = true
                        }
                    }
                }
                .onFailure {
                    // Fail silently or handle error
                }
            _isLoadingHistory.value = false
        }
    }

    fun reopenHistoryItem(item: StudyHistoryItem) {
        _questionText.value = item.question
        _uiState.value = StudentUiState.Success(item.answer)
        stopSpeaking()
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch {
            firebaseRepository.deleteStudyHistoryItem(id)
                .onSuccess {
                    _firestoreHistoryList.update { list -> list.filter { it.id != id } }
                }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUid()
            firebaseRepository.clearAllStudyHistory(uid)
                .onSuccess {
                    _firestoreHistoryList.value = emptyList()
                    _isLastPage.value = true
                    lastVisibleTimestamp = null
                }
        }
    }

    fun exportHistory(onResult: (Result<Uri>) -> Unit) {
        viewModelScope.launch {
            val list = _firestoreHistoryList.value
            if (list.isEmpty()) {
                onResult(Result.failure(Exception("History is empty. Nothing to export.")))
                return@launch
            }
            
            val textBuilder = java.lang.StringBuilder()
            textBuilder.append("=== SMART VISION AI - STUDY HISTORY ===\n\n")
            list.forEach { item ->
                val dateStr = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(
                    java.util.Date(item.timestamp)
                )
                textBuilder.append("Date: $dateStr\n")
                textBuilder.append("Subject: ${item.subject}\n")
                textBuilder.append("Question: ${item.question}\n")
                textBuilder.append("Answer:\n${item.answer}\n")
                textBuilder.append("---------------------------------------\n\n")
            }
            
            val filename = "StudyHistory_${System.currentTimeMillis()}.txt"
            val result = com.smartvision.ai.utils.FileExportHelper.saveTextFile(context, filename, textBuilder.toString())
            onResult(result)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
