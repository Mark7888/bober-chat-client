package me.mark7888.boberchat

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.io.IOException
import java.net.URL


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

        if (remoteMessage.data["message"]?.isNotEmpty() == true &&
            remoteMessage.data["senderEmail"]?.isNotEmpty() == true &&
            remoteMessage.data["senderName"]?.isNotEmpty() == true &&
            remoteMessage.data["senderPicture"]?.isNotEmpty() == true &&
            remoteMessage.data["messageType"]?.isNotEmpty() == true) {
            MessageHandler.notifyNewMessage(remoteMessage.data)

            val messageSenderName = remoteMessage.data["senderName"] ?: "Unknown"
            var textContent = remoteMessage.data["message"] ?: "Unknown"
            if (remoteMessage.data["messageType"] == "image") {
                textContent = "Sent an image."
            }

            var bitmapImage : Bitmap? = null
            try {
                val url = URL(remoteMessage.data["senderPicture"])
                bitmapImage = BitmapFactory.decodeStream(url.openConnection().getInputStream())
            } catch (e: IOException) {
                println(e)
            }

            // TODO: Show a notification
            val builder = NotificationCompat.Builder(this, "BOBERCHAT")
                .setSmallIcon(R.drawable.ic_stat_group)
                .setLargeIcon(bitmapImage)
                .setContentTitle(messageSenderName)
                .setContentText(textContent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(this)) {
                if (ActivityCompat.checkSelfPermission(
                        this@MessagingService,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@with
                }
                val notificationId = (0..100000).random()
                notify(notificationId, builder.build())
                Log.d("MessagingService", "Notification sent: $notificationId")
            }

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