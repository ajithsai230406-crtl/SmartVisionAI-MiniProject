package com.smartvision.ai.compose.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.smartvision.ai.ui.theme.*

/**
 * Reusable gallery image picker composable.
 * Launches the Android photo picker, returns the selected image URI.
 *
 * Usage:
 *   GalleryPickerLauncher { uri -> doSomethingWith(uri) }
 */
@Composable
fun rememberGalleryPickerLauncher(
    onImageSelected: (Uri) -> Unit
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onImageSelected(it) }
    }
    return { launcher.launch("image/*") }
}

/**
 * Gallery button matching the Smart Vision AI neon aesthetic.
 * Drop-in "🖼️ Gallery" button for any screen.
 */
@Composable
fun GalleryPickerButton(
    label:    String    = "Gallery",
    modifier: Modifier  = Modifier,
    onClick:  () -> Unit
) {
    val ext = MaterialTheme.extended
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ext.glassCard)
            .border(1.dp, NeonBlue.copy(0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("🖼️", fontSize = 16.sp)
            Text(
                label,
                style      = MaterialTheme.typography.labelMedium,
                color      = NeonBlue,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Reads bitmap from URI and passes it as android.graphics.Bitmap to callback.
 * Safe — catches all exceptions.
 */
fun loadBitmapFromUri(ctx: Context, uri: Uri): android.graphics.Bitmap? =
    runCatching {
        ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        }
    }.getOrNull()

/**
 * Converts android.graphics.Bitmap → android.media.Image-compatible via ML Kit InputImage.
 * Use with ML Kit's InputImage.fromBitmap(bitmap, 0).
 */
fun uriToInputImageBitmap(ctx: Context, uri: Uri): android.graphics.Bitmap? =
    loadBitmapFromUri(ctx, uri)
