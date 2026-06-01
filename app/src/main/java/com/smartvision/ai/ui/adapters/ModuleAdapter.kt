package com.smartvision.ai.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartvision.ai.R
import com.smartvision.ai.data.ModuleItem

class ModuleAdapter(
    private val items: List<ModuleItem>,
    private val onClick: (ModuleItem) -> Unit
) : RecyclerView.Adapter<ModuleAdapter.ModuleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_module_card, parent, false)
        return ModuleViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ModuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon = itemView.findViewById<ImageView>(R.id.moduleIcon)
        private val title = itemView.findViewById<TextView>(R.id.moduleTitle)

        fun bind(item: ModuleItem) {
            icon.setImageResource(item.iconRes)
            title.text = item.title
            itemView.setOnClickListener { onClick(item) }
            val position = adapterPosition

            if (position != RecyclerView.NO_POSITION) {
                itemView.animate()
                    .translationY(if (position % 2 == 0) -2f else 2f)
                    .setDuration(700)
                    .start()
            }

        }
    }
}
