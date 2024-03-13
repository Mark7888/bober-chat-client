package me.mark7888.boberchat

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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

        if (remoteMessage.data["message"]?.isNotEmpty() == true &&
            remoteMessage.data["senderEmail"]?.isNotEmpty() == true &&
            remoteMessage.data["senderName"]?.isNotEmpty() == true &&
            remoteMessage.data["senderPicture"]?.isNotEmpty() == true &&
            remoteMessage.data["messageType"]?.isNotEmpty() == true) {
            MessageHandler.notifyNewMessage(remoteMessage.data)

            val messageSenderProfileUrl = remoteMessage.data["senderPicture"] ?: ""
            val messageSenderEmail = remoteMessage.data["senderEmail"] ?: "unknown"
            val messageSenderName = remoteMessage.data["senderName"] ?: "Unknown"
            var textContent = remoteMessage.data["message"] ?: "Unknown"
            if (remoteMessage.data["messageType"] == "image") {
                textContent = "Sent an image."
            }

            val bitmapImage : Bitmap? = BitmapUtils.getBitmapFromURL(messageSenderProfileUrl)

            // Create an explicit intent for an Activity in your app.
            val intent = Intent(this, ChatActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            intent.putExtra("recipientProfilePicture", messageSenderProfileUrl)
            intent.putExtra("recipientName", messageSenderName)
            intent.putExtra("recipientEmail", messageSenderEmail)

            val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            val uniqueRequestCode = System.currentTimeMillis().toInt()
            val pendingIntent: PendingIntent = PendingIntent.getActivity(this, uniqueRequestCode, intent, PendingIntent.FLAG_IMMUTABLE)

            val builder = NotificationCompat.Builder(this, "BOBERCHAT")
                .setSmallIcon(R.drawable.ic_stat_group)
                .setLargeIcon(bitmapImage)
                .setContentTitle(messageSenderName)
                .setContentText(textContent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
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