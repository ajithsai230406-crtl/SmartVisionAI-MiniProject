package com.smartvision.ai.presentation.ocr

import android.media.Image
import androidx.lifecycle.ViewModel
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.data.repository.OcrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OcrScannerViewModel @Inject constructor(
    private val ocrRepository: OcrRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    suspend fun recognize(image: Image, rotation: Int): String {
        val text = ocrRepository.recognize(image, rotation).getOrElse { it.message ?: "OCR failed. Try again." }
        if (text.length > 18 && !text.startsWith("No readable")) {
            historyRepository.save("OCR", "OCR Scan", text.take(220))
        }
        return text
    }

    suspend fun saveManual(text: String) {
        if (text.isNotBlank()) historyRepository.save("OCR", "Saved OCR Text", text.take(220))
    }
}
