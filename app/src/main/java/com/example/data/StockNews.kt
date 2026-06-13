package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "stock_news")
data class StockNews(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val companyName: String,
    val ticker: String,
    val price: Double = 0.0,
    val volume: Long = 0,
    val newsSummary: String,
    val sentiment: String = "Neutral",
    val timestamp: Long = System.currentTimeMillis(),
    val sheetRowIndex: Int = -1,
    val syncStatus: String = "SUCCESS", // "SUCCESS", "FAILED"
    val emailSubject: String,
    val emailBody: String
)

@Entity(tableName = "sync_errors")
data class SyncError(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val errorMessage: String,
    val errorDetails: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isNotified: Boolean = false
)

@Entity(tableName = "sync_configs")
data class SyncConfig(
    @PrimaryKey val id: Int = 1,
    val userEmail: String = "inflexlabs@gmail.com",
    val googleSheetName: String = "MarketPulse_Log_2026",
    val financialApiKey: String = "demo_key_fmp_mv",
    val isSimulationErrorSheets: Boolean = false,
    val isSimulationErrorEmail: Boolean = false,
    val simulatedInboxCount: Int = 3
)

@Dao
interface StockNewsDao {
    @Query("SELECT * FROM stock_news ORDER BY timestamp DESC")
    fun getAllNewsFlow(): Flow<List<StockNews>>

    @Query("SELECT * FROM stock_news ORDER BY timestamp DESC")
    suspend fun getAllNews(): List<StockNews>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(news: StockNews)

    @Query("DELETE FROM stock_news")
    suspend fun clearNews()
}

@Dao
interface SyncErrorDao {
    @Query("SELECT * FROM sync_errors ORDER BY timestamp DESC")
    fun getAllErrorsFlow(): Flow<List<SyncError>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertError(error: SyncError)

    @Query("DELETE FROM sync_errors")
    suspend fun clearErrors()
}

@Dao
interface SyncConfigDao {
    @Query("SELECT * FROM sync_configs WHERE id = 1")
    fun getConfigFlow(): Flow<SyncConfig?>

    @Query("SELECT * FROM sync_configs WHERE id = 1")
    suspend fun getConfig(): SyncConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: SyncConfig)
}

@Database(entities = [StockNews::class, SyncError::class, SyncConfig::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockNewsDao(): StockNewsDao
    abstract fun syncErrorDao(): SyncErrorDao
    abstract fun syncConfigDao(): SyncConfigDao
}
