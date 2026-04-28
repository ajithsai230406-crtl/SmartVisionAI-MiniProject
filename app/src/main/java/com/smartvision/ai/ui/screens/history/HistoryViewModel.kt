package com.smartvision.ai.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.smartvision.ai.domain.models.ScanHistoryItem
import com.smartvision.ai.domain.usecase.ResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val items:       List<ScanHistoryItem> = emptyList(),
    val selectedIds: Set<String>           = emptySet(),
    val isLoading:   Boolean               = false,
    val error:       String?               = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ResultRepository,
    private val auth:       FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    fun loadHistory() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getHistory(uid).fold(
                onSuccess = { items -> _uiState.update { it.copy(items = items, isLoading = false) } },
                onFailure = { err  -> _uiState.update { it.copy(error = err.message, isLoading = false) } }
            )
        }
    }

    fun toggleSelection(id: String) {
        _uiState.update { state ->
            val newSet = if (id in state.selectedIds)
                state.selectedIds - id
            else
                state.selectedIds + id
            state.copy(selectedIds = newSet)
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds.toList()
            ids.forEach { id -> repository.deleteHistory(id) }
            _uiState.update { state ->
                state.copy(
                    items       = state.items.filter { it.id !in ids },
                    selectedIds = emptySet()
                )
            }
        }
    }
}
