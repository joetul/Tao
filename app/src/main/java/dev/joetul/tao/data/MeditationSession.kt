package dev.joetul.tao.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "meditation_sessions")
data class MeditationSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: LocalDateTime,
    val duration: Long, // Duration in seconds
    val plannedDuration: Long, // Planned duration in seconds
    val note: String = ""
)