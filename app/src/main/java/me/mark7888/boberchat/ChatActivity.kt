package me.mark7888.boberchat

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ChatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val recipientEmail = intent.getStringExtra("recipientEmail")
        val recipientName = intent.getStringExtra("recipientName")
        val recipientProfilePicture = intent.getStringExtra("recipientProfilePicture")

        Log.d("ChatActivity", "Recipient email: $recipientEmail")
        Log.d("ChatActivity", "Recipient name: $recipientName")
        Log.d("ChatActivity", "Profile picture: $recipientProfilePicture")
    }
}