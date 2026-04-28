package com.smartvision.ai.data.repository

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.domain.usecase.ScanQrUseCase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanQrUseCaseImpl @Inject constructor() : ScanQrUseCase {

    private val scanner = BarcodeScanning.getClient()

    override suspend fun invoke(bitmap: Bitmap): ScanResult {
        return try {
            val image   = InputImage.fromBitmap(bitmap, 0)
            val result  = scanner.process(image).await()
            val barcode = result.firstOrNull() ?: return ScanResult.Error("No QR code found")
            val type = when (barcode.valueType) {
                Barcode.TYPE_URL          -> QrType.URL
                Barcode.TYPE_EMAIL        -> QrType.EMAIL
                Barcode.TYPE_PHONE        -> QrType.PHONE
                Barcode.TYPE_SMS          -> QrType.SMS
                Barcode.TYPE_WIFI         -> QrType.WIFI
                Barcode.TYPE_CONTACT_INFO -> QrType.CONTACT
                Barcode.TYPE_TEXT         -> QrType.TEXT
                else                      -> QrType.OTHER
            }
            ScanResult.QrCodeResult(
                rawValue     = barcode.rawValue ?: "",
                type         = type,
                displayValue = barcode.displayValue ?: barcode.rawValue ?: ""
            )
        } catch (e: Exception) { ScanResult.Error(e.message ?: "QR scan failed") }
    }
}
