package com.smartvision.ai.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.smartvision.ai.R
import com.smartvision.ai.domain.AuthManager
import com.smartvision.ai.ui.common.startFloating

class LoginFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val auth = AuthManager(requireContext())
        view.findViewById<ImageView>(R.id.loginOrb).startFloating()
        val email = view.findViewById<EditText>(R.id.emailInput)
        val password = view.findViewById<EditText>(R.id.passwordInput)
        view.findViewById<MaterialButton>(R.id.loginButton).setOnClickListener {
            auth.login(email.text.toString(), password.text.toString()) {
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }
        }
        view.findViewById<MaterialButton>(R.id.googleButton).setOnClickListener {
            auth.googleSignInPlaceholder {
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }
        }
    }
}
