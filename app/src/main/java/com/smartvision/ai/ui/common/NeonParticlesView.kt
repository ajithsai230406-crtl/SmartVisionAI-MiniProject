package com.smartvision.ai.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

class NeonParticlesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particles = List(58) {
        Particle(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 2.4f + 0.8f, Random.nextFloat() * 6.28f)
    }
    private var frame = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        frame += 0.018f
        particles.forEachIndexed { index, particle ->
            val x = particle.x * width + sin(frame + particle.phase) * 18f
            val y = ((particle.y + frame * 0.012f * (index % 4 + 1)) % 1f) * height
            paint.color = if (index % 2 == 0) Color.parseColor("#6600E5FF") else Color.parseColor("#667B61FF")
            canvas.drawCircle(x, y, particle.radius, paint)
        }
        postInvalidateOnAnimation()
    }

    private data class Particle(val x: Float, val y: Float, val radius: Float, val phase: Float)
}
