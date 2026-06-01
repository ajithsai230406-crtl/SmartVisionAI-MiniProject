package com.example.smartvisionai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartvisionai.data.model.ScanHistoryEntity
import com.example.smartvisionai.repository.ScanHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: ScanHistoryRepository
) : ViewModel() {

    val history: StateFlow<List<ScanHistoryEntity>> = repo.getRecentHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteItem(item: ScanHistoryEntity) {
        viewModelScope.launch { repo.deleteItem(item) }
    }

    fun clearAll() {
        viewModelScope.launch { repo.clearAll() }
    }
}
