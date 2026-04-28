package com.smartvision.ai.ui.screens.waste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.domain.models.ScanResult
import com.smartvision.ai.domain.usecase.ClassifyWasteUseCase
import com.smartvision.ai.domain.usecase.ResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WasteClassifierUiState(
    val result:    ScanResult? = null,
    val imagePath: String?     = null,
    val isLoading: Boolean     = false
)

@HiltViewModel
class WasteClassifierViewModel @Inject constructor(
    private val useCase:    ClassifyWasteUseCase,
    private val repository: ResultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WasteClassifierUiState())
    val uiState: StateFlow<WasteClassifierUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val path   = repository.getCachedImagePath()
            val cached = repository.getCachedResult()
            _uiState.update { it.copy(imagePath = path, result = cached) }
        }
    }
}
