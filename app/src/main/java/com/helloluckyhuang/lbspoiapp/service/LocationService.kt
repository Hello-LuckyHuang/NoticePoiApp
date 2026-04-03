package com.helloluckyhuang.lbspoiapp.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.helloluckyhuang.lbspoiapp.PoiApp
import com.helloluckyhuang.lbspoiapp.data.repository.LocationRepository

class LocationService : Service() {

    private lateinit var repo: LocationRepository

    override fun onCreate() {
        super.onCreate()
        repo = PoiApp.locationRepo
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val notification = createNotification()

        startForeground(1, notification)

        repo.startWithService(notification)

        return START_STICKY
    }

    override fun onDestroy() {
        repo.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "location"

        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            channelId,
            "定位",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("正在定位")
            .setContentText("后台持续定位中")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }
}