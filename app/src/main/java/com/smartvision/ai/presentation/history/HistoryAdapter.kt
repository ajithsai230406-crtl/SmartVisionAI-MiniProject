package com.smartvision.ai.presentation.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartvision.ai.R
import com.smartvision.ai.databinding.ItemHistoryBinding
import com.smartvision.ai.domain.model.HistoryRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
    private val items = mutableListOf<HistoryRecord>()
    private val format = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

    fun submitList(newItems: List<HistoryRecord>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) = holder.bind(items[position], format)
    override fun getItemCount(): Int = items.size

    class HistoryViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryRecord, format: SimpleDateFormat) {
            binding.historyTitle.text = "${item.type}: ${item.title}"
            binding.historySubtitle.text = item.details
            binding.historyTime.text = format.format(Date(item.createdAt))
            binding.historyIcon.setImageResource(
                when (item.type) {
                    "OCR" -> R.drawable.ic_scan
                    "Translation" -> R.drawable.ic_translate
                    "Waste" -> R.drawable.ic_recycle
                    "Medicine" -> R.drawable.ic_medicine
                    else -> R.drawable.ic_chat
                }
            )
        }
    }
}
