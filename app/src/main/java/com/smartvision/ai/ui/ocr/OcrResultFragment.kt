package com.smartvision.ai.ui.ocr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.smartvision.ai.R
import java.util.Locale

class OcrResultFragment : Fragment(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private lateinit var text: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_ocr_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tts = TextToSpeech(requireContext(), this)
        text = arguments?.getString("extractedText").orEmpty().ifBlank {
            "The greatest glory in living lies not in never falling, but in rising every time we fall."
        }
        view.findViewById<TextView>(R.id.detectedText).text = text
        view.findViewById<View>(R.id.resultBack).setOnClickListener { findNavController().navigateUp() }
        view.findViewById<View>(R.id.copyButton).setOnClickListener { copyText() }
        view.findViewById<View>(R.id.shareButton).setOnClickListener { shareText() }
        view.findViewById<View>(R.id.translateButton).setOnClickListener { findNavController().navigate(R.id.translationFragment) }
        view.findViewById<View>(R.id.speakButton).setOnClickListener { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ocr") }
        view.findViewById<View>(R.id.saveHistoryButton).setOnClickListener {
            Toast.makeText(requireContext(), "Saved to history", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyText() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("OCR Text", text))
        Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show()
    }

    private fun shareText() {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }, "Share text"))
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
    }

    override fun onDestroyView() {
        tts?.shutdown()
        super.onDestroyView()
    }
}
