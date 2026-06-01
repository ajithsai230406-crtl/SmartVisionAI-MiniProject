package com.smartvision.ai.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartvision.ai.R
import com.smartvision.ai.data.HistoryItem

class HistoryAdapter(private val items: List<HistoryItem>) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        return HistoryViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon = view.findViewById<ImageView>(R.id.historyIcon)
        private val title = view.findViewById<TextView>(R.id.historyTitle)
        private val subtitle = view.findViewById<TextView>(R.id.historySubtitle)
        private val time = view.findViewById<TextView>(R.id.historyTime)

        fun bind(item: HistoryItem) {
            icon.setImageResource(item.iconRes)
            title.text = item.title
            subtitle.text = item.subtitle
            time.text = item.time
        }
    }
}
