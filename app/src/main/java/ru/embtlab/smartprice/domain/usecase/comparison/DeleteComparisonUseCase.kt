package ru.embtlab.smartprice.domain.usecase.comparison

import ru.embtlab.smartprice.domain.repository.ComparisonHistoryRepository

class DeleteComparisonUseCase(
    private val repository: ComparisonHistoryRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteComparison(id)
    }
}