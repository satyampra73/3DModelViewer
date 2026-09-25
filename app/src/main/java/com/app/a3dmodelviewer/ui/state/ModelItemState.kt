package com.app.a3dmodelviewer.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.app.a3dmodelviewer.engine.Model3DRenderer
import com.app.a3dmodelviewer.ui.container.InteractionMode
import com.app.a3dmodelviewer.ui.dialog.ModelItem
import java.util.UUID

/**
 * Observable UI state holder for an individual 3D model container.
 *
 * Coordinates (offsetX, offsetY) and dimensions (widthPx, heightPx) are backed by
 * mutable primitive states so layout updates can run in Compose's Layout/Draw phases
 * without triggering unnecessary recompositions.
 */
class ModelItemState(
    val id: String = UUID.randomUUID().toString(),
    val modelItem: ModelItem,
    initialX: Float,
    initialY: Float,
    initialSize: Int
) {
    var offsetX by mutableFloatStateOf(initialX)
    var offsetY by mutableFloatStateOf(initialY)
    var widthPx by mutableIntStateOf(initialSize)
    var heightPx by mutableIntStateOf(initialSize)
    var zIndex by mutableFloatStateOf(0f)
    var mode by mutableStateOf(InteractionMode.NORMAL)
    var isLabelsVisible by mutableStateOf(false)

    var renderer: Model3DRenderer? = null

    fun clampToBounds(parentW: Int, parentH: Int) {
        if (parentW <= 0 || parentH <= 0) return
        val maxTransX = (parentW - widthPx).coerceAtLeast(0).toFloat()
        val maxTransY = (parentH - heightPx).coerceAtLeast(0).toFloat()
        offsetX = offsetX.coerceIn(0f, maxTransX)
        offsetY = offsetY.coerceIn(0f, maxTransY)
    }

    fun resize(scaleFactor: Float, parentW: Int, parentH: Int, minSizePx: Int) {
        if (parentW <= 0 || parentH <= 0 || scaleFactor <= 0f) return
        val maxAllowedW = (parentW * 0.95f).toInt().coerceAtLeast(minSizePx)
        val maxAllowedH = (parentH * 0.95f).toInt().coerceAtLeast(minSizePx)

        val newW = (widthPx * scaleFactor).toInt().coerceIn(minSizePx, maxAllowedW)
        val newH = (heightPx * scaleFactor).toInt().coerceIn(minSizePx, maxAllowedH)

        widthPx = newW
        heightPx = newH
        clampToBounds(parentW, parentH)
    }
}
