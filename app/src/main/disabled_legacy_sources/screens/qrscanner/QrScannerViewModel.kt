package com.smartvision.ai.ui.screens.qrscanner

import android.app.Application
import android.content.*
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.domain.models.ScanResult
import com.smartvision.ai.domain.usecase.ScanQrUseCase
import com.smartvision.ai.ui.screens.camera.toBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QrScannerUiState(
    val result:   ScanResult? = null,
    val autoOpen: Boolean     = true
)

@HiltViewModel
class QrScannerViewModel @Inject constructor(
    private val scanQrUseCase: ScanQrUseCase,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(QrScannerUiState())
    val uiState: StateFlow<QrScannerUiState> = _uiState.asStateFlow()

    // Throttle: don't process every frame
    private var lastScanMs = 0L

    fun analyze(proxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastScanMs < 500L || _uiState.value.result != null) {
            proxy.close()
            return
        }
        lastScanMs = now

        viewModelScope.launch {
            try {
                val bitmap = proxy.toBitmap()
                val result = scanQrUseCase(bitmap)
                if (result is ScanResult.QrCodeResult) {
                    _uiState.update { it.copy(result = result) }
                }
            } finally {
                proxy.close()
            }
        }
    }

    fun setAutoOpen(value: Boolean) {
        _uiState.update { it.copy(autoOpen = value) }
    }

    fun rescan() {
        _uiState.update { it.copy(result = null) }
        lastScanMs = 0L
    }

    fun copyResult() {
        val text = when (val r = _uiState.value.result) {
            is ScanResult.QrCodeResult -> r.displayValue
            else                       -> return
        }
        val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("QR Result", text))
    }
}
