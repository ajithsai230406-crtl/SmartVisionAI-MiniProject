package com.smartvision.ai.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.animation.doOnEnd
import kotlin.math.sin
import kotlin.random.Random

class ParticleBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particles = List(42) {
        Particle(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 2.2f + 0.7f)
    }
    private var phase = 0f
    private var animator: ValueAnimator? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 7000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                phase = it.animatedFraction
                invalidate()
            }
            doOnEnd { animator = null }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.strokeWidth = 1.1f
        particles.forEachIndexed { index, particle ->
            val x = particle.x * width
            val y = ((particle.y + phase * 0.08f + sin(phase * 6.28f + index) * 0.015f) % 1f) * height
            paint.color = Color.argb(70, 77, 168, 255)
            canvas.drawCircle(x, y, particle.radius, paint)
            if (index % 5 == 0) {
                paint.color = Color.argb(28, 0, 229, 255)
                canvas.drawLine(x, y, width * 0.5f, height * 0.28f, paint)
            }
        }
    }

    private data class Particle(val x: Float, val y: Float, val radius: Float)
}
