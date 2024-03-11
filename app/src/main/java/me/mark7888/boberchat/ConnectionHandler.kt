package me.mark7888.boberchat

import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.awaitResponse
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import kotlinx.coroutines.runBlocking


object ConnectionHandler {
    // private const val SERVER_BASE_URL : String = "https://aef2-185-45-199-174.ngrok-free.app"
    private const val SERVER_BASE_URL : String = "https://api.boberchat.mark7888.hu"

    fun postRequestJson(endpoint: String, json: String) {
        Fuel.post(SERVER_BASE_URL + endpoint)
            .jsonBody(json)
            .also { println(it) }
            .response { _, _, _ -> }
    }

    fun postRequestJsonHold(endpoint: String, json: String): Int {
        var statusCode = 0

        val (request, response, result) = (SERVER_BASE_URL + endpoint)
            .httpPost().jsonBody(json).responseString()

        when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                Log.e("ConnectionHandler", ex.toString())
            }
            is Result.Success -> {
                statusCode = response.statusCode
            }
        }

        return statusCode
    }


    fun getRequestJson(endpoint: String): String {
        var responseJson = "[]"

        val (request, response, result) = (SERVER_BASE_URL + endpoint)
            .httpGet().responseString()

        when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                Log.e("ConnectionHandler", ex.toString())
            }
            is Result.Success -> {
                responseJson = result.get()
            }
        }

        return responseJson
    }
}
