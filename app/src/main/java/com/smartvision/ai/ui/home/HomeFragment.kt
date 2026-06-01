package com.smartvision.ai.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartvision.ai.R
import com.smartvision.ai.data.FakeRepository
import com.smartvision.ai.ui.adapters.ModuleAdapter

class HomeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navController = findNavController()

        // Use searchInput instead of homeSearch as defined in fragment_home.xml
        view.findViewById<View>(R.id.searchInput)?.setOnClickListener { 
            navController.navigate(R.id.aiChatFragment) 
        }

        // Fixed: Renamed modulesRecycler to featureRecycler to match fragment_home.xml
        view.findViewById<RecyclerView>(R.id.featureRecycler)?.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = ModuleAdapter(FakeRepository.modules()) { module ->
                try {
                    navController.navigate(module.destinationId)
                } catch (e: Exception) {
                    // Fallback or log if destination is missing in nav_graph
                }
            }
        }
    }
}
