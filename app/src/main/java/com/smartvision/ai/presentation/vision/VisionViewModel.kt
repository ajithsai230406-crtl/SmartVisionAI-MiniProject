package com.smartvision.ai.presentation.vision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.data.repository.VisionRepository
import com.smartvision.ai.domain.model.VisionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VisionViewModel @Inject constructor(
    private val visionRepository: VisionRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    private val _medicine = MutableStateFlow<VisionResult?>(null)
    val medicine: StateFlow<VisionResult?> = _medicine

    private val _waste = MutableStateFlow<VisionResult?>(null)
    val waste: StateFlow<VisionResult?> = _waste

    fun analyzeMedicine() = viewModelScope.launch {
        val result = visionRepository.classifyMedicine()
        _medicine.value = result
        historyRepository.save("Medicine", result.label, "${(result.confidence * 100).toInt()}% - ${result.tip}")
    }

    fun classifyWaste() = viewModelScope.launch {
        val result = visionRepository.classifyWaste()
        _waste.value = result
        historyRepository.save("Waste", result.label, "${(result.confidence * 100).toInt()}% - ${result.tip}")
    }
}
