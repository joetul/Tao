package dev.joetul.tao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.joetul.tao.data.MeditationSession
import kotlinx.coroutines.flow.Flow

@Dao
interface MeditationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: MeditationSession): Long

    @Update
    suspend fun updateSession(session: MeditationSession)

    @Delete
    suspend fun deleteSession(session: MeditationSession)

    @Query("SELECT * FROM meditation_sessions ORDER BY startTime DESC")
    fun getAllSessionsFlow(): Flow<List<MeditationSession>>

    @Query("SELECT * FROM meditation_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): MeditationSession?

    @Query("SELECT COUNT(*) FROM meditation_sessions")
    fun getSessionCount(): Flow<Int>

    @Query("SELECT SUM(duration) FROM meditation_sessions")
    fun getTotalMeditationTime(): Flow<Long?>

    @Query("SELECT * FROM meditation_sessions ORDER BY startTime DESC")
    suspend fun getAllSessions(): List<MeditationSession>
}