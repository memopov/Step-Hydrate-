package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val CHANNEL_ID = "step_hydrate_alerts"
    private const val CHANNEL_NAME = "Step & Hydrate Updates"
    private const val CHANNEL_DESC = "Notifications for step goals, dynamic GPS speed alerts, and water reminders"
    private var isChannelInitialized = false

    fun initChannel(context: Context) {
        if (isChannelInitialized) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
        isChannelInitialized = true
    }

    /**
     * Smart Notification Manager:
     * Respects DND hours (10 PM to 8 AM) unless user explicitly bypasses.
     */
    fun showGoalNotification(
        context: Context,
        title: String,
        body: String,
        bypassDnd: Boolean = false
    ) {
        initChannel(context)

        if (!bypassDnd) {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            // Silent between 22:00 and 08:00
            if (hour < 8 || hour >= 22) {
                // Return silently to respect user's rest (DND Mode active)
                return
            }
        }

        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
