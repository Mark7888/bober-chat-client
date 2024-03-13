package me.mark7888.boberchat

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.SearchView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity(), AuthenticationHandler.OnChatsUpdateListener {
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var chatListAdapter: ChatListAdapter
    private var chatList: List<ChatListItem> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout.setOnRefreshListener {
            loadChats()
        }

        AuthenticationHandler.setOnChatUpdateListener(this)

        mAuth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        val auth = Firebase.auth
        val user = auth.currentUser

        val profilePictureImage = findViewById<ImageView>(R.id.profile_picture_image)

        // Set the profile picture to the user's profile picture
        if (user != null) {
            AuthenticationHandler.authedEmail = user.email ?: ""

            authenticateUser()

            val profilePicUrl = user.photoUrl

            try {
                if (profilePicUrl != null) {

                    val downloadImageTask = BitmapLoader()
                    downloadImageTask.execute(profilePicUrl.toString())
                    val bitmap = downloadImageTask.get()

                    profilePictureImage.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()

                // Set the image to a default image
                profilePictureImage.setImageResource(R.drawable.dummy_profile_pic)
            }

        } else {
            // Handle the case where the user is not signed in
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        // move to LoginActivity when profile picture is clicked
        profilePictureImage.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // the button new_chat_button redirects to newchatactivity
        val newChatButton = findViewById<ImageButton>(R.id.new_chat_button)

        newChatButton.setOnClickListener {
            val intent = Intent(this, NewChatActivity::class.java)
            startActivity(intent)
        }

        val chatSearch = findViewById<SearchView>(R.id.chat_search)
        chatSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterChats(newText ?: "")
                return false
            }
        })

        // check if notification permission, request permission
        checkNotificationPermission()
    }

    override fun onChatsUpdate() {
        loadChats()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = "BoberChat"
        val descriptionText = "BoberChat notifications"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("BOBERCHAT", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }


    private fun loadChats() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                val chatsLoadingText = findViewById<TextView>(R.id.chats_loading_text)
                chatsLoadingText.text = "Loading chats..."
            }

            val apiKey = AuthenticationHandler.getApiKey()

            if (apiKey.isEmpty()) {
                Log.e("MainActivity", "Api key is empty!")
                // The user is not authenticated
                return@launch
            }

            val chats = ConnectionHandler.getRequestJson("/get_chats?apiKey=${AuthenticationHandler.getApiKey()}")

            Log.d("MainActivity", "Chats: '$chats'")

            val jsonArray: JsonArray = JsonParser.parseString(chats).asJsonArray
            chatList = jsonArray.map { ChatListItem(it.asJsonObject) }

            // Switch to the main thread to update the UI
            withContext(Dispatchers.Main) {
                val chatSelectList = findViewById<ListView>(R.id.chat_select_list)
                val chatsLoadingText = findViewById<TextView>(R.id.chats_loading_text)
                chatsLoadingText.text = "Loading chats..."

                chatListAdapter = ChatListAdapter(this@MainActivity, chatList)
                chatSelectList.adapter = chatListAdapter

                if (chatList.isEmpty()) {
                    chatsLoadingText.text = "No chats found"
                } else {
                    chatsLoadingText.text = ""
                    chatsLoadingText.textSize = 0F
                }

                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun filterChats(query: String) {
        val filteredChatList = chatList.filter { it.name.contains(query, ignoreCase = true) }
        chatListAdapter = ChatListAdapter(this, filteredChatList)
        findViewById<ListView>(R.id.chat_select_list).adapter = chatListAdapter
    }


    private fun authenticateUser() {
        CoroutineScope(Dispatchers.IO).launch {
            val mUser = FirebaseAuth.getInstance().currentUser
            mUser!!.getIdToken(true)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val idToken = task.result.token

                        if (idToken != null) {
                            AuthenticationHandler.setAuthToken(idToken)
                        } else {
                            // Handle error -> idToken is null
                        }
                    } else {
                        // Handle error -> task.getException();
                    }
                }
        }
    }
}

data class ChatListItem(
    val profilePicture: Bitmap,
    val profilePictureUrl: String,
    val name: String,
    val email: String,
    val time: String
) {
    constructor(json: JsonObject) : this(
        profilePicture = BitmapLoader().execute(json.get("partner_picture").asString).get(),
        profilePictureUrl = json.get("partner_picture").asString,
        name = json.get("partner_name").asString,
        email = json.get("partner_email").asString,
        time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(json.get("last_message_time").asLong))
    )
}

class ChatListAdapter(context: Context, private val data: List<ChatListItem>) :
    ArrayAdapter<ChatListItem>(context, R.layout.chat_list_item, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.chat_list_item, parent, false)

        val item = data[position]

        val profilePicture = view.findViewById<ImageView>(R.id.profile_picture)
        val name = view.findViewById<TextView>(R.id.name)
        val time = view.findViewById<TextView>(R.id.time)

        profilePicture.setImageBitmap(item.profilePicture)
        name.text = item.name
        time.text = item.time

        view.setOnClickListener(View.OnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("recipientProfilePicture", item.profilePictureUrl)
            intent.putExtra("recipientName", item.name)
            intent.putExtra("recipientEmail", item.email)
            context.startActivity(intent)
        })

        return view
    }
}