package com.app.a3dmodelviewer.ui.container

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.TextureView
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.app.a3dmodelviewer.R
import com.app.a3dmodelviewer.engine.Model3DRenderer
import com.app.a3dmodelviewer.labels.LabelOverlayView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlin.math.hypot

class ModelContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    // UI Components
    private val cardView: MaterialCardView
    private val headerBar: LinearLayout
    private val tvModelTitle: TextView
    private val btnModeToggle: MaterialButton
    private val btnLabelToggle: MaterialButton
    private val btnClose: MaterialButton
    private val textureView: TextureView
    private val labelOverlayView: LabelOverlayView

    // Model & Renderer
    private var model3DRenderer: Model3DRenderer? = null
    var modelFileName: String = ""
        private set

    // Interaction State
    var interactionMode: InteractionMode = InteractionMode.NORMAL
        private set

    // Close Callback
    var onCloseListener: ((ModelContainerView) -> Unit)? = null

    // Touch & Resize State
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var downRawX = 0f
    private var downRawY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private val minSizePx = (150 * density).toInt()

    private val scaleGestureDetector: ScaleGestureDetector

    init {
        LayoutInflater.from(context).inflate(R.layout.view_model_container, this, true)

        cardView = findViewById(R.id.containerCard)
        headerBar = findViewById(R.id.headerBar)
        tvModelTitle = findViewById(R.id.tvModelTitle)
        btnModeToggle = findViewById(R.id.btnModeToggle)
        btnLabelToggle = findViewById(R.id.btnLabelToggle)
        btnClose = findViewById(R.id.btnClose)
        textureView = findViewById(R.id.containerTextureView)
        labelOverlayView = findViewById(R.id.containerLabelOverlay)

        // Labels hidden initially per task requirement
        labelOverlayView.isLabelsVisible = false

        scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                when (interactionMode) {
                    InteractionMode.NORMAL -> {
                        val scaleFactor = detector.scaleFactor
                        val currentWidth = width
                        val currentHeight = height
                        if (currentWidth <= 0 || currentHeight <= 0) return false

                        val parentView = parent as? View
                        val parentW = parentView?.width ?: resources.displayMetrics.widthPixels
                        val parentH = parentView?.height ?: resources.displayMetrics.heightPixels

                        val maxAllowedW = (parentW * 0.95f).toInt().coerceAtLeast(minSizePx)
                        val maxAllowedH = (parentH * 0.95f).toInt().coerceAtLeast(minSizePx)

                        val newWidth = (currentWidth * scaleFactor).toInt().coerceIn(minSizePx, maxAllowedW)
                        val newHeight = (currentHeight * scaleFactor).toInt().coerceIn(minSizePx, maxAllowedH)

                        val lp = layoutParams
                        if (lp != null && (lp.width != newWidth || lp.height != newHeight)) {
                            lp.width = newWidth
                            lp.height = newHeight
                            layoutParams = lp
                        }

                        // Dynamically constrain container within canvas bounds when expanding
                        val maxTransX = (parentW - newWidth).coerceAtLeast(0).toFloat()
                        val maxTransY = (parentH - newHeight).coerceAtLeast(0).toFloat()
                        translationX = translationX.coerceIn(0f, maxTransX)
                        translationY = translationY.coerceIn(0f, maxTransY)

                        model3DRenderer?.requestRender()
                        return true
                    }
                    InteractionMode.INTERACTION -> {
                        val scaleFactor = detector.scaleFactor
                        model3DRenderer?.zoomBy(scaleFactor)
                        return true
                    }
                }
            }
        }).apply {
            isQuickScaleEnabled = false
        }

        setupControlListeners()
        updateModeVisuals()
    }

    private fun setupControlListeners() {
        // Mode Toggle Button
        btnModeToggle.setOnClickListener {
            interactionMode = if (interactionMode == InteractionMode.NORMAL) {
                InteractionMode.INTERACTION
            } else {
                InteractionMode.NORMAL
            }
            updateModeVisuals()
            model3DRenderer?.requestRender()
        }

        // Label Toggle Button
        btnLabelToggle.setOnClickListener {
            val isVisible = !labelOverlayView.isLabelsVisible
            labelOverlayView.isLabelsVisible = isVisible
            updateLabelButtonVisuals(isVisible)
            model3DRenderer?.requestRender()
        }

        // Close Button
        btnClose.setOnClickListener {
            destroy()
            onCloseListener?.invoke(this)
        }
    }

    private fun updateModeVisuals() {
        if (interactionMode == InteractionMode.NORMAL) {
            btnModeToggle.text = context.getString(R.string.mode_normal)
            btnModeToggle.setIconResource(R.drawable.ic_mode_normal)
            btnModeToggle.setBackgroundColor(0xFF2196F3.toInt())
            cardView.strokeColor = 0x662196F3.toInt()
        } else {
            btnModeToggle.text = context.getString(R.string.mode_interact)
            btnModeToggle.setIconResource(R.drawable.ic_mode_interact)
            btnModeToggle.setBackgroundColor(0xFFFF9800.toInt())
            cardView.strokeColor = 0xFFFF9800.toInt()
        }
    }

    private fun updateLabelButtonVisuals(isVisible: Boolean) {
        if (isVisible) {
            btnLabelToggle.setBackgroundColor(0xFFE91E63.toInt())
            btnLabelToggle.iconTint = ContextCompat.getColorStateList(context, android.R.color.white)
        } else {
            btnLabelToggle.setBackgroundColor(0xFF37474F.toInt())
            btnLabelToggle.iconTint = ContextCompat.getColorStateList(context, android.R.color.darker_gray)
        }
    }


    fun loadModel(glbAssetPath: String, title: String) {
        this.modelFileName = glbAssetPath
        this.tvModelTitle.text = title

        model3DRenderer = Model3DRenderer(context, textureView, labelOverlayView).apply {
            loadGlbFromAssets(glbAssetPath)
        }
    }


    fun clampToBounds() {
        val parentView = parent as? View ?: return
        val parentW = parentView.width
        val parentH = parentView.height
        if (parentW <= 0 || parentH <= 0) return

        val currentW = width.takeIf { it > 0 } ?: layoutParams?.width ?: 0
        val currentH = height.takeIf { it > 0 } ?: layoutParams?.height ?: 0

        val maxTransX = (parentW - currentW).coerceAtLeast(0).toFloat()
        val maxTransY = (parentH - currentH).coerceAtLeast(0).toFloat()

        translationX = translationX.coerceIn(0f, maxTransX)
        translationY = translationY.coerceIn(0f, maxTransY)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post { clampToBounds() }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        clampToBounds()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        clampToBounds()
    }

    companion object {
        private var topZ = 1f
    }

    private fun bringContainerToFront() {
        topZ += 1f
        translationZ = topZ
        bringToFront()
    }

    // --- Touch Dispatch & Normal Mode Gestures ---

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Bring touched container to the front of all containers and request disallow intercept
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            bringContainerToFront()
            parent?.requestDisallowInterceptTouchEvent(true)

            downRawX = ev.rawX
            downRawY = ev.rawY
            lastRawX = ev.rawX
            lastRawY = ev.rawY
            lastTouchX = ev.x
            lastTouchY = ev.y
            activePointerId = ev.getPointerId(0)
            isDragging = false

            // Do not intercept on ACTION_DOWN if touch is in header bar so buttons receive clicks
            val headerH = headerBar.height.takeIf { it > 0 } ?: (36 * density).toInt()
            if (ev.y <= headerH) {
                return false
            }
        }

        if (interactionMode == InteractionMode.NORMAL) {
            // Intercept multi-touch pinches immediately
            if (ev.pointerCount >= 2) {
                return true
            }

            // Intercept single-touch drags if movement exceeds touch slop
            if (ev.actionMasked == MotionEvent.ACTION_MOVE) {
                val dx = ev.rawX - downRawX
                val dy = ev.rawY - downRawY
                if (hypot(dx, dy) > touchSlop) {
                    isDragging = true
                    return true
                }
            }
        } else {
            // In INTERACTION mode, intercept touches in the viewport so 3D orbit and zoom are immediately responsive
            val headerH = headerBar.height.takeIf { it > 0 } ?: (36 * density).toInt()
            if (ev.y > headerH) {
                return true
            }
        }

        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Bring to front and disallow parent interception
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            bringContainerToFront()
        }
        parent?.requestDisallowInterceptTouchEvent(true)

        // Pass event to scaleGestureDetector (handles container resize in Normal mode, 3D zoom in Interaction mode)
        scaleGestureDetector.onTouchEvent(event)

        when (interactionMode) {
            InteractionMode.NORMAL -> {
                if (scaleGestureDetector.isInProgress) {
                    return true
                }

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        activePointerId = event.getPointerId(0)
                        lastRawX = event.rawX
                        lastRawY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        // Re-anchor baseline when a 2nd finger is placed
                        val actionIndex = event.actionIndex
                        activePointerId = event.getPointerId(actionIndex)
                        val rawOffsetX = event.rawX - event.x
                        val rawOffsetY = event.rawY - event.y
                        lastRawX = event.getX(actionIndex) + rawOffsetX
                        lastRawY = event.getY(actionIndex) + rawOffsetY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (event.pointerCount == 1 || !scaleGestureDetector.isInProgress) {
                            val pointerIndex = event.findPointerIndex(activePointerId)
                            if (pointerIndex != -1) {
                                val rawOffsetX = event.rawX - event.x
                                val rawOffsetY = event.rawY - event.y
                                val currentRawX = event.getX(pointerIndex) + rawOffsetX
                                val currentRawY = event.getY(pointerIndex) + rawOffsetY

                                val dx = currentRawX - lastRawX
                                val dy = currentRawY - lastRawY

                                val parentView = parent as? View
                                val parentW = parentView?.width ?: resources.displayMetrics.widthPixels
                                val parentH = parentView?.height ?: resources.displayMetrics.heightPixels

                                val currentW = width.takeIf { it > 0 } ?: layoutParams?.width ?: 0
                                val currentH = height.takeIf { it > 0 } ?: layoutParams?.height ?: 0

                                val maxTransX = (parentW - currentW).coerceAtLeast(0).toFloat()
                                val maxTransY = (parentH - currentH).coerceAtLeast(0).toFloat()

                                translationX = (translationX + dx).coerceIn(0f, maxTransX)
                                translationY = (translationY + dy).coerceIn(0f, maxTransY)

                                lastRawX = currentRawX
                                lastRawY = currentRawY
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_POINTER_UP -> {
                        // Re-anchor baseline when one finger is lifted
                        val actionIndex = event.actionIndex
                        val pointerId = event.getPointerId(actionIndex)
                        val newIndex = if (actionIndex == 0) 1 else 0
                        if (pointerId == activePointerId) {
                            activePointerId = event.getPointerId(newIndex)
                        }
                        val remainingPointerIndex = event.findPointerIndex(activePointerId).takeIf { it != -1 } ?: newIndex
                        val rawOffsetX = event.rawX - event.x
                        val rawOffsetY = event.rawY - event.y
                        lastRawX = event.getX(remainingPointerIndex) + rawOffsetX
                        lastRawY = event.getY(remainingPointerIndex) + rawOffsetY
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                        isDragging = false
                        return true
                    }
                }
            }

            InteractionMode.INTERACTION -> {
                if (scaleGestureDetector.isInProgress) {
                    return true
                }

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        activePointerId = event.getPointerId(0)
                        lastTouchX = event.getX(0)
                        lastTouchY = event.getY(0)
                        return true
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        // Re-anchor baseline when a 2nd finger is placed
                        val actionIndex = event.actionIndex
                        activePointerId = event.getPointerId(actionIndex)
                        lastTouchX = event.getX(actionIndex)
                        lastTouchY = event.getY(actionIndex)
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (event.pointerCount == 1 && !scaleGestureDetector.isInProgress) {
                            val pointerIndex = event.findPointerIndex(activePointerId)
                            if (pointerIndex != -1) {
                                val x = event.getX(pointerIndex)
                                val y = event.getY(pointerIndex)
                                val dx = x - lastTouchX
                                val dy = y - lastTouchY

                                model3DRenderer?.rotateBy(dx, dy)

                                lastTouchX = x
                                lastTouchY = y
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_POINTER_UP -> {
                        // Re-anchor baseline when one finger is lifted
                        val actionIndex = event.actionIndex
                        val pointerId = event.getPointerId(actionIndex)
                        val newIndex = if (actionIndex == 0) 1 else 0
                        if (pointerId == activePointerId) {
                            activePointerId = event.getPointerId(newIndex)
                        }
                        val remainingPointerIndex = event.findPointerIndex(activePointerId).takeIf { it != -1 } ?: newIndex
                        lastTouchX = event.getX(remainingPointerIndex)
                        lastTouchY = event.getY(remainingPointerIndex)
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                        return true
                    }
                }
            }
        }

        return true
    }

    // --- Lifecycle Management ---

    fun onResume() {
        model3DRenderer?.resumeRendering()
    }

    fun onPause() {
        model3DRenderer?.pauseRendering()
    }

    fun destroy() {
        model3DRenderer?.destroy()
        model3DRenderer = null
    }
}
