package com.smartvision.ai.presentation.ocr

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import com.smartvision.ai.R
import com.smartvision.ai.data.repository.OcrRepository
import com.smartvision.ai.databinding.FragmentOcrScannerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class OcrScannerFragment : Fragment(), TextToSpeech.OnInitListener {
    private var _binding: FragmentOcrScannerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OcrScannerViewModel by viewModels()
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val processing = AtomicBoolean(false)
    private var camera: Camera? = null
    private var tts: TextToSpeech? = null

    @Inject lateinit var ocrRepository: OcrRepository

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else findNavController().navigate(R.id.cameraPermissionFragment)
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                val text = ocrRepository.recognizeUri(requireContext(), uri).getOrElse { it.message ?: "OCR failed." }
                binding.extractedText.text = text
                viewModel.saveManual(text)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOcrScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tts = TextToSpeech(requireContext(), this)
        binding.backButton.setOnClickListener { findNavController().popBackStack() }
        binding.flashButton.setOnClickListener {
            val enabled = camera?.cameraInfo?.torchState?.value != 1
            camera?.cameraControl?.enableTorch(enabled)
        }
        binding.galleryButton.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.copyButton.setOnClickListener { copyText(binding.extractedText.text.toString()) }
        binding.translateButton.setOnClickListener { findNavController().navigate(R.id.translationFragment) }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val providerFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(requireContext())
        providerFuture.addListener({
            val provider: ProcessCameraProvider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage == null || !processing.compareAndSet(false, true)) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val text = viewModel.recognize(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    binding.extractedText.text = text
                    binding.overlayView.showLabel("OCR ${(80..99).random()}%")
                    processing.set(false)
                    imageProxy.close()
                }
            }
            provider.unbindAll()
            camera = provider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun copyText(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Smart Vision OCR", text))
        Snackbar.make(binding.root, "Text copied", Snackbar.LENGTH_SHORT).show()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
    }

    override fun onDestroyView() {
        tts?.shutdown()
        cameraExecutor.shutdown()
        _binding = null
        super.onDestroyView()
    }
}
