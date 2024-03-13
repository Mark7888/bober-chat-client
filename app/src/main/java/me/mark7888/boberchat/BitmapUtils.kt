package me.mark7888.boberchat

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class BitmapLoader : AsyncTask<String, Void, Bitmap>() {
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

object BitmapUtils {
    fun getBitmapFromURL(src: String?): Bitmap? {
        return try {
            val url = URL(src)
            return BitmapFactory.decodeStream(url.openConnection().getInputStream())
        } catch (e: Exception) {
            Log.e("BitmapUtils", e.message.toString())
            null
        }
    }
}

class MyApp : Application() {
    init {
        instance = this
    }

    companion object {
        private var instance: MyApp? = null

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }
}