package me.mark7888.boberchat

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.io.InputStream
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL


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
    }
}

class DownloadImageTask : AsyncTask<String, Void, Bitmap>() {
    override fun doInBackground(vararg urls: String): Bitmap? {
        val url = urls[0]
        var bmp: Bitmap? = null
        try {
            val urlConnection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
            urlConnection.doInput = true
            urlConnection.connect()
            val inputStream: InputStream = urlConnection.inputStream
            bmp = BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bmp
    }
}