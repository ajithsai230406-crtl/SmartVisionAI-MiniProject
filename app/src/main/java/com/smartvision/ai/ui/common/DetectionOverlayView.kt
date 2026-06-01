package com.smartvision.ai.ui.common

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
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC121826")
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawDetection(canvas, "Plant", "98%", RectF(width * 0.10f, height * 0.20f, width * 0.38f, height * 0.70f), "#00E5FF")
        drawDetection(canvas, "Sofa", "96%", RectF(width * 0.50f, height * 0.35f, width * 0.88f, height * 0.68f), "#7B61FF")
        drawDetection(canvas, "Table", "93%", RectF(width * 0.08f, height * 0.67f, width * 0.86f, height * 0.88f), "#00FFA8")
    }

    private fun drawDetection(canvas: Canvas, title: String, score: String, rect: RectF, color: String) {
        boxPaint.color = Color.parseColor(color)
        canvas.drawRoundRect(rect, 16f, 16f, boxPaint)
        val label = "$title\n$score"
        val labelRect = RectF(rect.left, rect.top - 4f, rect.left + 150f, rect.top + 82f)
        canvas.drawRoundRect(labelRect, 12f, 12f, labelPaint)
        canvas.drawText(title, labelRect.left + 12f, labelRect.top + 32f, textPaint)
        canvas.drawText(score, labelRect.left + 12f, labelRect.top + 66f, textPaint)
    }
}
