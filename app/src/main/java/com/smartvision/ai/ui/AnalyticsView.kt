package com.smartvision.ai.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class AnalyticsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val values = listOf(0.58f, 0.78f, 0.48f, 0.88f, 0.68f, 0.92f)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val padding = 28f
        paint.textSize = 30f
        paint.color = Color.WHITE
        paint.isFakeBoldText = true
        canvas.drawText("AI Activity", padding, 42f, paint)
        paint.isFakeBoldText = false
        paint.textSize = 24f
        paint.color = Color.argb(190, 229, 231, 235)
        canvas.drawText("OCR 86  Objects 342  Waste 41", padding, 76f, paint)
        val barWidth = (width - padding * 2) / values.size
        values.forEachIndexed { index, value ->
            paint.color = if (index % 2 == 0) Color.rgb(91, 140, 255) else Color.rgb(0, 229, 255)
            val left = padding + index * barWidth + 8f
            val top = height - 18f - (height * 0.42f * value)
            canvas.drawRoundRect(left, top, left + barWidth - 16f, height - 18f, 10f, 10f, paint)
        }
    }
}
