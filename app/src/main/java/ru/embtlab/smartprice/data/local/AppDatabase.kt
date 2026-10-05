package ru.embtlab.smartprice.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.embtlab.smartprice.data.local.converter.ProductListConverter
import ru.embtlab.smartprice.data.local.dao.ComparisonHistoryDao
import ru.embtlab.smartprice.data.local.entity.ComparisonHistoryEntity

@Database(entities = [ComparisonHistoryEntity::class], version = 2, exportSchema = false)
@TypeConverters(ProductListConverter::class)
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
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}