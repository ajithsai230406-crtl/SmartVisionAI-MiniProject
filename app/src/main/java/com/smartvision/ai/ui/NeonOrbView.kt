package com.smartvision.ai.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class NeonOrbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var pulse = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1500
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener {
            pulse = it.animatedFraction
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
        val radius = min(width, height) / 2f
        val centerX = width / 2f
        val centerY = height / 2f
        paint.shader = RadialGradient(
            centerX,
            centerY,
            radius,
            intArrayOf(Color.WHITE, Color.rgb(91, 140, 255), Color.argb(0, 0, 229, 255)),
            floatArrayOf(0f, 0.35f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, radius * (0.42f + pulse * 0.06f), paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.argb(150, 0, 229, 255)
        canvas.drawCircle(centerX, centerY, radius * (0.65f + pulse * 0.12f), paint)
        paint.color = Color.argb(70, 91, 140, 255)
        canvas.drawCircle(centerX, centerY, radius * (0.88f + pulse * 0.08f), paint)
        paint.style = Paint.Style.FILL
    }
}
