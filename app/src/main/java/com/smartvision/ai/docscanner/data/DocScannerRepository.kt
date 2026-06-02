package com.smartvision.ai.docscanner.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.smartvision.ai.docscanner.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocScannerRepository @Inject constructor() {

    // ── Apply filter to all pages ──────────────────────────────────────────────
    suspend fun applyFilter(
        context:  Context,
        doc:      ScannedDocument,
        filter:   DocumentFilter
    ): Result<ScannedDocument> = withContext(Dispatchers.IO) {
        runCatching {
            val filteredUris = doc.pages.map { uri ->
                val bmp     = loadBitmap(context, uri)
                val filtered = FilterHelper.applyFilter(bmp, filter)
                saveTempBitmap(context, filtered, "page_${System.currentTimeMillis()}")
            }
            doc.copy(pages = filteredUris, filter = filter)
        }
    }

    // ── Export document ────────────────────────────────────────────────────────
    suspend fun exportDocument(
        context:  Context,
        doc:      ScannedDocument,
        name:     String,
        format:   ExportFormat
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            when (format) {
                ExportFormat.PDF  -> exportPdf(context, doc, name)
                ExportFormat.JPEG -> exportImage(context, doc.pages.first(), name, ExportFormat.JPEG)
                ExportFormat.PNG  -> exportImage(context, doc.pages.first(), name, ExportFormat.PNG)
            }
        }
    }

    // ── PDF export ─────────────────────────────────────────────────────────────
    private fun exportPdf(context: Context, doc: ScannedDocument, name: String): Uri {
        val pdfDoc = PdfDocument()
        doc.pages.forEachIndexed { idx, uri ->
            val bmp   = loadBitmap(context, uri)
            val w     = bmp.width.coerceAtMost(2480)    // A4 @ 300dpi max
            val h     = (w * 1.4142f).toInt()           // A4 ratio
            val scaled = Bitmap.createScaledBitmap(bmp, w, h, true)

            val pageInfo = PdfDocument.PageInfo.Builder(w, h, idx + 1).create()
            val page     = pdfDoc.startPage(pageInfo)
            page.canvas.drawBitmap(scaled, 0f, 0f, null)
            pdfDoc.finishPage(page)
            scaled.recycle()
        }

        val folder = "Documents/SmartVisionAI"
        val (uri, stream) = createFileUriAndStream(context, "$name.pdf", "application/pdf", folder)
        try {
            pdfDoc.writeTo(stream)
            stream.flush()
        } finally {
            pdfDoc.close()
            stream.close()
        }

        return uri
    }

    // ── Image export ───────────────────────────────────────────────────────────
    private fun exportImage(context: Context, uri: Uri, name: String, format: ExportFormat): Uri {
        val bmp      = loadBitmap(context, uri)
        val ext      = format.extension
        val mime     = format.mimeType
        val fileName = "$name.$ext"
        val compFmt  = if (format == ExportFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val quality  = if (format == ExportFormat.PNG) 100 else 92

        val folder = "Pictures/SmartVisionAI"
        val (savedUri, stream) = createFileUriAndStream(context, fileName, mime, folder)
        try {
            bmp.compress(compFmt, quality, stream)
            stream.flush()
        } finally {
            stream.close()
        }

        return savedUri
    }

    // ── MediaStore and File helpers ────────────────────────────────────────────
    private fun createFileUriAndStream(
        context: Context,
        name: String,
        mime: String,
        folder: String
    ): Pair<Uri, OutputStream> {
        val resolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = if (mime == "application/pdf") {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }

            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
            }
            val insertUri = resolver.insert(collection, cv) ?: throw Exception("Failed to insert MediaStore entry")
            val outputStream = resolver.openOutputStream(insertUri) ?: throw Exception("Failed to open output stream")
            Pair(insertUri, outputStream)
        } else {
            val publicDir = if (mime == "application/pdf") {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            } else {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            }
            val dir = File(publicDir, "SmartVisionAI")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, name)
            val outputStream = FileOutputStream(file)
            val uri = Uri.fromFile(file)
            Pair(uri, outputStream)
        }
    }

    // ── Temp file helpers ──────────────────────────────────────────────────────
    private fun saveTempBitmap(context: Context, bmp: Bitmap, tag: String): Uri {
        val file = File(context.cacheDir, "$tag.jpg")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        return Uri.fromFile(file)
    }

    private fun loadBitmap(context: Context, uri: Uri): Bitmap =
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    // ── Delete temp cache ──────────────────────────────────────────────────────
    fun clearTempFiles(context: Context) {
        context.cacheDir.listFiles()?.filter { it.name.startsWith("page_") }?.forEach { it.delete() }
    }

    // ── Get file size from URI ─────────────────────────────────────────────────
    fun getFileSizeKb(context: Context, uri: Uri): Long {
        if (uri.scheme == "file") {
            val path = uri.path ?: return 0L
            val file = File(path)
            if (file.exists()) {
                return file.length() / 1024
            }
        }
        val cursor = context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null)
        return cursor?.use { if (it.moveToFirst()) it.getLong(0) / 1024 else 0L } ?: 0L
    }
}
