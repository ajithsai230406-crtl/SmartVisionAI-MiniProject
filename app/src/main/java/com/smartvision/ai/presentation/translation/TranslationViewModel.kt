package com.smartvision.ai.presentation.translation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.data.repository.TranslationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TranslationViewModel @Inject constructor(
    private val translationRepository: TranslationRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    private val _output = MutableStateFlow("Translated text will appear here.")
    val output: StateFlow<String> = _output

    fun translate(text: String, target: String) = viewModelScope.launch {
        _output.value = "Downloading language model and translating..."
        translationRepository.translate(text, target)
            .onSuccess {
                _output.value = it
                historyRepository.save("Translation", "Translated to $target", it.take(220))
            }
            .onFailure { _output.value = it.message ?: "Translation failed. Check internet for model download." }
    }
}
