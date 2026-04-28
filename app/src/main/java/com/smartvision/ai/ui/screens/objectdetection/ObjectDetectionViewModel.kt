package com.smartvision.ai.ui.screens.objectdetection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.domain.models.ScanResult
import com.smartvision.ai.domain.usecase.DetectObjectsUseCase
import com.smartvision.ai.domain.usecase.ResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ObjectDetectionUiState(
    val result:    ScanResult? = null,
    val imagePath: String?     = null,
    val isLoading: Boolean     = false,
    val error:     String?     = null
)

@HiltViewModel
class ObjectDetectionViewModel @Inject constructor(
    private val useCase:    DetectObjectsUseCase,
    private val repository: ResultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObjectDetectionUiState())
    val uiState: StateFlow<ObjectDetectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val path   = repository.getCachedImagePath()
            val cached = repository.getCachedResult()
            _uiState.update { it.copy(imagePath = path, result = cached) }
        }
    }

    fun reprocess(bitmapPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val bmp    = android.graphics.BitmapFactory.decodeFile(bitmapPath)
                val result = useCase(bmp)
                _uiState.update { it.copy(result = result, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
