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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
//import com.amap.api.maps2d.AMapUtils
//import com.amap.api.maps2d.model.LatLng
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.LatLng
import com.helloluckyhuang.lbspoiapp.PoiApp
import com.helloluckyhuang.lbspoiapp.R
import com.helloluckyhuang.lbspoiapp.data.local.PoiProjectData
import com.helloluckyhuang.lbspoiapp.data.repository.PoiProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.emptyList

data class PoiPoint(
    val latitude: Double,
    val longitude: Double
)

data class PoiPointData(
    val id: Int,
    val pos: PoiPoint,
    var isArrived: Boolean = false,
    val arriveDistance: Double = 100.0,
    val label: String = ""
)

data class PoiPointUiModel(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val distance: Float?,
    var isArrived: Boolean = false,
    val arriveDistance: Double = 100.0,
    val label: String = ""
)

class PoiProjectViewModel(
    private val repo: PoiProjectRepository
) : ViewModel() {
    private val notificationChannelId = "poi_notice_channel_important"
    private val notificationChannelName = "POI Important Notice"
    private var nextNotificationId = 1

    val projects = repo.projects.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var mapPoiList = mutableStateListOf<PoiPointData>()
    private val _uiPoiListState = MutableStateFlow<List<PoiPointUiModel>>(emptyList())
    val uiPoiListState: StateFlow<List<PoiPointUiModel>> = _uiPoiListState.asStateFlow()

    var locationPoint by mutableStateOf(PoiPoint(0.0, 0.0))
        private set

    private val _projectName = MutableStateFlow("")
    val projectName: StateFlow<String> = _projectName

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

    fun loadPoiProject(projectUid: Int) {
        viewModelScope.launch {
            val project = repo.getProjectById(projectUid)
            mapPoiList = parsePoiList(project?.dataJson ?: "[]")
            _projectName.value = project?.title ?: ""
            _uiPoiListState.value = mapPoiList.map {
                PoiPointUiModel(
                    it.id,
                    it.pos.latitude,
                    it.pos.longitude,
                    null,
                    it.isArrived,
                    it.arriveDistance,
                    it.label
                )
            }
        }
    }

    fun addPoiToCurrentMap(latitude: Double, longitude: Double, arriveDistance: Double, label: String) {
        val nextId = (mapPoiList.maxOfOrNull { it.id } ?: 0) + 1
        mapPoiList += PoiPointData(id = nextId, pos = PoiPoint(latitude, longitude), arriveDistance = arriveDistance, label = label)
        rebuildUI()
    }

    fun getPoiById(poiId: Int): PoiPointData? {
        return mapPoiList.find { it.id == poiId }
    }

    fun deletePoiToCurrentMap(poiId: Int) {
        mapPoiList.removeAll { it.id == poiId }
        rebuildUI()
    }

    fun updatePoiLabel(projectUid: Int, poiId: Int, label: String) {
        mapPoiList.replaceAll { poi ->
            if (poi.id == poiId) {
                poi.copy(label = label)
            } else {
                poi
            }
        }
        persistCurrentMapPoiList(projectUid)
        rebuildUI()
    }

    fun searchAndMarkArrivedPoi(projectUid: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            var changed = false
            var changedPoi: PoiPointData? = null
            val currentLatLng = LatLng(locationPoint.latitude, locationPoint.longitude)
            mapPoiList.replaceAll { poi ->
                val lat = poi.pos.latitude
                val lng = poi.pos.longitude
                val poiLatLng = LatLng(lat, lng)
                val distance = AMapUtils.calculateLineDistance(currentLatLng, poiLatLng)
                if (distance < poi.arriveDistance && !poi.isArrived) {
                    changed = true
                    changedPoi = poi
                    poi.copy(isArrived = true)
                } else {
                    poi
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
    }

    fun markPoiArrived(projectUid: Int, poiId: Int) {
        var changed = false
        var changedPoi: PoiPointData? = null
        mapPoiList.replaceAll { poi ->
            if (poi.id == poiId && !poi.isArrived) {
                changed = true
                changedPoi = poi
                poi.copy(isArrived = true)
            } else {
                poi
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

    fun erasePoiArrived(projectUid: Int, poiId: Int) {
        var changed = false
        mapPoiList.replaceAll { poi ->
            if (poi.id == poiId && poi.isArrived) {
                changed = true
                poi.copy(isArrived = false)
            } else {
                poi
            }
        }
        if (changed) {
            persistCurrentMapPoiList(projectUid)
        }
        rebuildUI()
    }

    // 刷新本地位置
    fun updateLocalPoint(newLocation: PoiPoint) {
        locationPoint = newLocation
        rebuildUI()
    }

    // 播放提示音
    private fun playNoticeSound() {
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
    private fun vibratePhone(durationMs: Long = 200L) {
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
    private fun showNotification(text: String) {
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
            .setContentTitle("接近预定点提醒")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
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
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Arrival reminder and system notice"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    // 刷新UI
    private fun rebuildUI() {
        viewModelScope.launch(Dispatchers.Default) {
            val sortedCards = mapPoiList
                .asSequence()
                .map { fixed ->
                    val poiLatLng = LatLng(fixed.pos.latitude, fixed.pos.longitude)
                    val currentLatLng = LatLng(locationPoint.latitude, locationPoint.longitude)
                    PoiPointUiModel(
                        id = fixed.id,
                        latitude = fixed.pos.latitude,
                        longitude = fixed.pos.longitude,
                        distance = AMapUtils.calculateLineDistance(currentLatLng, poiLatLng),
                        isArrived = fixed.isArrived,
                        arriveDistance = fixed.arriveDistance,
                        label = fixed.label
                    )
                }
                .sortedBy { it.distance }
                .toList()

            withContext(Dispatchers.Main) {
                _uiPoiListState.update {
                    sortedCards
                }
            }
        }
    }

    fun saveCurrentMapPoiList(projectUid: Int, onSaved: () -> Unit) {
        viewModelScope.launch {
            val project = repo.getProjectById(projectUid) ?: return@launch
            val json = serializePoiList(mapPoiList)
            repo.update(project.copy(dataJson = json))
            onSaved()
        }
    }

    private fun persistCurrentMapPoiList(projectUid: Int) {
        viewModelScope.launch {
            val project = repo.getProjectById(projectUid) ?: return@launch
            val json = serializePoiList(mapPoiList)
            repo.update(project.copy(dataJson = json))
        }
    }

    private fun serializePoiList(points: List<PoiPointData>): String {
        val array = JSONArray()
        points.forEach { point ->
            array.put(
                JSONObject()
                    .put("id", point.id)
                    .put("latitude", point.pos.latitude)
                    .put("longitude", point.pos.longitude)
                    .put("isArrived", point.isArrived)
                    .put("arriveDistance", point.arriveDistance)
                    .put("label", point.label)
            )
        }
        return array.toString()
    }

    private fun parsePoiList(json: String): SnapshotStateList<PoiPointData> {
        val points = SnapshotStateList<PoiPointData>()
        try {
            val array = JSONArray(json)
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                if (!item.has("latitude") || !item.has("longitude")) continue
                points.add(
                    PoiPointData(
                        id = if (item.has("id")) item.optInt("id") else index + 1,
                        pos = PoiPoint(item.optDouble("latitude"), item.optDouble("longitude")),
                        isArrived = if (item.has("isArrived")) item.optBoolean("isArrived")
                        else false,
                        arriveDistance = if (item.has("arriveDistance")) item.optDouble("arriveDistance") else 100.0,
                        label = if (item.has("label")) item.optString("label") else ""
                    )
                )
            }
        } catch (_: Exception) {
            return SnapshotStateList()
        }

        return points
    }
}
