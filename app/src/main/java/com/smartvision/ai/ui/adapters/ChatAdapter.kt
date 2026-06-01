package com.smartvision.ai.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartvision.ai.R
import com.smartvision.ai.data.ChatMessage

class ChatAdapter(private val messages: MutableList<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
    override fun getItemViewType(position: Int): Int = if (messages[position].fromUser) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layout = if (viewType == 1) R.layout.item_chat_user else R.layout.item_chat_ai
        return ChatViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.text.text = messages[position].text
    }

    fun add(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.lastIndex)
    }

    fun replaceLast(message: ChatMessage) {
        if (messages.isEmpty()) return
        messages[messages.lastIndex] = message
        notifyItemChanged(messages.lastIndex)
    }

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.chatText)
    }
}
