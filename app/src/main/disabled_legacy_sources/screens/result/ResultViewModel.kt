package com.smartvision.ai.ui.screens.result

import android.app.Application
import android.content.*
import androidx.lifecycle.*
import com.smartvision.ai.domain.models.ScanResult
import com.smartvision.ai.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultUiState(
    val scanResult:    ScanResult?  = null,
    val imagePath:     String?      = null,
    val aiExplanation: String       = "",
    val isLoadingAi:   Boolean      = false
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val repo: ResultRepository,
    private val student: StudentHelperUseCase,
    app: Application
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val result = repo.getCachedResult()
            val path = repo.getCachedImagePath()
            _uiState.update { it.copy(scanResult = result, imagePath = path) }
            if (result != null && result !is ScanResult.Error) {
                fetchAi(result)
            }
        }
    }

    private fun fetchAi(result: ScanResult) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAi = true) }
            try {
                val text = when (result) {
                    is ScanResult.ObjectDetectionResult -> result.objects.joinToString { "${it.label} (${(it.confidence*100).toInt()}%)" }
                    is ScanResult.OcrResult             -> result.rawText.take(400)
                    else                                -> ""
                }
                if (text.isNotEmpty()) {
                    val ai = student.explainText(text)
                    if (ai is ScanResult.StudentHelperResult) {
                        _uiState.update { it.copy(aiExplanation = ai.aiExplanation) }
                    }
                }
            } finally {
                _uiState.update { it.copy(isLoadingAi = false) }
            }
        }
    }

    fun copy() {
        val t = resultText() ?: return
        val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("SVA", t))
    }

    fun share() {
        val t = resultText() ?: return
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, t)
        }
        getApplication<Application>().startActivity(
            Intent.createChooser(i, "Share via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun speak() {
        // TTS integration
    }

    private fun resultText() = when (val r = _uiState.value.scanResult) {
        is ScanResult.OcrResult             -> r.rawText
        is ScanResult.ObjectDetectionResult -> r.objects.joinToString("\n") { "${it.label}: ${(it.confidence*100).toInt()}%" }
        is ScanResult.QrCodeResult          -> r.displayValue
        is ScanResult.StudentHelperResult   -> r.aiExplanation
        is ScanResult.MedicalScanResult     -> "${r.medicineName}\n${r.usage}\n${r.dosage}"
        is ScanResult.WasteClassifierResult -> r.categories.joinToString("\n") { "${it.label}: ${(it.confidence*100).toInt()}%" }
        else                                -> null
    }
}
