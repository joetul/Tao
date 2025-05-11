package dev.joetul.tao.data

import android.content.Context
import android.net.Uri
import dev.joetul.tao.MeditationDao
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MeditationRepository(private val meditationDao: MeditationDao) {
    val allSessions: Flow<List<MeditationSession>> = meditationDao.getAllSessionsFlow()
    val sessionCount: Flow<Int> = meditationDao.getSessionCount()
    val totalMeditationTime: Flow<Long?> = meditationDao.getTotalMeditationTime()

    suspend fun insertSession(session: MeditationSession): Long {
        return meditationDao.insertSession(session)
    }

    suspend fun updateSession(session: MeditationSession) {
        meditationDao.updateSession(session)
    }

    suspend fun deleteSession(session: MeditationSession) {
        meditationDao.deleteSession(session)
    }

    suspend fun getSessionById(id: Long): MeditationSession? {
        return meditationDao.getSessionById(id)
    }

    suspend fun exportSessions(context: Context, uri: Uri): Boolean {
        return try {
            val sessions = meditationDao.getAllSessions()
            val jsonArray = JSONArray()
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

            sessions.forEach { session ->
                val jsonObject = JSONObject().apply {
                    put("id", session.id)
                    put("startTime", session.startTime.format(formatter))
                    put("duration", session.duration)
                    put("plannedDuration", session.plannedDuration)
                    put("note", session.note)
                }
                jsonArray.put(jsonObject)
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonArray.toString().toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importSessions(context: Context, uri: Uri): Boolean {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: return false

            val jsonArray = JSONArray(jsonString)
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val session = MeditationSession(
                    id = jsonObject.optLong("id", 0),
                    startTime = LocalDateTime.parse(jsonObject.getString("startTime"), formatter),
                    duration = jsonObject.getLong("duration"),
                    plannedDuration = jsonObject.getLong("plannedDuration"),
                    note = jsonObject.optString("note", "")
                )
                meditationDao.insertSession(session)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Calculate streaks
    suspend fun getStreakData(): StreakData {
        val sessions = meditationDao.getAllSessions()
        if (sessions.isEmpty()) {
            return StreakData(0, 0)
        }

        // Sort sessions by start time
        val sortedSessions = sessions.sortedBy { it.startTime }

        var currentStreak = 0
        var maxStreak = 0
        var previousDate = sortedSessions.first().startTime.toLocalDate()

        sortedSessions.forEach { session ->
            val sessionDate = session.startTime.toLocalDate()

            if (sessionDate == previousDate || sessionDate.minusDays(1) == previousDate) {
                if (sessionDate != previousDate) {
                    // Only increment streak when moving to a new day
                    currentStreak++
                }
            } else {
                // Streak broken
                maxStreak = maxOf(maxStreak, currentStreak)
                currentStreak = 1
            }

            previousDate = sessionDate
        }

        // Check final streak
        maxStreak = maxOf(maxStreak, currentStreak)

        return StreakData(currentStreak, maxStreak)
    }
}

data class StreakData(val currentStreak: Int, val maxStreak: Int)