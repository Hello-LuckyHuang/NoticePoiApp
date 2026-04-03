package com.helloluckyhuang.lbspoiapp.data.repository

import android.app.Notification
import android.content.Context
import com.amap.api.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationRepository(context: Context) {

    private val client = AMapLocationClient(context.applicationContext)

    private val _location = MutableStateFlow<Pair<Double, Double>?>(null)
    val location: StateFlow<Pair<Double, Double>?> = _location

    private val listener = AMapLocationListener { loc ->
        if (loc != null && loc.errorCode == 0) {
            _location.value = loc.latitude to loc.longitude
        }
    }

    private fun config() {
        val option = AMapLocationClientOption().apply {
            isOnceLocation = false
            interval = 2000
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        }
        client.setLocationOption(option)
        client.setLocationListener(listener)
    }

    // 前台定位（无后台权限用这个）
    fun startForegroundOnly() {
        config()
        client.startLocation()
    }

    // 后台定位（有后台权限）
    fun startWithService(notification: Notification) {
        config()
        client.enableBackgroundLocation(1, notification)
        client.startLocation()
    }

    fun stop() {
        client.stopLocation()
        client.disableBackgroundLocation(true)
    }
}