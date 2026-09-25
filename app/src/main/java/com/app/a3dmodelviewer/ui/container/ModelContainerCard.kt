package com.app.a3dmodelviewer.ui.container

import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.app.a3dmodelviewer.R
import com.app.a3dmodelviewer.engine.Model3DRenderer
import com.app.a3dmodelviewer.labels.LabelOverlayCanvas
import com.app.a3dmodelviewer.labels.TrackedModelLabel
import com.app.a3dmodelviewer.ui.state.ModelItemState
import com.app.a3dmodelviewer.ui.theme.AccentBlue
import com.app.a3dmodelviewer.ui.theme.AccentOrange
import com.app.a3dmodelviewer.ui.theme.AccentPink
import com.app.a3dmodelviewer.ui.theme.CardBackground
import com.app.a3dmodelviewer.ui.theme.DangerRed
import com.app.a3dmodelviewer.ui.theme.InactiveGray
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Independent, draggable, resizable 3D model container Composable.
 *
 * Uses lambda offset and size modifiers so real-time drag/resize transforms
 * execute in the Layout/Draw phase without triggering recomposition storms.
 */
@Composable
fun ModelContainerCard(
    modelState: ModelItemState,
    parentWidthPx: Int,
    parentHeightPx: Int,
    onBringToFront: (ModelItemState) -> Unit,
    onClose: (ModelItemState) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val minSizePx = remember(density) { with(density) { 150.dp.roundToPx() } }

    val borderColor = if (modelState.mode == InteractionMode.NORMAL) {
        Color(0x662196F3)
    } else {
        AccentOrange
    }

    val currentLabels = remember { mutableStateListOf<TrackedModelLabel>() }

    // Remember a dedicated Model3DRenderer for this container instance
    val renderer = remember(modelState.id) {
        val textureView = TextureView(context).apply { isOpaque = false }
        Model3DRenderer(context, textureView).apply {
            isLabelsVisible = modelState.isLabelsVisible
            onLabelsUpdated = { updatedList ->
                currentLabels.clear()
                currentLabels.addAll(updatedList)
            }
            loadGlbFromAssets(modelState.modelItem.fileName)
            modelState.renderer = this
        }
    }

    // Clean up Filament resources when this container leaves composition
    DisposableEffect(modelState.id) {
        onDispose {
            renderer.destroy()
            modelState.renderer = null
        }
    }

    // Convert pixel dimensions to Dp for Compose layout
    val widthDp = with(density) { modelState.widthPx.toDp() }
    val heightDp = with(density) { modelState.heightPx.toDp() }

    Card(
        modifier = modifier
            .zIndex(modelState.zIndex)
            .offset { IntOffset(modelState.offsetX.roundToInt(), modelState.offsetY.roundToInt()) }
            .size(widthDp, heightDp)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(Color(0x80000000))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = modelState.modelItem.displayName,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )

                // Mode Toggle Button
                val modeBg = if (modelState.mode == InteractionMode.NORMAL) AccentBlue else AccentOrange
                val modeText = if (modelState.mode == InteractionMode.NORMAL) {
                    stringResource(R.string.mode_normal)
                } else {
                    stringResource(R.string.mode_interact)
                }
                val modeIcon = if (modelState.mode == InteractionMode.NORMAL) {
                    R.drawable.ic_mode_normal
                } else {
                    R.drawable.ic_mode_interact
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(modeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown()
                                onBringToFront(modelState)
                                modelState.mode = if (modelState.mode == InteractionMode.NORMAL) {
                                    InteractionMode.INTERACTION
                                } else {
                                    InteractionMode.NORMAL
                                }
                                renderer.requestRender()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(modeIcon),
                            contentDescription = modeText,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = modeText,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Label Toggle Button
                val labelBg = if (modelState.isLabelsVisible) AccentPink else InactiveGray
                val labelTint = if (modelState.isLabelsVisible) Color.White else Color.LightGray
                IconButton(
                    onClick = {
                        onBringToFront(modelState)
                        val newVis = !modelState.isLabelsVisible
                        modelState.isLabelsVisible = newVis
                        renderer.isLabelsVisible = newVis
                        renderer.requestRender()
                    },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = labelBg)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_label),
                        contentDescription = "Toggle Labels",
                        tint = labelTint,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Close Button
                IconButton(
                    onClick = { onClose(modelState) },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = DangerRed)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Close Model",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // 3D Rendering Viewport & 2D Label Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(modelState.mode, parentWidthPx, parentHeightPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            onBringToFront(modelState)

                            var prevPointerCount = 1
                            var prevPinchDist = 0f
                            var prevX = down.position.x
                            var prevY = down.position.y

                            while (true) {
                                val event = awaitPointerEvent()
                                val pressedList = event.changes.filter { it.pressed }
                                if (pressedList.isEmpty()) break

                                val currentPointerCount = pressedList.size

                                if (currentPointerCount == 1) {
                                    val pointer = pressedList[0]
                                    val currX = pointer.position.x
                                    val currY = pointer.position.y

                                    if (prevPointerCount == 1) {
                                        val dx = currX - prevX
                                        val dy = currY - prevY

                                        when (modelState.mode) {
                                            InteractionMode.NORMAL -> {
                                                modelState.offsetX += dx
                                                modelState.offsetY += dy
                                                modelState.clampToBounds(parentWidthPx, parentHeightPx)
                                                renderer.requestRender()
                                            }
                                            InteractionMode.INTERACTION -> {
                                                renderer.rotateBy(dx, dy)
                                            }
                                        }
                                    }
                                    prevX = currX
                                    prevY = currY
                                    pointer.consume()
                                } else if (currentPointerCount >= 2) {
                                    val p1 = pressedList[0]
                                    val p2 = pressedList[1]
                                    val currentDist = hypot(
                                        p1.position.x - p2.position.x,
                                        p1.position.y - p2.position.y
                                    )

                                    if (prevPointerCount >= 2 && prevPinchDist > 0f) {
                                        val scaleFactor = currentDist / prevPinchDist
                                        when (modelState.mode) {
                                            InteractionMode.NORMAL -> {
                                                modelState.resize(scaleFactor, parentWidthPx, parentHeightPx, minSizePx)
                                                renderer.requestRender()
                                            }
                                            InteractionMode.INTERACTION -> {
                                                renderer.zoomBy(scaleFactor)
                                            }
                                        }
                                    }
                                    prevPinchDist = currentDist
                                    p1.consume()
                                    p2.consume()
                                }

                                prevPointerCount = currentPointerCount
                            }
                        }
                    }
            ) {
                // Filament 3D Surface
                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            isOpaque = false
                            surfaceTextureListener = renderer
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 2D Leader Lines & Labels Overlay
                LabelOverlayCanvas(
                    labels = currentLabels,
                    isLabelsVisible = modelState.isLabelsVisible,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
