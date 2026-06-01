package com.smartvision.ai.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartvision.ai.R
import com.smartvision.ai.data.FakeRepository
import com.smartvision.ai.ui.adapters.HistoryAdapter

class HistoryFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val filters = view.findViewById<LinearLayout>(R.id.historyFilters)
        listOf("All", "OCR", "Translate", "Detect", "Other").forEach { filters.addView(chip(it)) }
        view.findViewById<RecyclerView>(R.id.historyRecycler).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = HistoryAdapter(FakeRepository.history())
        }
    }

    private fun chip(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 12f
            setPadding(26, 12, 26, 12)
            background = resources.getDrawable(R.drawable.bg_chip, null)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 10, 0)
            layoutParams = params
        }
    }
}
