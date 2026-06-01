package com.smartvision.ai.qr.data

import android.media.Image
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.smartvision.ai.qr.domain.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrScannerRepository @Inject constructor() {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
    )

    /** Live camera frame analysis */
    suspend fun scanFromImage(image: Image, rotation: Int): Result<ScannedCode?> =
        runCatching {
            val input   = InputImage.fromMediaImage(image, rotation)
            val results = scanner.process(input).await()
            results.firstOrNull()?.toScannedCode()
        }

    /** Bitmap-based scan (gallery) */
    suspend fun scanFromBitmap(bitmap: android.graphics.Bitmap): Result<ScannedCode?> =
        runCatching {
            val input   = InputImage.fromBitmap(bitmap, 0)
            val results = scanner.process(input).await()
            results.firstOrNull()?.toScannedCode()
        }

    // ── ML Kit Barcode → Domain ───────────────────────────────────────────────
    private fun Barcode.toScannedCode(): ScannedCode {
        val contentType = when (valueType) {
            Barcode.TYPE_URL          -> QrContentType.URL
            Barcode.TYPE_EMAIL        -> QrContentType.EMAIL
            Barcode.TYPE_PHONE        -> QrContentType.PHONE
            Barcode.TYPE_SMS          -> QrContentType.SMS
            Barcode.TYPE_WIFI         -> QrContentType.WIFI
            Barcode.TYPE_CONTACT_INFO -> QrContentType.CONTACT
            Barcode.TYPE_GEO          -> QrContentType.GEO
            Barcode.TYPE_CALENDAR_EVENT -> QrContentType.CALENDAR
            Barcode.TYPE_TEXT         -> QrContentType.TEXT
            Barcode.TYPE_PRODUCT      -> QrContentType.PRODUCT
            else                      -> QrContentType.OTHER
        }

        val formatName = when (format) {
            Barcode.FORMAT_QR_CODE      -> "QR Code"
            Barcode.FORMAT_EAN_13       -> "EAN-13 Barcode"
            Barcode.FORMAT_EAN_8        -> "EAN-8 Barcode"
            Barcode.FORMAT_CODE_128     -> "Code 128"
            Barcode.FORMAT_CODE_39      -> "Code 39"
            Barcode.FORMAT_DATA_MATRIX  -> "Data Matrix"
            Barcode.FORMAT_PDF417       -> "PDF417"
            Barcode.FORMAT_AZTEC        -> "Aztec"
            Barcode.FORMAT_UPC_A        -> "UPC-A"
            Barcode.FORMAT_UPC_E        -> "UPC-E"
            else                        -> "Unknown Format"
        }

        val wifiInfo = wifi?.let {
            WifiInfo(
                ssid     = it.ssid ?: "",
                password = it.password ?: "",
                security = when (it.encryptionType) {
                    Barcode.WiFi.TYPE_WPA -> "WPA/WPA2"
                    Barcode.WiFi.TYPE_WEP -> "WEP"
                    else                  -> "Open"
                }
            )
        }

        val displayValue = when (valueType) {
            Barcode.TYPE_URL          -> url?.url ?: rawValue ?: ""
            Barcode.TYPE_EMAIL        -> email?.address ?: rawValue ?: ""
            Barcode.TYPE_PHONE        -> phone?.number ?: rawValue ?: ""
            Barcode.TYPE_WIFI         -> wifi?.ssid ?: rawValue ?: ""
            Barcode.TYPE_CONTACT_INFO -> contactInfo?.name?.formattedName ?: rawValue ?: ""
            Barcode.TYPE_GEO          -> geoPoint?.let { "${it.lat}, ${it.lng}" } ?: rawValue ?: ""
            else                      -> displayValue ?: rawValue ?: ""
        }

        return ScannedCode(
            rawValue     = rawValue ?: "",
            displayValue = displayValue,
            contentType  = contentType,
            formatName   = formatName,
            wifiInfo     = wifiInfo
        )
    }
}
