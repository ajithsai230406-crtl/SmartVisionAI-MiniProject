package com.smartvision.ai.presentation.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartvision.ai.R
import com.smartvision.ai.databinding.ItemOnboardingBinding

class OnboardingAdapter : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {
    private val pages = listOf(
        Page(R.drawable.ic_camera, "AI Camera Detection", "Detect objects, text, medicine and waste with a clean CameraX workflow."),
        Page(R.drawable.ic_translate, "Translate and Speak", "Use ML Kit translation and Android Text-to-Speech for multilingual accessibility."),
        Page(R.drawable.ic_chat, "Assistant Dashboard", "Chat, voice commands, scan history and settings in one premium interface.")
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = ItemOnboardingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) = holder.bind(pages[position])
    override fun getItemCount(): Int = pages.size

    class OnboardingViewHolder(private val binding: ItemOnboardingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(page: Page) {
            binding.icon.setImageResource(page.icon)
            binding.title.text = page.title
            binding.subtitle.text = page.subtitle
        }
    }

    data class Page(val icon: Int, val title: String, val subtitle: String)
}
