package com.smartvision.ai.ui.screens.ocr

import android.app.Application
import android.content.*
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.*
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.domain.usecase.OcrUseCase
import com.smartvision.ai.domain.usecase.ResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OcrUiState(
    val imagePath:    String?         = null,
    val fullText:     String          = "",
    val textBlocks:   List<TextBlock> = emptyList(),
    val selectedText: String          = "",
    val isLoading:    Boolean         = false
)

@HiltViewModel
class OcrViewModel @Inject constructor(
    private val ocrUseCase:  OcrUseCase,
    private val repository:  ResultRepository,
    app: Application
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(OcrUiState())
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val path   = repository.getCachedImagePath()
            val cached = repository.getCachedResult()
            _uiState.update { it.copy(imagePath = path) }
            if (cached is ScanResult.OcrResult)
                _uiState.update { it.copy(fullText = cached.rawText, textBlocks = cached.textBlocks) }
        }
    }

    fun selectArea(normalizedRect: Rect) {
        val selected = _uiState.value.textBlocks.filter { block ->
            val b  = block.boundingBox
            val cx = (b.left + b.right) / 2f
            val cy = (b.top  + b.bottom) / 2f
            cx in normalizedRect.left..normalizedRect.right &&
            cy in normalizedRect.top..normalizedRect.bottom
        }
        _uiState.update { it.copy(selectedText = selected.joinToString(" ") { it.text }) }
    }

    fun clearSelection() { _uiState.update { it.copy(selectedText = "") } }

    fun copySelected() {
        val text = _uiState.value.selectedText.ifEmpty { _uiState.value.fullText }
        val cm   = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("OCR", text))
    }

    fun translateSelected() { /* navigate to translator */ }
    fun speakSelected()     { /* TtsManager.speak() */ }
}
