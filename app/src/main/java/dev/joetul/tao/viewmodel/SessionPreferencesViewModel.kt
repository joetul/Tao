package dev.joetul.tao.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.joetul.tao.data.SessionPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SessionPreferencesViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionPreferences = SessionPreferences(application)

    // Expose the last session duration as StateFlow that can be collected by composables
    val lastSessionDurationMinutes: StateFlow<Int> = sessionPreferences.lastSessionDurationMinutes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SessionPreferences.DEFAULT_DURATION_MINUTES
        )

    // Function to save the session duration
    fun saveSessionDuration(durationMinutes: Int) {
        sessionPreferences.saveSessionDuration(durationMinutes)
    }
}