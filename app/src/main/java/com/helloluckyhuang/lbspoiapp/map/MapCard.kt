package com.helloluckyhuang.lbspoiapp.map

import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.maps2d.AMap
import com.amap.api.maps2d.CameraUpdateFactory
import com.amap.api.maps2d.MapView
import com.amap.api.maps2d.model.LatLng
import com.amap.api.maps2d.model.MarkerOptions

@Composable
fun MapCard() {
    val lat = 39.9042
    val lon = 116.4074

    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            map.uiSettings.isZoomControlsEnabled = true
            //画点
            val latLng = LatLng(lat, lon)
            map.addMarker(MarkerOptions().position(latLng).title("北京").snippet("DefaultMarker"))
        }
    }

    AndroidView(modifier = Modifier.fillMaxSize(), factory = {
        mapView
    })
    MapLifecycle(mapView, onCreate = { mapView, _ ->
        mapView.map.apply {
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 15F)
            setMapLanguage(AMap.CHINESE)
            moveCamera(cameraUpdate)
        }
    })
}

private fun MapView.componentCallbacks(): ComponentCallbacks =
    object : ComponentCallbacks {
        // 设备配置发生改变，组件还在运行时
        override fun onConfigurationChanged(config: Configuration) {}
        // 系统运行的内存不足时，可以通过实现该方法去释放内存或不需要的资源
        override fun onLowMemory() {
            // 调用地图的onLowMemory
            this@componentCallbacks.onLowMemory()
        }
    }

@Composable
fun MapLifecycle(
    mapView: MapView,
    onCreate: (MapView, Bundle) -> Unit = { mapView: MapView, bundle: Bundle -> },
    onResume: (MapView) -> Unit = { mapView: MapView -> },
    onPause: (MapView) -> Unit = { mapView: MapView -> },
    onDestroy: (MapView) -> Unit = { mapView: MapView -> }
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(context, lifecycle, mapView) {
//        val mapLifecycleObserver = mapView.lifecycleObserver()
        val mapLifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> { mapView.onCreate(Bundle()); onCreate(mapView, Bundle()) }
                Lifecycle.Event.ON_RESUME -> { mapView.onResume(); onResume(mapView) } // 重新绘制加载地图
                Lifecycle.Event.ON_PAUSE -> { mapView.onPause(); onPause(mapView) }  // 暂停地图的绘制
                Lifecycle.Event.ON_DESTROY -> { mapView.onDestroy(); onDestroy(mapView) } // 销毁地图
                else -> {}
            }
        }
        val callbacks = mapView.componentCallbacks()
        // 添加生命周期观察者
        lifecycle.addObserver(mapLifecycleObserver)
        // 注册ComponentCallback
        context.registerComponentCallbacks(callbacks)
        onDispose {
            // 删除生命周期观察者
            lifecycle.removeObserver(mapLifecycleObserver)
            // 取消注册ComponentCallback
            context.unregisterComponentCallbacks(callbacks)
        }
    }
}