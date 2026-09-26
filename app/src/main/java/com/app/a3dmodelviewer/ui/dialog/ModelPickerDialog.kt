package com.app.a3dmodelviewer.ui.dialog

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import com.app.a3dmodelviewer.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

sealed class ModelSource {
    data class Asset(val assetPath: String) : ModelSource()
    data class UriSource(val uri: Uri) : ModelSource()
}

data class ModelItem(
    val fileName: String,
    val displayName: String,
    val description: String,
    val source: ModelSource = ModelSource.Asset(fileName)
)

object ModelPickerDialog {

    val BUNDLED_MODELS = listOf(
        ModelItem("Bulb.glb", "Light Bulb", "6 labeled parts (Filament, Base, Glass, etc.)", ModelSource.Asset("Bulb.glb")),
        ModelItem("Fiagena.glb", "Fiagena", "7 labeled parts (Hook, Rings, Filament, etc.)", ModelSource.Asset("Fiagena.glb")),
        ModelItem("Lungs.glb", "Human Lungs", "5 labeled parts (Larynx, Trachea, Bronchus, etc.)", ModelSource.Asset("Lungs.glb")),
        ModelItem("Microscope.glb", "Microscope", "12 labeled parts (Eyepiece, Objectives, Knobs, etc.)", ModelSource.Asset("Microscope.glb")),
        ModelItem("solarsystem.glb", "Solar System", "9 labeled celestial bodies (Planets & Sun)", ModelSource.Asset("solarsystem.glb"))
    )

    fun show(
        context: Context,
        onPickFromDevice: () -> Unit,
        onModelSelected: (ModelItem) -> Unit
    ) {
        val displayItems = mutableListOf<CharSequence>()

        // 1. Choose from Device option
        val devicePickerBuilder = SpannableStringBuilder()
        devicePickerBuilder.append(
            "📁 " + context.getString(R.string.choose_from_device),
            StyleSpan(Typeface.BOLD),
            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        devicePickerBuilder.append("\n")
        val deviceSubStart = devicePickerBuilder.length
        devicePickerBuilder.append(context.getString(R.string.choose_from_device_sub))
        devicePickerBuilder.setSpan(
            RelativeSizeSpan(0.85f),
            deviceSubStart,
            devicePickerBuilder.length,
            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        devicePickerBuilder.setSpan(
            ForegroundColorSpan(0xFF2196F3.toInt()),
            deviceSubStart,
            devicePickerBuilder.length,
            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        displayItems.add(devicePickerBuilder)

        // 2. Bundled models
        for (item in BUNDLED_MODELS) {
            val builder = SpannableStringBuilder()
            builder.append(item.displayName, StyleSpan(Typeface.BOLD), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.append("\n")
            val subStart = builder.length
            builder.append("${item.fileName} • ${item.description}")
            builder.setSpan(RelativeSizeSpan(0.85f), subStart, builder.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(ForegroundColorSpan(0xFF888888.toInt()), subStart, builder.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            displayItems.add(builder)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.dialog_add_model_title))
            .setItems(displayItems.toTypedArray()) { dialog, which ->
                if (which == 0) {
                    onPickFromDevice()
                } else {
                    onModelSelected(BUNDLED_MODELS[which - 1])
                }
                dialog.dismiss()
            }
            .setNegativeButton(context.getString(R.string.btn_cancel), null)
            .show()
    }

    fun show(context: Context, onModelSelected: (ModelItem) -> Unit) {
        show(context, onPickFromDevice = {}, onModelSelected = onModelSelected)
    }
}
