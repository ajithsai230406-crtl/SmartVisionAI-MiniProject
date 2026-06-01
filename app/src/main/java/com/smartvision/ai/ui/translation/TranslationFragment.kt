package com.smartvision.ai.ui.translation

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.smartvision.ai.R
import java.util.Locale

class TranslationFragment : Fragment(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_translation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tts = TextToSpeech(requireContext(), this)
        val source = view.findViewById<EditText>(R.id.sourceInput)
        val output = view.findViewById<TextView>(R.id.outputText)
        
        // Fixed: Use R.id.translateButton to match fragment_translation.xml
        view.findViewById<View>(R.id.translateButton).setOnClickListener {
            output.text = demoTranslate(source.text.toString())
        }
        
        output.setOnClickListener { tts?.speak(output.text, TextToSpeech.QUEUE_FLUSH, null, "translation") }

        // Optional: Handling other buttons if they exist in XML
        view.findViewById<View>(R.id.copyButton)?.setOnClickListener {
            // Copy implementation
        }
        
        view.findViewById<View>(R.id.speakButton)?.setOnClickListener {
            tts?.speak(output.text, TextToSpeech.QUEUE_FLUSH, null, "translation")
        }
    }

    private fun demoTranslate(input: String): String {
        val clean = input.trim()
        return when {
            clean.isBlank() -> "Please enter text to translate."
            "नमस्ते" in clean -> "Hello, how are you?"
            else -> "AI translation placeholder: $clean"
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
    }

    override fun onDestroyView() {
        tts?.shutdown()
        super.onDestroyView()
    }
}
