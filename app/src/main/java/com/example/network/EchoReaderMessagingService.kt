package com.example.network

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class EchoReaderMessagingService : FirebaseMessagingService() {
    
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // TODO: Send token to your server or save in Firestore under the user's document
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        
        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
        }

        // Handle notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            // The system handles showing the notification automatically if app is in background.
            // If in foreground, you can show a custom Toast or local notification here.
        }
    }

    companion object {
        private const val TAG = "EchoReaderFCM"
    }
}
