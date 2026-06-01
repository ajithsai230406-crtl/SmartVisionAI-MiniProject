package com.smartvision.ai.ui.screens.objectdetection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.domain.usecase.DetectObjectsUseCase
import com.smartvision.ai.domain.usecase.ResultRepository
import com.smartvision.ai.domain.usecase.SaveScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ObjectDetectionState(
    val result:          ScanResult?          = null,
    val imagePath:       String?              = null,
    val isLoading:       Boolean              = false,
    val error:           String?              = null
)

@HiltViewModel
class ObjectDetectionViewModel @Inject constructor(
    private val detectObjects: DetectObjectsUseCase,
    private val repository:    ResultRepository,
    private val saveScan:      SaveScanUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObjectDetectionState())
    val uiState: StateFlow<ObjectDetectionState> = _uiState.asStateFlow()

    // Screen-compatible properties
    val state:   StateFlow<ScanResult?> = _uiState.map { it.result }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val path:    StateFlow<String?>     = _uiState.map { it.imagePath }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val loading: StateFlow<Boolean>     = _uiState.map { it.isLoading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(
                imagePath = repository.getCachedImagePath(),
                result = repository.getCachedResult()
            )}
        }
    }

    fun reprocess(bitmapPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val bmp = android.graphics.BitmapFactory.decodeFile(bitmapPath)
                if (bmp != null) {
                    val r = detectObjects(bmp)
                    _uiState.update { it.copy(result = r, isLoading = false) }
                    repository.cacheResult(r)
                    
                    if (r is ScanResult.ObjectDetectionResult) {
                        val labelsText = r.objects.joinToString { it.label }
                        saveScan("object_detection", "${r.objects.size} objects: $labelsText")
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Could not load image") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun simulateDetection() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(700)
            val objects = listOf(
                DetectedObject("Plant",  0.98f, BoundingBox(0.05f, 0.05f, 0.30f, 0.42f)),
                DetectedObject("Sofa",   0.96f, BoundingBox(0.35f, 0.38f, 0.95f, 0.88f)),
                DetectedObject("Table",  0.93f, BoundingBox(0.28f, 0.60f, 0.82f, 0.95f)),
                DetectedObject("Person", 0.87f, BoundingBox(0.10f, 0.08f, 0.48f, 0.78f))
            )
            val result = ScanResult.ObjectDetectionResult(objects)
            _uiState.update { it.copy(isLoading = false, result = result) }
            
            val labelsText = objects.joinToString { it.label }
            saveScan("object_detection", "${objects.size} objects: $labelsText")
        }
    }

    fun clearResults() {
        _uiState.update { it.copy(result = null) }
    }
}
