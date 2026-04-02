package com.helloluckyhuang.lbspoiapp.ui.map

import android.Manifest
import android.content.ComponentCallbacks
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
//import com.amap.api.maps2d.AMap
//import com.amap.api.maps2d.CameraUpdateFactory
//import com.amap.api.maps2d.MapView
//import com.amap.api.maps2d.model.Circle
//import com.amap.api.maps2d.model.CircleOptions
//import com.amap.api.maps2d.model.LatLng
//import com.amap.api.maps2d.model.Marker
//import com.amap.api.maps2d.model.MarkerOptions
//import com.amap.api.maps2d.model.MyLocationStyle
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.Circle
import com.amap.api.maps.model.CircleOptions
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.helloluckyhuang.lbspoiapp.R
import com.helloluckyhuang.lbspoiapp.ui.component.SlidingDigitText
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiPoint
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiPointUiModel
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiProjectViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MapCard(
    projectUid: Int,
    viewModel: PoiProjectViewModel
) {
    // 地图标记
    val poiMarkersById = remember { mutableMapOf<Int, Marker>() }
    val circleMarkersById = remember { mutableMapOf<Int, Circle>() }
    val previousPoiSnapshotById = remember { mutableMapOf<Int, PoiPointUiModel>() }
    val reportedPoiIds = remember { mutableSetOf<Int>() }

    // 显示抽屉
    var showSheet by remember { mutableStateOf(false) }

    // 点列表
    val poiList by viewModel.uiPoiListState.collectAsState()

    // 位置坐标
    var locationPoint = viewModel.locationPoint

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

    // 权限申请(位置权限)
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
            map.myLocationStyle = MyLocationStyle().apply {
                myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
                interval(2000)
            }
            map.isMyLocationEnabled = true
            map.moveCamera(CameraUpdateFactory.zoomTo(17F))
            map.setOnMyLocationChangeListener { location ->
                locationPoint = PoiPoint(location.latitude, location.longitude)
            }
            map.setOnMapLongClickListener { point ->
                showCreateDialog = true
                inputName = ""
                selectPoint = point
            }
        }
    }

    // 定期更新位置
    LaunchedEffect(Unit) {
        while (true) {
            val lat = locationPoint.latitude
            val lng = locationPoint.longitude
            val isValidCoordinate = lat in -90.0..90.0 &&
                    lng in -180.0..180.0 &&
                    !(lat == 0.0 && lng == 0.0)
            if (isValidCoordinate) {
                viewModel.updateLocalPoint(locationPoint)
                viewModel.searchAndMarkArrivedPoi(projectUid)
            }
            delay(3000)
        }
    }

    LaunchedEffect(hasLocationPermission, mapView) {
        mapView.map.isMyLocationEnabled = hasLocationPermission
    }

    // 管理地图标记
    LaunchedEffect(poiList) {
        val validPoiIds = poiList.map { it.id }.toSet()
        reportedPoiIds.retainAll(validPoiIds)

        val aMap = mapView.map
        val removedPoiIds = previousPoiSnapshotById.keys - validPoiIds
        removedPoiIds.forEach { poiId ->
            poiMarkersById.remove(poiId)?.remove()
            circleMarkersById.remove(poiId)?.remove()
            previousPoiSnapshotById.remove(poiId)
        }

        poiList.forEach { point ->
            val latLng = LatLng(point.latitude, point.longitude)
            val previous = previousPoiSnapshotById[point.id]
            val marker = poiMarkersById[point.id]
            val circle = circleMarkersById[point.id]

            if (marker == null || circle == null) {
                val newMarker = aMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("点 ${point.label}")
                        .snippet("接近距离：${point.arriveDistance}米")
                )
                val newCircle = aMap.addCircle(
                    CircleOptions()
                        .center(latLng)
                        .radius(point.arriveDistance)
                        .fillColor(android.graphics.Color.argb(128, 50, 50, 255))
                        .strokeColor(android.graphics.Color.argb(255, 10, 10, 255))
                        .strokeWidth(2f)
                )
                if (newMarker != null && newCircle != null) {
                    poiMarkersById[point.id] = newMarker
                    circleMarkersById[point.id] = newCircle
                }
            } else if (previous != null) {
                if (previous.latitude != point.latitude || previous.longitude != point.longitude) {
                    marker.position = latLng
                    circle.center = latLng
                }
                if (previous.label != point.label) {
                    marker.title = "点 ${point.label}"
                }
                if (previous.arriveDistance != point.arriveDistance) {
                    marker.snippet = "接近距离：${point.arriveDistance}米"
                    circle.radius = point.arriveDistance
                }
            }

            previousPoiSnapshotById[point.id] = point
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
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false // 是否跳过半展开
        )

        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = poiList.isNotEmpty() && !showSheet,
                enter = fadeIn() + slideInVertically(),  // 进入动画
                exit = fadeOut() + slideOutVertically()  // 退出动画
            ) {
                if (poiList.isNotEmpty()) {
                    Card (
                        modifier = Modifier
                            .padding(horizontal = 30.dp, vertical = 12.dp)
                            .border(
                                width = 1.dp,
                                color = Color.LightGray,
                                shape = RoundedCornerShape(25.dp)
                            )
                            .clip(RoundedCornerShape(25.dp))
                            .align(Alignment.TopCenter),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        val item = poiList.first()
                        Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp)) {
                            val distance = item.distance
                            val color = if (distance != null && distance < item.arriveDistance) Color(0, 128, 0) else Color.Gray
                            HeightLightIcon(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                normalColor = Color.Gray,
                                blinkColor1 = Color.Green,
                                blinkColor2 = Color.Blue,
                                isBlinking = distance != null && distance < item.arriveDistance
                            )
                            Column(
                                modifier = Modifier.padding(10.dp)
                            ) {
                                SlidingDigitText(
                                    text = item.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = color
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row (horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (item.isArrived) "已到达" else "未到达",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = color
                                    )
                                    Text(
                                        text = "距离: ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = color
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Row (
                                modifier = Modifier.padding(end = 20.dp).align(Alignment.CenterVertically),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                SlidingDigitText(
                                    text = if (distance == null) "定位中" else if (distance < 1000) "%.2f".format(distance) else "%.2f".format(distance/1000),
                                    color = color,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 7.em
                                )
                                SlidingDigitText(
                                    text = if (distance == null) "" else if (distance < 1000) " m" else " km",
                                    color = color,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 3.em
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { showSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_location),
                    contentDescription = "打开抽屉"
                )
            }

            if (showSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSheet = false },
                    sheetState = sheetState
                ) {

                    if (poiList.isNotEmpty()) {
                        Column {
                            Text(
                                modifier = Modifier
                                    .padding(start = 16.dp, bottom = 8.dp),
                                color = Color.Gray,
                                text = "提醒点列表"
                            )
                            // 点列表
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = poiList,
                                    key = { it.id }
                                ) { item ->
                                    Modifier
                                        .fillMaxWidth()
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .border(
                                                width = 1.dp,
                                                color = Color.LightGray,
                                                shape = RoundedCornerShape(9.dp)
                                            )
                                            .clip(RoundedCornerShape(9.dp))
                                            .animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = spring<androidx.compose.ui.unit.IntOffset>(
                                                stiffness = Spring.StiffnessVeryLow,
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                visibilityThreshold = IntOffset.VisibilityThreshold
                                            ))
                                            .combinedClickable(
                                                onClick = {
//                                                viewModel.markPoiArrived(projectUid, item.id)
                                                },
                                                onLongClick = {
                                                    showEditDialog = true
                                                    editPoiId = item.id
                                                }
                                            )
                                    ) {
                                        Row(modifier = Modifier.fillMaxSize().padding(start = 16.dp)) {
                                            val distance = item.distance

                                            HeightLightIcon(
                                                modifier = Modifier.align(Alignment.CenterVertically),
                                                normalColor = Color.Gray,
                                                blinkColor1 = Color.Green,
                                                blinkColor2 = Color.Blue,
                                                isBlinking = distance != null && distance < item.arriveDistance
                                            )
                                            Column(
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                val color = if (distance != null && distance < item.arriveDistance) Color(0, 128, 0) else Color.Black
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
                                                    text = if (distance == null) "距离: 定位中" else "距离: ${if (distance < 1000) "%.2f m".format(distance) else "%.2f km".format(distance/1000)}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = color
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            Text(
                                color = Color.Gray,
                                fontSize = 3.5.em,
                                text = "目前没有创建途径点"
                            )
                            Text(
                                color = Color.Gray,
                                fontSize = 3.5.em,
                                text = "请长按地图新建点以创建到达提醒...\uD83D\uDE10"
                            )
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
                Column (
                    modifier = Modifier.padding(16.dp)
                ) {
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
                    if (viewModel.getPoiById(editPoiId) != null && viewModel.getPoiById(editPoiId)?.isArrived?:false) {
                        TextButton(onClick = {
                            viewModel.erasePoiArrived(projectUid, editPoiId)
                            showEditDialog = false
                        }) {
                            Text("重置到达")
                        }
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
                        if (renameName.trim().isNotEmpty()) {
                            viewModel.updatePoiLabel(projectUid,editPoiId, renameName)
                        }
                        renameName = ""
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
            poiMarkersById.values.forEach { it.remove() }
            poiMarkersById.clear()
            circleMarkersById.values.forEach { it.remove() }
            circleMarkersById.clear()
            previousPoiSnapshotById.clear()
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

@Composable
fun HeightLightIcon(
    modifier: Modifier = Modifier,
    outerSize: Dp = 24.dp,
    innerSize: Dp = 12.dp,
    normalColor: Color,
    blinkColor1: Color,
    blinkColor2: Color,
    isBlinking: Boolean,
    innerLightenFactor: Float = 0.25f,
) {
    var useBlinkColor1 by remember { mutableStateOf(true) }

    LaunchedEffect(isBlinking) {
        if (!isBlinking) {
            useBlinkColor1 = true
            return@LaunchedEffect
        }

        while (true) {
            useBlinkColor1 = !useBlinkColor1
            delay(320)
        }
    }

    val currentColor = when {
        !isBlinking -> normalColor
        useBlinkColor1 -> blinkColor1
        else -> blinkColor2
    }

    val innerColor = currentColor.lighter(innerLightenFactor)

    Box(
        modifier = modifier.size(outerSize),
        contentAlignment = Alignment.Center
    ) {
        // 外圈
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(currentColor, CircleShape)
        )

        // 内圈
        Box(
            modifier = Modifier
                .size(innerSize)
                .background(innerColor, CircleShape)
        )
    }
}

private fun Color.lighter(factor: Float): Color {
    return lerp(this, Color.White, factor.coerceIn(0f, 1f))
}
