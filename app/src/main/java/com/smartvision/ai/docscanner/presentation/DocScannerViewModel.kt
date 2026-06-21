package com.smartvision.ai.docscanner.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.data.repository.FirebaseRepository
import com.smartvision.ai.docscanner.data.DocScannerRepository
import com.smartvision.ai.docscanner.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class DocScannerViewModel @Inject constructor(
    private val repo:        DocScannerRepository,
    private val historyRepo: HistoryRepository,
    private val firebase:    FirebaseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ── State ──────────────────────────────────────────────────────────────────
    private val _state = MutableStateFlow<DocScanState>(DocScanState.Idle)
    val state: StateFlow<DocScanState> = _state.asStateFlow()

    private val _selectedFilter = MutableStateFlow(DocumentFilter.ORIGINAL)
    val selectedFilter: StateFlow<DocumentFilter> = _selectedFilter.asStateFlow()

    private val _exportFormat   = MutableStateFlow(ExportFormat.PDF)
    val exportFormat: StateFlow<ExportFormat> = _exportFormat.asStateFlow()

    private val _docName        = MutableStateFlow("Document_${System.currentTimeMillis()}")
    val docName: StateFlow<String> = _docName.asStateFlow()

    private val _savedDocs      = MutableStateFlow<List<ScannedDocument>>(emptyList())
    val savedDocs: StateFlow<List<ScannedDocument>> = _savedDocs.asStateFlow()

    private val _toast          = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    // ── ML Kit Scanner instance ────────────────────────────────────────────────
    private val scanner: GmsDocumentScanner by lazy {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .setScannerMode(SCANNER_MODE_FULL)      // FULL = edge detection + correction
            .build()
        GmsDocumentScanning.getClient(options)
    }

    // ── Launch ML Kit scanner ──────────────────────────────────────────────────
    fun launchScanner(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        viewModelScope.launch {
            _state.value = DocScanState.Scanning
            runCatching {
                val sender = scanner.getStartScanIntent(activity).await()
                launcher.launch(IntentSenderRequest.Builder(sender).build())
            }.onFailure {
                _state.value = DocScanState.Error("Could not start scanner: ${it.message}")
            }
        }
    }

    // ── Handle scanner result ──────────────────────────────────────────────────
    fun handleScanResult(result: GmsDocumentScanningResult?) {
        if (result == null) { _state.value = DocScanState.Idle; return }
        val pages = result.pages?.mapNotNull { it.imageUri } ?: emptyList()
        if (pages.isEmpty()) { _state.value = DocScanState.Idle; return }

        val doc = ScannedDocument(
            name      = _docName.value,
            pages     = pages,
            originalPages = pages,
            filter    = _selectedFilter.value,
            exportFormat = _exportFormat.value
        )
        _state.value = DocScanState.Preview(doc)
    }

    // ── Filter selection ───────────────────────────────────────────────────────
    fun selectFilter(filter: DocumentFilter) {
        _selectedFilter.value = filter
        val preview = _state.value as? DocScanState.Preview ?: return
        viewModelScope.launch {
            _state.value = DocScanState.Processing
            repo.applyFilter(context, preview.doc, filter)
                .onSuccess { _state.value = DocScanState.Preview(it) }
                .onFailure { _state.value = DocScanState.Preview(preview.doc) }
        }
    }

    // ── Export / save ──────────────────────────────────────────────────────────
    fun exportDocument() {
        val preview = _state.value as? DocScanState.Preview ?: return
        val name    = _docName.value.ifBlank { "Document_${System.currentTimeMillis()}" }
        viewModelScope.launch {
            _state.value = DocScanState.Processing
            repo.exportDocument(context, preview.doc, name, _exportFormat.value)
                .onSuccess { uri ->
                    val sizeKb  = repo.getFileSizeKb(context, uri)
                    val saved   = preview.doc.copy(name = name, savedUri = uri, fileSizeKb = sizeKb)
                    _savedDocs.update { listOf(saved) + it }
                    _state.value = DocScanState.Saved(saved, uri)
                    
                    // Upload to Firebase Storage and Save to Firestore
                    val extStr = _exportFormat.value.extension
                    val uid = firebase.getCurrentUid()
                    val storagePath = "users/$uid/documents/$name.$extStr"
                    val uploadResult = firebase.uploadFile(storagePath, uri)
                    val downloadUrl = uploadResult.getOrNull()
                    if (downloadUrl != null) {
                        firebase.saveScannedDocument(
                            name = name,
                            downloadUrl = downloadUrl,
                            fileSizeKb = sizeKb,
                            pageCount = preview.doc.pageCount,
                            format = _exportFormat.value.name
                        )
                    }

                    _toast.value = "✅ Saved: $name.${_exportFormat.value.extension}"
                    historyRepo.save("Document", "📄 $name", "${preview.doc.pageCount} page(s) · ${_exportFormat.value.displayName}")
                }
                .onFailure { _state.value = DocScanState.Error(it.message ?: "Export failed") }
        }
    }

    private fun getShareableUri(uri: Uri): Uri {
        if (uri.scheme == "file") {
            val file = java.io.File(uri.path ?: "")
            return try {
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                uri
            }
        }
        return uri
    }

    // ── Share ──────────────────────────────────────────────────────────────────
    fun shareDocument(uri: Uri) {
        val mimeType = context.contentResolver.getType(uri) ?: when (uri.path?.substringAfterLast('.')?.lowercase()) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "application/pdf"
        }
        val safeUri = getShareableUri(uri)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, safeUri)
            val clipData = android.content.ClipData.newRawUri("Document", safeUri)
            this.clipData = clipData
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching {
            val chooser = Intent.createChooser(intent, "Share Document").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    // ── Open saved file ────────────────────────────────────────────────────────
    fun openDocument(uri: Uri) {
        val mimeType = context.contentResolver.getType(uri) ?: when (uri.path?.substringAfterLast('.')?.lowercase()) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "application/pdf"
        }
        val safeUri = getShareableUri(uri)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(safeUri, mimeType)
            val clipData = android.content.ClipData.newRawUri("Document", safeUri)
            this.clipData = clipData
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }

    fun setDocName(n: String)        { _docName.value = n }
    fun setExportFormat(f: ExportFormat) { _exportFormat.value = f }
    fun resetScan()                  { _state.value = DocScanState.Idle; repo.clearTempFiles(context) }
    fun dismissToast()               { _toast.value = null }
    fun dismissError()               { _state.value = DocScanState.Idle }
}
