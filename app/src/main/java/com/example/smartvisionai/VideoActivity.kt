package com.example.smartvisionai

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * VideoActivity is currently disabled because the required video resource (R.raw.intro) is missing.
 * The app now uses SplashActivity (Compose-based) as the launcher.
 */
class VideoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Redirect to MainActivity since this activity is missing its video resource
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
