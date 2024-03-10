package me.mark7888.boberchat

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import kotlinx.coroutines.runBlocking


object ConnectionHandler {
    private const val SERVER_BASE_URL : String = "https://aef2-185-45-199-174.ngrok-free.app"

    fun postRequestJson(endpoint: String, json: String): Int {
        var statusCode = 0
        Fuel.post(SERVER_BASE_URL + endpoint)
            .jsonBody(json)
            .also { println(it) }
            .response { _, response, _ ->
                statusCode = response.statusCode
            }
        return statusCode
    }
}
