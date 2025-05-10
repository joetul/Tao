package dev.joetul.tao.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

/**
 * Class to manage session-related preferences using SharedPreferences
 */
class SessionPreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "session_preferences"
        private const val KEY_LAST_SESSION_DURATION_MINUTES = "last_session_duration_minutes"

        // Default values
        const val DEFAULT_DURATION_MINUTES = 15
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // StateFlow to hold the current duration value
    private val _lastSessionDurationMinutes = MutableStateFlow(
        sharedPreferences.getInt(KEY_LAST_SESSION_DURATION_MINUTES, DEFAULT_DURATION_MINUTES)
    )

    /**
     * Get the last set session duration in minutes as a Flow
     */
    val lastSessionDurationMinutes: Flow<Int> = _lastSessionDurationMinutes.asStateFlow()

    /**
     * Save the session duration set by the user
     */
    fun saveSessionDuration(durationMinutes: Int) {
        sharedPreferences.edit {
            putInt(KEY_LAST_SESSION_DURATION_MINUTES, durationMinutes)
        }

        // Update the flow value
        _lastSessionDurationMinutes.value = durationMinutes
    }
}