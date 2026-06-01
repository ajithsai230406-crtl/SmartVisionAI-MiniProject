package com.smartvision.ai.domain.usecase

import android.graphics.Bitmap
import com.smartvision.ai.domain.models.*
import javax.inject.Inject

// ── Validation (our addition — no interface conflict) ─────────────────────────

class ValidateInputUseCase @Inject constructor() {
    fun validateEmail(email: String): ValidationResult {
        if (email.isBlank()) return ValidationResult(false, "Email cannot be empty")
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return ValidationResult(false, "Enter a valid email address")
        return ValidationResult(true)
    }
    fun validatePassword(password: String): ValidationResult {
        if (password.isBlank()) return ValidationResult(false, "Password cannot be empty")
        if (password.length < 6) return ValidationResult(false, "Password must be at least 6 characters")
        return ValidationResult(true)
    }
    fun validateText(text: String, label: String = "Text"): ValidationResult {
        if (text.isBlank()) return ValidationResult(false, "$label cannot be empty")
        if (text.length > 5000) return ValidationResult(false, "$label is too long")
        return ValidationResult(true)
    }
}

data class ValidationResult(val isValid: Boolean, val error: String = "")
