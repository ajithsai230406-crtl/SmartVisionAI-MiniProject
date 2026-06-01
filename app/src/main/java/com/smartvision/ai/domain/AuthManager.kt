package com.smartvision.ai.domain

import android.content.Context
import android.widget.Toast

class AuthManager(private val context: Context) {
    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Use any email and password for the demo", Toast.LENGTH_SHORT).show()
            return
        }
        onSuccess()
    }

    fun googleSignInPlaceholder(onSuccess: () -> Unit) {
        Toast.makeText(context, "Firebase Google sign-in placeholder", Toast.LENGTH_SHORT).show()
        onSuccess()
    }
}
