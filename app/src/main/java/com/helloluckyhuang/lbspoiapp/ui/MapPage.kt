package com.helloluckyhuang.lbspoiapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.helloluckyhuang.lbspoiapp.ui.map.MapCard
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiProjectViewModel

@Composable
fun MapPage(
    viewModel: PoiProjectViewModel,
    projectUid: Int,
    onNavigateToHomePage: () -> Unit,
    onLocationUpdate: (latitude: Double, longitude: Double) -> Unit = { _, _ -> }
) {
    val poiList by viewModel.currentMapPoiList.collectAsState()

    LaunchedEffect(projectUid) {
        viewModel.loadMapPoiList(projectUid)
    }

    Column {
        Button(onClick = {
            viewModel.saveCurrentMapPoiList(projectUid) {
                onNavigateToHomePage()
            }
        }) {
            Text("返回首页")
        }
        MapCard(
            poiList = poiList,
            viewModel = viewModel,
            onMapClick = { latitude, longitude ->
                viewModel.addPoiToCurrentMap(latitude, longitude)
            }
        )
    }
}
