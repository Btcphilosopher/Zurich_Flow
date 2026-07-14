package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// --- Entities ---

@Entity(tableName = "tickets_passes")
data class TicketPass(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,          // "Single", "Day Pass", "Monthly", "Annual", "SwissPass"
    val title: String,         // e.g., "Zürich Card - Zone 110"
    val price: Double,         // Price in CHF
    val purchaseTime: Long = System.currentTimeMillis(),
    val validUntil: Long,
    val qrCodeData: String,    // Secure SwissPass mock payload
    val isSwissPass: Boolean = false,
    val isGoogleWalletSynced: Boolean = false
)

@Entity(tableName = "favorite_journeys")
data class FavoriteJourney(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fromName: String,
    val toName: String,
    val isStarred: Boolean = true
)

@Entity(tableName = "mobility_logs")
data class MobilityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val fromStation: String,
    val toStation: String,
    val mode: String,          // "Tram", "Bus", "S-Bahn", "Walk", "Cycling", "Ferry", "Cable Car"
    val carbonSavedKg: Double, // in kg
    val distanceKm: Double,    // in km
    val durationMinutes: Int,
    val energyKwh: Double      // energy efficiency in kWh
)

// --- DAOs ---

@Dao
interface ZurichFlowDao {
    // Tickets & Passes
    @Query("SELECT * FROM tickets_passes ORDER BY purchaseTime DESC")
    fun getAllTicketsAndPasses(): Flow<List<TicketPass>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicketPass(ticketPass: TicketPass)

    @Query("DELETE FROM tickets_passes WHERE id = :id")
    suspend fun deleteTicketPass(id: Int)

    // Favorite Journeys
    @Query("SELECT * FROM favorite_journeys")
    fun getFavoriteJourneys(): Flow<List<FavoriteJourney>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteJourney(journey: FavoriteJourney)

    @Query("DELETE FROM favorite_journeys WHERE id = :id")
    suspend fun deleteFavoriteJourney(id: Int)

    @Query("DELETE FROM favorite_journeys WHERE fromName = :fromName AND toName = :toName")
    suspend fun deleteFavoriteJourneyByName(fromName: String, toName: String)

    // Mobility Logs
    @Query("SELECT * FROM mobility_logs ORDER BY timestamp DESC")
    fun getMobilityLogs(): Flow<List<MobilityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMobilityLog(log: MobilityLog)

    @Query("DELETE FROM mobility_logs")
    suspend fun clearAllMobilityLogs()
}

// --- Database ---

@Database(entities = [TicketPass::class, FavoriteJourney::class, MobilityLog::class], version = 1, exportSchema = false)
abstract class ZurichFlowDatabase : RoomDatabase() {
    abstract fun flowDao(): ZurichFlowDao

    companion object {
        @Volatile
        private var INSTANCE: ZurichFlowDatabase? = null

        fun getDatabase(context: Context): ZurichFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZurichFlowDatabase::class.java,
                    "zurich_flow_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- Repository ---

class ZurichFlowRepository(private val dao: ZurichFlowDao) {
    val allTickets: Flow<List<TicketPass>> = dao.getAllTicketsAndPasses()
    val favorites: Flow<List<FavoriteJourney>> = dao.getFavoriteJourneys()
    val mobilityLogs: Flow<List<MobilityLog>> = dao.getMobilityLogs()

    suspend fun buyTicket(ticket: TicketPass) {
        dao.insertTicketPass(ticket)
    }

    suspend fun removeTicket(id: Int) {
        dao.deleteTicketPass(id)
    }

    suspend fun addFavorite(from: String, to: String) {
        dao.insertFavoriteJourney(FavoriteJourney(fromName = from, toName = to))
    }

    suspend fun removeFavorite(id: Int) {
        dao.deleteFavoriteJourney(id)
    }

    suspend fun removeFavoriteByName(from: String, to: String) {
        dao.deleteFavoriteJourneyByName(from, to)
    }

    suspend fun logJourney(log: MobilityLog) {
        dao.insertMobilityLog(log)
    }

    suspend fun clearLogs() {
        dao.clearAllMobilityLogs()
    }
}
