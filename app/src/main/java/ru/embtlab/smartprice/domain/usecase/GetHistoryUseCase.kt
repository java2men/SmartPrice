package ru.embtlab.smartprice.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.embtlab.smartprice.domain.model.SavedComparison
import ru.embtlab.smartprice.domain.repository.ComparisonHistoryRepository

class GetHistoryUseCase(private val repository: ComparisonHistoryRepository) {
    operator fun invoke(): Flow<List<SavedComparison>> = repository.getAllHistory()
}