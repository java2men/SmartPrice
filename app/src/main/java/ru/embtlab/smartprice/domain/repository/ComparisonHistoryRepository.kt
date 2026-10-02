package ru.embtlab.smartprice.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.embtlab.smartprice.domain.model.SavedComparison

interface ComparisonHistoryRepository {
    fun getAllHistory(): Flow<List<SavedComparison>>
    suspend fun saveComparison(comparison: SavedComparison)
    suspend fun deleteComparison(id: Long)
    suspend fun clearAll()
}