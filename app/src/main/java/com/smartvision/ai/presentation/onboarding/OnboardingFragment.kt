package com.smartvision.ai.presentation.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.smartvision.ai.R
import com.smartvision.ai.databinding.FragmentOnboardingBinding

class OnboardingFragment : Fragment() {
    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.onboardingPager.adapter = OnboardingAdapter()
        binding.skipButton.setOnClickListener { findNavController().navigate(R.id.loginFragment) }
        binding.nextButton.setOnClickListener {
            val next = binding.onboardingPager.currentItem + 1
            if (next < 3) binding.onboardingPager.currentItem = next else findNavController().navigate(R.id.loginFragment)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
