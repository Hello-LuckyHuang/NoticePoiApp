package com.helloluckyhuang.lbspoiapp.ui.map

import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amap.api.maps2d.AMap
import com.amap.api.maps2d.AMapUtils
import com.amap.api.maps2d.MapView
import com.amap.api.maps2d.model.LatLng
import com.amap.api.maps2d.model.Marker
import com.amap.api.maps2d.model.MarkerOptions
import com.amap.api.maps2d.model.MyLocationStyle
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiPoint
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiProjectViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MapCard(
    projectUid: Int,
    poiList: List<PoiPoint>,
    viewModel: PoiProjectViewModel
) {
    val latestPoiListState = rememberUpdatedState(poiList)
    val poiMarkers = remember { mutableListOf<Marker>() }

    var position by remember { mutableStateOf(Pair<Double, Double>(0.0, 0.0)) }
    val sortedPoiList = remember(poiList, position) {
        poiList.sortedBy { poi ->
            val latLng1 = LatLng(position.first, position.second)
            val latLng2 = LatLng(poi.latitude, poi.longitude)
            AMapUtils.calculateLineDistance(latLng1,latLng2)
        }.reversed()
    }

    // 新建弹窗
    var showCreateDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var selectPoint by remember { mutableStateOf<LatLng?>(null) }

    // 编辑弹窗
    var showEditDialog by remember { mutableStateOf(false) }
    var editPoiId by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
            map.setMyLocationStyle(MyLocationStyle().apply {
                myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER)
                interval(2000)
            })
            map.isMyLocationEnabled = true
            map.setOnMyLocationChangeListener { location ->
                position = Pair(location.latitude, location.longitude)
                val currentLatLng = LatLng(location.latitude, location.longitude)
                latestPoiListState.value.forEach { poi ->
                    if (!poi.isArrived) {
                        val poiLatLng = LatLng(poi.latitude, poi.longitude)
                        val distance = AMapUtils.calculateLineDistance(currentLatLng, poiLatLng)
                        if (distance < 100f) {
                            viewModel.markPoiArrived(projectUid, poi.id)
                        }
                    }
                }
            }
            map.setOnMapLongClickListener { point ->
                showCreateDialog = true
                inputName = ""
                selectPoint = point
            }
        }
    }

    LaunchedEffect(poiList) {
        val aMap = mapView.map
        poiMarkers.forEach { it.remove() }
        poiMarkers.clear()
        poiList.forEach { point ->
            val marker = aMap.addMarker(
                MarkerOptions()
                    .position(LatLng(point.latitude, point.longitude))
                    .title("点 ${point.id}")
            )
            if (marker != null) {
                poiMarkers.add(marker)
            }
        }
    }

    // 地图主界面
    Box(modifier = Modifier.fillMaxSize()) {
        // 地图
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )

        // 上拉抽屉
        var showSheet by remember { mutableStateOf(false) }

        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false // 是否跳过半展开
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Button(
                onClick = { showSheet = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Text("打开抽屉")
            }

            if (showSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSheet = false },
                    sheetState = sheetState
                ) {

                    // 点列表
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = sortedPoiList,
                            key = { it.id }
                        ) { item ->
                            Modifier
                                .fillMaxWidth()
                            Card(
                                modifier = Modifier
                                    .animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = spring<androidx.compose.ui.unit.IntOffset>(
                                            stiffness = Spring.StiffnessMediumLow,
                                            visibilityThreshold = IntOffset.VisibilityThreshold
                                        )
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            viewModel.markPoiArrived(projectUid, item.id)
                                        },
                                        onLongClick = {
                                            showEditDialog = true
                                            editPoiId = item.id
                                        }
                                    ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "${item.latitude} ${item.longitude}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (item.isArrived) "已到达" else "未到达",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 新建弹窗
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("新建途径点") },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("请输入途径点名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        if (inputName.trim().isNotEmpty() && selectPoint != null) {
                            viewModel.addPoiToCurrentMap(selectPoint!!.latitude, selectPoint!!.longitude)
                            poiList.sortedBy { poi ->
                                val latLng1 = LatLng(position.first, position.second)
                                val latLng2 = LatLng(poi.latitude, poi.longitude)
                                AMapUtils.calculateLineDistance(latLng1,latLng2)
                            }.reversed()
                        }
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }

    // 编辑弹窗
    if (showEditDialog) {
        Dialog(
            onDismissRequest = { showEditDialog = false },
        ) {
            Card {
                Column {
                    Text("编辑途径点")
                    TextButton(onClick = {
                        showEditDialog = false
                    }) {
                        Text("重命名")
                    }
                    TextButton(onClick = {
                        viewModel.deletePoiToCurrentMap(editPoiId)
                        showEditDialog = false
                    }) {
                        Text("删除")
                    }
                    TextButton(onClick = {
                        showEditDialog = false
                    }) {
                        Text("取消")
                    }
                }
            }
        }
    }

    MapLifecycle(
        mapView,
        onCreate = { currentMapView, _ ->
            currentMapView.map.setMapLanguage(AMap.CHINESE)
        },
        onDestroy = { currentMapView ->
            poiMarkers.forEach { it.remove() }
            poiMarkers.clear()
            currentMapView.map.isMyLocationEnabled = false
        }
    )
}

private fun MapView.componentCallbacks(): ComponentCallbacks =
    object : ComponentCallbacks {
        override fun onConfigurationChanged(config: Configuration) {}

        override fun onLowMemory() {
            this@componentCallbacks.onLowMemory()
        }
    }

@Composable
fun MapLifecycle(
    mapView: MapView,
    onCreate: (MapView, Bundle) -> Unit = { _: MapView, _: Bundle -> },
    onResume: (MapView) -> Unit = { _: MapView -> },
    onPause: (MapView) -> Unit = { _: MapView -> },
    onDestroy: (MapView) -> Unit = { _: MapView -> }
) {
    val context = LocalContext.current
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(context, lifecycle, mapView) {
        val mapLifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    mapView.onCreate(Bundle())
                    onCreate(mapView, Bundle())
                }

                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    onResume(mapView)
                }

                Lifecycle.Event.ON_PAUSE -> {
                    mapView.onPause()
                    onPause(mapView)
                }

                Lifecycle.Event.ON_DESTROY -> {
                    mapView.onDestroy()
                    onDestroy(mapView)
                }

                else -> {}
            }
        }
        val callbacks = mapView.componentCallbacks()
        lifecycle.addObserver(mapLifecycleObserver)
        context.registerComponentCallbacks(callbacks)
        onDispose {
            lifecycle.removeObserver(mapLifecycleObserver)
            context.unregisterComponentCallbacks(callbacks)
        }
    }
}
