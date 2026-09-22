package com.app.a3dmodelviewer.labels

/**
 * High-performance, allocation-free utility for 3D world-to-2D screen coordinate projection.
 */
object ProjectionUtils {

    /**
     * Projects a 3D world position [x, y, z] to 2D screen coordinates [screenX, screenY].
     *
     * @param worldX World X coordinate.
     * @param worldY World Y coordinate.
     * @param worldZ World Z coordinate.
     * @param viewMatrix 4x4 column-major view matrix from Filament Camera (float[16]).
     * @param projMatrix 4x4 column-major projection matrix from Filament Camera (double[16]).
     * @param viewportWidth Width of the rendering viewport in pixels.
     * @param viewportHeight Height of the rendering viewport in pixels.
     * @param outScreenPos Reusable 2-element FloatArray to receive [screenX, screenY].
     * @return true if the point is in front of the camera and within visible bounds, false if culled.
     */
    fun project(
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        viewMatrix: FloatArray,
        projMatrix: DoubleArray,
        viewportWidth: Int,
        viewportHeight: Int,
        outScreenPos: FloatArray
    ): Boolean {
        if (viewportWidth <= 0 || viewportHeight <= 0) return false

        // 1. Transform World Position -> View Space (P_view = ViewMatrix * P_world)
        // Matrix is column-major: V[col * 4 + row]
        val vx = viewMatrix[0] * worldX + viewMatrix[4] * worldY + viewMatrix[8] * worldZ + viewMatrix[12]
        val vy = viewMatrix[1] * worldX + viewMatrix[5] * worldY + viewMatrix[9] * worldZ + viewMatrix[13]
        val vz = viewMatrix[2] * worldX + viewMatrix[6] * worldY + viewMatrix[10] * worldZ + viewMatrix[14]
        val vw = viewMatrix[3] * worldX + viewMatrix[7] * worldY + viewMatrix[11] * worldZ + viewMatrix[15]

        // 2. Transform View Space -> Clip Space (P_clip = ProjMatrix * P_view)
        val cx = projMatrix[0] * vx + projMatrix[4] * vy + projMatrix[8] * vz + projMatrix[12] * vw
        val cy = projMatrix[1] * vx + projMatrix[5] * vy + projMatrix[9] * vz + projMatrix[13] * vw
        val cz = projMatrix[2] * vx + projMatrix[6] * vy + projMatrix[10] * vz + projMatrix[14] * vw
        val cw = projMatrix[3] * vx + projMatrix[7] * vy + projMatrix[11] * vz + projMatrix[15] * vw

        // 3. Near plane / Behind Camera culling (cw <= 0 means point is behind the camera eye)
        if (cw <= 0.0001) {
            return false
        }

        // 4. Perspective divide -> Normalized Device Coordinates (NDC)
        val invW = 1.0 / cw
        val ndcX = (cx * invW).toFloat()
        val ndcY = (cy * invW).toFloat()
        val ndcZ = (cz * invW).toFloat()

        // 5. Frustum depth culling (Filament OpenGL NDC depth range is [-1.0, 1.0])
        if (ndcZ < -1.05f || ndcZ > 1.05f) {
            return false
        }

        // 6. Viewport XY bounds check (allow slight 10% margin outside viewport)
        if (ndcX < -1.15f || ndcX > 1.15f || ndcY < -1.15f || ndcY > 1.15f) {
            return false
        }

        // 7. Convert NDC -> Android Screen Coordinates
        // NDC: X in [-1, 1] (left to right), Y in [-1, 1] (bottom to top)
        // Android Screen: (0,0) is top-left, (W,H) is bottom-right -> Invert Y!
        val screenX = (ndcX + 1.0f) * 0.5f * viewportWidth
        val screenY = (1.0f - ndcY) * 0.5f * viewportHeight

        outScreenPos[0] = screenX
        outScreenPos[1] = screenY
        return true
    }
}
