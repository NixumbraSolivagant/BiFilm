package com.bifilm.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlendModeTest {

    @Test
    fun `BlendMode enum has 6 entries`() {
        assertEquals(6, BlendMode.entries.size)
    }

    @Test
    fun `BlendMode uniform values are 0 to 5`() {
        val values = BlendMode.entries.map { it.uniformValue }.sorted()
        assertEquals(listOf(0, 1, 2, 3, 4, 5), values)
    }

    @Test
    fun `BlendMode fromName parses valid names`() {
        assertEquals(BlendMode.SCREEN, BlendMode.fromName("SCREEN"))
        assertEquals(BlendMode.ADDITIVE, BlendMode.fromName("ADDITIVE"))
    }

    @Test
    fun `BlendMode fromName falls back to SCREEN`() {
        assertEquals(BlendMode.SCREEN, BlendMode.fromName("UNKNOWN"))
    }

    @Test
    fun `BlendMode display names are non-empty Chinese strings`() {
        BlendMode.entries.forEach { mode ->
            assertTrue(
                "BlendMode ${mode.name} should have Chinese displayName",
                mode.displayName.any { c -> c.code >= 0x4E00 && c.code <= 0x9FFF }
            )
        }
    }
}