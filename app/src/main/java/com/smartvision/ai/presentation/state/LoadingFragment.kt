package com.smartvision.ai.presentation.state

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.smartvision.ai.databinding.FragmentLoadingBinding

class LoadingFragment : Fragment() {
    private var _binding: FragmentLoadingBinding? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoadingBinding.inflate(inflater, container, false)
        return _binding!!.root
    }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
