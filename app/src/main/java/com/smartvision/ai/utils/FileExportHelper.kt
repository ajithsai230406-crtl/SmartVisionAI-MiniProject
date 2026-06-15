package com.smartvision.ai.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object FileExportHelper {

    fun saveTextFile(context: Context, filename: String, text: String): Result<Uri> = runCatching {
        val folder = "Documents/SmartVisionAI"
        val resolver = context.contentResolver
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
            }
            val insertUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv) 
                ?: throw Exception("Failed to create MediaStore entry in Downloads")
            
            resolver.openOutputStream(insertUri)?.use { stream ->
                stream.write(text.toByteArray())
                stream.flush()
            } ?: throw Exception("Failed to open output stream")
            
            insertUri
        } else {
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val dir = File(publicDir, "SmartVisionAI")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, filename)
            FileOutputStream(file).use { stream ->
                stream.write(text.toByteArray())
                stream.flush()
            }
            Uri.fromFile(file)
        }
    }
}
