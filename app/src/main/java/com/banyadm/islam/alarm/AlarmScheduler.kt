package com.banyadm.islam.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.banyadm.islam.data.Prayer
import com.banyadm.islam.data.PrayerTimes
import java.text.SimpleDateFormat
import java.util.*

object AlarmScheduler {

    fun scheduleAll(
        context: Context,
        times: PrayerTimes,
        toggles: Map<Prayer, Boolean>,
        reminderToggles: Map<Prayer, Boolean> = emptyMap(),
        reminderMinutes: Int = 15
    ) {
        cancelAll(context)
        val prayerList = listOf(
            Prayer.FAJR to times.fajr,
            Prayer.DHUHR to times.dhuhr,
            Prayer.ASR to times.asr,
            Prayer.MAGHRIB to times.maghrib,
            Prayer.ISHA to times.isha
        )

        prayerList.forEachIndexed { index, (prayer, time) ->
            // Main alarm
            if (toggles[prayer] == true) {
                scheduleOne(context, prayer, time, isReminder = false)
            }
            // Reminder alarm — only if enabled and reminder time is before this prayer
            if (reminderToggles[prayer] == true) {
                val prayerMillis = getTimeMillis(time)
                val reminderMillis = prayerMillis - (reminderMinutes * 60 * 1000L)
                if (reminderMillis > System.currentTimeMillis()) {
                    scheduleReminder(context, prayer, reminderMillis, reminderMinutes)
                }
            }
        }
    }

    private fun scheduleOne(context: Context, prayer: Prayer, time: String, isReminder: Boolean) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("prayer_name", prayer.displayName)
            putExtra("prayer_arabic", prayer.arabic)
            putExtra("prayer_id", prayer.ordinal)
            putExtra("is_reminder", false)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, prayer.ordinal, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = getTimeMillis(time)
        if (triggerTime > System.currentTimeMillis()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    private fun scheduleReminder(context: Context, prayer: Prayer, triggerMillis: Long, minutes: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("prayer_name", prayer.displayName)
            putExtra("prayer_arabic", prayer.arabic)
            putExtra("prayer_id", prayer.ordinal + 10)
            putExtra("is_reminder", true)
            putExtra("reminder_minutes", minutes)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, prayer.ordinal + 10, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        Prayer.entries.forEach { prayer ->
            // Cancel main alarm
            val intent = Intent(context, AlarmReceiver::class.java)
            PendingIntent.getBroadcast(
                context, prayer.ordinal, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let { alarmManager.cancel(it) }
            // Cancel reminder
            PendingIntent.getBroadcast(
                context, prayer.ordinal + 10, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let { alarmManager.cancel(it) }
        }
    }

    fun getTimeMillis(time: String): Long {
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        val parsed = sdf.parse(time) ?: return 0L
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parsed.hours)
            set(Calendar.MINUTE, parsed.minutes)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
