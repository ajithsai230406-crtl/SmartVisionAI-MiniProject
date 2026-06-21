package com.smartvision.ai

import android.content.Context
import com.smartvision.ai.data.db.GeminiCacheDao
import com.smartvision.ai.data.repository.OpenRouterManager
import com.smartvision.ai.detector.data.ObjectDetectionRepository
import com.smartvision.ai.waste.data.WasteClassifierRepository
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.every
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RepositoryTests {

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun testWasteClassifierCocoLabelMappings() {
        val context = mockk<Context>(relaxed = true)
        val cacheDao = mockk<GeminiCacheDao>(relaxed = true)
        val openRouterManager = mockk<OpenRouterManager>(relaxed = true)
        val repo = WasteClassifierRepository(context, cacheDao, openRouterManager)

        val mapCocoLabelMethod = WasteClassifierRepository::class.java.getDeclaredMethod(
            "mapCocoLabelToWasteLabel",
            String::class.java
        )
        mapCocoLabelMethod.isAccessible = true

        assertEquals("electronic_waste", mapCocoLabelMethod.invoke(repo, "cell phone"))
        assertEquals("electronic_waste", mapCocoLabelMethod.invoke(repo, "keyboard"))
        assertEquals("electronic_waste", mapCocoLabelMethod.invoke(repo, "laptop"))
        assertEquals("plastic_bottle", mapCocoLabelMethod.invoke(repo, "bottle"))
        assertEquals("glass", mapCocoLabelMethod.invoke(repo, "wine glass"))

        val cocoLabelToExactItemNameMethod = WasteClassifierRepository::class.java.getDeclaredMethod(
            "cocoLabelToExactItemName",
            String::class.java
        )
        cocoLabelToExactItemNameMethod.isAccessible = true

        assertEquals("Smartphone", cocoLabelToExactItemNameMethod.invoke(repo, "cell phone"))
        assertEquals("Laptop Computer", cocoLabelToExactItemNameMethod.invoke(repo, "laptop"))
        assertEquals("Computer Keyboard", cocoLabelToExactItemNameMethod.invoke(repo, "keyboard"))
        assertEquals("Plastic Bottle", cocoLabelToExactItemNameMethod.invoke(repo, "bottle"))
    }

    @Test
    fun testObjectDetectorSanitizationAndCleaning() {
        val context = mockk<Context>(relaxed = true)
        val repo = ObjectDetectionRepository(context)

        val cleanLabelMethod = ObjectDetectionRepository::class.java.getDeclaredMethod(
            "cleanLabel",
            String::class.java
        )
        cleanLabelMethod.isAccessible = true

        assertEquals("Phone", cleanLabelMethod.invoke(repo, "cell phone"))
        assertEquals("Monitor", cleanLabelMethod.invoke(repo, "tv"))
        assertEquals("Bottle", cleanLabelMethod.invoke(repo, "bottle"))

        val sanitizeLabelMethod = ObjectDetectionRepository::class.java.getDeclaredMethod(
            "sanitizeLabel",
            String::class.java,
            Float::class.java
        )
        sanitizeLabelMethod.isAccessible = true

        // For large sports items, it should sanitize to bottle
        assertEquals("bottle", sanitizeLabelMethod.invoke(repo, "tennis racket", 0.45f))
        assertEquals("bottle", sanitizeLabelMethod.invoke(repo, "surfboard", 0.35f))
        
        // For small sports items, it should keep the raw label
        assertEquals("tennis racket", sanitizeLabelMethod.invoke(repo, "tennis racket", 0.15f))
        
        // For non-sports items, it should keep the raw label regardless of size
        assertEquals("cell phone", sanitizeLabelMethod.invoke(repo, "cell phone", 0.80f))
    }
}
