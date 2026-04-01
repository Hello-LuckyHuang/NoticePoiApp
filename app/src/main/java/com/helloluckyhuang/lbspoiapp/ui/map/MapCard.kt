package com.helloluckyhuang.lbspoiapp.ui.map

import android.Manifest
import android.content.ComponentCallbacks
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.maps2d.AMap
import com.amap.api.maps2d.AMapUtils
import com.amap.api.maps2d.MapView
import com.amap.api.maps2d.model.Circle
import com.amap.api.maps2d.model.CircleOptions
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
    val circleMarkers = remember { mutableListOf<Circle>() }

    var position by remember { mutableStateOf<LatLng?>(null) }
    val sortedPoiList = remember(poiList, position) {
        val currentPosition = position
        if (currentPosition == null) {
            poiList
        } else {
            poiList.sortedBy { poi ->
                AMapUtils.calculateLineDistance(currentPosition, LatLng(poi.latitude, poi.longitude))
            }
        }
    }

    // 新建弹窗
    var showCreateDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var inputDistance by remember { mutableStateOf("") }
    var selectPoint by remember { mutableStateOf<LatLng?>(null) }

    // 编辑弹窗
    var showEditDialog by remember { mutableStateOf(false) }
    var editPoiId by remember { mutableIntStateOf(0) }

    // 重命名弹窗
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameName by remember { mutableStateOf("") }

    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocationPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
            map.setMyLocationStyle(MyLocationStyle().apply {
                myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER)
                interval(2000)
            })
            map.isMyLocationEnabled = false
            map.setOnMyLocationChangeListener { location ->
                val lat = location.latitude
                val lng = location.longitude
                val isValidCoordinate = lat in -90.0..90.0 &&
                    lng in -180.0..180.0 &&
                    !(lat == 0.0 && lng == 0.0)
                if (!isValidCoordinate) {
                    return@setOnMyLocationChangeListener
                }

                val currentLatLng = LatLng(lat, lng)
                position = currentLatLng
                // 标记靠近的点
                latestPoiListState.value.forEach { poi ->
                    if (!poi.isArrived) {
                        val poiLatLng = LatLng(poi.latitude, poi.longitude)
                        val distance = AMapUtils.calculateLineDistance(currentLatLng, poiLatLng)
                        if (distance < poi.arriveDistance) {
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

    LaunchedEffect(hasLocationPermission, mapView) {
        mapView.map.isMyLocationEnabled = hasLocationPermission
    }

    LaunchedEffect(poiList) {
        val aMap = mapView.map
        poiMarkers.forEach { it.remove() }
        circleMarkers.forEach { it.remove() }
        poiMarkers.clear()
        circleMarkers.clear()
        poiList.forEach { point ->
            val marker = aMap.addMarker(
                MarkerOptions()
                    .position(LatLng(point.latitude, point.longitude))
                    .title("点 ${point.label}")
                    .snippet("接近距离：${point.arriveDistance}米")
            )
            val circle = aMap.addCircle(
                CircleOptions()
                    .center(LatLng(point.latitude, point.longitude))
                    .radius(point.arriveDistance)
                    .fillColor(android.graphics.Color.argb(128, 50, 50, 255))
                    .strokeColor(android.graphics.Color.argb(255, 10, 10, 255))
                    .strokeWidth(2f)
            )
            if (marker != null && circle != null) {
                poiMarkers.add(marker)
                circleMarkers.add(circle)
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
                                    .fillMaxWidth()
                                    .animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = spring<androidx.compose.ui.unit.IntOffset>(
                                            stiffness = Spring.StiffnessMediumLow,
                                            visibilityThreshold = IntOffset.VisibilityThreshold
                                        )
                                    )
                                    .combinedClickable(
                                        onClick = {
//                                            viewModel.markPoiArrived(projectUid, item.id)
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
                                    val poiLatLng = LatLng(item.latitude, item.longitude)
                                    val distance = position?.let { currentLatLng ->
                                        AMapUtils.calculateLineDistance(currentLatLng, poiLatLng)
                                    }
                                    val color = if (distance != null && distance < item.arriveDistance) Color.Green else Color.Black
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = color,
                                        textDecoration = if (item.isArrived) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (item.isArrived) "已到达" else "未到达",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = color
                                    )
                                    Text(
                                        text = if (distance == null) "距离: 定位中" else "距离: ${"%.2f".format(distance)} m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = color
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
                Column {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("请输入途径点名称") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inputDistance,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) {
                                inputDistance = input
                            }
                        },
                        label = { Text("请输入触发途径点距离(m)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        if (inputName.trim().isNotEmpty() && selectPoint != null) {
                            viewModel.addPoiToCurrentMap(selectPoint!!.latitude, selectPoint!!.longitude, inputDistance.toDoubleOrNull() ?: 100.0, inputName)
                            inputName = ""
                            inputDistance = ""
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
                        showRenameDialog = true
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

    // 重命名弹窗
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名途径点") },
            text = {
                OutlinedTextField(
                    value = renameName,
                    onValueChange = { renameName = it },
                    label = { Text("请输入途径点名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRenameDialog = false
                        if (renameName.trim().isNotEmpty() && selectPoint != null) {
                            viewModel.updatePoiLabel(projectUid,editPoiId, renameName)
                            renameName = ""
                        }
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRenameDialog = false
                        renameName = ""
                    }
                ) {
                    Text("取消")
                }
            }
        )
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
