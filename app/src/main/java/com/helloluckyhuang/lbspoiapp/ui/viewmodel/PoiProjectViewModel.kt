package com.helloluckyhuang.lbspoiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helloluckyhuang.lbspoiapp.data.local.PoiProjectData
import com.helloluckyhuang.lbspoiapp.data.repository.PoiProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class PoiPoint(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    var isArrived: Boolean = false
)

class PoiProjectViewModel(
    private val repo: PoiProjectRepository
) : ViewModel() {
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

    fun addPoiToCurrentMap(latitude: Double, longitude: Double) {
        _currentMapPoiList.update { poiList ->
            val nextId = (poiList.maxOfOrNull { it.id } ?: 0) + 1
            poiList + PoiPoint(id = nextId, latitude = latitude, longitude = longitude)
        }
    }

    fun deletePoiToCurrentMap(poiId: Int) {
        _currentMapPoiList.update { poiList ->
            poiList.filterNot { it.id == poiId }
        }
    }

    fun markPoiArrived(projectUid: Int, poiId: Int) {
        var changed = false
        _currentMapPoiList.update { poiList ->
            poiList.map { poi ->
                if (poi.id == poiId && !poi.isArrived) {
                    changed = true
                    poi.copy(isArrived = true)
                } else {
                    poi
                }
            }
        }
        if (changed) {
            persistCurrentMapPoiList(projectUid)
        }
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
                            else false
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
