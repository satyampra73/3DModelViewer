package com.app.a3dmodelviewer.ui.dialog

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Model description representing a bundled 3D GLB asset.
 */
data class ModelItem(
    val fileName: String,
    val displayName: String,
    val description: String
)

/**
 * Dialog for selecting and adding one of the 5 bundled GLB models to the canvas.
 */
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
