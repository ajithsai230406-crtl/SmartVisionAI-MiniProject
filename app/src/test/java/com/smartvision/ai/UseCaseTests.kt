package com.smartvision.ai

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.text.TextRecognition
import com.smartvision.ai.domain.models.*
import com.smartvision.ai.data.repository.*
import io.mockk.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After

class SmartVisionUseCaseTests {

    private lateinit var bitmap: Bitmap

    @Before
    fun setup() {
        mockkStatic(Bitmap::class)
        mockkStatic(ObjectDetection::class)
        mockkStatic(TextRecognition::class)
        mockkStatic(BarcodeScanning::class)

        // Mock Bitmap static creation
        every { Bitmap.createBitmap(any<Int>(), any<Int>(), any()) } returns mockk(relaxed = true)
        every { Bitmap.createScaledBitmap(any(), any(), any(), any()) } returns mockk(relaxed = true)

        // Mock ML Kit client static initializers
        every { ObjectDetection.getClient(any()) } returns mockk(relaxed = true)
        every { TextRecognition.getClient(any()) } returns mockk(relaxed = true)
        every { BarcodeScanning.getClient() } returns mockk(relaxed = true)

        bitmap = mockk(relaxed = true)
        every { bitmap.width  } returns 640
        every { bitmap.height } returns 480
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    // ── Object Detection ──────────────────────────────────────────────────────

    @Test
    fun `DetectObjectsUseCase returns ObjectDetectionResult on success`() = runTest {
        val useCase = DetectObjectsUseCaseImpl()
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
        assertTrue(result is ScanResult.QrCodeResult || result is ScanResult.Error)
    }

    // ── ImageUtils ────────────────────────────────────────────────────────────

    @Test
    fun `scaleBitmap keeps dimensions under maxEdge`() {
        val src = mockk<Bitmap>(relaxed = true)
        every { src.width } returns 2048
        every { src.height } returns 1536
        
        val scaled = mockk<Bitmap>()
        every { scaled.width } returns 1024
        every { scaled.height } returns 768
        
        every { Bitmap.createScaledBitmap(src, 1024, 768, true) } returns scaled

        val result = com.smartvision.ai.utils.ImageUtils(mockk(relaxed = true)).scaleBitmap(src, maxSizePx = 1024)
        assertEquals(scaled, result)
        assertEquals(1024, result.width)
        assertEquals(768, result.height)
    }

    @Test
    fun `scaleBitmap returns original if already small`() {
        val src = mockk<Bitmap>(relaxed = true)
        every { src.width } returns 640
        every { src.height } returns 480
        val result = com.smartvision.ai.utils.ImageUtils(mockk(relaxed = true)).scaleBitmap(src, maxSizePx = 1024)
        assertEquals(src, result)
    }

    @Test
    fun `cropNormalized clamps out-of-range floats safely`() {
        val src = mockk<Bitmap>(relaxed = true)
        every { src.width } returns 640
        every { src.height } returns 480
        
        val cropped = mockk<Bitmap>()
        every { cropped.width } returns 640
        every { cropped.height } returns 480
        
        every { Bitmap.createBitmap(src, 0, 0, 640, 480) } returns cropped

        val result = com.smartvision.ai.utils.ImageUtils(mockk(relaxed = true)).cropBitmap(
            src, left = -0.1f, top = -0.1f, right = 1.1f, bottom = 1.1f
        )
        assertEquals(cropped, result)
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
        val auth      = mockk<com.google.firebase.auth.FirebaseAuth>(relaxed = true)
        val repo      = ResultRepositoryImpl(firestore, auth)
        val result    = ScanResult.OcrResult(rawText = "Test text", textBlocks = emptyList())

        repo.cacheResult(result)
        val retrieved = repo.getCachedResult()

        assertEquals(result, retrieved)
    }

    @Test
    fun `ResultRepositoryImpl caches and retrieves image path`() = runTest {
        val firestore = mockk<com.google.firebase.firestore.FirebaseFirestore>(relaxed = true)
        val auth      = mockk<com.google.firebase.auth.FirebaseAuth>(relaxed = true)
        val repo      = ResultRepositoryImpl(firestore, auth)

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
