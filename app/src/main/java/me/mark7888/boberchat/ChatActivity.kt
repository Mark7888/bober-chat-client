package me.mark7888.boberchat

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity(), MessageHandler.OnNewMessageListener {
    private lateinit var chatList: MutableList<MessageListItem>
    private lateinit var listAdapter: MessageListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        MessageHandler.setOnNewMessageListener(this)

        val recipientEmail = intent.getStringExtra("recipientEmail")
        val recipientName = intent.getStringExtra("recipientName")
        val recipientProfilePicture = intent.getStringExtra("recipientProfilePicture")
        val profilePictureBitmap = BitmapLoader().execute(recipientProfilePicture).get()

        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val sendMessageButton = findViewById<ImageButton>(R.id.send_message_button)
        sendMessageButton.setOnClickListener {
            val chatInput = findViewById<EditText>(R.id.message_content_input)
            val messageContent = chatInput.text.toString()
            chatInput.text.clear()

            if (recipientEmail != null && messageContent.isNotEmpty()) {
                MessageHandler.sendMessage(recipientEmail, messageContent)

                // add message to list
                val message = MessageListItem(
                    messageType = "text",
                    messageContent = messageContent,
                    isSent = true,
                    profilePicture = profilePictureBitmap,
                    messageId = "0"
                )
                addMessageToList(message)
            }
        }

        val profilePicture = findViewById<ImageView>(R.id.profile_pic_image)
        profilePicture.setImageBitmap(profilePictureBitmap)

        val recipientNameText = findViewById<TextView>(R.id.name_text)
        recipientNameText.text = recipientName

        val chatInput = findViewById<EditText>(R.id.message_content_input)
        val messagesList = findViewById<ListView>(R.id.messages_list_view)

        CoroutineScope(Dispatchers.IO).launch {
            val messagesJson =
                ConnectionHandler.getRequestJson("/get_messages?apiKey=${AuthenticationHandler.getApiKey()}&recipientEmail=${recipientEmail}")

            val jsonArray: JsonArray = JsonParser.parseString(messagesJson).asJsonArray

            withContext(Dispatchers.Main) {
                chatList = jsonArray.map { MessageListItem(it.asJsonObject, profilePictureBitmap) }.toMutableList()

                listAdapter = MessageListAdapter(this@ChatActivity, chatList)
                messagesList.adapter = listAdapter
            }
        }

    }

    fun addMessageToList(message: MessageListItem) {
        chatList.add(message)
        listAdapter.notifyDataSetChanged()
    }


    override fun onNewMessage(data: Map<String, String>) {
        if (data["senderEmail"] != intent.getStringExtra("recipientEmail")) {
            return
        }

        runOnUiThread(Runnable {
            val message = MessageListItem(
                messageType = data["messageType"] ?: "text",
                messageContent = data["message"] ?: "",
                isSent = false,
                profilePicture = BitmapLoader().execute(data["senderPicture"]).get(),
                messageId = data["id"] ?: "0"
            )

            addMessageToList(message)
        })

        Log.d("ChatActivity", "New message received on listener!!!!: $data")
    }
}



data class MessageListItem(
    val messageType : String,
    val messageContent : String,
    val isSent : Boolean,
    val profilePicture : Bitmap,
    val messageId : String
) {
    constructor(json: JsonObject, picture: Bitmap) : this(
        messageContent = json.get("message").asString,
        messageType = json.get("message_type").asString,
        isSent = json.get("is_sent").asBoolean,
        profilePicture = picture,
        messageId = json.get("id").asString
    )
}

class MessageListAdapter(context: Context, private val data: List<MessageListItem>) :
    ArrayAdapter<MessageListItem>(context, R.layout.message_item_sender, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = data[position]

        var layoutId = R.layout.message_item_sender
        if (!item.isSent) {
            layoutId = R.layout.message_item_reciever
        }

        val view = convertView ?: LayoutInflater.from(context).inflate(layoutId, parent, false)

        if (!item.isSent) {
            val profilePicture = view.findViewById<ImageView>(R.id.profile_picture)
            profilePicture.setImageBitmap(item.profilePicture)
        }

        val messageContent = view.findViewById<TextView>(R.id.message_text)
        messageContent.text = item.messageContent

        return view
    }
}