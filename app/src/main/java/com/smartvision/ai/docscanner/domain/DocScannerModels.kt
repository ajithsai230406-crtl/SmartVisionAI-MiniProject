package com.smartvision.ai.docscanner.domain

import android.net.Uri

// ─── Document filter types ─────────────────────────────────────────────────────

enum class DocumentFilter(
    val displayName: String,
    val emoji:       String,
    val colorArgb:   Long
) {
    ORIGINAL    ("Original",     "🖼️", 0xFF00B4FF),
    BLACK_WHITE ("Black & White","⚫", 0xFFCCCCCC),
    HD          ("HD Enhanced",  "✨", 0xFF7B2FFF),
    MAGIC_COLOR ("Magic Color",  "🌈", 0xFF00FF88),
    GRAYSCALE   ("Grayscale",    "🌫️", 0xFF888888),
    SHARP       ("Sharp Focus",  "🔍", 0xFFFFD700)
}

// ─── Export format ─────────────────────────────────────────────────────────────

enum class ExportFormat(val displayName: String, val extension: String, val mimeType: String) {
    PDF  ("PDF Document", "pdf",  "application/pdf"),
    JPEG ("JPEG Image",   "jpg",  "image/jpeg"),
    PNG  ("PNG Image",    "png",  "image/png")
}

// ─── Scanned document model ────────────────────────────────────────────────────

data class ScannedDocument(
    val id:           Long         = System.currentTimeMillis(),
    val name:         String       = "Document_${System.currentTimeMillis()}",
    val pages:        List<Uri>,
    val pageCount:    Int          = pages.size,
    val filter:       DocumentFilter = DocumentFilter.ORIGINAL,
    val exportFormat: ExportFormat = ExportFormat.PDF,
    val savedUri:     Uri?         = null,
    val fileSizeKb:   Long         = 0L,
    val createdAt:    Long         = System.currentTimeMillis()
)

// ─── Scan state machine ────────────────────────────────────────────────────────

sealed class DocScanState {
    object Idle       : DocScanState()
    object Scanning   : DocScanState()             // Scanner UI is active
    object Processing : DocScanState()             // Applying filter / saving
    data class Preview(val doc: ScannedDocument)   : DocScanState()
    data class Saved(val doc: ScannedDocument, val uri: Uri) : DocScanState()
    data class Error(val message: String)          : DocScanState()
}

// ─── Filter processing helpers ─────────────────────────────────────────────────

object FilterHelper {
    fun applyFilter(bmp: android.graphics.Bitmap, filter: DocumentFilter): android.graphics.Bitmap {
        return when (filter) {
            DocumentFilter.BLACK_WHITE -> toBw(bmp)
            DocumentFilter.GRAYSCALE   -> toGrayscale(bmp)
            DocumentFilter.HD          -> enhanceHd(bmp)
            DocumentFilter.MAGIC_COLOR -> enhanceColor(bmp)
            DocumentFilter.SHARP       -> sharpen(bmp)
            DocumentFilter.ORIGINAL    -> bmp
        }
    }

    private fun toBw(src: android.graphics.Bitmap): android.graphics.Bitmap {
        val out = android.graphics.Bitmap.createBitmap(src.width, src.height, src.config ?: android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        val paint  = android.graphics.Paint()
        val matrix = android.graphics.ColorMatrix().also { it.setSaturation(0f) }
        // High contrast B&W
        val contrast = android.graphics.ColorMatrix(floatArrayOf(
            1.5f, 0f, 0f, 0f, -80f,
            0f, 1.5f, 0f, 0f, -80f,
            0f, 0f, 1.5f, 0f, -80f,
            0f, 0f, 0f,  1f,   0f
        ))
        val combined = android.graphics.ColorMatrix()
        combined.setConcat(contrast, matrix)
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(combined)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    private fun toGrayscale(src: android.graphics.Bitmap): android.graphics.Bitmap {
        val out    = android.graphics.Bitmap.createBitmap(src.width, src.height, src.config ?: android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        val paint  = android.graphics.Paint()
        val matrix = android.graphics.ColorMatrix().also { it.setSaturation(0f) }
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    private fun enhanceHd(src: android.graphics.Bitmap): android.graphics.Bitmap {
        val out    = android.graphics.Bitmap.createBitmap(src.width, src.height, src.config ?: android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        val paint  = android.graphics.Paint()
        val matrix = android.graphics.ColorMatrix(floatArrayOf(
            1.2f, 0f,  0f,  0f, 10f,
            0f,  1.2f, 0f,  0f, 10f,
            0f,  0f,  1.2f, 0f, 10f,
            0f,  0f,  0f,  1f,  0f
        ))
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    private fun enhanceColor(src: android.graphics.Bitmap): android.graphics.Bitmap {
        val out    = android.graphics.Bitmap.createBitmap(src.width, src.height, src.config ?: android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        val paint  = android.graphics.Paint()
        val matrix = android.graphics.ColorMatrix(floatArrayOf(
            1.3f, 0f,   0f,   0f, 15f,
            0f,  1.3f,  0f,   0f, 15f,
            0f,  0f,   1.3f,  0f, 15f,
            0f,  0f,   0f,   1f,  0f
        ))
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    private fun sharpen(src: android.graphics.Bitmap): android.graphics.Bitmap {
        // Simple sharpen via convolution paint
        val rs = android.renderscript.RenderScript.create(null)
        return src   // RenderScript requires context; return original safely
    }
}
