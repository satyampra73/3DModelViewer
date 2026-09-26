package com.app.a3dmodelviewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.a3dmodelviewer.engine.FilamentManager
import com.app.a3dmodelviewer.ui.container.ModelContainerView
import com.app.a3dmodelviewer.ui.dialog.ModelItem
import com.app.a3dmodelviewer.ui.dialog.ModelPickerDialog
import com.app.a3dmodelviewer.ui.dialog.ModelSource
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var modelsCanvas: FrameLayout
    private lateinit var emptyStateView: LinearLayout
    private lateinit var tvModelCount: TextView
    private lateinit var btnAddModel: ExtendedFloatingActionButton

    private val activeContainers = mutableListOf<ModelContainerView>()
    private var modelPlacementCounter = 0

    private val pickModelLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            handleSelectedModelUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize single shared Filament Engine
        FilamentManager.init(applicationContext)

        modelsCanvas = findViewById(R.id.modelsCanvas)
        emptyStateView = findViewById(R.id.emptyStateView)
        tvModelCount = findViewById(R.id.tvModelCount)
        btnAddModel = findViewById(R.id.btnAddModel)

        btnAddModel.setOnClickListener {
            ModelPickerDialog.show(
                context = this,
                onPickFromDevice = {
                    pickModelLauncher.launch(arrayOf("*/*"))
                },
                onModelSelected = { selectedModel ->
                    addModelContainer(selectedModel)
                }
            )
        }

        updateUiState()
    }

    fun addModelContainer(modelItem: ModelItem): ModelContainerView {
        val density = resources.displayMetrics.density
        val initialSizePx = (240 * density).toInt()

        val parentW = if (modelsCanvas.width > 0) modelsCanvas.width else resources.displayMetrics.widthPixels
        val parentH = if (modelsCanvas.height > 0) modelsCanvas.height else resources.displayMetrics.heightPixels

        val maxPosX = (parentW - initialSizePx).coerceAtLeast(0).toFloat()
        val maxPosY = (parentH - initialSizePx).coerceAtLeast(0).toFloat()

        // Staggered initial position, clamped strictly within visible canvas bounds
        val topOffset = 70 * density
        val step = modelPlacementCounter % 5
        val rawPosX = (16 + step * 20) * density
        val rawPosY = topOffset + (step * 28) * density

        val container = ModelContainerView(this).apply {
            layoutParams = FrameLayout.LayoutParams(initialSizePx, initialSizePx)
            translationX = rawPosX.coerceIn(0f, maxPosX)
            translationY = rawPosY.coerceIn(topOffset, maxPosY)

            modelPlacementCounter++

            // Close listener: cleanly removes and deallocates this container
            onCloseListener = { closedContainer ->
                modelsCanvas.removeView(closedContainer)
                activeContainers.remove(closedContainer)
                updateUiState()
            }

            // Load the chosen GLB model
            loadModel(modelItem)
        }

        activeContainers.add(container)
        modelsCanvas.addView(container)
        updateUiState()

        return container
    }

    private fun handleSelectedModelUri(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Not all document providers grant persistable permissions
        }

        if (!isValidGlbHeader(uri)) {
            Toast.makeText(this, getString(R.string.error_invalid_glb), Toast.LENGTH_LONG).show()
            return
        }

        val fileName = getFileNameFromUri(uri)
        val displayName = if (fileName.contains('.')) {
            fileName.substringBeforeLast('.')
        } else {
            fileName
        }.ifBlank { getString(R.string.default_model_title) }

        val modelItem = ModelItem(
            fileName = fileName,
            displayName = displayName,
            description = getString(R.string.custom_model_description),
            source = ModelSource.UriSource(uri)
        )
        addModelContainer(modelItem)
    }

    private fun isValidGlbHeader(uri: Uri): Boolean {
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(4)
                val read = stream.read(header)
                read == 4 &&
                    header[0] == 'g'.code.toByte() &&
                    header[1] == 'l'.code.toByte() &&
                    header[2] == 'T'.code.toByte() &&
                    header[3] == 'F'.code.toByte()
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "model.glb"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val displayName = cursor.getString(nameIndex)
                    if (!displayName.isNullOrBlank()) {
                        name = displayName
                    }
                }
            }
        } catch (e: Exception) {
            uri.lastPathSegment?.let { segment ->
                val clean = segment.substringAfterLast('/')
                if (clean.isNotBlank()) name = clean
            }
        }
        return name
    }

    private fun updateUiState() {
        val count = activeContainers.size
        emptyStateView.visibility = if (count == 0) View.VISIBLE else View.GONE
        tvModelCount.text = getString(R.string.model_count_active, count, if (count != 1) "s" else "")
    }

    override fun onResume() {
        super.onResume()
        for (container in activeContainers) {
            container.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        for (container in activeContainers) {
            container.onPause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanly destroy each active container's Filament resources
        for (container in activeContainers) {
            container.destroy()
        }
        activeContainers.clear()

        // Shutdown shared Filament Engine
        if (isFinishing) {
            FilamentManager.destroy()
        }
    }
}