package com.bifilm.app.render.engine

import com.bifilm.app.domain.model.BlendMode
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class BlendModeRegistryTest {

    @Test
    fun `all returns 6 entries`() {
        assertEquals(6, BlendModeRegistry.all().size)
    }

    @Test
    fun `parse valid name returns enum`() {
        assertEquals(BlendMode.MULTIPLY, BlendModeRegistry.parse("MULTIPLY"))
    }

    @Test
    fun `parse invalid name falls back to SCREEN`() {
        assertEquals(BlendMode.SCREEN, BlendModeRegistry.parse("???"))
    }
}

class ExposureApplierTest {

    @Test
    fun `apply zero stops is gain 1`() {
        assertEquals(1f, ExposureApplier.apply(0f), 0.001f)
    }

    @Test
    fun `apply minus one stop is gain 0_5`() {
        assertEquals(0.5f, ExposureApplier.apply(-1f), 0.001f)
    }

    @Test
    fun `apply plus one stop is gain 2`() {
        assertEquals(2f, ExposureApplier.apply(1f), 0.001f)
    }

    @Test
    fun `apply plus three stops is gain 8`() {
        assertEquals(8f, ExposureApplier.apply(3f), 0.001f)
    }

    @Test
    fun `suggested additive compensates downward for 4 layers`() {
        val s = ExposureApplier.suggested(layerCount = 4, mode = BlendMode.ADDITIVE)
        // -log2(4) = -2 stops
        assert(abs(s - (-2f)) < 0.001f)
    }

    @Test
    fun `suggested average is zero`() {
        assertEquals(0f, ExposureApplier.suggested(9, BlendMode.AVERAGE), 0.001f)
    }
}