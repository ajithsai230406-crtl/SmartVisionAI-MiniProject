package com.smartvision.ai.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * Executes the given block with standard exponential backoff retries.
 * Fortifies remote API and database transactions from transient networking glitches.
 */
suspend fun <T> retryWithDelay(
    retries: Int = 3,
    initialDelayMillis: Long = 1000L,
    maxDelayMillis: Long = 6000L,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMillis
    repeat(retries - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMillis)
        }
    }
    return block() // Last attempt throws the exception if it fails
}

/**
 * Runs the suspending block securely on Dispatchers.IO.
 */
suspend fun <T> ioSafe(block: suspend () -> T): T {
    return withContext(Dispatchers.IO) {
        block()
    }
}
