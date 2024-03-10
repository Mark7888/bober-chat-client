package me.mark7888.boberchat

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Log.d("MessagingService", "New token: $token")

        super.onNewToken(token)

        // Set the messaging token in the AuthenticationHandler
        AuthenticationHandler.setMessagingToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("MessagingService", "Message received: ${remoteMessage.data}")

        if (remoteMessage.data.containsKey("auth_ack") && remoteMessage.data["auth_ack"] == "true") {
            // The server has acknowledged the authentication
            AuthenticationHandler.acknowledge(remoteMessage.data)
            return
        }

        //
        // TODO: Handle the message
        //

        super.onMessageReceived(remoteMessage)
    }

    companion object {
        fun tokenRequest() {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("MessagingService", "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result

                // Set the messaging token in the AuthenticationHandler
                AuthenticationHandler.setMessagingToken(token)
            })
        }
    }
}

object MessageHandler {
    fun sendMessage(recipientEmail : String, message: String) {
        ConnectionHandler.postRequestJson("/send_message", "{ \"api_key\" : \"${AuthenticationHandler.getApiKey()}\", \"recipientEmail\" : \"${recipientEmail}\", \"messageType\" : \"text\", \"messageData\" : \"${message}\" }")
    }
}