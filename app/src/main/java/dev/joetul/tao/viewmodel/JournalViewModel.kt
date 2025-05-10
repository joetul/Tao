package dev.joetul.tao.viewmodel

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.joetul.tao.data.AppDatabase
import dev.joetul.tao.data.MeditationRepository
import dev.joetul.tao.data.MeditationSession
import dev.joetul.tao.data.StreakData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class JournalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MeditationRepository
    val allSessions: Flow<List<MeditationSession>>
    val sessionCount: Flow<Int>
    val totalMeditationTime: Flow<Long?>

    private val _streakData = MutableStateFlow(StreakData(0, 0))
    val streakData: StateFlow<StreakData> = _streakData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Add ImportExportState for communication with UI
    private val _importExportState = MutableStateFlow<ImportExportState>(ImportExportState.Idle)

    init {
        val database = AppDatabase.getDatabase(application)
        val meditationDao = database.meditationDao()
        repository = MeditationRepository(meditationDao)

        allSessions = repository.allSessions
        sessionCount = repository.sessionCount
        totalMeditationTime = repository.totalMeditationTime

        updateStreakData()
    }

    fun updateNote(sessionId: Long, note: String) {
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            session?.let {
                repository.updateSession(it.copy(note = note))
            }
        }
    }

    fun deleteSession(session: MeditationSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
            updateStreakData()
        }
    }

    fun recordSession(durationSeconds: Long, plannedDurationSeconds: Long, startTime: LocalDateTime = LocalDateTime.now()) {
        viewModelScope.launch {
            val session = MeditationSession(
                startTime = startTime,
                duration = durationSeconds,
                plannedDuration = plannedDurationSeconds
            )
            repository.insertSession(session)
            updateStreakData()
        }
    }

    private fun updateStreakData() {
        viewModelScope.launch {
            _streakData.value = repository.getStreakData()
        }
    }

    // Add Toast notifications
    private fun showToast(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _importExportState.value = ImportExportState.Loading

            val success = repository.exportSessions(getApplication(), uri)

            if (success) {
                _importExportState.value = ImportExportState.Success("Journal data exported successfully")
                showToast("Journal data exported successfully")
            } else {
                _importExportState.value = ImportExportState.Error("Failed to export journal data")
                showToast("Failed to export journal data")
            }

            _isLoading.value = false
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _importExportState.value = ImportExportState.Loading

            val success = repository.importSessions(getApplication(), uri)

            if (success) {
                _importExportState.value = ImportExportState.Success("Journal data imported successfully")
                showToast("Journal data imported successfully")
            } else {
                _importExportState.value = ImportExportState.Error("Failed to import journal data")
                showToast("Failed to import journal data")
            }

            updateStreakData()
            _isLoading.value = false
        }
    }
}