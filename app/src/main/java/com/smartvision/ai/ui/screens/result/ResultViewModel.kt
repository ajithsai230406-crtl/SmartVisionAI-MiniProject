package com.smartvision.ai.ui.screens.result

import android.app.Application
import android.content.*
import androidx.lifecycle.*
import com.smartvision.ai.domain.models.ScanResult
import com.smartvision.ai.domain.usecase.ResultRepository
import com.smartvision.ai.domain.usecase.StudentHelperUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultUiState(
    val scanResult:    ScanResult? = null,
    val imagePath:     String?     = null,
    val aiExplanation: String      = "",
    val isLoadingAi:   Boolean     = false,
    val error:         String?     = null
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val resultRepository: ResultRepository,
    private val studentHelper:    StudentHelperUseCase,
    app: Application
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    fun loadResult(moduleRoute: String) {
        viewModelScope.launch {
            val result    = resultRepository.getCachedResult()
            val imagePath = resultRepository.getCachedImagePath()
            _uiState.update { it.copy(scanResult = result, imagePath = imagePath) }
            if (result != null && result !is ScanResult.Error) fetchAiExplanation(result)
        }
    }

    private fun fetchAiExplanation(result: ScanResult) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAi = true) }
            try {
                val text = when (result) {
                    is ScanResult.ObjectDetectionResult -> result.objects.joinToString { it.label }
                    is ScanResult.OcrResult             -> result.rawText.take(500)
                    else                                -> ""
                }
                if (text.isNotEmpty()) {
                    val ai = studentHelper.explainText(text)
                    if (ai is ScanResult.StudentHelperResult)
                        _uiState.update { it.copy(aiExplanation = ai.aiExplanation) }
                }
            } finally { _uiState.update { it.copy(isLoadingAi = false) } }
        }
    }

    fun copyToClipboard() {
        val text = getResultText() ?: return
        val cm   = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("SmartVision", text))
    }

    fun shareResult() {
        val text   = getResultText() ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
        }
        getApplication<Application>().startActivity(
            Intent.createChooser(intent, "Share via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun speak() { /* wire TtsManager here */ }

    private fun getResultText(): String? = when (val r = _uiState.value.scanResult) {
        is ScanResult.OcrResult             -> r.rawText
        is ScanResult.ObjectDetectionResult -> r.objects.joinToString("\n") { "${it.label}: ${(it.confidence*100).toInt()}%" }
        is ScanResult.QrCodeResult          -> r.displayValue
        is ScanResult.StudentHelperResult   -> r.aiExplanation
        is ScanResult.MedicalScanResult     -> "${r.medicineName}\n${r.usage}\n${r.dosage}"
        else                                -> null
    }
}
