package com.smartvision.ai.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// KOTLIN EXTENSION UTILITIES
// ─────────────────────────────────────────────────────────────────────────────

fun Long.toReadableDate(): String {
    val sdf = SimpleDateFormat("dd MMM, HH:mm a", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toRelativeTime(): String {
    val diff = System.currentTimeMillis() - this
    return when {
        diff < 60_000          -> "Just now"
        diff < 3_600_000       -> "${diff / 60_000} min ago"
        diff < 86_400_000      -> "${diff / 3_600_000} hr ago"
        diff < 604_800_000     -> "${diff / 86_400_000} days ago"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))
    }
}

fun String.truncate(maxLen: Int = 60): String =
    if (length <= maxLen) this else "${take(maxLen)}…"

fun Float.toPercent(): String = "${(this * 100).toInt()}%"

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun Context.shareText(text: String, title: String = "Share via") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type    = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(intent, title))
}

fun Context.openUrl(url: String) {
    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    catch (_: Exception) { toast("Cannot open URL") }
}

// ─────────────────────────────────────────────────────────────────────────────
// COLOR UTILS
// ─────────────────────────────────────────────────────────────────────────────

fun Color.withAlpha(alpha: Float): Color = this.copy(alpha = alpha)

fun confidenceColor(confidence: Float): Color = when {
    confidence >= 0.8f -> Color(0xFF00E5FF)
    confidence >= 0.5f -> Color(0xFFFFAB00)
    else               -> Color(0xFFFF5252)
}
