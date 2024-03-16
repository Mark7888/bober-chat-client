package me.mark7888.boberchat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatActivity : AppCompatActivity(), MessageHandler.OnNewMessageListener {
    private lateinit var chatList: MutableList<MessageListItem>
    private lateinit var listAdapter: MessageListAdapter
    private lateinit var messagesList: ListView

    private val pickImage = 100
    private var imageUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        if(FirebaseAuth.getInstance().currentUser == null) {
            GoogleSignIn.getClient(this, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()).signOut()
            Firebase.auth.signOut()

            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }

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

        val pickImageButton = findViewById<ImageButton>(R.id.pick_image_button)
        pickImageButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            }

            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
        }

        val profilePicture = findViewById<ImageView>(R.id.profile_pic_image)
        profilePicture.setImageBitmap(profilePictureBitmap)

        val recipientNameText = findViewById<TextView>(R.id.name_text)
        recipientNameText.text = recipientName

        val chatInput = findViewById<EditText>(R.id.message_content_input)
        messagesList = findViewById(R.id.messages_list_view)

        CoroutineScope(Dispatchers.IO).launch {
            val messagesJson =
                ConnectionHandler.getRequestJson("/get_messages?apiKey=${AuthenticationHandler.getApiKey()}&recipientEmail=${recipientEmail}")

            val jsonArray: JsonArray = JsonParser.parseString(messagesJson).asJsonArray

            withContext(Dispatchers.Main) {
                chatList = jsonArray.map { MessageListItem(it.asJsonObject, profilePictureBitmap) }.toMutableList()

                listAdapter = MessageListAdapter(this@ChatActivity, chatList)
                messagesList.adapter = listAdapter
                messagesList.setSelection(listAdapter.count - 1)
            }
        }

        // add copy functionality
        messagesList.setOnItemLongClickListener { _, _, position, _ ->
            val item = listAdapter.getItem(position)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Message Content", item?.messageContent)
            clipboard.setPrimaryClip(clip)
            true
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data

            addMessageToList(MessageListItem(
                messageType = "image",
                messageContent = imageUri.toString(),
                isSent = true,
                profilePicture = BitmapLoader().execute(intent.getStringExtra("recipientProfilePicture")).get(),
                messageId = "0"
            ))

            val recipientEmail = intent.getStringExtra("recipientEmail")
            if (recipientEmail != null) {

                Thread {
                    val response = MessageHandler.sendImage(recipientEmail, imageUri.toString(), this)
                }.start()
            }
        }
    }

    fun addMessageToList(message: MessageListItem) {
        chatList.add(message)
        listAdapter.notifyDataSetChanged()
        // scroll list to bottom
        messagesList.setSelection(listAdapter.count - 1)
    }


    override fun onNewMessage(data: Map<String, String>) {
        Log.d("ChatActivity", "New message received on listener. Data: $data")

        if (data["senderEmail"] != intent.getStringExtra("recipientEmail") || data["senderEmail"] == AuthenticationHandler.authedEmail) {
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

    override fun getItemViewType(position: Int): Int {
        // Return 0 for sent messages, 1 for received messages
        // return 2 for sent images, 3 for received images
        return if (data[position].messageType == "image") {
            if (data[position].isSent) 2 else 3
        } else
        return if (data[position].isSent) 0 else 1
    }

    override fun getViewTypeCount(): Int {
        // We have two types of views
        return 4
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = data[position]


        val layoutId = if (getItemViewType(position) == 0) R.layout.message_item_sender else
            if (getItemViewType(position) == 1) R.layout.message_item_reciever else
                if (getItemViewType(position) == 2) R.layout.message_item_sender_image else
                    R.layout.message_item_reciever_image

        val view = convertView ?: LayoutInflater.from(context).inflate(layoutId, parent, false)

        if (!item.isSent) {
            view.findViewById<ImageView>(R.id.profile_picture)?.setImageBitmap(item.profilePicture)
        }

        if (item.messageType == "image") {
            val imageContent = view.findViewById<ImageView>(R.id.message_image)
            imageContent.loadImage(item.messageContent)
        } else {
            val messageContent = view.findViewById<TextView>(R.id.message_text)

            messageContent.text = item.messageContent
        }

        return view
    }

    fun ImageView.loadImage(url: String) {
        Glide.with(this)
            .load(url)
            .apply(RequestOptions().override(300, 300)) // Optional: resize the image
            .into(this)
    }
}
