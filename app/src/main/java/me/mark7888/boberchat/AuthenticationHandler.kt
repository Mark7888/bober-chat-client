package me.mark7888.boberchat

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object AuthenticationHandler {
    private var isAuthenticated: Boolean = false

    private var messagingToken: String = ""
    private var authToken: String = ""
    private var localApiKey: String = ""
    private var authRetryCount: Int = 0
    private const val MAX_AUTH_RETRY_COUNT: Int = 3

    private val sharedPreferences: SharedPreferences = MyApp.applicationContext().getSharedPreferences("AuthenticationHandler", Context.MODE_PRIVATE)

    private var onChatUpdateListener: OnChatsUpdateListener? = null

    fun setOnChatUpdateListener(listener: OnChatsUpdateListener) {
        onChatUpdateListener = listener
    }

    init {
        localApiKey = sharedPreferences.getString("localApiKey", "") ?: ""
    }

    fun setMessagingToken(token: String) {
        if (token == messagingToken) {
            return
        }

        messagingToken = token
        isAuthenticated = false
        authRetryCount = 0

        if (authToken.isNotEmpty()) {
            authToServer()
            return
        }

        SignInActivity.tokenRequest()
    }

    fun setAuthToken(token: String) {
        if (token == authToken) {
            return
        }

        authToken = token
        isAuthenticated = false
        authRetryCount = 0

        if (messagingToken.isNotEmpty()) {
            authToServer()
            return
        }

        MessagingService.tokenRequest()
    }

    fun getApiKey(): String {
        // localApiKey = sharedPreferences.getString("localApiKey", "") ?: ""
        return localApiKey
    }

    fun setApiKey(apiKey : String) {
        sharedPreferences.edit().putString("localApiKey", localApiKey).apply()
        localApiKey = apiKey
    }

    private fun authToServer(force:Boolean = false) {
        if (isAuthenticated && !force) {
            return
        }

        // check if both tokens are set
        if (messagingToken.isNotEmpty() && authToken.isNotEmpty()) {
            Log.d("AuthenticationHandler", "Authenticating to server")
            // send the tokens to the server
            ConnectionHandler.postRequestJson("/authenticate", "{ \"messagingToken\" : \"$messagingToken\", \"authToken\" : \"$authToken\" }")

            /*
            if (statusCode == 200) {
                Log.d("AuthenticationHandler", "Authentication request sent to server")
                return
            }
            Log.e("AuthenticationHandler", "Failed to send authentication request to server")
            */
        }
    }

    fun acknowledge(data: Map<String, String>) {
        var apiKey : String? = null

        if (data.containsKey("api_key")) {
            apiKey = data["api_key"]
        }

        if (apiKey != null) {
            // The server has acknowledged the authentication
            isAuthenticated = true
            setApiKey(apiKey)

            onChatUpdateListener?.onChatsUpdate()

            Log.d("AuthenticationHandler", "Server acknowledged authentication successfully")
            return
        }

        isAuthenticated = false
        Log.w("AuthenticationHandler", "Server acknowledged authentication, but missing api_key")

        if (authRetryCount > MAX_AUTH_RETRY_COUNT) {
            Log.e("AuthenticationHandler", "Failed to authenticate to server. Retry limit exceeded")
            return
        }
        authRetryCount++

        // re-authenticate to server after 10 seconds
        Thread.sleep(10000)
        authToServer(true)
    }


    interface OnChatsUpdateListener {
        fun onChatsUpdate()
    }
}