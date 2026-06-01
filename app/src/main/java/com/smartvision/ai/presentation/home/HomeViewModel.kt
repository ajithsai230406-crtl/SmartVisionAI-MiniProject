package com.smartvision.ai.presentation.home

import androidx.lifecycle.ViewModel
import com.smartvision.ai.R
import com.smartvision.ai.domain.model.FeatureItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    val features = listOf(
        FeatureItem("OCR Scanner", "Extract text from live camera and images", R.drawable.ic_scan, R.id.ocrScannerFragment),
        FeatureItem("Translator", "Translate and speak text in multiple languages", R.drawable.ic_translate, R.id.translationFragment),
        FeatureItem("Voice Assistant", "Control Smart Vision with voice commands", R.drawable.ic_mic, R.id.voiceAssistantFragment),
        FeatureItem("Medicine Scanner", "Identify common medicine packaging safely", R.drawable.ic_medicine, R.id.medicineDetectionFragment),
        FeatureItem("Waste Classifier", "Classify plastic, paper, organic, metal and glass", R.drawable.ic_recycle, R.id.wasteClassificationFragment),
        FeatureItem("AI Chat", "Ask project, scan and study questions", R.drawable.ic_chat, R.id.aiChatFragment)
    )
}
