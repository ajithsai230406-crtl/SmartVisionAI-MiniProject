package com.smartvision.ai.qr.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.Image
import android.net.Uri
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.qr.data.QrScannerRepository
import com.smartvision.ai.qr.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class QrScannerViewModel @Inject constructor(
    private val repo:        QrScannerRepository,
    private val historyRepo: HistoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _scanState = MutableStateFlow<QrScanState>(QrScanState.Idle)
    val scanState: StateFlow<QrScanState> = _scanState.asStateFlow()

    private val _history = MutableStateFlow<List<ScannedCode>>(emptyList())
    val history: StateFlow<List<ScannedCode>> = _history.asStateFlow()

    private val _flashEnabled = MutableStateFlow(false)
    val flashEnabled: StateFlow<Boolean> = _flashEnabled.asStateFlow()

    private val _showHistory  = MutableStateFlow(false)
    val showHistory: StateFlow<Boolean> = _showHistory.asStateFlow()

    private val analysisLock = AtomicBoolean(false)
    private var cooldownJob: Job? = null

    // ── Live frame scan ────────────────────────────────────────────────────────
    fun analyzeFrame(image: Image, rotation: Int) {
        if (!analysisLock.compareAndSet(false, true)) return
        if (_scanState.value is QrScanState.Success) { analysisLock.set(false); return }
        viewModelScope.launch {
            repo.scanFromImage(image, rotation)
                .onSuccess { code ->
                    if (code != null) {
                        _scanState.value = QrScanState.Success(code)
                        addToHistory(code)
                        saveHistory(code)
                        // Cooldown: prevent re-scan for 3 seconds
                        cooldownJob?.cancel()
                        cooldownJob = launch { delay(3000); if (_scanState.value is QrScanState.Success) resumeScan() }
                    }
                }
                .onFailure { /* silent — camera frames fail regularly */ }
            analysisLock.set(false)
        }
    }

    fun resumeScan()     { _scanState.value = QrScanState.Idle }
    fun toggleFlash()    { _flashEnabled.update { !it } }
    fun toggleHistory()  { _showHistory.update { !it } }
    fun clearHistory()   { _history.value = emptyList() }

    fun copyToClipboard(text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("QR Result", text))
    }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun dialPhone(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun sendEmail(address: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun sendSms(number: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openMap(geo: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$geo?q=$geo")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun addToHistory(code: ScannedCode) {
        _history.update { (listOf(code) + it).distinctBy { c -> c.rawValue }.take(50) }
    }

    private suspend fun saveHistory(code: ScannedCode) {
        historyRepo.save(
            "QR Scan",
            "${code.contentType.emoji} ${code.contentType.displayName}",
            code.displayValue.take(220)
        )
    }
}
