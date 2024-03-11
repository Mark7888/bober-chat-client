package me.mark7888.boberchat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.regex.Pattern

class NewChatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_chat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val emailInput = findViewById<TextView>(R.id.email_input)
        val chatInput = findViewById<TextView>(R.id.chat_input)
        val sendButton = findViewById<Button>(R.id.send_button)

        sendButton.setOnClickListener {
            var emailText = emailInput.text.toString()
            val chatText = chatInput.text.toString()

            if (emailText.isNotEmpty() && chatText.isNotEmpty()) {
                val emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$")
                val matcher = emailPattern.matcher(emailText)

                if (!matcher.matches()) {
                    emailText += "@gmail.com"
                }

                if (AuthenticationHandler.getApiKey().isEmpty()) {
                    Log.e("MainActivity", "Api key is empty!")
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }

                // send post message to server
                MessageHandler.sendMessage(emailText, chatText)

                Log.d("NewChatActivity", "Email: $emailText, Message: $chatText")
            }
        }
    }


}