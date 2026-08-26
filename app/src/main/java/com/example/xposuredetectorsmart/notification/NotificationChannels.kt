package com.example.xposuredetectorsmart.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import com.example.xposuredetectorsmart.utils.Constants

object NotificationChannels {

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService<NotificationManager>() ?: return

        val alertChannel = NotificationChannel(
            Constants.CHANNEL_ALERTS,
            "Exposure Alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Warnings when cumulative H2S exposure exceeds safe thresholds"
            enableVibration(true)
        }

        val syncChannel = NotificationChannel(
            Constants.CHANNEL_SYNC,
            "Background Sync",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Cloud sync status for dose logs"
        }

        manager.createNotificationChannels(listOf(alertChannel, syncChannel))
    }
}
