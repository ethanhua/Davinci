package com.ethanhua.davinci.library

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 类描述
 *
 * @author ethanhua
 * @version 0.1
 * @since 2018/12/5
 */
class DefaultImageHttpRequest :IImageHttpRequest {

    override fun load(url: String): InputStream? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            val headers = buildDefaultHeaders()
            for (header in headers) {
                conn.addRequestProperty(header.key, header.value)
            }
            conn.connect()
            if (isHttpOk(conn.responseCode)) conn.inputStream else null
        } catch (e: Exception) {
            null
        }
    }

    private fun buildDefaultHeaders():Map<String,String>{
       return mapOf()
    }

    private fun isHttpOk(statusCode: Int): Boolean {
        return statusCode / 100 == 2
    }

}