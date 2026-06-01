package com.smartvision.ai.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 229, 255)
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }
    private var phase = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1200
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            phase = it.animatedFraction
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val center = height / 2f
        val count = 32
        val gap = width / (count + 1f)
        repeat(count) { index ->
            val amp = (sin(index * 0.65f + phase * 6.28f) * 0.5f + 0.5f) * height * 0.35f + 8f
            val x = gap * (index + 1)
            canvas.drawLine(x, center - amp, x, center + amp, paint)
        }
    }
}
