package me.mark7888.boberchat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import java.util.regex.Pattern

class NewChatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_chat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val emailInput = findViewById<TextView>(R.id.email_input)
        val chatInput = findViewById<TextView>(R.id.chat_input)
        val sendButton = findViewById<ImageButton>(R.id.send_button)

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

                Thread {
                    val userData =
                        ConnectionHandler.getRequestJson("/get_user?apiKey=${AuthenticationHandler.getApiKey()}&userEmail=${emailText}")
                    val jsonObject = JSONObject(userData)

                    val name = jsonObject.getString("name")
                    val email = jsonObject.getString("email")
                    val picture = jsonObject.getString("picture")

                    runOnUiThread {
                        val intent = Intent(this, ChatActivity::class.java)
                        intent.putExtra("recipientProfilePicture", picture)
                        intent.putExtra("recipientName", name)
                        intent.putExtra("recipientEmail", email)
                        this.startActivity(intent)
                        this.finish()
                    }
                }.start()

            }
        }
    }


}