package com.helloluckyhuang.lbspoiapp.ui.viewmodel

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helloluckyhuang.lbspoiapp.PoiApp
import com.helloluckyhuang.lbspoiapp.R
import com.helloluckyhuang.lbspoiapp.data.local.PoiProjectData
import com.helloluckyhuang.lbspoiapp.data.repository.PoiProjectRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class PoiPoint(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    var isArrived: Boolean = false,
    val arriveDistance: Double = 100.0,
    val label: String = ""
)

class PoiProjectViewModel(
    private val repo: PoiProjectRepository
) : ViewModel() {
    private val notificationChannelId = "poi_notice_channel"
    private val notificationChannelName = "POI Notice"
    private var nextNotificationId = 1

    val projects = repo.projects.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    private val _currentMapPoiList = MutableStateFlow<List<PoiPoint>>(emptyList())
    val currentMapPoiList: StateFlow<List<PoiPoint>> = _currentMapPoiList

    fun addProject(title: String) {
        viewModelScope.launch {
            repo.addProject(title)
        }
    }

    fun deleteProject(project: PoiProjectData) {
        viewModelScope.launch {
            repo.deleteProject(project)
        }
    }

    fun updateProject(project: PoiProjectData) {
        viewModelScope.launch {
            repo.update(project)
        }
    }

    fun loadMapPoiList(projectUid: Int) {
        viewModelScope.launch {
            val project = repo.getProjectById(projectUid)
            _currentMapPoiList.value = parsePoiList(project?.dataJson ?: "[]")
        }
    }

    fun addPoiToCurrentMap(latitude: Double, longitude: Double, arriveDistance: Double, label: String) {
        _currentMapPoiList.update { poiList ->
            val nextId = (poiList.maxOfOrNull { it.id } ?: 0) + 1
            poiList + PoiPoint(id = nextId, latitude = latitude, longitude = longitude, arriveDistance = arriveDistance, label = label)
        }
    }

    fun deletePoiToCurrentMap(poiId: Int) {
        _currentMapPoiList.update { poiList ->
            poiList.filterNot { it.id == poiId }
        }
    }

    fun updatePoiLabel(projectUid: Int, poiId: Int, label: String) {
        _currentMapPoiList.update { poiList ->
            poiList.map { poi ->
                if (poi.id == poiId) {
                    poi.copy(label = label)
                } else {
                    poi
                }
            }
        }
        persistCurrentMapPoiList(projectUid)
    }

    fun markPoiArrived(projectUid: Int, poiId: Int) {
        var changed = false
        var changedPoi: PoiPoint? = null
        _currentMapPoiList.update { poiList ->
            poiList.map { poi ->
                if (poi.id == poiId && !poi.isArrived) {
                    changed = true
                    changedPoi = poi
                    poi.copy(isArrived = true)
                } else {
                    poi
                }
            }
        }
        if (changed) {
            persistCurrentMapPoiList(projectUid)
            // 提醒用户正在接近预定点
            playNoticeSound()
            vibratePhone(500)
            if (changedPoi != null) {
                showNotification("现在正在接近 ${changedPoi.label}")
            }
        }
    }

    // 播放提示音
    fun playNoticeSound() {
        val mediaPlayer = MediaPlayer.create(PoiApp.instance.applicationContext, R.raw.notice) ?: return
        mediaPlayer.setOnCompletionListener { player ->
            player.release()
        }
        mediaPlayer.setOnErrorListener { player, _, _ ->
            player.release()
            true
        }
        runCatching {
            mediaPlayer.start()
        }.onFailure {
            mediaPlayer.release()
        }
    }

    // 震动设备
    fun vibratePhone(durationMs: Long = 200L) {
        val context = PoiApp.instance.applicationContext
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (!vibrator.hasVibrator()) return

        vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    // 弹出通知
    fun showNotification(text: String) {
        val context = PoiApp.instance.applicationContext
        ensureNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notification = NotificationCompat.Builder(context, notificationChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("LBS POI Notice")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(nextNotificationId++, notification)
        }
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            notificationChannelId,
            notificationChannelName,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Arrival reminder and system notice"
        }
        manager.createNotificationChannel(channel)
    }

    fun saveCurrentMapPoiList(projectUid: Int, onSaved: () -> Unit) {
        viewModelScope.launch {
            val project = repo.getProjectById(projectUid) ?: return@launch
            val json = serializePoiList(_currentMapPoiList.value)
            repo.update(project.copy(dataJson = json))
            onSaved()
        }
    }

    private fun persistCurrentMapPoiList(projectUid: Int) {
        viewModelScope.launch {
            val project = repo.getProjectById(projectUid) ?: return@launch
            val json = serializePoiList(_currentMapPoiList.value)
            repo.update(project.copy(dataJson = json))
        }
    }

    private fun serializePoiList(points: List<PoiPoint>): String {
        val array = JSONArray()
        points.forEach { point ->
            array.put(
                JSONObject()
                    .put("id", point.id)
                    .put("latitude", point.latitude)
                    .put("longitude", point.longitude)
                    .put("isArrived", point.isArrived)
                    .put("arriveDistance", point.arriveDistance)
                    .put("label", point.label)
            )
        }
        return array.toString()
    }

    private fun parsePoiList(json: String): List<PoiPoint> {
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    if (!item.has("latitude") || !item.has("longitude")) continue
                    add(
                        PoiPoint(
                            id = if (item.has("id")) item.optInt("id") else index + 1,
                            latitude = item.optDouble("latitude"),
                            longitude = item.optDouble("longitude"),
                            isArrived = if (item.has("isArrived")) item.optBoolean("isArrived")
                            else false,
                            arriveDistance = if (item.has("arriveDistance")) item.optDouble("arriveDistance") else 100.0,
                            label = if (item.has("label")) item.optString("label") else ""
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
