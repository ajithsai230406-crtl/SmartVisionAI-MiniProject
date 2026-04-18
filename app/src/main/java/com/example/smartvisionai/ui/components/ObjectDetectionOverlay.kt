package com.example.smartvisionai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.objects.DetectedObject
import com.example.smartvisionai.ui.theme.CyanAccent
import com.example.smartvisionai.ui.theme.PurpleAccent
import com.example.smartvisionai.ui.theme.OrangeAccent
import com.example.smartvisionai.ui.theme.RedAccent
import com.example.smartvisionai.ui.theme.GreenAccent

data class DetectionBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val label: String,
    val confidence: Float,
    val colorIndex: Int = 0
)

private val boxColors = listOf(CyanAccent, PurpleAccent, OrangeAccent, GreenAccent, RedAccent)

@Composable
fun ObjectDetectionOverlay(
    modifier: Modifier = Modifier,
    detections: List<DetectionBox>,
    previewWidth: Int,
    previewHeight: Int
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val scaleX = size.width  / previewWidth.toFloat()
        val scaleY = size.height / previewHeight.toFloat()

        detections.forEachIndexed { idx, det ->
            val color = boxColors[idx % boxColors.size]

            val left   = det.left   * scaleX
            val top    = det.top    * scaleY
            val right  = det.right  * scaleX
            val bottom = det.bottom * scaleY
            val w = right - left
            val h = bottom - top

            // Bounding box
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(w, h),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 2.5f)
            )

            // Corner accent marks
            val cornerLen = 20f
            val p = Paint().apply {
                this.color = color
                strokeWidth = 4f
                style = PaintingStyle.Stroke
                strokeCap = StrokeCap.Round
            }
            drawContext.canvas.apply {
                // TL
                drawLine(Offset(left, top + cornerLen), Offset(left, top), p)
                drawLine(Offset(left, top), Offset(left + cornerLen, top), p)
                // TR
                drawLine(Offset(right - cornerLen, top), Offset(right, top), p)
                drawLine(Offset(right, top), Offset(right, top + cornerLen), p)
                // BL
                drawLine(Offset(left, bottom - cornerLen), Offset(left, bottom), p)
                drawLine(Offset(left, bottom), Offset(left + cornerLen, bottom), p)
                // BR
                drawLine(Offset(right - cornerLen, bottom), Offset(right, bottom), p)
                drawLine(Offset(right, bottom), Offset(right, bottom - cornerLen), p)
            }

            // Label pill background
            val label = "${det.label} ${(det.confidence * 100).toInt()}%"
            val textStyle = TextStyle(fontSize = 11.sp, color = Color.White)
            val measured = textMeasurer.measure(label, textStyle)
            val pillW = measured.size.width + 20f
            val pillH = measured.size.height + 10f
            val pillTop = (top - pillH - 4f).coerceAtLeast(0f)

            drawRoundRect(
                color = color,
                topLeft = Offset(left, pillTop),
                size = Size(pillW, pillH),
                cornerRadius = CornerRadius(6f, 6f)
            )

            drawText(
                textMeasurer = textMeasurer,
                text = label,
                style = textStyle,
                topLeft = Offset(left + 10f, pillTop + 5f)
            )
        }
    }
}

/** Convert ML Kit DetectedObject to our DetectionBox model */
fun DetectedObject.toDetectionBox(idx: Int): DetectionBox {
    val label = labels.firstOrNull()
    return DetectionBox(
        left       = boundingBox.left.toFloat(),
        top        = boundingBox.top.toFloat(),
        right      = boundingBox.right.toFloat(),
        bottom     = boundingBox.bottom.toFloat(),
        label      = label?.text ?: "Object",
        confidence = label?.confidence ?: 0f,
        colorIndex = idx
    )
}
