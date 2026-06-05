package com.banyadm.islam.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class AladhanResponse(val code: Int, val data: AladhanData)
data class AladhanData(val timings: AladhanTimings, val date: AladhanDate)
data class AladhanTimings(
    val Fajr: String,
    val Dhuhr: String,
    val Asr: String,
    val Maghrib: String,
    val Isha: String
)
data class AladhanDate(val readable: String)

interface AladhanService {
    @GET("timings/{date}")
    suspend fun getTimings(
        @Path("date") date: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 3
    ): AladhanResponse
}

object AladhanApi {
    val service: AladhanService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.aladhan.com/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AladhanService::class.java)
    }
}
