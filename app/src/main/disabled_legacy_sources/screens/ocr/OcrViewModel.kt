package com.smartvision.ai.ui.screens.ocr

import android.app.Application
import android.content.*
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.*
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OcrState(
    val imagePath:      String?         = null,
    val fullText:       String          = "",
    val textBlocks:     List<TextBlock> = emptyList(),
    val selectedText:   String          = "",
    val translatedText: String          = "",
    val isLoading:      Boolean         = false
)

@HiltViewModel
class OcrViewModel @Inject constructor(
    private val ocrUseCase: OcrUseCase,
    private val resultRepository: ResultRepository,
    app: Application
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(OcrState())
    val state: StateFlow<OcrState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val path = resultRepository.getCachedImagePath()
            val cached = resultRepository.getCachedResult()
            _state.update { it.copy(
                imagePath = path,
                fullText = if (cached is ScanResult.OcrResult) cached.rawText else it.fullText,
                textBlocks = if (cached is ScanResult.OcrResult) cached.textBlocks else it.textBlocks
            ) }
        }
    }

    // Called as user drags selection box
    fun selectArea(rect: Rect) {
        val blocks = _state.value.textBlocks
        val selectedText = blocks.filter { block ->
            val b = block.boundingBox
            // Check if block centre is inside the selection
            val cx = (b.left + b.right) / 2f
            val cy = (b.top  + b.bottom) / 2f
            cx in rect.left..rect.right &&
            cy in rect.top..rect.bottom
        }.joinToString(" ") { it.text }

        _state.update { it.copy(selectedText = selectedText) }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedText = "", translatedText = "") }
    }

    fun copy() {
        val text = _state.value.selectedText.ifEmpty { _state.value.fullText }
        if (text.isEmpty()) return
        val clip = ClipData.newPlainText("OCR Text", text)
        val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(clip)
    }

    fun speak() {
        // TTS implementation or navigation to TTS screen
    }
}
