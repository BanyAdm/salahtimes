package com.banyadm.islam.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "salah_prefs")

object Keys {
    val FAJR_ENABLED = booleanPreferencesKey("fajr_enabled")
    val DHUHR_ENABLED = booleanPreferencesKey("dhuhr_enabled")
    val ASR_ENABLED = booleanPreferencesKey("asr_enabled")
    val MAGHRIB_ENABLED = booleanPreferencesKey("maghrib_enabled")
    val ISHA_ENABLED = booleanPreferencesKey("isha_enabled")
    val SNOOZE_COUNT = intPreferencesKey("snooze_count")
    val SNOOZE_DURATION = intPreferencesKey("snooze_duration")
    val LATITUDE = stringPreferencesKey("latitude")
    val LONGITUDE = stringPreferencesKey("longitude")
    val CALC_METHOD = intPreferencesKey("calc_method")
    val SETUP_DONE = booleanPreferencesKey("setup_done")
    val LAST_FETCH_DATE = stringPreferencesKey("last_fetch_date")
    val CACHED_FAJR = stringPreferencesKey("cached_fajr")
    val CACHED_DHUHR = stringPreferencesKey("cached_dhuhr")
    val CACHED_ASR = stringPreferencesKey("cached_asr")
    val CACHED_MAGHRIB = stringPreferencesKey("cached_maghrib")
    val CACHED_ISHA = stringPreferencesKey("cached_isha")
}

class SalahPreferences(private val context: Context) {

    val isSetupDone: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.SETUP_DONE] ?: false }

    val prayerToggles: Flow<Map<Prayer, Boolean>> = context.dataStore.data
        .map { prefs ->
            mapOf(
                Prayer.FAJR to (prefs[Keys.FAJR_ENABLED] ?: true),
                Prayer.DHUHR to (prefs[Keys.DHUHR_ENABLED] ?: true),
                Prayer.ASR to (prefs[Keys.ASR_ENABLED] ?: true),
                Prayer.MAGHRIB to (prefs[Keys.MAGHRIB_ENABLED] ?: true),
                Prayer.ISHA to (prefs[Keys.ISHA_ENABLED] ?: true)
            )
        }

    val snoozeCount: Flow<Int> = context.dataStore.data
        .map { it[Keys.SNOOZE_COUNT] ?: 3 }

    val snoozeDuration: Flow<Int> = context.dataStore.data
        .map { it[Keys.SNOOZE_DURATION] ?: 5 }

    val location: Flow<Pair<Double, Double>?> = context.dataStore.data
        .map { prefs ->
            val lat = prefs[Keys.LATITUDE]?.toDoubleOrNull()
            val lon = prefs[Keys.LONGITUDE]?.toDoubleOrNull()
            if (lat != null && lon != null) Pair(lat, lon) else null
        }

    val calcMethod: Flow<Int> = context.dataStore.data
        .map { it[Keys.CALC_METHOD] ?: 3 }

    val cachedTimes: Flow<PrayerTimes?> = context.dataStore.data
        .map { prefs ->
            val fajr = prefs[Keys.CACHED_FAJR] ?: return@map null
            val dhuhr = prefs[Keys.CACHED_DHUHR] ?: return@map null
            val asr = prefs[Keys.CACHED_ASR] ?: return@map null
            val maghrib = prefs[Keys.CACHED_MAGHRIB] ?: return@map null
            val isha = prefs[Keys.CACHED_ISHA] ?: return@map null
            val date = prefs[Keys.LAST_FETCH_DATE] ?: return@map null
            PrayerTimes(fajr, dhuhr, asr, maghrib, isha, date)
        }

    suspend fun setSetupDone(done: Boolean) {
        context.dataStore.edit { it[Keys.SETUP_DONE] = done }
    }

    suspend fun setToggle(prayer: Prayer, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            when (prayer) {
                Prayer.FAJR -> prefs[Keys.FAJR_ENABLED] = enabled
                Prayer.DHUHR -> prefs[Keys.DHUHR_ENABLED] = enabled
                Prayer.ASR -> prefs[Keys.ASR_ENABLED] = enabled
                Prayer.MAGHRIB -> prefs[Keys.MAGHRIB_ENABLED] = enabled
                Prayer.ISHA -> prefs[Keys.ISHA_ENABLED] = enabled
            }
        }
    }

    suspend fun setSnooze(count: Int, duration: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SNOOZE_COUNT] = count
            prefs[Keys.SNOOZE_DURATION] = duration
        }
    }

    suspend fun setLocation(lat: Double, lon: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LATITUDE] = lat.toString()
            prefs[Keys.LONGITUDE] = lon.toString()
        }
    }

    suspend fun setCalcMethod(method: Int) {
        context.dataStore.edit { it[Keys.CALC_METHOD] = method }
    }

    suspend fun cacheTimes(times: PrayerTimes) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CACHED_FAJR] = times.fajr
            prefs[Keys.CACHED_DHUHR] = times.dhuhr
            prefs[Keys.CACHED_ASR] = times.asr
            prefs[Keys.CACHED_MAGHRIB] = times.maghrib
            prefs[Keys.CACHED_ISHA] = times.isha
            prefs[Keys.LAST_FETCH_DATE] = times.date
        }
    }
}
