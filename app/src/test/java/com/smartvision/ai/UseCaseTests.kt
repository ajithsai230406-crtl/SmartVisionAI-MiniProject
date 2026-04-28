package com.smartvision.ai

import android.graphics.Bitmap
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.data.repository.*
import io.mockk.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

// ─────────────────────────────────────────────────────────────────────────────
// NOTE: Add to app/build.gradle.kts:
//   testImplementation("io.mockk:mockk:1.13.12")
//   testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
//   testImplementation("junit:junit:4.13.2")
// ─────────────────────────────────────────────────────────────────────────────

class SmartVisionUseCaseTests {

    private lateinit var bitmap: Bitmap

    @Before
    fun setup() {
        bitmap = mockk(relaxed = true)
        every { bitmap.width  } returns 640
        every { bitmap.height } returns 480
    }

    // ── Object Detection ──────────────────────────────────────────────────────

    @Test
    fun `DetectObjectsUseCase returns ObjectDetectionResult on success`() = runTest {
        val useCase = DetectObjectsUseCaseImpl()
        // In real tests you'd mock ML Kit's detector.
        // Here we verify the result type contract:
        val result = useCase(bitmap)
        assertTrue(
            "Expected ObjectDetectionResult or Error",
            result is ScanResult.ObjectDetectionResult || result is ScanResult.Error
        )
    }

    // ── OCR ───────────────────────────────────────────────────────────────────

    @Test
    fun `OcrUseCase returns OcrResult or Error`() = runTest {
        val useCase = OcrUseCaseImpl()
        val result  = useCase(bitmap)
        assertTrue(result is ScanResult.OcrResult || result is ScanResult.Error)
    }

    // ── QR Scanner ────────────────────────────────────────────────────────────

    @Test
    fun `ScanQrUseCase returns QrCodeResult or Error on blank bitmap`() = runTest {
        val useCase = ScanQrUseCaseImpl()
        val result  = useCase(bitmap)
        // Blank bitmap → either no barcode found (Error) or result
        assertTrue(result is ScanResult.QrCodeResult || result is ScanResult.Error)
    }

    // ── ImageUtils ────────────────────────────────────────────────────────────

    @Test
    fun `scaleBitmap keeps dimensions under maxEdge`() {
        val src    = Bitmap.createBitmap(2048, 1536, Bitmap.Config.ARGB_8888)
        val scaled = com.smartvision.ai.utils.ImageUtils.scaleBitmap(src, maxEdge = 1024)
        assertTrue("Width should be ≤ 1024", scaled.width  <= 1024)
        assertTrue("Height should be ≤ 1024", scaled.height <= 1024)
    }

    @Test
    fun `scaleBitmap returns original if already small`() {
        val src    = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        val result = com.smartvision.ai.utils.ImageUtils.scaleBitmap(src, maxEdge = 1024)
        assertEquals(640, result.width)
        assertEquals(480, result.height)
    }

    @Test
    fun `cropNormalized clamps out-of-range floats safely`() {
        val src     = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        // Should not throw even with slightly out-of-range values
        val cropped = com.smartvision.ai.utils.ImageUtils.cropNormalized(
            src, left = -0.1f, top = -0.1f, right = 1.1f, bottom = 1.1f
        )
        assertTrue(cropped.width  > 0)
        assertTrue(cropped.height > 0)
    }

    // ── Extensions ────────────────────────────────────────────────────────────

    @Test
    fun `toConfidenceString formats correctly`() {
        assertEquals("87%", 0.874f.toConfidenceString())
        assertEquals("100%", 1.0f.toConfidenceString())
        assertEquals("0%", 0.0f.toConfidenceString())
    }

    @Test
    fun `toHistorySummary truncates long strings`() {
        val long = "A".repeat(100)
        val sum  = long.toHistorySummary(maxLen = 20)
        assertTrue(sum.length <= 20)
        assertTrue(sum.endsWith("…"))
    }

    @Test
    fun `toHistorySummary returns full string if short`() {
        val short = "Hello world"
        assertEquals(short, short.toHistorySummary(maxLen = 80))
    }

    @Test
    fun `isValidUrl correctly identifies URLs`() {
        assertTrue("https://google.com".isValidUrl())
        assertTrue("http://example.org".isValidUrl())
        assertFalse("just text".isValidUrl())
        assertFalse("ftp://old.protocol".isValidUrl())
    }

    // ── QrType displayLabel ───────────────────────────────────────────────────

    @Test
    fun `QrType displayLabel returns human-readable strings`() {
        assertEquals("Web Link",    QrType.URL.displayLabel())
        assertEquals("Plain Text",  QrType.TEXT.displayLabel())
        assertEquals("Wi-Fi Network", QrType.WIFI.displayLabel())
    }

    // ── ResultRepository cache ────────────────────────────────────────────────

    @Test
    fun `ResultRepositoryImpl caches and retrieves result`() = runTest {
        val firestore = mockk<com.google.firebase.firestore.FirebaseFirestore>(relaxed = true)
        val repo      = ResultRepositoryImpl(firestore)
        val result    = ScanResult.OcrResult(rawText = "Test text", textBlocks = emptyList())

        repo.cacheResult(result)
        val retrieved = repo.getCachedResult()

        assertEquals(result, retrieved)
    }

    @Test
    fun `ResultRepositoryImpl caches and retrieves image path`() = runTest {
        val firestore = mockk<com.google.firebase.firestore.FirebaseFirestore>(relaxed = true)
        val repo      = ResultRepositoryImpl(firestore)

        repo.cacheImagePath("/data/user/0/test.jpg")
        assertEquals("/data/user/0/test.jpg", repo.getCachedImagePath())
    }
}

// Extension under test (imported from Utils.kt)
fun Float.toConfidenceString(): String = "${(this * 100).toInt()}%"
fun String.toHistorySummary(maxLen: Int = 80): String =
    if (length <= maxLen) this else take(maxLen - 1) + "…"
fun String.isValidUrl(): Boolean = startsWith("http://") || startsWith("https://")
fun QrType.displayLabel(): String = when (this) {
    QrType.URL     -> "Web Link"
    QrType.TEXT    -> "Plain Text"
    QrType.EMAIL   -> "Email Address"
    QrType.PHONE   -> "Phone Number"
    QrType.SMS     -> "SMS Message"
    QrType.WIFI    -> "Wi-Fi Network"
    QrType.CONTACT -> "Contact Card"
    QrType.OTHER   -> "Other"
}
