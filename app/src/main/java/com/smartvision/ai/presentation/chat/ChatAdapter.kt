package com.smartvision.ai.presentation.chat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.smartvision.ai.R
import com.smartvision.ai.databinding.ItemChatMessageBinding
import com.smartvision.ai.domain.model.ChatMessage

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
    private val items = mutableListOf<ChatMessage>()

    fun submitList(newItems: List<ChatMessage>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    class ChatViewHolder(private val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.messageText.text = message.text
            binding.messageText.setBackgroundResource(if (message.fromUser) R.drawable.bg_message_user else R.drawable.bg_message_ai)
            val params = binding.messageText.layoutParams as FrameLayout.LayoutParams
            params.gravity = if (message.fromUser) Gravity.END else Gravity.START
            binding.messageText.layoutParams = params
        }
    }
}
