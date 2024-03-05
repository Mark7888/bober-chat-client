package me.mark7888.boberchat

import android.content.Context
import android.os.AsyncTask
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ConnectionHandler(private val context: Context) {
    fun sendAuthRequest(idToken: String?, email: String?, clientId: String?) {
        val serverUri = context.getString(R.string.server_url) // replace with your server URI
        val urlString = "$serverUri/auth?token=$idToken&email=$email&client_id=$clientId"

        AsyncTask.execute {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.doOutput = true

                val jsonInputString = """{
                    "token": "$idToken",
                    "email": "$email",
                    "client_id": "$clientId"
                }"""

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(jsonInputString)
                    writer.flush()
                }

                conn.inputStream.bufferedReader().use { reader ->
                    val response = StringBuilder()
                    var line = reader.readLine()
                    while (line != null) {
                        response.append(line)
                        line = reader.readLine()
                    }
                    // Here you can handle the server's response
                    println(response.toString())
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}