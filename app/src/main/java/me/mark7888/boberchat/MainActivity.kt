package me.mark7888.boberchat

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
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



        val chatSelectList = findViewById<ListView>(R.id.chat_select_list)


        //
        // fill in chat list from api TODO
        //


        // if chat_select_list is empty, redirect to new chat activity
        if (chatSelectList.isEmpty()) {
            val intent = Intent(this, NewChatActivity::class.java)
            startActivity(intent)
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
