package com.banyadm.islam.data

import java.text.SimpleDateFormat
import java.util.*

class PrayerRepository {
    suspend fun fetchTimes(lat: Double, lon: Double, method: Int): Result<PrayerTimes> {
        return try {
            val date = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
            val json = AladhanApi.fetchTimings(lat, lon, method, date)
                ?: return Result.failure(Exception("No response from server"))

            val code = json.getInt("code")
            if (code != 200) return Result.failure(Exception("API returned code $code"))

            val timings = json.getJSONObject("data").getJSONObject("timings")
            Result.success(
                PrayerTimes(
                    fajr = timings.getString("Fajr").take(5),
                    dhuhr = timings.getString("Dhuhr").take(5),
                    asr = timings.getString("Asr").take(5),
                    maghrib = timings.getString("Maghrib").take(5),
                    isha = timings.getString("Isha").take(5),
                    date = date
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
