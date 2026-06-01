package com.smartvision.ai.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.smartvision.ai.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = getColor(R.color.bg_amoled)
        window.navigationBarColor = getColor(R.color.bg_amoled)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val host = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = host.navController
        bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val hidden = setOf(
                R.id.splashFragment,
                R.id.loginFragment,
                R.id.ocrScannerFragment,
                R.id.objectDetectionFragment
            )
            bottomNav.visibility = if (destination.id in hidden) View.GONE else View.VISIBLE
        }
    }
}
