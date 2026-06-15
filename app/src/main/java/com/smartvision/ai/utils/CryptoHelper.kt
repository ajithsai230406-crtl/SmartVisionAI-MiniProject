package com.smartvision.ai.utils

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val STATIC_SALT = "SmartVisionAI_Secure_Salt_2026"

    private fun getSecretKeySpec(userId: String): SecretKeySpec {
        // Derive a 256-bit key using SHA-256 of the userId + STATIC_SALT
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest((userId + STATIC_SALT).toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun getIv(userId: String): IvParameterSpec {
        // Generate a deterministic 16-byte IV using MD5 of the userId
        val digest = MessageDigest.getInstance("MD5")
        val ivBytes = digest.digest(userId.toByteArray(Charsets.UTF_8))
        return IvParameterSpec(ivBytes)
    }

    fun encrypt(text: String, userId: String): String {
        if (text.isBlank()) return text
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKeySpec(userId), getIv(userId))
            val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            text
        }
    }

    fun decrypt(encryptedText: String, userId: String): String {
        if (encryptedText.isBlank()) return encryptedText
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKeySpec(userId), getIv(userId))
            val decodedBytes = Base64.decode(encryptedText, Base64.NO_WRAP)
            String(cipher.doFinal(decodedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedText
        }
    }
}
