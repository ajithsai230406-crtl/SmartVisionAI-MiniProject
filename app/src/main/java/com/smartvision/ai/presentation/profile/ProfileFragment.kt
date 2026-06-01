package com.smartvision.ai.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.smartvision.ai.R
import com.smartvision.ai.databinding.FragmentProfileBinding
import com.smartvision.ai.presentation.auth.AuthViewModel
import com.smartvision.ai.domain.model.UserProfile
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.settingsButton.setOnClickListener { findNavController().navigate(R.id.settingsFragment) }
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.profile.collectLatest { profile: UserProfile ->
                binding.nameText.text = profile.name
                binding.emailText.text = profile.email
                if (profile.photoUrl != null) {
                    binding.avatarView.load(profile.photoUrl)
                } else {
                    binding.avatarView.setImageResource(R.drawable.ic_profile)
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
