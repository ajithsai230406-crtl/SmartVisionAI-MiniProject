package com.smartvision.ai.presentation.translation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.smartvision.ai.databinding.FragmentTranslationBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class TranslationFragment : Fragment(), TextToSpeech.OnInitListener {
    private var _binding: FragmentTranslationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TranslationViewModel by viewModels()
    private var tts: TextToSpeech? = null
    private val languages = listOf("English", "Hindi", "Telugu", "Tamil", "French", "Spanish", "German", "Japanese")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTranslationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tts = TextToSpeech(requireContext(), this)
        binding.languageSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages)
        binding.sourceInput.setText("Smart Vision AI helps users read, translate and understand the world.")
        binding.translateButton.setOnClickListener {
            viewModel.translate(binding.sourceInput.text.toString(), binding.languageSpinner.selectedItem.toString())
        }
        binding.copyButton.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Smart Vision Translation", binding.outputText.text))
            Snackbar.make(binding.root, "Translation copied", Snackbar.LENGTH_SHORT).show()
        }
        binding.speakButton.setOnClickListener {
            tts?.speak(binding.outputText.text.toString(), TextToSpeech.QUEUE_FLUSH, null, "translation")
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.output.collectLatest { binding.outputText.text = it }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
    }

    override fun onDestroyView() {
        tts?.shutdown()
        _binding = null
        super.onDestroyView()
    }
}
