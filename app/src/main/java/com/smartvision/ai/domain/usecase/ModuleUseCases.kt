package com.smartvision.ai.domain.usecase

import android.graphics.Bitmap
import com.smartvision.ai.domain.models.*

// ─────────────────────────────────────────────────────────────────────────────
// DETECT OBJECTS USE CASE
// ─────────────────────────────────────────────────────────────────────────────

interface DetectObjectsUseCase {
    suspend operator fun invoke(bitmap: Bitmap): ScanResult
}

// ─────────────────────────────────────────────────────────────────────────────
// OCR USE CASE
// ─────────────────────────────────────────────────────────────────────────────

interface OcrUseCase {
    suspend operator fun invoke(bitmap: Bitmap): ScanResult
    suspend fun extractFromArea(bitmap: Bitmap, area: android.graphics.Rect): ScanResult
}

// ─────────────────────────────────────────────────────────────────────────────
// QR / BARCODE SCAN USE CASE
// ─────────────────────────────────────────────────────────────────────────────

interface ScanQrUseCase {
    suspend operator fun invoke(bitmap: Bitmap): ScanResult
}

// ─────────────────────────────────────────────────────────────────────────────
// STUDENT HELPER USE CASE
// ─────────────────────────────────────────────────────────────────────────────

interface StudentHelperUseCase {
    suspend operator fun invoke(questionBitmap: Bitmap): ScanResult
    suspend fun explainText(text: String): ScanResult
}

// ─────────────────────────────────────────────────────────────────────────────
// WASTE CLASSIFIER USE CASE
// ─────────────────────────────────────────────────────────────────────────────

interface ClassifyWasteUseCase {
    suspend operator fun invoke(bitmap: Bitmap): ScanResult
}

// ─────────────────────────────────────────────────────────────────────────────
// MEDICAL SCAN USE CASE
// ─────────────────────────────────────────────────────────────────────────────

interface MedicalScanUseCase {
    suspend operator fun invoke(bitmap: Bitmap): ScanResult
}
