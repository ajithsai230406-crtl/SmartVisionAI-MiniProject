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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage

// ─────────────────────────────────────────────────────────────────────────────
// GALLERY PICKER BUTTON — wraps Photo Picker API
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GalleryPickerButton(
    onImageSelected: (Uri) -> Unit,
    modifier:        Modifier = Modifier,
    label:           String   = "Gallery",
    accent:          Color    = Color(0xFF00E5FF)
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onImageSelected) }

    Box(modifier = modifier
        .clip(RoundedCornerShape(12.dp))
        .background(accent.copy(0.1f))
        .border(1.dp, accent.copy(0.3f), RoundedCornerShape(12.dp))
        .clickable {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.PhotoLibrary, null, tint = accent, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RECENT IMAGES STRIP — shows last few captured images
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RecentImagesStrip(
    images:          List<Uri> = emptyList(),
    onImageSelected: (Uri) -> Unit = {}
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding        = PaddingValues(horizontal = 2.dp)
    ) {
        if (images.isEmpty()) {
            items(5) {
                Box(Modifier.size(54.dp).clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF121826))
                    .border(1.dp, Color(0xFF1E2D47), RoundedCornerShape(10.dp)),
                    Alignment.Center) {
                    Icon(Icons.Rounded.Image, null, tint = Color(0xFF8892B0), modifier = Modifier.size(20.dp))
                }
            }
        } else {
            items(images) { uri ->
                AsyncImage(model = uri, contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF1E2D47), RoundedCornerShape(10.dp))
                        .clickable { onImageSelected(uri) })
            }
        }
    }
}
