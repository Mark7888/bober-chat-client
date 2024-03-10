package me.mark7888.boberchat

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isEmpty
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() {
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            authenticateUser()

            val profilePicUrl = user.photoUrl

            try {
                if (profilePicUrl != null) {
                    val downloadImageTask = DownloadImageTask()
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
        val newChatButton = findViewById<Button>(R.id.new_chat_button)

        newChatButton.setOnClickListener {
            val intent = Intent(this, NewChatActivity::class.java)
            startActivity(intent)
        }

        loadChats()
    }

    private fun loadChats() {
        val chatSelectList = findViewById<ListView>(R.id.chat_select_list)
        val chatsLoadingText = findViewById<TextView>(R.id.chats_loading_text)
        chatsLoadingText.text = "Loading chats..."


        //
        // fill in chat list from api TODO
        // Create some dummy data for testing
        val data = listOf(
            ChatListItem(BitmapFactory.decodeResource(resources, R.drawable.ic_google), "Name 1", "Time 1"),
            ChatListItem(BitmapFactory.decodeResource(resources, R.drawable.ic_google), "Name 2", "Time 2")
        )

        val adapter = ChatListAdapter(this, data)
        chatSelectList.adapter = adapter


        // test after api call TODO
        if (chatSelectList.isEmpty()) {
            // change chats_loading_text to "No chats found"
            chatsLoadingText.text = "No chats found"
        }
        else {
            // hide chats_loading_text
            chatsLoadingText.text = ""
            chatsLoadingText.textSize = 0F
        }
    }

    private fun authenticateUser() {
        val mUser = FirebaseAuth.getInstance().currentUser
        mUser!!.getIdToken(true)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val idToken = task.result.token

                    if (idToken != null) {
                        AuthenticationHandler.setAuthToken(idToken)
                    }
                    else {
                        // Handle error -> idToken is null
                    }
                } else {
                    // Handle error -> task.getException();
                }
            }
    }
}

data class ChatListItem(
    val profilePicture: Bitmap,
    val name: String,
    val time: String
)

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

        return view
    }
}