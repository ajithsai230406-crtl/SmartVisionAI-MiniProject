package com.smartvision.ai.ui.medicine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.smartvision.ai.R

class MedicineFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_medicine, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.medicineBack).setOnClickListener { findNavController().navigateUp() }
        view.findViewById<View>(R.id.medicineDetailsButton).setOnClickListener {
            Toast.makeText(requireContext(), "AI medicine knowledge card placeholder", Toast.LENGTH_SHORT).show()
        }
    }
}
