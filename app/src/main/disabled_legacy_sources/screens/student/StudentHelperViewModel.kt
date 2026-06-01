package com.smartvision.ai.ui.screens.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.domain.models.ScanResult
import com.smartvision.ai.domain.usecase.SaveScanUseCase
import com.smartvision.ai.domain.usecase.StudentHelperUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentHelperState(
    val question:   String  = "",
    val answer:     String  = "",
    val isLoading:  Boolean = false,
    val error:      String? = null,
    val history:    List<Pair<String, String>> = emptyList()
)

@HiltViewModel
class StudentHelperViewModel @Inject constructor(
    private val studentHelper: StudentHelperUseCase,
    private val saveScan:      SaveScanUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentHelperState())
    val uiState: StateFlow<StudentHelperState> = _uiState.asStateFlow()

    fun setQuestion(q: String) = _uiState.update { it.copy(question = q, error = null) }

    fun ask() {
        val q = _uiState.value.question.trim()
        if (q.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a question") }
            return
        }

        _uiState.update { it.copy(isLoading = true, answer = "", error = null) }
        viewModelScope.launch {
            // Using explainText because 'q' is a String. 'invoke' expects a Bitmap.
            val result = studentHelper.explainText(q)
            when (result) {
                is ScanResult.StudentHelperResult -> {
                    val answer = result.aiExplanation
                    val hist = (_uiState.value.history + Pair(q, answer)).takeLast(10)
                    _uiState.update { it.copy(isLoading = false, answer = answer, history = hist) }
                    saveScan("student_helper", "Q: ${q.take(60)}")
                }
                is ScanResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to get answer") }
                }
            }
        }
    }

    fun clearAnswer() = _uiState.update { it.copy(answer = "", question = "") }
}
