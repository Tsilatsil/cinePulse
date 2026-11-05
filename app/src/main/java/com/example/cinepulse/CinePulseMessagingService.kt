package com.example.cinepulse

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives push notifications from Firebase Cloud Messaging (FCM).
 *  - App in background: Android shows the notification automatically.
 *  - App open: onMessageReceived is called and we show it ourselves.
 */
class CinePulseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "CinePulseFCM"
    }

    /** Firebase gives this phone a new token: save it so the server can reach us. */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        val uid = Session.uid()
        if (uid.isNotEmpty()) Cloud.saveToken(uid, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: ""
        Log.d(TAG, "Push received: $title")
        Notifier.show(this, title, body)
    }
}