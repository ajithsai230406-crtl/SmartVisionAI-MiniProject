package com.smartvision.ai.qr.domain

// ─── QR / Barcode Domain Models ────────────────────────────────────────────────

enum class QrContentType(
    val displayName: String,
    val emoji:       String,
    val colorArgb:   Long,
    val actionLabel: String
) {
    URL      ("Website URL",    "🌐", 0xFF00B4FF, "Open Link"),
    EMAIL    ("Email Address",  "📧", 0xFF00D4FF, "Send Email"),
    PHONE    ("Phone Number",   "📞", 0xFF39FF14, "Call Now"),
    SMS      ("SMS Message",    "💬", 0xFF7B2FFF, "Send SMS"),
    WIFI     ("WiFi Network",   "📶", 0xFFFFD700, "Connect"),
    CONTACT  ("Contact Card",   "👤", 0xFFFF6600, "Save Contact"),
    TEXT     ("Plain Text",     "📄", 0xFF7B7BFF, "Copy Text"),
    PRODUCT  ("Product",        "🛍️", 0xFFBF5FFF, "View Info"),
    GEO      ("Location",       "📍", 0xFFFF073A, "Open Map"),
    CALENDAR ("Calendar Event", "📅", 0xFF00E5FF, "Add to Calendar"),
    OTHER    ("Unknown",        "❓", 0xFF555555, "Copy")
}

data class ScannedCode(
    val id:          Long            = System.currentTimeMillis(),
    val rawValue:    String,
    val displayValue: String,
    val contentType: QrContentType,
    val formatName:  String,         // QR Code, EAN-13, Code128, etc.
    val wifiInfo:    WifiInfo?       = null,
    val timestamp:   Long            = System.currentTimeMillis()
)

data class WifiInfo(
    val ssid:     String,
    val password: String,
    val security: String   // WPA, WEP, nopass
)

/** Scan state machine */
sealed class QrScanState {
    object Idle       : QrScanState()
    object Scanning   : QrScanState()
    data class Success(val code: ScannedCode) : QrScanState()
    data class Error(val message: String)     : QrScanState()
}
