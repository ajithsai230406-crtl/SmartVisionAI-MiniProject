package com.smartvision.ai.data

import com.smartvision.ai.R

object FakeRepository {
    fun modules() = listOf(
        ModuleItem("OCR Scanner", R.drawable.ic_scan_text, R.id.ocrScannerFragment),
        ModuleItem("Object Detection", R.drawable.ic_object, R.id.ocrScannerFragment), // Fallback or specific fragment if exists
        ModuleItem("Translation", R.drawable.ic_translate, R.id.translationFragment),
        ModuleItem("Voice Assistant", R.drawable.ic_voice, R.id.voiceAssistantFragment),
        ModuleItem("Medicine ID", R.drawable.ic_medicine, R.id.medicineDetectionFragment),
        ModuleItem("Waste Classifier", R.drawable.ic_recycle, R.id.wasteClassificationFragment),
        ModuleItem("Barcode Scanner", R.drawable.ic_barcode, R.id.ocrScannerFragment),
        ModuleItem("Document Scanner", R.drawable.ic_document, R.id.ocrScannerFragment)
    )

    fun history() = listOf(
        HistoryItem("OCR Text", "The greatest glory in living...", "11 May, 10:30 AM", R.drawable.ic_scan_text),
        HistoryItem("Translation", "Hello, how are you?", "11 May, 10:20 AM", R.drawable.ic_translate),
        HistoryItem("Object Detection", "3 objects detected", "11 May, 10:15 AM", R.drawable.ic_object),
        HistoryItem("Medicine Identifier", "Paracetamol 500mg", "11 May, 10:10 AM", R.drawable.ic_medicine),
        HistoryItem("Waste Classifier", "Plastic (Recyclable)", "11 May, 10:05 AM", R.drawable.ic_recycle)
    )
}
