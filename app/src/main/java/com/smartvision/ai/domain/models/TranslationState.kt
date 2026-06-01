package com.smartvision.ai.domain.models

data class TranslationState(
    val inputText: String = "",
    val sourceLang: String = "auto",
    val targetLang: String = "en",
    val outputText: String = "",
    val isTranslating: Boolean = false,
    val isListening: Boolean = false,
    val recentTranslations: List<Pair<String, String>> = emptyList(),
    val error: String? = null
)
