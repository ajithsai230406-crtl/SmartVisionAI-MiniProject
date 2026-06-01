package com.smartvision.ai.ui.screens.camera

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// UI STATE
// ─────────────────────────────────────────────────────────────────────────────

data class CameraUiState(
    val isProcessing: Boolean                     = false,
    val liveMode: Boolean                         = false,
    val overlayBoxes: List<BoundingBox>           = emptyList(),
    val liveObjects: List<DetectedObject>         = emptyList(),
    val error: String?                            = null
)

// ─────────────────────────────────────────────────────────────────────────────
// VIEW MODEL
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val detectObjectsUseCase:   DetectObjectsUseCase,
    private val ocrUseCase:             OcrUseCase,
    private val classifyWasteUseCase:   ClassifyWasteUseCase,
    private val scanQrUseCase:          ScanQrUseCase,
    private val resultRepository:       ResultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    // ── Live frame analysis ───────────────────────────────────────────────────
    fun analyzeFrame(proxy: ImageProxy, module: ModuleType) {
        if (!_uiState.value.liveMode) {
            proxy.close()
            return
        }
        viewModelScope.launch {
            try {
                val bitmap = proxy.toBitmap()
                when (module) {
                    ModuleType.OBJECT_DETECTION -> {
                        val result = detectObjectsUseCase(bitmap)
                        if (result is ScanResult.ObjectDetectionResult) {
                            _uiState.update { it.copy(
                                liveObjects  = result.objects,
                                overlayBoxes = result.objects.map { o -> o.boundingBox }
                            )}
                        }
                    }
                    ModuleType.QR_SCANNER -> {
                        val result = scanQrUseCase(bitmap)
                        if (result is ScanResult.QrCodeResult) {
                            resultRepository.cacheResult(result)
                        }
                    }
                    else -> {} // others require explicit capture
                }
            } catch (e: Exception) {
                // silently drop frame errors in live mode
            } finally {
                proxy.close()
            }
        }
    }

    // ── Capture + process ─────────────────────────────────────────────────────
    fun captureImage(
        imageCapture: ImageCapture,
        context: Context,
        module: ModuleType,
        onComplete: () -> Unit
    ) {
        _uiState.update { it.copy(isProcessing = true) }
        val outputFile = File(context.cacheDir, "smartvision_${System.currentTimeMillis()}.jpg")
        val options    = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    viewModelScope.launch {
                        processFile(outputFile, module)
                        onComplete()
                    }
                }
                override fun onError(exc: ImageCaptureException) {
                    _uiState.update { it.copy(isProcessing = false, error = exc.message) }
                }
            }
        )
    }

    // ── Process URI (from gallery) ────────────────────────────────────────────
    fun processUri(uri: Uri, module: ModuleType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                resultRepository.cacheUri(uri)
                // Processing happens in ResultScreen via its own ViewModel
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    // ── Live mode toggle ──────────────────────────────────────────────────────
    fun toggleLiveMode() {
        _uiState.update { it.copy(liveMode = !it.liveMode) }
    }

    private suspend fun processFile(file: File, module: ModuleType) {
        try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            val result = when (module) {
                ModuleType.OBJECT_DETECTION -> detectObjectsUseCase(bitmap)
                ModuleType.TEXT_SCANNER     -> ocrUseCase(bitmap)
                ModuleType.WASTE_CLASSIFIER -> classifyWasteUseCase(bitmap)
                ModuleType.QR_SCANNER       -> scanQrUseCase(bitmap)
                else                        -> detectObjectsUseCase(bitmap) // fallback
            }
            resultRepository.cacheResult(result)
            resultRepository.cacheImagePath(file.absolutePath)
        } finally {
            _uiState.update { it.copy(isProcessing = false) }
        }
    }
}

// Extension: ImageProxy → Bitmap
fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    buffer.rewind()
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
