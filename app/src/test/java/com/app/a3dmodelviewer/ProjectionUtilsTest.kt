package com.app.a3dmodelviewer

import com.app.a3dmodelviewer.labels.ProjectionUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectionUtilsTest {

    private fun identityFloatMatrix(): FloatArray {
        return FloatArray(16).apply {
            this[0] = 1f; this[5] = 1f; this[10] = 1f; this[15] = 1f
        }
    }

    private fun identityDoubleMatrix(): DoubleArray {
        return DoubleArray(16).apply {
            this[0] = 1.0; this[5] = 1.0; this[10] = 1.0; this[15] = 1.0
        }
    }

    @Test
    fun testProjectCenterPoint() {
        val viewMatrix = identityFloatMatrix()
        val projMatrix = identityDoubleMatrix()
        val screenPos = FloatArray(2)

        val result = ProjectionUtils.project(
            worldX = 0f,
            worldY = 0f,
            worldZ = 0f,
            viewMatrix = viewMatrix,
            projMatrix = projMatrix,
            viewportWidth = 800,
            viewportHeight = 600,
            outScreenPos = screenPos
        )

        assertTrue("Center point should be visible in frustum", result)
        assertEquals(400f, screenPos[0], 0.001f)
        assertEquals(300f, screenPos[1], 0.001f)
    }

    @Test
    fun testBehindCameraCulling() {
        val viewMatrix = identityFloatMatrix()
        val projMatrix = DoubleArray(16).apply {
            this[0] = 1.0; this[5] = 1.0; this[10] = -1.0; this[11] = -1.0
        }
        val screenPos = FloatArray(2)

        val result = ProjectionUtils.project(
            worldX = 0f,
            worldY = 0f,
            worldZ = 5f,
            viewMatrix = viewMatrix,
            projMatrix = projMatrix,
            viewportWidth = 800,
            viewportHeight = 600,
            outScreenPos = screenPos
        )

        assertFalse("Point behind camera eye (cw <= 0) must be culled", result)
    }

    @Test
    fun testDepthFrustumCulling() {
        val viewMatrix = identityFloatMatrix()
        val projMatrix = identityDoubleMatrix()
        val screenPos = FloatArray(2)

        val result = ProjectionUtils.project(
            worldX = 0f,
            worldY = 0f,
            worldZ = 2.0f,
            viewMatrix = viewMatrix,
            projMatrix = projMatrix,
            viewportWidth = 800,
            viewportHeight = 600,
            outScreenPos = screenPos
        )

        assertFalse("Point beyond far clipping range must be culled", result)
    }

    @Test
    fun testOutOfViewportBounds() {
        val viewMatrix = identityFloatMatrix()
        val projMatrix = identityDoubleMatrix()
        val screenPos = FloatArray(2)

        val result = ProjectionUtils.project(
            worldX = 2.0f,
            worldY = 0f,
            worldZ = 0f,
            viewMatrix = viewMatrix,
            projMatrix = projMatrix,
            viewportWidth = 800,
            viewportHeight = 600,
            outScreenPos = screenPos
        )

        assertFalse("Point outside viewport margin must be culled", result)
    }

    @Test
    fun testInvalidViewportDimensions() {
        val viewMatrix = identityFloatMatrix()
        val projMatrix = identityDoubleMatrix()
        val screenPos = FloatArray(2)

        val resultZeroW = ProjectionUtils.project(0f, 0f, 0f, viewMatrix, projMatrix, 0, 600, screenPos)
        assertFalse("Zero width viewport must return false", resultZeroW)

        val resultZeroH = ProjectionUtils.project(0f, 0f, 0f, viewMatrix, projMatrix, 800, 0, screenPos)
        assertFalse("Zero height viewport must return false", resultZeroH)
    }

    @Test
    fun testScreenCoordinateConversion() {
        val viewMatrix = identityFloatMatrix()
        val projMatrix = identityDoubleMatrix()
        val screenPos = FloatArray(2)

        val result = ProjectionUtils.project(
            worldX = 1.0f,
            worldY = 1.0f,
            worldZ = 0f,
            viewMatrix = viewMatrix,
            projMatrix = projMatrix,
            viewportWidth = 1000,
            viewportHeight = 500,
            outScreenPos = screenPos
        )

        assertTrue("Point on NDC boundary should be visible", result)
        assertEquals(1000f, screenPos[0], 0.001f)
        assertEquals(0f, screenPos[1], 0.001f)
    }
}
