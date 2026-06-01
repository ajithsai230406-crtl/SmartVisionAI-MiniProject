package com.smartvision.ai.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class ScanHudView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val cyan = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val purple = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7B61FF")
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8800E5FF")
        strokeWidth = 3f
    }
    private var sweep = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rect = RectF(width * 0.12f, height * 0.23f, width * 0.88f, height * 0.62f)
        canvas.drawRoundRect(rect, 24f, 24f, purple)
        val corner = 58f
        canvas.drawLine(rect.left, rect.top, rect.left + corner, rect.top, cyan)
        canvas.drawLine(rect.left, rect.top, rect.left, rect.top + corner, cyan)
        canvas.drawLine(rect.right, rect.top, rect.right - corner, rect.top, cyan)
        canvas.drawLine(rect.right, rect.top, rect.right, rect.top + corner, cyan)
        canvas.drawLine(rect.left, rect.bottom, rect.left + corner, rect.bottom, cyan)
        canvas.drawLine(rect.left, rect.bottom, rect.left, rect.bottom - corner, cyan)
        canvas.drawLine(rect.right, rect.bottom, rect.right - corner, rect.bottom, cyan)
        canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - corner, cyan)
        val y = rect.top + (rect.height() * sweep)
        canvas.drawLine(rect.left + 20f, y, rect.right - 20f, y, line)
        sweep += 0.008f
        if (sweep > 1f) sweep = 0f
        postInvalidateOnAnimation()
    }
}
