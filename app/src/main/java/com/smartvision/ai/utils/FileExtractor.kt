package com.smartvision.ai.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Extracts text content from uploaded files (PDF, TXT, CSV, images).
 * Used by ConversationChatViewModel to inject file contents into AI prompts.
 */
object FileExtractor {

    private const val TAG = "FileExtractor"
    private const val MAX_TEXT_LENGTH = 15000 // Max chars to inject into prompt

    /**
     * Extracts text from a file URI based on its MIME type.
     * Returns the extracted text or an error message.
     */
    fun extractText(context: Context, uri: Uri, mimeType: String): String {
        return try {
            when {
                mimeType.startsWith("image/") -> {
                    // Images are handled separately via multimodal — return a marker
                    "[Image attached — will be analyzed by AI vision model]"
                }
                mimeType == "application/pdf" -> {
                    extractPdfText(context, uri)
                }
                mimeType == "text/plain" || mimeType == "text/csv" ||
                mimeType == "text/comma-separated-values" -> {
                    readTextFile(context, uri)
                }
                mimeType.contains("wordprocessingml") ||
                mimeType == "application/msword" ||
                mimeType.contains("opendocument.text") -> {
                    extractDocxText(context, uri)
                }
                else -> {
                    // Try reading as plain text as fallback
                    try {
                        readTextFile(context, uri)
                    } catch (e: Exception) {
                        "⚠️ Unsupported file type: $mimeType"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract text: ${e.message}", e)
            "⚠️ Failed to read file: ${e.message}"
        }
    }

    /**
     * Extracts text from a PDF using Android's PdfRenderer.
     * Renders each page to a bitmap and uses basic OCR-like text extraction.
     * Note: PdfRenderer can only extract rendered content, not selectable text.
     * For text-based PDFs, we attempt to read the raw bytes for text patterns.
     */
    private fun extractPdfText(context: Context, uri: Uri): String {
        val fd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: return "⚠️ Could not open PDF file"

        return try {
            val renderer = PdfRenderer(fd)
            val pageCount = renderer.pageCount
            val textBuilder = StringBuilder()
            textBuilder.append("[PDF Document — $pageCount page(s)]\n\n")

            // Try raw text extraction from the PDF stream first
            val rawText = tryExtractRawPdfText(context, uri)
            if (rawText.isNotBlank() && rawText.length > 50) {
                textBuilder.append(rawText.take(MAX_TEXT_LENGTH))
            } else {
                // Fallback: note the page count (image-based PDF)
                textBuilder.append("This PDF appears to contain scanned images. ")
                textBuilder.append("$pageCount page(s) detected. ")
                textBuilder.append("Please describe what you need help with regarding this document.")
            }

            renderer.close()
            fd.close()
            textBuilder.toString()
        } catch (e: Exception) {
            fd.close()
            Log.e(TAG, "PDF extraction failed: ${e.message}", e)
            "⚠️ Failed to read PDF: ${e.message}"
        }
    }

    /**
     * Attempts to extract raw text strings from a PDF byte stream.
     * This works for text-based PDFs (not scanned images).
     */
    private fun tryExtractRawPdfText(context: Context, uri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
            val bytes = inputStream.readBytes()
            inputStream.close()

            // Convert to string and extract text between parentheses or BT/ET blocks
            val content = String(bytes, Charsets.ISO_8859_1)
            val textBuilder = StringBuilder()

            // Simple regex to find text strings in PDF content streams
            val textPattern = Regex("""\(([^)]+)\)""")
            textPattern.findAll(content).forEach { match ->
                val text = match.groupValues[1]
                // Filter out binary/control sequences
                if (text.length > 1 && text.all { it.code in 32..126 || it == '\n' || it == '\r' }) {
                    textBuilder.append(text)
                    textBuilder.append(" ")
                }
            }

            textBuilder.toString().trim()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Reads a plain text or CSV file as UTF-8 text.
     */
    private fun readTextFile(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return "⚠️ Could not open file"

        return try {
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val text = reader.readText()
            reader.close()
            inputStream.close()

            if (text.isBlank()) {
                "⚠️ File is empty"
            } else {
                text.take(MAX_TEXT_LENGTH)
            }
        } catch (e: Exception) {
            inputStream.close()
            "⚠️ Failed to read text file: ${e.message}"
        }
    }

    /**
     * Extracts text from DOCX files using simple XML parsing.
     * DOCX files are ZIP archives containing XML documents.
     * The main content is in word/document.xml.
     */
    private fun extractDocxText(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return "⚠️ Could not open document"

        return try {
            val zipInputStream = java.util.zip.ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            val textBuilder = StringBuilder()

            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xmlContent = zipInputStream.bufferedReader(Charsets.UTF_8).readText()
                    // Extract text between <w:t> tags
                    val textPattern = Regex("""<w:t[^>]*>([^<]*)</w:t>""")
                    textPattern.findAll(xmlContent).forEach { match ->
                        textBuilder.append(match.groupValues[1])
                        textBuilder.append(" ")
                    }
                    break
                }
                entry = zipInputStream.nextEntry
            }

            zipInputStream.close()
            inputStream.close()

            val result = textBuilder.toString().trim()
            if (result.isBlank()) {
                "⚠️ Could not extract text from this document"
            } else {
                "[DOCX Document]\n\n${result.take(MAX_TEXT_LENGTH)}"
            }
        } catch (e: Exception) {
            inputStream.close()
            Log.e(TAG, "DOCX extraction failed: ${e.message}", e)
            "⚠️ Failed to read document: ${e.message}"
        }
    }

    /**
     * Loads a Bitmap from an image URI for multimodal AI processing.
     */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load image: ${e.message}", e)
            null
        }
    }

    /**
     * Returns a human-readable file type label from MIME type.
     */
    fun getFileTypeLabel(mimeType: String): String {
        return when {
            mimeType.startsWith("image/") -> "Image"
            mimeType == "application/pdf" -> "PDF"
            mimeType == "text/plain" -> "Text"
            mimeType == "text/csv" || mimeType == "text/comma-separated-values" -> "CSV"
            mimeType.contains("wordprocessingml") || mimeType == "application/msword" -> "DOCX"
            else -> "File"
        }
    }

    /**
     * Returns an emoji icon for the file type.
     */
    fun getFileTypeEmoji(mimeType: String): String {
        return when {
            mimeType.startsWith("image/") -> "🖼️"
            mimeType == "application/pdf" -> "📄"
            mimeType == "text/plain" -> "📝"
            mimeType == "text/csv" || mimeType == "text/comma-separated-values" -> "📊"
            mimeType.contains("wordprocessingml") || mimeType == "application/msword" -> "📃"
            else -> "📎"
        }
    }
}
