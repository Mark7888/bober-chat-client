package me.mark7888.boberchat

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import java.io.File


object ConnectionHandler {
    // private const val SERVER_BASE_URL : String = "https://aef2-185-45-199-174.ngrok-free.app"
    const val SERVER_BASE_URL : String = "https://api.boberchat.mark7888.hu"

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

    fun uploadImageRequest(endpoint: String, apiKey: String, imageUri: String, context: Context): String? {
        FuelManager.instance.baseHeaders = mapOf("apiKey" to apiKey)

        val inputStream = context.contentResolver.openInputStream(imageUri.toUri())
        val file = File.createTempFile("upload", null, context.cacheDir)
        inputStream?.copyTo(file.outputStream())

        val (_, _, result) = Fuel.upload(SERVER_BASE_URL + endpoint)
            .add(FileDataPart(file, name = "file", filename = "image.jpg"))
            .responseString()

        return when (result) {
            is Result.Failure -> {
                val ex = result.getException()
                Log.e("ConnectionHandler", ex.toString())
                null
            }
            is Result.Success -> {
                return result.get()
            }
        }
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
