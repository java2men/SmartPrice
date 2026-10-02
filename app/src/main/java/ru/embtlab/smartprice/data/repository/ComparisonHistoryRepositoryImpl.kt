package ru.embtlab.smartprice.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.embtlab.smartprice.data.local.dao.ComparisonHistoryDao
import ru.embtlab.smartprice.data.local.entity.ComparisonHistoryEntity
import ru.embtlab.smartprice.domain.model.SavedComparison
import ru.embtlab.smartprice.domain.repository.ComparisonHistoryRepository

class ComparisonHistoryRepositoryImpl(
    private val dao: ComparisonHistoryDao
) : ComparisonHistoryRepository {

    override fun getAllHistory(): Flow<List<SavedComparison>> {
        return dao.getAllHistory().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveComparison(comparison: SavedComparison) {
        dao.insertComparison(ComparisonHistoryEntity.fromDomain(comparison))
    }

    override suspend fun deleteComparison(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}