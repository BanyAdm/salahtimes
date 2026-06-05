package com.banyadm.islam.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AladhanApi {
    suspend fun fetchTimings(lat: Double, lon: Double, method: Int, date: String): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.aladhan.com/v1/timings/$date?latitude=$lat&longitude=$lon&method=$method")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("User-Agent", "SalahTimes/1.0")
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                JSONObject(response)
            } catch (e: Exception) {
                null
            }
        }
    }
}
