package com.app.a3dmodelviewer

import com.app.a3dmodelviewer.ui.container.InteractionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class InteractionModeTest {

    @Test
    fun testInteractionModeValues() {
        val modes = InteractionMode.values()
        assertEquals(2, modes.size)
        assertEquals(InteractionMode.NORMAL, modes[0])
        assertEquals(InteractionMode.INTERACTION, modes[1])
    }

    @Test
    fun testModeToggleTransition() {
        var currentMode = InteractionMode.NORMAL

        // Toggle 1: Normal -> Interaction
        currentMode = if (currentMode == InteractionMode.NORMAL) {
            InteractionMode.INTERACTION
        } else {
            InteractionMode.NORMAL
        }
        assertEquals(InteractionMode.INTERACTION, currentMode)

        // Toggle 2: Interaction -> Normal
        currentMode = if (currentMode == InteractionMode.NORMAL) {
            InteractionMode.INTERACTION
        } else {
            InteractionMode.NORMAL
        }
        assertEquals(InteractionMode.NORMAL, currentMode)
    }

    @Test
    fun testModeDistinctness() {
        assertNotEquals(InteractionMode.NORMAL, InteractionMode.INTERACTION)
    }
}
