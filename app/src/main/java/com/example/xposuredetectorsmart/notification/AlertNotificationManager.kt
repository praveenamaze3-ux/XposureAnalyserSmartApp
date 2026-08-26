package com.example.xposuredetectorsmart.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.xposuredetectorsmart.R
import com.example.xposuredetectorsmart.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AlertNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun showExposureAlert(workerId: String, cumulativePpm: Double) {
        if (!hasPostPermission()) return

        val title = if (cumulativePpm >= Constants.IDLH_PPM) {
            "CRITICAL: H2S exposure limit exceeded"
        } else {
            "Warning: approaching H2S exposure limit"
        }
        val text = "Worker $workerId cumulative exposure is %.1f ppm this shift.".format(cumulativePpm)

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(Constants.NOTIFICATION_ID_ALERT, notification)
    }

    fun showRemoteMessage(title: String, body: String) {
        if (!hasPostPermission()) return

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(Constants.NOTIFICATION_ID_ALERT, notification)
    }

    private fun hasPostPermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
