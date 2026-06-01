package com.smartvision.ai.presentation.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartvision.ai.R
import com.smartvision.ai.databinding.FragmentAiChatBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AiChatFragment : Fragment() {
    private var _binding: FragmentAiChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AiChatViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAiChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = ChatAdapter()
        binding.chatRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.chatRecycler.adapter = adapter
        listOf("Explain OCR", "Waste tip", "Medicine warning", "Viva summary").forEach { prompt ->
            binding.suggestionRow.addView(TextView(requireContext()).apply {
                text = prompt
                setTextColor(resources.getColor(R.color.white, null))
                setBackgroundResource(R.drawable.bg_chip)
                setPadding(28)
                setOnClickListener { viewModel.send(prompt) }
            })
        }
        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text.toString()
            binding.messageInput.text?.clear()
            viewModel.send(text)
        }
        binding.voiceButton.setOnClickListener { viewModel.send("Open voice assistant") }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.messages.collectLatest {
                adapter.submitList(it)
                binding.chatRecycler.scrollToPosition((it.size - 1).coerceAtLeast(0))
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
