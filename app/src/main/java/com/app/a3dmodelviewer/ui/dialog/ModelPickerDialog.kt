package com.app.a3dmodelviewer.ui.dialog

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.a3dmodelviewer.R
import com.app.a3dmodelviewer.ui.theme.AccentBlue
import com.google.android.material.dialog.MaterialAlertDialogBuilder

data class ModelItem(
    val fileName: String,
    val displayName: String,
    val description: String
)

object ModelPickerDialog {

    val BUNDLED_MODELS = listOf(
        ModelItem("Bulb.glb", "Light Bulb", "6 labeled parts (Filament, Base, Glass, etc.)"),
        ModelItem("Fiagena.glb", "Fiagena", "7 labeled parts (Hook, Rings, Filament, etc.)"),
        ModelItem("Lungs.glb", "Human Lungs", "5 labeled parts (Larynx, Trachea, Bronchus, etc.)"),
        ModelItem("Microscope.glb", "Microscope", "12 labeled parts (Eyepiece, Objectives, Knobs, etc.)"),
        ModelItem("solarsystem.glb", "Solar System", "9 labeled celestial bodies (Planets & Sun)")
    )

    fun show(context: Context, onModelSelected: (ModelItem) -> Unit) {
        val items = BUNDLED_MODELS.map { item ->
            val builder = SpannableStringBuilder()
            builder.append(item.displayName, StyleSpan(Typeface.BOLD), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.append("\n")
            val subStart = builder.length
            builder.append("${item.fileName} • ${item.description}")
            builder.setSpan(RelativeSizeSpan(0.85f), subStart, builder.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(ForegroundColorSpan(0xFF888888.toInt()), subStart, builder.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder
        }.toTypedArray()

        MaterialAlertDialogBuilder(context)
            .setTitle("Add 3D Model")
            .setItems(items) { dialog, which ->
                onModelSelected(BUNDLED_MODELS[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

/**
 * Compose dialog to select and add one of the 5 bundled GLB models to the canvas.
 */
@Composable
fun ModelPickerComposeDialog(
    onDismissRequest: () -> Unit,
    onModelSelected: (ModelItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(R.string.dialog_add_model_title),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            LazyColumn {
                items(ModelPickerDialog.BUNDLED_MODELS) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onModelSelected(item)
                                onDismissRequest()
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = item.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${item.fileName} • ${item.description}",
                            color = Color(0xFF9E9E9E),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = stringResource(R.string.btn_cancel),
                    color = AccentBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(16.dp)
    )
}
