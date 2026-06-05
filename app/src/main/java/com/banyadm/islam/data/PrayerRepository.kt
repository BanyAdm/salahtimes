package com.banyadm.islam.data

import java.text.SimpleDateFormat
import java.util.*

class PrayerRepository {

    suspend fun fetchTimes(lat: Double, lon: Double, method: Int): Result<PrayerTimes> {
        return try {
            val date = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
            val response = AladhanApi.service.getTimings(date, lat, lon, method)
            if (response.code == 200) {
                val t = response.data.timings
                Result.success(
                    PrayerTimes(
                        fajr = t.Fajr.take(5),
                        dhuhr = t.Dhuhr.take(5),
                        asr = t.Asr.take(5),
                        maghrib = t.Maghrib.take(5),
                        isha = t.Isha.take(5),
                        date = date
                    )
                )
            } else {
                Result.failure(Exception("API error ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
