package com.smartvision.ai.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class DetectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.rgb(0, 229, 255)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        style = Paint.Style.FILL
    }
    private var label = "AI 96%"

    fun showLabel(value: String) {
        label = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val rect = RectF(width * 0.12f, height * 0.24f, width * 0.88f, height * 0.58f)
        canvas.drawRoundRect(rect, 18f, 18f, boxPaint)
        canvas.drawText(label, rect.left + 16f, rect.top - 14f, textPaint)
    }
}
