package me.mark7888.boberchat

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
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
        val profilePictureBitmap = BitmapLoader().execute(recipientProfilePicture).get()

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
            val chatList: List<MessageListItem> = jsonArray.map { MessageListItem(it.asJsonObject, profilePictureBitmap) }

            withContext(Dispatchers.Main) {
                val adapter = MessageListAdapter(this@ChatActivity, chatList)
                messagesList.adapter = adapter
            }
        }

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

        // Choose the layout based on the isSent property
        val layoutId = if (item.isSent) R.layout.message_item_sender else R.layout.message_item_reciever

        val view = convertView ?: LayoutInflater.from(context).inflate(layoutId, parent, false)

        val profilePicture = view.findViewById<ImageView>(R.id.profile_picture)
        val messageContent = view.findViewById<TextView>(R.id.message_text)

        profilePicture.setImageBitmap(item.profilePicture)
        messageContent.text = item.messageContent

        return view
    }
}