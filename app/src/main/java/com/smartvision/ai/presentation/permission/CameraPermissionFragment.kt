package com.smartvision.ai.presentation.permission

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.smartvision.ai.R
import com.smartvision.ai.databinding.FragmentCameraPermissionBinding

class CameraPermissionFragment : Fragment() {
    private var _binding: FragmentCameraPermissionBinding? = null
    private val binding get() = _binding!!

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) findNavController().navigate(R.id.homeFragment)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCameraPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.allowButton.setOnClickListener { requestPermission.launch(Manifest.permission.CAMERA) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
