package com.app.a3dmodelviewer.labels

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

class LabelOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density

    // Configurable visual styling
    private val dotRadius = 3.5f * density
    private val cornerRadius = 6f * density
    private val paddingH = 8f * density
    private val paddingV = 4f * density
    private val lineOffsetDx = 28f * density
    private val lineOffsetDy = -20f * density

    // Pre-allocated Paint instances
    private val dotFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2196F3.toInt() // Accent blue
        style = Paint.Style.FILL
    }

    private val dotStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCC2196F3.toInt() // Translucent blue line
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }

    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xEE1E1E1E.toInt() // High-contrast dark charcoal
        style = Paint.Style.FILL
    }

    private val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66FFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 11f * density * context.resources.configuration.fontScale
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    // Reusable bounds to avoid allocation
    private val textBounds = Rect()
    private val badgeRect = RectF()

    private var labels: List<TrackedModelLabel> = emptyList()

    /**
     * Master visibility toggle for all labels.
     */
    var isLabelsVisible: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    /**
     * Updates the tracked labels and triggers a lightweight redraw.
     */
    fun updateLabels(trackedLabels: List<TrackedModelLabel>) {
        this.labels = trackedLabels
        if (isLabelsVisible) {
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isLabelsVisible || labels.isEmpty()) return

        for (label in labels) {
            if (!label.isVisible) continue

            val anchorX = label.screenX
            val anchorY = label.screenY

            // Measure text without object allocation
            val text = label.labelText
            textPaint.getTextBounds(text, 0, text.length, textBounds)
            val textWidth = textPaint.measureText(text)
            val textHeight = textBounds.height().toFloat()

            // Badge dimensions
            val badgeWidth = textWidth + paddingH * 2
            val badgeHeight = textHeight + paddingV * 2

            // Calculate badge position with edge clamping to stay inside view
            var badgeLeft = anchorX + lineOffsetDx
            var badgeTop = anchorY + lineOffsetDy - badgeHeight * 0.5f

            // Clamp badge within view boundaries
            if (badgeLeft + badgeWidth > width - 8 * density) {
                badgeLeft = anchorX - lineOffsetDx - badgeWidth
            }
            if (badgeLeft < 8 * density) {
                badgeLeft = 8 * density
            }
            if (badgeTop < 8 * density) {
                badgeTop = 8 * density
            }
            if (badgeTop + badgeHeight > height - 8 * density) {
                badgeTop = height - 8 * density - badgeHeight
            }

            val badgeRight = badgeLeft + badgeWidth
            val badgeBottom = badgeTop + badgeHeight
            badgeRect.set(badgeLeft, badgeTop, badgeRight, badgeBottom)

            // Determine line target on the badge edge
            val lineTargetX = if (anchorX < badgeLeft) badgeLeft else if (anchorX > badgeRight) badgeRight else anchorX
            val lineTargetY = badgeTop + badgeHeight * 0.5f

            // 1. Draw connector line
            canvas.drawLine(anchorX, anchorY, lineTargetX, lineTargetY, linePaint)

            // 2. Draw anchor point (inner dot + outer white ring)
            canvas.drawCircle(anchorX, anchorY, dotRadius, dotFillPaint)
            canvas.drawCircle(anchorX, anchorY, dotRadius, dotStrokePaint)

            // 3. Draw badge rounded rectangle background & border
            canvas.drawRoundRect(badgeRect, cornerRadius, cornerRadius, badgeBgPaint)
            canvas.drawRoundRect(badgeRect, cornerRadius, cornerRadius, badgeBorderPaint)

            // 4. Draw label text
            val textX = badgeLeft + paddingH
            val textY = badgeTop + paddingV + textHeight - textBounds.bottom
            canvas.drawText(text, textX, textY, textPaint)
        }
    }
}
