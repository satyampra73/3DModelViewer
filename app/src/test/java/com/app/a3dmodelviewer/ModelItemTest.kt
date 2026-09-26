package com.app.a3dmodelviewer

import com.app.a3dmodelviewer.ui.dialog.ModelItem
import com.app.a3dmodelviewer.ui.dialog.ModelPickerDialog
import com.app.a3dmodelviewer.ui.dialog.ModelSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class ModelItemTest {

    @Test
    fun testBundledModelsHaveAssetSources() {
        assertEquals(5, ModelPickerDialog.BUNDLED_MODELS.size)
        for (item in ModelPickerDialog.BUNDLED_MODELS) {
            assertTrue("Bundled model source must be Asset", item.source is ModelSource.Asset)
            val assetSource = item.source as ModelSource.Asset
            assertEquals(item.fileName, assetSource.assetPath)
            assertTrue("Display name must not be empty", item.displayName.isNotEmpty())
            assertTrue("Description must not be empty", item.description.isNotEmpty())
        }
    }

    @Test
    fun testModelItemDefaultSourceIsAsset() {
        val item = ModelItem(
            fileName = "Robot.glb",
            displayName = "Robot",
            description = "A 3D Robot"
        )
        assertTrue(item.source is ModelSource.Asset)
        assertEquals("Robot.glb", (item.source as ModelSource.Asset).assetPath)
        assertEquals("Robot", item.displayName)
    }

    @Test
    fun testGlbHeaderValidation() {
        // Valid GLB header: 'g', 'l', 'T', 'F'
        val validGlbHeader = byteArrayOf('g'.code.toByte(), 'l'.code.toByte(), 'T'.code.toByte(), 'F'.code.toByte(), 0x02, 0, 0, 0)
        val validStream = ByteArrayInputStream(validGlbHeader)

        val header = ByteArray(4)
        val read = validStream.read(header)
        val isValid = read == 4 &&
                header[0] == 'g'.code.toByte() &&
                header[1] == 'l'.code.toByte() &&
                header[2] == 'T'.code.toByte() &&
                header[3] == 'F'.code.toByte()
        assertTrue("Valid GLB header must pass", isValid)

        // Invalid header: text or non-glb file
        val invalidHeader = "GIF89a".toByteArray(Charsets.US_ASCII)
        val invalidStream = ByteArrayInputStream(invalidHeader)
        val invHeader = ByteArray(4)
        val invRead = invalidStream.read(invHeader)
        val isInvValid = invRead == 4 &&
                invHeader[0] == 'g'.code.toByte() &&
                invHeader[1] == 'l'.code.toByte() &&
                invHeader[2] == 'T'.code.toByte() &&
                invHeader[3] == 'F'.code.toByte()
        assertFalse("Invalid header must be rejected", isInvValid)

        // Truncated header (< 4 bytes)
        val shortHeader = byteArrayOf('g'.code.toByte(), 'l'.code.toByte())
        val shortStream = ByteArrayInputStream(shortHeader)
        val shortBuf = ByteArray(4)
        val shortRead = shortStream.read(shortBuf)
        val isShortValid = shortRead == 4 &&
                shortBuf[0] == 'g'.code.toByte() &&
                shortBuf[1] == 'l'.code.toByte() &&
                shortBuf[2] == 'T'.code.toByte() &&
                shortBuf[3] == 'F'.code.toByte()
        assertFalse("Short stream must be rejected", isShortValid)
    }
}
