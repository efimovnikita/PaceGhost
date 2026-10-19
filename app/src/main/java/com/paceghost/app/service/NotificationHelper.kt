package com.paceghost.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.paceghost.app.MainActivity
import com.paceghost.app.R
import com.paceghost.app.domain.engine.PaceCalculator

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "walk_tracking_channel"
        const val CHANNEL_NAME = "PaceGhost Walk Tracking"
        const val NOTIFICATION_ID = 1001
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications about ongoing walk and ghost pacing"
                setShowBadge(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(
        steps: Int,
        distanceMeters: Double,
        paceSecondsPerKm: Double,
        isPaused: Boolean,
        isDemo: Boolean = false
    ): Notification {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val distText = PaceCalculator.formatDistance(distanceMeters)
        val paceText = PaceCalculator.formatPace(paceSecondsPerKm)
        val title = when {
            isPaused -> "PaceGhost: Paused"
            isDemo -> "PaceGhost: Demo Walk"
            else -> "PaceGhost: Active Walk"
        }
        val content = "🚶 $steps steps • $distText • $paceText"

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun updateNotification(
        steps: Int,
        distanceMeters: Double,
        paceSecondsPerKm: Double,
        isPaused: Boolean,
        isDemo: Boolean = false
    ) {
        val notification = buildNotification(steps, distanceMeters, paceSecondsPerKm, isPaused, isDemo)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
