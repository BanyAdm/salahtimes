package com.banyadm.islam

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class SalahApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        val alarmChannel = NotificationChannel(
            "salah_alarm", "Prayer Alarms", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setBypassDnd(true)
            enableVibration(true)
        }
        val syncChannel = NotificationChannel(
            "salah_sync", "Prayer Sync", NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(alarmChannel)
        manager.createNotificationChannel(syncChannel)
    }
}
