package com.smartvision.ai.presentation.history

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.domain.model.HistoryRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val historyRepo: HistoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _record = MutableStateFlow<HistoryRecord?>(null)
    val record: StateFlow<HistoryRecord?> = _record.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        val historyId = savedStateHandle.get<Long>("historyId") ?: -1L
        tts = TextToSpeech(context) { /* ready */ }
        loadRecord(historyId)
    }

    private fun loadRecord(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _record.value = historyRepo.getById(id)
            _isLoading.value = false
        }
    }

    fun speak(text: String, langCode: String = "en") {
        tts?.language = Locale.forLanguageTag(langCode)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
