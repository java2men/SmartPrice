package ru.embtlab.smartprice.domain.usecase

import ru.embtlab.smartprice.domain.model.SavedComparison
import ru.embtlab.smartprice.domain.repository.ComparisonHistoryRepository

class SaveComparisonUseCase(private val repository: ComparisonHistoryRepository) {
    suspend operator fun invoke(comparison: SavedComparison) {
        repository.saveComparison(comparison)
    }
}