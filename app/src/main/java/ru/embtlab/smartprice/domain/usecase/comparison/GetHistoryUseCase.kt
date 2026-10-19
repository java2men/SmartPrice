package ru.embtlab.smartprice.domain.usecase.comparison

import kotlinx.coroutines.flow.Flow
import ru.embtlab.smartprice.domain.model.history.SavedComparison
import ru.embtlab.smartprice.domain.repository.ComparisonHistoryRepository

class GetHistoryUseCase(private val repository: ComparisonHistoryRepository) {
    operator fun invoke(): Flow<List<SavedComparison>> = repository.getAllHistory()
}