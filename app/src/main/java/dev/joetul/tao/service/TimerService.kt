package dev.joetul.tao.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import dev.joetul.tao.MainActivity
import dev.joetul.tao.R
import dev.joetul.tao.model.TimerData
import dev.joetul.tao.model.TimerState
import dev.joetul.tao.util.DoNotDisturbManager
import dev.joetul.tao.viewmodel.JournalViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import androidx.core.content.edit
import dev.joetul.tao.model.MeditationSounds

class TimerService : Service() {
    private val binder = TimerBinder()
    private var countDownTimer: CountDownTimer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null

    // Make these properties public
    var timerDurationMillis: Long = 0
    private var timeLeftMillis: Long = 0
    var sessionStartTime: Long = 0

    // Add direct access to JournalViewModel
    private val journalViewModel by lazy {
        JournalViewModel(application)
    }

    // Add DoNotDisturbManager
    private lateinit var doNotDisturbManager: DoNotDisturbManager

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _remainingTime = MutableStateFlow(TimerData(0, 0, 0))
    val remainingTime: StateFlow<TimerData> = _remainingTime.asStateFlow()

    // SharedPreferences reference for more direct access
    private val sessionPrefs by lazy {
        applicationContext.getSharedPreferences("meditation_session_tracking", MODE_PRIVATE)
    }

    // SharedPreferences for app settings
    private val settingsPrefs by lazy {
        applicationContext.getSharedPreferences("meditation_settings", MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize DND manager
        doNotDisturbManager = DoNotDisturbManager(this)

        createNotificationChannel()
        initMediaPlayer()

        // Check if we should restore a running session
        restorePreviousSession()
    }

    private fun restorePreviousSession() {
        // Try to restore session from SharedPreferences
        val savedStartTime = sessionPrefs.getLong("service_session_start_time", 0)
        val savedTimerDuration = sessionPrefs.getLong("service_timer_duration", 0)

        if (savedStartTime > 0 && savedTimerDuration > 0) {
            // Calculate how much time has passed since the timer started
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - savedStartTime

            // If timer isn't finished yet, restore it
            if (elapsedTime < savedTimerDuration) {
                sessionStartTime = savedStartTime
                timerDurationMillis = savedTimerDuration
                timeLeftMillis = savedTimerDuration - elapsedTime

                // Start the timer service in running state
                _timerState.value = TimerState.RUNNING
                updateRemainingTime(timeLeftMillis)
                startForegroundService()
                startCountdown()

                // Ensure screen stays on if setting is enabled
                handleScreenWakeLock()

                // Ensure DND state is correct after restoration
                ensureDoNotDisturbState()
            } else {
                // Timer would have finished, record the session
                completeExpiredSession(savedStartTime, savedTimerDuration)
            }
        }
    }

    private fun completeExpiredSession(startTime: Long, duration: Long) {
        // Record a session that would have completed while the app was closed
        val sessionDurationSeconds = duration / 1000
        val plannedDurationSeconds = duration / 1000

        // Only process sessions that are at least 30 seconds long
        if (sessionDurationSeconds >= 30) {
            // Convert timestamp to LocalDateTime
            val startDateTime = Instant.ofEpochMilli(startTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()

            // Record the session
            journalViewModel.recordSession(
                durationSeconds = sessionDurationSeconds,
                plannedDurationSeconds = plannedDurationSeconds,
                startTime = startDateTime
            )
        } else {
            // Show a toast for short sessions (less than 30 seconds)
            android.widget.Toast.makeText(
                applicationContext,
                getString(R.string.short_meditation_message),
                android.widget.Toast.LENGTH_LONG // Use LONG duration for better visibility
            ).show()
        }

        // Clear saved session data
        sessionPrefs.edit {
            remove("service_session_start_time")
                .remove("service_timer_duration")
        }
    }

    private fun initMediaPlayer() {
        try {
            // Get the selected sound from preferences
            val sharedPrefs = applicationContext.getSharedPreferences("meditation_settings",
                MODE_PRIVATE
            )
            val soundId = sharedPrefs.getString("meditation_sound", MeditationSounds.DEFAULT.id) ?: MeditationSounds.DEFAULT.id

            // Get the sound object for the selected sound ID
            val selectedSound = MeditationSounds.getSoundById(soundId)

            // Get the resource ID
            val resourceId = selectedSound.resourceId

            mediaPlayer = MediaPlayer().apply {
                // Set audio attributes for better volume control and headphone compatibility
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                // Load the selected sound file
                val descriptor = applicationContext.resources.openRawResourceFd(resourceId)
                setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                descriptor.close()

                prepare()

                // Set a moderate volume level
                setVolume(0.6f, 0.6f)

                // Set up completion listener to reset the player
                setOnCompletionListener {
                    reset()
                    initMediaPlayer()
                }
            }
        } catch (_: Exception) {
            // Silent error handling
        }
    }

    private fun playSound() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                    it.reset()
                    initMediaPlayer()
                }
                it.start()
            }
        } catch (_: Exception) {
            // Try to reinitialize the player if it fails
            try {
                mediaPlayer?.release()
                mediaPlayer = null
                initMediaPlayer()
                mediaPlayer?.start()
            } catch (_: Exception) {
                // Silent error handling
            }
        }
    }

    // New method to ensure Do Not Disturb state matches the timer state
    private fun ensureDoNotDisturbState() {
        // Get the current timer state
        val currentState = _timerState.value

        // Check if Do Not Disturb is enabled in settings
        val sharedPrefs = getSharedPreferences("meditation_settings", MODE_PRIVATE)
        val doNotDisturbEnabled = sharedPrefs.getBoolean(DoNotDisturbManager.DO_NOT_DISTURB_KEY, false)

        if (doNotDisturbEnabled && doNotDisturbManager.hasNotificationPolicyAccess()) {
            when (currentState) {
                TimerState.RUNNING -> doNotDisturbManager.enableDoNotDisturb()
                TimerState.IDLE, TimerState.PAUSED -> doNotDisturbManager.disableDoNotDisturb()
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification(getString(R.string.meditation_notification_title))

        // Proper way to start foreground service with MEDIA_PLAYBACK type on Android 10+
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )

        // Handle the screen wake lock based on current settings
        handleScreenWakeLock()

        // Ensure DND state matches current timer state
        ensureDoNotDisturbState()

        return START_STICKY
    }

    fun startTimer(timerData: TimerData) {
        // Reinitialize media player to load potentially updated sound
        initMediaPlayer()

        // Play sound when starting timer
        playSound()

        timerDurationMillis = timerData.toTotalMillis()
        timeLeftMillis = timerDurationMillis
        sessionStartTime = System.currentTimeMillis()

        // Store both the session start time and duration
        sessionPrefs.edit {
            putLong("service_session_start_time", sessionStartTime)
                .putLong("service_timer_duration", timerDurationMillis)
        }

        // Create wake lock to handle screen on policy
        handleScreenWakeLock()

        _timerState.value = TimerState.RUNNING
        updateRemainingTime(timeLeftMillis)

        // Now that state is updated, ensure DND state
        ensureDoNotDisturbState()

        startForegroundService()
        startCountdown()
    }

    fun stopTimer() {
        // Play sound when stopping timer
        playSound()

        countDownTimer?.cancel()
        releaseWakeLock()

        _timerState.value = TimerState.IDLE

        // Now that state is updated, ensure DND state
        ensureDoNotDisturbState()

        // If the timer was manually stopped, check if session was too short
        // This is helpful when coming back from app being killed
        if (sessionStartTime > 0) {
            val sessionDurationSeconds = (System.currentTimeMillis() - sessionStartTime) / 1000

            // If session was too short, show a toast message
            if (sessionDurationSeconds < 30) {
                android.widget.Toast.makeText(
                    applicationContext,
                    getString(R.string.short_meditation_message),
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            // The actual recording is handled by TimerViewModel which calls this method
        }

        // Clear shared preferences
        sessionPrefs.edit {
            remove("service_session_start_time")
                .remove("service_timer_duration")
        }

        stopForeground(STOP_FOREGROUND_REMOVE)

        stopSelf()
    }

    fun resumeTimer() {
        // Play sound when resuming timer
        playSound()

        // Ensure the wake lock is properly acquired when resuming
        handleScreenWakeLock()

        startCountdown()
        _timerState.value = TimerState.RUNNING

        // Ensure DND state after resuming
        ensureDoNotDisturbState()
    }

    private fun startCountdown() {
        countDownTimer = object : CountDownTimer(timeLeftMillis, COUNTDOWN_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftMillis = millisUntilFinished
                updateRemainingTime(timeLeftMillis)
                updateNotification()
            }

            override fun onFinish() {
                // Play sound when timer finishes
                playSound()

                updateRemainingTime(0)

                // Calculate session duration
                val sessionDurationSeconds = timerDurationMillis / 1000
                val plannedDurationSeconds = timerDurationMillis / 1000

                // Only record sessions that are at least 30 seconds long
                if (sessionDurationSeconds >= 30) {
                    // Convert timestamp to LocalDateTime
                    val startDateTime = Instant.ofEpochMilli(sessionStartTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()

                    // Directly record the session in the journal
                    journalViewModel.recordSession(
                        durationSeconds = sessionDurationSeconds,
                        plannedDurationSeconds = plannedDurationSeconds,
                        startTime = startDateTime
                    )

                    // Also send a broadcast for backward compatibility
                    val completionIntent = Intent(ACTION_TIMER_COMPLETED).apply {
                        putExtra(EXTRA_SESSION_DURATION, sessionDurationSeconds)
                        putExtra(EXTRA_PLANNED_DURATION, plannedDurationSeconds)
                        putExtra(EXTRA_SESSION_START_TIME, sessionStartTime / 1000) // Convert to seconds
                    }
                    sendBroadcast(completionIntent)
                } else {
                    // Show a toast message for sessions under 30 seconds
                    android.widget.Toast.makeText(
                        applicationContext,
                        getString(R.string.short_meditation_message),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }

                // Clear the session tracking data
                sessionPrefs.edit {
                    remove("service_session_start_time")
                        .remove("service_timer_duration")
                }

                _timerState.value = TimerState.IDLE

                // Ensure DND state is updated after timer completes
                ensureDoNotDisturbState()

                releaseWakeLock()

                stopForeground(STOP_FOREGROUND_REMOVE)

                // Show a completion notification
                showCompletionNotification()

                // Don't stop the service immediately to allow sound to play
                postStopSelf()
            }
        }.start()
    }

    private fun postStopSelf() {
        // Delay stopping the service to allow the sound to play completely
        android.os.Handler(mainLooper).postDelayed({
            stopSelf()
        }, 3000) // 3 seconds delay
    }

    private fun showCompletionNotification() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, COMPLETED_CHANNEL_ID)
            .setContentTitle(getString(R.string.meditation_complete_title))
            .setContentText(getString(R.string.meditation_complete_text))
            .setSmallIcon(R.drawable.ic_timer_complete_notification)
            .setContentIntent(pendingIntent)
            .setSilent(true) // Ensure notification is silent
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    private fun updateRemainingTime(millisLeft: Long) {
        val hours = (millisLeft / (1000 * 60 * 60)).toInt()
        val minutes = ((millisLeft % (1000 * 60 * 60)) / (1000 * 60)).toInt()
        val seconds = ((millisLeft % (1000 * 60)) / 1000).toInt()

        _remainingTime.value = TimerData(hours, minutes, seconds)
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Create channel for in-progress meditation timer
        val timerChannel = NotificationChannel(
            TIMER_CHANNEL_ID,
            getString(R.string.meditation_timer_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.meditation_timer_channel_description)
        }

        // Create channel for completed meditation notifications
        val completedChannel = NotificationChannel(
            COMPLETED_CHANNEL_ID,
            getString(R.string.meditation_completed_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.meditation_completed_channel_description)
            setSound(null, null) // Ensure no sound by default
            enableVibration(false) // Disable vibration
        }

        // Register both channels
        notificationManager.createNotificationChannel(timerChannel)
        notificationManager.createNotificationChannel(completedChannel)
    }

    private fun createNotification(contentText: String): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, TIMER_CHANNEL_ID)
            .setContentTitle(getString(R.string.meditation_timer_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification() {
        val hours = _remainingTime.value.hours
        val minutes = _remainingTime.value.minutes
        val seconds = _remainingTime.value.seconds

        val timeString = when {
            hours > 0 -> String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }

        val notification = createNotification(getString(R.string.meditation_progress_prefix) + " $timeString")
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startForegroundService() {
        val notification = createNotification(getString(R.string.meditation_notification_title))

        // Proper way to start foreground service with MEDIA_PLAYBACK type on Android 10+
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
    }

    // New implementation of screen wake lock handling using modern APIs
    private fun handleScreenWakeLock() {
        // First release any existing wake lock to prevent leaks
        releaseWakeLock()

        try {
            val keepScreenOn = settingsPrefs.getBoolean("keep_screen_on", true)

            if (keepScreenOn && _timerState.value == TimerState.RUNNING) {
                val powerManager = getSystemService(POWER_SERVICE) as PowerManager

                // Use FULL_WAKE_LOCK to keep screen on (modern approach)
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "Tao:MeditationWakeLock"
                )

                // Set a timeout slightly longer than the timer duration
                wakeLock?.acquire(timerDurationMillis + 10000) // Add a 10-second buffer

                // Note: The actual screen-on functionality is handled at the Activity level
                // by setting the FLAG_KEEP_SCREEN_ON flag in the window
            }
        } catch (e: SecurityException) {
            android.util.Log.e("TimerService", "Security exception acquiring wake lock: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Error acquiring wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Error releasing wake lock: ${e.message}")
        }
    }

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        releaseWakeLock()

        // Release media player resources
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {
            // Silent error handling
        }

        // Ensure Do Not Disturb is disabled when service is destroyed
        if (_timerState.value != TimerState.RUNNING) {
            ensureDoNotDisturbState()
        }

        super.onDestroy()
    }

    companion object {
        // Notification channel IDs
        private const val TIMER_CHANNEL_ID = "meditation_timer_channel"
        private const val COMPLETED_CHANNEL_ID = "meditation_completed_channel"

        // Notification IDs
        private const val NOTIFICATION_ID = 1001
        private const val COMPLETION_NOTIFICATION_ID = 1002

        private const val COUNTDOWN_INTERVAL = 1000L // Update every second

        // Add constants for broadcast intents - these are needed for the TimerViewModel
        const val ACTION_TIMER_COMPLETED = "dev.joetul.tao.ACTION_TIMER_COMPLETED"
        const val EXTRA_SESSION_DURATION = "dev.joetul.tao.EXTRA_SESSION_DURATION"
        const val EXTRA_PLANNED_DURATION = "dev.joetul.tao.EXTRA_PLANNED_DURATION"
        const val EXTRA_SESSION_START_TIME = "dev.joetul.tao.EXTRA_SESSION_START_TIME"
    }
}

// Extension function to convert TimerData to total milliseconds
fun TimerData.toTotalMillis(): Long {
    return (hours * 60 * 60 * 1000 + minutes * 60 * 1000 + seconds * 1000).toLong()
}