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

        // Group sessions by date to handle multiple sessions per day
        val sessionsByDate = sessions.groupBy { it.startTime.toLocalDate() }
        val sessionDates = sessionsByDate.keys.sorted()

        // Get current date
        val currentDate = LocalDateTime.now().toLocalDate()
        val mostRecentSessionDate = sessionDates.last()

        // Calculate current streak
        var currentStreak = 0

        // Check if the streak is active (session today or yesterday)
        if (mostRecentSessionDate == currentDate || mostRecentSessionDate == currentDate.minusDays(1)) {
            // Start counting from the most recent session date
            var checkDate = mostRecentSessionDate
            var consecutive = true

            while (consecutive) {
                if (sessionsByDate.containsKey(checkDate)) {
                    currentStreak++
                    checkDate = checkDate.minusDays(1)
                } else {
                    consecutive = false
                }
            }
        }

        // Calculate maximum streak from history (independent of current streak)
        var maxStreak = 0
        var tempStreak = 0

        // Scan all dates in order
        for (i in sessionDates.indices) {
            val dateInSequence = sessionDates[i]  // Renamed to avoid shadowing

            // Check if this is the start of a new streak or continuation
            if (i == 0 || dateInSequence != sessionDates[i-1].plusDays(1)) {
                // Reset streak counter for new streak
                tempStreak = 1
            } else {
                // Continue the streak
                tempStreak++
            }

            // Update max streak if current temp streak is larger
            maxStreak = maxOf(maxStreak, tempStreak)
        }

        // Make sure max streak isn't less than current streak
        maxStreak = maxOf(maxStreak, currentStreak)

        return StreakData(currentStreak, maxStreak)
    }
}

data class StreakData(val currentStreak: Int, val maxStreak: Int)