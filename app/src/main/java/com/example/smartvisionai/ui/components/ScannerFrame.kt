package com.example.smartvisionai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

@Composable
fun ScannerFrame(
    modifier: Modifier = Modifier,
    color: Color = Color.Cyan
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 6f
        val cornerLength = size.minDimension * 0.2f

        // Top-left
        drawLine(color, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
        drawLine(color, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)

        // Top-right
        drawLine(color, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), strokeWidth)
        drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth)

        // Bottom-left
        drawLine(color, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth)
        drawLine(color, Offset(0f, size.height), Offset(0f, size.height - cornerLength), strokeWidth)

        // Bottom-right
        drawLine(color, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), strokeWidth)
        drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), strokeWidth)
    }
}
