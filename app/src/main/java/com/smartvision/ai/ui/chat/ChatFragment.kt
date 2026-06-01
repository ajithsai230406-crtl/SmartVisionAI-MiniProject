package com.smartvision.ai.ui.chat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartvision.ai.R
import com.smartvision.ai.data.ChatMessage
import com.smartvision.ai.domain.GeminiAssistant
import com.smartvision.ai.ui.adapters.ChatAdapter

class ChatFragment : Fragment() {
    private val assistant = GeminiAssistant()
    private lateinit var adapter: ChatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val messages = mutableListOf(
            ChatMessage("Hello Ajith. I am your AI assistant. How can I help you today?", false)
        )
        adapter = ChatAdapter(messages)
        val recycler = view.findViewById<RecyclerView>(R.id.chatRecycler)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        val input = view.findViewById<EditText>(R.id.messageInput)
        view.findViewById<View>(R.id.sendButton).setOnClickListener {
            send(input.text.toString(), input, recycler)
        }
        view.findViewById<View>(R.id.chatCameraButton).setOnClickListener {
            findNavController().navigate(R.id.ocrScannerFragment)
        }
        view.findViewById<View>(R.id.chatBack).setOnClickListener { findNavController().navigateUp() }

        val suggestions = view.findViewById<LinearLayout>(R.id.suggestionChips)
        listOf("Translate this image", "What medicine is this?", "Read this text", "Identify object", "Open OCR").forEach { label ->
            suggestions.addView(chip(label) { send(label, input, recycler) })
        }
    }

    private fun send(message: String, input: EditText, recycler: RecyclerView) {
        val clean = message.trim()
        if (clean.isEmpty()) return
        input.setText("")
        adapter.add(ChatMessage(clean, true))
        recycler.scrollToPosition(adapter.itemCount - 1)
        adapter.add(ChatMessage("SmartVision is thinking...", false))
        recycler.scrollToPosition(adapter.itemCount - 1)
        Handler(Looper.getMainLooper()).postDelayed({
            val response = assistant.reply(clean)
            adapter.replaceLast(ChatMessage(response, false))
            recycler.scrollToPosition(adapter.itemCount - 1)
            routeIfNeeded(clean)
        }, 750)
    }

    private fun routeIfNeeded(input: String) {
        val command = input.lowercase()
        val destination = when {
            "ocr" in command || "read" in command || "scan text" in command -> R.id.ocrScannerFragment
            "translate" in command -> R.id.translationFragment
            "medicine" in command -> R.id.medicineDetectionFragment
            "waste" in command -> R.id.wasteClassificationFragment
            "object" in command || "identify" in command || "detect" in command -> R.id.objectDetectionFragment
            else -> null
        }
        destination?.let { findNavController().navigate(it) }
    }

    private fun chip(text: String, onClick: () -> Unit): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 12f
            setPadding(22, 12, 22, 12)
            background = resources.getDrawable(R.drawable.bg_chip, null)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 10, 0)
            layoutParams = params
            setOnClickListener { onClick() }
        }
    }
}
