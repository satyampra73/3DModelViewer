package com.app.a3dmodelviewer

import com.app.a3dmodelviewer.glb.GlbMetadataParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GlbMetadataParserTest {

    private val assetsDir = File("src/main/assets")

    @Test
    fun testParseBulb() {
        val file = File(assetsDir, "Bulb.glb")
        assertTrue("Bulb.glb must exist", file.exists())

        val labels = GlbMetadataParser.parse(file.inputStream())
        assertEquals(6, labels.size)

        val map = labels.associate { it.nodeName to it.labelText }
        assertEquals("Filament", map["Empty.004"])
        assertEquals("Glass Bulb", map["Empty.001"])
        assertEquals("Support Wires", map["Empty.002"])
        assertEquals("Glass Mount", map["Empty.003"])
        assertEquals("Metal Base", map["Empty.005"])
        assertEquals("Insulator", map["Empty.006"])
    }

    @Test
    fun testParseFiagena() {
        val file = File(assetsDir, "Fiagena.glb")
        assertTrue("Fiagena.glb must exist", file.exists())

        val labels = GlbMetadataParser.parse(file.inputStream())
        assertEquals(7, labels.size)

        val map = labels.associate { it.nodeName to it.labelText }
        assertEquals("Hook", map["Empty.004"])
        assertEquals("Hook Filament junction", map["Empty.001"])
        assertEquals("L ring", map["Empty.002"])
        assertEquals("P ring", map["Empty.003"])
        assertEquals("MS ring", map["Empty.005"])
        assertEquals("filament", map["Empty.006"])
        assertEquals("Helical structure", map["Empty.007"])
    }

    @Test
    fun testParseLungs() {
        val file = File(assetsDir, "Lungs.glb")
        assertTrue("Lungs.glb must exist", file.exists())

        val labels = GlbMetadataParser.parse(file.inputStream())
        assertEquals(5, labels.size)

        val map = labels.associate { it.nodeName to it.labelText }
        assertEquals("Larynx", map["Empty.003"])
        assertEquals("Trachea", map["Empty.001"])
        assertEquals("Main bronchus", map["Empty.002"])
        assertEquals("Bronchial tree", map["Empty.004"])
        assertEquals("Lung", map["Empty.005"])
    }

    @Test
    fun testParseMicroscope() {
        val file = File(assetsDir, "Microscope.glb")
        assertTrue("Microscope.glb must exist", file.exists())

        val labels = GlbMetadataParser.parse(file.inputStream())
        assertEquals(12, labels.size)

        val map = labels.associate { it.nodeName to it.labelText }
        assertEquals("Eyepiece", map["Empty.004"])
        assertEquals("Body Tube", map["Empty.001"])
        assertEquals("Revolving Nosepiece", map["Empty.002"])
        assertEquals("Objective Lenses", map["Empty.003"])
        assertEquals("Stage", map["Empty.005"])
        assertEquals("Diaphragm", map["Empty.006"])
        assertEquals("Illuminator", map["Empty.007"])
        assertEquals("Base", map["Empty.008"])
        assertEquals("Stage Clip", map["Empty.009"])
        assertEquals("Arm", map["Empty.010"])
        assertEquals("Fine Adjustment Knob", map["Empty.011"])
        assertEquals("Coarse Adjustment Knob", map["Empty.012"])
    }

    @Test
    fun testParseSolarSystem() {
        val file = File(assetsDir, "solarsystem.glb")
        assertTrue("solarsystem.glb must exist", file.exists())

        val labels = GlbMetadataParser.parse(file.inputStream())
        assertEquals(9, labels.size)

        val map = labels.associate { it.nodeName to it.labelText }
        assertEquals("Mercury", map["Empty.007"])
        assertEquals("venus", map["Empty.008"])
        assertEquals("Earth", map["Empty.009"])
        assertEquals("mars", map["Empty.003"])
        assertEquals("Jupiter", map["Empty.002"])
        assertEquals("Saturn", map["Empty.005"])
        assertEquals("Uranus", map["Empty.006"])
        assertEquals("Neptune", map["Empty.001"])
        assertEquals("Bronchial tree", map["Empty.004"])
    }
}
