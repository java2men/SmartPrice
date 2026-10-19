// data/local/dao/ComparisonHistoryDao.kt
package ru.embtlab.smartprice.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.embtlab.smartprice.data.local.db.entity.ComparisonHistoryEntity

@Dao
interface ComparisonHistoryDao {
    @Query("SELECT * FROM comparison_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ComparisonHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComparison(entity: ComparisonHistoryEntity)

    @Query("DELETE FROM comparison_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM comparison_history")
    suspend fun clearAll()
}