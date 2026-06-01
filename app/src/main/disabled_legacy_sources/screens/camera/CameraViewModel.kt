package com.smartvision.ai.ui.screens.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class CameraUiState(
    val isProcessing: Boolean = false,
    val liveMode: Boolean = false,
    val overlayBoxes: List<BoundingBox> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val detectObjects: DetectObjectsUseCase,
    private val ocr: OcrUseCase,
    private val classifyWaste: ClassifyWasteUseCase,
    private val scanQr: ScanQrUseCase,
    private val repository: ResultRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _state.asStateFlow()

    fun analyzeFrame(proxy: ImageProxy, module: ModuleType) {
        if (!_state.value.liveMode) { proxy.close(); return }
        viewModelScope.launch {
            try {
                val bmp = proxy.toBitmap()
                when (module) {
                    ModuleType.OBJECT_DETECTION -> {
                        val r = detectObjects(bmp)
                        if (r is ScanResult.ObjectDetectionResult)
                            _state.update { it.copy(overlayBoxes = r.objects.map { o -> o.boundingBox }) }
                    }
                    ModuleType.QR_SCANNER -> { val r = scanQr(bmp); if (r is ScanResult.QrCodeResult) repository.cacheResult(r) }
                    else -> {}
                }
            } catch (_: Exception) { } finally { proxy.close() }
        }
    }

    fun captureImage(cap: ImageCapture, context: Context, module: ModuleType, onComplete: () -> Unit) {
        _state.update { it.copy(isProcessing = true) }
        // Auto-delete files older than 7 days
        cleanOldCaptures(context)
        val file = File(context.cacheDir, "sv_${System.currentTimeMillis()}.jpg")
        cap.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    viewModelScope.launch {
                        processFile(file.absolutePath, module)
                        onComplete()
                    }
                }
                override fun onError(e: ImageCaptureException) {
                    _state.update { it.copy(isProcessing = false, error = e.message) }
                }
            })
    }

    fun processImagePath(path: String, module: ModuleType) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true) }
            processFile(path, module)
        }
    }

    fun processUri(uri: Uri, module: ModuleType) {
        viewModelScope.launch { repository.cacheUri(uri) }
    }

    fun toggleLiveMode() { _state.update { it.copy(liveMode = !it.liveMode) } }

    private suspend fun processFile(path: String, module: ModuleType) {
        withContext(Dispatchers.Default) {
            try {
                val bmp = android.graphics.BitmapFactory.decodeFile(path)
                    ?: return@withContext
                val result = when (module) {
                    ModuleType.OBJECT_DETECTION -> detectObjects(bmp)
                    ModuleType.TEXT_SCANNER     -> ocr(bmp)
                    ModuleType.WASTE_CLASSIFIER -> classifyWaste(bmp)
                    ModuleType.QR_SCANNER       -> scanQr(bmp)
                    else                        -> detectObjects(bmp)
                }
                repository.cacheResult(result)
                repository.cacheImagePath(path)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            } finally {
                _state.update { it.copy(isProcessing = false) }
            }
        }
    }

    /** Auto-delete captures older than 7 days from app-specific cache */
    private fun cleanOldCaptures(context: Context) {
        val sevenDaysMs = TimeUnit.DAYS.toMillis(7)
        val now = System.currentTimeMillis()
        context.cacheDir.listFiles { f -> f.name.startsWith("sv_") && f.extension == "jpg" }
            ?.filter { now - it.lastModified() > sevenDaysMs }
            ?.forEach { it.delete() }
    }
}

fun ImageProxy.toBitmap(): android.graphics.Bitmap {
    val buf = planes[0].buffer.also { it.rewind() }
    val bytes = ByteArray(buf.remaining())
    buf.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
