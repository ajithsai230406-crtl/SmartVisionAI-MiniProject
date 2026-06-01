package com.smartvision.ai.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.smartvision.ai.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = FeatureAdapter { feature -> findNavController().navigate(feature.destinationId) }
        binding.featureRecycler.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.featureRecycler.adapter = adapter
        adapter.submitList(viewModel.features)
        binding.searchInput.setOnEditorActionListener { _, _, _ ->
            findNavController().navigate(com.smartvision.ai.R.id.aiChatFragment)
            true
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
