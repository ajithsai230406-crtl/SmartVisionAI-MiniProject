package com.smartvision.ai

import com.smartvision.ai.util.*
import org.junit.Assert.*
import org.junit.Test

class ExtensionsTest {

    @Test
    fun `truncate short string unchanged`() {
        assertEquals("Hello", "Hello".truncate(60))
    }

    @Test
    fun `truncate long string adds ellipsis`() {
        val long   = "a".repeat(100)
        val result = long.truncate(60)
        assertTrue(result.endsWith("…"))
        assertEquals(61, result.length)
    }

    @Test
    fun `toPercent converts float correctly`() {
        assertEquals("75%", 0.75f.toPercent())
        assertEquals("100%", 1.0f.toPercent())
        assertEquals("0%", 0.0f.toPercent())
    }

    @Test
    fun `toRelativeTime just now for very recent`() {
        val now    = System.currentTimeMillis() - 5000
        val result = now.toRelativeTime()
        assertEquals("Just now", result)
    }

    @Test
    fun `toRelativeTime minutes ago`() {
        val fiveMin = System.currentTimeMillis() - 5 * 60_000
        val result  = fiveMin.toRelativeTime()
        assertTrue(result.contains("min ago"))
    }

    @Test
    fun `confidenceColor cyan for high confidence`() {
        val color = confidenceColor(0.9f)
        assertEquals(0xFF00E5FF, (color.value shr 0).toLong() and 0xFFFFFFFF or 0xFF000000)
    }
}
