package com.example.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R

/**
 * Posts real local OS notification alerts for genuine local management events
 * when opted in by the user and permitted by the OS (POST_NOTIFICATIONS on Android 13+).
 * Never simulates remote/push delivery.
 */
class LocalAlertNotifier(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "chevalier_management_alerts"
        private const val CHANNEL_NAME = "Management & Governance Alerts"
        private const val CHANNEL_DESC = "Local alerts for approvals, security recovery, and encrypted backups."
    }

    fun hasOsNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureChannelCreated() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = CHANNEL_DESC
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun notifyLocalEventIfPermitted(
        notificationId: Int,
        title: String,
        message: String,
        alertsEnabled: Boolean
    ): Boolean {
        if (!alertsEnabled) return false
        if (!hasOsNotificationPermission()) return false
        return runCatching {
            ensureChannelCreated()
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        }.getOrDefault(false)
    }
}
