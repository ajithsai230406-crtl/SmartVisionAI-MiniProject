package com.smartvision.ai.presentation.state

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.smartvision.ai.databinding.FragmentSuccessBinding

class SuccessFragment : Fragment() {
    private var _binding: FragmentSuccessBinding? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSuccessBinding.inflate(inflater, container, false)
        return _binding!!.root
    }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
