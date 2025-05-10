package dev.joetul.tao.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.getSystemService

class DoNotDisturbManager(private val context: Context) {

    private val notificationManager = context.getSystemService<NotificationManager>()

    fun hasNotificationPolicyAccess(): Boolean {
        return notificationManager?.isNotificationPolicyAccessGranted ?: false
    }

    fun requestNotificationPolicyAccess() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun enableDoNotDisturb() {
        if (hasNotificationPolicyAccess()) {
            // Use INTERRUPTION_FILTER_PRIORITY instead of INTERRUPTION_FILTER_NONE
            // This allows alarms, media and app sounds to come through
            notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)

            // Set policy to allow our sounds through
            notificationManager?.notificationPolicy = NotificationManager.Policy(
                // Allow alarm, media and system sounds
                NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS or
                        NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA or
                        NotificationManager.Policy.PRIORITY_CATEGORY_SYSTEM,
                NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                NotificationManager.Policy.PRIORITY_SENDERS_ANY
            )
        }
    }

    fun disableDoNotDisturb() {
        if (hasNotificationPolicyAccess()) {
            notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }

    companion object {
        const val DO_NOT_DISTURB_KEY = "do_not_disturb"
    }
}