package com.smartvision.ai.presentation.settings

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.snackbar.Snackbar
import com.smartvision.ai.R
import com.smartvision.ai.databinding.FragmentSettingsBinding
import com.smartvision.ai.presentation.auth.AuthViewModel
import com.smartvision.ai.presentation.history.HistoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsFragmentViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val account = if (result.resultCode == Activity.RESULT_OK) GoogleSignIn.getSignedInAccountFromIntent(result.data).result else null
        authViewModel.connectGoogle(account) {
            Snackbar.make(binding.root, "Google profile connected", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.themeSwitch.setOnCheckedChangeListener { _, checked -> viewModel.setDarkMode(checked) }
        binding.clearHistoryButton.setOnClickListener {
            historyViewModel.clearHistory()
            Snackbar.make(binding.root, "History cleared", Snackbar.LENGTH_SHORT).show()
        }
        binding.aboutButton.setOnClickListener { findNavController().navigate(R.id.aboutProjectFragment) }
        binding.connectGoogleButton.setOnClickListener {
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            googleLauncher.launch(GoogleSignIn.getClient(requireActivity(), options).signInIntent)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.darkMode.collectLatest { binding.themeSwitch.isChecked = it }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
