package com.smartvision.ai.presentation.state

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.smartvision.ai.databinding.FragmentErrorBinding

class ErrorFragment : Fragment() {
    private var _binding: FragmentErrorBinding? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentErrorBinding.inflate(inflater, container, false)
        _binding!!.retryButton.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        return _binding!!.root
    }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
