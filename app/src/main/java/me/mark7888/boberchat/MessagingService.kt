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

        if (remoteMessage.data.containsKey("message")) {
            MessageHandler.notifyNewMessage(remoteMessage.data)

            // TODO: Show a notification

            return
        }

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
    private var onNewMessageListener: OnNewMessageListener? = null
    fun setOnNewMessageListener(listener: OnNewMessageListener) {
        onNewMessageListener = listener
    }

    interface OnNewMessageListener {
        fun onNewMessage(data : Map<String, String>)
    }

    fun notifyNewMessage(data : Map<String, String>) {
        onNewMessageListener?.onNewMessage(data)
    }

    fun sendMessage(recipientEmail : String, message: String) {
        ConnectionHandler.postRequestJson("/send_message", "{ \"apiKey\" : \"${AuthenticationHandler.getApiKey()}\", \"recipientEmail\" : \"${recipientEmail}\", \"messageType\" : \"text\", \"messageData\" : \"${message}\" }")
        Log.d("MessageHandler", "Message sent to $recipientEmail: $message")
    }
}