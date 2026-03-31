package com.helloluckyhuang.lbspoiapp.ui.map

import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
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
    var clickedCoordinate by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            map.uiSettings.isZoomControlsEnabled = true
            val defaultPoint = LatLng(lat, lon)
            map.addMarker(MarkerOptions().position(defaultPoint).title("北京").snippet("DefaultMarker"))
            map.setOnMapClickListener { point ->
                clickedCoordinate = point.latitude to point.longitude
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )

        clickedCoordinate?.let { (clickLat, clickLon) ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(
                    text = "纬度: %.6f, 经度: %.6f".format(clickLat, clickLon),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }

    MapLifecycle(mapView, onCreate = { currentMapView, _ ->
        currentMapView.map.apply {
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 15F)
            setMapLanguage(AMap.CHINESE)
            moveCamera(cameraUpdate)
        }
    })
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
    val lifecycle = LocalLifecycleOwner.current.lifecycle
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
