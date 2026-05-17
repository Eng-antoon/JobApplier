package com.aplicator.jobapplier.service

import android.animation.ValueAnimator
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.abs
import kotlin.math.hypot

class BubblePhysics(
    private val windowManager: WindowManager,
    private var screenWidth: Int,
    private val bubbleSizePx: Int,
) {
    private val edgeMargin = 8
    private var snapAnimator: ValueAnimator? = null
    private var dismissAnimator: ValueAnimator? = null

    companion object {
        private const val FLING_THRESHOLD = 1500f
        private const val DISMISS_ZONE_RADIUS_DP = 80
    }

    fun updateScreenWidth(newWidth: Int) {
        screenWidth = newWidth
    }

    fun snapToEdge(
        view: View,
        params: WindowManager.LayoutParams,
        currentX: Int,
        currentY: Int,
        velocityX: Float = 0f,
    ) {
        snapAnimator?.cancel()

        val bubbleCenter = currentX + bubbleSizePx / 2
        val targetX = when {
            abs(velocityX) > FLING_THRESHOLD -> {
                if (velocityX > 0) screenWidth - bubbleSizePx - edgeMargin else edgeMargin
            }
            bubbleCenter < screenWidth / 2 -> edgeMargin
            else -> screenWidth - bubbleSizePx - edgeMargin
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

    fun isInDismissZone(
        bubbleX: Int,
        bubbleY: Int,
        screenHeight: Int,
        density: Float,
    ): Boolean {
        val bubbleCenterX = bubbleX + bubbleSizePx / 2
        val bubbleCenterY = bubbleY + bubbleSizePx / 2

        val targetX = screenWidth / 2
        val targetY = screenHeight - (80 * density).toInt()
        val radius = (DISMISS_ZONE_RADIUS_DP * density).toInt()

        val distance = hypot(
            (bubbleCenterX - targetX).toDouble(),
            (bubbleCenterY - targetY).toDouble(),
        )
        return distance < radius
    }

    fun animateToDismiss(
        view: View,
        params: WindowManager.LayoutParams,
        screenHeight: Int,
        density: Float,
        onComplete: () -> Unit,
    ) {
        dismissAnimator?.cancel()

        val targetX = screenWidth / 2 - bubbleSizePx / 2
        val targetY = screenHeight - (80 * density).toInt() - bubbleSizePx / 2
        val startX = params.x
        val startY = params.y

        dismissAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                params.x = startX + ((targetX - startX) * fraction).toInt()
                params.y = startY + ((targetY - startY) * fraction).toInt()
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (_: Exception) {}
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onComplete()
                }
            })
            start()
        }
    }

    fun bounceBack(
        view: View,
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
        dismissAnimator?.cancel()
        dismissAnimator = null
    }
}
