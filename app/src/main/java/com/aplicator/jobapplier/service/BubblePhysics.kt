package com.aplicator.jobapplier.service

import android.animation.ValueAnimator
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.view.animation.DecelerateInterpolator

class BubblePhysics(
    private val windowManager: WindowManager,
    private val screenWidth: Int,
) {
    private val edgeMargin = 8
    private var snapAnimator: ValueAnimator? = null

    fun snapToEdge(
        view: android.view.View,
        params: WindowManager.LayoutParams,
        currentX: Int,
        currentY: Int,
        velocityX: Float = 0f,
    ) {
        snapAnimator?.cancel()

        val targetX = if (currentX + 28 < screenWidth / 2) {
            edgeMargin
        } else {
            screenWidth - 56 - edgeMargin
        }

        snapAnimator = ValueAnimator.ofInt(currentX, targetX).apply {
            duration = 350
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                params.x = value
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (_: Exception) {}
            }
            start()
        }
    }

    fun bounceBack(
        view: android.view.View,
        params: WindowManager.LayoutParams,
        fromY: Int,
        toY: Int,
    ) {
        ValueAnimator.ofInt(fromY, toY).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                params.y = value
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (_: Exception) {}
            }
            start()
        }
    }

    fun release() {
        snapAnimator?.cancel()
        snapAnimator = null
    }
}
