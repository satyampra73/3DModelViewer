package com.app.a3dmodelviewer.engine

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.Choreographer
import android.view.Surface
import android.view.TextureView
import com.app.a3dmodelviewer.glb.GlbMetadataParser
import com.app.a3dmodelviewer.labels.LabelOverlayView
import com.app.a3dmodelviewer.labels.ProjectionUtils
import com.app.a3dmodelviewer.labels.TrackedModelLabel
import com.google.android.filament.Camera
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.TransformManager
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class Model3DRenderer(
    private val context: Context,
    private val textureView: TextureView,
    private var labelOverlayView: LabelOverlayView? = null
) : TextureView.SurfaceTextureListener {

    private val engine = FilamentManager.engine
    private val entityManager = EntityManager.get()
    private val transformManager: TransformManager = engine.transformManager

    private val renderer: Renderer = engine.createRenderer()
    private val scene: Scene = engine.createScene()
    private val view: View = engine.createView()

    private val cameraEntity: Int = entityManager.create()
    private val camera: Camera = engine.createCamera(cameraEntity)

    private var surface: Surface? = null
    private var swapChain: SwapChain? = null
    private var assetLoader: AssetLoader? = null
    private var resourceLoader: ResourceLoader? = null
    private var filamentAsset: FilamentAsset? = null

    private val lightEntities = mutableListOf<Int>()
    private val trackedLabels = mutableListOf<TrackedModelLabel>()

    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var isRendering = false
    private var isDestroyed = false

    // Model bounding box center and extent for camera framing
    private var modelCenter = FloatArray(3) { 0f }
    private var modelRadius = 1.0f

    // Spherical Orbit Camera State (independent per renderer instance)
    var azimuth: Float = 0f
        private set
    var elevation: Float = 0.15f
        private set
    var cameraDistance: Float = 1.0f
        private set
    private var minDistance: Float = 0.5f
    private var maxDistance: Float = 8.0f

    // Dirty-flag rendering state (demand-driven rendering for idle efficiency)
    private var dirtyFrames = 0
    private val DEFAULT_SETTLE_FRAMES = 3

    // Pre-allocated matrix buffers for zero-allocation per-frame projection
    private val viewMatrix = FloatArray(16)
    private val projMatrix = DoubleArray(16)

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (isDestroyed) {
                isRendering = false
                return
            }

            val currentSwapChain = swapChain
            if (currentSwapChain == null) {
                isRendering = false
                return
            }

            if (dirtyFrames > 0) {
                if (renderer.beginFrame(currentSwapChain, frameTimeNanos)) {
                    renderer.render(view)
                    renderer.endFrame()
                }

                // Update 3D-to-2D projected label positions
                updateLabelsProjection()

                dirtyFrames--
            }

            if (dirtyFrames > 0) {
                Choreographer.getInstance().postFrameCallback(this)
            } else {
                isRendering = false
            }
        }
    }

    init {
        textureView.isOpaque = false

        view.camera = camera
        view.scene = scene
        view.blendMode = View.BlendMode.TRANSLUCENT
        view.isPostProcessingEnabled = false

        renderer.clearOptions = Renderer.ClearOptions().apply {
            clearColor = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
            clear = true
            discard = true
        }

        assetLoader = AssetLoader(engine, FilamentManager.materialProvider, entityManager)
        resourceLoader = ResourceLoader(engine)

        setupLighting()

        textureView.surfaceTextureListener = this
        if (textureView.isAvailable) {
            val surfaceTexture = textureView.surfaceTexture
            if (surfaceTexture != null) {
                onSurfaceTextureAvailable(surfaceTexture, textureView.width, textureView.height)
            }
        }
    }

    fun setLabelOverlayView(overlay: LabelOverlayView) {
        this.labelOverlayView = overlay
    }

    private fun setupLighting() {
        // Key Light: Direct illumination
        val keyLight = entityManager.create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.98f, 0.95f)
            .intensity(80_000.0f)
            .direction(0.4f, -0.8f, -0.5f)
            .build(engine, keyLight)
        scene.addEntity(keyLight)
        lightEntities.add(keyLight)

        // Fill Light: Soft ambient fill
        val fillLight = entityManager.create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(0.9f, 0.92f, 1.0f)
            .intensity(35_000.0f)
            .direction(-0.5f, -0.3f, 0.5f)
            .build(engine, fillLight)
        scene.addEntity(fillLight)
        lightEntities.add(fillLight)

        // Rim/Back Light: Edge definition
        val rimLight = entityManager.create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 1.0f, 1.0f)
            .intensity(25_000.0f)
            .direction(0.0f, 0.8f, 0.8f)
            .build(engine, rimLight)
        scene.addEntity(rimLight)
        lightEntities.add(rimLight)
    }

    fun loadGlbFromAssets(assetPath: String) {
        if (isDestroyed) return

        val loader = assetLoader ?: return
        val resLoader = resourceLoader ?: return

        // 1. Read GLB bytes from assets
        val bytes = context.assets.open(assetPath).use { it.readBytes() }
        val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
            order(ByteOrder.nativeOrder())
            put(bytes)
            flip()
        }

        // 2. Parse glTF metadata for extras.prop labels
        val metadataList = GlbMetadataParser.parse(bytes)

        // 3. Create asset and load GPU resources
        val asset = loader.createAsset(buffer) ?: return
        resLoader.loadResources(asset)
        asset.releaseSourceData()

        filamentAsset = asset
        scene.addEntities(asset.renderableEntities)
        if (asset.lightEntities.isNotEmpty()) {
            scene.addEntities(asset.lightEntities)
        }

        // 4. Map parsed metadata to Filament entities
        trackedLabels.clear()
        for (meta in metadataList) {
            val entity = asset.getFirstEntityByName(meta.nodeName)
            if (entity != 0) {
                trackedLabels.add(
                    TrackedModelLabel(
                        entity = entity,
                        nodeName = meta.nodeName,
                        labelText = meta.labelText,
                        localTranslation = meta.localTranslation
                    )
                )
            }
        }

        // 5. Calculate bounding box and center camera
        val aabb = asset.boundingBox
        val center = aabb.center
        val halfExtent = aabb.halfExtent

        modelCenter[0] = center[0]
        modelCenter[1] = center[1]
        modelCenter[2] = center[2]

        modelRadius = max(halfExtent[0], max(halfExtent[1], halfExtent[2]))
        if (modelRadius <= 0f) modelRadius = 1.0f

        cameraDistance = modelRadius * 2.6f
        minDistance = modelRadius * 0.6f
        maxDistance = modelRadius * 8.0f
        azimuth = 0f
        elevation = 0.15f

        updateCameraFraming()
        requestRender(DEFAULT_SETTLE_FRAMES)
    }


    fun rotateBy(deltaX: Float, deltaY: Float) {
        val sensitivity = 0.005f
        azimuth -= deltaX * sensitivity
        val twoPi = (Math.PI * 2).toFloat()
        if (azimuth > twoPi) azimuth -= twoPi
        if (azimuth < -twoPi) azimuth += twoPi

        elevation = (elevation + deltaY * sensitivity).coerceIn(-1.48f, 1.48f)
        updateCameraFraming()
        requestRender(DEFAULT_SETTLE_FRAMES)
    }

    fun zoomBy(scaleFactor: Float) {
        if (scaleFactor <= 0f) return
        cameraDistance = (cameraDistance / scaleFactor).coerceIn(minDistance, maxDistance)
        updateCameraFraming()
        requestRender(DEFAULT_SETTLE_FRAMES)
    }

    private fun updateCameraFraming() {
        if (surfaceWidth <= 0 || surfaceHeight <= 0) return

        val aspect = surfaceWidth.toDouble() / surfaceHeight.toDouble()
        camera.setProjection(45.0, aspect, 0.01, 1000.0, Camera.Fov.VERTICAL)

        val cosElev = cos(elevation)
        val sinElev = sin(elevation)
        val sinAzim = sin(azimuth)
        val cosAzim = cos(azimuth)

        val eyeX = (modelCenter[0] + cameraDistance * cosElev * sinAzim).toDouble()
        val eyeY = (modelCenter[1] + cameraDistance * sinElev).toDouble()
        val eyeZ = (modelCenter[2] + cameraDistance * cosElev * cosAzim).toDouble()

        camera.lookAt(
            eyeX,
            eyeY,
            eyeZ,
            modelCenter[0].toDouble(),
            modelCenter[1].toDouble(),
            modelCenter[2].toDouble(),
            0.0,
            1.0,
            0.0
        )
    }


    private fun updateLabelsProjection() {
        val overlay = labelOverlayView ?: return
        if (!overlay.isLabelsVisible || trackedLabels.isEmpty() || surfaceWidth <= 0 || surfaceHeight <= 0) return

        camera.getViewMatrix(viewMatrix)
        camera.getProjectionMatrix(projMatrix)

        for (label in trackedLabels) {
            label.updateWorldPosition(transformManager)
            val inFrustum = ProjectionUtils.project(
                worldX = label.worldPosition[0],
                worldY = label.worldPosition[1],
                worldZ = label.worldPosition[2],
                viewMatrix = viewMatrix,
                projMatrix = projMatrix,
                viewportWidth = surfaceWidth,
                viewportHeight = surfaceHeight,
                outScreenPos = label.screenPosBuffer
            )
            label.isVisible = inFrustum
            if (inFrustum) {
                label.screenX = label.screenPosBuffer[0]
                label.screenY = label.screenPosBuffer[1]
            }
        }

        overlay.updateLabels(trackedLabels)
    }

    // --- TextureView.SurfaceTextureListener ---

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        if (isDestroyed) return

        surfaceWidth = width
        surfaceHeight = height

        val newSurface = Surface(surfaceTexture)
        surface = newSurface
        swapChain = engine.createSwapChain(newSurface, 1L)

        view.viewport = Viewport(0, 0, width, height)
        updateCameraFraming()

        requestRender(DEFAULT_SETTLE_FRAMES)
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        if (isDestroyed) return

        surfaceWidth = width
        surfaceHeight = height

        view.viewport = Viewport(0, 0, width, height)
        updateCameraFraming()
        requestRender(DEFAULT_SETTLE_FRAMES)
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        pauseRendering()
        swapChain?.let {
            engine.destroySwapChain(it)
            swapChain = null
        }
        surface?.release()
        surface = null
        return true
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        // No-op
    }

    fun requestRender(frames: Int = DEFAULT_SETTLE_FRAMES) {
        if (isDestroyed || swapChain == null) return
        dirtyFrames = maxOf(dirtyFrames, frames)
        if (!isRendering) {
            isRendering = true
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    fun resumeRendering() {
        requestRender(DEFAULT_SETTLE_FRAMES)
    }

    fun pauseRendering() {
        isRendering = false
        dirtyFrames = 0
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    // --- Lifecycle Cleanup ---

    fun destroy() {
        if (isDestroyed) return
        isDestroyed = true

        pauseRendering()
        trackedLabels.clear()
        labelOverlayView = null

        // 1. Destroy loaded GLB asset and loaders
        filamentAsset?.let { asset ->
            scene.removeEntities(asset.entities)
            assetLoader?.destroyAsset(asset)
            filamentAsset = null
        }
        resourceLoader?.destroy()
        resourceLoader = null

        assetLoader?.destroy()
        assetLoader = null

        // 2. Destroy lights
        for (light in lightEntities) {
            scene.removeEntity(light)
            engine.destroyEntity(light)
            entityManager.destroy(light)
        }
        lightEntities.clear()

        // 3. Destroy camera
        engine.destroyCameraComponent(cameraEntity)
        entityManager.destroy(cameraEntity)

        // 4. Destroy view and scene
        engine.destroyView(view)
        engine.destroyScene(scene)

        // 5. Destroy renderer and swapchain
        swapChain?.let {
            engine.destroySwapChain(it)
            swapChain = null
        }
        surface?.release()
        surface = null

        engine.destroyRenderer(renderer)
    }
}
