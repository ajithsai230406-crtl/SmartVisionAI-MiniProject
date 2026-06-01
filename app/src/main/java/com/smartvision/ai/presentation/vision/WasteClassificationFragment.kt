package com.smartvision.ai.presentation.vision

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.smartvision.ai.R
import com.smartvision.ai.databinding.FragmentWasteClassificationBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WasteClassificationFragment : Fragment() {
    private var _binding: FragmentWasteClassificationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VisionViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else findNavController().navigate(R.id.cameraPermissionFragment)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWasteClassificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.captureButton.setOnClickListener { viewModel.classifyWaste() }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.waste.collectLatest { result ->
                if (result != null) {
                    binding.resultText.text = "${result.label}  ${(result.confidence * 100).toInt()}%"
                    binding.tipText.text = result.tip
                    binding.overlayView.showLabel("${result.label} ${(result.confidence * 100).toInt()}%")
                }
            }
        }
    }

    private fun startCamera() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val provider = ProcessCameraProvider.getInstance(requireContext()).await()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
                provider.unbindAll()
                provider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
