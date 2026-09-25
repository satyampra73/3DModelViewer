package com.app.a3dmodelviewer.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.app.a3dmodelviewer.R
import com.app.a3dmodelviewer.ui.container.ModelContainerCard
import com.app.a3dmodelviewer.ui.dialog.ModelItem
import com.app.a3dmodelviewer.ui.dialog.ModelPickerComposeDialog
import com.app.a3dmodelviewer.ui.state.ModelItemState
import com.app.a3dmodelviewer.ui.theme.AccentBlue
import com.app.a3dmodelviewer.ui.theme.DarkBackground
import com.app.a3dmodelviewer.ui.theme.HeaderBackground

/**
 * Main interactive multi-model canvas screen.
 *
 * Hosts dynamically added 3D model containers with real-time bounds clamping,
 * active Z-ordering, model selection dialog, and header status bar.
 */
@Composable
fun ModelViewerScreen(
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val insets = WindowInsets.systemBars.asPaddingValues()

    val activeModels = remember { mutableStateListOf<ModelItemState>() }
    var modelPlacementCounter by remember { mutableIntStateOf(0) }
    var topZ by remember { mutableFloatStateOf(1f) }
    var showPickerDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(insets)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val parentWidthPx = constraints.maxWidth
            val parentHeightPx = constraints.maxHeight

            // Empty state placeholder
            if (activeModels.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.empty_state_title),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.empty_state_subtitle),
                        color = Color(0xFF9E9E9E),
                        fontSize = 13.sp
                    )
                }
            }

            // Render all active 3D model containers
            for (modelState in activeModels) {
                ModelContainerCard(
                    modelState = modelState,
                    parentWidthPx = parentWidthPx,
                    parentHeightPx = parentHeightPx,
                    onBringToFront = { state ->
                        topZ += 1f
                        state.zIndex = topZ
                    },
                    onClose = { state ->
                        activeModels.remove(state)
                    }
                )
            }

            // Top Status Header Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .zIndex(1000f),
                colors = CardDefaults.cardColors(containerColor = HeaderBackground),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.app_header_title),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    val count = activeModels.size
                    val countText = stringResource(
                        R.string.model_count_active,
                        count,
                        if (count != 1) "s" else ""
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x262196F3))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = countText,
                            color = AccentBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Add Model Extended Floating Action Button
            ExtendedFloatingActionButton(
                onClick = { showPickerDialog = true },
                containerColor = AccentBlue,
                contentColor = Color.White,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = "Add Model",
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.btn_add_model),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .zIndex(1001f)
            )

            // Model Selection Dialog
            if (showPickerDialog) {
                ModelPickerComposeDialog(
                    onDismissRequest = { showPickerDialog = false },
                    onModelSelected = { selectedModel ->
                        val initialSizePx = with(density) { 240.dp.roundToPx() }
                        val topOffset = with(density) { 70.dp.roundToPx() }
                        val step = modelPlacementCounter % 5
                        val rawPosX = with(density) { (16 + step * 20).dp.roundToPx() }.toFloat()
                        val rawPosY = topOffset + with(density) { (step * 28).dp.roundToPx() }.toFloat()

                        val maxPosX = (parentWidthPx - initialSizePx).coerceAtLeast(0).toFloat()
                        val maxPosY = (parentHeightPx - initialSizePx).coerceAtLeast(0).toFloat()

                        topZ += 1f
                        val newModel = ModelItemState(
                            modelItem = selectedModel,
                            initialX = rawPosX.coerceIn(0f, maxPosX),
                            initialY = rawPosY.coerceIn(topOffset.toFloat(), maxPosY),
                            initialSize = initialSizePx
                        ).apply {
                            zIndex = topZ
                        }

                        modelPlacementCounter++
                        activeModels.add(newModel)
                    }
                )
            }
        }
    }
}
