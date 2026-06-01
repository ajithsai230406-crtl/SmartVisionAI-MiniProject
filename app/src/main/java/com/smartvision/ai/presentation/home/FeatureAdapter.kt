package com.smartvision.ai.presentation.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartvision.ai.databinding.ItemFeatureCardBinding
import com.smartvision.ai.domain.model.FeatureItem

class FeatureAdapter(
    private val onClick: (FeatureItem) -> Unit
) : RecyclerView.Adapter<FeatureAdapter.FeatureViewHolder>() {
    private val items = mutableListOf<FeatureItem>()

    fun submitList(newItems: List<FeatureItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val binding = ItemFeatureCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeatureViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class FeatureViewHolder(private val binding: ItemFeatureCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FeatureItem) {
            binding.title.text = item.title
            binding.subtitle.text = item.subtitle
            binding.icon.setImageResource(item.icon)
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
