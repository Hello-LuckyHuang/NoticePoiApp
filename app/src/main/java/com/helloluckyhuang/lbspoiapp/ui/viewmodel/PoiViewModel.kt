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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.LatLng
import com.helloluckyhuang.lbspoiapp.PoiApp
import com.helloluckyhuang.lbspoiapp.R
import com.helloluckyhuang.lbspoiapp.data.repository.PoiProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

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

class PoiViewModel(
    private val repo: PoiProjectRepository
) : ViewModel() {
    private val notificationChannelId = "poi_notice_channel_important"
    private val notificationChannelName = "POI Important Notice"
    private var nextNotificationId = 1

    private var projectUid: Int = -1

    private val _projectName = MutableStateFlow("")
    val projectName: StateFlow<String> = _projectName

    var mapPoiList = mutableStateListOf<PoiPointData>()
    private val poiListVersionFlow = MutableStateFlow(0)

    // 定位服务位置
    val backGroundLocation = PoiApp.locationRepo.location
    val locationPoint: StateFlow<PoiPoint?> =
        backGroundLocation
            .filterNotNull()
            .filter { (lat, lng) ->
                lat != 0.0 && lng != 0.0 &&
                        lat in -90.0..90.0 &&
                        lng in -180.0..180.0
            }
            .map { (lat, lng) ->
                PoiPoint(lat, lng)
            }
            .distinctUntilChanged()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )

    val uiPoiListState: StateFlow<List<PoiPointUiModel>> =
        combine(locationPoint, poiListVersionFlow) { location, _ ->
            mapPoiList
                .map { poi ->
                    val poiLatLng = LatLng(poi.pos.latitude, poi.pos.longitude)
                    val distance = location?.let {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        AMapUtils.calculateLineDistance(currentLatLng, poiLatLng)
                    }
                    if (distance != null && distance < poi.arriveDistance && !poi.isArrived) {
                        poi.isArrived = true
                        showNotification("正在接近: ${poi.label}")
                        playNoticeSound()
                        vibratePhone()
                    }
                    PoiPointUiModel(
                        id = poi.id,
                        latitude = poi.pos.latitude,
                        longitude = poi.pos.longitude,
                        distance = distance,
                        isArrived = poi.isArrived,
                        arriveDistance = poi.arriveDistance,
                        label = poi.label
                    )
                }
                .sortedWith(
                    compareBy<PoiPointUiModel> { it.distance == null }
                        .thenBy { it.distance ?: Float.MAX_VALUE }
                        .thenBy { it.id }
                )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    fun loadPoiProject(projectUid: Int) {
        this.projectUid = projectUid
        viewModelScope.launch {
            val project = repo.getProjectById(projectUid)
            mapPoiList = parsePoiList(project?.dataJson ?: "[]")
            _projectName.value = project?.title ?: ""
            poiListVersionFlow.value++
        }
    }

    fun saveCurrentMapPoiListAndExit(onSaved: () -> Unit) {
        viewModelScope.launch {
            val project = repo.getProjectById(projectUid) ?: return@launch
            val json = serializePoiList(mapPoiList)
            repo.update(project.copy(dataJson = json))
            onSaved()
        }
        projectUid = -1
    }

    fun persistCurrentMapPoiList(projectUid: Int) {
        viewModelScope.launch {
            val project = repo.getProjectById(projectUid) ?: return@launch
            val json = serializePoiList(mapPoiList)
            repo.update(project.copy(dataJson = json))
        }
    }

    fun addPoiToCurrentMap(latitude: Double, longitude: Double, arriveDistance: Double, label: String) {
        val nextId = (mapPoiList.maxOfOrNull { it.id } ?: 0) + 1
        mapPoiList += PoiPointData(id = nextId, pos = PoiPoint(latitude, longitude), arriveDistance = arriveDistance, label = label)
        poiListVersionFlow.value++
    }

    fun getPoiById(poiId: Int): PoiPointData? {
        return mapPoiList.find { it.id == poiId }
    }

    fun deletePoiToCurrentMap(poiId: Int) {
        mapPoiList.removeAll { it.id == poiId }
        poiListVersionFlow.value++
    }

    fun updatePoiLabel(poiId: Int, label: String) {
        mapPoiList.replaceAll { poi ->
            if (poi.id == poiId) {
                poi.copy(label = label)
            } else {
                poi
            }
        }
        poiListVersionFlow.value++
    }

    fun erasePoiArrived(poiId: Int) {
        mapPoiList.replaceAll { poi ->
            if (poi.id == poiId && poi.isArrived) {
                poi.copy(isArrived = false)
            } else {
                poi
            }
        }
        poiListVersionFlow.value++
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
