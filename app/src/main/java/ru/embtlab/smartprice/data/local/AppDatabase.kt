package ru.embtlab.smartprice.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.embtlab.smartprice.data.local.dao.ComparisonHistoryDao
import ru.embtlab.smartprice.data.local.entity.ComparisonHistoryEntity

@Database(entities = [ComparisonHistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): ComparisonHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_price_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}