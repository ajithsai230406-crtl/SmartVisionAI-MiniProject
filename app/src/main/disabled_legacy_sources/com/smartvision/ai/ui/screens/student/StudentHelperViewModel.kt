package com.smartvision.ai.ui.screens.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.domain.models.ScanResult
import com.smartvision.ai.domain.usecase.StudentHelperUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentHelperUiState(
    val manualQuery: String      = "",
    val result:      ScanResult? = null,
    val isLoading:   Boolean     = false
)

@HiltViewModel
class StudentHelperViewModel @Inject constructor(
    private val useCase: StudentHelperUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentHelperUiState())
    val uiState: StateFlow<StudentHelperUiState> = _uiState.asStateFlow()

    fun setManualQuery(q: String) { _uiState.update { it.copy(manualQuery = q) } }

    fun askQuestion() {
        val q = _uiState.value.manualQuery.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, result = null) }
            val result = useCase.explainText(q)
            _uiState.update { it.copy(isLoading = false, result = result) }
        }
    }

    fun askFromPrompt(prompt: String) {
        _uiState.update { it.copy(manualQuery = prompt) }
        askQuestion()
    }
}
