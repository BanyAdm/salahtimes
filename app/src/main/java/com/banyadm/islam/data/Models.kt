package com.banyadm.islam.data

data class PrayerTimes(
    val fajr: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val date: String
)

enum class Prayer(val displayName: String, val arabic: String, val prefKey: String) {
    FAJR("Fajr", "الفجر", "fajr_enabled"),
    DHUHR("Dhuhr", "الظهر", "dhuhr_enabled"),
    ASR("Asr", "العصر", "asr_enabled"),
    MAGHRIB("Maghrib", "المغرب", "maghrib_enabled"),
    ISHA("Isha", "العشاء", "isha_enabled")
}
