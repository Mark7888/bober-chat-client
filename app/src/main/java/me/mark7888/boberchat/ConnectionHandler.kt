package me.mark7888.boberchat

import android.content.Context
import android.content.res.Resources
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import java.net.HttpURLConnection
import java.net.URL
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result


object ConnectionHandler {
    private val serverUrl : String = "https://e49f-185-45-199-174.ngrok-free.app"
    fun postRequestJson(endpoint: String, json: String) {
        Fuel.post(serverUrl + endpoint)
            .jsonBody(json)
            .also { println(it) }
            .response { result -> println(result)}
    }
}
