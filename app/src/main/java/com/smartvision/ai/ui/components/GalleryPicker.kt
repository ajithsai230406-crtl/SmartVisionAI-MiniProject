package com.smartvision.ai.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.smartvision.ai.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// GALLERY PICKER BUTTON — opens system photo picker
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GalleryPickerButton(
    onImagePicked: (Uri) -> Unit,
    modifier:      Modifier = Modifier,
    label:         String   = "Gallery"
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { onImagePicked(it) } }

    val colors = svColors
    OutlinedButton(
        onClick  = {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        modifier = modifier,
        border   = BorderStroke(1.dp, colors.border),
        shape    = RoundedCornerShape(14.dp)
    ) {
        Icon(Icons.Rounded.PhotoLibrary, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RECENT PHOTOS STRIP — loads thumbnails from MediaStore
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RecentPhotosStrip(
    recentUris:      List<Uri>,
    onPhotoSelected: (Uri) -> Unit,
    onOpenGallery:   () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { onPhotoSelected(it) } }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding        = PaddingValues(horizontal = 16.dp)
    ) {
        // Gallery all-photos button
        item {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(0.4f))
                    .border(1.dp, Color.White.copy(0.25f), RoundedCornerShape(10.dp))
                    .clickable {
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        // Recent thumbnails
        items(recentUris.take(8)) { uri ->
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onPhotoSelected(uri) }
            ) {
                AsyncImage(
                    model              = uri,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
            }
        }

        // Placeholder if no recent
        if (recentUris.isEmpty()) {
            items(5) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.08f))
                        .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Image, null, tint = Color.White.copy(0.35f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MEDIA STORE HELPER (ViewModel-level, call from coroutine)
// ─────────────────────────────────────────────────────────────────────────────

fun loadRecentImages(context: android.content.Context, limit: Int = 10): List<Uri> {
    val uris     = mutableListOf<Uri>()
    val external = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(android.provider.MediaStore.Images.Media._ID)
    val sortOrder  = "${android.provider.MediaStore.Images.Media.DATE_ADDED} DESC"

    context.contentResolver.query(external, projection, null, null, sortOrder)?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media._ID)
        while (cursor.moveToNext() && uris.size < limit) {
            val id  = cursor.getLong(idCol)
            val uri = android.content.ContentUris.withAppendedId(external, id)
            uris.add(uri)
        }
    }
    return uris
}
