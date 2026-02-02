package com.example.pivech3.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.min

/**
 * A lightweight joystick view.
 *
 * - Reports normalized axis values in range [-1, 1]
 * - (0,0) is centered
 * - +X to the right, +Y downward (Android view coordinates)
 */
class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        style = Paint.Style.FILL
    }

    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.FILL
    }

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var knobRadius = 0f

    private var knobX = 0f
    private var knobY = 0f

    private var moveListener: ((x: Float, y: Float) -> Unit)? = null

    fun setOnMoveListener(listener: ((x: Float, y: Float) -> Unit)?) {
        moveListener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = min(w, h) * 0.45f
        knobRadius = baseRadius * 0.35f
        resetKnob(notify = false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (baseRadius <= 0f) return
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint)
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                updateKnob(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                resetKnob(notify = true)
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                resetKnob(notify = true)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        // Required for accessibility when overriding onTouchEvent.
        return super.performClick()
    }

    private fun updateKnob(x: Float, y: Float) {
        val dx = x - centerX
        val dy = y - centerY
        val distance = hypot(dx, dy)

        val scale = if (distance > baseRadius && distance > 0f) baseRadius / distance else 1f
        knobX = centerX + dx * scale
        knobY = centerY + dy * scale
        invalidate()

        val normalizedX = ((knobX - centerX) / baseRadius).coerceIn(-1f, 1f)
        val normalizedY = ((knobY - centerY) / baseRadius).coerceIn(-1f, 1f)
        moveListener?.invoke(normalizedX, normalizedY)
    }

    private fun resetKnob(notify: Boolean) {
        knobX = centerX
        knobY = centerY
        invalidate()
        if (notify) moveListener?.invoke(0f, 0f)
    }
}
