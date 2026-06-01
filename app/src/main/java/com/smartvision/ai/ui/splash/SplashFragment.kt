package com.smartvision.ai.ui.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.smartvision.ai.R
import com.smartvision.ai.ui.common.startPulse

class SplashFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val motion = view.findViewById<MotionLayout>(R.id.splashMotion)
        val logo = view.findViewById<ImageView>(R.id.logoEye)
        motion.post { motion.transitionToEnd() }
        logo.startPulse()
        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
        }, 3000)
    }
}
