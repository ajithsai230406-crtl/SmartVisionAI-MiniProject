package com.smartvision.ai.ui.screens.medical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.domain.models.ScanResult
import com.smartvision.ai.domain.usecase.MedicalScanUseCase
import com.smartvision.ai.domain.usecase.ResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MedicalScannerUiState(
    val result:    ScanResult? = null,
    val imagePath: String?     = null,
    val isLoading: Boolean     = false,
    val error:     String?     = null
)

@HiltViewModel
class MedicalScannerViewModel @Inject constructor(
    private val useCase:    MedicalScanUseCase,
    private val repository: ResultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicalScannerUiState())
    val uiState: StateFlow<MedicalScannerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val path   = repository.getCachedImagePath()
            val cached = repository.getCachedResult()
            _uiState.update { currentState: MedicalScannerUiState ->
                currentState.copy(
                    imagePath = path,
                    result = cached
                )
            }
        }
    }
}
