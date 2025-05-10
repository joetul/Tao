package dev.joetul.tao.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.joetul.tao.model.TimerData
import dev.joetul.tao.model.TimerState
import dev.joetul.tao.service.TimerService
import dev.joetul.tao.util.BatteryOptimizationHelper
import dev.joetul.tao.util.DoNotDisturbManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import androidx.core.content.edit
import java.lang.ref.WeakReference

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private var timerService: WeakReference<TimerService>? = null
    private var bound = false

    // Added properties
    private val journalViewModel = JournalViewModel(application)
    private var sessionStartTime: Long = 0
    private var plannedDurationSeconds: Long = 0

    // Shared Preferences for timer settings
    private val sharedPrefs = getApplication<Application>().getSharedPreferences("meditation_settings", Context.MODE_PRIVATE)

    // Session tracking shared preferences
    private val sessionPrefs = getApplication<Application>().getSharedPreferences("meditation_session_tracking", Context.MODE_PRIVATE)

    // Add Battery Optimization Helper
    private val batteryOptimizationHelper = BatteryOptimizationHelper(getApplication())

    // Add pending action for when battery permission is granted
    private var pendingStartTimer = false

    // Add DoNotDisturbManager
    private val doNotDisturbManager = DoNotDisturbManager(getApplication())

    // For UI to observe battery optimization state
    private val _batteryOptimizationRequired = MutableStateFlow(false)
    val batteryOptimizationRequired: StateFlow<Boolean> = _batteryOptimizationRequired.asStateFlow()

    // Timer completion receiver
    private val timerCompletionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == TimerService.ACTION_TIMER_COMPLETED) {
                val sessionDuration = intent.getLongExtra(TimerService.EXTRA_SESSION_DURATION, 0)

                // Only process sessions that are at least 30 seconds long
                if (sessionDuration >= 30) {
                    val plannedDuration = intent.getLongExtra(TimerService.EXTRA_PLANNED_DURATION, 0)
                    val startTimeSeconds = intent.getLongExtra(
                        TimerService.EXTRA_SESSION_START_TIME,
                        System.currentTimeMillis() / 1000 - sessionDuration
                    )

                    // Convert Unix timestamp (seconds) to LocalDateTime
                    val startDateTime = Instant.ofEpochSecond(startTimeSeconds)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()

                    // Record the completed session with the correct LocalDateTime
                    journalViewModel.recordSession(
                        durationSeconds = sessionDuration,
                        plannedDurationSeconds = plannedDuration,
                        startTime = startDateTime
                    )
                }

                // Clear session tracking data when timer completes
                clearSessionTrackingInfo()
            } else if (intent.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED ||
                intent.action == PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED) {
                // Refresh battery optimization state when power settings change
                checkBatteryOptimizationStatus()
            }
        }
    }

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    // Load saved timer duration or use default
    private val _timerData = MutableStateFlow(loadTimerData())
    val timerData: StateFlow<TimerData> = _timerData.asStateFlow()

    private val _currentTime = MutableStateFlow(loadTimerData())
    val currentTime: StateFlow<TimerData> = _currentTime.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.TimerBinder
            timerService = WeakReference(binder.getService())
            bound = true

            // Start observing service state
            viewModelScope.launch {
                timerService?.get()?.timerState?.collect { state ->
                    _timerState.value = state

                    // Handle Do Not Disturb based on timer state
                    handleDoNotDisturbForState(state)
                }
            }

            viewModelScope.launch {
                timerService?.get()?.remainingTime?.collect { time ->
                    _currentTime.value = time
                }
            }

            // Refresh session info from the service
            viewModelScope.launch {
                restoreSessionInfo()
            }

            // Check if there was a pending start timer request
            if (pendingStartTimer) {
                pendingStartTimer = false
                actuallyStartTimer()
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
            timerService = null
        }
    }

    init {
        // Register for timer completion broadcasts
        val filter = IntentFilter().apply {
            addAction(TimerService.ACTION_TIMER_COMPLETED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
        }

        // Always use RECEIVER_NOT_EXPORTED flag
        getApplication<Application>().registerReceiver(
            timerCompletionReceiver,
            filter,
            Context.RECEIVER_NOT_EXPORTED
        )

        // Initial check for battery optimization status
        checkBatteryOptimizationStatus()

        // Try to restore session info from preferences
        restoreSessionInfo()

        // Bind to the timer service
        bindTimerService()
    }

    /**
     * Restore session information from SharedPreferences
     */
    private fun restoreSessionInfo() {
        // Try to get from service first if it's running
        timerService?.get()?.let { service ->
            if (service.timerState.value == TimerState.RUNNING && service.sessionStartTime > 0) {
                sessionStartTime = service.sessionStartTime / 1000 // Convert to seconds if stored as millis
                plannedDurationSeconds = service.timerDurationMillis / 1000
                return // Successfully restored from service
            }
        }

        // If service doesn't have the info or isn't connected, try to restore from SharedPreferences
        val savedStartTimeMillis = sessionPrefs.getLong("service_session_start_time", 0)
        if (savedStartTimeMillis > 0) {
            sessionStartTime = savedStartTimeMillis / 1000 // Convert from millis to seconds
            plannedDurationSeconds = sessionPrefs.getLong("service_timer_duration", 0) / 1000
        }
    }

    /**
     * Clear session tracking information
     */
    private fun clearSessionTrackingInfo() {
        sessionPrefs.edit {
            remove("service_session_start_time")
            remove("service_timer_duration")
            remove("session_start_time")
            remove("planned_duration_seconds")
        }
        sessionStartTime = 0
        plannedDurationSeconds = 0
    }

    private fun checkBatteryOptimizationStatus() {
        _batteryOptimizationRequired.value = !batteryOptimizationHelper.isIgnoringBatteryOptimizations()
    }

    /**
     * Request to be excluded from battery optimizations
     * @return True if permission is already granted, false if it needs to be requested
     */
    fun requestBatteryOptimizationExemption(): Boolean {
        if (batteryOptimizationHelper.isIgnoringBatteryOptimizations()) {
            return true
        }

        batteryOptimizationHelper.requestIgnoreBatteryOptimizations()
        return false
    }

    private fun loadTimerData(): TimerData {
        // Load timer duration from SharedPreferences
        val hours = sharedPrefs.getInt("timer_hours", 0)
        val minutes = sharedPrefs.getInt("timer_minutes", 10) // Default to 10 minutes
        val seconds = sharedPrefs.getInt("timer_seconds", 0)
        return TimerData(hours, minutes, seconds)
    }

    private fun saveTimerData(timerData: TimerData) {
        // Save timer duration to SharedPreferences
        sharedPrefs.edit {
            putInt("timer_hours", timerData.hours)
            putInt("timer_minutes", timerData.minutes)
            putInt("timer_seconds", timerData.seconds)
        }
    }

    private fun bindTimerService() {
        Intent(getApplication(), TimerService::class.java).also { intent ->
            getApplication<Application>().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun startTimerService() {
        Intent(getApplication(), TimerService::class.java).also { intent ->
            getApplication<Application>().startService(intent)
        }
    }

    fun updateTimerData(hours: Int, minutes: Int, seconds: Int) {
        val newTimerData = TimerData(hours, minutes, seconds)
        _timerData.value = newTimerData
        _currentTime.value = newTimerData

        // Save the new timer data to SharedPreferences
        saveTimerData(newTimerData)
    }

    private fun handleDoNotDisturbForState(state: TimerState) {
        val doNotDisturbEnabled = sharedPrefs.getBoolean(DoNotDisturbManager.DO_NOT_DISTURB_KEY, false)

        if (doNotDisturbEnabled) {
            when (state) {
                TimerState.RUNNING -> {
                    // Enable Do Not Disturb when timer is running
                    if (doNotDisturbManager.hasNotificationPolicyAccess()) {
                        doNotDisturbManager.enableDoNotDisturb()
                    }
                }
                TimerState.IDLE, TimerState.PAUSED -> {
                    // Disable Do Not Disturb when timer is not running
                    if (doNotDisturbManager.hasNotificationPolicyAccess()) {
                        doNotDisturbManager.disableDoNotDisturb()
                    }
                }
            }
        }
    }

    fun toggleTimer() {
        when (_timerState.value) {
            TimerState.IDLE -> {
                // Check if battery optimization exemption is needed
                if (_batteryOptimizationRequired.value) {
                    // Request exemption first
                    pendingStartTimer = true
                    requestBatteryOptimizationExemption()
                } else {
                    // We already have exemption, proceed with starting timer
                    actuallyStartTimer()
                }
            }
            TimerState.RUNNING -> {
                // When stopping a running timer, check if we need to get the session info
                // from the service or shared preferences
                if (sessionStartTime == 0L) {
                    restoreSessionInfo()
                }

                // Calculate the session duration using our saved sessionStartTime
                val currentTime = System.currentTimeMillis() / 1000

                // Make sure we have a valid session start time
                if (sessionStartTime <= 0) {
                    // If sessionStartTime is invalid, get from shared preferences as fallback
                    val savedStartTimeMillis = sessionPrefs.getLong("service_session_start_time", 0)
                    if (savedStartTimeMillis > 0) {
                        sessionStartTime = savedStartTimeMillis / 1000
                    } else {
                        // If still no valid start time, use a reasonable fallback
                        // This shouldn't happen but prevents the Jan 1, 1970 issue
                        sessionStartTime = currentTime - 60 // Assume at least a 1-minute session
                        android.widget.Toast.makeText(
                            getApplication(),
                            "Session time data was lost; approximate duration recorded",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }

                val sessionDurationSeconds = currentTime - sessionStartTime

                // Only record sessions that are at least 30 seconds long
                if (sessionDurationSeconds >= 30) {
                    // Convert Unix timestamp to LocalDateTime for manual stop
                    val startDateTime = Instant.ofEpochSecond(sessionStartTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()

                    // Record the completed session
                    journalViewModel.recordSession(
                        durationSeconds = sessionDurationSeconds,
                        plannedDurationSeconds = plannedDurationSeconds,
                        startTime = startDateTime
                    )
                } else {
                    // Always show a toast message for sessions under 30 seconds
                    android.widget.Toast.makeText(
                        getApplication(),
                        "Meditation sessions under 30 seconds are not recorded",
                        android.widget.Toast.LENGTH_LONG // Use LONG duration for better visibility
                    ).show()
                }

                // Clear the session tracking info
                clearSessionTrackingInfo()

                timerService?.get()?.stopTimer()
            }
            TimerState.PAUSED -> {
                timerService?.get()?.resumeTimer()
            }
        }
    }

    // This is called after battery optimization permission is granted
    private fun actuallyStartTimer() {
        startTimerService()
        sessionStartTime = System.currentTimeMillis() / 1000
        plannedDurationSeconds = _timerData.value.toTotalSeconds()

        // Note: The service will now handle saving session data to SharedPreferences

        timerService?.get()?.startTimer(_timerData.value)

        // Refresh battery optimization status
        checkBatteryOptimizationStatus()
    }

    // Call this when returning to the app - it will check if we need to start the timer
    fun checkPendingActions() {
        // Check if the app now has battery optimization exemption
        checkBatteryOptimizationStatus()

        // If we were trying to start a timer and now have exemption, proceed
        if (pendingStartTimer && !_batteryOptimizationRequired.value) {
            pendingStartTimer = false
            actuallyStartTimer()
        }

        // Always attempt to restore session info when the app returns to foreground
        restoreSessionInfo()
    }

    override fun onCleared() {
        // Unregister the broadcast receiver
        try {
            getApplication<Application>().unregisterReceiver(timerCompletionReceiver)
        } catch (e: Exception) {
            // Ignore if already unregistered
        }

        // Ensure Do Not Disturb is disabled when ViewModel is cleared
        val doNotDisturbEnabled = sharedPrefs.getBoolean(DoNotDisturbManager.DO_NOT_DISTURB_KEY, false)

        if (doNotDisturbEnabled && doNotDisturbManager.hasNotificationPolicyAccess()) {
            doNotDisturbManager.disableDoNotDisturb()
        }

        // Unbind from the service if still bound
        if (bound) {
            getApplication<Application>().unbindService(connection)
            bound = false
        }

        // Set timerService to null to prevent leaks
        timerService = null

        super.onCleared()
    }
}