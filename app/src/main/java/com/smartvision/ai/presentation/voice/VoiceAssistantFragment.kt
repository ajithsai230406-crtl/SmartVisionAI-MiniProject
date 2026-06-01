package com.smartvision.ai.presentation.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.smartvision.ai.R
import com.smartvision.ai.databinding.FragmentVoiceAssistantBinding
import java.util.Locale

class VoiceAssistantFragment : Fragment(), TextToSpeech.OnInitListener {
    private var _binding: FragmentVoiceAssistantBinding? = null
    private val binding get() = _binding!!
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    private val audioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening() else binding.listeningText.text = "Microphone permission denied"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVoiceAssistantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tts = TextToSpeech(requireContext(), this)
        recognizer = SpeechRecognizer.createSpeechRecognizer(requireContext()).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { binding.listeningText.text = "Listening..." }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() { binding.listeningText.text = "Processing..." }
                override fun onError(error: Int) { binding.listeningText.text = "Tap the orb and try again" }
                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    binding.transcriptText.text = text.ifBlank { "No command heard." }
                    handleCommand(text)
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
        binding.orbView.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startListening()
            } else {
                audioPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        recognizer?.startListening(intent)
    }

    private fun handleCommand(text: String) {
        val lower = text.lowercase()
        val response = when {
            "ocr" in lower || "scan text" in lower -> {
                findNavController().navigate(R.id.ocrScannerFragment)
                "Opening OCR Scanner."
            }
            "translate" in lower -> {
                findNavController().navigate(R.id.translationFragment)
                "Opening Translator."
            }
            "medicine" in lower -> {
                findNavController().navigate(R.id.medicineDetectionFragment)
                "Opening Medicine Scanner."
            }
            "waste" in lower || "recycle" in lower -> {
                findNavController().navigate(R.id.wasteClassificationFragment)
                "Opening Waste Classifier."
            }
            else -> "You can say open OCR, translate text, open medicine detector, or open waste classifier."
        }
        tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, "voice-response")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
    }

    override fun onDestroyView() {
        recognizer?.destroy()
        tts?.shutdown()
        _binding = null
        super.onDestroyView()
    }
}
