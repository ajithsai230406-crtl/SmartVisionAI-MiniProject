package com.smartvision.ai.ui.common

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

fun View.startFloating(distance: Float = 14f, duration: Long = 2600L) {
    ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, -distance, distance).apply {
        repeatCount = ObjectAnimator.INFINITE
        repeatMode = ObjectAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
        this.duration = duration
        start()
    }
}

fun View.startPulse(minScale: Float = 0.96f, maxScale: Float = 1.05f, duration: Long = 1400L) {
    ObjectAnimator.ofFloat(this, View.SCALE_X, minScale, maxScale).apply {
        repeatCount = ObjectAnimator.INFINITE
        repeatMode = ObjectAnimator.REVERSE
        this.duration = duration
        start()
    }
    ObjectAnimator.ofFloat(this, View.SCALE_Y, minScale, maxScale).apply {
        repeatCount = ObjectAnimator.INFINITE
        repeatMode = ObjectAnimator.REVERSE
        this.duration = duration
        start()
    }
}
