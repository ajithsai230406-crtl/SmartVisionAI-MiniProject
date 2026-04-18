package com.example.smartvisionai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class VideoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        val videoView = findViewById<VideoView>(R.id.videoView)

        val uri = Uri.parse("android.resource://" + packageName + "/" + R.raw.intro)
        videoView.setVideoURI(uri)

        videoView.setOnCompletionListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        videoView.start()
    }
}