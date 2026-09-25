package com.app.a3dmodelviewer.labels

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity

/**
 * Encapsulates pre-allocated Paint, Rect, and drawing math for zero-allocation
 * label rendering inside Compose draw passes.
 */
class LabelPaints(density: Float, fontScale: Float) {
    private val dotRadius = 3.5f * density
    private val cornerRadius = 6f * density
    private val paddingH = 8f * density
    private val paddingV = 4f * density
    private val lineOffsetDx = 28f * density
    private val lineOffsetDy = -20f * density
    private val viewMargin = 8f * density

    private val dotFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2196F3.toInt()
        style = Paint.Style.FILL
    }

    private val dotStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCC2196F3.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }

    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xEE1E1E1E.toInt()
        style = Paint.Style.FILL
    }

    private val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66FFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 11f * density * fontScale
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val textBounds = Rect()
    private val badgeRect = RectF()

    fun drawLabel(canvas: android.graphics.Canvas, label: TrackedModelLabel, width: Float, height: Float) {
        val anchorX = label.screenX
        val anchorY = label.screenY

        val text = label.labelText
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val textWidth = textPaint.measureText(text)
        val textHeight = textBounds.height().toFloat()

        val badgeWidth = textWidth + paddingH * 2
        val badgeHeight = textHeight + paddingV * 2

        var badgeLeft = anchorX + lineOffsetDx
        var badgeTop = anchorY + lineOffsetDy - badgeHeight * 0.5f

        if (badgeLeft + badgeWidth > width - viewMargin) {
            badgeLeft = anchorX - lineOffsetDx - badgeWidth
        }
        if (badgeLeft < viewMargin) {
            badgeLeft = viewMargin
        }
        if (badgeTop < viewMargin) {
            badgeTop = viewMargin
        }
        if (badgeTop + badgeHeight > height - viewMargin) {
            badgeTop = height - viewMargin - badgeHeight
        }

        val badgeRight = badgeLeft + badgeWidth
        val badgeBottom = badgeTop + badgeHeight
        badgeRect.set(badgeLeft, badgeTop, badgeRight, badgeBottom)

        val lineTargetX = if (anchorX < badgeLeft) badgeLeft else if (anchorX > badgeRight) badgeRight else anchorX
        val lineTargetY = badgeTop + badgeHeight * 0.5f

        // 1. Draw connector line
        canvas.drawLine(anchorX, anchorY, lineTargetX, lineTargetY, linePaint)

        // 2. Draw anchor point
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

/**
 * Zero-allocation Compose Canvas rendering 2D leader lines and badge overlays.
 */
@Composable
fun LabelOverlayCanvas(
    labels: List<TrackedModelLabel>,
    isLabelsVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isLabelsVisible || labels.isEmpty()) return

    val density = LocalDensity.current.density
    val fontScale = LocalDensity.current.fontScale

    val paints = remember(density, fontScale) {
        LabelPaints(density, fontScale)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val w = size.width
            val h = size.height
            for (label in labels) {
                if (!label.isVisible) continue
                paints.drawLabel(nativeCanvas, label, w, h)
            }
        }
    }
}
