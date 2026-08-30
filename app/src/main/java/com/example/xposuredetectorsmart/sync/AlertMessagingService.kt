package com.example.xposuredetectorsmart.sync

import com.example.xposuredetectorsmart.notification.AlertNotificationManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/** Receives FCM pushes for cumulative-exposure alerts triggered server-side or by other devices. */
@AndroidEntryPoint
class AlertMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationManager: AlertNotificationManager

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "H2S Dose Reader"
        val body = message.notification?.body ?: message.data["body"] ?: "New exposure alert"
        notificationManager.showRemoteMessage(title, body)
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM token refreshed: %s", token)
    }
}
