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

    private val vectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E88E5")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }

    private val componentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#43A047")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    /** Whether to draw the vector + X/Y component projections on top of the joystick. */
    var showVectorOverlay: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    /** Current normalized X in [-1, 1]. */
    var normalizedX: Float = 0f
        private set

    /** Current normalized Y in [-1, 1]. */
    var normalizedY: Float = 0f
        private set

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

        // Work in the drawable content area (exclude padding) so circles never get clipped.
        val contentW = (w - paddingLeft - paddingRight).coerceAtLeast(0)
        val contentH = (h - paddingTop - paddingBottom).coerceAtLeast(0)

        centerX = paddingLeft + contentW / 2f
        centerY = paddingTop + contentH / 2f

        val contentMin = min(contentW, contentH).toFloat()
        val maxRadius = contentMin / 2f

        // Tunables (keep simple, but guarantee no clipping):
        // - knob takes a reasonable fraction of the view
        // - base radius is the remaining radius minus a tiny safety margin
        val safetyMarginPx = 2f

        // Knob size: ~12% of the content min dimension (radius).
        knobRadius = maxRadius * 0.12f

        // Base size: make the big disk a bit smaller and ensure base + knob <= maxRadius.
        baseRadius = (maxRadius * 0.78f).coerceAtMost(maxRadius - knobRadius - safetyMarginPx)
            .coerceAtLeast(0f)

        resetKnob(notify = false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (baseRadius <= 0f) return
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint)

        if (showVectorOverlay) {
            // Main vector (center -> knob)
            canvas.drawLine(centerX, centerY, knobX, knobY, vectorPaint)
            // X component projection (center -> (knobX, centerY))
            canvas.drawLine(centerX, centerY, knobX, centerY, componentPaint)
            // Y component projection ((knobX, centerY) -> knob)
            canvas.drawLine(knobX, centerY, knobX, knobY, componentPaint)
        }

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

        normalizedX = ((knobX - centerX) / baseRadius).coerceIn(-1f, 1f)
        normalizedY = ((knobY - centerY) / baseRadius).coerceIn(-1f, 1f)

        invalidate()
        moveListener?.invoke(normalizedX, normalizedY)
    }

    private fun resetKnob(notify: Boolean) {
        knobX = centerX
        knobY = centerY
        normalizedX = 0f
        normalizedY = 0f
        invalidate()
        if (notify) moveListener?.invoke(0f, 0f)
    }
}
