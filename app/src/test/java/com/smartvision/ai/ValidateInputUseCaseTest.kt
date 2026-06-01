package com.smartvision.ai

import com.smartvision.ai.domain.usecase.ValidateInputUseCase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ValidateInputUseCaseTest {

    private lateinit var useCase: ValidateInputUseCase

    @Before
    fun setUp() { useCase = ValidateInputUseCase() }

    // ── Email validation ──────────────────────────────────────────────────────

    @Test
    fun `valid email passes`() {
        val result = useCase.validateEmail("test@example.com")
        assertTrue(result.isValid)
    }

    @Test
    fun `empty email fails`() {
        val result = useCase.validateEmail("")
        assertFalse(result.isValid)
        assertEquals("Email cannot be empty", result.error)
    }

    @Test
    fun `malformed email fails`() {
        val result = useCase.validateEmail("not-an-email")
        assertFalse(result.isValid)
    }

    // ── Password validation ───────────────────────────────────────────────────

    @Test
    fun `valid password passes`() {
        val result = useCase.validatePassword("secret123")
        assertTrue(result.isValid)
    }

    @Test
    fun `short password fails`() {
        val result = useCase.validatePassword("abc")
        assertFalse(result.isValid)
        assertTrue(result.error.contains("6"))
    }

    @Test
    fun `empty password fails`() {
        val result = useCase.validatePassword("")
        assertFalse(result.isValid)
    }

    // ── Text validation ───────────────────────────────────────────────────────

    @Test
    fun `valid text passes`() {
        val result = useCase.validateText("Hello world")
        assertTrue(result.isValid)
    }

    @Test
    fun `blank text fails`() {
        val result = useCase.validateText("   ")
        assertFalse(result.isValid)
    }

    @Test
    fun `too long text fails`() {
        val result = useCase.validateText("a".repeat(6000))
        assertFalse(result.isValid)
    }
}
