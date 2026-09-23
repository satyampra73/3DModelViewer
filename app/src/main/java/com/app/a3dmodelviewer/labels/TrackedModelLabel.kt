package com.app.a3dmodelviewer.labels

class TrackedModelLabel(
    val entity: Int,
    val nodeName: String,
    val labelText: String,
    val localTranslation: FloatArray? = null
) {
    // Current projected 2D screen coordinates
    var screenX: Float = 0f
    var screenY: Float = 0f
    var isVisible: Boolean = false

    // Pre-allocated scratch buffers to eliminate per-frame GC allocations
    val worldMatrix = FloatArray(16)
    val worldPosition = FloatArray(3)
    val screenPosBuffer = FloatArray(2)

    fun updateWorldPosition(transformManager: com.google.android.filament.TransformManager) {
        val instance = transformManager.getInstance(entity)
        if (instance != 0) {
            transformManager.getWorldTransform(instance, worldMatrix)
            if (localTranslation != null) {
                // If localTranslation is provided, transform the local offset through the parent world matrix
                val lx = localTranslation[0]
                val ly = localTranslation[1]
                val lz = localTranslation[2]
                worldPosition[0] = worldMatrix[0] * lx + worldMatrix[4] * ly + worldMatrix[8] * lz + worldMatrix[12]
                worldPosition[1] = worldMatrix[1] * lx + worldMatrix[5] * ly + worldMatrix[9] * lz + worldMatrix[13]
                worldPosition[2] = worldMatrix[2] * lx + worldMatrix[6] * ly + worldMatrix[10] * lz + worldMatrix[14]
            } else {
                // Otherwise use the origin of the node in world space
                worldPosition[0] = worldMatrix[12]
                worldPosition[1] = worldMatrix[13]
                worldPosition[2] = worldMatrix[14]
            }
        }
    }
}
