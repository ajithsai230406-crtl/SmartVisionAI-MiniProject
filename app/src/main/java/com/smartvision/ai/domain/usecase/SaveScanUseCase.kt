package com.smartvision.ai.domain.usecase

import com.smartvision.ai.data.repository.HistoryRepository
import javax.inject.Inject

class SaveScanUseCase @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(moduleType: String, summary: String) {
        // Corrected: Passing the required string arguments to the local history repository.
        // HistoryRepository.save expects (type: String, title: String, details: String)
        historyRepository.save(
            type = moduleType,
            title = summary,
            details = "" // or any additional details if available
        )
    }
}
