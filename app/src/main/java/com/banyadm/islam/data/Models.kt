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

// Reminder minutes options are calculated dynamically based on gap between prayers
// We divide gap by 2, 3, 4 and filter to reasonable values (max 60 min, min 5 min)
fun calculateReminderOptions(gapMinutes: Int): List<Int> {
    val raw = listOf(2, 3, 4, 5, 6).map { gapMinutes / it }
    return raw.filter { it in 5..60 }.distinct().sorted()
}
